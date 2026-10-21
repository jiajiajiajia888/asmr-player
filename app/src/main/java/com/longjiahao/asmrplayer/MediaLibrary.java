package com.longjiahao.asmrplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Stores document references only; it never reads or copies media contents. */
public final class MediaLibrary {
    public record Entry(String uri,String name,boolean video) {}
    private final SharedPreferences prefs;
    private final ArrayList<Entry> entries=new ArrayList<>();
    public MediaLibrary(Context context){
        prefs=context.getSharedPreferences("media-library",Context.MODE_PRIVATE);
        try {JSONArray data=new JSONArray(prefs.getString("entries","[]"));for(int i=0;i<data.length();i++){JSONObject row=data.getJSONObject(i);entries.add(new Entry(row.getString("uri"),row.getString("name"),row.getBoolean("video")));}}
        catch(Exception ignored){ /* Preserve stored data until a successful subsequent mutation. */ }
    }
    public List<Entry> list(boolean video){ArrayList<Entry> result=new ArrayList<>();for(Entry e:entries)if(e.video()==video)result.add(e);return result;}
    public Entry find(String uri){for(Entry e:entries)if(e.uri().equals(uri))return e;return null;}
    public void add(Uri uri,String name,boolean video){
        String key=uri.toString();for(int i=0;i<entries.size();i++)if(entries.get(i).uri().equals(key)){entries.set(i,new Entry(key,name,video));save();return;}
        entries.add(new Entry(key,name,video));save();
    }
    public void remove(String uri){entries.removeIf(e->e.uri().equals(uri));save();}
    private void save(){
        JSONArray data=new JSONArray();for(Entry e:entries){JSONObject row=new JSONObject();try{row.put("uri",e.uri());row.put("name",e.name());row.put("video",e.video());data.put(row);}catch(Exception error){throw new IllegalStateException("列表记录失败",error);}}
        prefs.edit().putString("entries",data.toString()).apply();
    }
}
