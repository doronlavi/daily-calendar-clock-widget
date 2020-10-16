/*
Round Calendar
Copyright (C) 2020 Mil Tolstoy <miltolstoy@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.miltolstoy.roundcalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Calendar;

import static com.miltolstoy.roundcalendar.Logging.TAG;


public class WidgetProvider extends AppWidgetProvider {

    private static final String previousDayAction = "previousDayAction";
    private static final String nextDayAction = "nextDayAction";
    private static final String todayAction = "todayAction";
    private static final String tickAction = "com.miltolstoy.roundcalendar.clockTickAction";

    private static int daysShift = 0;

    @Override
    public void onEnabled(Context context) {
        setupNextClockTick(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.d(TAG, "Empty action");
            return;
        }

        if (action.equals(tickAction)) {
            setupNextClockTick(context);
            Intent updateIntent = new Intent(context, WidgetProvider.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(updateIntent);
            super.onReceive(context, intent);
            return;
        }

        if (!action.equals(previousDayAction) && !action.equals(nextDayAction) && !action.equals(todayAction)
                && !action.equals(AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED)) {
            Log.d(TAG, "Unhandled action: " + action);
            super.onReceive(context, intent);
            return;
        }

        if (action.equals(previousDayAction)) {
            daysShift -= 1;
        } else if (action.equals(nextDayAction)) {
            daysShift += 1;
        } else {
            daysShift = 0;
        }

        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        drawAndUpdate(context, widgetId);

        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            drawAndUpdate(context, id);
            setOnClickButtonsIntents(context, id);
        }
    }

    private static void setOnClickIntent(Context context, RemoteViews views, int widgetId, int viewId,
                                         String intentAction) {
        Intent intent = new Intent(context, WidgetProvider.class);
        intent.setAction(intentAction);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(viewId, pendingIntent);
    }

    private static void drawAndUpdate(Context context, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        Point widgetSize = WidgetConfigurationActivity.getWidgetSize(appWidgetManager, widgetId);
        WidgetConfigurationActivity.drawWidget(context, views, widgetSize, daysShift, CalendarAdapter.CALENDAR_EMPTY_ID);
        appWidgetManager.updateAppWidget(widgetId, views);
    }

    private static void setOnClickButtonsIntents(Context context, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        setOnClickIntent(context, views, widgetId, R.id.previous_button, previousDayAction);
        setOnClickIntent(context, views, widgetId, R.id.next_button, nextDayAction);
        setOnClickIntent(context, views, widgetId, R.id.today_button, todayAction);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(widgetId, views);
    }


    private void setupNextClockTick(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        final int tickPeriodMillisecond = 1000; // Intents are missed sometimes, so just bombing app with them.
        calendar.add(Calendar.MILLISECOND, tickPeriodMillisecond);
        Intent tickIntent = new Intent(context, WidgetProvider.class);
        tickIntent.setAction(tickAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, tickIntent, 0);
        alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }
}
