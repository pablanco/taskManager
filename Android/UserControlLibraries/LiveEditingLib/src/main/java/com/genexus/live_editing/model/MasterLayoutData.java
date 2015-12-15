package com.genexus.live_editing.model;

import java.util.List;

public class MasterLayoutData {
    private List<ControlData> controls;
    private int windowHeight;
    private int windowWidth;

    public MasterLayoutData(List<ControlData> controls, int windowHeight, int windowWidth) {
        this.controls = controls;
        this.windowHeight = windowHeight;
        this.windowWidth = windowWidth;
    }

    public List<ControlData> getControls() {
        return controls;
    }

    public void setControls(List<ControlData> controls) {
        this.controls = controls;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }
}
