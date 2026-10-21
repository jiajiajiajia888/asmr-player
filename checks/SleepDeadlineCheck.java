import com.longjiahao.asmrplayer.SleepDeadline;
public class SleepDeadlineCheck {
    private static int count;
    private static void check(boolean ok){count++;if(!ok)throw new AssertionError("check "+count);}
    public static void main(String[] args){
        SleepDeadline timer=new SleepDeadline();check(!timer.active());check(!timer.expired(0));check(timer.remaining(0)==0);
        for(double minutes:new double[]{30,60,0.5,45.25,1440}){long start=123456;timer.set(start,minutes);long duration=Math.round(minutes*60000);check(timer.active());check(timer.remaining(start)==duration);check(!timer.expired(start+duration-1));check(timer.expired(start+duration));check(timer.remaining(start+duration+1000)==0);timer.cancel();check(!timer.active());check(!timer.expired(start+duration));}
        timer.set(100,30);timer.set(200,60);check(timer.remaining(200)==3600000);check(!timer.expired(1800100));timer.cancel();check(timer.remaining(200)==0);
        for(double invalid:new double[]{0,-1,1440.1,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}){boolean rejected=false;try{timer.set(0,invalid);}catch(IllegalArgumentException e){rejected=true;}check(rejected);}
        timer.set(0,0.000001);check(timer.remaining(0)==1);check(timer.expired(1));
        System.out.println("SLEEP_CHECK_OK: "+count+" assertions");
    }
}
