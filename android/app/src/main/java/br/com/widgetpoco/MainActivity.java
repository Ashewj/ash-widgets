package br.com.widgetpoco;

import android.animation.ValueAnimator;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    LinearLayout root; TimLogin login; WidgetPreview preview; TextView accountStatus; boolean busy, destroyed, homeVisible;
    int selectedTab;
    final Handler ui=new Handler(Looper.getMainLooper());
    final Runnable redraw=()->{if(!destroyed){if(preview!=null)preview.refresh();if(homeVisible&&accountStatus!=null)accountStatus.setText(accountSummary());}};
    final android.content.SharedPreferences.OnSharedPreferenceChangeListener changes=(p,key)->{
        if("updated".equals(key)||"error".equals(key)||"networkState".equals(key)){ui.removeCallbacks(redraw);ui.postDelayed(redraw,100);}
    };
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    TextView label(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(8),0,dp(8));return t;}
    void button(String title,boolean primary,Runnable action){
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextColor(primary?0xFF10253B:0xFFEAF5FF);b.setTextSize(16);
        GradientDrawable bg=new GradientDrawable();bg.setColor(primary?0xFF9ADFFF:0xFF263C52);bg.setCornerRadius(dp(16));b.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.topMargin=dp(12);root.addView(b,lp);b.setOnClickListener(v->action.run());
    }
    void shell(String title){
        homeVisible=false;
        selectedTab=title==null?2:"Customização".equals(title)?1:"Login".equals(title)?3:0;
        if(preview!=null){preview.active(false);preview=null;}accountStatus=null;
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(24),dp(20),dp(24),dp(28));root.setBackgroundColor(0xFF101C2C);
        FrameLayout app=new FrameLayout(this);app.setBackgroundColor(0xFF101C2C);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        scroll.setPadding(0,0,0,dp(104));scroll.addView(root);app.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(0);tabs.setPadding(dp(6),dp(6),dp(6),dp(6));
        GradientDrawable dock=new GradientDrawable();dock.setColor(0xFF203044);dock.setCornerRadius(dp(28));tabs.setBackground(dock);tabs.setElevation(dp(12));
        String[] names={"Home","Customização","Outros","Login"};
        for(int index=0;index<names.length;index++){
            final int tab=index;
            LinearLayout item=new LinearLayout(this);item.setOrientation(1);item.setGravity(Gravity.CENTER);
            boolean selected=index==selectedTab;
            GradientDrawable itemBg=new GradientDrawable();itemBg.setColor(selected?0xFF324A60:Color.TRANSPARENT);itemBg.setCornerRadius(dp(22));
            GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(dp(22));
            item.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x339ADFFF),itemBg,mask));
            ImageView icon=new ImageView(this);icon.setImageDrawable(new DockIcon(index,selected?0xFF9ADFFF:0xFFB9CDDD));
            item.addView(icon,new LinearLayout.LayoutParams(dp(24),dp(24)));
            TextView name=new TextView(this);name.setText(names[index]);name.setTextSize(10);name.setGravity(Gravity.CENTER);name.setTextColor(selected?Color.WHITE:0xFFB9CDDD);
            name.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);LinearLayout.LayoutParams caption=new LinearLayout.LayoutParams(-1,-2);caption.topMargin=dp(4);item.addView(name,caption);
            item.setSelected(selected);item.setContentDescription(names[index]+(selected?", selecionada":""));item.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            item.setFocusable(true);item.setOnClickListener(v->{if(selectedTab!=tab)openTab(tab);});
            tabs.addView(item,new LinearLayout.LayoutParams(0,dp(60),1));
        }
        FrameLayout.LayoutParams dockPosition=new FrameLayout.LayoutParams(-1,dp(72),Gravity.BOTTOM);dockPosition.setMargins(dp(20),0,dp(20),dp(16));
        app.addView(tabs,dockPosition);setContentView(app);
        getWindow().setStatusBarColor(0xFF101C2C);getWindow().setNavigationBarColor(0xFF101C2C);
        if(title!=null)root.addView(label(title,30,Color.WHITE));
    }
    void openTab(int tab){
        if(login!=null){TimLogin pending=login;login=null;pending.done=null;pending.cancel();busy=false;}
        if(tab==1)settings();else if(tab==2)others();else if(tab==3)connect();else home();
    }
    void others(){shell(null);TextView message=label("Em desenvolvimento",20,0xFFB9CDDD);message.setGravity(Gravity.CENTER);root.addView(message,new LinearLayout.LayoutParams(-1,-1));}
    public void onCreate(Bundle state){super.onCreate(state);preferDisplayRate();Store.prefs(this).registerOnSharedPreferenceChangeListener(changes);RefreshJob.schedule(this);ConnectionService.ensure(this);openTab(state==null?0:state.getInt("tab",0));}
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("tab",selectedTab);super.onSaveInstanceState(state);}
    void home(){
        shell("ash-widgets");
        homeVisible=true;
        root.addView(label("Seus dados, no seu ritmo.",17,0xFFB9CDDD));
        for(WidgetCatalog.Entry item:WidgetCatalog.ENTRIES){
            TextView title=label(item.title,23,Color.WHITE);title.setPadding(0,dp(28),0,dp(12));root.addView(title);
            preview=new WidgetPreview(this);
            root.addView(preview,new LinearLayout.LayoutParams(-1,dp(172)));
        }
        boolean known=Store.prefs(this).contains("updated");
        accountStatus=label(accountSummary(),16,0xFFB9CDDD);root.addView(accountStatus);
        if(!known)button("Conectar à TIM",true,()->openTab(3));
        button("Adicionar à tela inicial",known,()->{
            AppWidgetManager m=AppWidgetManager.getInstance(this);
            if(m.isRequestPinAppWidgetSupported())m.requestPinAppWidget(new ComponentName(this,DataWidget.class),null,null);
            else new AlertDialog.Builder(this).setMessage("Na tela inicial, abra Widgets e escolha ash-widgets.").setPositiveButton("Entendi",null).show();
        });
        button(busy?"Atualizando…":"Atualizar agora",false,()->{
            if(busy)return;
            if(!Store.hasCredentials(this)){connect();return;}
            busy=true;home();
            new Thread(()->{boolean ok=Tim.refresh(this);runOnUiThread(()->{busy=false;if(destroyed||selectedTab!=0)return;if(!ok&&Store.prefs(this).getBoolean("needsLogin",false))authenticate();else home();});}).start();
        });
        root.addView(label("A cada 1 hora",18,Color.WHITE));
        root.addView(label("O celular consulta a TIM diretamente. O Android pode adiar a atualização para economizar bateria. Toque no widget para abrir este aplicativo e use Atualizar agora para consultar antes.",15,0xFFB9CDDD));
    }
    void settings(){
        shell("Customização");
        root.addView(label("Background do card",18,Color.WHITE));
        WidgetPreview sample=new WidgetPreview(this);preview=sample;
        sample.setContentDescription("Prévia do background selecionado");
        root.addView(sample,new LinearLayout.LayoutParams(-1,dp(172)));
        Spinner themes=new Spinner(this,Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,CardBackground.THEMES){
            @Override public View getView(int position,View convert,ViewGroup parent){
                TextView v=(TextView)super.getView(position,convert,parent);v.setTextColor(Color.WHITE);v.setTextSize(17);v.setPadding(dp(16),dp(12),dp(16),dp(12));return v;
            }
            @Override public View getDropDownView(int position,View convert,ViewGroup parent){
                TextView v=(TextView)super.getDropDownView(position,convert,parent);v.setTextColor(Color.WHITE);v.setBackgroundColor(0xFF262C34);v.setMinHeight(dp(52));v.setGravity(Gravity.CENTER_VERTICAL);return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);themes.setAdapter(adapter);
        themes.setContentDescription("Tema do background do card");themes.setMinimumHeight(dp(56));themes.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF262C34));
        for(int i=0;i<CardBackground.THEMES.length;i++)if(CardBackground.THEMES[i].equals(CardBackground.theme(this)))themes.setSelection(i);
        root.addView(themes,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView status=label("A escolha é salva e aplicada ao widget automaticamente.",15,0xFFB9CDDD);root.addView(status);
        Button color=new Button(this);color.setAllCaps(false);color.setTextSize(16);color.setTextColor(Color.WHITE);
        GradientDrawable colorBg=new GradientDrawable();colorBg.setColor(0xFF263C52);colorBg.setCornerRadius(dp(16));color.setBackground(colorBg);
        color.setText(String.format(java.util.Locale.ROOT,"Cor do Smoky e do anel · #%06X",CardBackground.accent(this)&0xFFFFFF));
        color.setVisibility(CardBackground.smoky(this)?View.VISIBLE:View.GONE);
        LinearLayout.LayoutParams colorLp=new LinearLayout.LayoutParams(-1,dp(56));colorLp.topMargin=dp(8);root.addView(color,colorLp);
        color.setOnClickListener(v->ColorPicker.show(this,CardBackground.accent(this),chosen->{
            color.setEnabled(false);themes.setEnabled(false);status.setText("Aplicando cor…");
            new Thread(()->{
                try{
                    synchronized(CardBackground.class){
                        CardBackground.prepare(this,chosen);
                        Store.prefs(this).edit().putInt("smokyColor",chosen).apply();
                    }
                    DataWidget.renderAll(this);
                    runOnUiThread(()->{if(!destroyed&&selectedTab==1){color.setEnabled(true);themes.setEnabled(true);color.setText(String.format(java.util.Locale.ROOT,"Cor do Smoky e do anel · #%06X",chosen&0xFFFFFF));status.setText("Cor aplicada à fumaça e ao anel no Wi-Fi.");animateSample(sample);}});
                }catch(RuntimeException e){runOnUiThread(()->{if(!destroyed){color.setEnabled(true);themes.setEnabled(true);status.setText("Não foi possível aplicar a cor. Tente novamente.");}});}
            }).start();
        }));
        themes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int index,long id){
                String choice=CardBackground.THEMES[index];if(choice.equals(CardBackground.theme(MainActivity.this)))return;
                themes.setEnabled(false);color.setEnabled(false);status.setText("Aplicando "+choice+"…");
                new Thread(()->{
                    try{
                        if(choice.endsWith(" Smoky"))CardBackground.prepare(MainActivity.this,CardBackground.shaderColor(MainActivity.this),!"Transparent Smoky".equals(choice));
                        Store.prefs(MainActivity.this).edit().putString("cardTheme",choice).apply();
                        DataWidget.renderAll(MainActivity.this);
                        runOnUiThread(()->{if(!destroyed&&selectedTab==1){themes.setEnabled(true);color.setEnabled(true);color.setVisibility(choice.endsWith(" Smoky")?View.VISIBLE:View.GONE);status.setText("Tema "+choice+" aplicado.");animateSample(sample);}});
                    }catch(RuntimeException e){runOnUiThread(()->{if(!destroyed){themes.setEnabled(true);color.setEnabled(true);status.setText("Não foi possível carregar Smoky neste aparelho. O tema anterior foi mantido.");for(int i=0;i<CardBackground.THEMES.length;i++)if(CardBackground.THEMES[i].equals(CardBackground.theme(MainActivity.this)))themes.setSelection(i);}});}
                }).start();
            }
        });
        animateSample(sample);
        root.addView(label("Verde: rede móvel · Azul ou cor escolhida: Wi-Fi · Vermelho: sem internet",15,0xFFB9CDDD));
        root.addView(label("O Android mantém um serviço de conexão enquanto houver widgets na tela inicial. As consultas à TIM continuam a cada 1 hora.",14,0xFFB9CDDD));
    }
    void animateSample(WidgetPreview image){image.refresh();}
    String accountSummary(){String error=Store.prefs(this).getString("error","");return !error.isEmpty()?error:Store.prefs(this).contains("updated")?"Conectado à TIM neste aparelho":"Conecte sua conta para mostrar os dados reais.";}
    void preferDisplayRate(){
        Display display=getWindowManager().getDefaultDisplay();Display.Mode current=display.getMode(),best=current;
        for(Display.Mode mode:display.getSupportedModes())if(mode.getPhysicalWidth()==current.getPhysicalWidth()&&mode.getPhysicalHeight()==current.getPhysicalHeight()&&mode.getRefreshRate()>best.getRefreshRate())best=mode;
        WindowManager.LayoutParams params=getWindow().getAttributes();params.preferredDisplayModeId=best.getModeId();params.preferredRefreshRate=best.getRefreshRate();getWindow().setAttributes(params);
        android.util.Log.i("AshSmoke","Requested display Hz="+best.getRefreshRate());
    }
    void connect(){
        shell("Login");
        if(Store.hasCredentials(this)){
            root.addView(label(Store.prefs(this).getBoolean("needsLogin",false)?"Reconecte sua conta TIM.":"Conta TIM salva neste aparelho.",17,0xFFB9CDDD));
            button("Reconectar à TIM",true,()->authenticate());
        }
        String loginError=Store.prefs(this).getString("error","");if(!loginError.isEmpty())root.addView(label(loginError,15,0xFFB9CDDD));
        root.addView(label("Os dados são consultados pelo celular. Seu número e senha ficam protegidos pelo Android para renovar a conexão.",16,0xFFB9CDDD));
        EditText number=new EditText(this);number.setHint("Número TIM com DDD");number.setTextColor(Color.WHITE);number.setHintTextColor(0xFFB9CDDD);number.setInputType(InputType.TYPE_CLASS_PHONE);number.setMinHeight(dp(56));root.addView(number);
        EditText password=new EditText(this);password.setHint("Senha Meu TIM");password.setTextColor(Color.WHITE);password.setHintTextColor(0xFFB9CDDD);password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);password.setMinHeight(dp(56));root.addView(password);
        button("Conectar e salvar neste aparelho",true,()->{
            String phone=number.getText().toString().replaceAll("\\D","");String pass=password.getText().toString();
            if(phone.length()!=11||pass.isEmpty()){number.setError("Informe o número com DDD e a senha");return;}
            try{Store.credentials(this,phone,pass);password.setText("");authenticate();}catch(Exception e){new AlertDialog.Builder(this).setMessage("Não foi possível proteger a conta neste aparelho.").setPositiveButton("Entendi",null).show();}
        });
    }
    void authenticate(){
        shell("Login");
        root.addView(label("Conectando à TIM…",22,Color.WHITE));
        root.addView(label("Login no site oficial. Aguarde a consulta da franquia.",14,0xFFB9CDDD));
        login=new TimLogin(this);login.start(root,ok->{login=null;busy=false;if(!destroyed){if(ok)home();else connect();}});
    }
    @Override protected void onPause(){super.onPause();if(preview!=null)preview.active(false);}
    @Override protected void onResume(){super.onResume();if(preview!=null)preview.active(true);}
    @Override protected void onDestroy(){destroyed=true;Store.prefs(this).unregisterOnSharedPreferenceChangeListener(changes);ui.removeCallbacks(redraw);if(preview!=null)preview.active(false);if(login!=null)login.cancel();super.onDestroy();}
    @Override public void onBackPressed(){openTab(0);}
}
