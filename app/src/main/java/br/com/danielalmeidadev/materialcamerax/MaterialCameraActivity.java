package br.com.danielalmeidadev.materialcamerax;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.LifecycleCameraController;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.danielalmeidadev.materialcamerax.databinding.ActivityMaterialCameraBinding;

public class MaterialCameraActivity extends AppCompatActivity {

    private Uri savedUri;
    private int resultCode;

    @ColorRes
    private int controlsColor;
    private String labelSave;
    private String labelRetry;
    private boolean allowRetry;
    private String saveDir;
    private String successMessage;
    private SaveType saveType;

    private ActivityMaterialCameraBinding binding;
    private ExecutorService cameraExecutor;
    private LifecycleCameraController controller;
    boolean hasFlashUnit = false;

    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMaterialCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        controlsColor = getIntent().getIntExtra(CameraConstants.CONTROLS_COLOR, 0);
        labelSave = getIntent().getStringExtra(CameraConstants.LABEL_SAVE);
        labelRetry = getIntent().getStringExtra(CameraConstants.LABEL_RETRY);
        allowRetry = getIntent().getBooleanExtra(CameraConstants.ALLOW_RETRY, false);
        resultCode = getIntent().getIntExtra(CameraConstants.RESULT_CODE, 0);
        saveDir = getIntent().getStringExtra(CameraConstants.SAVE_DIR);
        successMessage = getIntent().getStringExtra(CameraConstants.SUCCESS_MESSAGE);
        saveType = (SaveType) getIntent().getSerializableExtra(CameraConstants.SAVE_TYPE);

