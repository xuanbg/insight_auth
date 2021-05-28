package com.insight.base.auth.common.dto;

import com.insight.utils.pojo.BaseXo;
import com.insight.utils.pojo.FuncInfo;

/**
 * @author 宣炳刚
 * @date 2018/4/20
 * @remark 功能及权限数据DTO
 */
public class FuncDto extends BaseXo {

    /**
     * 功能ID
     */
    private Long id;

    /**
     * 导航ID
     */
    private Long navId;

    /**
     * 功能类型 0:全局功能;1:数据项功能;2:其他功能
     */
    private Integer type;

    /**
     * 索引,排序用
     */
    private Integer index;

    /**
     * 功能名称
     */
    private String name;

    /**
     * 授权码
     */
    private String authCodes;

    /**
     * 功能图标信息
     */
    private FuncInfo funcInfo;

    /**
     * 是否授权(true:已授权,false:已拒绝,null:未授权)
     */
    private Boolean permit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNavId() {
        return navId;
    }

    public void setNavId(Long navId) {
        this.navId = navId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthCodes() {
        return authCodes;
    }

    public void setAuthCodes(String authCodes) {
        this.authCodes = authCodes;
    }

    public FuncInfo getFuncInfo() {
        return funcInfo;
    }

    public void setFuncInfo(FuncInfo funcInfo) {
        this.funcInfo = funcInfo;
    }

    public Boolean getPermit() {
        return permit;
    }

    public void setPermit(Boolean permit) {
        this.permit = permit;
    }
}

