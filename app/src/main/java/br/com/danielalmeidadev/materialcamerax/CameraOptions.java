package br.com.danielalmeidadev.materialcamerax;

import androidx.annotation.ColorRes;

public class CameraOptions extends SaveOptions {

    public CameraOptions allowRetry() {
        this.allowRetry = true;
        return this;
    }

    public CameraOptions labelSave(String labelSave) {
        this.labelSave = labelSave;
        return this;
    }

    public CameraOptions labelRetry(String labelRetry) {
        this.labelRetry = labelRetry;
        return this;
    }

    public CameraOptions controlsColor(@ColorRes int colorId) {
        this.controlsColor = colorId;
        return this;
    }
}
