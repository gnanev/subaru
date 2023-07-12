package com.gran.subaru;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    public final static String RUN_FROM_BOOT = "IsFirstRunFromBoot";

    @Override
    public void onReceive(Context context, Intent intent) {

        //Toast.makeText(context, "!!! BOOT !!!", Toast.LENGTH_LONG).show();

        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(RUN_FROM_BOOT, true);
        context.startActivity(i);
    }
}
