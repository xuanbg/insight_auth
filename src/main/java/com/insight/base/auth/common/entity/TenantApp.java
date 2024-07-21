package com.insight.base.auth.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.utils.pojo.base.DataBase;

import java.time.LocalDate;
import java.util.Objects;

/**
 * @author 宣炳刚
 * @date 2019/12/23
 * @remark 租户应用实体类
 */
public class TenantApp extends DataBase {

    /**
     * 过期日期
     */
    private LocalDate expireDate;

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    @JsonIgnore
    public Boolean equals(Long id) {
        return Objects.equals(id, this.getId());
    }
}
