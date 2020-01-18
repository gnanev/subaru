package com.gran.subaru;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class SubaruWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.subaru_widget);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        runSubaruApp(context);

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    void runSubaruApp(final Context context) {
        Log.d("xxx", "runSubaruApp");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000); // you have 2 sec to place widget
                    PackageManager pm = context.getPackageManager();
                    Intent intent = pm.getLaunchIntentForPackage("com.gran.subaru");

                    if (intent != null) {
                        intent.putExtra("FromWidget", 42);
                        context.startActivity(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).run();
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }
}
