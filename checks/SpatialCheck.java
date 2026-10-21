import com.longjiahao.asmrplayer.*;
public class SpatialCheck {
 private static int checks;
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 private static float[][] impulse(int rate,long start,int mode){BinauralOrbit orbit=new BinauralOrbit(rate);float[][] response=new float[2][(int)Math.ceil(128*rate/44100.0)+2];float[] out=new float[2];SoundEngine.Settings s=new SoundEngine.Settings(mode,false,3,1,true);for(int i=0;i<response[0].length;i++){orbit.push(i==0?.5f:0,i==0?(mode==0?-.5f:.5f):0);orbit.render(start+i,s,out);response[0][i]=out[0];response[1][i]=out[1];}return response;}
 private static double energy(float[] a){double sum=0;for(float v:a)sum+=v*v;return sum;}
 public static void main(String[] args){
  for(int rate:new int[]{8000,44100,48000,96000}){float[][] left=impulse(rate,0,3),right=impulse(rate,(long)(rate*1.5),3),front=impulse(rate,(long)(rate*.75),3),back=impulse(rate,(long)(rate*2.25),3);check(energy(left[0])>energy(left[1])*3,"left dominance "+rate);check(energy(right[1])>energy(right[0])*3,"right dominance "+rate);check(Math.abs(energy(front[0])-energy(front[1]))<.01,"front center "+rate);double difference=0;for(int i=0;i<front[0].length;i++)difference+=Math.abs(front[0][i]-back[0][i]);check(difference>.01,"front/back distinct "+rate);float[][] antiphase=impulse(rate,0,0);check(energy(antiphase[0])+energy(antiphase[1])>.0001,"stereo antiphase retained "+rate);}
  float[] out=new float[2];BinauralOrbit orbit=new BinauralOrbit(48000);SoundEngine.Settings s=new SoundEngine.Settings(0,true,.1,2,true);for(int i=0;i<9600;i++){orbit.push((float)Math.sin(i*.17)*.4f,(float)Math.sin(i*.31)*.3f);orbit.render(i,s,out);check(Float.isFinite(out[0]) && Float.isFinite(out[1]),"finite");}
  check(SwipeSeek.target(100000,100,1000,1000000)==112000,"swipe forward");check(SwipeSeek.target(100000,-100,1000,1000000)==88000,"swipe back");check(SwipeSeek.target(1000,-1000,1000,1000000)==0,"clamp beginning");check(SwipeSeek.target(990000,1000,1000,1000000)==1000000,"clamp end");check(SwipeSeek.target(100000,100,0,1000000)==100000,"zero viewport");
  System.out.println("SPATIAL_SWIPE_CHECK_OK: "+checks+" assertions");
 }
}
