package com.longjiahao.asmrplayer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.audio.AudioCapabilities;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class PlaybackService extends MediaSessionService {
    public static final String LOCAL_BIND="com.longjiahao.asmrplayer.LOCAL_BIND";
    public final class LocalBinder extends Binder {public PlaybackService service(){return PlaybackService.this;}}
    private final LocalBinder binder=new LocalBinder();
    public final SoundProcessor processor=new SoundProcessor();
    public ExoPlayer player;
    public QueuePlayer queue;
    public String sourceUri,sourceName;
    public String onlineUrl;
    public void playOnline(OnlineResolver.Result result,String link,String title,long position){
        androidx.media3.datasource.DefaultHttpDataSource.Factory http=new androidx.media3.datasource.DefaultHttpDataSource.Factory().setUserAgent(OnlineResolver.UA).setDefaultRequestProperties(java.util.Collections.singletonMap("Referer",result.referer()));
        androidx.media3.datasource.DefaultDataSource.Factory data=new androidx.media3.datasource.DefaultDataSource.Factory(this,http);
        androidx.media3.common.MediaItem item=new androidx.media3.common.MediaItem.Builder().setUri(result.mediaUrl()).setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()).build();
        player.setWakeMode(C.WAKE_MODE_NETWORK);onlineUrl=link;sourceUri=link;sourceName=title;player.setMediaSource(new androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(data).createMediaSource(item),Math.max(0,position));player.prepare();player.play();
    }
    private MediaSession session;
    private final SleepDeadline timer=new SleepDeadline();
    public boolean timerFinished;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){
        if(timer.expired(SystemClock.elapsedRealtime())){timer.cancel();timerFinished=true;if(queue!=null)queue.cancelResolve();player.pause();}
        if(timer.active())handler.postDelayed(this,Math.min(1000,Math.max(1,timer.remaining(SystemClock.elapsedRealtime()))));
    }};
    @Override public void onCreate(){
        super.onCreate();
        DefaultRenderersFactory factory=new DefaultRenderersFactory(this){
            @Override protected AudioSink buildAudioSink(Context context,boolean floatOutput,boolean playbackParams){return new DefaultAudioSink.Builder(context).setAudioCapabilities(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES).setEnableFloatOutput(false).setAudioProcessors(new AudioProcessor[]{processor}).build();}
        };
        androidx.media3.exoplayer.DefaultLoadControl load=new androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMsForLocalPlayback(PlaybackTuning.MIN_MS,PlaybackTuning.MAX_MS,PlaybackTuning.START_MS,PlaybackTuning.REBUFFER_MS)
            .setBackBuffer(PlaybackTuning.BACK_MS,true).build();
        player=new ExoPlayer.Builder(this,factory).setLoadControl(load).build();
        // Bounded tolerance avoids decoding an entire distant GOP for an exact seek.
        player.setSeekParameters(new androidx.media3.exoplayer.SeekParameters(PlaybackTuning.SEEK_TOLERANCE_US,PlaybackTuning.SEEK_TOLERANCE_US));
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);
        player.setHandleAudioBecomingNoisy(true);player.setWakeMode(C.WAKE_MODE_LOCAL);
        queue=new QueuePlayer(this);
        player.addListener(new androidx.media3.common.Player.Listener(){@Override public void onPlayWhenReadyChanged(boolean ready,int reason){if(ready)checkTimer();}});
        PendingIntent launch=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        session=new MediaSession.Builder(this,player).setSessionActivity(launch).build();addSession(session);
    }
    @Override public MediaSession onGetSession(MediaSession.ControllerInfo info){return session;}
    @Override public IBinder onBind(Intent intent){return intent!=null && LOCAL_BIND.equals(intent.getAction())?binder:super.onBind(intent);}
    public void setTimer(double minutes){timer.set(SystemClock.elapsedRealtime(),minutes);timerFinished=false;handler.removeCallbacks(tick);handler.post(tick);}
    public void cancelTimer(){timer.cancel();timerFinished=false;handler.removeCallbacks(tick);}
    public long timerRemaining(){return timer.remaining(SystemClock.elapsedRealtime());}
    public boolean timerActive(){return timer.active();}
    public void checkTimer(){if(timer.expired(SystemClock.elapsedRealtime()))tick.run();}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(queue!=null)queue.close();if(session!=null)session.release();if(player!=null)player.release();super.onDestroy();}
}
