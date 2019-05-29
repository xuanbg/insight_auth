package com.insight.base.auth.common;

import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.util.Generator;
import com.insight.util.Json;
import com.insight.util.Redis;
import com.insight.util.Util;
import com.insight.util.pojo.Reply;
import com.insight.util.pojo.User;
import com.insight.utils.message.pojo.Sms;
import com.insight.utils.wechat.WeChatHelper;
import com.insight.utils.wechat.WeChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 宣炳刚
 * @date 2017/9/7
 * @remark 用户身份验证核心类(组件类)
 * @since 3、管理签名-Code对应关系缓存
 */
@Component
public class Core {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AuthMapper authMapper;
    private final WeChatHelper weChatHelper;

    /**
     *
     * @param authMapper
     * @param weChatHelper
     */
    @Autowired
    public Core(AuthMapper authMapper, WeChatHelper weChatHelper) {
        this.authMapper = authMapper;
        this.weChatHelper = weChatHelper;
    }


    /**
     * 根据用户登录账号获取Account缓存中的用户ID
     *
     * @param account 登录账号(账号、手机号、E-mail、openId)
     * @return 用户ID
     */
    public String getUserId(String account) {
        String key = "ID:" + account;
        String userId = Redis.get(key);
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        synchronized (this) {
            userId = Redis.get(account);
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }

            User user = authMapper.getUser(account);
            if (user == null) {
                return null;
            }

            // 缓存用户ID到Redis
            userId = user.getId();
            key = "ID:" + user.getAccount();
            Redis.set(key, userId);

            String mobile = user.getMobile();
            if (mobile != null && !mobile.isEmpty()) {
                key = "ID:" + mobile;
                Redis.set(key, userId);
            }

            String unionId = user.getUnionId();
            if (unionId != null && !unionId.isEmpty()) {
                key = "ID:" + unionId;
                Redis.set(key, userId);
            }

            String mail = user.getEmail();
            if (mail != null && !mail.isEmpty()) {
                key = "ID:" + mail;
                Redis.set(key, userId);
            }

            TokenManage tokenManage = new TokenManage(user);

            return userId;
        }
    }

    /**
     * 生成Code,缓存后返回
     *
     * @param tokenManage TokenManage
     * @param account     登录账号
     * @param type        登录类型(0:密码登录、1:验证码登录)
     * @return Code
     */
    public String generateCode(TokenManage tokenManage, String account, int type) {
        String key;
        int seconds = 30;
        switch (type) {
            case 0:
                key = Util.md5(account + tokenManage.getPassword());
                break;
            case 1:
                // 生成短信验证码(5分钟内有效)并发送
                String mobile = tokenManage.getMobile();
                if (mobile == null || mobile.isEmpty()) {
                    return null;
                }

                seconds = 60 * 5;
                Map<String, Object> map = new HashMap<>(16);

                String smsCode = generateSmsCode(4, mobile, 5, 4);
                key = Util.md5(mobile + Util.md5(smsCode));
                map.put("code", smsCode);

                Reply reply = sendMessageSyn("SMS00005", mobile, map);
                if (reply == null || !reply.getSuccess()) {
                    return "短信发送失败";
                }
                break;
            default:
                // Invalid type! You guess, you guess, you guess. (≧∇≦)
                key = Util.md5(Generator.uuid());
                break;
        }

        String code = Generator.uuid();
        String signature = Util.md5(key + code);
        // 缓存签名-Code,以及Code-用户ID.
        Redis.set(signature, code, seconds, TimeUnit.SECONDS);
        Redis.set(code, tokenManage.getUserId(), seconds + 1, TimeUnit.SECONDS);

        return code;
    }

    /**
     * 生成短信验证码
     *
     * @param type    验证码类型(0:验证手机号;1:注册用户账号;2:重置密码;3:修改支付密码;4:登录验证码)
     * @param mobile  手机号
     * @param minutes 验证码有效时长(分钟)
     * @param length  验证码长度
     * @return 短信验证码
     */
    public String generateSmsCode(int type, String mobile, int minutes, int length) {
        String code = Generator.randomStr(length);
        logger.info("为手机号【" + mobile + "】生成了类型为" + type + "的验证码:" + code + ",有效时间:" + minutes + "分钟.");
        String key = "SMSCode:" + Util.md5(type + mobile + code);
        if (type == 4) {
            return code;
        }

        Redis.set(key, code, minutes, TimeUnit.MINUTES);
        return code;
    }

    /**
     * 同步发送短信
     *
     * @param templateId 短信模板ID
     * @param mobile     手机号
     * @param map        模板参数Map
     */
    public Reply sendMessageSyn(String templateId, String mobile, Map<String, Object> map) {
        Sms sms = new Sms();
        sms.setCode(templateId);
        sms.setParams(map);
        sms.setReceivers(Collections.singletonList(mobile));

        return null;
    }

    /**
     * 验证短信验证码
     *
     * @param type   验证码类型(0:验证手机号;1:注册用户账号;2:重置密码;3:修改支付密码;4:登录验证码)
     * @param mobile 手机号
     * @param code   验证码
     * @return 是否通过验证
     */
    public Boolean verifySmsCode(int type, String mobile, String code) {
        return verifySmsCode(type, mobile, code, false);
    }

    /**
     * 验证短信验证码
     *
     * @param type    验证码类型(0:验证手机号;1:注册用户账号;2:重置密码;3:修改支付密码;4:登录验证码)
     * @param mobile  手机号
     * @param code    验证码
     * @param isCheck 是否检验模式(true:检验模式,验证后验证码不失效;false:验证模式,验证后验证码失效)
     * @return 是否通过验证
     */
    public Boolean verifySmsCode(int type, String mobile, String code, Boolean isCheck) {
        String key = "SMSCode:" + Util.md5(type + mobile + code);
        Boolean isExisted = Redis.hasKey(key);
        if (!isExisted || isCheck) {
            return isExisted;
        }

        Redis.deleteKey(key);
        return true;
    }

    /**
     * 通过签名获取Code
     *
     * @param sign 签名
     * @return 签名对应的Code
     */
    public String getCode(String sign) {

        String code = Redis.get(sign);
        if (code == null || code.isEmpty()) {
            return null;
        }

        Redis.deleteKey(sign);
        return code;
    }

    /**
     * 根据用户ID获取Token缓存中的Token
     *
     * @param userId 用户ID
     * @return TokenManage(可能为null)
     */
    public TokenManage getUserInfo(String userId) {
        String key = "User:" + userId;
        String json = (String) Redis.get(key, "User");
        if (json == null || json.isEmpty()) {
            return null;
        }

        User user = Json.toBean(json, User.class);

        return new TokenManage(user);
    }

    /**
     * 查询指定ID的应用的令牌生命周期小时数
     *
     * @param appId 应用ID
     * @return 应用的令牌生命周期小时数
     */
    public Long getTokenLife(String appId) {
        if (appId == null || appId.isEmpty()) {
            return Long.valueOf("86400000");
        }

        // 从缓存读取应用的令牌生命周期
        String fiele = "TokenLife";
        Object val = Redis.get(appId, fiele);
        if (val != null) {
            return Long.valueOf(val.toString());
        }

        // 从数据库读取应用的令牌生命周期
        Long hours = authMapper.getTokenLife(appId);
        if (hours == null) {
            hours = Long.valueOf("86400000");
        }

        Redis.set(appId, fiele, hours.toString());

        return hours;
    }

    /**
     * 用户是否存在
     *
     * @param user User数据
     * @return 用户是否存在
     */
    public Boolean isExisted(User user) {
        return authMapper.getExistedUserCount(user.getAccount(), user.getMobile(), user.getEmail(), user.getUnionId()) > 0;
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @param appId  微信AppID
     * @return 用户对象实体
     */
    public User getUser(String userId, String appId) {
        return authMapper.getUserWithAppId(userId, appId);
    }

    /**
     * 根据授权码获取用户的微信OpenID
     *
     * @param code 授权码
     * @return 微信OpenID
     */
    public WeChatUser getWeChatInfo(String code, String weChatAppId) {
        Object secret = Redis.get(weChatAppId, "secret");

        return weChatHelper.getUserInfo(code, weChatAppId, secret.toString());
    }

    /**
     * 是否绑定了指定的应用
     *
     * @param tenantId 租户ID
     * @param appId    应用ID
     * @return 是否绑定了指定的应用
     */
    public Boolean containsApp(String tenantId, String appId) {
        return authMapper.containsApp(tenantId, appId) > 0;
    }

}
