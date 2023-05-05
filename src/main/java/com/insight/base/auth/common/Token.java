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
     * 获取应用ID
     *
     * @return 应用ID
     */
    public Long getAppId() {
        return loginInfo.getAppId();
    }

    /**
     * 设置应用ID
     *
     * @param id 应用ID
     */
    public void setAppId(Long id) {
        loginInfo.setAppId(id);
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return loginInfo.getId();
    }

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    public Long getTenantId() {
        return loginInfo.getTenantId();
    }

    /**
     * 设置登租户名称
     *
     * @param name 租户名称
     */
    public void setTenantName(String name) {
        loginInfo.setTenantName(name);
    }

    /**
     * 设置登录机构ID
     *
     * @param id 机构ID
     */
    public void setOrgId(Long id) {
        loginInfo.setOrgId(id);
    }

    /**
     * 设置登录机构名称
     *
     * @param name 机构名称
     */
    public void setOrgName(String name) {
        loginInfo.setOrgName(name);
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

    public UserBase getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserBase userInfo) {
        this.userInfo = userInfo;
    }
}

