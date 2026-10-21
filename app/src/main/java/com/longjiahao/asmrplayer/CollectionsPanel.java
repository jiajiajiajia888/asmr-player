package com.longjiahao.asmrplayer;

import android.app.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class CollectionsPanel {
    public interface Host {void changed();void play(List<MediaRef> items,int index);void next(MediaRef item);}
    private final Activity activity;
    private final Host host;
    private final CollectionLibrary library;
    private LinearLayout root;
    private String selected;
    public CollectionsPanel(Activity activity,Host host){this.activity=activity;this.host=host;library=new CollectionLibrary(activity);}
    public View createView(){ScrollView scroll=new ScrollView(activity);root=column();root.setPadding(dp(18),dp(16),dp(18),dp(20));scroll.addView(root);render();return scroll;}
    public Button heart(MediaRef item){Button b=new CenteredIconButton(activity);b.setBackgroundColor(Color.TRANSPARENT);b.setElevation(0);b.setStateListAnimator(null);b.setMinimumWidth(0);b.setMinimumHeight(0);updateHeart(b,item);b.setOnClickListener(v->{library.toggleFavorite(item);updateHeart(b,item);render();host.changed();});return b;}
    private void updateHeart(Button b,MediaRef item){boolean filled=library.favorite(item.uri());((CenteredIconButton)b).setIcon(filled?"heartFilled":"heart",filled?0xFFEF5350:Color.LTGRAY,dp(22));b.setContentDescription((filled?"取消收藏 ":"收藏 ")+item.title());}
    public void addTo(MediaRef item){List<CollectionLibrary.Collection> lists=library.lists();String[] names=new String[lists.size()+1];for(int i=0;i<lists.size();i++)names[i]=lists.get(i).name();names[lists.size()]="新建清单";new AlertDialog.Builder(activity).setTitle("加入清单").setItems(names,(d,which)->{if(which==lists.size())nameDialog("新建清单","",name->{String id=library.create(name);library.add(id,item);render();host.changed();});else{library.add(lists.get(which).id(),item);render();host.changed();}}).setNegativeButton("返回",null).show();}
    public void information(MediaRef item){TextView address=LibraryStyle.text(activity,(item.online()?"在线分享地址：\n":"源文件地址：\n")+item.uri(),14,false);address.setTextIsSelectable(true);address.setPadding(dp(24),dp(16),dp(24),dp(16));new AlertDialog.Builder(activity).setTitle(item.title()).setView(address).setNeutralButton("复制地址",(d,w)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(android.content.ClipData.newPlainText("源地址",item.uri()));}).setPositiveButton("返回",null).show();}
    public void render(){
        if(root==null)return;
        root.removeAllViews();
        CollectionLibrary.Collection chosen=selected==null?null:library.get(selected);
        if(selected!=null && chosen==null){selected=null;render();return;}
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(LibraryStyle.text(activity,selected==null?"我的清单":chosen.name(),26,true),new LinearLayout.LayoutParams(0,-2,1));
        Button action=button(selected==null?"＋ 新建":"返回清单");
        action.setOnClickListener(v->{if(selected==null)nameDialog("新建清单","",name->{library.create(name);render();});else{selected=null;render();}});
        header.addView(action);root.addView(header);
        TextView intro=LibraryStyle.text(activity,selected==null?"把喜欢的声音，放在一起":chosen.items().size()+"个条目 · 音乐、视频和在线链接",13,false);
        intro.setPadding(0,dp(10),0,dp(22));root.addView(intro);
        if(selected==null){
            for(CollectionLibrary.Collection list:library.lists()){
                boolean favorite=list.id().equals(CollectionLibrary.FAVORITES);
                LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(20),dp(8),dp(20));LibraryStyle.card(row,false);
                row.addView(LibraryStyle.badge(activity,favorite?"heartFilled":"queue",favorite?0xFFEF737E:LibraryStyle.ACCENT),new LinearLayout.LayoutParams(dp(58),dp(58)));
                LinearLayout labels=column();labels.setPadding(dp(14),0,dp(8),0);
                labels.addView(LibraryStyle.text(activity,list.name(),19,true));
                TextView count=LibraryStyle.text(activity,list.items().size()+"个条目"+(favorite?" · 默认清单":""),13,false);count.setPadding(0,dp(8),0,0);labels.addView(count);
                row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
                if(!favorite){Button more=LibraryStyle.more(activity,list.name());row.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));more.setOnClickListener(v->manage(list));}
                else {ImageView arrow=new ImageView(activity);arrow.setImageDrawable(new UiIcon("chevron",LibraryStyle.MUTED));row.addView(arrow,new LinearLayout.LayoutParams(dp(20),dp(20)));}
                row.setOnClickListener(v->{selected=list.id();render();});LibraryStyle.addCard(root,row);
            }
            return;
        }
        if(chosen.items().isEmpty()){
            LinearLayout empty=column();empty.setGravity(Gravity.CENTER);empty.setPadding(dp(20),dp(32),dp(20),dp(32));LibraryStyle.card(empty,false);
            empty.addView(LibraryStyle.badge(activity,selected.equals(CollectionLibrary.FAVORITES)?"heart":"queue",LibraryStyle.ACCENT),new LinearLayout.LayoutParams(dp(64),dp(64)));
            TextView title=LibraryStyle.text(activity,"还没有加入声音",18,true);title.setPadding(0,dp(20),0,dp(12));empty.addView(title);
            TextView tip=LibraryStyle.text(activity,selected.equals(CollectionLibrary.FAVORITES)?"点音乐、视频或在线条目旁的爱心，即可收藏":"在条目的三个点菜单中选择“加入清单”",14,false);tip.setGravity(Gravity.CENTER);empty.addView(tip);LibraryStyle.addCard(root,empty);return;
        }
        LinearLayout playHeader=new LinearLayout(activity);playHeader.setGravity(Gravity.CENTER_VERTICAL);
        Button playAll=button("▶ 播放全部");playAll.setOnClickListener(v->{java.util.List<MediaRef> active=new java.util.ArrayList<>();for(MediaRef item:chosen.items())if(item.playable())active.add(item);if(active.isEmpty())android.widget.Toast.makeText(activity,"清单中没有可播放的条目",android.widget.Toast.LENGTH_SHORT).show();else host.play(active,0);});playHeader.addView(playAll);root.addView(playHeader);
        for(int i=0;i<chosen.items().size();i++){
            final int index=i;MediaRef item=chosen.items().get(i);
            LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(14),dp(4),dp(14));LibraryStyle.card(row,false);
            row.addView(LibraryStyle.badge(activity,item.online()?item.platform():item.video()?"video":"music",LibraryStyle.ACCENT),new LinearLayout.LayoutParams(dp(48),dp(48)));
            LinearLayout labels=column();labels.setPadding(dp(12),0,dp(4),0);TextView name=LibraryStyle.text(activity,item.title(),16,true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(name);
            TextView detail=LibraryStyle.text(activity,LibraryStyle.type(item),12,false);detail.setPadding(0,dp(7),0,0);labels.addView(detail);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(heart(item),new LinearLayout.LayoutParams(dp(48),dp(48)));Button more=LibraryStyle.more(activity,item.title());row.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));
            row.setOnClickListener(v->{if(item.playable())host.play(chosen.items(),index);else android.widget.Toast.makeText(activity,"1.7已移除抖音链接功能",android.widget.Toast.LENGTH_SHORT).show();});
            more.setOnClickListener(v->new AlertDialog.Builder(activity).setTitle(item.title()).setItems(new String[]{"加入清单","信息","下一首播放","移出此清单"},(d,which)->{if(which==0)addTo(item);else if(which==1)information(item);else if(which==2)host.next(item);else{library.remove(chosen.id(),item.uri());render();host.changed();}}).setNegativeButton("返回",null).show());
            LibraryStyle.addCard(root,row);
        }
    }

    private void manage(CollectionLibrary.Collection list){new AlertDialog.Builder(activity).setTitle(list.name()).setItems(new String[]{"重命名","删除清单"},(d,which)->{if(which==0)nameDialog("重命名清单",list.name(),name->{library.rename(list.id(),name);render();});else new AlertDialog.Builder(activity).setTitle("删除“"+list.name()+"”？").setMessage("只删除清单记录，源文件和在线列表保留。").setPositiveButton("删除清单",(dialog,w)->{library.removeCollection(list.id());render();}).setNegativeButton("取消",null).show();}).setNegativeButton("返回",null).show();}
    private interface NameAction {void run(String name);}
    private void nameDialog(String title,String initial,NameAction action){EditText input=new EditText(activity);input.setText(initial);input.setSingleLine();AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(title).setView(input).setPositiveButton("保存",null).setNegativeButton("取消",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String name=input.getText().toString().trim();if(name.isEmpty()){input.setError("请输入清单名称");return;}action.run(name);dialog.dismiss();}));dialog.show();}
    private Button button(String value){Button b=new Button(activity);b.setText(value);b.setTextColor(0xFF92D8C0);b.setBackgroundColor(Color.TRANSPARENT);b.setAllCaps(false);return b;}
    private TextView text(String value,int size){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}
