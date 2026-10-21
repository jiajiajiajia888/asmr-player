import com.longjiahao.asmrplayer.*;
public class AudioQualityCheck {
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
 public static void main(String[] args){
  int rate=48000;float[] out=new float[2];AutoChannelFill fill=new AutoChannelFill(rate);
  for(int i=0;i<rate/2;i++)fill.process(.995f,.995f,-30,out);
  check(Math.abs(out[0]-.995f)<1e-6,"normal stereo peaks must remain unchanged");
  BinauralOrbit center=new BinauralOrbit(rate);SoundEngine.Settings s=new SoundEngine.Settings(3,false,3,1);
  for(int i=0;i<rate/2;i++){center.push(.2f,0);center.render(i,s,out);}
  check(Math.abs(out[0]-.2f)<.002,"single-ear center must not halve amplitude");
  center=new BinauralOrbit(rate);double energy=0;
  for(int i=0;i<rate;i++){float x=(float)(.2*Math.sin(i*.07));center.push(x,-x);center.render(i,s,out);if(i>rate/2)energy+=out[0]*out[0];}
  check(energy>200,"antiphase center must not become silence");
  for(int sampleRate:new int[]{8000,44100,48000,96000}){
   double previousEnergy=-1;
   for(double distance:new double[]{0,.45,1}){
    BinauralOrbit room=new BinauralOrbit(sampleRate);SoundEngine.Settings spatial=new SoundEngine.Settings(0,true,3,1,false,-30,distance);
    double tail=0;for(int i=0;i<sampleRate/10;i++){room.push(i==0?.5f:0,0);room.render(i,spatial,out);check(Float.isFinite(out[0]) && Float.isFinite(out[1]) && Math.abs(out[0])<=1 && Math.abs(out[1])<=1,"distance finite bounded");if(i>sampleRate/100)tail+=out[0]*out[0]+out[1]*out[1];}
    if(distance==0)check(tail==0,"zero distance must bypass room");else check(tail>previousEnergy,"higher distance increases reflection energy");previousEnergy=tail;
   }
   BinauralOrbit bypass=new BinauralOrbit(sampleRate);SoundEngine.Settings off=new SoundEngine.Settings(0,false,3,1,false,-30,1);
   for(int i=0;i<1000;i++){float l=(float)Math.sin(i*.13)*.995f,r=(float)Math.cos(i*.17)*.9f;bypass.push(l,r);bypass.render(i,off,out);check(out[0]==l && out[1]==r,"disabled spatial exact bypass");}
  }
  System.out.println("AUDIO_QUALITY_CHECK_OK: original peaks, centered level, antiphase, distance tails, bypass");
 }
}
