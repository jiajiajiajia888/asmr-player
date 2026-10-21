package com.longjiahao.asmrplayer;

import java.util.*;

/** The next-up list takes precedence over repeat and the shuffle bag. */
public final class PlaybackOrder {
    public static final int ORDER=0,LIST_LOOP=1,SINGLE_LOOP=2,SHUFFLE=3;
    public static final String[] LABELS={"顺序播放","列表循环","单曲循环","随机播放"};
    private final ArrayList<MediaRef> items=new ArrayList<>();
    private final ArrayDeque<MediaRef> nextUp=new ArrayDeque<>();
    private final ArrayList<Integer> bag=new ArrayList<>();
    private final Random random;
    private int index=-1,mode;
    public PlaybackOrder(){this(new Random());}
    public PlaybackOrder(Random random){this.random=random;}
    public void set(List<MediaRef> tracks,int selected){if(selected<0 || selected>=tracks.size())throw new IllegalArgumentException("queue index");items.clear();items.addAll(tracks);index=selected;nextUp.clear();bag.clear();}
    public void mode(int value){if(value<0 || value>3)throw new IllegalArgumentException("playback mode");mode=value;bag.clear();}
    public int mode(){return mode;}
    public List<MediaRef> items(){return new ArrayList<>(items);}
    public List<MediaRef> nextUp(){return new ArrayList<>(nextUp);}
    public MediaRef current(){return index>=0?items.get(index):null;}
    public int index(){return index;}
    public void select(int selected){if(selected<0 || selected>=items.size())throw new IllegalArgumentException("queue index");index=selected;bag.clear();}
    public void move(int from,int to){if(from<0 || to<0 || from>=items.size() || to>=items.size())throw new IllegalArgumentException("queue index");if(from==to)return;MediaRef item=items.remove(from);items.add(to,item);if(index==from)index=to;else if(from<index && to>=index)index--;else if(from>index && to<=index)index++;bag.clear();}
    public void moveNextUp(int from,int to){List<MediaRef> pending=new ArrayList<>(nextUp);if(from<0 || to<0 || from>=pending.size() || to>=pending.size())throw new IllegalArgumentException("next index");pending.add(to,pending.remove(from));nextUp.clear();nextUp.addAll(pending);}
    public void enqueue(MediaRef track){nextUp.removeIf(t->t.uri().equals(track.uri()));nextUp.addLast(track);}
    public void update(MediaRef track){for(int i=0;i<items.size();i++)if(items.get(i).uri().equals(track.uri()))items.set(i,track);List<MediaRef> pending=new ArrayList<>(nextUp);nextUp.clear();for(MediaRef item:pending)nextUp.addLast(item.uri().equals(track.uri())?track:item);}
    public MediaRef advance(boolean manual){
        if(!nextUp.isEmpty()){
            MediaRef track=nextUp.removeFirst();
            for(int i=items.size()-1;i>index;i--)if(items.get(i).uri().equals(track.uri()))items.remove(i);
            items.add(index+1,track);index++;bag.clear();return current();
        }
        if(items.isEmpty())return null;
        if(mode==SINGLE_LOOP && !manual)return current();
        if(mode==SHUFFLE){
            if(items.size()==1)return current();
            if(bag.isEmpty()){for(int i=0;i<items.size();i++)if(i!=index)bag.add(i);Collections.shuffle(bag,random);}
            index=bag.remove(bag.size()-1);return current();
        }
        int next=index+1;
        if(next>=items.size()){if(mode==ORDER)return null;next=0;}
        index=next;return current();
    }
}
