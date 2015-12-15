package com.genexus.live_editing;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Base64;
import android.view.View;

import com.artech.activities.ActivityHelper;
import com.artech.android.layout.LayoutTag;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.compatibility.SherlockHelper;
import com.artech.controls.ApplicationBarControl;
import com.artech.fragments.ComponentContainer;
import com.artech.fragments.LayoutFragment;
import com.artech.fragments.LayoutFragmentActivity;
import com.artech.utils.Cast;
import com.genexus.live_editing.model.Command;
import com.genexus.live_editing.model.CommandType;
import com.genexus.live_editing.model.ControlData;
import com.genexus.live_editing.model.MasterLayoutData;
import com.genexus.live_editing.model.serializers.MasterLayoutDataSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LiveInspector {
    public static Command captureScreen(Activity activity) {
        if (activity instanceof LayoutFragmentActivity) {
            LayoutFragmentActivity layoutFragmentActivity = (LayoutFragmentActivity) activity;
            if (!layoutFragmentActivity.isPaused()) {
                List<GxControl> controls = getControlsList(layoutFragmentActivity.getMainFragment());
                return LiveInspector.createMasterLayoutCommand(activity, controls);
            }
        }
        return null;
    }

    private static class GxControl {
        private final View mView;
        private final int mLevel;
        private final int mZ;

        public GxControl(View view, int level, int z) {
            mView = view;
            mLevel = level;
            mZ = z;
        }

        public View getView() {
            return mView;
        }

        public int getLevel() {
            return mLevel;
        }

        public int getZ() {
            return mZ;
        }
    }

    private static List<GxControl> getControlsList(LayoutFragment mainFragment) {
        List<GxControl> controlsList = new ArrayList<>();
        populateControlsList(mainFragment, controlsList, 0, 0);
        return controlsList;
    }

    /**
     * The level indicates the depth in the view hierarchy tree.
     * The z indicates which view should be drawn first. It's incremented in a BFS fashion.
     */
    private static void populateControlsList(LayoutFragment layoutFragment, List<GxControl> controlsList, int fragmentLevel, int baseZ) {
        List<View> layoutViews = layoutFragment.getControlViews();
        int layoutViewsCount = layoutViews.size();
        for (int i = 0; i < layoutViewsCount; i++) {
            View view = layoutViews.get(i);
            LayoutItemDefinition controlDefinition = (LayoutItemDefinition) view.getTag(LayoutTag.CONTROL_DEFINITION);
            int absoluteLevel = fragmentLevel + controlDefinition.getLevel();
            int relativeZ = i + 1;
            int z = baseZ + relativeZ;
            GxControl gxControl = new GxControl(view, absoluteLevel, z);
            controlsList.add(gxControl);

            if (view instanceof ComponentContainer) {
                ComponentContainer container = (ComponentContainer) view;
                LayoutFragment fragment = Cast.as(LayoutFragment.class, container.getFragment());
                if (fragment != null) {
                    int nextLevel = absoluteLevel + 1;
                    int nextLevelBaseZ = baseZ + layoutViewsCount;
                    populateControlsList(fragment, controlsList, nextLevel, nextLevelBaseZ);
                }
            }
        }
    }

    private static Command createMasterLayoutCommand(Activity activity, List<GxControl> controls) {
        Rect windowDimensions = ActivityHelper.getWindowDimensions(activity);
        MasterLayoutData masterLayoutData = new MasterLayoutData(
                createControlsDataList(activity, controls),
                Services.Device.pixelsToDips(windowDimensions.height()),
                Services.Device.pixelsToDips(windowDimensions.width())
        );
        Gson gson = new GsonBuilder().
                registerTypeAdapter(MasterLayoutData.class, new MasterLayoutDataSerializer()).
                create();
        Command command = new Command();
        command.setType(CommandType.MASTER_LAYOUT);
        command.setData(gson.toJsonTree(masterLayoutData));
        return command;
    }

    private static List<ControlData> createControlsDataList(Activity activity, List<GxControl> controls) {
        List<ControlData> controlsData = new ArrayList<>();

        for (GxControl control : controls) {
            control.getView().setVisibility(View.INVISIBLE);
        }

        int maxLevel = 0;
        int maxZ = 0;
        for (GxControl control : controls) {
            ControlData controlData = createControlData(control);
            int level = control.getLevel();
            if (level > maxLevel) {
                maxLevel = level;
            }
            int z = control.getZ();
            if (z > maxZ) {
                maxZ = z;
            }
            controlsData.add(controlData);
        }

        // TODO: Include a control that represents the 'Application'? (To show background color, etc)
        // Add a control data for the ActionBar, if the activity has one.
        if (SherlockHelper.hasActionBar(activity)) {
            ControlData appBarControlData = createApplicationBarDataControl(new ApplicationBarControl(activity), maxLevel + 1, maxZ + 1);
            controlsData.add(appBarControlData);
        }

        for (GxControl control : controls) {
            control.getView().setVisibility(View.VISIBLE);
        }

        // Adjust 'y' position to account for the status bar.
        int statusBarHeight = ActivityHelper.getStatusBarHeight(activity);
        for (ControlData controlData : controlsData) {
            controlData.setyPos(controlData.getyPos() - statusBarHeight);
            controlData.setOriginalYPosition(controlData.getOriginalYPosition() - statusBarHeight);
        }

        return controlsData;
    }

    private static ControlData createApplicationBarDataControl(ApplicationBarControl appBar, int appBarLevel, int appBarZ) {
        ControlData controlData = new ControlData();
        controlData.setName(appBar.getName());
        ThemeClassDefinition themeClassDef = appBar.getThemeClass();
        String themeClassName = (appBar.getThemeClass() != null) ? themeClassDef.getName() : Strings.EMPTY;
        controlData.setThemeClassName(themeClassName);
        controlData.setLayoutId(Strings.EMPTY);
        controlData.setParentObjectName(ApplicationBarControl.CONTROL_NAME);
        controlData.setVisible(appBar.isVisible());

        View appBarView = appBar.getView();
        if (appBarView != null) {
            int[] position = new int[2];
            appBarView.getLocationInWindow(position);
            controlData.setxPos(position[0]);
            controlData.setyPos(position[1]);
            controlData.setActualHeight(appBarView.getHeight());
            controlData.setActualWidth(appBarView.getWidth());
            controlData.setEncodedImage(getControlImage(appBarView));
            controlData.setOriginalHeight(appBarView.getHeight());
            controlData.setOriginalYPosition(position[1]);
        }

        controlData.setLevel(appBarLevel);
        controlData.setzPos(appBarZ);

        return controlData;
    }

    private static ControlData createControlData(GxControl control) {
        ControlData controlData = new ControlData();

        View view = control.getView();
        int level = control.getLevel();
        int z = control.getZ();

        String name = (String) view.getTag(LayoutTag.CONTROL_NAME);
        controlData.setName(name);

        ThemeClassDefinition controlThemeClassDef = (ThemeClassDefinition) view.getTag(LayoutTag.CONTROL_THEME_CLASS);
        String themeClassName = (controlThemeClassDef != null) ? controlThemeClassDef.getName() : Strings.EMPTY;
        controlData.setThemeClassName(themeClassName);

        LayoutItemDefinition controlDefinition = (LayoutItemDefinition) view.getTag(LayoutTag.CONTROL_DEFINITION);
        if (controlDefinition != null) {
            LayoutDefinition layoutDefinition = controlDefinition.getLayout();
            controlData.setLayoutId(layoutDefinition.getId());
            controlData.setParentObjectName(layoutDefinition.getParent().getPattern().getName());
            controlData.setVisible(controlDefinition.isVisible());
            controlData.setLevel(level);
        } else {
            controlData.setLayoutId(Strings.EMPTY);
            controlData.setParentObjectName(Strings.EMPTY);
            controlData.setVisible(true);
        }

        int[] position = new int[2];
        view.getLocationInWindow(position);
        controlData.setxPos(position[0]);
        controlData.setyPos(position[1]);
        controlData.setzPos(z);
        controlData.setActualHeight(view.getHeight());
        controlData.setActualWidth(view.getWidth());
        controlData.setEncodedImage(getControlImage(view));

        // TODO: These are intended to be used to display the auto-grow.
        controlData.setOriginalHeight(controlData.getActualHeight());
        controlData.setOriginalYPosition(controlData.getyPos());

        return controlData;
    }

    /**
     * Returns the view's bitmap, compressed to PNG and encoded in Base64.
     */
    private static String getControlImage(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.setVisibility(View.VISIBLE);
        view.draw(canvas);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
        IOUtils.closeQuietly(byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP);
    }
}
