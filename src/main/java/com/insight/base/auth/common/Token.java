package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.utils.Util;
import com.insight.utils.pojo.auth.TokenData;
import com.insight.utils.pojo.user.UserBase;
import com.insight.utils.redis.HashOps;

import java.time.LocalDate;
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
    @JsonIgnore
    private String appKey;

    /**
     * 用户信息
     */
    @JsonIgnore
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
        userInfo = user;
        appKey = "App:" + appId;

        var type = HashOps.get(appKey, "Type");
        setAppId(appId);
        setUserId(user.getId());
        setTenantId("0".equals(type) ? null : tenantId);
        setExpireDate(getDateValue(tenantId));
        setPermitTime(LocalDateTime.now());
        setPermitLife(getLongValue("PermitLife"));
        setLife(getLongValue("TokenLife"));
        setSignInOne(getBooleanValue("SignInOne"));
        setAutoRefresh(getBooleanValue("AutoRefresh"));
        setSecretKey(Util.uuid());
    }

    /**
     * 获取用户类型
     *
     * @return 用户类型
     */
    public Integer getType() {
        return userInfo.getType();
    }

    /**
     * 来源不匹配
     *
     * @param fingerprint 特征字符串
     * @return 是否不同来源
     */
    @JsonIgnore
    public Boolean sourceNotMatch(String fingerprint) {
        return getSignInOne() && Util.isNotEmpty(fingerprint) && !fingerprint.equals(getFingerprint());
    }

    public UserBase getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserBase userInfo) {
        this.userInfo = userInfo;
    }

    /**
     * 获取Long值
     *
     * @param field Redis field
     * @return Long值
     */
    private Long getLongValue(Object field) {
        String value = HashOps.get(appKey, field);
        return Util.isNotEmpty(value) ? Long.valueOf(value) : null;
    }

    /**
     * 获取日期值
     *
     * @param field Redis field
     * @return 日期值
     */
    private LocalDate getDateValue(Object field) {
        String value = HashOps.get(appKey, field);
        return Util.isNotEmpty(value) ? LocalDate.parse(value) : null;
    }

    /**
     * 获取Boolean值
     *
     * @param field Redis field
     * @return Boolean值
     */
    private Boolean getBooleanValue(Object field) {
        String value = HashOps.get(appKey, field);
        return Util.isNotEmpty(value) ? Boolean.valueOf(value) : null;
    }
}

