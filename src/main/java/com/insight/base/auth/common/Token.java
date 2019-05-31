package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.base.auth.common.enums.TokenType;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.util.Generator;
import com.insight.util.Redis;
import com.insight.util.common.ApplicationContextHolder;
import com.insight.util.pojo.Application;
import com.insight.util.pojo.TokenInfo;

import java.util.Date;

/**
 * @author 宣炳刚
 * @date 2018/1/4
 * @remark 令牌关键数据集
 */
class Token extends TokenInfo {

    /**
     * 构造方法
     *
     * @param appId    应用ID
     * @param tenantId 租户ID
     * @param deptId   登录部门ID
     */
    Token(String appId, String tenantId, String deptId) {
        AuthMapper mapper = ApplicationContextHolder.getContext().getBean(AuthMapper.class);
        super.setAppId(appId);
        super.setTenantId(tenantId);
        super.setDeptId(deptId);

        Application app = null;
        String key = "App:" + appId;
        Object tokenLife = Redis.get(key, "TokenLife");
        if (tokenLife == null) {
            app = mapper.getApp(appId);

            if (app == null) {
                super.setLife(Long.valueOf("7200000"));
            } else {
                super.setLife(app.getTokenLife());
                Redis.set(key, "TokenLife", super.getLife().toString());
            }
        } else {
            super.setLife(Long.valueOf(tokenLife.toString()));
        }

        Object signInType = Redis.get(key, "SignInType");
        if (signInType == null) {
            if (app == null) {
                app = mapper.getApp(appId);
            }

            if (app == null) {
                super.setSignInOne(false);
            } else {
                super.setSignInOne(app.getSigninOne());
                Redis.set(key, "SignInType", super.getSignInOne().toString());
            }
        } else {
            super.setSignInOne(Boolean.valueOf(signInType.toString()));
        }

        Object refreshType = Redis.get(key, "RefreshType");
        if (refreshType == null) {
            if (app == null) {
                app = mapper.getApp(appId);
            }

            if (app == null) {
                super.setAutoRefresh(false);
            } else {
                super.setAutoRefresh(app.getAutoRefresh());
                Redis.set(key, "RefreshType", super.getAutoRefresh().toString());
            }
        } else {
            super.setAutoRefresh(Boolean.valueOf(refreshType.toString()));
        }

        setSecretKey(Generator.uuid());
        setRefreshKey(Generator.uuid());
        setExpiryTime(new Date(super.getLife() + System.currentTimeMillis()));
        setFailureTime(new Date(super.getLife() * 12 + System.currentTimeMillis()));
    }

    /**
     * 验证密钥
     *
     * @param key  密钥
     * @param type 验证类型(1:验证AccessToken、2:验证RefreshToken)
     * @return 是否通过验证
     */
    @JsonIgnore
    Boolean verify(String key, TokenType type) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        Boolean passed = key.equals(type == TokenType.AccessToken ? super.getSecretKey() : super.getRefreshKey());
        Date expiry = new Date(super.getLife() / 2 + System.currentTimeMillis());
        if (passed && type == TokenType.AccessToken && super.getAutoRefresh() && expiry.after(super.getExpiryTime())) {
            setExpiryTime(new Date(super.getLife() + System.currentTimeMillis()));
            setFailureTime(new Date(super.getLife() * 12 + System.currentTimeMillis()));
        }

        return passed;
    }

    /**
     * 刷新令牌关键数据
     **/
    @JsonIgnore
    void refresh() {
        setExpiryTime(new Date(super.getLife() + System.currentTimeMillis()));
        setFailureTime(new Date(super.getLife() * 12 + System.currentTimeMillis()));
        super.setSecretKey(Generator.uuid());
    }
}

