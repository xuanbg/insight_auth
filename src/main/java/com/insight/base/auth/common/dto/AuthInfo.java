package com.insight.base.auth.common.dto;

import java.io.Serializable;

/**
 * @author 宣炳刚
 * @date 2017/9/15
 * @remark 授权信息类
 */
public class AuthInfo implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 功能ID
     */
    private String id;

    /**
     * 模块ID
     */
    private String navId;

    /**
     * 功能代码(多个以逗号分隔)
     */
    private String authCodes;

    /**
     * 接口URL(多个以逗号分隔)
     */
    private String interfaces;

    /**
     * 授权(0:拒绝;1:允许)
     */
    private Integer permit;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNavId() {
        return navId;
    }

    public void setNavId(String navId) {
        this.navId = navId;
    }

    public String getAuthCodes() {
        return authCodes;
    }

    public void setAuthCodes(String authCodes) {
        this.authCodes = authCodes;
    }

    public String getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(String interfaces) {
        this.interfaces = interfaces;
    }

    public Integer getPermit() {
        return permit;
    }

    public void setPermit(Integer permit) {
        this.permit = permit;
    }
}
