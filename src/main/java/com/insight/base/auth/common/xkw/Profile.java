package com.insight.base.auth.common.xkw;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.insight.utils.pojo.base.BaseXo;

/**
 * @author 宣炳刚
 * @date 2025/8/23
 * @remark 学科网用户信息
 */
public class Profile extends BaseXo {

    /**
     * 学科网OpenId
     */
    @JsonAlias("open_id")
    private String openId;

    /**
     * 错误信息
     */
    private String error;

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
