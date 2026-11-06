package br.com.widgetpoco;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.IBinder;

/** Keeps connectivity callbacks alive while a home-screen widget is installed. No polling. */
public class ConnectionService extends Service {
    static boolean hasWidgets(Context c){
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,DataWidget.class)).length>0;
    }
    static void ensure(Context c){
        if(!hasWidgets(c))return;
        try{c.startForegroundService(new Intent(c,ConnectionService.class));}
        catch(IllegalStateException e){android.util.Log.i("AshWidgets","Connection monitor awaits a widget or app interaction");}
    }
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager nm=getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel("connection","Conexão do widget",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Mantém a cor do anel atualizada quando a conexão muda.");
        channel.setShowBadge(false);nm.createNotificationChannel(channel);
        PendingIntent open=PendingIntent.getActivity(this,10,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(703,new Notification.Builder(this,"connection")
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle("ash-widgets")
            .setContentText("Acompanhando a conexão do widget")
            .setContentIntent(open).setOngoing(true).setShowWhen(false).build());
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(!hasWidgets(this)){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
        DataWidget.renderAll(this);return START_STICKY;
    }
    @Override public IBinder onBind(Intent i){return null;}
}
