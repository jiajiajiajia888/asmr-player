package com.longjiahao.asmrplayer;

import android.app.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Live queue controls; reordering does not prepare or seek the playing media. */
public final class QueuePanel {
 private final Activity activity;private final QueuePlayer queue;private final Runnable changed;
 private final Handler main=new Handler(Looper.getMainLooper());
 private LinearLayout rows;private Button mode;private Dialog dialog;private int index=-2,orderMode=-1;private List<MediaRef> displayed=Collections.emptyList(),pending=Collections.emptyList();
 private final Runnable tick=new Runnable(){public void run(){if(dialog==null || !dialog.isShowing())return;if(index!=queue.order.index() || orderMode!=queue.order.mode() || !displayed.equals(queue.order.items()) || !pending.equals(queue.order.nextUp()))render();main.postDelayed(this,500);}};
 public QueuePanel(Activity activity,QueuePlayer queue,Runnable changed){this.activity=activity;this.queue=queue;this.changed=changed;}
 public void close(){main.removeCallbacks(tick);if(dialog!=null)dialog.dismiss();}
 public void show(){
  LinearLayout panel=column();panel.setPadding(dp(16),dp(18),dp(16),dp(12));panel.setBackground(LibraryStyle.shape(activity,0xFF102021,22));
  LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);header.addView(LibraryStyle.text(activity,"播放列表",23,true),new LinearLayout.LayoutParams(0,-2,1));Button close=button("关闭");header.addView(close);panel.addView(header);
  mode=button("");LibraryStyle.card(mode,false);panel.addView(mode,new LinearLayout.LayoutParams(-1,dp(48)));mode.setOnClickListener(v->chooseMode());
  TextView hint=LibraryStyle.text(activity,"点卡片播放，用上移／下移调整顺序",13,false);hint.setPadding(0,dp(14),0,dp(12));panel.addView(hint);
  ScrollView scroll=new ScrollView(activity);rows=column();scroll.addView(rows);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  Button next=button("播放下一首");panel.addView(next,new LinearLayout.LayoutParams(-1,dp(48)));next.setOnClickListener(v->{queue.next(true);changed.run();render();});
  dialog=new Dialog(activity);dialog.setContentView(panel);close.setOnClickListener(v->dialog.dismiss());dialog.setOnDismissListener(d->main.removeCallbacks(tick));dialog.show();android.view.Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setGravity(Gravity.BOTTOM);window.setLayout(-1,Math.min(dp(620),activity.getResources().getDisplayMetrics().heightPixels-dp(80)));}render();main.postDelayed(tick,500);
 }
 private void chooseMode(){new AlertDialog.Builder(activity).setTitle("播放模式").setSingleChoiceItems(PlaybackOrder.LABELS,queue.order.mode(),(d,which)->{queue.setMode(which);changed.run();render();d.dismiss();}).setNegativeButton("返回",null).show();}
 private void render(){
  index=queue.order.index();orderMode=queue.order.mode();displayed=queue.order.items();pending=queue.order.nextUp();mode.setText("播放模式："+PlaybackOrder.LABELS[orderMode]+"  ▾");rows.removeAllViews();
  if(!pending.isEmpty()){label("优先下一首 · "+pending.size()+"项");for(int i=0;i<pending.size();i++)card(pending.get(i),i,pending.size(),true);}
  label("当前队列 · "+displayed.size()+"项");
  if(displayed.isEmpty()){TextView empty=LibraryStyle.text(activity,"还没有播放队列，先从列表选择一项",15,false);empty.setPadding(dp(16),dp(24),dp(16),dp(24));LibraryStyle.card(empty,false);LibraryStyle.addCard(rows,empty);}
  for(int i=0;i<displayed.size();i++)card(displayed.get(i),i,displayed.size(),false);
 }
 private void card(MediaRef item,int position,int size,boolean priority){
  LinearLayout card=column();card.setPadding(dp(12),dp(12),dp(12),dp(8));LibraryStyle.card(card,!priority && position==index);
  LinearLayout top=new LinearLayout(activity);top.setGravity(Gravity.CENTER_VERTICAL);top.addView(LibraryStyle.badge(activity,item.online()?item.platform():item.video()?"video":"music",LibraryStyle.ACCENT),new LinearLayout.LayoutParams(dp(44),dp(44)));
  LinearLayout labels=column();labels.setPadding(dp(12),0,0,0);TextView name=LibraryStyle.text(activity,item.title(),16,true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(name);TextView detail=LibraryStyle.text(activity,(!priority && position==index?"正在播放 · ":priority?"待播 · ":(position+1)+" · ")+LibraryStyle.type(item),12,false);detail.setPadding(0,dp(7),0,0);labels.addView(detail);top.addView(labels,new LinearLayout.LayoutParams(0,-2,1));card.addView(top);
  if(!priority)card.setOnClickListener(v->{int selected=locate(queue.order.items(),item,position);if(selected>=0){queue.select(selected);changed.run();}render();});
  LinearLayout controls=new LinearLayout(activity);controls.setGravity(Gravity.END);Button up=button("↑ 上移"),down=button("↓ 下移");up.setEnabled(position>0);down.setEnabled(position<size-1);up.setAlpha(position>0?1:.35f);down.setAlpha(position<size-1?1:.35f);controls.addView(up,new LinearLayout.LayoutParams(dp(92),dp(48)));controls.addView(down,new LinearLayout.LayoutParams(dp(92),dp(48)));card.addView(controls);
  up.setOnClickListener(v->move(item,position,-1,priority));down.setOnClickListener(v->move(item,position,1,priority));LibraryStyle.addCard(rows,card);
 }
 private int locate(List<MediaRef> list,MediaRef item,int hint){if(hint>=0 && hint<list.size() && list.get(hint).uri().equals(item.uri()))return hint;for(int i=0;i<list.size();i++)if(list.get(i).uri().equals(item.uri()))return i;return -1;}
 private void move(MediaRef item,int hint,int direction,boolean priority){List<MediaRef> fresh=priority?queue.order.nextUp():queue.order.items();int from=locate(fresh,item,hint),to=from+direction;if(from>=0 && to>=0 && to<fresh.size()){if(priority)queue.order.moveNextUp(from,to);else queue.order.move(from,to);changed.run();}render();}
 private void label(String value){TextView label=LibraryStyle.text(activity,value,15,true);label.setPadding(0,dp(10),0,dp(12));rows.addView(label);}
 private Button button(String value){Button b=new Button(activity);b.setText(value);b.setTextColor(LibraryStyle.ACCENT);b.setTextSize(14);b.setAllCaps(false);b.setMinimumWidth(0);b.setMinimumHeight(0);b.setElevation(0);b.setStateListAnimator(null);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x2292D8C0),LibraryStyle.shape(activity,Color.TRANSPARENT,12),null));return b;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private int dp(int value){return LibraryStyle.dp(activity,value);}
}
