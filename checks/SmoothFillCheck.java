import com.longjiahao.asmrplayer.AutoChannelFill;
public class SmoothFillCheck {
 public static void main(String[] args){int rate=48000;AutoChannelFill fill=new AutoChannelFill(rate);float[] out=new float[2];float previous=0;double peakStep=0;
  for(int i=0;i<rate;i++){float left=(float)(.15*Math.sin(i*2*Math.PI*220/rate)),right=(float)(.007*Math.sin(i*2*Math.PI*1730/rate));fill.process(left,right,-30,out);if(i>rate/2)peakStep=Math.max(peakStep,Math.abs(out[1]-previous));previous=out[1];}
  System.out.println("AUTO_FILL_PEAK_STEP="+peakStep);if(peakStep>.0065)throw new AssertionError("copy introduces abrupt sample steps at source zero crossings");
  AutoChannelFill fast=new AutoChannelFill(rate);for(int i=0;i<rate*.08;i++)fast.process(.2f,0,-30,out);if(out[1]<.195)throw new AssertionError("automatic fill takes too long to settle");
  AutoChannelFill peak=new AutoChannelFill(rate);for(int i=0;i<rate/4;i++){peak.process(.99f,.2f,0,out);if(!Float.isFinite(out[0]) || !Float.isFinite(out[1]) || Math.abs(out[0])>1 || Math.abs(out[1])>1)throw new AssertionError("auto crossfeed peak overload");}
  new com.longjiahao.asmrplayer.SoundEngine.Settings(4,false,3,1,false,0);new com.longjiahao.asmrplayer.SoundEngine.Settings(4,false,3,1,false,-70);
  System.out.println("SMOOTH_FILL_CHECK_OK: waveform continuity, 80ms response, peak bounds, threshold endpoints");
 }
}
