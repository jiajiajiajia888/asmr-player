package com.longjiahao.asmrplayer;

public record MediaRef(String uri,String title,boolean video,String platform) {
    public boolean online(){return !platform.isEmpty();}
    public boolean playable(){return !online() || "bili".equals(platform);}
    public static MediaRef local(MediaLibrary.Entry e){return new MediaRef(e.uri(),e.name(),e.video(),"");}
    public static MediaRef online(OnlineLibrary.Entry e){return new MediaRef(e.url(),e.title(),true,e.platform());}
}
