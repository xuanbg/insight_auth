package com.insight.base.auth.common;

import com.insight.base.auth.common.client.MessageClient;
import com.insight.base.auth.common.dto.*;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.util.Generator;
import com.insight.util.Json;
import com.insight.util.Redis;
import com.insight.util.Util;
import com.insight.util.encrypt.Encryptor;
import com.insight.util.pojo.*;
import com.insight.utils.wechat.WeChatHelper;
import com.insight.utils.wechat.WeChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

            User user = mapper.getUser(account);
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

            key = "User:" + userId;
            Redis.set(key, "User", Json.toJson(user));
            Redis.set(key, "IsInvalid", user.getInvalid().toString());
            Redis.set(key, "FailureCount", "0");

            String pw = user.getPassword();
            String password = pw.length() > 32 ? Encryptor.rsaDecrypt(pw, PRIVATE_KEY) : pw;
            Redis.set(key, "Password", password);

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

        Sms sms = new Sms();
        sms.setMobiles(mobile);
        sms.setScene("0001");
        sms.setParam(map);
        try {
            Reply reply = client.sendMessage(sms);
            String key = Util.md5(mobile + Util.md5(smsCode));
            logger.info("手机号{}的短信验证码为: {}", mobile, smsCode);

            return generateCode(userId, key, SMS_CODE_LEFT);
        }catch (Exception ex){
            return null;
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

        Token token = new Token(appId, tenantId, deptId);
        if (tenantId != null) {
            List<AuthDto> funs = mapper.getAuthInfos(appId, userId, tenantId, deptId);
            List<String> list = funs.stream().filter(i -> i.getPermit() > 0).map(AuthDto::getAuthCode).collect(Collectors.toList());
            token.setPermitFuncs(list);
        }

        String key = "User:" + userId;
        Redis.set(key, appId, code);

        return initPackage(token, code, fingerprint, userId, appId);
    }

    /**
     * 刷新Secret过期时间
     *
     * @param token       令牌
     * @param tokenId     令牌ID
     * @param fingerprint 用户特征串
     * @param userId      用户ID
     * @return 令牌数据包
     */
    public TokenDto refreshToken(Token token, String tokenId, String fingerprint, String userId) {
        String appId = token.getAppId();
        token.refresh();

        return initPackage(token, tokenId, fingerprint, userId, appId);
    }

    /**
     * 初始化令牌数据包
     *
     * @param token       令牌数据
     * @param code        Code
     * @param fingerprint 用户特征串
     * @param userId      用户ID
     * @param appId       应用ID
     * @return 令牌数据包
     */
    private TokenDto initPackage(Token token, String code, String fingerprint, String userId, String appId) {
        // 生成令牌数据
        AccessToken accessToken = new AccessToken();
        accessToken.setId(code);
        accessToken.setUserId(userId);
        accessToken.setSecret(token.getSecretKey());

        AccessToken refreshToken = new AccessToken();
        refreshToken.setId(code);
        refreshToken.setUserId(userId);
        refreshToken.setSecret(token.getRefreshKey());

        long life = token.getLife() * 12;
        TokenDto tokenDto = new TokenDto();
        tokenDto.setAccessToken(Json.toBase64(accessToken));
        tokenDto.setRefreshToken(Json.toBase64(refreshToken));
        tokenDto.setExpire(token.getLife());
        tokenDto.setFailure(life);

        // 缓存令牌数据
        String hashKey = tokenDto.getAccessToken() + fingerprint;
        token.setHash(Util.md5(hashKey));
        Redis.set("Token:" + code, Json.toJson(token), life, TimeUnit.MILLISECONDS);

        // 更新用户缓存
        String key = "User:" + userId;
        String json = Redis.get(key, "User");
        UserInfoDto info = Json.toBean(json, UserInfoDto.class);
        String imgUrl = info.getHeadImg();
        if (imgUrl == null || imgUrl.isEmpty()) {
            String defaultHead = Redis.get("Config:DefaultHead");
            info.setHeadImg(defaultHead);
        } else if (!imgUrl.contains("http://") && !imgUrl.contains("https://")) {
            String host = Redis.get("Config:FileHost");
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
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户对象实体
     */
    public User getUser(String userId) {
        return mapper.getUserWithId(userId);
    }

    /**
     * 用户是否失效状态
     *
     * @return 用户是否失效状态
     */
    public Boolean userIsInvalid(String userId) {
        String key = "User:" + userId;
        String value = Redis.get(key, "LastFailureTime");
        if (value == null || value.isEmpty()) {
            return false;
        }

        LocalDateTime lastFailureTime = LocalDateTime.parse(value);
        LocalDateTime resetTime = lastFailureTime.plusMinutes(10);
        LocalDateTime now = LocalDateTime.now();

        int failureCount = Integer.parseInt(Redis.get(key, "FailureCount"));
        if (failureCount > 0 && now.isAfter(resetTime)) {
            failureCount = 0;
        }

        return failureCount > 5 || Boolean.parseBoolean(Redis.get(key, "IsInvalid"));
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
        Integer count = mapper.addUserOpenId(openId, userId, appId);
        if (count <= 0) {
            logger.error("绑定openId写入数据到数据库失败!");
        }
    }

    /**
     * 是否绑定了指定的应用
     *
     * @param tenantId 租户ID
     * @param appId    应用ID
     * @return 是否绑定了指定的应用
     */
    public Boolean containsApp(String tenantId, String appId) {
        return mapper.containsApp(tenantId, appId) > 0;
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
