package com.insight.base.auth.common.dto;

import com.insight.utils.pojo.BaseXo;
import com.insight.utils.pojo.ModuleInfo;

import java.util.List;

/**
 * @author 宣炳刚
 * @date 2018/4/20
 * @remark 导航数据DTO
 */
public class NavDto extends BaseXo {

    /**
     * 导航ID
     */
    private Long id;

    /**
     * 父级导航ID
     */
    private Long parentId;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
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
}
