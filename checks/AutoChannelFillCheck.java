import com.longjiahao.asmrplayer.*;
public class AutoChannelFillCheck {
 static int checks;static void check(boolean ok,String msg){checks++;if(!ok)throw new AssertionError(msg);}
 static void near(float actual,float expected,String msg){check(Math.abs(actual-expected)<.0001,msg);}
 public static void main(String[] args){
  for(int rate:new int[]{8000,44100,48000,96000}){
   AutoChannelFill fill=new AutoChannelFill(rate);float[] out=new float[2];
   for(int i=0;i<rate/3;i++)fill.process(.2f,0,-45,out);near(out[0],.2f,"left source preserved");near(out[1],.2f,"left to right");
   for(int i=0;i<rate/2;i++){fill.process(0,.3f,-45,out);check(Math.abs(out[1])>=.2999,"new right source never erased");}near(out[0],.3f,"right to left after direction switch");
   for(int i=0;i<rate/2;i++)fill.process(.15f,-.2f,-45,out);near(out[0],.15f,"normal stereo restored");near(out[1],-.2f,"normal stereo right");
   for(int i=0;i<rate/3;i++)fill.process(0,0,-45,out);near(out[0],0,"silence left");near(out[1],0,"silence right");
   AutoChannelFill whisper=new AutoChannelFill(rate);for(int i=0;i<rate/3;i++)whisper.process(.001f,0,-45,out);near(out[0],.001f,"quiet ASMR source preserved");near(out[1],.001f,"quiet ASMR copied");
   AutoChannelFill normalQuiet=new AutoChannelFill(rate);for(int i=0;i<rate/3;i++)normalQuiet.process(.002f,.001f,-45,out);near(out[0],.002f,"both quiet normal left");near(out[1],.001f,"both quiet stereo retained");
   AutoChannelFill shortGap=new AutoChannelFill(rate);for(int i=0;i<rate/3;i++)shortGap.process(.1f,.1f,-45,out);for(int i=0;i<rate/100;i++)shortGap.process(.1f,0,-45,out);near(out[1],0,"10ms gap should not trigger");
   AutoChannelFill strict=new AutoChannelFill(rate),sensitive=new AutoChannelFill(rate);float[] a=new float[2],b=new float[2];for(int i=0;i<rate/3;i++){strict.process(.1f,.002f,-70,a);sensitive.process(.1f,.002f,-20,b);}near(a[1],.002f,"strict threshold keeps channel");check(b[1]>.099f && b[1]<.103f,"sensitive threshold adds source without erasing quiet signal");
   BinauralOrbit orbit=new BinauralOrbit(rate);SoundEngine.Settings auto=new SoundEngine.Settings(4,false,3,1,false,-45);for(int i=0;i<rate/3;i++){orbit.push(.2f,0,auto.autoThresholdDb());orbit.render(i,auto,out);}near(out[1],.2f,"wired to orbit processing");
   float[] original=new float[2];orbit.render(rate/3,new SoundEngine.Settings(0,false,3,1),original);near(original[1],0,"original path unaffected by detector");
   for(int i=0;i<rate/4;i++){orbit.push(.2f,0);orbit.render(i,new SoundEngine.Settings(4,true,3,1,true),out);check(Float.isFinite(out[0]) && Float.isFinite(out[1]),"auto with HRTF finite");}
  }
  BinauralOrbit antiphase=new BinauralOrbit(48000);float[] stereo=new float[2];double energy=0;SoundEngine.Settings both=new SoundEngine.Settings(4,false,3,2,true);
  for(int i=0;i<2400;i++){float tone=(float)Math.sin(i*.3)*.2f;antiphase.push(tone,-tone);antiphase.render(i,both,stereo);energy+=stereo[0]*stereo[0]+stereo[1]*stereo[1];}check(energy>1,"normal antiphase stereo must not disappear with auto+surround");
  boolean rejected=false;try{new SoundEngine.Settings(4,false,3,1,false,-71);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"bad threshold rejected");
  System.out.println("AUTO_CHANNEL_FILL_OK: "+checks+" assertions");
 }
}
