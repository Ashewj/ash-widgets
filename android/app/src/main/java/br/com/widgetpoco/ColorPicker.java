package br.com.widgetpoco;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.util.Locale;

/** Native HSV picker, with hex input for precise colors; rendering waits until Apply. */
final class ColorPicker extends View {
    interface Selected {void accept(int color);}
    private final float[] hsv={200,0.5f,1};
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean draggingHue;
    private Runnable changed;
    ColorPicker(Context c,int color){super(c);Color.colorToHSV(color,hsv);setContentDescription("Seletor de cor: saturação e brilho acima, matiz abaixo");}
    int color(){return Color.HSVToColor(hsv);}
    private float density(){return getResources().getDisplayMetrics().density;}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),(int)(260*density()));}
    @Override protected void onDraw(Canvas canvas){
        float pad=12*density(), width=getWidth()-pad*2, bottom=getHeight()-60*density();
        paint.setShader(new LinearGradient(pad,0,pad+width,0,Color.WHITE,Color.HSVToColor(new float[]{hsv[0],1,1}),Shader.TileMode.CLAMP));
        canvas.drawRect(pad,pad,pad+width,bottom,paint);
        paint.setShader(new LinearGradient(0,pad,0,bottom,Color.TRANSPARENT,Color.BLACK,Shader.TileMode.CLAMP));canvas.drawRect(pad,pad,pad+width,bottom,paint);
        int[] hues=new int[7];for(int i=0;i<7;i++)hues[i]=Color.HSVToColor(new float[]{i*60,1,1});
        paint.setShader(new LinearGradient(pad,0,pad+width,0,hues,null,Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(pad,bottom+24*density(),pad+width,bottom+44*density()),8*density(),8*density(),paint);
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3*density());paint.setColor(Color.BLACK);
        float x=pad+width*hsv[1],y=pad+(bottom-pad)*(1-hsv[2]);canvas.drawCircle(x,y,8*density(),paint);
        paint.setStrokeWidth(2*density());paint.setColor(Color.WHITE);canvas.drawCircle(x,y,8*density(),paint);
        canvas.drawCircle(pad+width*hsv[0]/360,bottom+34*density(),12*density(),paint);paint.setStyle(Paint.Style.FILL);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        float pad=12*density(),width=getWidth()-pad*2,bottom=getHeight()-60*density();
        if(event.getAction()==MotionEvent.ACTION_DOWN){draggingHue=event.getY()>bottom;getParent().requestDisallowInterceptTouchEvent(true);}
        if(event.getAction()==MotionEvent.ACTION_DOWN||event.getAction()==MotionEvent.ACTION_MOVE||event.getAction()==MotionEvent.ACTION_UP){
            float x=Math.max(0,Math.min(1,(event.getX()-pad)/width));
            if(draggingHue)hsv[0]=Math.min(359.99f,x*360);else{hsv[1]=x;hsv[2]=1-Math.max(0,Math.min(1,(event.getY()-pad)/(bottom-pad)));}
            invalidate();if(changed!=null)changed.run();if(event.getAction()==MotionEvent.ACTION_UP)performClick();return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private static android.graphics.drawable.GradientDrawable surface(int color,int radius){
        android.graphics.drawable.GradientDrawable drawable=new android.graphics.drawable.GradientDrawable();drawable.setColor(color);drawable.setCornerRadius(radius);return drawable;
    }
    private static TextView action(Context c,String title,int textColor,int background,int pad){
        TextView view=new TextView(c);view.setText(title);view.setTextColor(textColor);view.setTextSize(14);view.setGravity(Gravity.CENTER);view.setTypeface(Typeface.create("sans-serif-medium",0));
        view.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x336EBFE5),surface(background,pad*2/3),null));view.setFocusable(true);view.setClickable(true);return view;
    }
    static void show(Context c,int initial,Selected selected){
        float density=c.getResources().getDisplayMetrics().density;int pad=Math.round(24*density),gap=Math.round(12*density);
        LinearLayout content=new LinearLayout(c);content.setOrientation(1);content.setPadding(pad,pad,pad,pad);
        TextView title=new TextView(c);title.setText("Cor do Smoky e do anel");title.setTextColor(0xFFF4F6F8);title.setTextSize(20);title.setTypeface(Typeface.create("sans-serif-medium",0));content.addView(title);
        ColorPicker picker=new ColorPicker(c,initial);LinearLayout.LayoutParams pickerLayout=new LinearLayout.LayoutParams(-1,-2);pickerLayout.topMargin=gap;content.addView(picker,pickerLayout);
        LinearLayout colorRow=new LinearLayout(c);colorRow.setGravity(Gravity.CENTER_VERTICAL);colorRow.setPadding(gap,0,gap,0);colorRow.setBackground(surface(0xFF101C2C,gap));content.addView(colorRow,new LinearLayout.LayoutParams(-1,Math.round(52*density)));
        View swatch=new View(c);swatch.setBackground(surface(initial,gap/2));colorRow.addView(swatch,new LinearLayout.LayoutParams(Math.round(26*density),Math.round(26*density)));
        EditText hex=new EditText(c);hex.setSingleLine(true);hex.setHint("#RRGGBB");hex.setContentDescription("Cor hexadecimal");hex.setTextColor(0xFFF4F6F8);hex.setHintTextColor(0xFFB6C7D8);hex.setTextSize(16);hex.setPadding(gap,0,0,0);hex.setBackgroundColor(Color.TRANSPARENT);hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);hex.setText(String.format(Locale.ROOT,"#%06X",initial&0xFFFFFF));colorRow.addView(hex,new LinearLayout.LayoutParams(0,-1,1));
        boolean[] syncing={false};
        picker.changed=()->{syncing[0]=true;hex.setText(String.format(Locale.ROOT,"#%06X",picker.color()&0xFFFFFF));swatch.setBackground(surface(picker.color(),gap/2));syncing[0]=false;};
        hex.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int n,int f){}public void onTextChanged(CharSequence s,int a,int before,int count){if(!syncing[0]&&s.toString().matches("#[0-9a-fA-F]{6}")){Color.colorToHSV(Color.parseColor(s.toString()),picker.hsv);picker.invalidate();swatch.setBackground(surface(picker.color(),gap/2));hex.setError(null);}}public void afterTextChanged(Editable e){}});
        TextView restore=action(c,"Restaurar azul",0xFF8DDEFF,Color.TRANSPARENT,pad);LinearLayout.LayoutParams restoreLayout=new LinearLayout.LayoutParams(-1,Math.round(48*density));restoreLayout.topMargin=gap/2;content.addView(restore,restoreLayout);
        LinearLayout buttons=new LinearLayout(c);LinearLayout.LayoutParams buttonsLayout=new LinearLayout.LayoutParams(-1,Math.round(52*density));buttonsLayout.topMargin=gap/2;content.addView(buttons,buttonsLayout);
        TextView cancel=action(c,"Cancelar",0xFFE1EBF4,0xFF324A60,pad),apply=action(c,"Aplicar",0xFF102333,0xFF8DDEFF,pad);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-1,1);left.rightMargin=gap;buttons.addView(cancel,left);buttons.addView(apply,new LinearLayout.LayoutParams(0,-1,1));
        AlertDialog dialog=new AlertDialog.Builder(c).create();dialog.setView(content,0,0,0,0);
        cancel.setOnClickListener(v->dialog.dismiss());restore.setOnClickListener(v->{selected.accept(CardBackground.DEFAULT_SMOKY_COLOR);dialog.dismiss();});
        apply.setOnClickListener(v->{if(!hex.getText().toString().matches("#[0-9a-fA-F]{6}")){hex.setError("Use #RRGGBB, por exemplo #8DDEFF");return;}selected.accept(picker.color());dialog.dismiss();});
        dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(surface(0xFF203044,Math.round(28*density)));window.setLayout(Math.min(c.getResources().getDisplayMetrics().widthPixels-Math.round(32*density),Math.round(400*density)),-2);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams params=window.getAttributes();params.dimAmount=.6f;window.setAttributes(params);}
    }
}
