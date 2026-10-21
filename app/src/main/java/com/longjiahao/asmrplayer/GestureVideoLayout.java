package com.longjiahao.asmrplayer;
import android.content.Context;
import android.widget.FrameLayout;
/** Gesture video surface retains an accessibility click action. */
public final class GestureVideoLayout extends FrameLayout {
 public GestureVideoLayout(Context context){super(context);}
 @Override public boolean performClick(){return super.performClick();}
}
