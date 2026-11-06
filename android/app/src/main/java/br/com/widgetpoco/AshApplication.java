package br.com.widgetpoco;

import android.app.Application;
import android.content.Context;
import android.net.*;
import android.os.*;

public class AshApplication extends Application {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private static volatile int activeState=-1;
    static int state(NetworkCapabilities caps){
        if(caps==null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))return 0;
        if(caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            && !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))return 2;
        return 1;
    }
    static int networkState(Context c){
        if(activeState>=0)return activeState;
        ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm==null?0:state(cm.getNetworkCapabilities(cm.getActiveNetwork()));
    }
    private void changed(int value){
        if(activeState==value)return;
        activeState=value;
        // Repaint only; changing connection never starts a TIM data capture.
        DataWidget.renderAll(this);
        Store.prefs(this).edit().putInt("networkState",value).apply();
    }
    @Override public void onCreate(){
        super.onCreate();
        ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if(cm==null)return;
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
            @Override public void onCapabilitiesChanged(Network n,NetworkCapabilities caps){changed(state(caps));}
            @Override public void onLost(Network n){changed(0);}
        },ui);
    }
}
