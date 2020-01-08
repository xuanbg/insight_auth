package com.insight.base.auth.common.dto;

import com.insight.util.Json;
import com.insight.util.pojo.ModuleInfo;

import java.io.Serializable;
import java.util.List;

/**
 * @author 宣炳刚
 * @date 2018/4/20
 * @remark 导航数据DTO
 */
public class NavDto implements Serializable {
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
     * 模块信息
     */
    private ModuleInfo moduleInfo;

    /**
     * 功能集合
     */
    private List<FuncDto> functions;

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

    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public void setModuleInfo(ModuleInfo moduleInfo) {
        this.moduleInfo = moduleInfo;
    }

    public List<FuncDto> getFunctions() {
        return functions;
    }

    public void setFunctions(List<FuncDto> functions) {
        this.functions = functions;
    }

    @Override
    public String toString() {
        return Json.toJson(this);
    }
}
