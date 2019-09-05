package com.insight.base.auth.common.entity;

/**
 * @author 宣炳刚
 * @date 2017/9/15
 * @remark 授权信息类
 */
public class IconInfo {

    /**
     * 图标
     */
    private String icon;

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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

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
}
