package com.insight.base.auth.common.dto;

import com.insight.utils.pojo.base.BaseXo;

/**
 * @author 宣炳刚
 * @date 2023/3/15
 * @remark CodeDTO
 */
public class CodeDto extends BaseXo {

    /**
     * 登录类型 0.密码登录, 1.验证码登录, 2.APP扫码授权登录, 3.获取临时访问令牌
     */
    private Integer type;

    /**
     * 用户登录账号
     */
    private String account;

    /**
     * 接口哈希值
     */
    private String hashKey;

    public Integer getType() {
        return type == null ? 0 : type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }
}
