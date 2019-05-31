package com.insight.base.auth.common;

import com.insight.base.auth.common.enums.TokenType;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.util.Json;
import com.insight.util.Redis;
import com.insight.util.Util;
import com.insight.util.pojo.User;

/**
 * @author 宣炳刚
 * @date 2017/9/7
 * @remark 用户身份验证令牌类
 */
public class TokenManage {
    private AuthMapper mapper;

    /**
     * 用户数据
     */
    private final User user;

    /**
     * 用户ID
     */
    private final String userId;

    /**
     * 当前令牌对应的关键数据集
     */
    private Token token;

    /**
     * 构造函数
     *
     * @param userId 用户ID
     */
    public TokenManage(String userId) {
        this.user = null;
        this.userId = userId;
    }

    /**
     * 构造函数
     *
     * @param user User实体数据
     */
    public TokenManage(User user) {
        this.user = user;
        this.userId = user.getId();
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
        return userId;
    }

    public void setToken(Token token) {
        this.token = token;
    }
}

