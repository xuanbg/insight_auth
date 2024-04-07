package com.insight.base.auth.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insight.utils.Util;
import com.insight.utils.pojo.auth.TokenData;
import com.insight.utils.pojo.user.UserBase;

/**
 * @author 宣炳刚
 * @date 2018/1/4
 * @remark 令牌关键数据集
 */
public class Token extends TokenData {

    /**
     * 用户信息
     */
    @JsonIgnore
    private UserBase userInfo;

    /**
     * 来源不匹配
     *
     * @param fingerprint 特征字符串
     * @return 是否不同来源
     */
    @JsonIgnore
    public Boolean sourceNotMatch(String fingerprint) {
        return getSignInOne() && Util.isNotEmpty(fingerprint) && !fingerprint.equals(getFingerprint());
    }

    /**
     * 类型不匹配
     *
     * @return 是否不同类型
     */
    @JsonIgnore
    public Boolean typeNotMatch() {
        return userInfo != null && getLimitType() != null && !getLimitType().equals(userInfo.getType());
    }

    public UserBase getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserBase userInfo) {
        this.userInfo = userInfo;
        setUserId(userInfo.getId());
    }
}

