package com.genexus.live_editing.model;

import com.artech.base.services.Services;
import com.google.gson.annotations.SerializedName;

public class ControlData {
    private String name;

    @SerializedName("Class")
    private String themeClassName;

    @SerializedName("Layout")
    private String layoutId;

    @SerializedName("ObjName")
    private String parentObjectName;

    private boolean visible;

    private int level;

    @SerializedName("h")
    private int actualHeight;

    @SerializedName("w")
    private int actualWidth;

    @SerializedName("x")
    private int xPos;

    @SerializedName("y")
    private int yPos;

    @SerializedName("z")
    private int zPos;

    @SerializedName("oh")
    private int originalHeight;

    @SerializedName("oy")
    private int originalYPosition;

    @SerializedName("image")
    private String encodedImage;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThemeClassName() {
        return themeClassName;
    }

    public void setThemeClassName(String themeClassName) {
        this.themeClassName = themeClassName;
    }

    public String getLayoutId() {
        return layoutId;
    }

    public void setLayoutId(String layoutId) {
        this.layoutId = layoutId;
    }

    public String getParentObjectName() {
        return parentObjectName;
    }

    public void setParentObjectName(String parentObjectName) {
        this.parentObjectName = parentObjectName;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getActualHeight() {
        return Services.Device.dipsToPixels(actualHeight);
    }

    public void setActualHeight(int actualHeight) {
        this.actualHeight = Services.Device.pixelsToDips(actualHeight);
    }

    public int getActualWidth() {
        return Services.Device.dipsToPixels(actualWidth);
    }

    public void setActualWidth(int actualWidth) {
        this.actualWidth = Services.Device.pixelsToDips(actualWidth);
    }

    public int getxPos() {
        return Services.Device.dipsToPixels(xPos);
    }

    public void setxPos(int xPos) {
        this.xPos = Services.Device.pixelsToDips(xPos);
    }

    public int getyPos() {
        return Services.Device.dipsToPixels(yPos);
    }

    public void setyPos(int yPos) {
        this.yPos = Services.Device.pixelsToDips(yPos);
    }

    public int getzPos() {
        return zPos;
    }

    public void setzPos(int zPos) {
        this.zPos = zPos;
    }

    public int getOriginalHeight() {
        return Services.Device.dipsToPixels(originalHeight);
    }

    public void setOriginalHeight(int originalHeight) {
        this.originalHeight = Services.Device.pixelsToDips(originalHeight);
    }

    public int getOriginalYPosition() {
        return Services.Device.dipsToPixels(originalYPosition);
    }

    public void setOriginalYPosition(int originalYPosition) {
        this.originalYPosition = Services.Device.pixelsToDips(originalYPosition);
    }

    public String getEncodedImage() {
        return encodedImage;
    }

    public void setEncodedImage(String encodedImage) {
        this.encodedImage = encodedImage;
    }
}
