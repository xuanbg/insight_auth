package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.utils.Redis;
import com.insight.utils.Util;
import com.insight.utils.pojo.auth.AccessToken;
import com.insight.utils.pojo.auth.TokenInfo;

/**
 * @author 宣炳刚
 * @date 2018/1/4
 * @remark 令牌关键数据集
 */
public class Token extends TokenInfo {
    private String key;

    /**
     * 构造方法
     */
    public Token() {
    }

    /**
     * 构造方法
     *
     * @param appId 应用ID
     */
    public Token(Long appId) {
        key = "App:" + appId;
        setAppId(appId);
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
    public boolean verifyRefreshKey(AccessToken token) {
        return token != null && token.getSecret().equals(getRefreshKey());
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
}

