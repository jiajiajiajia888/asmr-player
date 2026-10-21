import com.longjiahao.asmrplayer.*;
public final class AlternationCheck {
 private static long checks;
 private static void check(boolean condition,String name){checks++;if(!condition)throw new AssertionError(name);}
 private static SoundEngine.Settings settings(int mode,double seconds){return new SoundEngine.Settings(mode,false,4.5,3,false,-30,1,true,seconds);}
 public static void main(String[] args){
  check(EffectDefaults.ALTERNATE_MILLIS==200,"default is 0.2 seconds per ear");
  check(!new SoundEngine.Settings(0,false,4.5,2,false,-30,.5).alternating(),"old settings remain off");
  for(int rate:new int[]{8000,44100,48000,96000})for(double seconds:new double[]{.05,.2,2}){
   long interval=Math.round(rate*seconds);float[] out=new float[2];float[] previous={0,0};
   double edge=Math.max(1,Math.min(interval/4,Math.round(rate*.002)));
   for(long frame=0;frame<interval*4;frame++){
    LeftRightAlternation.process(.6f,frame,rate,seconds,out);int active=(int)((frame/interval)&1);
    check(out[1-active]==0,"inactive ear exactly silent");
    check(Float.isFinite(out[active])&&out[active]>=0&&out[active]<=.600001,"finite bounded active signal");
    check(Math.abs(out[0]-previous[0])<=.6*Math.PI/(2*edge)+1e-6&&Math.abs(out[1]-previous[1])<=.6*Math.PI/(2*edge)+1e-6,"no full-level switching step");
    if(frame%interval==interval/2)check(Math.abs(out[active]-.6)<1e-6,"correct ear and full level at half dwell");
    previous[0]=out[0];previous[1]=out[1];
   }
  }
  int rate=48000;float[] out=new float[2];
  for(int mode=0;mode<=4;mode++){
   BinauralOrbit engine=new BinauralOrbit(rate);SoundEngine.Settings s=settings(mode,.2);double leftEnergy=0,rightEnergy=0;
   for(int frame=0;frame<rate;frame++){
    engine.push(.3f,.3f);engine.render(frame,s,out);int active=(frame/9600)&1;
    check(out[1-active]==0,"shared processor bypasses spatial tails in alternating mode");
    if(frame>rate/4){leftEnergy+=out[0]*out[0];rightEnergy+=out[1]*out[1];}
    float l=out[0],r=out[1];engine.render(frame,s,out);check(out[0]==l&&out[1]==r,"same-frame transition rendering does not advance detection twice");
   }
   check(leftEnergy>100&&rightEnergy>100,"both ears receive the full source over time");
  }
  for(float[] source:new float[][]{{.4f,0},{0,.4f},{.4f,-.4f},{0,0}}){
   BinauralOrbit engine=new BinauralOrbit(rate);double energy=0;
   for(int frame=0;frame<rate;frame++){engine.push(source[0],source[1]);engine.render(frame,settings(0,.2),out);if(frame>rate/2)energy+=out[0]*out[0]+out[1]*out[1];}
   check(source[0]==0&&source[1]==0?energy==0:energy>100,"single-ear, opposing-polarity, and silent sources");
  }
  for(double invalid:new double[]{0,.049,2.01,Double.NaN,Double.POSITIVE_INFINITY}){try{settings(0,invalid);throw new AssertionError("invalid interval accepted");}catch(IllegalArgumentException expected){checks++;}}
  try{new SoundEngine.Settings(0,true,4.5,2,false,-30,.5,true,.2);throw new AssertionError("conflicting effects accepted");}catch(IllegalArgumentException expected){checks++;}
  System.out.println("ALTERNATION_CHECK_OK: "+checks+" assertions");
 }
}
