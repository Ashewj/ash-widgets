package br.com.widgetpoco;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.widget.*;
import java.util.*;

public class DataWidget extends AppWidgetProvider {
    static final String REFRESH="br.com.widgetpoco.REFRESH";
    static final int INK=0xFFF4F6F8, MUTED=0xFFB6BEC7, BLUE=0xFF8DDEFF, GREEN=0xFF72DEA6, RED=0xFFFF8388;
    static void renderAll(Context c) {
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        for(int id:m.getAppWidgetIds(new ComponentName(c,DataWidget.class)))render(c,m,id);
    }
    static void text(Canvas a,Paint p,String value,float x,float y,float size,int color) {
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color);p.setTextSize(size);
        p.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        if(p.getTextAlign()==Paint.Align.LEFT){Rect bounds=new Rect();p.getTextBounds(value,0,value.length(),bounds);x-=bounds.left;}
        a.drawText(value,x,y,p);
    }
    static Bitmap bitmap(Context c,int frame) {
        return bitmap(c,frame,true);
    }
    static int frameCount(Context c){return CardBackground.smoky(c)?CardBackground.SMOKE_FRAMES:8;}
    static int frameInterval(Context c){return CardBackground.smoky(c)?CardBackground.SMOKE_INTERVAL:180;}
    static Bitmap bitmap(Context c,int frame,boolean background) {
        android.content.SharedPreferences s=Store.prefs(c);
        int network=AshApplication.networkState(c);
        boolean known=s.contains("updated"), error=network==0||!s.getString("error","").isEmpty();
        float total=s.getFloat("total",0),left=s.getFloat("left",0),used=total-left;
        Bitmap b=Bitmap.createBitmap(720,320,Bitmap.Config.ARGB_8888);
        Canvas a=new Canvas(b); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Path clip=new Path();clip.addRoundRect(new RectF(0,0,720,320),30,30,Path.Direction.CW);
        if(background){a.save();a.clipPath(clip);CardBackground.draw(c,a,p,frame);a.restore();p.setShader(null);}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(0xFF454E59);
        RectF arc=new RectF(45,37,291,283);a.drawArc(arc,0,360,false,p);
        if(network==0 || (known&&total>0)) {
            p.setColor(network==0?RED:network==2?GREEN:CardBackground.smoky(c)?CardBackground.accent(c):BLUE);p.setStrokeCap(Paint.Cap.ROUND);
            a.drawArc(arc,-90,known&&total>0?Math.max(0,Math.min(360,360*used/total)):360,false,p);
        }
        p.setStyle(Paint.Style.FILL);p.setStrokeWidth(0);
        for(int i=0;i<4;i++) {
            float h=10+i*8;int alpha=error?170:(int)(190+65*Math.sin((frame+i)*Math.PI/4));
            p.setColor((Math.max(90,alpha)<<24)|0x00F5FAFF);
            a.drawRoundRect(new RectF(146+i*13,153-h,153+i*13,153),2,2,p);
        }
        if(error&&known) {p.setColor(0xFFFF8F91);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);a.drawLine(194,143,204,153,p);a.drawLine(204,143,194,153,p);}
        p.setTextAlign(Paint.Align.CENTER);
        text(a,p,known?(error?"Offline":"TIM"):"Conectar à TIM",168,193,21,INK);
        p.setColor(0xFF414852);p.setStrokeWidth(1);a.drawLine(344,68,344,252,p);
        p.setTextAlign(Paint.Align.LEFT);
        String amount=known?String.format(new Locale("pt","BR"),"%.1f",used):"—";
        text(a,p,amount,388,144,76,INK);
        float amountWidth=p.measureText(amount);
        if(known)text(a,p,"GB",388+amountWidth+12,142,25,MUTED);
        text(a,p,known?String.format(new Locale("pt","BR"),"usados de %.1f GB",total):"Sua internet TIM",388,183,24,MUTED);
        text(a,p,known?String.format(new Locale("pt","BR"),"%.1f GB livres",left):"Toque para conectar",388,235,28,INK);
        return b;
    }
    static Bitmap stackForeground(Context c,boolean development){if(!development)return bitmap(c,0,false);Bitmap b=Bitmap.createBitmap(720,320,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(INK);p.setTextSize(32);p.setTextAlign(Paint.Align.CENTER);canvas.drawText("Em desenvolvimento",360,160-(p.ascent()+p.descent())/2,p);return b;}
    static Bitmap stackPage(Context c,boolean development){
        Bitmap result=Bitmap.createBitmap(720,320,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(result);canvas.drawColor(0xFF101C2C);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);CardBackground.draw(c,canvas,paint,0);
        if(development){paint.setColor(INK);paint.setTextSize(32);paint.setTypeface(Typeface.create("sans-serif",0));paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("Em desenvolvimento",360,160-(paint.ascent()+paint.descent())/2,paint);}else canvas.drawBitmap(bitmap(c,0,false),0,0,null);
        return result;
    }
    static void render(Context c,AppWidgetManager m,int id) {
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
        v.removeAllViews(R.id.frames);
        boolean smoky=CardBackground.smoky(c);
        int count=android.animation.ValueAnimator.areAnimatorsEnabled()?frameCount(c):1;
        for(int i=0;i<count;i++) {RemoteViews f=new RemoteViews(c.getPackageName(),smoky?R.layout.smoke_frame:R.layout.frame);f.setImageViewBitmap(R.id.card_image,smoky?CardBackground.widgetFrame(c,i):bitmap(c,i));v.addView(R.id.frames,f);}
        v.setViewVisibility(R.id.widget_foreground,smoky?android.view.View.VISIBLE:android.view.View.GONE);
        if(smoky)v.setImageViewBitmap(R.id.widget_foreground,bitmap(c,0,false));
        v.setInt(R.id.frames,"setFlipInterval",frameInterval(c));
        Intent i=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_root,pi);
        v.setContentDescription(R.id.widget_root,Store.prefs(c).contains("updated")?
            String.format(new Locale("pt","BR"),"Internet TIM: %.1f GB usados de %.1f GB. Toque para abrir ash-widgets.",Store.prefs(c).getFloat("total",0)-Store.prefs(c).getFloat("left",0),Store.prefs(c).getFloat("total",0)):"Conectar conta TIM no ash-widgets");
        m.updateAppWidget(id,v);
    }
    public void onUpdate(Context c,AppWidgetManager m,int[] ids){renderAll(c);RefreshJob.schedule(c);ConnectionService.ensure(c);}
    public void onDisabled(Context c){c.stopService(new Intent(c,ConnectionService.class));}
    public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,android.os.Bundle b){render(c,m,id);}
    public void onReceive(Context c,Intent i){super.onReceive(c,i);if(REFRESH.equals(i.getAction()))RefreshJob.request(c);}
}
