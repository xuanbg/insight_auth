package com.insight.base.auth.common;

import com.insight.base.auth.common.client.MessageClient;
import com.insight.base.auth.common.client.RabbitClient;
import com.insight.base.auth.common.dto.LoginDto;
import com.insight.base.auth.common.dto.TokenDto;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.utils.*;
import com.insight.utils.pojo.auth.TokenData;
import com.insight.utils.pojo.auth.TokenKey;
import com.insight.utils.pojo.base.BusinessException;
import com.insight.utils.pojo.message.SmsCode;
import com.insight.utils.pojo.user.User;
import com.insight.utils.pojo.wechat.WechatUser;
import com.insight.utils.redis.HashOps;
import com.insight.utils.redis.KeyOps;
import com.insight.utils.redis.StringOps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * @author 宣炳刚
 * @date 2017/9/7
 * @remark 用户身份验证核心类(组件类)
 */
@Component
public class Core {
    private final SnowflakeCreator creator;
    private final AuthMapper mapper;
    private final MessageClient client;

    /**
     * RSA私钥
     */
    private static final String PRIVATE_KEY = "";
    /**
     * Code生命周期(30秒)
     */
    private static final int GENERAL_CODE_LEFT = 30;
    /**
     * 登录短信验证码长度(6位)
     */
    private static final int SMS_CODE_LENGTH = 6;
    /**
     * 登录短信验证码有效时间(300秒)
     */
    private static final int SMS_CODE_LEFT = 300;

    @Value("${insight.sms.channel}")
    private String smsChannel;

    /**
     * 构造函数
     *
     * @param creator 雪花算法ID生成器
     * @param mapper  AuthMapper
     * @param client  MessageClient
     */
    public Core(SnowflakeCreator creator, AuthMapper mapper, MessageClient client) {
        this.creator = creator;
        this.mapper = mapper;
        this.client = client;
    }

    /**
     * 根据用户登录账号获取Account缓存中的用户ID
     *
     * @param account 登录账号(账号、手机号、E-mail、openId)
     * @return 用户ID
     */
    public Long getUserId(String account) {
        var value = StringOps.get("ID:" + account);
        if (Util.isNotEmpty(value)) {
            return Long.valueOf(value);
        }

        synchronized (this) {
            value = StringOps.get("ID:" + account);
            if (Util.isNotEmpty(value)) {
                return Long.valueOf(value);
            }

            var user = mapper.getUser(account);
            if (user == null) {
                return null;
            }

            // 缓存用户ID到Redis
            var userId = user.getId();
            StringOps.set("ID:" + user.getAccount(), userId);

            var mobile = user.getMobile();
            if (Util.isNotEmpty(mobile)) {
                StringOps.set("ID:" + mobile, userId);
            }

            var mail = user.getEmail();
            if (Util.isNotEmpty(mail)) {
                StringOps.set("ID:" + mail, userId);
            }

            var unionId = user.getUnionId();
            if (Util.isNotEmpty(unionId)) {
                StringOps.set("ID:" + unionId, userId);
            }

            user.setPassword(user.getPassword());

            var key = "User:" + userId;
            HashOps.putAll(key, user);
            HashOps.put(key, "FailureCount", 0);
            return userId;
        }
    }

    /**
     * 根据登录账号生成Code
     *
     * @param userId   用户ID
     * @param account  登录账号
     * @param password 密码
     * @return Code
     */
    public String getGeneralCode(Long userId, String account, String password) {
        var key = Util.md5(account + password);
        return generateCode(userId, key, GENERAL_CODE_LEFT);
    }

    /**
     * 根据手机号生成Code
     *
     * @param userId 用户ID
     * @param mobile 手机号
     * @return Code
     */
    public String getSmsCode(Long userId, String mobile) {
        var smsCode = Util.randomString(SMS_CODE_LENGTH);

        var sms = new SmsCode();
        sms.setChannel(smsChannel);
        sms.setType(1);
        sms.setMobile(mobile);
        sms.setCode(smsCode);
        client.sendSmsCode(sms);

        var key = Util.md5(mobile + Util.md5(smsCode));
        return generateCode(userId, key, SMS_CODE_LEFT);
    }

    /**
     * 生成令牌数据包
     *
     * @param login 用户登录数据
     * @param code  Code
     * @return Reply
     */
    public TokenDto creatorToken(LoginDto login, String code) {
        return creatorToken(login, getId(code));
    }

    /**
     * 生成令牌数据包
     *
     * @param login  登录信息
     * @param userId 用户Id
     * @return 令牌数据包
     */
    public TokenDto creatorToken(LoginDto login, Long userId) {
        var key = new TokenKey(login.getAppId(), login.getTenantId(), userId);
        return creatorToken(key, login.getFingerprint(), login.getDeviceId());
    }

