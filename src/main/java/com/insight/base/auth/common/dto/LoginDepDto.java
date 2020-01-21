package com.insight.base.auth.common.dto;

import com.insight.util.Json;

import java.io.Serializable;

/**
 * @author 宣炳刚
 * @date 2020/1/21
 * @remark 可选登录部门实体类
 */
public class LoginDepDto implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 机构/部门ID
     */
    private String id;

    /**
     * 父级ID
     */
    private String parentId;

    /**
     * 节点类型
     */
    private Integer nodeType;

    /**
     * 排序索引
     */
    private Integer index;

    /**
     * 编码
     */
    private String code;

    /**
     * 名称
     */
    private String name;

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

    public Integer getNodeType() {
        return nodeType;
    }

    public void setNodeType(Integer nodeType) {
        this.nodeType = nodeType;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
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

    @Override
    public String toString() {
        return Json.toJson(this);
    }
}
