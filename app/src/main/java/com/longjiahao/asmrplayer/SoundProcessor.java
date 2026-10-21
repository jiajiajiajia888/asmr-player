package com.longjiahao.asmrplayer;

import androidx.media3.common.C;
import androidx.media3.common.audio.BaseAudioProcessor;
import androidx.media3.common.audio.AudioProcessor;
import java.nio.ByteBuffer;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class SoundProcessor extends BaseAudioProcessor {
    public volatile SoundEngine.Settings settings=SoundEngine.ORIGINAL;
    private long frames;
    private BinauralOrbit orbit;
    private final float[] result=new float[2];
    private SoundEngine.Settings previous=SoundEngine.ORIGINAL;
    private int transition;
    private final float[] oldResult=new float[2];
    @Override protected AudioFormat onConfigure(AudioFormat input) throws AudioProcessor.UnhandledAudioFormatException {
        if((input.encoding!=C.ENCODING_PCM_16BIT && input.encoding!=C.ENCODING_PCM_FLOAT && input.encoding!=C.ENCODING_PCM_24BIT && input.encoding!=C.ENCODING_PCM_32BIT && input.encoding!=C.ENCODING_PCM_8BIT) || input.channelCount<1)throw new AudioProcessor.UnhandledAudioFormatException(input);
        return new AudioFormat(input.sampleRate,2,C.ENCODING_PCM_16BIT);
    }
    @Override public void queueInput(ByteBuffer input) {
        if(orbit==null)orbit=new BinauralOrbit(inputAudioFormat.sampleRate);
        int channels=inputAudioFormat.channelCount;
        int count=input.remaining()/inputAudioFormat.bytesPerFrame;
        ByteBuffer out=replaceOutputBuffer(count*4);
        SoundEngine.Settings current=settings;
        int fade=Math.max(1,inputAudioFormat.sampleRate/50);
        if(!current.equals(previous) && transition==0)transition=fade;
        for(int n=0;n<count;n++) {
            float left=sample(input);
            float right=channels==1?left:sample(input);
            if(channels>2){float total=left+right;for(int c=2;c<channels;c++)total+=sample(input);left=right=total/channels;}
            orbit.push(left,right,current.autoThresholdDb());orbit.render(frames,current,result);
            if(transition>0){orbit.render(frames,previous,oldResult);float t=1-transition/(float)fade;result[0]=oldResult[0]*(1-t)+result[0]*t;result[1]=oldResult[1]*(1-t)+result[1]*t;transition--;}
            out.putShort((short)Math.max(-32768,Math.min(32767,Math.round(result[0]*32768))));
            out.putShort((short)Math.max(-32768,Math.min(32767,Math.round(result[1]*32768))));frames++;
        }
        if(transition==0)previous=current;
        out.flip();
    }
    private float sample(ByteBuffer input){return switch(inputAudioFormat.encoding){
        case C.ENCODING_PCM_FLOAT -> input.getFloat();
        case C.ENCODING_PCM_32BIT -> input.getInt()/2147483648f;
        case C.ENCODING_PCM_24BIT -> ((input.get()&255)|((input.get()&255)<<8)|(input.get()<<16))/8388608f;
        case C.ENCODING_PCM_8BIT -> ((input.get()&255)-128)/128f;
        default -> input.getShort()/32768f;
    };}
    @Override protected void onFlush(){frames=0;previous=settings;transition=0;if(inputAudioFormat.sampleRate>0)orbit=new BinauralOrbit(inputAudioFormat.sampleRate);}
}
