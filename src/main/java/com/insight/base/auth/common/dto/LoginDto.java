package com.insight.base.auth.common.dto;

import com.insight.util.Json;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * @author 宣炳刚
 * @date 2017/9/7
 * @remark 登录数据DTO
 */
public class LoginDto implements Serializable {
    private static final long serialVersionUID = -1L;
    /**
     * 应用ID
     */
    @NotEmpty(message = "应用ID不能为空")
    private String appId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 签名
     */
    private String signature;

    /**
     * 验证码/微信授权码(微信登录用)
     */
    private String code;

    /**
     * 微信appId(微信登录用)
     */
    private String weChatAppId;

    /**
     * 微信用户唯一ID(微信登录用)
     */
    private String unionId;

    /**
     * 是否替换用户的UnionId(微信登录用)
     */
    private Boolean isReplace;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备型号
     */
    private String deviceModel;

    /**
     * 用户特征串
     */
    private String fingerprint;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getWeChatAppId() {
        return weChatAppId;
    }

    public void setWeChatAppId(String weChatAppId) {
        this.weChatAppId = weChatAppId;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public Boolean getReplace() {
        return isReplace;
    }

    public void setReplace(Boolean replace) {
        isReplace = replace;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    public String toString() {
        return Json.toJson(this);
    }
}
