package com.genexus.live_editing;

import java.util.concurrent.LinkedBlockingQueue;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.artech.application.MyApplication;
import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.base.metadata.languages.Language;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.metadata.theme.ThemeDefinition;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.fedorvlasov.lazylist.ImageLoader;
import com.genexus.live_editing.model.Command;
import com.genexus.live_editing.model.CommandType;
import com.genexus.live_editing.util.Intents;
import com.squareup.okhttp.HttpUrl;

public class CommandExecutor extends Thread {
    private LinkedBlockingQueue<Command> mCommandsQueue;
    private boolean mStop = false;
    private HttpUrl mLiveEditingUrl;

    public CommandExecutor(HttpUrl liveEditingUrl) {
        mCommandsQueue = new LinkedBlockingQueue<>();
        mLiveEditingUrl = liveEditingUrl;
    }

    @Override
    public void run() {
        while (!mStop) {
            try {
                Command command = mCommandsQueue.take();
                if (command != null) {
                    executeCommand(command);
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public void enqueue(Command command) {
        if (command == null) {
            return;
        }
        try {
            mCommandsQueue.put(command);
        } catch (InterruptedException e) {
        }
    }

    public void shutdown() {
        mStop = true;
        // Wake up the wake in case it's blocked.
        Command command = new Command();
        command.setType(CommandType.NO_OP);
        enqueue(command);
    }

    /**
     * Execute a command according its type.
     */
    private void executeCommand(Command command) {
        switch (command.getType()) {
            case THEME_STYLE_CHANGED:
                onThemeStyleChanged(command);
                break;
            case THEME_TRANSFORMATION_CHANGED:
                onThemeTransformationChanged(command);
                break;
            case TRANSLATION_CHANGED:
                onTranslationChanged(command);
                break;
            case THEME_COLOR_CHANGED:
                onThemeColorChanged(command);
                break;
            case IMAGE_CHANGED:
                onImageChanged(command);
                break;
            case LAYOUT_CHANGED:
                onLayoutChanged(command);
                break;
            case INSPECT_UI:
                onInspectUI();
                break;
            case NO_OP:
        }
    }

    private void onThemeStyleChanged(Command command) {
        String themeName = command.getObjName();
        ThemeDefinition currentTheme = PlatformHelper.getTheme();

        if (themeName != null && themeName.equals(currentTheme.getName())) {
            String className = command.getStyleName();
            String parentClassName = command.getParent();
            INodeObject newMetadata = (INodeObject) command.getData();
            String newClassName = newMetadata.getString("Name");

            ThemeClassDefinition classDef = currentTheme.getClass(className);
            ThemeClassDefinition parentClassDef = Strings.hasValue(parentClassName) ? currentTheme.getClass(parentClassName) : null;
            // New class has been added
            if (classDef == null) {
                // No new root classes allowed
                if (parentClassDef != null) {
                    MetadataHelper.addThemeClass(currentTheme, parentClassDef, newMetadata);
                    notifyMetadataChanged(CommandType.THEME_STYLE_CHANGED, new String[] {className, newClassName});
                }
                // Existent class has been modified
            } else {
                MetadataHelper.replaceThemeClass(currentTheme, parentClassDef, className, newMetadata);
                notifyMetadataChanged(CommandType.THEME_STYLE_CHANGED, new String[] {className, newClassName});
            }
        }
    }

    private void onThemeTransformationChanged(Command command) {
        String themeName = command.getObjName();
        ThemeDefinition currentTheme = PlatformHelper.getTheme();

        if (themeName != null && themeName.equals(currentTheme.getName())) {
            String transformationName = command.getTransformName();
            INodeObject newMetadata = (INodeObject) command.getData();
            MetadataHelper.replaceTransformation(currentTheme, transformationName, newMetadata);
            notifyMetadataChanged(CommandType.THEME_TRANSFORMATION_CHANGED, new String[] {transformationName});
        }
    }

    private void onTranslationChanged(Command command) {
        String languageName = command.getObjName();
        Language currentLanguage = Services.Application.getDefinition().getLanguageCatalog().
                getCurrentLanguage();

        if (languageName != null && languageName.equals(currentLanguage.getName())) {
            INodeObject newMetadata = (INodeObject) command.getData();
            MetadataHelper.replaceTranslation(currentLanguage, newMetadata);
            notifyMetadataChanged(CommandType.TRANSLATION_CHANGED, null);
        }
    }

    private void onThemeColorChanged(Command command) {
        String themeName = command.getObjName();
        ThemeDefinition currentTheme = PlatformHelper.getTheme();

        if (themeName != null && themeName.equals(currentTheme.getName())) {
            INodeCollection newClassesMetadata = (INodeCollection) command.getData();
            for (INodeObject newMetadata : newClassesMetadata) {
                String className = newMetadata.getString("Name");
                if (Strings.hasValue(className)) {
                    ThemeClassDefinition parentDefinition = currentTheme.getClass(className).getParentClass();
                    MetadataHelper.replaceThemeClass(currentTheme, parentDefinition, className, newMetadata);
                }
            }
            notifyMetadataChanged(CommandType.THEME_COLOR_CHANGED, null);
        }
    }

    private void onImageChanged(Command command) {
        String imageName = command.getObjName();

        String themeName = PlatformHelper.getTheme().getName();
        String languageName = Services.Application.getDefinition().getLanguageCatalog().getCurrentLanguage().getName();

        String query = mLiveEditingUrl.query()
                .concat("," + themeName)
                .concat("," + languageName)
                .concat("," + imageName);
        String imageUrl = mLiveEditingUrl
                .newBuilder()
                .addPathSegment("getimage")
                .addPathSegment("")
                .query(query)
                .toString();

        MetadataHelper.replaceImage(imageName, imageUrl);
        ImageLoader.clearCache();

        notifyMetadataChanged(CommandType.IMAGE_CHANGED, new String[] {imageName});
    }

    private void onLayoutChanged(Command command) {
        String objType = command.getObjType();
        String objName = command.getObjName();

        if (GxObjectTypes.WorkWithGuid.equals(objType) && objName != null) {
            INodeObject newMetadata = (INodeObject) command.getData();
            MetadataHelper.replacePattern(objName, newMetadata);
            notifyMetadataChanged(CommandType.LAYOUT_CHANGED, null);
        }
    }

    private void onInspectUI() {
        Intent intent = new Intent(Intents.EVENT_COMMAND_RECEIVED); // TODO: Change to EVENT_INSPECT_UI
        intent.putExtra(Intents.EXTRA_COMMAND_TYPE, CommandType.INSPECT_UI);
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).sendBroadcast(intent);
    }

    /**
     * Send a local broadcast with the corresponding intent.
     */
    private void notifyMetadataChanged(CommandType type, String[] extras) {
        Intent intent = new Intent(Intents.EVENT_COMMAND_RECEIVED); // TODO: Change to EVENT_METADATA_CHANGED
        intent.putExtra(Intents.EXTRA_COMMAND_TYPE, type);
        if (CommandType.THEME_STYLE_CHANGED.equals(type)) {
            intent.putExtra(Intents.EXTRA_OLD_THEME_CLASS_NAME, extras[0]);
            intent.putExtra(Intents.EXTRA_NEW_THEME_CLASS_NAME, extras[1]);
        } else if (CommandType.THEME_TRANSFORMATION_CHANGED.equals(type)) {
            intent.putExtra(Intents.EXTRA_TRANSFORMATION_NAME, extras[0]);
        } else if (CommandType.IMAGE_CHANGED.equals(type)) {
            intent.putExtra(Intents.EXTRA_IMAGE_NAME, extras[0]);
        }

        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).sendBroadcast(intent);
    }
}
