import com.longjiahao.asmrplayer.*;
import java.util.*;
public class QueueReorderCheck {
 static int checks;static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 public static void main(String[] args){
  List<MediaRef> tracks=new ArrayList<>();for(int i=0;i<5;i++)tracks.add(new MediaRef("track"+i,"曲目"+i,false,""));
  for(int current=0;current<5;current++)for(int from=0;from<5;from++)for(int to=0;to<5;to++){
   PlaybackOrder q=new PlaybackOrder(new Random(4));q.set(tracks,current);q.enqueue(tracks.get(4));MediaRef playing=q.current();List<MediaRef> expected=new ArrayList<>(tracks);expected.add(to,expected.remove(from));q.move(from,to);
   check(q.current().equals(playing),"playing identity preserved");check(q.items().equals(expected),"requested order");check(q.nextUp().equals(Collections.singletonList(tracks.get(4))),"priority unchanged");
   q.set(tracks,current);q.move(from,to);q.mode(PlaybackOrder.ORDER);MediaRef next=q.advance(false);check(next==null?q.index()==4:next.equals(expected.get(expected.indexOf(playing)+1)),"next follows new order");
  }
  PlaybackOrder q=new PlaybackOrder();q.set(tracks,0);q.enqueue(tracks.get(1));q.enqueue(tracks.get(2));q.enqueue(tracks.get(3));q.moveNextUp(2,0);check(q.advance(false).equals(tracks.get(3)),"priority reordered");check(q.advance(false).equals(tracks.get(1)),"priority remaining preserved");
  q.select(0);check(q.current().equals(tracks.get(0)),"select item");check(q.nextUp().size()==1,"select preserves pending");
  q.mode(PlaybackOrder.SHUFFLE);q.advance(false);q.move(0,4);for(int i=0;i<100;i++)check(q.items().contains(q.advance(false)),"shuffle after move valid");
  boolean rejected=false;try{q.move(-1,0);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"invalid move");
  System.out.println("QUEUE_REORDER_CHECK_OK: "+checks+" assertions");
 }
}
