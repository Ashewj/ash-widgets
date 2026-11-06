package br.com.widgetpoco;

import android.content.*;
import android.os.*;
import android.webkit.*;
import org.json.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** On-device login engine. Never reads a PC file or talks to a PC service. */
final class TimLogin {
    static final AtomicBoolean active=new AtomicBoolean();
    interface Done {void finish(boolean success);}
    final Context context;
    final Handler handler=new Handler(Looper.getMainLooper());
    final AtomicBoolean complete=new AtomicBoolean(), capturing=new AtomicBoolean();
    WebView web; Done done; boolean submitted;
    TimLogin(Context context){this.context=context;}
    void start(android.widget.LinearLayout container,Done callback) {
        done=callback;
        if(!active.compareAndSet(false,true)){callback.finish(false);return;}
        web=new WebView(context);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        CookieManager.getInstance().setAcceptCookie(true);
        if(container!=null)container.addView(web,new android.widget.LinearLayout.LayoutParams(-1,0,1));
        web.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                String host=r.getUrl().getHost();
                if("http".equals(r.getUrl().getScheme())&&host!=null&&(host.equals("tim.com.br")||host.endsWith(".tim.com.br"))){
                    v.loadUrl(r.getUrl().buildUpon().scheme("https").build().toString());return true;
                }
                return !"https".equals(r.getUrl().getScheme())||host==null||!(host.equals("tim.com.br")||host.endsWith(".tim.com.br")||host.equals("timbrasil.com.br")||host.endsWith(".timbrasil.com.br"));
            }
            public void onPageFinished(WebView v,String url){
                if(url.startsWith("https://meutim.tim.com.br/novo/login")&&!submitted)fillForm();
            }
            public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())android.util.Log.w("AshTim","Page error "+e.getErrorCode());}
            public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response){if(r.isForMainFrame())android.util.Log.w("AshTim","Page HTTP "+response.getStatusCode());}
            public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
                if(Tim.isHome(r.getUrl())&&capturing.compareAndSet(false,true)) {
                    try {
                        JSONObject headers=new JSONObject(r.getRequestHeaders());
                        String cookie=CookieManager.getInstance().getCookie(Tim.URL);
                        if(cookie!=null)headers.put("Cookie",cookie);
                        JSONObject result=Tim.request(headers);
                        Store.secret(context,headers.toString());Tim.save(context,result);
                        handler.post(()->finish(true));
                        return new WebResourceResponse("application/json","UTF-8",new java.io.ByteArrayInputStream(result.toString().getBytes("UTF-8")));
                    }catch(Exception e){capturing.set(false);android.util.Log.w("AshTim","Capture failed: "+e.getClass().getSimpleName());}
                }
                return null;
            }
        });
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
        web.loadUrl("https://meuplano2.tim.com.br/");
        handler.postDelayed(()->{
            if(web!=null&&!complete.get()&&!capturing.get()&&!submitted){
                String url=web.getUrl();
                if(url==null||url.startsWith("https://meuplano2.tim.com.br/"))web.loadUrl("https://meutim.tim.com.br/");
            }
        },15000);
        handler.postDelayed(()->finish(false),90000);
    }
    void fillForm() {
        if(submitted||complete.get()||web==null)return;
        try {
            JSONObject credentials=Store.credentials(context);
            String phone=credentials.getString("number");
            String formatted="("+phone.substring(0,2)+") "+phone.substring(2,7)+"-"+phone.substring(7);
            String script="(function(){var n=document.querySelector('#campo-numero'),p=document.querySelector('#campo-senha'),b=document.querySelector('#btn-entrar');if(!n||!p||!b)return 'wait';"
                +"n.value="+JSONObject.quote(formatted)+";p.value="+JSONObject.quote(credentials.getString("password"))+";"
                +"[n,p].forEach(function(e){['input','change','keyup','blur'].forEach(function(t){e.dispatchEvent(new Event(t,{bubbles:true}));});if(window.jQuery)window.jQuery(e).trigger('keyup').trigger('change');});"
                +"if(b.disabled)return 'disabled';b.click();return 'submitted';})()";
            web.evaluateJavascript(script,value->{
                if(value.contains("submitted"))submitted=true;
                else if(!complete.get())handler.postDelayed(()->{if(web!=null)fillForm();},1000);
            });
        }catch(Exception e){finish(false);}
    }
    void finish(boolean success) {
        if(!complete.compareAndSet(false,true))return;
        active.set(false);
        handler.removeCallbacksAndMessages(null);
        if(!success){Store.prefs(context).edit().putBoolean("needsLogin",true).putString("error","Entre na TIM para conectar").apply();DataWidget.renderAll(context);}
        if(web!=null){web.stopLoading();web.destroy();web=null;}
        if(done!=null)done.finish(success);
    }
    void cancel(){finish(false);}
}
