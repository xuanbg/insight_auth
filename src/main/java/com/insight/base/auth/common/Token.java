package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.util.Generator;
import com.insight.util.Redis;
import com.insight.util.pojo.AccessToken;
import com.insight.util.pojo.TokenInfo;

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
     * @param deptId   登录部门ID
     */
    Token(String userId, String appId, String tenantId, String deptId) {
        String key = "App:" + appId;
        setUserId(userId);
        setAppId(appId);
        setTenantId(tenantId);
        setDeptId(deptId);
        setPermitLife(Long.valueOf(Redis.get(key, "PermitLife")));
        setLife(Long.valueOf(Redis.get(key, "TokenLife")));
        setSignInOne(Boolean.valueOf(Redis.get(key, "SignInType")));
        setAutoRefresh(Boolean.valueOf(Redis.get(key, "RefreshType")));
        setSecretKey(Generator.uuid());
        setRefreshKey(Generator.uuid());
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

