package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.base.auth.common.enums.TokenType;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.util.Generator;
import com.insight.util.Redis;
import com.insight.util.common.ApplicationContextHolder;
import com.insight.util.pojo.Application;
import com.insight.util.pojo.TokenInfo;

import java.time.LocalDateTime;

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
        AuthMapper mapper = ApplicationContextHolder.getContext().getBean(AuthMapper.class);
        setUserId(userId);
        setAppId(appId);
        setTenantId(tenantId);
        setDeptId(deptId);

        Application app = null;
        String key = "App:" + appId;
        Object tokenLife = Redis.get(key, "TokenLife");
        if (tokenLife == null) {
            app = mapper.getApp(appId);
            if (app == null) {
                setLife(Long.valueOf("7200000"));
            } else {
                setLife(app.getTokenLife() * 1000);
            }

            Redis.set(key, "TokenLife", getLife());
        } else {
            setLife(Long.valueOf(tokenLife.toString()));
        }

        Object signInType = Redis.get(key, "SignInType");
        if (signInType == null) {
            if (app == null) {
                app = mapper.getApp(appId);
            }

            if (app == null) {
                setSignInOne(false);
            } else {
                setSignInOne(app.getSigninOne());
            }

            Redis.set(key, "SignInType", getSignInOne().toString());
        } else {
            setSignInOne(Boolean.valueOf(signInType.toString()));
        }

        Object refreshType = Redis.get(key, "RefreshType");
        if (refreshType == null) {
            if (app == null) {
                app = mapper.getApp(appId);
            }

            if (app == null) {
                setAutoRefresh(false);
            } else {
                setAutoRefresh(app.getAutoRefresh());
            }

            Redis.set(key, "RefreshType", getAutoRefresh().toString());
        } else {
            setAutoRefresh(Boolean.valueOf(refreshType.toString()));
        }


        setSecretKey(Generator.uuid());
        setRefreshKey(Generator.uuid());
        setExpiryTime(LocalDateTime.now().plusSeconds(getLife() / 1000));
        setFailureTime(LocalDateTime.now().plusSeconds(getLife() * 12 / 1000));
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

    /**
     * 刷新令牌关键数据
     **/
    @JsonIgnore
    void refresh() {
        setExpiryTime(LocalDateTime.now().plusSeconds(getLife() / 1000));
        setFailureTime(LocalDateTime.now().plusSeconds(getLife() * 12 / 1000));
        setSecretKey(Generator.uuid());
    }
}