    /**
     * 生成令牌数据包
     *
     * @param key         用户关键信息
     * @param fingerprint 用户特征串
     * @param deviceId    设备ID
     * @return 令牌数据包
     */
    private TokenDto creatorToken(TokenKey key, String fingerprint, String deviceId) {
        var app = mapper.getApp(key.getAppId());
        if (app == null) {
            throw new BusinessException("未找指定的应用");
        }

        if (app.getPlatform()) {
            key.setTenantId(null);
        } else {
            var tenants = mapper.getTenants(key.getAppId(), key.getUserId());
            if (Util.isEmpty(tenants)) {
                throw new BusinessException("应用未授权, 请先为租户授权此应用");
            }

            // 自动登录到唯一租户
            if (key.getTenantId() == null && tenants.size() == 1) {
                key.setTenantId(tenants.get(0).getId());
            }

            // 验证租户的应用授权是否在租期内
            if (key.getTenantId() != null) {
                var tenant = tenants.stream().filter(i -> i.equals(key.getTenantId())).findFirst().orElse(null);
                if (tenant == null) {
                    throw new BusinessException("应用未授权, 请先为租户授权此应用");
                }

                if (LocalDate.now().isAfter(tenant.getExpireDate())) {
                    throw new BusinessException("应用授权已过期,请续租");
                }
            }
        }

        // 验证设备ID绑定是否匹配
        if (app.getSigninOne() && Util.isNotEmpty(deviceId) && !"Unknown".equals(deviceId)) {
            var binding = mapper.bindingUser(key.getUserId(), deviceId);
            if (Util.isNotEmpty(binding)) {
                if (binding.stream().noneMatch(i -> i.userEquals(key.getUserId(), deviceId))) {
                    var device = binding.stream().filter(i -> i.userEquals(key.getUserId())).findFirst().orElse(null);
                    if (device == null){
                        throw new BusinessException("当前设备已绑定账号, 请使用正确的账号登录慧学堡!");
                    }else {
                        throw new BusinessException("%s的设备ID为: %s. 请使用正确的设备登录慧学堡!".formatted(device.getName(), device.getDeviceId()));
                    }
                }
            } else {
                var list = mapper.getUsers(key.getTenantId(), deviceId);
                if (list.isEmpty()) {
                    mapper.addUserDeviceId(key.getUserId(), deviceId);
                } else if (list.stream().noneMatch(i -> i.userEquals(key.getUserId()))) {
                    var other = list.get(0);
                    throw new BusinessException("当前的账号与所使用的设备不匹配! 该设备属于%s(%s)".formatted(other.getName(), other.getCode()));
                }
            }
        }

        // 加载用户信息并验证用户类型是否与应用匹配
        var userInfo = getUser(key.getUserId());
        if (app.getLimitType() != null && !Objects.equals(app.getLimitType(), userInfo.getType())) {
            throw new BusinessException("当前用户与应用不匹配，请使用正确的用户进行登录");
        }

        // 存在令牌数据时, 应用不限制单设备登录或指纹相同, 重用令牌Secret
        key.setSecret(Util.uuid());
        if (KeyOps.hasKey(key.getKey())) {
            var exist = StringOps.get(key.getKey(), TokenData.class);
            if (exist != null && (!app.getSigninOne() || exist.fingerprintIsMatch(fingerprint))) {
                key.setSecret(exist.getSecret());
            }
        }

        // 生成Token数据
        var token = key.convert(TokenData.class);
        token.setFingerprint(fingerprint);

        // 加载用户登录信息
        if (token.getTenantId() != null) {
            token.setTenantName(mapper.getTenant(token.getTenantId()));
            var org = mapper.getLoginOrg(token.getTenantId(), token.getUserId());
            if (org != null) {
                token.setOrgId(org.getId());
                token.setOrgName(org.getName());
            }
        }

        // 设定令牌生命周期
        var life = app.getTokenLife();
        token.setLife(life);
        token.setExpiryTime(LocalDateTime.now().plusSeconds(life + TokenData.TIME_OUT));
        token.setAutoRefresh(app.getAutoRefresh());

        // 加载用户授权码和授权生命周期
        var permitFuns = mapper.getAuthInfos(token);
        token.setPermitFuncs(permitFuns);
        token.setPermitLife(app.getPermitLife());
        token.setPermitTime(LocalDateTime.now());

        // 缓存令牌数据
        var expiryTime = life > 0 ? life + TokenData.TIME_OUT : -1;
        StringOps.set(token.getKey(), token, expiryTime);

        // 生成令牌数据
        var accessToken = Json.toBase64(key);
        return new TokenDto(accessToken, life, userInfo);
    }

    /**
     * 使用户离线
     *
     * @param key 用户关键信息
     */
    public void deleteToken(TokenKey key) {
        if (KeyOps.hasKey(key.getKey())) {
            KeyOps.delete(key.getKey());
        }
    }

