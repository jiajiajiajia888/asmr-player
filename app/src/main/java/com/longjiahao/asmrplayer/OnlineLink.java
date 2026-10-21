package com.longjiahao.asmrplayer;
import java.net.URI;
import java.util.regex.*;
/** Extract only known platform links from Android's full share text. */
public final class OnlineLink {
 public record Link(String url,String platform){}
 public static Link parse(String text){boolean removed=false;Matcher m=Pattern.compile("https?://[^\\s<>\"，。！？]+",Pattern.CASE_INSENSITIVE).matcher(text==null?"":text);while(m.find()){String value=m.group().replaceAll("[)）\\]】,;；]+$","");try{URI u=URI.create(value);String host=u.getHost();if(host==null || u.getUserInfo()!=null || u.getPort()!=-1 && u.getPort()!=443)continue;host=host.toLowerCase(java.util.Locale.ROOT);String platform=platform(host);if("douyin".equals(platform)){removed=true;continue;}if("bili".equals(platform))return new Link(value.replaceFirst("(?i)^http:","https:"),platform);}catch(IllegalArgumentException ignored){}}throw new IllegalArgumentException(removed?"1.7已移除抖音链接功能，仅支持B站链接":"请分享或粘贴B站的视频链接");}
 public static String platform(String host){if(host.equals("b23.tv") || host.equals("bilibili.com") || host.endsWith(".bilibili.com"))return "bili";if(host.equals("douyin.com") || host.endsWith(".douyin.com") || host.equals("iesdouyin.com") || host.endsWith(".iesdouyin.com"))return "douyin";return null;}
 public static String safeName(String title){String value=title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_").trim();if(value.isBlank())value="在线视频";return value.length()>100?value.substring(0,100):value;}
 private OnlineLink(){}
}
