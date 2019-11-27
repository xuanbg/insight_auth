package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.base.auth.common.enums.TokenType;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.util.Generator;
import com.insight.util.Redis;
import com.insight.util.common.ApplicationContextHolder;
import com.insight.util.pojo.Application;
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
        if (Redis.hasKey(key)) {
            setPermitLife(Long.valueOf(Redis.get(key, "PermitLife")));
            setLife(Long.valueOf(Redis.get(key, "TokenLife")));
            setSignInOne(Boolean.valueOf(Redis.get(key, "SignInType")));
            setAutoRefresh(Boolean.valueOf(Redis.get(key, "RefreshType")));
        } else {
            initAppInfo(appId);
        }

        setUserId(userId);
        setAppId(appId);
        setTenantId(tenantId);
        setDeptId(deptId);

        setSecretKey(Generator.uuid());
        setRefreshKey(Generator.uuid());
    }

    /**
     * 加载应用信息到Redis
     *
     * @param appId 应用ID
     */
    private void initAppInfo(String appId) {
        AuthMapper mapper = ApplicationContextHolder.getContext().getBean(AuthMapper.class);
        Application app = mapper.getApp(appId);
        if (app == null) {
            setPermitLife(Long.valueOf("300000"));
            setLife(Long.valueOf("7200000"));
            setSignInOne(false);
            setAutoRefresh(false);
        } else {
            setPermitLife(app.getPermitLife());
            setLife(app.getTokenLife());
            setSignInOne(app.getSigninOne());
            setAutoRefresh(app.getAutoRefresh());

            String key = "App:" + appId;
            Redis.set(key, "PermitLife", getPermitLife());
            Redis.set(key, "TokenLife", getLife());
            Redis.set(key, "SignInType", getSignInOne());
            Redis.set(key, "RefreshType", getAutoRefresh());
            Redis.set(key, "AutoTenant", app.getAutoTenant());
        }
    }

    /**
     * 验证密钥
     *
     * @param key     密钥
     * @param type    验证类型(1:验证AccessToken、2:验证RefreshToken)
     * @param tokenId 令牌ID
     * @param userId  用户ID
     * @return 是否通过验证
     */
    @JsonIgnore
    public Boolean verify(String key, TokenType type, String tokenId, String userId) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        // 单点登录时验证令牌ID
        if (getSignInOne()) {
            String appId = getAppId();
            String id = Redis.get("User:" + userId, appId);
            if (!tokenId.equals(id)) {
                return false;
            }
        }

        String secret = type == TokenType.AccessToken ? getHash() : getRefreshKey();

        return key.equals(secret);
    }
}

