package com.insight.base.auth.common.entity;

import com.insight.utils.pojo.base.BaseXo;

import java.time.LocalDate;

/**
 * @author 宣炳刚
 * @date 2019/12/23
 * @remark 租户应用实体类
 */
public class TenantApp extends BaseXo {

    /**
     * UUID主键
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 过期日期
     */
    private LocalDate expireDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }
}
