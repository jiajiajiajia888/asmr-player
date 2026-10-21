import com.longjiahao.asmrplayer.*;
public class Release162Check {
 static int checks;static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 public static void main(String[] args){
  check(EffectDefaults.CYCLE_TENTHS==45 && EffectDefaults.WIDTH==200 && EffectDefaults.DISTANCE==50 && EffectDefaults.MAX_WIDTH==300,"requested defaults");
  for(int rate:new int[]{8000,44100,48000,96000})for(boolean surround:new boolean[]{false,true}){
   BinauralOrbit old=new BinauralOrbit(rate),wide=new BinauralOrbit(rate);float[] a=new float[2],b=new float[2];double difference=0;
   SoundEngine.Settings s2=new SoundEngine.Settings(0,!surround,4.5,2,surround,-30,.5),s3=new SoundEngine.Settings(0,!surround,4.5,3,surround,-30,.5);
   for(int i=0;i<rate;i++){float x=(float)(.2*Math.sin(i*.09));old.push(x,x);wide.push(x,x);old.render(i,s2,a);wide.render(i,s3,b);check(Float.isFinite(b[0]) && Float.isFinite(b[1]) && Math.abs(b[0])<=1 && Math.abs(b[1])<=1,"300 percent finite");difference+=Math.abs(a[0]-b[0])+Math.abs(a[1]-b[1]);}
   check(difference>1,"300 must affect audio rather than just slider");
  }
  check(Mp4Header.valid(new byte[]{0,0,0,24,'f','t','y','p','i','s','o','m'}),"MP4 accepted");check(!Mp4Header.valid("<html>error!!".getBytes()),"HTML rejected");check(!Mp4Header.valid(new byte[3]),"truncated rejected");
  boolean invalid=false;try{new SoundEngine.Settings(0,true,4.5,3.01);}catch(IllegalArgumentException expected){invalid=true;}check(invalid,"maximum enforced");
  System.out.println("RELEASE_162_CHECK_OK: "+checks+" assertions");
 }
}
