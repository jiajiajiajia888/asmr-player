import com.longjiahao.asmrplayer.SoundEngine;
import java.io.*;

public class SoundEngineCheck {
    private static int assertions;
    private static final float[] out=new float[2];
    public static void main(String[] args)throws Exception {
        int rate=48000;SoundEngine.Settings pan=new SoundEngine.Settings(0,true,2,1);
        SoundEngine.process(.4f,.4f,0,rate,pan,out);near(out[0],.4,"starts left");near(out[1],0,"starts muted right");
        SoundEngine.process(.4f,.4f,rate,rate,pan,out);near(out[0],0,"half loop muted left");near(out[1],.4,"half loop right");
        SoundEngine.process(.4f,.4f,rate*2L,rate,pan,out);near(out[0],.4,"full loop left");
        SoundEngine.process(.4f,.4f,rate/2L,rate,pan,out);near(out[0],out[1],"center equal");
        for(int mode=0;mode<=3;mode++)for(boolean moving:new boolean[]{false,true})for(double period:new double[]{0.1,1,5,10})for(double width:new double[]{0,.5,1,1.5,2}){
            SoundEngine.Settings s=new SoundEngine.Settings(mode,moving,period,width);
            for(long f:new long[]{0,12000,240000,480000,1000000000L}){
                SoundEngine.process(.7f,-.3f,f,rate,s,out);
                check(Float.isFinite(out[0]) && Float.isFinite(out[1]) && Math.abs(out[0])<=1 && Math.abs(out[1])<=1,"finite bounded samples");
            }
        }
        SoundEngine.process(.3f,-.7f,0,rate,new SoundEngine.Settings(1,false,2,1),out);near(out[0],.3,"left copy source");near(out[1],.3,"left copy target");
        SoundEngine.process(.3f,-.7f,0,rate,new SoundEngine.Settings(2,false,2,1),out);near(out[0],-.7,"right copy target");near(out[1],-.7,"right copy source");
        for(int i=0;i<=100;i++) {SoundEngine.process(.5f,-.5f,rate*i/10L,rate,pan,out);near(out[0]*out[0]+out[1]*out[1],.25,"antiphase retains energy");}
        SoundEngine.process(.3f,.3f,0,rate,new SoundEngine.Settings(3,false,2,1),out);near(out[0],.3,"dual mono left");near(out[1],.3,"dual mono right");
        boolean rejected=false;try{new SoundEngine.Settings(0,true,0,1);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid period rejected");
        rejected=false;try{new SoundEngine.Settings(0,true,10.1,1);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"above maximum rejected");
        if(args.length>0)writeWave(new File(args[0]),rate,20,pan);
        System.out.println("SOUND_ENGINE_CHECK_OK: "+assertions+" assertions");
    }
    private static void check(boolean pass,String msg){assertions++;if(!pass)throw new AssertionError(msg);}
    private static void near(double actual,double expected,String msg){check(Math.abs(actual-expected)<.00001,msg+": "+actual);}
    private static void le(DataOutputStream s,long n,int bytes)throws IOException{for(int i=0;i<bytes;i++)s.writeByte((int)(n>>(8*i))&255);}
    private static void writeWave(File file,int rate,int seconds,SoundEngine.Settings pan)throws Exception {
        try(DataOutputStream s=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))){
            int size=rate*seconds*4;s.writeBytes("RIFF");le(s,size+36,4);s.writeBytes("WAVEfmt ");le(s,16,4);le(s,1,2);le(s,2,2);le(s,rate,4);le(s,rate*4,4);le(s,4,2);le(s,16,2);s.writeBytes("data");le(s,size,4);
            for(int frame=0;frame<rate*seconds;frame++){float source=(float)(.08*Math.sin(2*Math.PI*300*frame/rate));SoundEngine.process(source,source,frame,rate,pan,out);le(s,Math.round(out[0]*32767),2);le(s,Math.round(out[1]*32767),2);}
        }
    }
}
