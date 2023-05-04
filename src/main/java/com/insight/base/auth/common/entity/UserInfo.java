package com.insight.base.auth.common.entity;

import com.insight.utils.pojo.base.BaseXo;

import java.time.LocalDateTime;

/**
 * @author 宣炳刚
 * @date 2018/4/13
 * @remark 用户关键信息DTO
 */
public class UserInfo extends BaseXo {

    /**
     * 用户ID(UUID,唯一)
     */
    private Long id;

    /**
     * 用户编码
     */
    private String code;

    /**
     * 用户名称
     */
    private String name;

    /**
     * 用户账号,登录名
     */
    private String account;

    /**
     * 用户绑定手机号,可作登录名
     */
    private String mobile;

    /**
     * 用户绑定E-mail,可作登录名
     */
    private String email;

    /**
     * 微信昵称
     */
    private String wechatName;

    /**
     * 用户头像
     */
    private String headImg;

    /**
     * 是否内置用户
     */
    private Boolean builtin;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWechatName() {
        return wechatName;
    }

    public void setWechatName(String wechatName) {
        this.wechatName = wechatName;
    }

    public String getHeadImg() {
        return headImg;
    }

    public void setHeadImg(String headImg) {
        this.headImg = headImg;
    }

    public Boolean getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Boolean builtin) {
        this.builtin = builtin;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}