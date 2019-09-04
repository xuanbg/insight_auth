package com.insight.base.auth.common.entity;

import javax.validation.constraints.NotEmpty;
import java.util.Date;

/**
 * @author 宣炳刚
 * @date 2019-08-29
 * @remark 接口配置信息
 */
public class InterfaceConfig {

    /**
     * 唯一ID
     */
    private String id;

    /**
     * 接口名称
     */
    @NotEmpty(message = "接口名称不能为空")
    private String name;

    /**
     * 接口HTTP请求方法
     */
    @NotEmpty(message = "接口请求方法不能为空")
    private String method;

    /**
     * 接口URL
     */
    @NotEmpty(message = "接口URL不能为空")
    private String url;

    /**
     * 接口类型:0.公开;1.私有;2.授权
     */
    @NotEmpty(message = "接口类型不能为空")
    private Integer type;

    /**
     * 接口URL正则表达式
     */
    private String regular;

    /**
     * 接口授权码
     */
    private String authCode;

    /**
     * 是否限流
     */
    @NotEmpty(message = "接口限流设置不能为空")
    private Boolean isLimit;

    /**
     * 访问最小时间间隔(秒),0表示无调用时间间隔
     */
    private Integer limitGap;

    /**
     * 限流周期(秒),null表示不进行周期性限流
     */
    private Integer limitCycle;

    /**
     * 限制次数/限流周期,null表示不进行周期性限流
     */
    private Integer limitMax;

    /**
     * 限流消息
     */
    private String message;

    /**
     * 接口描述
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createdTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getRegular() {
        return regular;
    }

    public void setRegular(String regular) {
        this.regular = regular;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public Boolean getLimit() {
        return isLimit;
    }

    public void setLimit(Boolean limit) {
        isLimit = limit;
    }

    public Integer getLimitGap() {
        return limitGap;
    }

    public void setLimitGap(Integer limitGap) {
        this.limitGap = limitGap;
    }

    public Integer getLimitCycle() {
        return limitCycle;
    }

    public void setLimitCycle(Integer limitCycle) {
        this.limitCycle = limitCycle;
    }

    public Integer getLimitMax() {
        return limitMax;
    }

    public void setLimitMax(Integer limitMax) {
        this.limitMax = limitMax;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
