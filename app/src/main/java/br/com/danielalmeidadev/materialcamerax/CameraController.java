package br.com.danielalmeidadev.materialcamerax;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.ColorRes;
import androidx.fragment.app.Fragment;

public class CameraController {

    @ColorRes
    protected int controlsColor = 0;
    protected String labelSave = null;
    protected String labelRetry = null;
    protected boolean allowRetry = false;
    protected String saveDir = "";
    protected String successMessage = "";
    protected SaveType saveType = SaveType.SAVE_AS_URI;

    private Intent getIntent(Context context, int resultCode) {
        Intent intent = new Intent(context, MaterialCameraActivity.class);
        intent.putExtra(CameraConstants.RESULT_CODE, resultCode);
        intent.putExtra(CameraConstants.ALLOW_RETRY, allowRetry);
        intent.putExtra(CameraConstants.LABEL_SAVE, labelSave);
        intent.putExtra(CameraConstants.LABEL_RETRY, labelRetry);
        intent.putExtra(CameraConstants.CONTROLS_COLOR, controlsColor);
        intent.putExtra(CameraConstants.SAVE_DIR, saveDir);
        intent.putExtra(CameraConstants.SUCCESS_MESSAGE, successMessage);
        intent.putExtra(CameraConstants.SAVE_TYPE, saveType);

        return intent;
    }

    public void startWith(Activity activity, ActivityResultLauncher<Intent> resultLauncher, int resultCode) {
        resultLauncher.launch(getIntent(activity, resultCode));
    }

    public void startWith(Fragment fragment, ActivityResultLauncher<Intent> resultLauncher, int resultCode) {
        resultLauncher.launch(getIntent(fragment.requireActivity(), resultCode));
    }
}
