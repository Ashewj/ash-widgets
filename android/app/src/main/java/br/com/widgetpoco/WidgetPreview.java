package br.com.widgetpoco;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.Interpolator;
import android.widget.*;

/** Custom vertical paging inside the app; the launcher widget stays a single card. */
final class WidgetPreview extends FrameLayout {
    private final ImageView background;
    private final ImageView[] pages=new ImageView[2];
    private final Interpolator easeOutQuart=t->1-(float)Math.pow(1-t,4);
    private LiveSmokeView smoke;
    private AnimatorSet transition;
    private boolean active=true, dragging;
    private int selected;
    private float downX, downY;
    private final int slop;

    WidgetPreview(Context c){
        super(c);setFocusable(true);setClickable(true);setClipChildren(true);setClipToOutline(true);
        setBackgroundColor(0xFF101C2C);
        setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View v,Outline outline){outline.setRoundRect(0,0,v.getWidth(),v.getHeight(),v.getWidth()*30/720f);}});
        slop=ViewConfiguration.get(c).getScaledTouchSlop();
        background=new ImageView(c);background.setScaleType(ImageView.ScaleType.FIT_XY);
        background.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(background,new LayoutParams(-1,-1));
        for(int i=0;i<pages.length;i++){
            pages[i]=new ImageView(c);pages[i].setScaleType(ImageView.ScaleType.FIT_CENTER);
            pages[i].setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(pages[i],new LayoutParams(-1,-1));
        }
        refresh();
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);super.onMeasure(widthSpec,MeasureSpec.makeMeasureSpec(Math.round(width*320/720f),MeasureSpec.EXACTLY));}
    void refresh(){
        Bitmap bg=Bitmap.createBitmap(720,320,Bitmap.Config.ARGB_8888);
        CardBackground.draw(getContext(),new Canvas(bg),new Paint(Paint.ANTI_ALIAS_FLAG),0);background.setImageBitmap(bg);
        if(CardBackground.smoky(getContext())){
            if(smoke==null){smoke=new LiveSmokeView(getContext());addView(smoke,1,new LayoutParams(-1,-1));}
            smoke.active(active);
        }else if(smoke!=null){smoke.active(false);removeView(smoke);smoke=null;}
        for(int i=0;i<pages.length;i++)pages[i].setImageBitmap(DataWidget.stackForeground(getContext(),i==1));
        if(transition==null)settle();
    }
    private void settle(){
        for(int i=0;i<pages.length;i++){pages[i].setTranslationY(0);pages[i].setAlpha(1f);pages[i].setVisibility(i==selected?VISIBLE:INVISIBLE);}
        setContentDescription((selected==0?"Internet TIM":"Em desenvolvimento")+". Deslize para cima ou para baixo para trocar de pagina.");
    }
    private void changePage(boolean upward){
        if(transition!=null)return;
        ImageView outgoing=pages[selected],incoming=pages[1-selected];selected=1-selected;
        if(!ValueAnimator.areAnimatorsEnabled()){settle();return;}
        float distance=getHeight()*(upward?1:-1);
        incoming.setVisibility(VISIBLE);incoming.setTranslationY(distance);incoming.setAlpha(0f);
        AnimatorSet move=new AnimatorSet();move.playTogether(ObjectAnimator.ofFloat(outgoing,View.TRANSLATION_Y,0,-distance),ObjectAnimator.ofFloat(incoming,View.TRANSLATION_Y,distance,0));move.setDuration(120);move.setInterpolator(easeOutQuart);
        AnimatorSet fade=new AnimatorSet();fade.playTogether(ObjectAnimator.ofFloat(outgoing,View.ALPHA,1,0),ObjectAnimator.ofFloat(incoming,View.ALPHA,0,1));fade.setDuration(160);fade.setInterpolator(easeOutQuart);
        transition=new AnimatorSet();transition.playTogether(move,fade);
        transition.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator animation){transition=null;settle();}});transition.start();
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                downX=event.getX();downY=event.getY();dragging=false;
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;
            case MotionEvent.ACTION_MOVE:
                float dy=event.getY()-downY,dx=event.getX()-downX;
                if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx))dragging=true;
                return true;
            case MotionEvent.ACTION_UP:
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                float travel=event.getY()-downY;
                if(dragging&&Math.abs(travel)>Math.max(slop*2,getHeight()*.12f))changePage(travel<0);
                else if(!dragging)performClick();return true;
            case MotionEvent.ACTION_CANCEL:
                dragging=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){
        if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){changePage(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);return true;}
        return super.performAccessibilityAction(action,args);
    }
    @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(true);info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
    void active(boolean value){active=value;if(smoke!=null)smoke.active(value);}
    @Override protected void onDetachedFromWindow(){if(transition!=null)transition.cancel();active(false);super.onDetachedFromWindow();}
}
