package com.genexus.live_editing;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.artech.activities.ActivityHelper;
import com.artech.activities.dashboard.DashboardActivity;
import com.artech.android.layout.GxTheme;
import com.artech.base.metadata.ILayoutDefinition;
import com.artech.base.metadata.theme.ThemeApplicationBarClassDefinition;
import com.artech.base.metadata.theme.ThemeApplicationClassDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.metadata.theme.ThemeDefinition;
import com.artech.base.utils.PlatformHelper;
import com.artech.controls.IGxLocalizable;
import com.artech.controls.IGxThemeable;
import com.artech.fragments.LayoutFragmentActivity;
import com.genexus.live_editing.model.Command;
import com.genexus.live_editing.model.CommandType;
import com.genexus.live_editing.util.ControlsUtils;
import com.genexus.live_editing.util.Intents;

public class ActivityCommandReceiver extends BroadcastReceiver {
    private final Activity mActivity;
    private final LocalHttpServer mLocalHttpServer;

    public ActivityCommandReceiver(Activity activity, LocalHttpServer localHttpServer) {
        mActivity = activity;
        mLocalHttpServer = localHttpServer;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        CommandType type = (CommandType) intent.getSerializableExtra(Intents.EXTRA_COMMAND_TYPE);
        switch (type) {
            case THEME_STYLE_CHANGED: {
                String oldThemeClassName = intent.getStringExtra(Intents.EXTRA_OLD_THEME_CLASS_NAME);
                String newThemeClassName = intent.getStringExtra(Intents.EXTRA_NEW_THEME_CLASS_NAME);
                applyThemeClassChange(mActivity, oldThemeClassName, newThemeClassName);
                break;
            }
            case THEME_TRANSFORMATION_CHANGED: {
                String transformationName = intent.getStringExtra(Intents.EXTRA_TRANSFORMATION_NAME);
                applyThemeTransformationChange(mActivity, transformationName);
                break;
            }
            case TRANSLATION_CHANGED: {
                applyTranslationChange(mActivity);
                break;
            }
            case THEME_COLOR_CHANGED: {
                applyThemeColorChange(mActivity);
                break;
            }
            case IMAGE_CHANGED: {
                String imageName = intent.getStringExtra(Intents.EXTRA_IMAGE_NAME);
                applyImageChange(mActivity, imageName);
                break;
            }
            case LAYOUT_CHANGED: {
                applyLayoutChange(mActivity);
                break;
            }
            case INSPECT_UI: {
                break;
            }
        }
        Command command = LiveInspector.captureScreen(mActivity);
        if (command != null) {
            mLocalHttpServer.send(command);
        }
    }

    private void applyThemeClassChange(Activity activity, String oldThemeClassName, String newThemeClassName) {
        ThemeDefinition currentTheme = PlatformHelper.getTheme();
        ThemeClassDefinition newThemeClassDefinition = currentTheme.getClass(newThemeClassName);
        String rootThemeClassName = newThemeClassDefinition.getRootName();

        if (ThemeApplicationClassDefinition.CLASS_NAME.equals(rootThemeClassName) ||
                ThemeApplicationBarClassDefinition.CLASS_NAME.equals(rootThemeClassName)) {
            applyGlobalThemeClassChanges(activity);
        } else {
            for (IGxThemeable control : ControlsUtils.getControlsWithThemeClassName(activity, oldThemeClassName)) {
                GxTheme.applyStyle(control, newThemeClassDefinition);
            }
        }
    }

    private void applyThemeTransformationChange(Activity activity, String transformationName) {
        // Apply transformation changes to each control that has a theme class that uses this transformation.
        for (IGxThemeable control : ControlsUtils.getControlsWithTransformationName(activity, transformationName)) {
            // Re-apply the control's theme class definition in order to re-apply its associated transformation.
            GxTheme.applyStyle(control, control.getThemeClass(), true);
        }
    }

    private void applyTranslationChange(Activity activity) {
        // Activity (to change the action bar title)
        if (activity instanceof IGxLocalizable) {
            ((IGxLocalizable) activity).onTranslationChanged();
        }

        // Individual controls
        for (IGxLocalizable control : ControlsUtils.getLocalizableControls(activity)) {
            control.onTranslationChanged();
        }
    }

    private void applyThemeColorChange(Activity activity) {
        applyGlobalThemeClassChanges(activity);

        ThemeDefinition currentTheme = PlatformHelper.getTheme();
        for (IGxThemeable control : ControlsUtils.getControlsWithThemeClass(activity)) {
            String themeClassName = control.getThemeClass().getName();
            ThemeClassDefinition newDefinition = currentTheme.getClass(themeClassName);
            GxTheme.applyStyle(control, newDefinition);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void applyImageChange(Activity activity, String imageName) {
        // TODO: Refrescar únicamente las imagenes que cambiaron.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            activity.recreate();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void applyLayoutChange(Activity activity) {
        // TODO: Únicamente refrescar la activity si el layout que cambió se encuentra en alguno de los layouts de esta activity.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            activity.recreate();
        }
    }

    private void applyGlobalThemeClassChanges(Activity activity) {
        ILayoutDefinition layoutDefinition = null;
        if (activity instanceof LayoutFragmentActivity) {
            layoutDefinition = ((LayoutFragmentActivity) activity).getMainLayout();
        } else if (activity instanceof DashboardActivity) {
            layoutDefinition = (ILayoutDefinition) ((DashboardActivity) activity).getDashboardDefinition();
        }
        if (layoutDefinition != null) {
            ActivityHelper.applyStyle(activity, layoutDefinition);
        }
    }
}