        setOptions();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, CameraConstants.REQUEST_CODE_PERMISSIONS);
        }

        binding.btnTakePicture.setOnClickListener(v -> takePicture());
        binding.btnSavePicture.setOnClickListener(v -> savePicture());
        binding.btnTakeAnother.setOnClickListener(v -> takeAnother());
        binding.btnCamChange.setOnClickListener(v -> changeCam());
        binding.btnFlashConfig.setOnClickListener(v -> changeFlash());
        binding.sldZoomControl.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                changeZoom(value);
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

        verifyCameras();
    }

    private void setOptions() {
        if (controlsColor != 0) {
            binding.btnTakePicture.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
            binding.btnSavePicture.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
            binding.btnTakeAnother.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
            binding.btnSavePicture.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
            binding.btnTakeAnother.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
            binding.btnCamChange.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
            binding.btnFlashConfig.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
            binding.sldZoomControl.setTickTintList(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
            binding.sldZoomControl.setThumbTintList(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
            binding.sldZoomControl.setTrackTintList(ColorStateList.valueOf(ContextCompat.getColor(this, controlsColor)));
        }

        if (labelSave != null) {
            binding.btnSavePicture.setText(labelSave);
        }

        if (labelRetry != null) {
            binding.btnTakeAnother.setText(labelRetry);
        }
    }

    private void changeZoom(float zoom) {
        controller.setZoomRatio(zoom);
    }

    private void verifyCameras() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    binding.btnCamChange.setVisibility(View.INVISIBLE);
                }

                for (CameraInfo info : cameraProvider.getAvailableCameraInfos()) {
                    if (info.hasFlashUnit()) {
                        hasFlashUnit = true;
                    }
                }

                if (!hasFlashUnit) {
                    binding.btnFlashConfig.setVisibility(View.INVISIBLE);
                }
            } catch (ExecutionException | InterruptedException | CameraInfoUnavailableException e) {
                e.printStackTrace();

                binding.btnCamChange.setVisibility(View.INVISIBLE);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void changeCam() {
        CameraSelector cameraSelector;
        CameraSelector selectedCameraSelector = controller.getCameraSelector();

        if (selectedCameraSelector.equals(CameraSelector.DEFAULT_BACK_CAMERA)) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

            binding.btnFlashConfig.setVisibility(View.INVISIBLE);
            binding.btnCamChange.setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_cam_rear));
        } else {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            if (hasFlashUnit) {
                binding.btnFlashConfig.setVisibility(View.VISIBLE);
            }

            binding.btnCamChange.setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_cam_front));
        }

        controller.setCameraSelector(cameraSelector);
    }

    private void changeFlash() {
        int flahsMode = ImageCapture.FLASH_MODE_AUTO;
        int selectedFlahsMode = controller.getImageCaptureFlashMode();

        switch (selectedFlahsMode) {
            case ImageCapture.FLASH_MODE_AUTO:
                flahsMode = ImageCapture.FLASH_MODE_ON;
                binding.btnFlashConfig.setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_flash_on));
                break;
            case ImageCapture.FLASH_MODE_ON:
                flahsMode = ImageCapture.FLASH_MODE_OFF;
                binding.btnFlashConfig.setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_flash_off));
                break;
            case ImageCapture.FLASH_MODE_OFF:
                flahsMode = ImageCapture.FLASH_MODE_AUTO;
                binding.btnFlashConfig.setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_flash_auto));
                break;
        }

        controller.setImageCaptureFlashMode(flahsMode);
    }

    private void savePicture() {
        Intent intent = new Intent();

        if (saveType.equals(SaveType.SAVE_AS_URI)) {
            if (savedUri != null) {
                intent.setDataAndType(savedUri, "image/jpeg");

                setResult(resultCode, intent);
            } else {
                setResult(RESULT_CANCELED);
            }

            this.finish();
        } else {
            if (savedUri != null) {
                if (saveDir == null || saveDir.equals("")) {
                    saveDir = CameraConstants.DEFAULT_DIR;
                }

                if (successMessage == null || successMessage.equals("")) {
                    successMessage = getString(R.string.default_success_message);
                }

                try {
                    CameraUtils.saveFileOnStorage(this, savedUri, saveDir);

                    showShortToast(successMessage);
                } catch (Exception e) {
                    showShortToast(getString(R.string.default_error_message));
                }
            } else {
                showShortToast(getString(R.string.default_error_message));
            }
        }
    }

    private void showShortToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void takeAnother() {
        changeViewTo(ViewType.CAMERA);
    }

    private void takePicture() {
        File file = CameraUtils.makeTempFile(this, null, getString(R.string.temp_img), ".jpg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        controller.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    CameraUtils.clearApplicationTempImgData(MaterialCameraActivity.this, file.getName());

                    final int width = binding.viewImage.getMeasuredWidth();
                    final int height = binding.viewImage.getMeasuredHeight();

                    Bitmap imgPreview = CameraUtils.getRotatedBitmap(
                            Objects.requireNonNull(outputFileResults.getSavedUri()).getPath(), width, height);

                    if (imgPreview != null) {
                        binding.viewImage.setImageBitmap(imgPreview);
                    }

                    savedUri = outputFileResults.getSavedUri();

                    changeViewTo(ViewType.PREVIEW);
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                error.printStackTrace();
            }
        });
    }

    private void startCamera() {
        controller = new LifecycleCameraController(this);
        controller.unbind();

        final Observer<ZoomState> zoomObserver = zoomState -> binding.sldZoomControl.setValue(Math.round(zoomState.getZoomRatio()));
        controller.getZoomState().observe(this, zoomObserver);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        controller.setCameraSelector(cameraSelector);

        controller.setImageCaptureFlashMode(ImageCapture.FLASH_MODE_AUTO);

        controller.bindToLifecycle(this);

        binding.viewFinder.setController(controller);
    }

    private void changeViewTo(ViewType viewType) {
        if (viewType == ViewType.CAMERA) {
            CameraSelector selectedCameraSelector = controller.getCameraSelector();

            if (selectedCameraSelector.equals(CameraSelector.DEFAULT_BACK_CAMERA) && hasFlashUnit) {
                binding.btnFlashConfig.setVisibility(View.VISIBLE);
            }

            binding.viewFinder.setVisibility(View.VISIBLE);
            binding.btnCamChange.setVisibility(View.VISIBLE);
            binding.btnTakePicture.setVisibility(View.VISIBLE);
            binding.sldZoomControl.setVisibility(View.VISIBLE);

            binding.viewImage.setVisibility(View.INVISIBLE);
            binding.btnSavePicture.setVisibility(View.INVISIBLE);
            binding.btnTakeAnother.setVisibility(View.INVISIBLE);
        } else if (viewType == ViewType.PREVIEW) {
            binding.viewFinder.setVisibility(View.INVISIBLE);
            binding.btnCamChange.setVisibility(View.INVISIBLE);
            binding.btnFlashConfig.setVisibility(View.INVISIBLE);
            binding.btnTakePicture.setVisibility(View.INVISIBLE);
            binding.sldZoomControl.setVisibility(View.INVISIBLE);

            binding.viewImage.setVisibility(View.VISIBLE);
            binding.btnSavePicture.setVisibility(View.VISIBLE);

            if (allowRetry) {
                binding.btnTakeAnother.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CameraConstants.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, getString(R.string.default_permission_error_message), Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        setResult(RESULT_CANCELED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cameraExecutor.shutdown();
    }

    @Override
    public void finish() {
        super.finish();

        if (saveType.equals(SaveType.SAVE_TO_DISK)) {
            setResult(RESULT_CANCELED);
        }
    }
}