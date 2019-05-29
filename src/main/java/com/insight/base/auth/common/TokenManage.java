package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.base.auth.common.dto.AuthInfo;
import com.insight.base.auth.common.dto.TokenPackage;
import com.insight.base.auth.common.dto.UserInfo;
import com.insight.base.auth.common.enums.TokenType;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.util.Json;
import com.insight.util.Redis;
import com.insight.util.Util;
import com.insight.util.common.ApplicationContextHolder;
import com.insight.util.encrypt.Encryptor;
import com.insight.util.pojo.AccessToken;
import com.insight.util.pojo.User;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 宣炳刚
 * @date 2017/9/7
 * @remark 用户身份验证令牌类
 */
public class TokenManage {
    private AuthMapper mapper;

    /**
     * RSA私钥
     */
    private static final String PRIVATE_KEY = "";

    /**
     * 当前令牌对应的关键数据集
     */
    private Token token;

    /**
     * 用户数据
     */
    private User user;

    /**
     * 构造函数
     */
    public TokenManage() {

    }

    /**
     * 构造函数
     *
     * @param user User实体数据
     */
    public TokenManage(User user) {
        mapper = ApplicationContextHolder.getContext().getBean(AuthMapper.class);
        this.user = user;

        String key = "User:" + user.getId();
        Redis.set(key, "User", Json.toJson(user));
        Redis.set(key, "IsInvalid", user.getInvalid().toString());
        Redis.set(key, "FailureCount", "0");

        String pw = user.getPassword();
        String password = pw.length() > 32 ? Encryptor.rsaDecrypt(pw, PRIVATE_KEY) : pw;
        Redis.set(key, "Password", password);
    }

    /**
     * 获取缓存中的令牌数据
     *
     * @param tokenId 令牌ID
     */
    public void getToken(String tokenId) {
        String key = "Token:" + tokenId;
        String json = Redis.get(key);
        token = Json.toBean(json, Token.class);
    }

    /**
     * 生成令牌数据包
     *
     * @param userAgent 用户信息
     * @param code      Code
     * @param appId     应用ID
     * @return 令牌数据包
     */
    public TokenPackage creatorKey(String userAgent, String code, String appId) {
        return creatorKey(userAgent, code, appId, null, null);
    }

    /**
     * 生成令牌数据包
     *
     * @param userAgent 用户信息
     * @param code      Code
     * @param appId     应用ID
     * @param tenantId  租户ID
     * @param deptId    登录部门ID
     * @return 令牌数据包
     */
    public TokenPackage creatorKey(String userAgent, String code, String appId, String tenantId, String deptId) {
        TokenPackage tokenPackage = initPackage(code, userAgent);
        token = new Token(appId, tenantId, deptId);
        if (tenantId != null) {
            List<AuthInfo> funs = mapper.getAuthInfos(appId, user.getId(), tenantId, deptId);
            List<String> list = funs.stream().filter(i -> i.getPermit() > 0).map(i -> {
                String urls = i.getInterfaces();
                String codes = i.getAuthCodes();
                return codes + (codes != null && urls != null ? "," : "") + urls;
            }).collect(Collectors.toList());
            token.setPermitFuncs(list);
        }

        String key = "Apps:" + user.getId();
        long life = getLife() * 12;
        Redis.set("Token:" + code, Json.toJson(token), life, TimeUnit.MILLISECONDS);
        Redis.set(key, appId, code);

        String val = (String) Redis.get(key, "Codes");
        String codes = (val == null || val.isEmpty()) ? "" : val + ",";
        Redis.set(key, "Codes", codes + code);

        return tokenPackage;
    }

    /**
     * 刷新Secret过期时间
     *
     * @param tokenId   令牌ID
     * @param userAgent 用户信息
     * @return 令牌数据包
     */
    public TokenPackage refreshToken(String tokenId, String userAgent) {
        token.refresh();
        long life = getLife() * 12;
        TokenPackage tokenPackage = initPackage(tokenId, userAgent);
        Redis.set("Token:" + tokenId, Json.toJson(token), life, TimeUnit.MILLISECONDS);

        return tokenPackage;
    }

