package com.longjiahao.asmrplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Switch;
import android.view.WindowManager;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.ArrayList;
import java.util.Locale;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class MainActivity extends Activity {
    private static final int[] MODE_IDS={R.id.channel_original,R.id.channel_left,R.id.channel_right,R.id.channel_mono,R.id.channel_auto};
    private static final int PICK_FILE = 101;
    private static final int SAVE_FILE = 102;
    private static final int BG = Color.rgb(16,26,29), PANEL = Color.rgb(26,39,43), ACCENT = Color.rgb(146,216,192);
    private ExoPlayer player;
    private TextView fileName, status, clock;
    private Button playButton,miniPlayButton,fullscreenButton;
    private android.widget.FrameLayout shell,videoBox,fullVideo;
    private GestureVideoLayout fullscreenLayer;
    private LinearLayout appFrame,miniPlayer,channelPanel,movingPanel;
    private TextView durationLabel,channelSummary,movingSummary,miniName,miniState,spatialPath,gestureHud;
    private float swipeStartX,swipeStartY;private long swipeStartPosition,swipeTarget;private boolean swiping;
    private final Runnable hideGestureHud=()->{if(gestureHud!=null)gestureHud.setVisibility(View.GONE);};
    private boolean fullscreen,restoreFullscreen;
    private int previousOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private int activePage;
    private android.widget.FrameLayout fullChrome;
    private android.window.OnBackInvokedCallback fullBack;
    private final Runnable hideFullControls=()->{if(fullscreen && !swiping && player!=null && player.isPlaying())fullChrome.setVisibility(View.GONE);};
    private double videoRatio=16.0/9;
    private final ExecutorService mediaReader=Executors.newSingleThreadExecutor();
    private final ExecutorService fileLookup=Executors.newSingleThreadExecutor();
    private boolean detailsBusy;
    private final java.util.HashMap<String,String> durationCache=new java.util.HashMap<>();
    private final android.util.LruCache<String,android.graphics.Bitmap> thumbnailCache=new android.util.LruCache<>(8*1024*1024){@Override protected int sizeOf(String key,android.graphics.Bitmap value){return value.getAllocationByteCount();}};
    private final Runnable readVisibleDetails=this::loadVisibleDetails;
    private long queuedSeek=C.TIME_UNSET;
    private final Runnable commitSeek=()->{long target=queuedSeek;queuedSeek=C.TIME_UNSET;if(player!=null && target!=C.TIME_UNSET && player.isCurrentMediaItemSeekable())player.seekTo(target);};
    private SeekBar seekBar;
    private SurfaceView surface;
    private Uri selectedUri;
    private boolean dragging;
    private SoundProcessor soundProcessor=new SoundProcessor();
    private PlaybackService playbackService;
    private MediaLibrary library;
    private CollectionsPanel collectionsPanel;private Button queueButton,playModeButton,miniModeButton;private QueuePanel queuePanel;
    private OnlinePanel onlinePanel;private boolean onlinePlayback;
    private OnlineResolver.Result pendingOnline;private String pendingOnlineLink,pendingOnlineTitle,pendingRestoreOnline,pendingRestoreTitle;private long pendingOnlinePosition,pendingRestorePosition;
    private boolean bound,visible,pendingSource,pendingPlay;
    private long pendingPosition;
    private TextView timerStatus;
    private final View[] pages=new View[5];
    private final Button[] tabs=new Button[5];
    private final LinearLayout[] libraryRows=new LinearLayout[2];
    private final MediaExport exporter=new MediaExport();
    private final ExecutorService writer=Executors.newSingleThreadExecutor();
    private RadioGroup channelMode;
    private Switch moving,surround;
    private SeekBar cycle,width,autoThreshold,distance,alternateSpeed;
    private RadioGroup spatialMode;private TextView distanceLabel,alternateSpeedLabel;private LinearLayout spatialControls,alternateControls;
    private TextView autoThresholdLabel;
    private TextView effectLabel,exportStatus;
    private Button saveButton,cancelButton,retrySave;
    private File pendingExport;
    private String originalName="文件",exportName,exportMime;
    private boolean exporting,copying,destroyed,nameResolved;
    private final java.util.concurrent.ConcurrentHashMap<String,String> nameCache=new java.util.concurrent.ConcurrentHashMap<>();
    private final ArrayList<Button> seekButtons = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable update = new Runnable() {
        @Override public void run() { refresh(); if(exporting){int p=exporter.progress();exportStatus.setText(p>=0?"正在处理："+p+"%":"正在准备处理…");}handler.postDelayed(this, 250); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        library=new MediaLibrary(this);
        collectionsPanel=new CollectionsPanel(this,new CollectionsPanel.Host(){public void changed(){if(libraryRows[0]!=null)renderLibrary(false);if(libraryRows[1]!=null)renderLibrary(true);if(onlinePanel!=null)onlinePanel.refreshLibrary();}public void play(java.util.List<MediaRef> items,int index){startQueue(items,index);}public void next(MediaRef item){enqueueNext(item);}});
        buildUi();
        bindPlayback();
        restoreFullscreen=state!=null && state.getBoolean("fullscreen",false);if(state!=null)previousOrientation=state.getInt("previousOrientation",android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if(state != null && state.getString("uri") != null) {
            String restore=state.getString("uri");if(restore.startsWith("https://")){pendingRestoreOnline=restore;pendingRestoreTitle=state.getString("onlineTitle","在线视频");pendingRestorePosition=state.getLong("position",0);}else openFile(Uri.parse(restore),state.getLong("position",0));
        }
        android.content.SharedPreferences prefs=getPreferences(MODE_PRIVATE);
        String pending=prefs.getString("pendingFile",null);
        if(pending!=null){File f=new File(getCacheDir(),"asmr-processing.mp4");if(f.isFile() && f.length()>0 && f.length()==prefs.getLong("pendingBytes",-1) && f.getAbsolutePath().equals(pending)){pendingExport=f;exportName=prefs.getString("pendingName","改-文件.m4a");exportMime=prefs.getString("pendingMime","audio/mp4");retrySave.setVisibility(View.VISIBLE);exportStatus.setText("有已处理结果，可点击“保存刚才的结果”继续保存。");}}
    }

    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);onlinePanel.acceptIntent(intent);}
    private final Player.Listener playerListener=new Player.Listener() {
            @Override public void onPlaybackStateChanged(int playbackState) {
                status.setText(switch(playbackState) {
                    case Player.STATE_BUFFERING -> "正在读取文件…";
                    case Player.STATE_READY -> player.isPlaying() ? "正在播放" : "已准备，可以播放";
                    case Player.STATE_ENDED -> "播放结束";
                    case Player.STATE_IDLE -> selectedUri == null ? "请选择音频或视频文件" : "等待播放";
                    default -> "等待播放";
                });
                refresh();if(playbackState==Player.STATE_READY)scheduleDetails();
            }
            @Override public void onIsPlayingChanged(boolean playing) {
                if(player.getPlaybackState() == Player.STATE_READY) status.setText(playing ? "正在播放" : "已暂停");
                if(fullscreen){fullChrome.setVisibility(View.VISIBLE);armFullscreenControls();}refresh();
            }
            @Override public void onTracksChanged(Tracks tracks) {
                updateVideoVisibility();
            }
            @Override public void onVideoSizeChanged(androidx.media3.common.VideoSize size){if(size.height>0 && size.width>0){videoRatio=size.width*size.pixelWidthHeightRatio/(double)size.height;fitVideo();}}
            @Override public void onPlayerError(PlaybackException error) {
                status.setText("无法播放此文件：可能是编码不支持、文件损坏或读取权限失效。请尝试其他文件。");
                refresh();
            }
        };
    private final android.content.ServiceConnection connection=new android.content.ServiceConnection(){
        public void onServiceConnected(android.content.ComponentName name,android.os.IBinder binder){
            playbackService=((PlaybackService.LocalBinder)binder).service();player=playbackService.player;soundProcessor=playbackService.processor;
            updateEffects();player.addListener(playerListener);if(visible)player.setVideoSurfaceView(surface);
            if(pendingRestoreOnline!=null){if(playbackService.sourceUri==null)onlinePanel.restore(pendingRestoreOnline,pendingRestoreTitle,pendingRestorePosition);pendingRestoreOnline=null;}
            if(pendingOnline!=null){OnlineResolver.Result next=pendingOnline;pendingOnline=null;openOnline(next,pendingOnlineLink,pendingOnlineTitle,pendingOnlinePosition);}
            else if(pendingSource && playbackService.sourceUri==null)openFile(selectedUri,pendingPosition);
            else if(playbackService.sourceUri!=null){onlinePlayback=playbackService.onlineUrl!=null;selectedUri=Uri.parse(playbackService.sourceUri);originalName=playbackService.sourceName;nameResolved=!"正在读取文件名…".equals(originalName);fileName.setText(originalName);updateLibrarySelection();pendingSource=false;}
            if(pendingPlay){player.play();pendingPlay=false;}
            updateVideoVisibility();androidx.media3.common.VideoSize size=player.getVideoSize();if(size.height>0)videoRatio=size.width*size.pixelWidthHeightRatio/(double)size.height;fitVideo();
            playbackService.checkTimer();playerListener.onPlaybackStateChanged(player.getPlaybackState());refresh();
        }
        public void onServiceDisconnected(android.content.ComponentName name){player=null;playbackService=null;status.setText("播放服务已断开，请重新打开软件。");}
    };
    private void bindPlayback(){Intent intent=new Intent(this,PlaybackService.class).setAction(PlaybackService.LOCAL_BIND);bound=bindService(intent,connection,Context.BIND_AUTO_CREATE);}

    private void buildUi(){
        shell=new android.widget.FrameLayout(this);shell.setBackgroundColor(BG);
        appFrame=new LinearLayout(this);appFrame.setOrientation(LinearLayout.VERTICAL);shell.addView(appFrame,new android.widget.FrameLayout.LayoutParams(-1,-1));
        android.widget.FrameLayout content=new android.widget.FrameLayout(this);appFrame.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout root=column(18);scroll.addView(root);pages[0]=scroll;content.addView(scroll);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);TextView title=text("asmr播放器",25,Color.WHITE,true);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button choose=button("选文件",false);choose.setTextSize(14);heading.addView(choose,new LinearLayout.LayoutParams(dp(82),dp(48)));choose.setOnClickListener(v->selectFile());root.addView(heading);gap(root,16);
        videoBox=new android.widget.FrameLayout(this);videoBox.setBackground(round(Color.BLACK,12));videoBox.setVisibility(View.GONE);root.addView(videoBox,new LinearLayout.LayoutParams(-1,dp(190)));
        surface=new SurfaceView(this);videoBox.addView(surface,new android.widget.FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        fullscreenButton=iconButton("full",false);fullscreenButton.setContentDescription("视频全屏");android.widget.FrameLayout.LayoutParams fl=new android.widget.FrameLayout.LayoutParams(dp(48),dp(48),Gravity.BOTTOM|Gravity.END);fl.setMargins(0,0,dp(6),dp(6));videoBox.addView(fullscreenButton,fl);fullscreenButton.setOnClickListener(v->enterFullscreen());videoBox.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fitVideo());
        gap(root,12);fileName=text("尚未选择文件",21,Color.WHITE,true);fileName.setMaxLines(2);root.addView(fileName);gap(root,4);status=text("请选择音频或视频文件",14,ACCENT,false);root.addView(status);gap(root,10);
        LinearLayout progress=new LinearLayout(this);progress.setGravity(Gravity.CENTER_VERTICAL);clock=text("00:00",12,Color.LTGRAY,false);durationLabel=text("00:00",12,Color.LTGRAY,false);progress.addView(clock);
        seekBar=new SeekBar(this);seekBar.setMax(10000);seekBar.setEnabled(false);progress.addView(seekBar,new LinearLayout.LayoutParams(0,dp(48),1));progress.addView(durationLabel);root.addView(progress);wireSeek(seekBar);
        root.addView(transport(),new LinearLayout.LayoutParams(-1,dp(64)));LinearLayout directions=new LinearLayout(this);TextView back=text("快退",12,Color.LTGRAY,false),forward=text("快进",12,Color.LTGRAY,false);forward.setGravity(Gravity.END);directions.addView(back,new LinearLayout.LayoutParams(0,-2,1));queueButton=button("播放列表",false);queueButton.setTextSize(13);queueButton.setContentDescription("展开播放列表");queueButton.setOnClickListener(v->showQueue());directions.addView(queueButton,new LinearLayout.LayoutParams(dp(108),dp(48)));directions.addView(forward,new LinearLayout.LayoutParams(0,-2,1));root.addView(directions);gap(root,8);playModeButton=button("播放模式：顺序播放 ▾",false);playModeButton.setTextSize(14);playModeButton.setOnClickListener(v->choosePlaybackMode());root.addView(playModeButton,new LinearLayout.LayoutParams(-1,dp(48)));gap(root,18);
        setupEffects();
        timerStatus=text("未设置",13,Color.LTGRAY,false);root.addView(settingRow("clock","定时停止",timerStatus,()->chooseTimer()));gap(root,8);
        channelSummary=text("保留原声道",13,Color.LTGRAY,false);root.addView(settingRow("channel","声道处理",channelSummary,()->showPanel("声道处理",channelPanel)));gap(root,8);
        movingSummary=text("未开启",13,Color.LTGRAY,false);root.addView(settingRow("orbit","环绕效果",movingSummary,()->showPanel("环绕效果",movingPanel)));updateEffects();gap(root,14);

        saveButton=button("处理并保存新文件",false);UiIcon saveIcon=icon("save",ACCENT,22);saveButton.setCompoundDrawables(saveIcon,null,null,null);saveButton.setCompoundDrawablePadding(dp(10));saveButton.setTextColor(ACCENT);saveButton.setEnabled(false);root.addView(saveButton);saveButton.setOnClickListener(v->startExport());
        retrySave=button("保存刚才的结果",false);retrySave.setVisibility(View.GONE);root.addView(retrySave);retrySave.setOnClickListener(v->chooseDestination());
        cancelButton=button("取消处理",false);cancelButton.setVisibility(View.GONE);root.addView(cancelButton);cancelButton.setOnClickListener(v->{exporter.cancel();exporting=false;finishExportUi();exportStatus.setText("处理已取消，原文件未修改。");});
        exportStatus=text("戴耳机试听后，再保存当前效果。",12,Color.LTGRAY,false);gap(root,8);root.addView(exportStatus);
        for(int i=0;i<2;i++){ScrollView list=new ScrollView(this);LinearLayout rows=column(20);list.addView(rows);libraryRows[i]=rows;pages[i+1]=list;content.addView(list);renderLibrary(i==1);list.setOnScrollChangeListener((v,x,y,oldX,oldY)->scheduleDetails());}
        onlinePanel=new OnlinePanel(this,new OnlinePanel.Host(){public void play(OnlineResolver.Result source,String link,String title,long position){openOnline(source,link,title,position);}public Button heart(OnlineLibrary.Entry entry){return collectionsPanel.heart(MediaRef.online(entry));}public void addTo(OnlineLibrary.Entry entry){collectionsPanel.addTo(MediaRef.online(entry));}public void information(OnlineLibrary.Entry entry){collectionsPanel.information(MediaRef.online(entry));}public void next(OnlineLibrary.Entry entry){enqueueNext(MediaRef.online(entry));}public boolean busy(){return exporting || copying;}public void downloaded(Uri uri,String name){library.add(uri,name,true);renderLibrary(true);}});pages[3]=onlinePanel.createView();content.addView(pages[3]);pages[4]=collectionsPanel.createView();content.addView(pages[4]);
        miniPlayer=new LinearLayout(this);miniPlayer.setOrientation(LinearLayout.VERTICAL);miniPlayer.setPadding(dp(12),dp(8),dp(12),dp(8));miniPlayer.setBackgroundColor(PANEL);
        LinearLayout miniTop=new LinearLayout(this);miniTop.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);miniName=text("",16,Color.WHITE,true);miniName.setSingleLine();miniName.setEllipsize(android.text.TextUtils.TruncateAt.END);miniState=text("",12,Color.LTGRAY,false);info.addView(miniName);info.addView(miniState);miniTop.addView(info,new LinearLayout.LayoutParams(0,-2,1));info.setOnClickListener(v->showPage(0));
        miniPlayButton=iconButton("play",true);miniPlayButton.setContentDescription("播放");miniTop.addView(miniPlayButton,new LinearLayout.LayoutParams(dp(48),dp(48)));miniPlayButton.setOnClickListener(v->togglePlay());Button open=iconButton("chevron",false);open.setContentDescription("返回播放页");miniTop.addView(open,new LinearLayout.LayoutParams(dp(48),dp(48)));open.setOnClickListener(v->showPage(0));miniPlayer.addView(miniTop);
        LinearLayout miniActions=new LinearLayout(this);miniModeButton=button("模式：顺序播放 ▾",false);miniModeButton.setTextSize(14);miniModeButton.setSingleLine();miniModeButton.setPadding(dp(4),0,dp(4),0);miniModeButton.setOnClickListener(v->choosePlaybackMode());miniActions.addView(miniModeButton,new LinearLayout.LayoutParams(0,dp(48),1));Button miniQueue=button("播放列表",false);miniQueue.setTextSize(14);miniQueue.setOnClickListener(v->showQueue());miniActions.addView(miniQueue,new LinearLayout.LayoutParams(0,dp(48),1));miniPlayer.addView(miniActions);miniPlayer.setVisibility(View.GONE);appFrame.addView(miniPlayer);
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(10),dp(4),dp(10),dp(4));nav.setBackgroundColor(PANEL);appFrame.addView(nav);String[] names={"播放","音乐","视频","在线","清单"};for(int i=0;i<5;i++){final int page=i;tabs[i]=button(names[i],false);tabs[i].setTextSize(13);tabs[i].setPadding(0,dp(5),0,dp(5));nav.addView(tabs[i],new LinearLayout.LayoutParams(0,dp(64),1));tabs[i].setOnClickListener(v->showPage(page));}
        createFullscreen();showPage(0);
        shell.setOnApplyWindowInsetsListener((v,insets)->{int l,t,r,b;if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(fullscreen?WindowInsets.Type.displayCutout():WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());l=safe.left;t=safe.top;r=safe.right;b=safe.bottom;}else{l=insets.getSystemWindowInsetLeft();t=fullscreen?0:insets.getSystemWindowInsetTop();r=insets.getSystemWindowInsetRight();b=fullscreen?0:insets.getSystemWindowInsetBottom();}v.setPadding(l,t,r,b);return insets;});setContentView(shell);onlinePanel.acceptIntent(getIntent());
    }
    private LinearLayout column(int padding){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(padding),dp(16),dp(padding),dp(18));return root;}
    private UiIcon icon(String kind,int color,int size){UiIcon icon=new UiIcon(kind,color);icon.setBounds(0,0,dp(size),dp(size));return icon;}
    private void setIcon(Button b,String kind,int color,int size){if(b instanceof CenteredIconButton centered)centered.setIcon(kind,color,dp(size));else b.setCompoundDrawables(icon(kind,color,size),null,null,null);b.setText("");b.setGravity(Gravity.CENTER);}
    private Button iconButton(String kind,boolean primary){Button b=new CenteredIconButton(this);b.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),round(primary?ACCENT:PANEL,12),null));b.setElevation(0);b.setStateListAnimator(null);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinimumHeight(0);b.setPadding(0,0,0,0);setIcon(b,kind,primary?BG:ACCENT,24);return b;}
    private LinearLayout transport(){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        int[] steps={60,30,10,0,10,30,60};
        for(int i=0;i<steps.length;i++){if(i==3){Button play=iconButton("play",true);play.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),round(ACCENT,100),null));android.widget.FrameLayout cell=new android.widget.FrameLayout(this);cell.addView(play,new android.widget.FrameLayout.LayoutParams(dp(52),dp(52),Gravity.CENTER));row.addView(cell,new LinearLayout.LayoutParams(0,dp(60),1.45f));play.setEnabled(false);play.setOnClickListener(v->togglePlay());playButton=play;continue;}
            int sign=i<3?-1:1;int seconds=steps[i];Button b=iconButton(sign<0?"back":"forward",false);((CenteredIconButton)b).setIcon(sign<0?"back":"forward",ACCENT,dp(18));b.setText(seconds==60?"1分钟":seconds+"秒");b.setTextSize(11);b.setTextColor(ACCENT);b.setCompoundDrawablePadding(dp(4));b.setGravity(Gravity.CENTER);b.setContentDescription((sign<0?"快退":"快进")+(seconds==60?"1分钟":seconds+"秒"));b.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),round(BG,12),null));row.addView(b,new LinearLayout.LayoutParams(0,dp(56),1));b.setEnabled(false);b.setOnClickListener(v->{if(player!=null && player.isCurrentMediaItemSeekable())queueSeek(SeekMath.offset(queuedSeek!=C.TIME_UNSET?queuedSeek:player.getCurrentPosition(),seconds*1000L*sign,player.getDuration()));refresh();});seekButtons.add(b);
        }return row;
    }
    private void queueSeek(long target){queuedSeek=target;handler.removeCallbacks(commitSeek);handler.postDelayed(commitSeek,60);}
    private void updateNotificationName(){if(player==null || selectedUri==null)return;MediaItem item=player.getCurrentMediaItem();if(item!=null && item.localConfiguration!=null && selectedUri.equals(item.localConfiguration.uri) && !originalName.contentEquals(item.mediaMetadata.title==null?"":item.mediaMetadata.title))player.replaceMediaItem(player.getCurrentMediaItemIndex(),item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setTitle(originalName).build()).build());}
    private void togglePlay(){armFullscreenControls();if(player==null || selectedUri==null)return;if(player.getPlayWhenReady())player.pause();else{if(player.getPlaybackState()==Player.STATE_ENDED)player.seekTo(0);player.play();}}
    private void wireSeek(SeekBar bar){bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){dragging=true;}public void onProgressChanged(SeekBar s,int progress,boolean user){if(user && player!=null)clock.setText(time(SeekMath.fromProgress(progress,player.getDuration())));}public void onStopTrackingTouch(SeekBar s){if(player!=null && player.isCurrentMediaItemSeekable())queueSeek(SeekMath.fromProgress(s.getProgress(),player.getDuration()));dragging=false;refresh();}});}
    private View settingRow(String glyph,String title,TextView summary,Runnable action){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(10),dp(10),dp(10));row.setMinimumHeight(dp(64));row.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x22FFFFFF),round(PANEL,12),null));android.widget.ImageView image=new android.widget.ImageView(this);image.setImageDrawable(icon(glyph,Color.WHITE,28));row.addView(image,new LinearLayout.LayoutParams(dp(32),dp(32)));LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(12),0,dp(8),0);labels.addView(text(title,16,Color.WHITE,true));labels.addView(summary);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));android.widget.ImageView arrow=new android.widget.ImageView(this);arrow.setImageDrawable(icon("chevron",Color.LTGRAY,20));row.addView(arrow,new LinearLayout.LayoutParams(dp(24),dp(24)));row.setOnClickListener(v->action.run());return row;}
    private void setupEffects(){
        channelPanel=column(20);channelMode=new RadioGroup(this);String[] modes={"保留原声道","左复制到右","右复制到左","单声道转双声道／居中","自动声道补全"};for(int i=0;i<modes.length;i++){RadioButton r=new RadioButton(this);r.setId(MODE_IDS[i]);r.setText(modes[i]);r.setTextColor(Color.WHITE);r.setMinHeight(dp(52));channelMode.addView(r);}channelPanel.addView(channelMode);
        autoThresholdLabel=text("",14,ACCENT,false);channelPanel.addView(autoThresholdLabel);autoThreshold=new SeekBar(this);autoThreshold.setMax(70);autoThreshold.setProgress(30);channelPanel.addView(autoThreshold);channelPanel.addView(text("仅在自动声道补全模式生效。默认30分贝，可调0～70分贝。数值表示低于最大数字音量多少分贝；越小越容易触发，越大越接近无声才补全。不是耳机音量百分比。两边明显不均衡且较安静一边低于阈值时，才补全。",13,Color.LTGRAY,false));
        channelPanel.addView(text("保留原声道：保留文件原来的左右声音。\n\n左复制到右：两耳都播放左声道，适合只有左边有声。\n\n右复制到左：两耳都播放右声道，适合只有右边有声。\n\n单声道转双声道／居中：根据声道能量混合后送到两耳，并减轻反相抵消；居中会改变原立体声空间，不能保持所有原始细节。单声道文件本来就会自动复制到两耳。\n\n自动声道补全：跟随有声的一边补到较安静的一边；两边正常有声时保留原声道。检测使用原始声音，先补全，再应用左右移动或环绕。\n\n复制或居中本身不产生环绕感，在环绕效果入口选择左右环绕或耳机环绕。",14,Color.LTGRAY,false));
        movingPanel=column(20);moving=new Switch(this);moving.setText("左右循环移动");moving.setTextColor(Color.WHITE);moving.setMinHeight(dp(52));spatialMode=new RadioGroup(this);String[] spatialNames={"关闭环绕","左右环绕","耳机环绕","左右交替"};int[] spatialIds={R.id.spatial_off,R.id.spatial_left,R.id.spatial_headphone,R.id.spatial_alternate};for(int i=0;i<4;i++){RadioButton option=new RadioButton(this);option.setId(spatialIds[i]);option.setText(spatialNames[i]);option.setTextColor(Color.WHITE);option.setMinHeight(dp(48));spatialMode.addView(option);}movingPanel.addView(spatialMode);spatialControls=column(0);alternateControls=column(0);movingPanel.addView(alternateControls);movingPanel.addView(spatialControls);alternateSpeedLabel=text("",16,ACCENT,true);alternateControls.addView(alternateSpeedLabel);alternateSpeed=new SeekBar(this);alternateSpeed.setMax((EffectDefaults.ALTERNATE_MAX_MILLIS-EffectDefaults.ALTERNATE_MIN_MILLIS)/EffectDefaults.ALTERNATE_STEP_MILLIS);alternateSpeed.setProgress((EffectDefaults.ALTERNATE_MILLIS-EffectDefaults.ALTERNATE_MIN_MILLIS)/EffectDefaults.ALTERNATE_STEP_MILLIS);alternateControls.addView(alternateSpeed,new LinearLayout.LayoutParams(-1,dp(52)));alternateControls.addView(text("每0.05～2.00秒换一边，每档0.01秒。默认左侧发声0.20秒，再换右侧0.20秒，完整一轮0.40秒。先合并两侧声音再交替输出；已选声道复制时按所选声道处理。切换边缘短暂淡出、淡入，减轻爆音。左右交替与两种环绕效果只能选一个。",13,Color.LTGRAY,false));spatialControls.addView(text("效果调节",18,ACCENT,true));spatialPath=text("",16,ACCENT,true);spatialControls.addView(spatialPath);gap(spatialControls,12);effectLabel=text("",16,Color.WHITE,false);spatialControls.addView(effectLabel);gap(spatialControls,12);spatialControls.addView(text("循环时间：0.1～10秒（每档0.1秒）",14,Color.LTGRAY,false));cycle=new SeekBar(this);cycle.setMax(99);cycle.setProgress(29);spatialControls.addView(cycle,new LinearLayout.LayoutParams(-1,dp(52)));spatialControls.addView(text("移动幅度：0～300%",14,Color.LTGRAY,false));width=new SeekBar(this);width.setMax(EffectDefaults.MAX_WIDTH);width.setProgress(100);spatialControls.addView(width,new LinearLayout.LayoutParams(-1,dp(52)));distanceLabel=text("",14,ACCENT,false);spatialControls.addView(distanceLabel);distance=new SeekBar(this);distance.setMax(100);distance.setProgress(45);spatialControls.addView(distance,new LinearLayout.LayoutParams(-1,dp(52)));spatialControls.addView(text("空间距离：0保留贴耳效果，调高增加短时空间反射。不是实际米数。左右环绕沿左右移动，耳机环绕使用双耳模型绕头旋转；两者只能选一个。戴耳机试听，反射太强会改变原录音的清晰度。",13,Color.LTGRAY,false));
        surround=new Switch(this);
        android.content.SharedPreferences prefs=getPreferences(MODE_PRIVATE);if(!prefs.getBoolean("defaults13",false))prefs.edit().putInt("cycleTenths",30).putInt("width",100).putBoolean("defaults13",true).apply();if(!prefs.getBoolean("autoDefaults141",false))prefs.edit().putInt("autoDepthDb",30).putBoolean("autoDefaults141",true).apply();if(!prefs.getBoolean("spatialDefaults162",false))prefs.edit().putInt("cycleTenths",EffectDefaults.CYCLE_TENTHS).putInt("width",EffectDefaults.WIDTH).putInt("distance",EffectDefaults.DISTANCE).putBoolean("spatialDefaults162",true).apply();int mode=Math.max(0,Math.min(4,prefs.getInt("mode",0)));int tenths=Math.max(1,Math.min(100,prefs.getInt("cycleTenths",30)));autoThreshold.setProgress(Math.max(0,Math.min(70,prefs.getInt("autoDepthDb",30))));channelMode.check(MODE_IDS[mode]);moving.setChecked(prefs.getBoolean("moving",false));surround.setChecked(prefs.getBoolean("surround",false));if(surround.isChecked())moving.setChecked(false);boolean alternating=prefs.getBoolean("alternating",false);if(alternating){moving.setChecked(false);surround.setChecked(false);}spatialMode.check(alternating?R.id.spatial_alternate:surround.isChecked()?R.id.spatial_headphone:moving.isChecked()?R.id.spatial_left:R.id.spatial_off);alternateSpeed.setProgress((Math.max(EffectDefaults.ALTERNATE_MIN_MILLIS,Math.min(EffectDefaults.ALTERNATE_MAX_MILLIS,prefs.getInt("alternateMillis",EffectDefaults.ALTERNATE_MILLIS)))-EffectDefaults.ALTERNATE_MIN_MILLIS)/EffectDefaults.ALTERNATE_STEP_MILLIS);distance.setProgress(Math.max(0,Math.min(100,prefs.getInt("distance",45))));cycle.setProgress(tenths-1);width.setProgress(Math.max(0,Math.min(EffectDefaults.MAX_WIDTH,prefs.getInt("width",EffectDefaults.WIDTH))));

        channelMode.setOnCheckedChangeListener((g,id)->updateEffects());spatialMode.setOnCheckedChangeListener((g,id)->{moving.setChecked(id==R.id.spatial_left);surround.setChecked(id==R.id.spatial_headphone);updateEffects();});SeekBar.OnSeekBarChangeListener effects=new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){updateEffects();}};cycle.setOnSeekBarChangeListener(effects);width.setOnSeekBarChangeListener(effects);autoThreshold.setOnSeekBarChangeListener(effects);distance.setOnSeekBarChangeListener(effects);alternateSpeed.setOnSeekBarChangeListener(effects);Button reset=button("一键默认",true);movingPanel.addView(reset,new LinearLayout.LayoutParams(-1,dp(48)));reset.setOnClickListener(v->{if(spatialMode.getCheckedRadioButtonId()==R.id.spatial_alternate){alternateSpeed.setProgress((EffectDefaults.ALTERNATE_MILLIS-EffectDefaults.ALTERNATE_MIN_MILLIS)/EffectDefaults.ALTERNATE_STEP_MILLIS);updateEffects();return;}cycle.setProgress(EffectDefaults.CYCLE_TENTHS-1);width.setProgress(EffectDefaults.WIDTH);distance.setProgress(EffectDefaults.DISTANCE);updateEffects();});
    }
    private void showPanel(String title,LinearLayout panel){if(panel.getParent()!=null)((android.view.ViewGroup)panel.getParent()).removeView(panel);android.app.Dialog dialog=new android.app.Dialog(this);LinearLayout wrap=column(0);wrap.setBackground(round(PANEL,20));TextView heading=text(title,21,Color.WHITE,true);heading.setPadding(dp(20),dp(8),dp(20),dp(8));wrap.addView(heading);ScrollView scroll=new ScrollView(this);scroll.addView(panel);wrap.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));Button done=button("完成",true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(dp(20),dp(8),dp(20),dp(8));wrap.addView(done,lp);done.setOnClickListener(v->dialog.dismiss());dialog.setContentView(wrap);dialog.setOnDismissListener(d->scroll.removeView(panel));dialog.show();android.view.Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setGravity(Gravity.BOTTOM);window.setLayout(-1,Math.min(dp(460),getResources().getDisplayMetrics().heightPixels-dp(80)));}}
    private void chooseTimer(){if(playbackService==null)return;new android.app.AlertDialog.Builder(this).setTitle("定时停止播放").setItems(new String[]{"半小时","一小时","自定义","取消定时"},(d,choice)->{if(choice==0 || choice==1)playbackService.setTimer(choice==0?30:60);else if(choice==2)customTimer();else playbackService.cancelTimer();refresh();}).setNegativeButton("返回",null).show();}
    private void showPage(int page){activePage=page;updateLibrarySelection();if(page==4)collectionsPanel.render();for(int i=0;i<5;i++){pages[i].setVisibility(i==page?View.VISIBLE:View.GONE);tabs[i].setTextColor(i==page?ACCENT:Color.LTGRAY);tabs[i].setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x22FFFFFF),round(PANEL,0),null));tabs[i].setCompoundDrawables(null,icon(new String[]{"play","music","video","online","queue"}[i],i==page?ACCENT:Color.LTGRAY,24),null,null);}if(miniPlayer!=null)miniPlayer.setVisibility(page!=0 && selectedUri!=null?View.VISIBLE:View.GONE);scheduleDetails();}
    private void updateLibrarySelection(){String uri=selectedUri==null?"":selectedUri.toString();for(LinearLayout rows:libraryRows)if(rows!=null)for(int i=0;i<rows.getChildCount();i++){View row=rows.getChildAt(i);if(row.getTag() instanceof String key)LibraryStyle.card(row,key.equals(uri));}}
    private void renderLibrary(boolean video){
        LinearLayout rows=libraryRows[video?1:0];rows.removeAllViews();java.util.List<MediaLibrary.Entry> entries=library.list(video);LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);heading.addView(text(video?"视频":"音乐",28,Color.WHITE,true),new LinearLayout.LayoutParams(0,-2,1));Button add=button(video?"＋ 导入视频":"＋ 导入音乐",true);add.setTextSize(14);heading.addView(add,new LinearLayout.LayoutParams(dp(136),dp(48)));add.setOnClickListener(v->importFiles(video));rows.addView(heading);gap(rows,10);rows.addView(text(video?"退出页面，声音继续播放":"直接读取源文件",14,Color.LTGRAY,false));gap(rows,18);if(entries.isEmpty())rows.addView(text("还没有导入文件",16,Color.LTGRAY,false));
        for(MediaLibrary.Entry entry:entries){LinearLayout row=new LinearLayout(this);row.setTag(entry.uri());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(12),dp(4),dp(12));LibraryStyle.card(row,entry.uri().equals(selectedUri==null?"":selectedUri.toString()));android.widget.ImageView preview=new android.widget.ImageView(this);preview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);preview.setBackground(round(PANEL,12));preview.setClipToOutline(true);preview.setImageDrawable(icon(video?"video":"music",ACCENT,28));row.addView(preview,new LinearLayout.LayoutParams(dp(video?100:52),dp(video?62:52)));LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(12),0,dp(4),0);TextView name=text(entry.name(),16,Color.WHITE,true);name.setMaxLines(2);labels.addView(name);TextView detail=text("源文件",12,Color.LTGRAY,false);labels.addView(detail);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));row.addView(collectionsPanel.heart(MediaRef.local(entry)),new LinearLayout.LayoutParams(dp(48),dp(48)));Button info=LibraryStyle.more(this,entry.name());info.setContentDescription(entry.name()+"操作");row.addView(info,new LinearLayout.LayoutParams(dp(48),dp(48)));info.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle(entry.name()).setItems(new String[]{"加入清单","信息","下一首播放","移出列表"},(d,which)->{MediaRef item=MediaRef.local(entry);if(which==0)collectionsPanel.addTo(item);else if(which==1)collectionsPanel.information(item);else if(which==2)enqueueNext(item);else{library.remove(entry.uri());renderLibrary(video);}}).setNegativeButton("返回",null).show());row.setOnClickListener(v->{java.util.List<MediaRef> tracks=localQueue(video);int index=0;for(int j=0;j<tracks.size();j++)if(tracks.get(j).uri().equals(entry.uri())){index=j;break;}startQueue(tracks,index);});LibraryStyle.addCard(rows,row);applyCachedDetails(entry,preview,detail);}
        scheduleDetails();
    }
    private void applyCachedDetails(MediaLibrary.Entry entry,android.widget.ImageView preview,TextView detail){String duration=durationCache.get(entry.uri());if(duration!=null)detail.setText(duration);android.graphics.Bitmap cached=thumbnailCache.get(entry.uri());if(cached!=null)preview.setImageBitmap(cached);}
    private void scheduleDetails(){handler.removeCallbacks(readVisibleDetails);if(activePage>0 && activePage<3 && visible)handler.postDelayed(readVisibleDetails,100);}
    private void loadVisibleDetails(){
        if(destroyed || !visible || detailsBusy || activePage==0 || activePage>2 || player!=null && player.getPlaybackState()==Player.STATE_BUFFERING)return;
        LinearLayout rows=libraryRows[activePage-1];android.graphics.Rect visibleRect=new android.graphics.Rect();
        for(int i=0;i<rows.getChildCount();i++){View view=rows.getChildAt(i);if(!(view.getTag() instanceof String uri) || !view.getGlobalVisibleRect(visibleRect))continue;
            MediaLibrary.Entry entry=library.find(uri);if(entry==null)continue;LinearLayout row=(LinearLayout)view;android.widget.ImageView preview=(android.widget.ImageView)row.getChildAt(0);TextView detail=(TextView)((LinearLayout)row.getChildAt(1)).getChildAt(1);applyCachedDetails(entry,preview,detail);if(durationCache.containsKey(uri))continue;loadDetails(entry,preview,detail);break;
        }
    }
    private void loadDetails(MediaLibrary.Entry entry,android.widget.ImageView preview,TextView detail){detailsBusy=true;mediaReader.execute(()->{
        android.graphics.Bitmap thumb=null;String duration="源文件";android.media.MediaMetadataRetriever reader=new android.media.MediaMetadataRetriever();
        try{reader.setDataSource(this,Uri.parse(entry.uri()));String value=reader.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);if(value!=null)duration=time(Long.parseLong(value));if(entry.video() && android.os.Build.VERSION.SDK_INT>=27)thumb=reader.getScaledFrameAtTime(0,android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC,dp(100),dp(62));}catch(Exception ignored){}finally{try{reader.release();}catch(Exception ignored){}}
        final android.graphics.Bitmap image=thumb;final String label=duration;handler.post(()->{detailsBusy=false;if(destroyed){if(image!=null)image.recycle();return;}durationCache.put(entry.uri(),label);if(image!=null)thumbnailCache.put(entry.uri(),image);detail.setText(label);if(image!=null)preview.setImageBitmap(image);scheduleDetails();});
    });}
    // GestureDetector.onSingleTapConfirmed delegates clicks to performClick.
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void createFullscreen(){
        fullscreenLayer=new GestureVideoLayout(this);fullscreenLayer.setBackgroundColor(Color.BLACK);fullscreenLayer.setVisibility(View.GONE);shell.addView(fullscreenLayer,new android.widget.FrameLayout.LayoutParams(-1,-1));fullVideo=new android.widget.FrameLayout(this);fullscreenLayer.addView(fullVideo,new android.widget.FrameLayout.LayoutParams(-1,-1));fullVideo.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fitVideo());
        fullChrome=new android.widget.FrameLayout(this);fullscreenLayer.addView(fullChrome,new android.widget.FrameLayout.LayoutParams(-1,-1));Button close=iconButton("close",false);close.setContentDescription("退出全屏");android.widget.FrameLayout.LayoutParams closeLp=new android.widget.FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.START);closeLp.setMargins(dp(12),dp(12),0,0);fullChrome.addView(close,closeLp);close.setOnClickListener(v->exitFullscreen());
        gestureHud=text("",20,Color.WHITE,true);gestureHud.setGravity(Gravity.CENTER);gestureHud.setPadding(dp(20),dp(14),dp(20),dp(14));gestureHud.setBackground(round(0xBB101A1D,14));gestureHud.setVisibility(View.GONE);fullscreenLayer.addView(gestureHud,new android.widget.FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));
        fullscreenLayer.setContentDescription("左右滑动快进快退，双击播放暂停，单击显示退出入口");fullscreenLayer.setOnClickListener(v->{fullChrome.setVisibility(fullChrome.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);armFullscreenControls();});
        android.view.GestureDetector taps=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(android.view.MotionEvent e){return true;}@Override public boolean onSingleTapConfirmed(android.view.MotionEvent e){if(!swiping)fullscreenLayer.performClick();return true;}@Override public boolean onDoubleTap(android.view.MotionEvent e){togglePlay();showGestureHud(player!=null && player.getPlayWhenReady()?"播放":"暂停");handler.postDelayed(hideGestureHud,1000);return true;}});
        fullscreenLayer.setOnTouchListener((view,event)->{if(player==null)return true;switch(event.getActionMasked()){
            case android.view.MotionEvent.ACTION_DOWN -> {swipeStartX=event.getX();swipeStartY=event.getY();swipeStartPosition=player.getCurrentPosition();swiping=false;handler.removeCallbacks(hideGestureHud);taps.onTouchEvent(event);}
            case android.view.MotionEvent.ACTION_MOVE -> {float dx=event.getX()-swipeStartX,dy=event.getY()-swipeStartY;if(!swiping && Math.abs(dx)>dp(14) && Math.abs(dx)>Math.abs(dy)*1.3 && player.isCurrentMediaItemSeekable()){swiping=true;handler.removeCallbacks(hideFullControls);android.view.MotionEvent cancel=android.view.MotionEvent.obtain(event.getDownTime(),event.getEventTime(),android.view.MotionEvent.ACTION_CANCEL,event.getX(),event.getY(),0);taps.onTouchEvent(cancel);cancel.recycle();}if(swiping){swipeTarget=SwipeSeek.target(swipeStartPosition,dx,fullscreenLayer.getWidth(),player.getDuration());long delta=swipeTarget-swipeStartPosition;showGestureHud((delta>=0?"快进 ":"快退 ")+time(Math.abs(delta))+"\n"+time(swipeTarget)+" / "+time(player.getDuration()));}else taps.onTouchEvent(event);}
            case android.view.MotionEvent.ACTION_UP -> {if(swiping){queueSeek(swipeTarget);swiping=false;handler.postDelayed(hideGestureHud,700);armFullscreenControls();}else taps.onTouchEvent(event);}
            case android.view.MotionEvent.ACTION_CANCEL -> {swiping=false;gestureHud.setVisibility(View.GONE);taps.onTouchEvent(event);armFullscreenControls();}
            default -> {}
        }return true;});
    }
    private void showGestureHud(String label){gestureHud.setText(label);gestureHud.setVisibility(View.VISIBLE);}
    private void updateVideoVisibility(){boolean video=player!=null && player.getCurrentTracks().isTypeSelected(C.TRACK_TYPE_VIDEO);if(!video && fullscreen)exitFullscreen();videoBox.setVisibility(video?View.VISIBLE:View.GONE);if(video && restoreFullscreen && visible){restoreFullscreen=false;enterFullscreen();}}
    private void fitVideo(){android.widget.FrameLayout target=fullscreen?fullVideo:videoBox;if(target==null || surface==null)return;VideoFit.Size size=VideoFit.fit(target.getWidth(),target.getHeight(),videoRatio);if(size.width()==0)return;android.view.ViewGroup.LayoutParams current=surface.getLayoutParams();if(current.width!=size.width() || current.height!=size.height()){surface.setLayoutParams(new android.widget.FrameLayout.LayoutParams(size.width(),size.height(),Gravity.CENTER));}}
    private void enterFullscreen(){if(fullscreen || player==null || !player.getCurrentTracks().isTypeSelected(C.TRACK_TYPE_VIDEO))return;fullscreen=true;if(getRequestedOrientation()!=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)previousOrientation=getRequestedOrientation();player.clearVideoSurfaceView(surface);videoBox.removeView(surface);fullVideo.addView(surface,new android.widget.FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));appFrame.setVisibility(View.GONE);fullscreenLayer.setVisibility(View.VISIBLE);fullChrome.setVisibility(View.VISIBLE);if(android.os.Build.VERSION.SDK_INT>=33){fullBack=this::exitFullscreen;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,fullBack);}player.setVideoSurfaceView(surface);setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);systemBars(true);shell.requestApplyInsets();shell.post(this::fitVideo);armFullscreenControls();refresh();}
    private void exitFullscreen(){if(!fullscreen)return;fullscreen=false;swiping=false;gestureHud.setVisibility(View.GONE);handler.removeCallbacks(hideGestureHud);handler.removeCallbacks(hideFullControls);if(android.os.Build.VERSION.SDK_INT>=33 && fullBack!=null){getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(fullBack);fullBack=null;}if(player!=null)player.clearVideoSurfaceView(surface);fullVideo.removeView(surface);videoBox.addView(surface,0,new android.widget.FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));fullscreenLayer.setVisibility(View.GONE);appFrame.setVisibility(View.VISIBLE);if(player!=null && visible)player.setVideoSurfaceView(surface);setRequestedOrientation(previousOrientation);systemBars(false);shell.requestApplyInsets();shell.post(this::fitVideo);refresh();}
    private void armFullscreenControls(){handler.removeCallbacks(hideFullControls);if(fullscreen && !swiping && player!=null && player.isPlaying())handler.postDelayed(hideFullControls,3000);}
    private void systemBars(boolean hide){if(android.os.Build.VERSION.SDK_INT>=30){android.view.WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null){controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);if(hide)controller.hide(WindowInsets.Type.systemBars());else controller.show(WindowInsets.Type.systemBars());}}else getWindow().getDecorView().setSystemUiVisibility(hide?View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN:View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}

    private void importFiles(boolean video){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(video?"video/*":"audio/*");intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(intent,video?104:103);}
    private String displayName(Uri uri){String cached=nameCache.get(uri.toString());if(cached!=null)return cached;String name="已选择文件";try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null && c.moveToFirst() && c.getString(0)!=null && !c.getString(0).isBlank())name=c.getString(0);}catch(RuntimeException ignored){}nameCache.put(uri.toString(),name);return name;}
    private void importDocuments(Intent data,Boolean kind,boolean open){
        ArrayList<Uri> uris=new ArrayList<>();if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)uris.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)uris.add(data.getData());
        for(Uri uri:uris)if((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0)try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){status.setText("文件提供方未允许长期访问，下次可能需要重新导入。");}
        if(open && !uris.isEmpty())openFile(uris.get(0),0);
        fileLookup.execute(()->{ArrayList<MediaLibrary.Entry> entries=new ArrayList<>();for(Uri uri:uris){String mime=null;if(kind==null)try{mime=getContentResolver().getType(uri);}catch(RuntimeException ignored){}entries.add(new MediaLibrary.Entry(uri.toString(),displayName(uri),kind!=null?kind:mime!=null && mime.startsWith("video/")));}
            handler.post(()->{if(destroyed)return;for(MediaLibrary.Entry entry:entries){library.add(Uri.parse(entry.uri()),entry.name(),entry.video());if(playbackService!=null)playbackService.queue.order.update(MediaRef.local(entry));if(selectedUri!=null && selectedUri.toString().equals(entry.uri())){nameResolved=true;originalName=entry.name();fileName.setText(originalName);if(playbackService!=null)playbackService.sourceName=originalName;updateNotificationName();}}renderLibrary(false);renderLibrary(true);refresh();});});
    }
    private void customTimer(){android.widget.EditText input=new android.widget.EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);input.setHint("分钟，例如45或0.5");input.setPadding(dp(24),dp(12),dp(24),dp(12));android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("自定义停止时间").setMessage("输入分钟数，大于0且不超过1440。到时暂停播放，保留界面。").setView(input).setNegativeButton("取消",null).setPositiveButton("开始计时",null).create();dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{double minutes=Double.parseDouble(input.getText().toString().trim());if(playbackService==null)return;playbackService.setTimer(minutes);dialog.dismiss();refresh();}catch(IllegalArgumentException error){input.setError("请输入大于0、不超过1440的分钟数");}}));dialog.show();}

    private int copyMode(){for(int i=0;i<MODE_IDS.length;i++)if(channelMode.getCheckedRadioButtonId()==MODE_IDS[i])return i;return 0;}
    private void updateEffects(){
        if(channelMode==null || cycle==null || width==null)return;
        double seconds=(cycle.getProgress()+1)/10.0;boolean alternating=spatialMode.getCheckedRadioButtonId()==R.id.spatial_alternate;int alternateMillis=EffectDefaults.ALTERNATE_MIN_MILLIS+alternateSpeed.getProgress()*EffectDefaults.ALTERNATE_STEP_MILLIS;spatialControls.setVisibility(alternating?View.GONE:View.VISIBLE);alternateControls.setVisibility(alternating?View.VISIBLE:View.GONE);alternateSpeedLabel.setText(String.format(Locale.CHINA,"每%.2f秒换一边 · 一轮%.2f秒",alternateMillis/1000.0,alternateMillis/500.0));
        soundProcessor.settings=new SoundEngine.Settings(copyMode(),moving.isChecked(),seconds,width.getProgress()/100.0,surround.isChecked(),-autoThreshold.getProgress(),distance.getProgress()/100.0,alternating,alternateMillis/1000.0);
        autoThresholdLabel.setText(String.format(Locale.CHINA,"自动补全检测阈值：%d分贝",autoThreshold.getProgress()));autoThreshold.setEnabled(copyMode()==4);
        effectLabel.setText(String.format(Locale.CHINA,"循环%.1f秒 · 幅度%d%%",seconds,width.getProgress()));
        if(channelSummary!=null){String[] labels={"保留原声道","左复制到右","右复制到左","单声道转双声道／居中","自动声道补全"};channelSummary.setText(labels[copyMode()]);movingSummary.setText(alternating?String.format(Locale.CHINA,"左右交替 · 每%.2f秒换边",alternateMillis/1000.0):surround.isChecked()?String.format(Locale.CHINA,"耳机环绕 · %.1f秒",seconds):moving.isChecked()?String.format(Locale.CHINA,"左右环绕 · %.1f秒",seconds):"未开启");}
        spatialPath.setText(surround.isChecked()?"左 → 前 → 右 → 后 → 左":"左 → 中 → 右 → 中 → 左");
        distanceLabel.setText("空间距离："+distance.getProgress()+"%");getPreferences(MODE_PRIVATE).edit().putBoolean("alternating",alternating).putInt("alternateMillis",alternateMillis).putInt("distance",distance.getProgress()).putInt("autoDepthDb",autoThreshold.getProgress()).putBoolean("surround",surround.isChecked()).putInt("mode",copyMode()).putBoolean("moving",moving.isChecked()).putInt("cycleTenths",cycle.getProgress()+1).putInt("width",width.getProgress()).apply();
    }
    private void startExport(){
        if(selectedUri==null || player==null || exporting || copying)return;
        if(!player.getCurrentTracks().isTypeSelected(C.TRACK_TYPE_AUDIO)){exportStatus.setText("此文件没有可处理的音轨，或音轨编码不受支持。");return;}
        if(!getPreferences(MODE_PRIVATE).edit().remove("pendingFile").commit()){exportStatus.setText("无法更新处理状态，请检查手机存储空间后重试。");return;}
        boolean video=player.getCurrentTracks().isTypeSelected(C.TRACK_TYPE_VIDEO);
        String extension=video?".mp4":".m4a";
        exportName="改-"+originalName+(originalName.toLowerCase(Locale.ROOT).endsWith(extension)?"":extension);
        exportMime=video?"video/mp4":"audio/mp4";pendingExport=null;retrySave.setVisibility(View.GONE);
        exporting=true;player.pause();saveButton.setEnabled(false);cancelButton.setVisibility(View.VISIBLE);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        exporter.start(this,selectedUri,soundProcessor.settings,new MediaExport.Callback(){
            public void completed(File file){if(destroyed)return;exporting=false;pendingExport=file;cancelButton.setVisibility(View.GONE);exportStatus.setText("处理完成，请选择保存位置。");
                getPreferences(MODE_PRIVATE).edit().putString("pendingFile",file.getAbsolutePath()).putLong("pendingBytes",file.length()).putString("pendingName",exportName).putString("pendingMime",exportMime).commit();retrySave.setVisibility(View.VISIBLE);chooseDestination();
            }
            public void failed(String message){if(destroyed)return;exporting=false;finishExportUi();exportStatus.setText(message);}
        });
    }
    private void chooseDestination(){if(pendingExport==null || exporting || copying)return;Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);save.addCategory(Intent.CATEGORY_OPENABLE);save.setType(exportMime);save.putExtra(Intent.EXTRA_TITLE,exportName);startActivityForResult(save,SAVE_FILE);}
    private void finishExportUi(){cancelButton.setVisibility(View.GONE);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);refresh();}
    private void saveCompletedFile(Uri destination){
        File source=pendingExport;if(source==null || !source.isFile()){finishExportUi();exportStatus.setText("处理缓存已失效，请重新处理后保存。选定位置可能有空文件。");return;}copying=true;saveButton.setEnabled(false);retrySave.setEnabled(false);exportStatus.setText("正在写入保存位置…");
        writer.execute(()->{
            String message;boolean success=false;
            try(FileInputStream input=new FileInputStream(source);OutputStream out=getContentResolver().openOutputStream(destination,"w")){
                if(out==null)throw new java.io.IOException("保存位置不可写");byte[] buffer=new byte[262144];int n;while((n=input.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new java.io.IOException("写入中断");out.write(buffer,0,n);}out.flush();message="已保存："+exportName+"\n位置："+destination;success=true;
            }catch(Exception error){message="保存失败："+error.getMessage()+"。该位置可能有未完成文件；可点保存重试。";}
            final String result=message;final boolean saved=success;handler.post(()->{if(destroyed)return;copying=false;retrySave.setEnabled(true);if(saved){pendingExport=null;getPreferences(MODE_PRIVATE).edit().remove("pendingFile").apply();retrySave.setVisibility(View.GONE);}finishExportUi();exportStatus.setText(result);});
        });
    }

    private void selectFile(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"audio/*","video/*"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent,PICK_FILE);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==OnlinePanel.DOWNLOAD_REQUEST){onlinePanel.activityResult(result,data);return;}
        if(request==SAVE_FILE){if(result==RESULT_OK && data!=null && data.getData()!=null)saveCompletedFile(data.getData());else{finishExportUi();exportStatus.setText("已取消选择保存位置，可重新处理并保存。");}return;}
        if((request==103 || request==104) && result==RESULT_OK && data!=null){importDocuments(data,request==104,false);return;}
        if(request==PICK_FILE && result==RESULT_OK && data!=null && data.getData()!=null)importDocuments(data,null,true);
    }
    private void openOnline(OnlineResolver.Result source,String link,String title,long position){
        handler.removeCallbacks(commitSeek);queuedSeek=C.TIME_UNSET;selectedUri=Uri.parse(link);onlinePlayback=true;pendingSource=false;pendingPlay=false;nameResolved=true;originalName=title;fileName.setText(title);status.setText("正在加载在线视频…");exportStatus.setText("在线音效即时生效。下载源视频后，可在视频列表处理并保存。");showPage(0);
        if(player==null){pendingOnline=source;pendingOnlineLink=link;pendingOnlineTitle=title;pendingOnlinePosition=position;return;}if(fullscreen)exitFullscreen();videoBox.setVisibility(View.GONE);playbackService.queue.attach(new MediaRef(link,title,true,OnlineLink.parse(link).platform()),onlinePanel.references());playbackService.playOnline(source,link,title,position);refresh();
    }
    private void openFile(Uri uri,long position){
        onlinePanel.cancelResolve();onlinePlayback=false;pendingOnline=null;if(playbackService!=null)playbackService.onlineUrl=null;
        handler.removeCallbacks(commitSeek);queuedSeek=C.TIME_UNSET;selectedUri=uri;updateLibrarySelection();MediaLibrary.Entry known=library.find(uri.toString());nameResolved=known!=null;originalName=known!=null?known.name():"正在读取文件名…";fileName.setText(originalName);if(!nameResolved)fileLookup.execute(()->{String resolved=displayName(uri);handler.post(()->{if(destroyed || !uri.equals(selectedUri))return;nameResolved=true;originalName=resolved;fileName.setText(resolved);if(playbackService!=null){MediaLibrary.Entry entry=library.find(uri.toString());playbackService.queue.order.update(new MediaRef(uri.toString(),resolved,entry!=null && entry.video(),""));if(uri.toString().equals(playbackService.sourceUri))playbackService.sourceName=resolved;}updateNotificationName();refresh();});});status.setText("正在读取文件…");
        if(player==null){pendingSource=true;pendingPosition=position;return;}
        playbackService.queue.attach(new MediaRef(uri.toString(),originalName,known!=null && known.video(),""),localQueue(known!=null && known.video()));player.setWakeMode(C.WAKE_MODE_LOCAL);if(fullscreen)exitFullscreen();pendingSource=false;player.pause();videoBox.setVisibility(View.GONE);playbackService.sourceUri=uri.toString();playbackService.sourceName=originalName;
        player.setMediaItem(new MediaItem.Builder().setUri(uri).setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder().setTitle(originalName).build()).build(),Math.max(0,position));player.prepare();if(pendingPlay){player.play();pendingPlay=false;}refresh();
    }
    private java.util.List<MediaRef> localQueue(boolean video){java.util.List<MediaRef> result=new ArrayList<>();for(MediaLibrary.Entry e:library.list(video))result.add(MediaRef.local(e));return result;}
    private void startQueue(java.util.List<MediaRef> tracks,int index){if(playbackService==null){android.widget.Toast.makeText(this,"播放服务正在连接，请稍后重试",android.widget.Toast.LENGTH_SHORT).show();return;}onlinePanel.cancelResolve();if(fullscreen)exitFullscreen();playbackService.queue.start(tracks,index);showPage(0);refresh();}
    private void enqueueNext(MediaRef item){if(!item.playable()){android.widget.Toast.makeText(this,"1.7已移除抖音链接功能",android.widget.Toast.LENGTH_SHORT).show();return;}if(playbackService==null || playbackService.sourceUri==null){android.widget.Toast.makeText(this,"请先播放一个文件，再安排下一首",android.widget.Toast.LENGTH_SHORT).show();return;}playbackService.queue.enqueue(item);android.widget.Toast.makeText(this,"已加入下一首播放："+item.title(),android.widget.Toast.LENGTH_SHORT).show();}
    private void showQueue(){if(playbackService==null)return;if(queuePanel!=null)queuePanel.close();queuePanel=new QueuePanel(this,playbackService.queue,this::refresh);queuePanel.show();}
    private void choosePlaybackMode(){if(playbackService==null)return;new android.app.AlertDialog.Builder(this).setTitle("播放模式").setSingleChoiceItems(PlaybackOrder.LABELS,playbackService.queue.order.mode(),(d,which)->{playbackService.queue.setMode(which);refresh();d.dismiss();}).setNegativeButton("返回",null).show();}
    private void refresh(){
        if(playbackService!=null){playbackService.checkTimer();timerStatus.setText(playbackService.timerActive()?"剩余 "+time((playbackService.timerRemaining()+999)/1000*1000):playbackService.timerFinished?"已停止":"未设置");}
        if(player==null)return;
        if(playbackService.sourceUri!=null && (selectedUri==null || !playbackService.sourceUri.equals(selectedUri.toString()))){handler.removeCallbacks(commitSeek);queuedSeek=C.TIME_UNSET;selectedUri=Uri.parse(playbackService.sourceUri);onlinePlayback=playbackService.onlineUrl!=null;originalName=playbackService.sourceName;nameResolved=true;fileName.setText(originalName);updateLibrarySelection();exportStatus.setText(onlinePlayback?"下载源视频后，可在视频列表处理并保存。":"戴耳机试听后，再保存当前效果。");}
        if(playbackService.queue.loading)status.setText("正在解析在线链接…");else if(!playbackService.queue.error.isEmpty())status.setText(playbackService.queue.error);
        long duration=player.getDuration(),position=player.getCurrentPosition();
        boolean ready=player.getPlaybackState()==Player.STATE_READY || player.getPlaybackState()==Player.STATE_ENDED;
        boolean canSeek=(ready || player.getPlaybackState()==Player.STATE_BUFFERING) && player.isCurrentMediaItemSeekable() && duration>0;
        seekBar.setEnabled(canSeek);for(Button b:seekButtons){b.setEnabled(canSeek);b.setAlpha(canSeek?1f:0.4f);}
        for(Button b:new Button[]{playButton,miniPlayButton})if(b!=null){b.setEnabled(ready || player.getPlaybackState()==Player.STATE_BUFFERING);b.setAlpha(b.isEnabled()?1f:0.4f);b.setContentDescription(player.getPlayWhenReady()?"暂停":"播放");setIcon(b,player.getPlayWhenReady()?"pause":"play",BG,b==playButton?24:22);}
        if(saveButton!=null){saveButton.setEnabled(ready && nameResolved && !onlinePlayback && !onlinePanel.downloading() && !exporting && !copying);saveButton.setAlpha(saveButton.isEnabled()?1f:0.4f);}
        if(!dragging){clock.setText(time(position));durationLabel.setText(time(duration));seekBar.setProgress(duration>0?(int)Math.min(10000,position*10000.0/duration):0);}

        miniPlayer.setVisibility(activePage!=0 && selectedUri!=null?View.VISIBLE:View.GONE);miniName.setText(originalName);miniState.setText(player.isPlaying()?"正在播放":"已暂停");String modeName=PlaybackOrder.LABELS[playbackService.queue.order.mode()];playModeButton.setText("播放模式："+modeName+" ▾");miniModeButton.setText("模式："+modeName+" ▾");playModeButton.setContentDescription("切换播放模式，当前"+modeName);miniModeButton.setContentDescription("切换播放模式，当前"+modeName);
        if(fullscreen || exporting || copying || onlinePanel.downloading())getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    // API 33+ uses the native back callback above; this override serves API 26-32 only.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){if(fullscreen)exitFullscreen();else super.onBackPressed();}
    @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);shell.post(this::fitVideo);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus && fullscreen)systemBars(true);}
    private String time(long ms){if(ms<0)ms=0;long s=ms/1000;return s>=3600?String.format(Locale.CHINA,"%d:%02d:%02d",s/3600,(s/60)%60,s%60):String.format(Locale.CHINA,"%02d:%02d",s/60,s%60);}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Button button(String value,boolean primary){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(primary?BG:Color.WHITE);b.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),round(primary?ACCENT:PANEL,12),null));b.setMinimumHeight(dp(48));b.setElevation(0);b.setStateListAnimator(null);return b;}
    private void gap(LinearLayout parent,int size){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(size)));}
    @Override protected void onStart(){super.onStart();visible=true;scheduleDetails();if(player!=null){player.setVideoSurfaceView(surface);playbackService.checkTimer();}handler.post(update);}
    @Override protected void onStop(){visible=false;handler.removeCallbacks(update);if(player!=null)player.clearVideoSurfaceView(surface);super.onStop();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(selectedUri!=null){state.putString("uri",selectedUri.toString());state.putString("onlineTitle",originalName);state.putLong("position",player!=null?player.getCurrentPosition():pendingPosition);state.putBoolean("fullscreen",fullscreen);state.putInt("previousOrientation",previousOrientation);}}
    @Override protected void onDestroy(){if(android.os.Build.VERSION.SDK_INT>=33 && fullBack!=null){getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(fullBack);fullBack=null;}destroyed=true;if(queuePanel!=null)queuePanel.close();onlinePanel.close();exporter.cancel();writer.shutdownNow();mediaReader.shutdownNow();fileLookup.shutdownNow();handler.removeCallbacksAndMessages(null);if(player!=null){player.clearVideoSurfaceView(surface);player.removeListener(playerListener);}if(bound)unbindService(connection);super.onDestroy();}
}
