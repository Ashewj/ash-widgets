package br.com.widgetpoco;

import android.content.Context;
import android.graphics.*;
import android.opengl.*;
import java.nio.*;
import java.io.*;

/** Cached launcher frames; the app uses the live renderer in WidgetPreview. */
final class CardBackground {
    static final String[] THEMES={"Opaque","Transparent","Opaque Smoky","Transparent Smoky"};
    static final int SMOKE_FRAMES=240, SMOKE_INTERVAL=16;
    static final int DEFAULT_SMOKY_COLOR=0xFF002FA7;
    private static Bitmap[] smoke;
    private static int smokeColor;
    private static boolean smokeOpaque;
    private static Long failedKey;
    private static final int W=160,H=71;
    static String theme(Context c){String saved=Store.prefs(c).getString("cardTheme","Opaque");return "Smoky".equals(saved)?"Opaque Smoky":saved;}
    static boolean smoky(Context c){return theme(c).endsWith(" Smoky");}
    static boolean transparentSmoke(Context c){return "Transparent Smoky".equals(theme(c));}
    static int accent(Context c){return Store.prefs(c).getInt("smokyColor",DEFAULT_SMOKY_COLOR);}
    static int shaderColor(Context c){return Store.prefs(c).getInt("smokyColor",DEFAULT_SMOKY_COLOR);}
    static void draw(Context c,Canvas canvas,Paint paint,int frame){
        String theme=theme(c);
        if("Transparent".equals(theme)){drawGlass(canvas,paint);return;}
        if("Transparent Smoky".equals(theme))drawGlass(canvas,paint);
        if(theme.endsWith(" Smoky")){
            try{prepare(c);paint.setShader(null);paint.setFilterBitmap(true);canvas.drawBitmap(smoke[frame%SMOKE_FRAMES],null,new RectF(0,0,720,320),paint);return;}
            catch(RuntimeException e){android.util.Log.w("AshWidgets","Smoky GPU unavailable",e);}
        }
        canvas.drawColor(0xFF1B1F25);
    }
    // Translucent charcoal glass treatment, shared by preview and launcher.
    static void drawGlass(Canvas canvas,Paint paint){
        paint.setShader(new LinearGradient(0,0,720,320,new int[]{0xE51B2532,0xD5111822,0xE518222E},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,720,320,paint);
        paint.setShader(new RadialGradient(180,-100,610,new int[]{0x08BDDDF0,0x00BDDDF0},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,720,320,paint);paint.setShader(null);
    }
    static Bitmap widgetFrame(Context c,int frame){
        Bitmap result=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(result);canvas.scale(W/720f,H/320f);
        draw(c,canvas,new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),frame);
        return result;
    }
    static void prepare(Context c){prepare(c,shaderColor(c));}
    static void prepare(Context c,int color){prepare(c,color,!transparentSmoke(c));}
    static synchronized void prepare(Context c,int color,boolean opaque){
        long key=(color&0xffffffffL)|(opaque?(1L<<32):0);
        if(smoke!=null&&smokeColor==color&&smokeOpaque==opaque)return;
        if(failedKey!=null&&failedKey==key)throw new IllegalStateException("Smoky GPU unavailable for this color");
        try(SmokeRenderer renderer=new SmokeRenderer(c,W,H,null)){
            Bitmap[] frames=new Bitmap[SMOKE_FRAMES];ByteBuffer pixels=ByteBuffer.allocateDirect(W*H*4);int[] argb=new int[W*H];
            final int overlap=40;Bitmap[] head=new Bitmap[overlap];
            for(int frame=0;frame<SMOKE_FRAMES+overlap;frame++){
                renderer.draw(W,H,frame*SMOKE_INTERVAL/1000f,color,opaque);pixels.position(0);
                GLES20.glReadPixels(0,0,W,H,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,pixels);
                if(GLES20.glGetError()!=GLES20.GL_NO_ERROR)throw new IllegalStateException("GPU readback");
                for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=(y*W+x)*4;int alpha=pixels.get(i+3)&255;int red=alpha==0?0:Math.min(255,(pixels.get(i)&255)*255/alpha),green=alpha==0?0:Math.min(255,(pixels.get(i+1)&255)*255/alpha),blue=alpha==0?0:Math.min(255,(pixels.get(i+2)&255)*255/alpha);argb[(H-1-y)*W+x]=(alpha<<24)|(red<<16)|(green<<8)|blue;}
                Bitmap rendered=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);rendered.setPixels(argb,0,W,0,0,W,H);
                if(frame<overlap){head[frame]=rendered;continue;}
                int output=frame-overlap;
                if(output>=SMOKE_FRAMES-overlap){int index=output-(SMOKE_FRAMES-overlap);float t=index/(float)(overlap-1);t=t*t*(3-2*t);Bitmap tail=rendered.copy(Bitmap.Config.ARGB_8888,false);rendered.eraseColor(Color.TRANSPARENT);Canvas mix=new Canvas(rendered);Paint blend=new Paint(Paint.ANTI_ALIAS_FLAG);blend.setAlpha(Math.round(255*(1-t)));mix.drawBitmap(tail,0,0,blend);blend.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));blend.setAlpha(Math.round(255*t));mix.drawBitmap(head[index],0,0,blend);tail.recycle();}
                frames[output]=rendered;
            }
            for(Bitmap bitmap:head)bitmap.recycle();smoke=frames;smokeColor=color;smokeOpaque=opaque;failedKey=null;
        }catch(RuntimeException e){failedKey=key;throw e;}
    }
}