    /**
     * 初始化令牌数据包
     *
     * @param code      Code
     * @param userAgent 用户信息
     * @return 令牌数据包
     */
    private TokenPackage initPackage(String code, String userAgent) {
        AccessToken accessToken = new AccessToken();
        accessToken.setId(code);
        accessToken.setUserId(user.getId());
        accessToken.setSecret(token.getSecretKey());

        AccessToken refreshToken = new AccessToken();
        refreshToken.setId(code);
        refreshToken.setUserId(user.getId());
        refreshToken.setSecret(token.getRefreshKey());

        TokenPackage tokenPackage = new TokenPackage();
        tokenPackage.setAccessToken(Json.toBase64(accessToken));
        tokenPackage.setRefreshToken(Json.toBase64(refreshToken));
        tokenPackage.setExpire(getLife());
        tokenPackage.setFailure(getLife() * 12);

        String key = tokenPackage.getAccessToken() + userAgent;
        token.setHash(Util.md5(key));

        UserInfo info = Json.clone(user, UserInfo.class);
        String imgUrl = user.getHeadImg();
        if (imgUrl == null || imgUrl.isEmpty()) {
            String defaultHead = (String) Redis.get("BaseConfig", "DefaultHead");
            info.setHeadImg(defaultHead);
        } else if (!imgUrl.contains("http://") && !imgUrl.contains("https://")) {
            String host = (String) Redis.get("BaseConfig", "FileHost");
            info.setHeadImg(host + imgUrl);
        }

        info.setTenantId(token.getTenantId());
        tokenPackage.setUserInfo(info);

        return tokenPackage;
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
     * 清除用户令牌
     */
    public void cleanTokens() {
        String key = "Apps:" + user.getId();
        if (!Redis.hasKey(key)) {
            return;
        }

        String val = Redis.get(key, "Codes").toString();
        String[] codes = val.split(",");
        for (String code : codes) {
            deleteToken(code);
        }

        Redis.set(key, "Codes", "");
    }

    /**
     * 验证Token是否合法
     *
     * @param hash 令牌MD5
     * @param key  密钥
     * @param type 验证类型(1:验证AccessToken、2:验证RefreshToken)
     * @return Token是否合法
     */
    public Boolean verifyToken(String hash, String key, TokenType type) {
        if (token == null) {
            return false;
        }

        return (type == TokenType.RefreshToken || getHash().equals(hash)) && token.verify(key, type);
    }

    /**
     * 使用缓存鉴权
     *
     * @param key 功能ID/代码或接口URL
     * @return 是否授权
     */
    public Boolean isPermitInCache(String key) {
        if (token == null) {
            return false;
        }

        return token.getPermitFuncs().stream().anyMatch(i -> i.contains(key));
    }

    /**
     * 验证PayPassword
     *
     * @param key PayPassword
     * @return PayPassword是否正确
     */
    public Boolean verifyPayPassword(String key) {
        if (user.getPayPassword() == null) {
            return null;
        }

        String pw = Util.md5(user.getId() + key);
        return pw.equals(user.getPayPassword());
    }

    /**
     * Token是否过期
     *
     * @return Token是否过期
     */
    public Boolean isExpiry() {
        return token == null || token.isExpiry(true);
    }

    /**
     * Token是否失效
     *
     * @return Token是否失效
     */
    public Boolean isFailure() {
        return token == null || token.isFailure();
    }

    /**
     * 用户是否失效状态
     *
     * @return 用户是否失效状态
     */
    public Boolean userIsInvalid() {
        String key = "User:" + user.getId();
        String value = (String) Redis.get(key, "LastFailureTime");
        if (value == null || value.isEmpty()) {
            return false;
        }

        LocalDateTime lastFailureTime = LocalDateTime.parse(value);
        LocalDateTime resetTime = lastFailureTime.plusMinutes(10);
        LocalDateTime now = LocalDateTime.now();

        Integer failureCount = (Integer) Redis.get(key, "FailureCount");
        if (failureCount > 0 && now.isAfter(resetTime)) {
            failureCount = 0;
        }

        return failureCount > 5 || (boolean) Redis.get(key, "IsInvalid");
    }

    /**
     * 累计失败次数(有效时)
     */
    @JsonIgnore
    public void addFailureCount() {
        if (userIsInvalid()) {
            return;
        }

        String key = "User:" + user.getId();
        int failureCount = (int) Redis.get(key, "FailureCount");
        failureCount++;
        Redis.set(key, "FailureCount", failureCount);
        Redis.set(key, "LastFailureTime", new Date());
    }

    /**
     * 获取RSA私钥
     *
     * @return RSA私钥
     */
    public String getPrivateKey() {
        return PRIVATE_KEY;
    }

    /**
     * 获取令牌哈希值
     *
     * @return 令牌哈希值
     */
    public String getHash() {
        return token.getHash();
    }

    /**
     * 获取令牌生命周期
     *
     * @return 令牌生命周期
     */
    public Long getLife() {
        return token.getLife();
    }

    /**
     * 获取应用ID
     *
     * @return 应用ID
     */
    public String getAppId() {
        return token.getAppId();
    }

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    public String getTenantId() {
        return token.getTenantId();
    }

    /**
     * 获取用户当前登录部门ID
     *
     * @return 部门ID
     */
    public String getDeptId() {
        return token.getDeptId();
    }

    /**
     * 获取用户密码
     *
     * @return 用户登录密码
     */
    public String getPassword() {
        return user.getPassword();
    }

    public String getMobile() {
        return user.getMobile();
    }

    public String getUserId() {
        return user.getId();
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public void setUser(User user) {
        this.user = user;
    }
}