    /**
     * 生成Code,缓存后返回
     *
     * @param userId  用户ID
     * @param key     密钥
     * @param seconds 缓存有效时间(秒)
     * @return Code
     */
    private String generateCode(Long userId, String key, long seconds) {
        var code = Util.uuid();
        var signature = Util.md5(key + code);

        // 用签名作为Key缓存Code
        StringOps.set("Sign:" + signature, code, seconds);

        // 用Code作为Key缓存用户ID
        StringOps.set("Code:" + code, userId, seconds + 1);
        return code;
    }

    /**
     * 通过签名获取Code
     *
     * @param sign 签名
     * @return 签名对应的Code
     */
    public String getCode(String sign) {
        var key = "Sign:" + sign;
        var code = StringOps.get(key);
        if (code == null || code.isEmpty()) {
            return null;
        }

        KeyOps.delete(key);
        return code;
    }

    /**
     * 通过Code获取用户ID
     *
     * @param code Code
     * @return Code对应的用户ID
     */
    public Long getId(String code) {
        var key = "Code:" + code;
        var val = StringOps.get(key);
        if (Util.isEmpty(val)) {
            return null;
        }

        KeyOps.delete(key);
        return Long.valueOf(val);
    }

    /**
     * 用户是否失效状态
     *
     * @return 用户是否失效状态
     */
    public int getFailureCount(Long userId) {
        var key = "User:" + userId;
        var value = HashOps.get(key, "LastFailureTime");
        if (value == null || value.isEmpty()) {
            return 0;
        }

        var lastFailureTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        var resetTime = lastFailureTime.plusMinutes(10);
        var now = LocalDateTime.now();

        var failureCount = Integer.parseInt(HashOps.get(key, "FailureCount"));
        if (failureCount > 0 && now.isAfter(resetTime)) {
            failureCount = 0;
            HashOps.put(key, "FailureCount", 0);
            HashOps.put(key, "LastFailureTime", DateTime.formatCurrentDate());
        }

        return failureCount;
    }

    /**
     * 根据授权码获取用户的微信OpenID
     *
     * @param code 授权码
     * @return 微信用户信息
     */
    public WechatUser getWeChatInfo(String code, String weChatAppId) {
        var key = "WeChatApp:" + weChatAppId;
        var secret = HashOps.get(key, "secret");

        return WechatHelper.getUserInfo(code, weChatAppId, secret);
    }

    /**
     * 记录用户绑定的微信OpenID
     *
     * @param userId 用户ID
     * @param openId 微信OpenID
     * @param appId  微信AppID
     */
    public void bindOpenId(Long userId, String openId, String appId) {
        var map = mapper.getOpenId(userId);
        Map<String, String> ids = map == null ? new HashMap<>(16) : map.get("openId");

        ids.put(openId, appId);
        mapper.updateOpenId(userId, ids);
    }

    /**
     * 更新用户微信UnionID
     *
     * @param userId   用户ID
     * @param unionId  微信UnionID
     * @param nickname 微信昵称
     */
    public void updateUnionId(Long userId, String unionId, String nickname) {
        mapper.updateUnionId(userId, unionId, nickname);

        var key = "User:" + userId;
        HashOps.put(key, "unionId", unionId);
        HashOps.put(key, "nickname", nickname);
        StringOps.set("ID:" + unionId, userId);
    }

    /**
     * 验证短信验证码
     *
     * @param verifyKey 验证参数
     */
    public void verifySmsCode(String verifyKey) {
        var reply = client.verifySmsCode(verifyKey);
        if (!reply.getSuccess()) {
            throw new BusinessException("短信验证码错误");
        }
    }

    /**
     * 新增用户
     *
     * @param user 用户信息
     */
    public Long addUser(User user) {
        var userId = creator.nextId(3);
        user.setId(userId);
        user.setType(0);
        user.setBuiltin(false);
        user.setInvalid(false);
        user.setCreator(user.getName());
        user.setCreatorId(userId);
        user.setCreatedTime(LocalDateTime.now());

        // 缓存用户ID到Redis
        StringOps.set("ID:" + user.getMobile(), userId);
        var unionId = user.getUnionId();
        if (unionId != null && !unionId.isEmpty()) {
            StringOps.set("ID:" + unionId, userId);
        }

        var key = "User:" + userId;
        HashOps.putAll(key, user);
        HashOps.put(key, "FailureCount", 0);

        RabbitClient.addUser(user);
        return userId;
    }

    /**
     * 获取缓存中指定ID的用户
     *
     * @param userId 用户ID
     * @return 用户数据
     */
    public User getUser(Long userId) {
        var key = "User:" + userId;
        if (!KeyOps.hasKey(key)) {
            throw new BusinessException("用户不存在");
        }

        var user = HashOps.entries(key, User.class);
        if (user.getInvalid()) {
            throw new BusinessException("用户被禁止使用");
        }

        return user;
    }
}
