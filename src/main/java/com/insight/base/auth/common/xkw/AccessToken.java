package com.insight.base.auth.common.xkw;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.insight.utils.pojo.base.BaseXo;

/**
 * @author 宣炳刚
 * @date 2025/8/23
 * @remark
 */
public class AccessToken extends BaseXo {

    /**
     * AccessToken
     */
    @JsonAlias("access_token")
    private String token;

    /**
     * 过期时间
     */
    private Integer expires;

    /**
     * 错误信息
     */
    private String error;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getExpires() {
        return expires;
    }

    public void setExpires(Integer expires) {
        this.expires = expires;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
