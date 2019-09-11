package com.insight.base.auth.common.dto;

import com.insight.util.Json;

import java.io.Serializable;

/**
 * @author 宣炳刚
 * @date 2019-09-11
 * @remark 接口配置列表DTO
 */
public class ConfigListDto implements Serializable {
    private static final long serialVersionUID = -1L;
    /**
     * 唯一ID
     */
    private String id;

    /**
     * 接口名称
     */
    private String name;

    /**
     * 接口HTTP请求方法
     */
    private String method;

    /**
     * 接口URL
     */
    private String url;

    /**
     * 接口授权码
     */
    private String authCode;

    /**
     * 是否验证Token
     */
    private Boolean isVerify;

    /**
     * 是否限流
     */
    private Boolean isLimit;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public Boolean getVerify() {
        return isVerify;
    }

    public void setVerify(Boolean verify) {
        isVerify = verify;
    }

    public Boolean getLimit() {
        return isLimit;
    }

    public void setLimit(Boolean limit) {
        isLimit = limit;
    }

    @Override
    public String toString() {
        return Json.toJson(this);
    }
}
