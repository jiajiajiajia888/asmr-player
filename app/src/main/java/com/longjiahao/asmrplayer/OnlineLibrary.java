package com.longjiahao.asmrplayer;
import android.content.*;
import org.json.*;
import java.util.*;
/** Persistent share URLs only; expiring CDN media addresses are never stored. */
public final class OnlineLibrary {
 public record Entry(String url,String title,String platform){}
 private final SharedPreferences prefs;private final ArrayList<Entry> entries=new ArrayList<>();
 public OnlineLibrary(Context context){prefs=context.getSharedPreferences("online-library",Context.MODE_PRIVATE);try{JSONArray rows=new JSONArray(prefs.getString("entries","[]"));for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);entries.add(new Entry(row.getString("url"),row.getString("title"),row.getString("platform")));}}catch(JSONException ignored){}}
 public List<Entry> list(){return new ArrayList<>(entries);}
 public void save(Entry entry){for(int i=0;i<entries.size();i++)if(entries.get(i).url().equals(entry.url())){entries.set(i,entry);persist();return;}entries.add(entry);persist();}
 public void remove(String url){entries.removeIf(e->e.url().equals(url));persist();}
 private void persist(){JSONArray rows=new JSONArray();for(Entry e:entries){JSONObject row=new JSONObject();try{row.put("url",e.url());row.put("title",e.title());row.put("platform",e.platform());rows.put(row);}catch(JSONException error){throw new IllegalStateException(error);}}prefs.edit().putString("entries",rows.toString()).apply();}
}
