package com.insight.base.auth.common;

import com.insight.base.auth.common.client.MessageClient;
import com.insight.base.auth.common.client.RabbitClient;
import com.insight.base.auth.common.dto.LoginDto;
import com.insight.base.auth.common.dto.TokenDto;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.utils.*;
import com.insight.utils.encrypt.Encryptor;
import com.insight.utils.pojo.auth.TokenData;
import com.insight.utils.pojo.auth.TokenKey;
import com.insight.utils.pojo.base.BusinessException;
import com.insight.utils.pojo.message.SmsCode;
import com.insight.utils.pojo.user.User;
import com.insight.utils.pojo.wechat.WechatUser;
import com.insight.utils.redis.Redis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


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
        var value = Redis.get("ID:" + account);
        if (Util.isNotEmpty(value)) {
            return Long.valueOf(value);
        }

        synchronized (this) {
            value = Redis.get("ID:" + account);
            if (Util.isNotEmpty(value)) {
                return Long.valueOf(value);
            }

            var user = mapper.getUser(account);
            if (user == null) {
                return null;
            }

            // 缓存用户ID到Redis
            var userId = user.getId();
            Redis.set("ID:" + user.getAccount(), userId.toString());

            var mobile = user.getMobile();
            if (Util.isNotEmpty(mobile)) {
                Redis.set("ID:" + mobile, userId.toString());
            }

            var mail = user.getEmail();
            if (Util.isNotEmpty(mail)) {
                Redis.set("ID:" + mail, userId.toString());
            }

            var unionId = user.getUnionId();
            if (Util.isNotEmpty(unionId)) {
                Redis.set("ID:" + unionId, userId.toString());
            }

            // 解密用户密码
            var pw = user.getPassword();
            var password = pw.length() > 32 ? Encryptor.rsaDecrypt(pw, PRIVATE_KEY) : pw;
            user.setPassword(password);

            var key = "User:" + userId;
            Redis.setHash(key, Json.toStringValueMap(user), -1L);
            Redis.setHash(key, "FailureCount", 0);
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
     * 应用是否过期
     *
     * @param tenantId 租户ID
     * @param appId    应用ID
     */
    public void checkExpired(Long tenantId, Long appId) {
        var key = "App:" + appId;
        if (!Redis.hasKey(key)) {
            var app = mapper.getApp(appId);
            if (app == null) {
                throw new BusinessException("未找指定的应用");
            }

            Redis.setHash(key, "Type", app.getType());
            Redis.setHash(key, "VerifySource", app.getVerifySource());
            Redis.setHash(key, "PermitLife", app.getPermitLife());
            Redis.setHash(key, "TokenLife", app.getTokenLife());
            Redis.setHash(key, "SignInType", app.getSigninOne());
            Redis.setHash(key, "RefreshType", app.getAutoRefresh());
        }

        if (tenantId == null) {
            return;
        }

        var date = Redis.get("App:" + appId, tenantId.toString());
        if (date == null) {
            var data = mapper.getApps(tenantId, appId);
            if (data == null) {
                throw new BusinessException("应用未授权, 请先为租户授权此应用");
            }

            Redis.setHash(key, data.getTenantId().toString(), data.getExpireDate());
            date = Redis.get("App:" + appId, tenantId.toString());
            if (date == null) {
                throw new BusinessException("应用已过期,请续租");
            }
        }

        var expire = LocalDate.parse(date);
        if (LocalDate.now().isAfter(expire)) {
            throw new BusinessException("应用已过期,请续租");
        }
    }

    /**
     * 检查用户类型和应用是否匹配
     *
     * @param appId    应用ID
     * @param userType 用户类型
     */
    public void checkType(Long appId, Integer userType) {
        var data = Redis.get("App:" + appId, "Type");
        if (data == null) {
            throw new BusinessException("应用类型数据不存在");
        }

        var type = Integer.parseInt(data);
        if (type > 0 && !userType.equals(type)) {
            throw new BusinessException("当前用户与应用不匹配，请使用正确的用户进行登录");
        }
    }

    /**
     * 生成令牌数据包
     *
     * @param code  Code
     * @param login 登录信息
     * @param user  用户信息
     * @return 令牌数据包
     */
    public TokenDto creatorToken(String code, LoginDto login, User user) {
        var fingerprint = login.getFingerprint();
        var appId = login.getAppId();
        var tenantId = login.getTenantId();

        // 加载用户授权码
        var userId = user.getId();
        var token = new Token(appId, tenantId, user);
        if (tenantId != null) {
            token.setTenantName(mapper.getTenant(tenantId));
            var org = mapper.getLoginOrg(userId, tenantId);
            if (org != null) {
                token.setOrgId(org.getId());
                token.setOrgName(org.getName());
            }
        }

        var permitFuns = mapper.getAuthInfos(appId, tenantId, userId);
        token.setPermitFuncs(permitFuns);

        // 缓存用户Token
        var key = "UserToken:" + userId;
        Redis.setHash(key, appId.toString(), code);
        return initPackage(code, token, fingerprint);
    }

    /**
     * 生成令牌数据包
     *
     * @param token       令牌
     * @param fingerprint 用户特征串
     * @return 令牌数据包
     */
    public TokenDto creatorToken(Token token, String fingerprint) {
        var tokenId = Util.uuid();
        var key = "UserToken:" + token.getUserId();
        Redis.setHash(key, token.getAppId().toString(), tokenId);

        var permitFuns = mapper.getAuthInfos(token.getAppId(), token.getTenantId(), token.getUserId());
        token.setSecretKey(Util.uuid());
        token.setPermitFuncs(permitFuns);
        return initPackage(tokenId, token, fingerprint);
    }

    /**
     * 刷新Secret过期时间
     *
     * @param tokenId     令牌ID
     * @param token       令牌
     * @param fingerprint 用户特征串
     * @return 令牌数据包
     */
    public TokenDto refreshToken(String tokenId, Token token, String fingerprint) {
        token.setSecretKey(Util.uuid());
        return initPackage(tokenId, token, fingerprint);
    }

    /**
     * 初始化令牌数据包
     *
     * @param tokenId     令牌ID
     * @param token       令牌数据
     * @param fingerprint 用户特征串
     * @return 令牌数据包
     */
    private TokenDto initPackage(String tokenId, Token token, String fingerprint) {
        var tokenDto = new TokenDto();

        // 生成令牌数据
        long life = token.getLife();
        var accessToken = new TokenKey();
        accessToken.setId(tokenId);
        accessToken.setSecret(token.getSecretKey());
        tokenDto.setAccessToken(Json.toBase64(accessToken));
        tokenDto.setExpire(life);

        var hashKey = tokenDto.getAccessToken() + fingerprint;
        token.setHash(Util.md5(hashKey));
        var now = LocalDateTime.now();
        token.setExpiryTime(now.plusSeconds(TokenData.TIME_OUT + life));

        var failure = life * 12;
        var refreshToken = new TokenKey();
        refreshToken.setId(tokenId);
        refreshToken.setSecret(token.getRefreshKey());
        tokenDto.setRefreshToken(Json.toBase64(refreshToken));
        tokenDto.setFailure(failure);

        // 缓存令牌数据
        var failureTime = token.getFailureTime();
        if (failureTime == null || now.isAfter(failureTime)) {
            token.setFailureTime(now.plusSeconds(TokenData.TIME_OUT + failure));
        }

        Redis.set("Token:" + tokenId, token.toString(), life > 0 ? TokenData.TIME_OUT + failure : -1);
        tokenDto.setUserInfo(token.getUserInfo());
        return tokenDto;
    }

    /**
     * 获取缓存中的令牌数据
     *
     * @param tokenId 令牌ID
     * @return 缓存中的令牌
     */
    public Token getToken(String tokenId) {
        var key = "Token:" + tokenId;
        var json = Redis.get(key);
        if (Util.isEmpty(json)) {
            throw new BusinessException(421, "指定的Token不存在");
        }

        var token = Json.toBean(json, Token.class);
        var user = getUser(token.getUserId());
        token.setUserInfo(user);
        return token;
    }

    /**
     * 使用户离线
     *
     * @param tokenId 令牌ID
     */
    public void deleteToken(String tokenId) {
        var key = "Token:" + tokenId;
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
    private String generateCode(Long userId, String key, long seconds) {
        var code = Util.uuid();
        var signature = Util.md5(key + code);

        // 用签名作为Key缓存Code
        Redis.set("Sign:" + signature, code, seconds);

        // 用Code作为Key缓存用户ID
        Redis.set("Code:" + code, userId.toString(), seconds + 1);

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
        var code = Redis.get(key);
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
    public Long getId(String code) {
        var key = "Code:" + code;
        var val = Redis.get(key);
        if (Util.isEmpty(val)) {
            return null;
        }

        Redis.deleteKey(key);
        return Long.valueOf(val);
    }

    /**
     * 获取Token
     *
     * @param code  Code
     * @param login 用户登录数据
     * @return Reply
     */
    public TokenDto getToken(String code, LoginDto login) {
        var userId = getId(code);
        if (userId == null) {
            throw new BusinessException("发生了一点小意外,请重新提交");
        }

        var user = getUser(userId);
        checkExpired(login.getTenantId(), login.getAppId());
        checkType(login.getAppId(), user.getType());
        return creatorToken(code, login, user);
    }

    /**
     * 用户是否失效状态
     *
     * @return 用户是否失效状态
     */
    public int getFailureCount(Long userId) {
        var key = "User:" + userId;
        var value = Redis.get(key, "LastFailureTime");
        if (value == null || value.isEmpty()) {
            return 0;
        }

        var lastFailureTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        var resetTime = lastFailureTime.plusMinutes(10);
        var now = LocalDateTime.now();

        var failureCount = Integer.parseInt(Redis.get(key, "FailureCount"));
        if (failureCount > 0 && now.isAfter(resetTime)) {
            failureCount = 0;
            Redis.setHash(key, "FailureCount", 0);
            Redis.setHash(key, "LastFailureTime", DateTime.formatCurrentDate());
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
        var secret = Redis.get(key, "secret");

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
        Redis.setHash(key, "unionId", unionId);
        Redis.setHash(key, "nickname", nickname);
        Redis.set("ID:" + unionId, userId.toString());
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
        Redis.set("ID:" + user.getMobile(), userId.toString());
        var unionId = user.getUnionId();
        if (unionId != null && !unionId.isEmpty()) {
            Redis.set("ID:" + unionId, userId.toString());
        }

        var key = "User:" + userId;
        Redis.setHash(key, Json.toStringValueMap(user), -1L);
        Redis.setHash(key, "FailureCount", 0);

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
        var data = Redis.getEntity("User:" + userId);
        var user = Json.clone(data, User.class);

        // 验证用户
        if (user.getInvalid()) {
            throw new BusinessException("用户被禁止使用");
        }

        return user;
    }
}
