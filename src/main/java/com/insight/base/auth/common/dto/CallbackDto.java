package com.insight.base.auth.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.utils.encrypt.AesUtil;
import com.insight.utils.pojo.base.BaseXo;

/**
 * @author 宣炳刚
 * @date 2025/8/23
 * @remark
 */
public class CallbackDto extends BaseXo {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 账号
     */
    private String account;

    /**
     * 微信OpenId
     */
    @JsonIgnore
    private String openId;

    /**
     * 授权码
     */
    private String code;

    /**
     * 状态
     */
    private String service;

    /**
     * 重定向地址
     */
    private String redirect;

    /**
     * 应用密钥
     */
    private String appSecret;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getOpenId() {
        return openId == null ? "" : AesUtil.aesEncrypt(openId, appSecret);
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getService() {
        return service;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    @JsonIgnore
    public String getAuthId() {
        return openId;
    }

    @JsonIgnore
    public String getExtra() {
        return account == null ? "" : AesUtil.aesEncrypt(account, appSecret);
    }

    @JsonIgnore
    public String getTimespan() {
        var timespan = String.valueOf(System.currentTimeMillis());
        return AesUtil.aesEncrypt(timespan, appSecret);
    }
}
