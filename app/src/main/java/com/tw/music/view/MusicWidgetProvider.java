package com.tw.music.view;

import java.util.Locale;

import com.tw.music.MusicActivity;
import com.tw.music.MusicService;
import com.tw.music.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.RemoteViews;

public class MusicWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "MusicWidgetProvider";

    private static MusicWidgetProvider sInstance;

    public static synchronized MusicWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new MusicWidgetProvider();
        }
        return sInstance;
    }

    public void notifyChange(MusicService service) {
    	if (hasInstances(service)) {
    		performUpdate(service, null);
    	}
	}

    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }

	public void performUpdate(MusicService service, int[] appWidgetIds) {
    	final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.music_widget); 

        CharSequence titleName = service.getTrackName();
        CharSequence artistName = service.getArtistName();

        if(titleName == null) {
        	titleName = service.getFileName();
        	if(titleName == null) {
        		service.getString(R.string.unknown);
        	}
        }
        views.setTextViewText(R.id.title, titleName);
        if(artistName == null) {
        	titleName = service.getString(R.string.unknown);
        }
        views.setTextViewText(R.id.artist, artistName);

        int ctime = (int)service.getCurrentPosition();
		int ttime = (int)service.getDuration();
		if(ctime < 0) {
			ctime = 0;
		}
		if(ttime <= 0) {
			ttime = 0;
		}
		int currenttime = ctime / 1000;
		int scurrenttime = currenttime;
		int mcurrenttime = scurrenttime / 60;
		int hcurrenttime = mcurrenttime / 60;
		scurrenttime %= 60;
		mcurrenttime %= 60;
		hcurrenttime %= 24;
		if(hcurrenttime == 0) {
			views.setTextViewText(R.id.currenttime, String.format(Locale.US, "%d:%02d", mcurrenttime, scurrenttime));
		} else {
			views.setTextViewText(R.id.currenttime, String.format(Locale.US, "%d:%02d:%02d", hcurrenttime, mcurrenttime, scurrenttime));
		}
		int totaltime = ttime / 1000;
		int stotaltime = totaltime;
		int mtotaltime = stotaltime / 60;
		int htotaltime = mtotaltime / 60;
		stotaltime %= 60;
		mtotaltime %= 60;
		htotaltime %= 24;
		if(htotaltime == 0) {
			views.setTextViewText(R.id.totaltime, String.format(Locale.US, "%d:%02d", mtotaltime, stotaltime));
		} else {
			views.setTextViewText(R.id.totaltime, String.format(Locale.US, "%d:%02d:%02d", htotaltime, mtotaltime, stotaltime));
		}
		views.setProgressBar(R.id.progress, totaltime, currenttime, false);
		Bitmap bm = service.getAlbumArt();
		if(bm == null) {
			views.setImageViewResource(R.id.albumart, R.mipmap.album);
		} else {
			views.setImageViewBitmap(R.id.albumart, bm);
		}

		linkButtons(service, views);
        pushUpdate(service, appWidgetIds, views);
    }

	private void linkButtons(Context context, RemoteViews views) {
        Intent intent;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(context, MusicService.class);
        intent = new Intent(context, MusicActivity.class);
        pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.albumart, pendingIntent);
		intent = new Intent(MusicService.ACTIONPREV);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_prev, pendingIntent);
		intent = new Intent(MusicService.ACTIONPP);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);
		intent = new Intent(MusicService.ACTIONNEXT);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
	}

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        try {
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
        } catch (Exception e) {
        }
    }

    @Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
    	defaultAppWidget(context, appWidgetIds);
    	context.startService(new Intent(context, MusicService.class));
    	Intent updateIntent = new Intent(MusicService.ACTIONCMD);
    	updateIntent.putExtra("cmd", MusicService.CMDUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendStickyBroadcast(updateIntent);
	}

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.music_widget);

        linkButtons(context, views);
        pushUpdate(context, appWidgetIds, views);
    }
}
