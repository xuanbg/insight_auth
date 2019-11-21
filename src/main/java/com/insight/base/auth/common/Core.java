package com.insight.base.auth.common;

import com.insight.base.auth.common.client.MessageClient;
import com.insight.base.auth.common.client.RabbitClient;
import com.insight.base.auth.common.dto.*;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.util.*;
import com.insight.util.encrypt.Encryptor;
import com.insight.util.pojo.*;
import com.insight.utils.wechat.WeChatHelper;
import com.insight.utils.wechat.WeChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 宣炳刚
 * @date 2017/9/7
 * @remark 用户身份验证核心类(组件类)
 */
@Component
public class Core {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AuthMapper mapper;
    private final WeChatHelper weChatHelper;
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

    /**
     * 构造函数
     *
     * @param mapper       AuthMapper
     * @param weChatHelper WeChatHelper
     * @param client       MessageClient
     */
    public Core(AuthMapper mapper, WeChatHelper weChatHelper, MessageClient client) {
        this.mapper = mapper;
        this.weChatHelper = weChatHelper;
        this.client = client;
    }

    /**
     * 根据用户登录账号获取Account缓存中的用户ID
     *
     * @param account 登录账号(账号、手机号、E-mail、openId)
     * @return 用户ID
     */
    public String getUserId(String account) {
        String userId = Redis.get("ID:" + account);
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        synchronized (this) {
            userId = Redis.get(account);
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }

            User user = mapper.getUser(account);
            if (user == null) {
                return null;
            }

            // 缓存用户ID到Redis
            userId = user.getId();
            Redis.set("ID:" + user.getAccount(), userId);

            String mobile = user.getMobile();
            if (mobile != null && !mobile.isEmpty()) {
                Redis.set("ID:" + mobile, userId);
            }

            String mail = user.getEmail();
            if (mail != null && !mail.isEmpty()) {
                Redis.set("ID:" + mail, userId);
            }

            String unionId = user.getUnionId();
            if (unionId != null && !unionId.isEmpty()) {
                Redis.set("ID:" + unionId, userId);
            }

            // 解密用户密码
            String pw = user.getPassword();
            String password = pw.length() > 32 ? Encryptor.rsaDecrypt(pw, PRIVATE_KEY) : pw;
            user.setPassword(password);

            String key = "User:" + userId;
            Redis.set(key, Json.toMap(user));
            Redis.set(key, "FailureCount", 0);

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
    public String getGeneralCode(String userId, String account, String password) {
        String key = Util.md5(account + password);

        return generateCode(userId, key, GENERAL_CODE_LEFT);
    }

    /**
     * 根据手机号生成Code
     *
     * @param userId 用户ID
     * @param mobile 手机号
     * @return Code
     */
    public String getSmsCode(String userId, String mobile) {
        String smsCode = Generator.randomInt(SMS_CODE_LENGTH);
        Map<String, Object> map = new HashMap<>(4);
        map.put("code", smsCode);
        map.put("minutes", SMS_CODE_LEFT);

        NormalMessage message = new NormalMessage();
        message.setSceneCode("0001");
        message.setReceivers(mobile);
        message.setParams(map);
        message.setBroadcast(false);
        try {
            Reply reply = client.sendMessage(message);
            if (!reply.getSuccess()) {
                return reply.getMessage();
            }

            String key = Util.md5(mobile + Util.md5(smsCode));
            logger.info("账户[{}]的验证码为: {}", mobile, smsCode);

            return generateCode(userId, key, SMS_CODE_LEFT);
        } catch (Exception ex) {
            return "发送短信失败,请稍后重试";
        }
    }

    /**
     * 生成令牌数据包
     *
     * @param code   Code
     * @param login  登录信息
     * @param userId 用户ID
     * @return 令牌数据包
     */
    public TokenDto creatorToken(String code, LoginDto login, String userId) {
        String appId = login.getAppId();
        String tenantId = login.getTenantId();
        String deptId = login.getDeptId();
        String fingerprint = login.getFingerprint();

        List<String> list = getPermits(appId, userId, tenantId, deptId);
        Token token = new Token(userId, appId, tenantId, deptId);
        token.setPermitFuncs(list);
        token.setPermitTime(LocalDateTime.now());

        String key = "UserToken:" + userId;
        Redis.set(key, appId, code);

        return initPackage(token, code, fingerprint, appId);
    }

    /**
     * 刷新Secret过期时间
     *
     * @param token       令牌
     * @param tokenId     令牌ID
     * @param fingerprint 用户特征串
     * @return 令牌数据包
     */
    public TokenDto refreshToken(Token token, String tokenId, String fingerprint) {
        String appId = token.getAppId();
        token.refresh();

        return initPackage(token, tokenId, fingerprint, appId);
    }

    /**
     * 获取用户授权码
     *
     * @param appId    应用ID
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param deptId   登录部门ID
     * @return 用户授权码集合
     */
    public List<String> getPermits(String appId, String userId, String tenantId, String deptId) {
        return mapper.getAuthInfos(appId, userId, tenantId, deptId);
    }

    /**
     * 初始化令牌数据包
     *
     * @param token       令牌数据
     * @param code        Code
     * @param fingerprint 用户特征串
     * @param appId       应用ID
     * @return 令牌数据包
     */
    private TokenDto initPackage(Token token, String code, String fingerprint, String appId) {
        // 生成令牌数据
        AccessToken accessToken = new AccessToken();
        accessToken.setId(code);
        accessToken.setSecret(token.getSecretKey());

        AccessToken refreshToken = new AccessToken();
        refreshToken.setId(code);
        refreshToken.setSecret(token.getRefreshKey());

        long life = token.getLife();
        TokenDto tokenDto = new TokenDto();
        tokenDto.setAccessToken(accessToken.toString());
        tokenDto.setRefreshToken(refreshToken.toString());
        tokenDto.setExpire(life);
        tokenDto.setFailure(life * 12);

        // 缓存令牌数据
        String hashKey = tokenDto.getAccessToken() + fingerprint;
        token.setHash(Util.md5(hashKey));
        Redis.set("Token:" + code, Json.toJson(token), life * 12, TimeUnit.MILLISECONDS);

        // 更新用户缓存
        String key = "User:" + token.getUserId();
        UserInfoDto info = Redis.get(key, UserInfoDto.class);
        String host = Redis.get("Config:FileHost");
        String imgUrl = info.getHeadImg();
        if (imgUrl == null || imgUrl.isEmpty()) {
            String defaultHead = Redis.get("Config:DefaultHead");
            info.setHeadImg(host + defaultHead);
        } else if (!imgUrl.contains("http://") && !imgUrl.contains("https://")) {
            info.setHeadImg(host + imgUrl);
        }

        info.setTenantId(token.getTenantId());
        tokenDto.setUserInfo(info);

        return tokenDto;
    }

    /**
     * 获取缓存中的令牌数据
     *
     * @param tokenId 令牌ID
     * @return 缓存中的令牌
     */
    public Token getToken(String tokenId) {
        String key = "Token:" + tokenId;
        String json = Redis.get(key);
        if (json == null || json.isEmpty()) {
            return null;
        }

        return Json.toBean(json, Token.class);
    }

    /**
     * 使用户离线
     *
     * @param tokenId 令牌ID
     */
    public void deleteToken(String tokenId) {
        String key = "Token:" + tokenId;
        if (!Redis.hasKey(key)) {
            return;
        }

        Redis.deleteKey(key);
    }

    /**
     * 生成Code,缓存后返回
     *
     * @param userId  用户ID
     * @param key     密钥
     * @param seconds 缓存有效时间(秒)
     * @return Code
     */
    private String generateCode(String userId, String key, int seconds) {
        String code = Generator.uuid();
        String signature = Util.md5(key + code);

        // 用签名作为Key缓存Code
        Redis.set("Sign:" + signature, code, seconds, TimeUnit.SECONDS);

        // 用Code作为Key缓存用户ID
        Redis.set("Code:" + code, userId, seconds + 1, TimeUnit.SECONDS);

        return code;
    }

    /**
     * 通过签名获取Code
     *
     * @param sign 签名
     * @return 签名对应的Code
     */
    public String getCode(String sign) {
        String key = "Sign:" + sign;
        String code = Redis.get(key);
        if (code == null || code.isEmpty()) {
            return null;
        }

        Redis.deleteKey(key);
        return code;
    }

    /**
     * 通过Code获取用户ID
     *
     * @param code Code
     * @return Code对应的用户ID
     */
    public String getId(String code) {
        String key = "Code:" + code;
        String id = Redis.get(key);
        if (id == null || id.isEmpty()) {
            return null;
        }

        Redis.deleteKey(key);
        return id;
    }

    /**
     * 用户是否失效状态
     *
     * @return 用户是否失效状态
     */
    public int getFailureCount(String userId) {
        String key = "User:" + userId;
        String value = Redis.get(key, "LastFailureTime");
        if (value == null || value.isEmpty()) {
            return 0;
        }

        LocalDateTime lastFailureTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime resetTime = lastFailureTime.plusMinutes(10);
        LocalDateTime now = LocalDateTime.now();

        int failureCount = Integer.parseInt(Redis.get(key, "FailureCount"));
        if (failureCount > 0 && now.isAfter(resetTime)) {
            failureCount = 0;
            Redis.set(key, "FailureCount", 0);
            Redis.set(key, "LastFailureTime", DateHelper.getDateTime());
        }

        return failureCount;
    }

    /**
     * 根据授权码获取用户的微信OpenID
     *
     * @param code 授权码
     * @return 微信用户信息
     */
    public WeChatUser getWeChatInfo(String code, String weChatAppId) {
        String key = "WeChatApp:" + weChatAppId;
        String secret = Redis.get(key, "secret");

        return weChatHelper.getUserInfo(code, weChatAppId, secret);
    }

    /**
     * 记录用户绑定的微信OpenID
     *
     * @param userId 用户ID
     * @param openId 微信OpenID
     * @param appId  微信AppID
     */
    public void bindOpenId(String userId, String openId, String appId) {
        Map<String, Map<String, String>> map = mapper.getOpenId(userId);
        Map<String, String> ids = map == null ? new HashMap<>(16) : map.get("openId");

        ids.put(openId, appId);
        mapper.updateOpenId(userId, ids);
    }

    /**
     * 更新用户微信UnionID
     *
     * @param userId  用户ID
     * @param unionId 微信UnionID
     */
    public void updateUnionId(String userId, String unionId) {
        mapper.updateUnionId(userId, unionId);

        Redis.set("User:" + userId, "unionId", unionId);
        Redis.set("ID:" + unionId, userId);
    }

    /**
     * 验证短信验证码
     *
     * @param verifyKey 验证参数
     * @return Reply
     */
    public Reply verifySmsCode(String verifyKey) {
        return client.verifySmsCode(verifyKey);
    }

    /**
     * 新增用户
     *
     * @param name    用户姓名
     * @param mobile  用户手机号
     * @param unionId 微信UnionID
     * @param head    用户头像
     */
    public String addUser(String name, String mobile, String unionId, String head) {
        String userId = Generator.uuid();
        User user = new User();
        user.setId(userId);
        user.setName(name);
        user.setMobile(mobile);
        user.setUnionId(unionId);
        user.setHeadImg(head);
        user.setBuiltin(false);
        user.setInvalid(false);
        user.setCreator(name);
        user.setCreatorId(userId);
        user.setCreatedTime(LocalDateTime.now());

        // 缓存用户ID到Redis
        Redis.set("ID:" + mobile, userId);
        if (unionId != null && !unionId.isEmpty()) {
            Redis.set("ID:" + unionId, userId);
        }

        String key = "User:" + userId;
        Redis.set(key, Json.toMap(user));
        Redis.set(key, "FailureCount", 0);

        RabbitClient.sendTopic(user);
        return userId;
    }

    /**
     * 获取登录用户的导航数据
     *
     * @param info 用户登录信息
     * @return 导航数据
     */
    public List<NavDto> getNavigators(LoginInfo info) {
        return mapper.getNavigators(info.getTenantId(), info.getAppId(), info.getUserId(), info.getDeptId());
    }

    /**
     * 获取登录用户的模块功能数据
     *
     * @param info     用户登录信息
     * @param moduleId 模块ID
     * @return 模块功能数据
     */
    public List<FuncDto> getModuleFunctions(LoginInfo info, String moduleId) {
        return mapper.getModuleFunctions(info.getTenantId(), info.getDeptId(), info.getUserId(), moduleId);
    }
}
