package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.utils.Util;
import com.insight.utils.pojo.auth.LoginInfo;
import com.insight.utils.pojo.auth.TokenData;
import com.insight.utils.pojo.auth.TokenKey;
import com.insight.utils.pojo.user.UserBase;
import com.insight.utils.redis.Redis;

import java.time.LocalDateTime;

/**
 * @author 宣炳刚
 * @date 2018/1/4
 * @remark 令牌关键数据集
 */
public class Token extends TokenData {

    /**
     * Redis KEY
     */
    private String key;

    /**
     * 用户信息
     */
    private UserBase userInfo;

    /**
     * 构造方法
     */
    public Token() {
    }

    /**
     * 构造方法
     *
     * @param appId    应用ID
     * @param tenantId 租户ID
     * @param user     用户数据
     */
    public Token(Long appId, Long tenantId, UserBase user) {
        key = "App:" + appId;
        userInfo = user;

        loginInfo = new LoginInfo();
        loginInfo.setId(user.getId());
        loginInfo.setName(user.getName());
        loginInfo.setAppId(appId);
        loginInfo.setTenantId(tenantId);

        setPermitTime(LocalDateTime.now());
        setPermitLife(getLongValue("PermitLife"));
        setLife(getLongValue("TokenLife"));
        setVerifySource(getBooleanValue("VerifySource"));
        setSignInOne(getBooleanValue("SignInType"));
        setAutoRefresh(getBooleanValue("RefreshType"));
        setSecretKey(Util.uuid());
        setRefreshKey(Util.uuid());
    }

    /**
     * 验证刷新密钥
     *
     * @param token 用户令牌
     * @return 是否通过验证
     */
    @JsonIgnore
    public boolean refreshKeyIsInvalid(TokenKey token) {
        return token == null || !token.getSecret().equals(getRefreshKey());
    }

    /**
     * 获取Long值
     *
     * @param field Redis field
     * @return Long值
     */
    @JsonIgnore
    private Long getLongValue(String field) {
        String value = Redis.get(key, field);
        return Util.isNotEmpty(value) ? Long.valueOf(value) : null;
    }

    /**
     * 获取Boolean值
     *
     * @param field Redis field
     * @return Boolean值
     */
    @JsonIgnore
    private Boolean getBooleanValue(String field) {
        String value = Redis.get(key, field);
        return Util.isNotEmpty(value) ? Boolean.valueOf(value) : null;
    }

    @JsonIgnore
    public UserBase getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserBase userInfo) {
        this.userInfo = userInfo;
    }
}

