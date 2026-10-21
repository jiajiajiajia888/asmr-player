package com.longjiahao.asmrplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.Button;

/** Draw icons independently of TextView's left compound-drawable positioning. */
public final class CenteredIconButton extends Button {
 private UiIcon glyph;private int iconSize;private final Paint label=new Paint(Paint.ANTI_ALIAS_FLAG);
 public CenteredIconButton(Context context){super(context);setIncludeFontPadding(false);}
 public void setIcon(String kind,int color,int size){glyph=new UiIcon(kind,color);iconSize=size;invalidate();}
 @Override protected void onDraw(Canvas canvas){
  if(glyph==null){super.onDraw(canvas);return;}
  String caption=getText().toString();float density=getResources().getDisplayMetrics().density;
  label.setTextSize(getTextSize());label.setColor(getCurrentTextColor());label.setTypeface(getTypeface());label.setTextAlign(Paint.Align.CENTER);
  Paint.FontMetrics metrics=label.getFontMetrics();float textHeight=caption.isEmpty()?0:metrics.descent-metrics.ascent;
  float gap=caption.isEmpty()?0:3*density;float block=iconSize+gap+textHeight;
  int x=Math.round((getWidth()-iconSize)/2f),y=Math.round((getHeight()-block)/2f);
  glyph.setBounds(x,y,x+iconSize,y+iconSize);glyph.draw(canvas);
  if(!caption.isEmpty())canvas.drawText(caption,getWidth()/2f,y+iconSize+gap-metrics.ascent,label);
 }
}
