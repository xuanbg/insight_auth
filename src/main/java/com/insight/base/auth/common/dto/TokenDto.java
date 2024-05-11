package com.insight.base.auth.common.dto;

import com.insight.utils.pojo.base.BaseXo;
import com.insight.utils.pojo.user.UserBase;

/**
 * @author 宣炳刚
 * @date 2017/9/15
 * @remark 令牌数据DTO
 */
public class TokenDto extends BaseXo {

    /**
     * 访问令牌(Base64编码)
     */
    private String accessToken;

    /**
     * 令牌过期秒数
     */
    private Long expire;

    /**
     * 用户信息
     */
    private UserBase userInfo;

    public TokenDto() {
    }

    public TokenDto(String accessToken, Long expire) {
        this.accessToken = accessToken;
        this.expire = expire;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Long getExpire() {
        return expire;
    }

    public void setExpire(Long expire) {
        this.expire = expire;
    }

    public UserBase getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserBase userInfo) {
        this.userInfo = userInfo;
    }
}
