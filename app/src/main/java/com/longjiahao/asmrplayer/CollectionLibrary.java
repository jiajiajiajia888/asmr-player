package com.longjiahao.asmrplayer;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

public final class CollectionLibrary {
    public static final String FAVORITES="favorites";
    public record Collection(String id,String name,List<MediaRef> items){}
    private final SharedPreferences prefs;
    private final LinkedHashMap<String,Collection> lists=new LinkedHashMap<>();
    public CollectionLibrary(Context context){
        prefs=context.getSharedPreferences("collections",Context.MODE_PRIVATE);
        lists.put(FAVORITES,new Collection(FAVORITES,"收藏",new ArrayList<>()));
        try{JSONArray data=new JSONArray(prefs.getString("lists","[]"));for(int i=0;i<data.length();i++){JSONObject row=data.getJSONObject(i);String id=row.getString("id");List<MediaRef> items=new ArrayList<>();JSONArray tracks=row.getJSONArray("items");for(int j=0;j<tracks.length();j++){JSONObject t=tracks.getJSONObject(j);items.add(new MediaRef(t.getString("uri"),t.getString("title"),t.getBoolean("video"),t.optString("platform","")));}lists.put(id,new Collection(id,id.equals(FAVORITES)?"收藏":row.getString("name"),items));}}catch(JSONException ignored){}
    }
    public List<Collection> lists(){List<Collection> result=new ArrayList<>();for(Collection c:lists.values())result.add(new Collection(c.id(),c.name(),new ArrayList<>(c.items())));return result;}
    public Collection get(String id){Collection c=lists.get(id);return c==null?null:new Collection(c.id(),c.name(),new ArrayList<>(c.items()));}
    public boolean favorite(String uri){return lists.get(FAVORITES).items().stream().anyMatch(t->t.uri().equals(uri));}
    public void toggleFavorite(MediaRef track){if(favorite(track.uri()))remove(FAVORITES,track.uri());else add(FAVORITES,track);}
    public String create(String name){String clean=name.trim();if(clean.isEmpty())throw new IllegalArgumentException("请输入清单名称");String id=UUID.randomUUID().toString();lists.put(id,new Collection(id,clean,new ArrayList<>()));save();return id;}
    public void add(String id,MediaRef track){Collection c=lists.get(id);if(c==null)return;for(int i=0;i<c.items().size();i++)if(c.items().get(i).uri().equals(track.uri())){c.items().set(i,track);save();return;}c.items().add(track);save();}
    public void remove(String id,String uri){Collection c=lists.get(id);if(c!=null){c.items().removeIf(t->t.uri().equals(uri));save();}}
    public void rename(String id,String name){if(id.equals(FAVORITES))return;Collection c=lists.get(id);if(c==null)return;if(name.trim().isEmpty())throw new IllegalArgumentException("请输入清单名称");lists.put(id,new Collection(id,name.trim(),c.items()));save();}
    public void removeCollection(String id){if(id.equals(FAVORITES))return;lists.remove(id);save();}
    private void save(){JSONArray data=new JSONArray();try{for(Collection c:lists.values()){JSONObject row=new JSONObject();row.put("id",c.id());row.put("name",c.name());JSONArray items=new JSONArray();for(MediaRef t:c.items()){JSONObject item=new JSONObject();item.put("uri",t.uri());item.put("title",t.title());item.put("video",t.video());item.put("platform",t.platform());items.put(item);}row.put("items",items);data.put(row);}}catch(JSONException e){throw new IllegalStateException(e);}prefs.edit().putString("lists",data.toString()).apply();}
}
