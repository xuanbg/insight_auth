package com.insight.base.auth.common.dto;

import com.insight.util.Json;

/**
 * @author 宣炳刚
 * @date 2017/9/15
 * @remark 图标信息
 */
public class IconInfo {

    /**
     * 图标路径
     */
    private String iconUrl;

    /**
     * 是否开始分组
     */
    private Boolean isBeginGroup;

    /**
     * 是否隐藏文字
     */
    private Boolean isHideText;

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Boolean getBeginGroup() {
        return isBeginGroup;
    }

    public void setBeginGroup(Boolean beginGroup) {
        isBeginGroup = beginGroup;
    }

    public Boolean getHideText() {
        return isHideText;
    }

    public void setHideText(Boolean hideText) {
        isHideText = hideText;
    }

    @Override
    public String toString() {
        return Json.toJson(this);
    }
}
