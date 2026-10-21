import com.longjiahao.asmrplayer.SeekMath;

public class SeekMathCheck {
    public static void main(String[] args) {
        long[] steps = {10000,30000,60000};
        for(long step:steps) {
            check(SeekMath.offset(120000,step,300000)==120000+step,"forward "+step);
            check(SeekMath.offset(120000,-step,300000)==120000-step,"backward "+step);
            check(SeekMath.offset(1000,-step,300000)==0,"start "+step);
            check(SeekMath.offset(299000,step,300000)==300000,"end "+step);
        }
        check(SeekMath.fromProgress(0,3600000)==0,"slider start");
        check(SeekMath.fromProgress(5000,3600000)==1800000,"slider middle");
        check(SeekMath.fromProgress(10000,3600000)==3600000,"slider end");
        check(SeekMath.fromProgress(5000,-1)==0,"unknown duration");
        check(SeekMath.offset(Long.MAX_VALUE-1,60000,Long.MAX_VALUE)==Long.MAX_VALUE,"overflow");
        System.out.println("SEEK_CHECK_OK: 17 assertions");
    }
    private static void check(boolean ok,String detail) {if(!ok)throw new AssertionError(detail);}
}
