package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.utils.Redis;
import com.insight.utils.Util;
import com.insight.utils.pojo.AccessToken;
import com.insight.utils.pojo.TokenInfo;

/**
 * @author 宣炳刚
 * @date 2018/1/4
 * @remark 令牌关键数据集
 */
public class Token extends TokenInfo {

    /**
     * 构造方法
     */
    public Token() {
    }

    /**
     * 构造方法
     *
     * @param userId   用户ID
     * @param appId    应用ID
     * @param tenantId 租户ID
     */
    Token(String userId, String appId, String tenantId) {
        String key = "App:" + appId;
        setUserId(userId);
        setAppId(appId);
        setTenantId(tenantId);
        setPermitLife(Long.valueOf(Redis.get(key, "PermitLife")));
        setLife(Long.valueOf(Redis.get(key, "TokenLife")));
        setVerifySource(Boolean.valueOf(Redis.get(key, "VerifySource")));
        setSignInOne(Boolean.valueOf(Redis.get(key, "SignInType")));
        setAutoRefresh(Boolean.valueOf(Redis.get(key, "RefreshType")));
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
}

