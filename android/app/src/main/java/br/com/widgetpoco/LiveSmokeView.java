package br.com.widgetpoco;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.*;
import android.view.*;

/** GPU rendering paced by display vsync, with elapsed time independent of frame count. */
final class LiveSmokeView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final long EPOCH=System.nanoTime();
    private volatile int color;
    private volatile boolean opaque;
    private boolean active=true;
    private Loop loop;
    LiveSmokeView(Context c){super(c);color=CardBackground.shaderColor(c);opaque=!CardBackground.transparentSmoke(c);setOpaque(opaque);setSurfaceTextureListener(this);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void color(int value){color=value;}
    void opaque(boolean value){opaque=value;setOpaque(value);}
    void active(boolean value){active=value;if(loop!=null)loop.active(value);}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture texture,int width,int height){loop=new Loop(texture,width,height);loop.start();loop.active(active);}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture,int width,int height){if(loop!=null)loop.resize(width,height);}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture){if(loop!=null){loop.stop(true);loop=null;}else texture.release();return false;}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture texture){}
    @Override protected void onDetachedFromWindow(){active(false);super.onDetachedFromWindow();}
    private final class Loop implements Choreographer.FrameCallback {
        final SurfaceTexture texture;final HandlerThread thread=new HandlerThread("AshSmokeGPU");
        Handler handler;Choreographer clock;SmokeRenderer renderer;
        volatile boolean stopped,visible=true;volatile int width,height;
        long frames,firstFrame,lastFrame;boolean scheduled,releaseRequested,closed,textureReleased;
        Loop(SurfaceTexture texture,int width,int height){this.texture=texture;resize(width,height);}
        void resize(int w,int h){width=Math.max(1,w);height=Math.max(1,h);}
        void start(){thread.start();handler=new Handler(thread.getLooper());handler.post(()->{
            try{renderer=new SmokeRenderer(getContext().getApplicationContext(),width,height,texture);clock=Choreographer.getInstance();schedule();}
            catch(RuntimeException e){android.util.Log.e("AshSmoke","Live renderer failed",e);stop(false);}
        });}
        void active(boolean value){visible=value;if(handler!=null)handler.post(()->{if(clock==null)return;clock.removeFrameCallback(this);scheduled=false;if(value)schedule();});}
        void schedule(){if(!stopped&&visible&&!scheduled&&clock!=null){scheduled=true;clock.postFrameCallback(this);}}
        @Override public void doFrame(long frameTimeNanos){
            scheduled=false;if(stopped||!visible||renderer==null)return;
            try{
                renderer.draw(width,height,(frameTimeNanos-EPOCH)/1_000_000_000f,color,opaque);renderer.present();
                if(firstFrame==0)firstFrame=frameTimeNanos;lastFrame=frameTimeNanos;frames++;
                if(android.animation.ValueAnimator.areAnimatorsEnabled())schedule();
            }catch(RuntimeException e){android.util.Log.e("AshSmoke","Live draw failed",e);stop(false);}
        }
        synchronized void releaseTexture(){if(!textureReleased){textureReleased=true;texture.release();}}
        synchronized void stop(boolean release){
            releaseRequested|=release;
            if(closed){if(release)releaseTexture();return;}
            if(stopped)return;stopped=true;
            if(handler==null){if(release)texture.release();return;}
            handler.post(()->{if(clock!=null)clock.removeFrameCallback(this);if(renderer!=null){renderer.close();renderer=null;}if(frames>1)android.util.Log.i("AshSmoke","Live frames="+frames+", averageHz="+String.format(java.util.Locale.ROOT,"%.1f",(frames-1)*1e9/(lastFrame-firstFrame)));synchronized(this){if(releaseRequested)releaseTexture();closed=true;}thread.quitSafely();});
        }
    }
}
