package com.insight.base.auth.common.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 宣炳刚
 * @date 2018/1/4
 * @remark 用户绑定设备实体
 */
public class Device implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 设备id
     */
    private String id;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 所属应用id
     */
    private String appId;

    /**
     * 设备型号
     */
    private String deviceModel;

    /**
     * 是否有效 0 有效 1 失效
     */
    private Boolean isInvalid;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 是否单点登录
     */
    private Boolean isSigninOne;

    public Boolean getSigninOne() {
        return isSigninOne;
    }

    public void setSigninOne(Boolean signinOne) {
        isSigninOne = signinOne;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public Boolean getInvalid() {
        return isInvalid;
    }

    public void setInvalid(Boolean invalid) {
        isInvalid = invalid;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
