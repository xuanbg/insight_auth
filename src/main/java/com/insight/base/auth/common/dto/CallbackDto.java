package com.insight.base.auth.common.dto;

import com.insight.utils.pojo.base.BaseXo;

/**
 * @author 宣炳刚
 * @date 2025/8/23
 * @remark
 */
public class CallbackDto extends BaseXo {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 授权码
     */
    private String code;

    /**
     * 状态
     */
    private String state;

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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
