package br.com.danielalmeidadev.materialcamerax;

public class MaterialCameraX extends CameraOptions {

    private MaterialCameraX() {

    }

    public static MaterialCameraX instance() {

        return new MaterialCameraX();
    }
}
