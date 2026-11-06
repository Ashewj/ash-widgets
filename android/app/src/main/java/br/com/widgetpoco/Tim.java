package br.com.widgetpoco;

import android.content.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

final class Tim {
    static final String URL="https://meutim-aks.timbrasil.com.br/api/v1/cards/card-internet/individual?scope=home";
    static boolean isHome(android.net.Uri u) {
        return "meutim-aks.timbrasil.com.br".equals(u.getHost())
            && "/api/v1/cards/card-internet/individual".equals(u.getPath())
            && "home".equals(u.getQueryParameter("scope"));
    }
    static synchronized void save(Context c, JSONObject json) throws Exception {
        JSONObject p=json.getJSONObject("totalConsumption").getJSONObject("internetPackage");
        if(!p.getJSONObject("totalCapacity").getString("unit").equals("GB")) throw new IOException("Unidade desconhecida");
        double total=p.getJSONObject("totalCapacity").getDouble("value");
        double left=p.getJSONObject("remaining").getDouble("value");
        if(!Double.isFinite(total)||!Double.isFinite(left)||total<=0||left<0||left>total) throw new IOException("Saldo inválido");
        long now=System.currentTimeMillis();
        android.content.SharedPreferences s=Store.prefs(c);
        android.content.SharedPreferences.Editor e=s.edit();
        if(!s.contains("since")||s.getFloat("left",0)<left) e.putLong("since",now).putFloat("initialLeft",(float)left);
        e.putFloat("total",(float)total).putFloat("left",(float)left).putLong("updated",now)
            .putString("error","").putBoolean("needsLogin",false).apply();
        DataWidget.renderAll(c);
    }
    static JSONObject request(JSONObject headers) throws Exception {
        HttpURLConnection x=(HttpURLConnection)new java.net.URL(URL).openConnection();
        x.setInstanceFollowRedirects(false); x.setConnectTimeout(20000); x.setReadTimeout(20000);
        Iterator<String> keys=headers.keys();
        while(keys.hasNext()) {
            String k=keys.next();
            if(!k.startsWith(":")&&!Arrays.asList("host","content-length","accept-encoding","connection").contains(k.toLowerCase(Locale.ROOT)))
                x.setRequestProperty(k,headers.getString(k));
        }
        x.setRequestProperty("Accept-Encoding","identity");
        try {
            int status=x.getResponseCode();
            android.util.Log.i("AshTim","API HTTP "+status);
            if(status==401||status==403) throw new LoginRequired();
            if(status!=200) throw new IOException("HTTP "+status);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=x.getInputStream()) {
                byte[] b=new byte[8192]; int n;
                while((n=in.read(b))!=-1) {out.write(b,0,n); if(out.size()>2000000)throw new IOException("Resposta excessiva");}
            }
            return new JSONObject(out.toString("UTF-8"));
        } finally {x.disconnect();}
    }
    static final class LoginRequired extends IOException {}
    static boolean refresh(Context c) {
        try {JSONObject headers=Store.secret(c);if(headers.length()<2)throw new LoginRequired();save(c,request(headers)); return true; }
        catch(Exception e) {
            android.util.Log.w("AshTim","API: "+e.getClass().getSimpleName()+" cause="+(e.getCause()==null?"none":e.getCause().getClass().getSimpleName()));
            boolean auth=e instanceof LoginRequired || !Store.prefs(c).contains("session");
            Store.prefs(c).edit().putBoolean("needsLogin",auth).putString("error",auth?"Conectar à TIM":"Sem conexão · toque para tentar").apply();
            DataWidget.renderAll(c); return false;
        }
    }
}
