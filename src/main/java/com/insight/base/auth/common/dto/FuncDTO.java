package com.insight.base.auth.common.dto;

import com.insight.util.Json;

import java.io.Serializable;

/**
 * @author 宣炳刚
 * @date 2018/4/20
 * @remark 功能及权限数据类
 */
public class FuncDTO implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 功能ID
     */
    private String id;

    /**
     * 上级节点ID
     */
    private String navId;

    /**
     * 节点类型
     */
    private Integer type;

    /**
     * 功能名称
     */
    private String code;

    /**
     * 索引,排序用
     */
    private Integer index;

    /**
     * 功能名称
     */
    private String name;

    /**
     * 功能图标(URL)
     */
    private String icon;

    /**
     * 功能URL
     */
    private String url;

    /**
     * 是否授权(true:已授权,false:已拒绝,null:未授权)
     */
    private Boolean permit;

    /**
     * 是否开始分组
     */
    private Boolean beginGroup;

    /**
     * 是否隐藏文字
     */
    private Boolean hideText;

    @Override
    public String toString() {
        return Json.toJson(this);
    }

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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getPermit() {
        return permit;
    }

    public void setPermit(Boolean permit) {
        this.permit = permit;
    }

    public Boolean getBeginGroup() {
        return beginGroup;
    }

    public void setBeginGroup(Boolean beginGroup) {
        this.beginGroup = beginGroup;
    }

    public Boolean getHideText() {
        return hideText;
    }

    public void setHideText(Boolean hideText) {
        this.hideText = hideText;
    }
}

