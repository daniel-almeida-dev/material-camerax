package br.com.danielalmeidadev.materialcamerax;

public class SaveOptions extends CameraController {

    public SaveOptions saveOnStorage(String saveDir, String successMessage) {
        this.saveDir = saveDir;
        this.successMessage = successMessage;
        this.saveType = SaveType.SAVE_TO_DISK;
        return this;
    }

    public SaveOptions saveAsUri() {
        this.saveType = SaveType.SAVE_AS_URI;
        return this;
    }
}
