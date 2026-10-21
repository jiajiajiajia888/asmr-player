package com.longjiahao.asmrplayer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Shared native card and control styles for all media lists. */
public final class LibraryStyle {
 public static final int ACCENT=0xFF92D8C0,CARD=0xFF192B2C,ACTIVE=0xFF25423F,MUTED=0xFF9BB1AE;
 private LibraryStyle(){}
 public static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 public static GradientDrawable shape(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
 public static void card(View v,boolean active){Context c=v.getContext();GradientDrawable d=shape(c,active?ACTIVE:CARD,18);if(active)d.setStroke(dp(c,1),0x6692D8C0);v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x2292D8C0),d,null));v.setClipToOutline(true);}
 public static Button more(Context c,String title){CenteredIconButton b=new CenteredIconButton(c);b.setIcon("more",MUTED,dp(c,20));b.setText("");b.setPadding(0,0,0,0);b.setMinimumWidth(0);b.setMinimumHeight(0);b.setElevation(0);b.setStateListAnimator(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x2292D8C0),shape(c,Color.TRANSPARENT,12),null));b.setContentDescription(title+"操作");return b;}
 public static TextView text(Context c,String value,int size,boolean bold){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(bold?Color.WHITE:MUTED);t.setIncludeFontPadding(false);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 public static ImageView badge(Context c,String glyph,int color){ImageView image=new ImageView(c);image.setImageDrawable(new UiIcon(glyph,color));image.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));image.setBackground(shape(c,0xFF233D39,14));return image;}
 public static void addCard(LinearLayout parent,View card){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(parent.getContext(),10);parent.addView(card,p);}
 public static String type(MediaRef item){return item.online()?(item.platform().equals("bili")?"B站 · 在线":"抖音 · 不再支持"):item.video()?"视频 · 源文件":"音乐 · 源文件";}
}
