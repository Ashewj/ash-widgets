package br.com.widgetpoco;

import android.content.Context;
import android.graphics.*;
import android.opengl.*;
import java.io.*;
import java.nio.*;

/** LRan's Smoky Button shader, with the hovered canvas treatment. Owns one EGL context. */
final class SmokeRenderer implements AutoCloseable {
    private final EGLDisplay display;
    private EGLContext context=EGL14.EGL_NO_CONTEXT;
    private EGLSurface surface=EGL14.EGL_NO_SURFACE;
    private int program, timeLocation, primaryLocation, resolutionLocation, opaqueLocation;
    private FloatBuffer triangle;
    private final boolean live;
    private final int[] surfaceSize=new int[1];
    SmokeRenderer(Context app,int width,int height,SurfaceTexture texture){
        live=texture!=null;
        display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        try{
            int[] version=new int[2];check(EGL14.eglInitialize(display,version,0,version,1),"EGL initialization");
            EGLConfig[] configs=new EGLConfig[1];int[] count=new int[1];
            int surfaceType=texture==null?EGL14.EGL_PBUFFER_BIT:EGL14.EGL_WINDOW_BIT;
            check(EGL14.eglChooseConfig(display,new int[]{EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,surfaceType,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_NONE},0,configs,0,1,count,0)&&count[0]>0,"RGBA8888 configuration");
            context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);
            surface=texture==null?EGL14.eglCreatePbufferSurface(display,configs[0],new int[]{EGL14.EGL_WIDTH,width,EGL14.EGL_HEIGHT,height,EGL14.EGL_NONE},0):EGL14.eglCreateWindowSurface(display,configs[0],texture,new int[]{EGL14.EGL_NONE},0);
            check(EGL14.eglMakeCurrent(display,surface,surface,context),"EGL surface");
            EGL14.eglSwapInterval(display,1);
            int[] range=new int[2],precision=new int[1];GLES20.glGetShaderPrecisionFormat(GLES20.GL_FRAGMENT_SHADER,GLES20.GL_HIGH_FLOAT,range,0,precision,0);
            check(precision[0]>=16,"highp fragment precision");
            String source;
            try(InputStream in=app.getResources().openRawResource(R.raw.smoky_fragment);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);source=out.toString("UTF-8");
            }
            source=source.replace("precision mediump float;","precision highp float;").replace("gl_FragCoord.xy","hoverCoordinates()");
            String hover="vec2 hoverCoordinates(){vec2 p=gl_FragCoord.xy-.5*uResolution;float a=radians(.5);return mat2(cos(a),sin(a),-sin(a),cos(a))*p/1.14+.5*uResolution;}\n";
            source=source.replace("void main() {","uniform float uOpaqueBase;\n"+hover+"void main() {");
            // CSS hover: saturate(1.08), contrast(1.04). Composite explicitly over #111.
            // Dither by less than one 8-bit step, with a stationary pattern (no sparkle).
            String output="vec3 ink=clamp(color,0.0,1.0);float luma=dot(ink,vec3(.2126,.7152,.0722));ink=clamp((mix(vec3(luma),ink,1.08)-.5)*1.04+.5,0.0,1.0);"
                +"float grain=fract(52.9829189*fract(dot(floor(gl_FragCoord.xy),vec2(.06711056,.00583715))))-.5;"
                +"vec3 rgb=ink*alpha+vec3(17.0/255.0)*(1.0-alpha)*uOpaqueBase;float outAlpha=alpha+(1.0-alpha)*uOpaqueBase;gl_FragColor=vec4(clamp(rgb+grain*alpha/255.0,0.0,outAlpha),outAlpha);";
            check(source.contains("gl_FragColor = vec4(clamp(color, 0.0, 1.0), alpha);"),"Smoky source output");
            source=source.replace("gl_FragColor = vec4(clamp(color, 0.0, 1.0), alpha);",output);
            int vertex=0,fragment=0;
            try{
                vertex=compile(GLES20.GL_VERTEX_SHADER,"attribute vec2 aPosition;void main(){gl_Position=vec4(aPosition,0.0,1.0);}");
                fragment=compile(GLES20.GL_FRAGMENT_SHADER,source);
                program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vertex);GLES20.glAttachShader(program,fragment);GLES20.glLinkProgram(program);
                int[] ok=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);check(ok[0]!=0,GLES20.glGetProgramInfoLog(program));
            }finally{if(vertex!=0)GLES20.glDeleteShader(vertex);if(fragment!=0)GLES20.glDeleteShader(fragment);}
            GLES20.glUseProgram(program);GLES20.glDisable(GLES20.GL_DITHER);
            triangle=ByteBuffer.allocateDirect(24).order(ByteOrder.nativeOrder()).asFloatBuffer();triangle.put(new float[]{-1,-1,3,-1,-1,3}).position(0);
            int position=GLES20.glGetAttribLocation(program,"aPosition");GLES20.glEnableVertexAttribArray(position);GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,0,triangle);
            resolutionLocation=GLES20.glGetUniformLocation(program,"uResolution");timeLocation=GLES20.glGetUniformLocation(program,"uTime");primaryLocation=GLES20.glGetUniformLocation(program,"uPrimary");
            opaqueLocation=GLES20.glGetUniformLocation(program,"uOpaqueBase");
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"uSpeed"),1);
            GLES20.glUniform3f(GLES20.glGetUniformLocation(program,"uSecondary"),92/255f,51/255f,31/255f);
            GLES20.glUniform3f(GLES20.glGetUniformLocation(program,"uShadow"),5/255f,5/255f,6/255f);
            StringBuilder channels=new StringBuilder();for(int key:new int[]{EGL14.EGL_RED_SIZE,EGL14.EGL_GREEN_SIZE,EGL14.EGL_BLUE_SIZE,EGL14.EGL_ALPHA_SIZE}){int[] value=new int[1];EGL14.eglGetConfigAttrib(display,configs[0],key,value,0);channels.append(value[0]).append('/');}
            android.util.Log.i("AshSmoke","highp="+precision[0]+", RGBA="+channels+", live="+(texture!=null)+", GPU="+GLES20.glGetString(GLES20.GL_RENDERER));
        }catch(Exception e){close();throw new IllegalStateException("Smoky initialization",e);}
    }
    private static void check(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
    private static int compile(int type,String source){int shader=GLES20.glCreateShader(type);GLES20.glShaderSource(shader,source);GLES20.glCompileShader(shader);int[] ok=new int[1];GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){String error=GLES20.glGetShaderInfoLog(shader);GLES20.glDeleteShader(shader);throw new IllegalStateException(error);}return shader;}
    void draw(int width,int height,float seconds,int color,boolean opaque){if(live){EGL14.eglQuerySurface(display,surface,EGL14.EGL_WIDTH,surfaceSize,0);width=surfaceSize[0];EGL14.eglQuerySurface(display,surface,EGL14.EGL_HEIGHT,surfaceSize,0);height=surfaceSize[0];}GLES20.glViewport(0,0,width,height);GLES20.glUseProgram(program);GLES20.glUniform2f(resolutionLocation,width,height);GLES20.glUniform1f(timeLocation,seconds);GLES20.glUniform1f(opaqueLocation,opaque?1:0);GLES20.glUniform3f(primaryLocation,Color.red(color)/255f,Color.green(color)/255f,Color.blue(color)/255f);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,3);}
    void present(){check(EGL14.eglSwapBuffers(display,surface),"Smoky swap buffers");}
    @Override public void close(){
        if(program!=0){GLES20.glDeleteProgram(program);program=0;}
        EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
        if(surface!=EGL14.EGL_NO_SURFACE){EGL14.eglDestroySurface(display,surface);surface=EGL14.EGL_NO_SURFACE;}
        if(context!=EGL14.EGL_NO_CONTEXT){EGL14.eglDestroyContext(display,context);context=EGL14.EGL_NO_CONTEXT;}
        EGL14.eglReleaseThread();
    }
}
