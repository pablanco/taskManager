package com.genexus.live_editing;

import android.support.annotation.NonNull;

import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.images.ImageCatalog;
import com.artech.base.metadata.images.ImageFile;
import com.artech.base.metadata.languages.Language;
import com.artech.base.metadata.loader.ApplicationLoader;
import com.artech.base.metadata.loader.WorkWithMetadataLoader;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.metadata.theme.ThemeDefinition;
import com.artech.base.metadata.theme.TransformationDefinition;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class MetadataHelper {
    public static void addThemeClass(@NonNull ThemeDefinition theme, ThemeClassDefinition parentDefinition,
                                     @NonNull INodeObject newMetadata) {
        ThemeClassDefinition newDefinition = ApplicationLoader.readOneStyleAndChilds(theme, parentDefinition, newMetadata);
        theme.putClass(newDefinition);
    }

    public static void replaceThemeClass(@NonNull ThemeDefinition theme, ThemeClassDefinition parentDefinition,
                                         @NonNull String previousThemeClassName, @NonNull INodeObject newMetadata) {
        theme.removeClass(previousThemeClassName);
        addThemeClass(theme, parentDefinition, newMetadata);
    }

    public static void replaceTransformation(@NonNull ThemeDefinition theme, @NonNull String transformationName,
                                             @NonNull INodeObject newMetadata) {
        theme.removeTransformation(transformationName);
        TransformationDefinition newDefinition = new TransformationDefinition(newMetadata);
        theme.putTransformation(newDefinition);
    }

    public static void replaceTranslation(@NonNull Language currentLanguage, @NonNull INodeObject newMetadata) {
        String message = newMetadata.getString("M");
        String translation = newMetadata.getString("T");

        if (Strings.hasValue(translation)) {
            currentLanguage.add(message, translation);
        } else {
            currentLanguage.remove(message);
        }
    }

    public static void replaceImage(@NonNull String imageName, @NonNull String imageUrl) {
        ImageCatalog imageCatalog = Services.Application.getDefinition().getImageCatalog();
        ImageFile imageFile = imageCatalog.getImage(imageName);
        if (imageFile != null) {
            imageFile.setUri(imageUrl);
        }
    }

    public static void replacePattern(@NonNull String objName, @NonNull INodeObject newMetadata) {
        WorkWithMetadataLoader wwLoader = new WorkWithMetadataLoader();
        WorkWithDefinition newDefinition = wwLoader.loadJSON(newMetadata);
        if (newDefinition != null) {
            newDefinition.setName(objName);
            Services.Application.putPattern(newDefinition, wwLoader, null);
        }
    }
}
