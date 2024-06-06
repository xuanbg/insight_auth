package com.insight.base.auth.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.utils.pojo.base.BaseXo;

/**
 * @author 宣炳刚
 * @date 2024/6/6
 * @remark
 */
public class UserTenant extends BaseXo {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户代码
     */
    private String code;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 租户ID
     */
    private Long tenantId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @JsonIgnore
    public Boolean userEquals(Long id){
        return this.id.equals(id);
    }

    public Boolean tenantEquals(Long id){
        return tenantId.equals(id);
    }
}
