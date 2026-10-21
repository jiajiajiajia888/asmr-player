package com.longjiahao.asmrplayer;

import android.content.Context;
import android.os.*;
import androidx.media3.common.*;
import java.util.*;
import java.util.concurrent.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class QueuePlayer {
    public final PlaybackOrder order=new PlaybackOrder();
    public String error="";
    public boolean loading;
    private final PlaybackService service;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService resolver=Executors.newSingleThreadExecutor();
    private Future<?> resolving;
    private int generation;
    private boolean closed;
    public QueuePlayer(PlaybackService service){this.service=service;order.mode(service.getSharedPreferences("queue",Context.MODE_PRIVATE).getInt("mode",0));service.player.addListener(new Player.Listener(){@Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED && service.player.getPlayWhenReady()){service.checkTimer();if(service.player.getPlayWhenReady())next(false);}}});}
    public void setMode(int mode){order.mode(mode);service.getSharedPreferences("queue",Context.MODE_PRIVATE).edit().putInt("mode",mode).apply();}
    public void attach(MediaRef track,List<MediaRef> context){cancelResolve();if(track.uri().equals(service.sourceUri) && order.current()!=null && order.current().uri().equals(track.uri()))return;int index=-1;for(int i=0;i<context.size();i++)if(context.get(i).uri().equals(track.uri())){index=i;break;}order.set(index>=0?context:Collections.singletonList(track),index>=0?index:0);}
    public void start(List<MediaRef> tracks,int index){MediaRef selected=tracks.get(index);if(!selected.playable()){error="1.7已移除抖音链接功能";return;}List<MediaRef> active=new ArrayList<>();int chosen=0;for(MediaRef item:tracks)if(item.playable()){if(item==selected)chosen=active.size();active.add(item);}cancelResolve();order.set(active,chosen);play(order.current());}
    public void enqueue(MediaRef track){if(!track.playable()){error="1.7已移除抖音链接功能";return;}if(order.current()==null && service.sourceUri!=null){String platform=service.onlineUrl==null?"":OnlineLink.parse(service.onlineUrl).platform();order.set(Collections.singletonList(new MediaRef(service.sourceUri,service.sourceName,true,platform)),0);}order.enqueue(track);}
    public void next(boolean manual){MediaRef next=order.advance(manual);if(next!=null)play(next);}
    public void select(int index){order.select(index);play(order.current());}
    public void cancelResolve(){generation++;loading=false;error="";if(resolving!=null)resolving.cancel(true);}
    private void play(MediaRef track){if(!track.playable()){error="1.7已移除抖音链接功能";return;}cancelResolve();service.timerFinished=false;int token=generation;service.player.pause();service.sourceUri=track.uri();service.sourceName=track.title();service.onlineUrl=track.online()?track.uri():null;if(track.online()){loading=true;service.player.stop();resolving=resolver.submit(()->{try{OnlineResolver.Result result=new OnlineResolver(service).resolve(new OnlineLibrary.Entry(track.uri(),track.title(),track.platform()));if(result.mediaUrl()==null)throw new java.io.IOException("平台未提供可播放地址");main.post(()->{if(closed || token!=generation)return;loading=false;service.playOnline(result,track.uri(),track.title(),0);});}catch(Exception e){main.post(()->{if(closed || token!=generation)return;loading=false;error="无法播放“"+track.title()+"”："+e.getMessage();});}});}else{service.player.setWakeMode(C.WAKE_MODE_LOCAL);service.player.setMediaItem(new MediaItem.Builder().setUri(track.uri()).setMediaMetadata(new MediaMetadata.Builder().setTitle(track.title()).build()).build());service.player.prepare();service.player.play();}}
    public void close(){closed=true;cancelResolve();resolver.shutdownNow();main.removeCallbacksAndMessages(null);}
}
