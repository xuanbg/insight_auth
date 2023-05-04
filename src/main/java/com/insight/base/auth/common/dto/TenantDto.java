package com.insight.base.auth.common.dto;

import com.insight.utils.pojo.base.BaseVo;
import com.insight.utils.pojo.base.BaseXo;

import java.util.List;

/**
 * @author 宣炳刚
 * @date 2023/5/4
 * @remark
 */
public class TenantDto extends BaseXo {

    /**
     * 微信用户唯一ID
     */
    private String unionId;

    /**
     * 租户集合
     */
    private List<BaseVo> tenants;

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public List<BaseVo> getTenants() {
        return tenants;
    }

    public void setTenants(List<BaseVo> tenants) {
        this.tenants = tenants;
    }
}
