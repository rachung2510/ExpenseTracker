package com.example.expensetracker.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.SizeF;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

import java.util.Calendar;
import java.util.Map;

public class WidgetStaticProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetStaticProvider";

    private RemoteViews remoteViews;

    public static final String KEY = "ACTION";
    public static final int EDIT_AMOUNT = 1;
    public static final int EDIT_DESCRIPTION = 2;
    public static final int EDIT_ACCOUNT = 3;
    public static final int EDIT_CATEGORY = 4;
    public static final int EDIT_DATE = 5;
    public static final int SAVE = 6;
    public static final int UPDATE = 7;
    public static final int SCAN_RECEIPT = 8;
    public static final int FAVOURITES = 9;

    /**
     * Return remote views for different widget sizes, and also the default view
     * @param context
     * @return tuple containing dictionary of remote views with sizes as keys, and default view
     */
    public static Pair<Map<SizeF, RemoteViews>, RemoteViews> getResizedViews(Context context) {
        RemoteViews defaultView = new RemoteViews(context.getPackageName(), R.layout.widget_static);
        RemoteViews tallView = new RemoteViews(context.getPackageName(), R.layout.widget_static_tall);
        Map<SizeF, RemoteViews> viewMapping = new ArrayMap<>();
        viewMapping.put(new SizeF(250f, 240f), defaultView);
        viewMapping.put(new SizeF(250f, 360f), tallView);
        return new Pair<> (viewMapping, defaultView);
    }

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // clear stored values
        SharedPreferences pref = context.getSharedPreferences(Constants.TMP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(WidgetStaticActivity.KEY_AMT);
        editor.remove(WidgetStaticActivity.KEY_DESC);
        editor.remove(WidgetStaticActivity.KEY_ACC);
        editor.remove(WidgetStaticActivity.KEY_CAT);
        editor.remove(WidgetStaticActivity.KEY_DATE);
        editor.remove(WidgetStaticActivity.KEY_ADAPTER_LIST);
        editor.remove(WidgetStaticActivity.KEY_ADAPTER_CURR);
        editor.apply();

        // set interactions
        Pair<Map<SizeF, RemoteViews>, RemoteViews> pair = getResizedViews(context);
        Map<SizeF, RemoteViews> viewMapping = pair.first;
        RemoteViews defaultView = pair.second;

        DatabaseHelper db = new DatabaseHelper(context);
        Account acc = db.getAccount(db.getDefaultAccName());
        for (Map.Entry<SizeF, RemoteViews> entry : viewMapping.entrySet()) {
            RemoteViews view = entry.getValue();
            view.setOnClickPendingIntent(R.id.newExpAmt, getPendingIntent(context, EDIT_AMOUNT));
            view.setOnClickPendingIntent(R.id.newExpDesc, getPendingIntent(context, EDIT_DESCRIPTION));
            view.setOnClickPendingIntent(R.id.newExpAccBox, getPendingIntent(context, EDIT_ACCOUNT));
            view.setOnClickPendingIntent(R.id.newExpCatBox, getPendingIntent(context, EDIT_CATEGORY));
            view.setOnClickPendingIntent(R.id.newExpDate, getPendingIntent(context, EDIT_DATE));
            view.setOnClickPendingIntent(R.id.newExpSave, getPendingIntent(context, SAVE));
            view.setOnClickPendingIntent(R.id.update, getPendingSelfIntent(context));
            view.setOnClickPendingIntent(R.id.scanReceiptBtn, getPendingIntent(context, SCAN_RECEIPT));
            view.setOnClickPendingIntent(R.id.favouritesBtn, getPendingIntent(context, FAVOURITES));

            // configure default values
            Category cat = db.getCategory(db.getDefaultCatName());
            view.setTextViewText(R.id.newExpAccName, acc.getName());
            view.setTextViewText(R.id.newExpCatName, cat.getName());
            view.setImageViewBitmap(R.id.newExpAccIcon, MainActivity.drawableToBitmap(MainActivity.getIconFromId(context, R.drawable.shape_rounded_top_left_rectangle)));
            view.setInt(R.id.newExpAccBox, "setColorFilter", acc.getColor());
            view.setImageViewBitmap(R.id.newExpCatIcon, MainActivity.drawableToBitmap(MainActivity.getIconFromId(context, R.drawable.shape_rounded_top_right_rectangle)));
            view.setInt(R.id.newExpCatBox, "setColorFilter", cat.getColor());
            view.setTextViewText(R.id.newExpCurrency, acc.getCurrencySymbol());
            view.setTextViewText(R.id.newExpAmt, "");
            view.setTextViewText(R.id.newExpDesc, "");
            Calendar cal = Calendar.getInstance(MainActivity.locale);
            view.setTextViewText(R.id.expDate, (MainActivity.getRelativePrefix(cal) + ", " + MainActivity.getDatetimeStr(cal, "dd MMMM yyyy")).toUpperCase());
            view.setImageViewBitmap(R.id.newExpAccIcon, MainActivity.drawableToBitmap(acc.getIcon()));
            view.setInt(R.id.newExpAccIcon,"setColorFilter", acc.getColor());
            view.setImageViewBitmap(R.id.newExpCatIcon, MainActivity.drawableToBitmap(cat.getIcon()));
            view.setInt(R.id.newExpCatIcon,"setColorFilter", cat.getColor());
            view.setImageViewBitmap(R.id.scanReceiptBtn, MainActivity.drawableToBitmap(MainActivity.getIconFromId(context, R.drawable.ic_baseline_camera_alt_24)));
            view.setImageViewBitmap(R.id.favouritesBtn, MainActivity.drawableToBitmap(MainActivity.getIconFromId(context, R.drawable.ic_baseline_star_outline_24)));
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            remoteViews = new RemoteViews(viewMapping);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        } else {
            remoteViews = defaultView;
            appWidgetManager.updateAppWidget(appWidgetId, defaultView);
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        onUpdate(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    @Override
    public void onEnabled(Context context) {}
    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static PendingIntent getPendingIntent(Context context, int key) {
        Intent intent = new Intent(context, WidgetStaticActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(KEY, key);
        return PendingIntent.getActivity(context, key, intent, PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent getPendingSelfIntent(Context context) {
        Intent intent = new Intent(context, WidgetStaticProvider.class);
        return PendingIntent.getBroadcast(context, UPDATE, intent, PendingIntent.FLAG_IMMUTABLE);
    }
    private void onUpdate(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, WidgetStaticProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

}
