package com.insight.base.auth.common.dto;

import com.insight.utils.pojo.auth.LoginInfo;
import com.insight.utils.pojo.base.BaseXo;

/**
 * @author 宣炳刚
 * @date 2017/9/15
 * @remark 令牌数据DTO
 */
public class TokenDto extends BaseXo {

    /**
     * 访问令牌(Base64编码)
     */
    private String accessToken;

    /**
     * 刷新令牌(Base64编码)
     */
    private String refreshToken;

    /**
     * 令牌过期秒数
     */
    private Long expire;

    /**
     * 令牌失效秒数
     */
    private Long failure;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 登录信息
     */
    private LoginInfo loginInfo;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpire() {
        return expire;
    }

    public void setExpire(Long expire) {
        this.expire = expire;
    }

    public Long getFailure() {
        return failure;
    }

    public void setFailure(Long failure) {
        this.failure = failure;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public LoginInfo getLoginInfo() {
        return loginInfo;
    }

    public void setLoginInfo(LoginInfo loginInfo) {
        this.loginInfo = loginInfo;
    }
}
