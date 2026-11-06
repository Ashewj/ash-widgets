package br.com.widgetpoco;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Shared 24-unit outline icons, rendered sharply at every display density. */
final class DockIcon extends Drawable {
    private final int kind;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    DockIcon(int kind,int color){
        this.kind=kind;paint.setColor(color);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
    }
    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();canvas.save();canvas.translate(bounds.left,bounds.top);canvas.scale(bounds.width()/24f,bounds.height()/24f);
        if(kind==0){
            Path house=new Path();house.moveTo(3,10);house.lineTo(12,3);house.lineTo(21,10);house.lineTo(21,20);house.quadTo(21,21,20,21);house.lineTo(15,21);house.lineTo(15,14);house.lineTo(9,14);house.lineTo(9,21);house.lineTo(4,21);house.quadTo(3,21,3,20);house.close();canvas.drawPath(house,paint);
        }else if(kind==1){
            canvas.drawLine(5,3,5,7,paint);canvas.drawLine(5,11,5,21,paint);canvas.drawCircle(5,9,2,paint);
            canvas.drawLine(12,3,12,13,paint);canvas.drawLine(12,17,12,21,paint);canvas.drawCircle(12,15,2,paint);
            canvas.drawLine(19,3,19,6,paint);canvas.drawLine(19,10,19,21,paint);canvas.drawCircle(19,8,2,paint);
        }else if(kind==2){
            for(int row=0;row<2;row++)for(int col=0;col<2;col++)canvas.drawRoundRect(new RectF(3+col*11,3+row*11,10+col*11,10+row*11),1.5f,1.5f,paint);
        }else{
            canvas.drawCircle(12,7,4,paint);Path shoulders=new Path();shoulders.moveTo(4,21);shoulders.lineTo(4,19);shoulders.cubicTo(4,12,20,12,20,19);shoulders.lineTo(20,21);canvas.drawPath(shoulders,paint);
        }
        canvas.restore();
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
