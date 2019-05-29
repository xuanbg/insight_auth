package com.insight.base.auth.common.dto;

import com.insight.util.Json;

import java.io.Serializable;
import java.util.List;

/**
 * @author 宣炳刚
 * @date 2018/4/20
 * @remark 导航数据类
 */
public class NavDTO implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 导航ID
     */
    private String id;

    /**
     * 父级导航ID
     */
    private String parentId;

    /**
     * 导航级别
     */
    private Integer type;

    /**
     * 索引,排序用
     */
    private Integer index;

    /**
     * 导航名称
     */
    private String name;

    /**
     * 导航图标(URL)
     */
    private String icon;

    /**
     * 导航路由URL(模块组该值为空)
     */
    private String url;

    /**
     * 功能集合
     */
    private List<FuncDTO> functions;

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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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

    public List<FuncDTO> getFunctions() {
        return functions;
    }

    public void setFunctions(List<FuncDTO> functions) {
        this.functions = functions;
    }
}
