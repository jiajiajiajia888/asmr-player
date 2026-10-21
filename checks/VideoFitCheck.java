import com.longjiahao.asmrplayer.VideoFit;
public class VideoFitCheck {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("case "+checks);}
    public static void main(String[] args){
        VideoFit.Size landscape=VideoFit.fit(1920,1080,16.0/9);check(landscape.width()==1920 && landscape.height()==1080);
        VideoFit.Size portrait=VideoFit.fit(1920,1080,9.0/16);check(portrait.width()==608 && portrait.height()==1080);
        VideoFit.Size square=VideoFit.fit(1920,1080,1);check(square.width()==1080 && square.height()==1080);
        check(VideoFit.fit(0,1080,1).width()==0);check(VideoFit.fit(1920,0,1).height()==0);
        check(VideoFit.fit(1920,1080,Double.NaN).equals(landscape));check(VideoFit.fit(1920,1080,0).equals(landscape));check(VideoFit.fit(1920,1080,Double.POSITIVE_INFINITY).equals(landscape));
        java.util.Random random=new java.util.Random(1234);
        for(int i=0;i<100;i++){int w=320+random.nextInt(2400),h=200+random.nextInt(1600);double ratio=0.25+random.nextDouble()*4;VideoFit.Size size=VideoFit.fit(w,h,ratio);check(size.width()>0 && size.width()<=w && size.height()>0 && size.height()<=h);check(size.width()==w || size.height()==h);check(Math.abs(size.width()-size.height()*ratio)<=Math.max(1,ratio)*0.51);}
        System.out.println("VIDEO_FIT_CHECK_OK: "+checks+" assertions");
    }
}
