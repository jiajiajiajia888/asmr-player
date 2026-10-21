import com.longjiahao.asmrplayer.MediaRef;
import com.longjiahao.asmrplayer.PlaybackOrder;
import java.util.*;

public final class PlaybackOrderCheck {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
    public static void main(String[] args){
        MediaRef a=new MediaRef("a","A",false,""),b=new MediaRef("b","B",true,""),c=new MediaRef("https://v.douyin.com/test/","C",true,"douyin"),x=new MediaRef("x","X",false,"");
        List<MediaRef> tracks=Arrays.asList(a,b,c);PlaybackOrder q=new PlaybackOrder(new Random(5));
        check(q.advance(false)==null);
        q.set(tracks,0);check(q.advance(false).equals(b));check(q.advance(false).equals(c));check(q.advance(false)==null);
        q.set(tracks,2);q.mode(PlaybackOrder.LIST_LOOP);check(q.advance(false).equals(a));
        q.set(tracks,1);q.mode(PlaybackOrder.SINGLE_LOOP);check(q.advance(false).equals(b));check(q.advance(true).equals(c));
        q.set(tracks,0);q.mode(PlaybackOrder.ORDER);q.enqueue(c);check(q.advance(false).equals(c));check(q.advance(false).equals(b));check(q.advance(false)==null);
        q.set(tracks,0);q.mode(PlaybackOrder.SINGLE_LOOP);q.enqueue(x);q.enqueue(x);check(q.nextUp().size()==1);check(q.advance(false).equals(x));check(q.advance(false).equals(x));
        q.set(tracks,0);q.mode(PlaybackOrder.SHUFFLE);
        for(int cycle=0;cycle<100;cycle++){MediaRef current=q.current();Set<String> seen=new HashSet<>();for(int i=0;i<2;i++){MediaRef next=q.advance(false);check(!next.equals(current));check(seen.add(next.uri()));}check(seen.size()==2);}
        q.set(Collections.singletonList(a),0);check(q.advance(false).equals(a));
        List<MediaRef> copy=q.items();copy.clear();check(q.items().size()==1);
        q.enqueue(x);q.set(tracks,0);check(q.nextUp().isEmpty());
        q.enqueue(b);MediaRef updated=new MediaRef("b","新名称",false,"");q.update(updated);
        check(q.index()==0);check(q.current().equals(a));check(q.items().get(1).equals(updated));check(q.nextUp().get(0).equals(updated));check(q.advance(false).equals(updated));
        try{q.mode(4);throw new AssertionError("invalid mode");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("PlaybackOrder checks="+checks);
    }
}
