package com.artech.android.media.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.artech.R;
import com.artech.base.services.Services;

public class VideoUtils {
    private static final Pattern sYoutubeUrlPattern = Pattern.compile(
            "https?:\\/\\/(?:[0-9A-Z-]+\\.)?(?:youtu\\.be\\/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*",
            Pattern.CASE_INSENSITIVE);

    public static boolean isYouTubeUrl(String url) {
        if (url == null) {
            return false;
        }

        return sYoutubeUrlPattern.matcher(url).matches();
    }

    public static String getYouTubeVideoId(@NonNull String url) {
        Matcher matcher = sYoutubeUrlPattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static boolean openVideoIntent(final Context context, Uri videoUri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(videoUri, "video/*");

        List<ResolveInfo> appsList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        boolean videoPlayersAvailable = appsList.size() > 0;

        if (videoPlayersAvailable) {
            new AlertDialog.Builder(context)
                    .setTitle(Services.Strings.getResource(R.string.GXM_VideoErrorTitle))
                    .setMessage(Services.Strings.getResource(R.string.GXM_VideoErrorMsg))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            context.startActivity(intent);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }

        return videoPlayersAvailable;
    }
}
