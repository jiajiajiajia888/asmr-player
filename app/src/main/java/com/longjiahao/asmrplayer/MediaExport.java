package com.longjiahao.asmrplayer;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Format;
import android.media.metrics.LogSessionId;
import androidx.media3.transformer.*;
import java.io.File;
import java.util.List;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class MediaExport {
    public interface Callback {void completed(File file);void failed(String message);}
    private Transformer transformer;
    public void start(Context context,Uri source,SoundEngine.Settings settings,Callback callback) {
        try {
            File file=new File(context.getCacheDir(),"asmr-processing.mp4");
            SoundProcessor processor=new SoundProcessor();processor.settings=settings;
            EditedMediaItem item=new EditedMediaItem.Builder(MediaItem.fromUri(source)).setEffects(new Effects(List.of(processor),List.of())).build();
            Composition composition=new Composition.Builder(new EditedMediaItemSequence.Builder(item).build()).setTransmuxVideo(true).build();
            DefaultEncoderFactory audioEncoder=new DefaultEncoderFactory.Builder(context).setRequestedAudioEncoderSettings(new AudioEncoderSettings.Builder().setBitrate(256000).build()).build();
            Codec.EncoderFactory onlyAudio=new Codec.EncoderFactory(){
                public Codec createForAudioEncoding(Format format,LogSessionId session) throws ExportException{return audioEncoder.createForAudioEncoding(format,session);}
                public Codec createForVideoEncoding(Format format,LogSessionId session) throws ExportException{throw ExportException.createForUnexpected(new IllegalArgumentException("此视频画面无法直接复制到MP4，已停止以保留原画面"));}
            };
            transformer=new Transformer.Builder(context).setAudioMimeType(MimeTypes.AUDIO_AAC).setEncoderFactory(onlyAudio).addListener(new Transformer.Listener(){
                @Override public void onCompleted(Composition c,ExportResult result){transformer=null;callback.completed(file);}
                @Override public void onError(Composition c,ExportResult result,ExportException error){transformer=null;callback.failed("处理失败："+error.getMessage()+"。可能是文件编码无法解码、画面不兼容MP4或存储空间不足。");}
            }).build();
            transformer.start(composition,file.getAbsolutePath());
        }catch(Exception error){transformer=null;callback.failed("无法开始处理："+error.getMessage());}
    }
    public int progress(){if(transformer==null)return -1;ProgressHolder holder=new ProgressHolder();return transformer.getProgress(holder)==Transformer.PROGRESS_STATE_AVAILABLE?holder.progress:-1;}
    public void cancel(){if(transformer!=null){transformer.cancel();transformer=null;}}
}
