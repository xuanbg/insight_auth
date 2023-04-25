package com.insight.base.auth.common;

import com.insight.base.auth.common.client.MessageClient;
import com.insight.base.auth.common.client.RabbitClient;
import com.insight.base.auth.common.dto.LoginDto;
import com.insight.base.auth.common.dto.TokenDto;
import com.insight.base.auth.common.dto.UserInfo;
import com.insight.base.auth.common.entity.LoginInfo;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.utils.*;
import com.insight.utils.encrypt.Encryptor;
import com.insight.utils.pojo.app.Application;
import com.insight.utils.pojo.auth.AccessToken;
import com.insight.utils.pojo.auth.TokenInfo;
import com.insight.utils.pojo.base.BaseVo;
import com.insight.utils.pojo.base.BusinessException;
import com.insight.utils.pojo.message.SmsCode;
import com.insight.utils.pojo.user.User;
import com.insight.utils.wechat.WeChatHelper;
import com.insight.utils.wechat.WeChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SnowflakeCreator creator;
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

    @Value("${insight.sms.channel}")
    private String smsChannel;

    /**
     * 构造函数
     *
     * @param creator      雪花算法ID生成器
     * @param mapper       AuthMapper
     * @param weChatHelper WeChatHelper
     * @param client       MessageClient
     */
    public Core(SnowflakeCreator creator, AuthMapper mapper, WeChatHelper weChatHelper, MessageClient client) {
        this.creator = creator;
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
    public Long getUserId(String account) {
        String value = Redis.get("ID:" + account);
        if (Util.isNotEmpty(value)) {
            return Long.valueOf(value);
        }

        synchronized (this) {
            value = Redis.get("ID:" + account);
            if (Util.isNotEmpty(value)) {
                return Long.valueOf(value);
            }

            User user = mapper.getUser(account);
            if (user == null) {
                return null;
            }

            // 缓存用户ID到Redis
            Long userId = user.getId();
            Redis.set("ID:" + user.getAccount(), userId.toString());

            String mobile = user.getMobile();
            if (Util.isNotEmpty(mobile)) {
                Redis.set("ID:" + mobile, userId.toString());
            }

            String mail = user.getEmail();
            if (Util.isNotEmpty(mail)) {
                Redis.set("ID:" + mail, userId.toString());
            }

            String unionId = user.getUnionId();
            if (Util.isNotEmpty(unionId)) {
                Redis.set("ID:" + unionId, userId.toString());
            }

            // 解密用户密码
            String pw = user.getPassword();
            String password = pw.length() > 32 ? Encryptor.rsaDecrypt(pw, PRIVATE_KEY) : pw;
            user.setPassword(password);

            String key = "User:" + userId;
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
    public String getSmsCode(Long userId, String mobile) {
        String smsCode = Util.randomString(SMS_CODE_LENGTH);

        var sms = new SmsCode();
        sms.setChannel(smsChannel);
        sms.setMobile(mobile);
        sms.setCode(smsCode);
        client.sendSmsCode(sms);

        String key = Util.md5(mobile + Util.md5(smsCode));
        logger.info("账户[{}]的验证码为: {}", mobile, smsCode);
        return generateCode(userId, key, SMS_CODE_LEFT);
    }

    /**
     * 应用是否过期
     *
     * @param login  登录信息
     * @param userId 用户ID
     */
    public void checkExpired(LoginDto login, Long userId) {
        Long appId = login.getAppId();
        String key = "App:" + appId;
        if (!Redis.hasKey(key)) {
            Application app = mapper.getApp(appId);
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

        Long tenantId = login.getTenantId();
        if (tenantId == null) {
            return;
        }

        String date = Redis.get("App:" + appId, tenantId.toString());
        if (date == null) {
            mapper.getApps(appId).forEach(i -> Redis.setHash(key, i.getTenantId().toString(), i.getExpireDate()));
            date = Redis.get("App:" + appId, tenantId.toString());
        }

        LocalDate expire = LocalDate.parse(date);
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
        String fingerprint = login.getFingerprint();
        Long appId = login.getAppId();
        Long tenantId = login.getTenantId();

        // 加载用户授权码
        Long userId = user.getId();
        Token token = new Token(appId);
        token.setTenantId(tenantId);
        token.setTenantName(mapper.getTenant(tenantId));
        token.setUserType(user.getType());
        token.setUserId(userId);
        token.setUserCode(user.getCode());
        token.setUserName(user.getName());
        token.setMobile(user.getMobile());
        token.setHeadImg(user.getHeadImg());
        token.setPermitFuncs(mapper.getAuthInfos(appId, tenantId, userId));
        token.setPermitTime(LocalDateTime.now());

        if (tenantId != null) {
            BaseVo org = mapper.getLoginOrg(userId, tenantId);
            if (org != null) {
                token.setOrgId(org.getId());
                token.setOrgName(org.getName());
            }
        }

        // 缓存用户Token
        String key = "UserToken:" + userId;
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
        String key = "UserToken:" + token.getUserId();
        Redis.setHash(key, token.getAppId().toString(), tokenId);

        token.setSecretKey(Util.uuid());
        token.setPermitFuncs(mapper.getAuthInfos(token.getAppId(), token.getTenantId(), token.getUserId()));
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
        TokenDto tokenDto = new TokenDto();

        // 生成令牌数据
        long life = token.getLife();
        AccessToken accessToken = new AccessToken();
        accessToken.setId(tokenId);
        accessToken.setSecret(token.getSecretKey());
        tokenDto.setAccessToken(Json.toBase64(accessToken));
        tokenDto.setExpire(life);

        String hashKey = tokenDto.getAccessToken() + fingerprint;
        token.setHash(Util.md5(hashKey));
        LocalDateTime now = LocalDateTime.now();
        token.setExpiryTime(now.plusSeconds(TokenInfo.TIME_OUT + life));

        long failure = life * 12;
        AccessToken refreshToken = new AccessToken();
        refreshToken.setId(tokenId);
        refreshToken.setSecret(token.getRefreshKey());
        tokenDto.setRefreshToken(Json.toBase64(refreshToken));
        tokenDto.setFailure(failure);

        // 缓存令牌数据
        LocalDateTime failureTime = token.getFailureTime();
        if (failureTime == null || now.isAfter(failureTime)) {
            token.setFailureTime(now.plusSeconds(TokenInfo.TIME_OUT + failure));
            Redis.set("Token:" + tokenId, token.toString(), life > 0 ? TokenInfo.TIME_OUT + failure : -1);
        } else {
            Redis.set("Token:" + tokenId, token.toString());
        }

        // 构造用户信息
        Long userId = token.getUserId();
        String key = "User:" + userId;
        Map<Object, Object> user = Redis.getEntity(key);
        UserInfo info = Json.clone(user, UserInfo.class);
        String host = Redis.get("Config:FileHost");
        String imgUrl = info.getHeadImg();
        if (Util.isNotEmpty(imgUrl) && !imgUrl.matches("^https?://")) {
            info.setHeadImg(host + imgUrl);
        }

        tokenDto.setUserInfo(info);
        Long tenantId = token.getTenantId();
        if (tenantId != null) {
            LoginInfo loginInfo = new LoginInfo();
            loginInfo.setAppId(token.getAppId());
            loginInfo.setTenantId(tenantId);

            BaseVo data = mapper.getLoginOrg(userId, tenantId);
            if (data != null) {
                loginInfo.setOrgId(data.getId());
                loginInfo.setOrgName(data.getName());
                loginInfo.setAreaCode(data.getCode());
            }

            tokenDto.setLoginInfo(loginInfo);
        }

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
        if (Util.isEmpty(json)) {
            throw new BusinessException("指定的Token不存在");
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
    private String generateCode(Long userId, String key, long seconds) {
        String code = Util.uuid();
        String signature = Util.md5(key + code);

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
    public Long getId(String code) {
        String key = "Code:" + code;
        String val = Redis.get(key);
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
        Long userId = getId(code);
        if (userId == null) {
            throw new BusinessException("发生了一点小意外,请重新提交");
        }

        // 验证用户
        String key = "User:" + userId;
        User user = Json.clone(Redis.getEntity(key), User.class);
        if (user.getInvalid()) {
            throw new BusinessException("用户被禁止使用");
        }

        checkExpired(login, userId);
        checkType(login.getAppId(), user.getType());
        return creatorToken(code, login, user);
    }

    /**
     * 用户是否失效状态
     *
     * @return 用户是否失效状态
     */
    public int getFailureCount(Long userId) {
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
    public void bindOpenId(Long userId, String openId, String appId) {
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
    public void updateUnionId(Long userId, String unionId) {
        mapper.updateUnionId(userId, unionId);

        Redis.setHash("User:" + userId, "unionId", unionId);
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
        Long userId = creator.nextId(3);
        user.setId(userId);
        user.setType(0);
        user.setBuiltin(false);
        user.setInvalid(false);
        user.setCreator(user.getName());
        user.setCreatorId(userId);
        user.setCreatedTime(LocalDateTime.now());

        // 缓存用户ID到Redis
        Redis.set("ID:" + user.getMobile(), userId.toString());
        String unionId = user.getUnionId();
        if (unionId != null && !unionId.isEmpty()) {
            Redis.set("ID:" + unionId, userId.toString());
        }

        String key = "User:" + userId;
        Redis.setHash(key, Json.toStringValueMap(user), -1L);
        Redis.setHash(key, "FailureCount", 0);

        RabbitClient.addUser(user);
        return userId;
    }
}
