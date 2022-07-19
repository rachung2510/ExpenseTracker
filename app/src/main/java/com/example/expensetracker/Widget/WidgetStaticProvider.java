package com.example.expensetracker.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.example.expensetracker.Account;
import com.example.expensetracker.Category;
import com.example.expensetracker.Constants;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

import java.util.Calendar;

public class WidgetStaticProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetStaticProvider";

    public static final String KEY = "ACTION";
    public static final int EDIT_AMOUNT = 1;
    public static final int EDIT_DESCRIPTION = 2;
    public static final int EDIT_ACCOUNT = 3;
    public static final int EDIT_CATEGORY = 4;
    public static final int EDIT_DATE = 5;
    public static final int SAVE = 6;
    public static final int UPDATE = 7;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // clear stored values
        SharedPreferences pref = context.getSharedPreferences(Constants.TMP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(WidgetStaticActivity.KEY_AMT);
        editor.remove(WidgetStaticActivity.KEY_DESC);
        editor.remove(WidgetStaticActivity.KEY_ACC);
        editor.remove(WidgetStaticActivity.KEY_CAT);
        editor.remove(WidgetStaticActivity.KEY_DATE);
        editor.apply();

        // set interactions
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_static);
        views.setOnClickPendingIntent(R.id.newExpAmt, getPendingIntent(context, EDIT_AMOUNT));
        views.setOnClickPendingIntent(R.id.newExpDesc, getPendingIntent(context, EDIT_DESCRIPTION));
        views.setOnClickPendingIntent(R.id.newExpAccBox, getPendingIntent(context, EDIT_ACCOUNT));
        views.setOnClickPendingIntent(R.id.newExpCatBox, getPendingIntent(context, EDIT_CATEGORY));
        views.setOnClickPendingIntent(R.id.newExpDate, getPendingIntent(context, EDIT_DATE));
        views.setOnClickPendingIntent(R.id.newExpSave, getPendingIntent(context, SAVE));
        views.setOnClickPendingIntent(R.id.update, getPendingSelfIntent(context));

        // configure default values
        DatabaseHelper db = new DatabaseHelper(context);
        Account acc = db.getAccount(db.getDefaultAccName());
        Category cat = db.getCategory(db.getDefaultCatName());
        views.setTextViewText(R.id.newExpAccName, acc.getName());
        views.setTextViewText(R.id.newExpCatName, cat.getName());
        views.setInt(R.id.newExpAccBox, "setBackgroundColor", MainActivity.getColorFromHex(acc.getColorHex()));
        views.setInt(R.id.newExpCatBox, "setBackgroundColor", MainActivity.getColorFromHex(cat.getColorHex()));
        views.setTextViewText(R.id.newExpCurrency, acc.getCurrencySymbol());
        views.setTextViewText(R.id.newExpAmt, "");
        views.setTextViewText(R.id.newExpDesc, "");
        Calendar cal = Calendar.getInstance(MainActivity.locale);
        views.setTextViewText(R.id.expDate, (MainActivity.getRelativePrefix(cal) + ", " + MainActivity.getDatetimeStr(cal, "dd MMMM yyyy")).toUpperCase());

        appWidgetManager.updateAppWidget(appWidgetId, views);
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
