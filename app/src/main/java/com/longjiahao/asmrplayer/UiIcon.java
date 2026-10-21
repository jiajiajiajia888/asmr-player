package com.longjiahao.asmrplayer;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Resolution independent controls drawn with a consistent stroke. */
public final class UiIcon extends Drawable {
    private final String kind;private final int color,seconds;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public UiIcon(String kind,int color){this(kind,color,0);}
    public UiIcon(String kind,int color,int seconds){this.kind=kind;this.color=color;this.seconds=seconds;}
    @Override public void draw(Canvas canvas){
        canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/32f,getBounds().height()/32f);
        paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        Path path=new Path();
        switch(kind){
            case "queue" -> {for(int y:new int[]{8,16,24}){canvas.drawCircle(5,y,1,paint);canvas.drawLine(11,y,27,y,paint);}}
            case "heart", "heartFilled" -> {if(kind.equals("heartFilled"))paint.setStyle(Paint.Style.FILL);path.moveTo(16,28);path.cubicTo(5,20,1,14,5,8);path.cubicTo(8,4,13,5,16,10);path.cubicTo(19,5,24,4,27,8);path.cubicTo(31,14,27,20,16,28);path.close();canvas.drawPath(path,paint);}
            case "online" -> {canvas.drawCircle(16,16,12,paint);canvas.drawOval(10,4,22,28,paint);canvas.drawLine(4,16,28,16,paint);}
            case "bili" -> {paint.setStyle(Paint.Style.FILL);paint.setColor(0xFFFB7299);canvas.drawCircle(16,16,15,paint);paint.setColor(Color.WHITE);paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));paint.setTextSize(22);paint.setTextAlign(Paint.Align.CENTER);Paint.FontMetrics fm=paint.getFontMetrics();canvas.drawText("B",16,16-(fm.ascent+fm.descent)/2,paint);}
            case "douyin" -> {paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF080A10);canvas.drawCircle(16,16,15,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);path.moveTo(17,21);path.lineTo(17,7);path.lineTo(24,11);paint.setColor(0xFF25F4EE);canvas.save();canvas.translate(-1,1);canvas.drawPath(path,paint);canvas.drawCircle(13,23,4,paint);canvas.restore();paint.setColor(0xFFFE2C55);canvas.save();canvas.translate(1,0);canvas.drawPath(path,paint);canvas.drawCircle(13,23,4,paint);canvas.restore();paint.setColor(Color.WHITE);canvas.drawPath(path,paint);canvas.drawCircle(13,23,4,paint);}
            case "play" -> {paint.setStyle(Paint.Style.FILL);path.moveTo(11,6);path.lineTo(26,16);path.lineTo(11,26);path.close();canvas.drawPath(path,paint);}
            case "pause" -> {paint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(9,6,13,26,1.5f,1.5f,paint);canvas.drawRoundRect(19,6,23,26,1.5f,1.5f,paint);}
            case "back", "forward" -> {paint.setStyle(Paint.Style.FILL);boolean back=kind.equals("back");for(int x:new int[]{5,17}){path.reset();if(back){path.moveTo(x+10,6);path.lineTo(x,16);path.lineTo(x+10,26);}else{path.moveTo(x,6);path.lineTo(x+10,16);path.lineTo(x,26);}path.close();canvas.drawPath(path,paint);}}
            case "orbit" -> {canvas.drawOval(3,8,29,24,paint);canvas.drawCircle(16,16,5,paint);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(28,14,3,paint);}
            case "music" -> {path.moveTo(13,23);path.lineTo(13,8);path.lineTo(25,5);path.lineTo(25,20);path.moveTo(13,12);path.lineTo(25,9);canvas.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);canvas.drawOval(5,21,14,28,paint);canvas.drawOval(17,18,26,25,paint);}
            case "video" -> {canvas.drawRoundRect(3,6,29,26,3,3,paint);path.moveTo(13,11);path.lineTo(22,16);path.lineTo(13,21);path.close();canvas.drawPath(path,paint);}
            case "channel" -> {path.moveTo(4,12);path.lineTo(9,12);path.lineTo(16,6);path.lineTo(16,26);path.lineTo(9,20);path.lineTo(4,20);path.close();canvas.drawPath(path,paint);canvas.drawArc(9,7,26,25,-65,130,false,paint);canvas.drawLine(28,8,28,24,paint);}
            case "pan" -> {canvas.drawLine(4,16,28,16,paint);path.moveTo(10,10);path.lineTo(4,16);path.lineTo(10,22);path.moveTo(22,10);path.lineTo(28,16);path.lineTo(22,22);canvas.drawPath(path,paint);}
            case "clock" -> {canvas.drawCircle(16,16,12,paint);canvas.drawLine(16,8,16,16,paint);canvas.drawLine(16,16,22,19,paint);}
            case "full" -> {path.moveTo(4,12);path.lineTo(4,4);path.lineTo(12,4);path.moveTo(20,4);path.lineTo(28,4);path.lineTo(28,12);path.moveTo(28,20);path.lineTo(28,28);path.lineTo(20,28);path.moveTo(12,28);path.lineTo(4,28);path.lineTo(4,20);canvas.drawPath(path,paint);}
            case "more" -> {paint.setStyle(Paint.Style.FILL);for(int y:new int[]{7,16,25})canvas.drawCircle(16,y,2,paint);}
            case "close" -> {canvas.drawLine(8,8,24,24,paint);canvas.drawLine(24,8,8,24,paint);}
            case "save" -> {path.moveTo(8,3);path.lineTo(20,3);path.lineTo(26,9);path.lineTo(26,29);path.lineTo(8,29);path.close();path.moveTo(20,3);path.lineTo(20,10);path.lineTo(26,10);canvas.drawPath(path,paint);}
            default -> {path.moveTo(12,7);path.lineTo(21,16);path.lineTo(12,25);canvas.drawPath(path,paint);}
        }
        canvas.restore();
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
