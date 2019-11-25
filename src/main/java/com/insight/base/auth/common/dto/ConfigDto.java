package com.insight.base.auth.common.dto;

import com.insight.util.Json;

import java.io.Serializable;

/**
 * @author 宣炳刚
 * @date 2019-09-05
 * @remark 接口配置DTO
 */
public class ConfigDto implements Serializable {
    private static final long serialVersionUID = -1L;
    /**
     * 接口HTTP请求方法
     */
    private String method;

    /**
     * 接口URL
     */
    private String url;

    /**
     * 接口授权码
     */
    private String authCode;

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
     * 是否验证Token
     */
    private Boolean isVerify;

    /**
     * 是否限流
     */
    private Boolean isLimit;

    /**
     * 是否通过日志输出返回值
     */
    private Boolean isLogResult;

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

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
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

    public Boolean getVerify() {
        return isVerify;
    }

    public void setVerify(Boolean verify) {
        isVerify = verify;
    }

    public Boolean getLimit() {
        return isLimit;
    }

    public void setLimit(Boolean limit) {
        isLimit = limit;
    }

    public Boolean getLogResult() {
        return isLogResult;
    }

    public void setLogResult(Boolean logResult) {
        isLogResult = logResult;
    }

    @Override
    public String toString() {
        return Json.toJson(this);
    }
}
