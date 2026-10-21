package com.longjiahao.asmrplayer;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

/** Public metadata/pages only. No login, signature bypass or paid-content bypass. */
public final class OnlineResolver {
 public OnlineResolver(){}
 public OnlineResolver(android.content.Context context){}
 public static final String UA="Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36";
 public record Result(String shareUrl,String title,String platform,String mediaUrl,String referer){}
 public Result resolve(OnlineLibrary.Entry entry)throws Exception{return resolve(entry,true);}
 public Result resolve(OnlineLibrary.Entry entry,boolean media)throws Exception{if(!"bili".equals(entry.platform()))throw new IOException("1.7已移除抖音链接功能，仅支持B站链接");OnlineLink.parse(entry.url());return bili(entry,media);}
 private Result bili(OnlineLibrary.Entry e,boolean mediaRequired)throws Exception{
  String page=expand(e.url());String identity;Matcher bv=Pattern.compile("BV[0-9A-Za-z]{10}").matcher(page);Matcher av=Pattern.compile("/av(\\d+)").matcher(page);
  if(bv.find())identity="bvid="+bv.group();else if(av.find())identity="aid="+av.group(1);else throw new IOException("暂不支持这个B站链接，请分享普通视频页面");
  JSONObject view=json("https://api.bilibili.com/x/web-interface/view?"+identity,"https://www.bilibili.com/");JSONObject data=success(view);String title=data.getString("title");long cid=data.getLong("cid");int part=1;Matcher pm=Pattern.compile("[?&]p=(\\d+)").matcher(page);if(pm.find())part=Integer.parseInt(pm.group(1));JSONArray pages=data.optJSONArray("pages");if(part>1){if(pages==null || part>pages.length())throw new IOException("视频分P不存在");JSONObject item=pages.getJSONObject(part-1);cid=item.getLong("cid");title+=" · "+item.optString("part","第"+part+"P");}
  String canonical="https://www.bilibili.com/video/"+(identity.startsWith("bvid=")?identity.substring(5):"av"+identity.substring(4))+(part>1?"/?p="+part:"/");
  if(!mediaRequired)return new Result(canonical,title,"bili",null,"https://www.bilibili.com/");
  JSONObject playback=success(json("https://api.bilibili.com/x/player/playurl?"+identity+"&cid="+cid+"&qn=32&fnval=0&platform=html5","https://www.bilibili.com/"));JSONArray urls=playback.optJSONArray("durl");String media=null;if(urls!=null && urls.length()==1 && playback.optString("format").contains("mp4"))media=urls.getJSONObject(0).optString("url",null);

  return new Result(canonical,title,"bili",usable(media),"https://www.bilibili.com/");
 }
 private JSONObject json(String url,String referer)throws Exception{return new JSONObject(read(url,referer));}
 private static JSONObject success(JSONObject data)throws Exception{int code=data.optInt("code",-1);if(code!=0)throw new IOException(code==-412 || code==-403?"平台暂时限制访问，请稍后重试或在原平台打开":"平台未提供该视频，可能已失效、需要登录或访问权限（"+code+"）");return data.getJSONObject("data");}
 static String usable(String value)throws IOException{if(value==null || value.isBlank())return null;if(value.startsWith("http://"))value="https://"+value.substring(7);URI uri=URI.create(value);String host=uri.getHost();if(!"https".equals(uri.getScheme()) || host==null || uri.getUserInfo()!=null || uri.getPort()!=-1 && uri.getPort()!=443 || !"bili".equals(OnlineLink.platform(host)) && !host.toLowerCase(java.util.Locale.ROOT).endsWith(".bilivideo.com") && !host.toLowerCase(java.util.Locale.ROOT).endsWith(".bilivideo.cn"))throw new IOException("平台返回了不可用的媒体地址");return value;}
 public static HttpURLConnection connect(String url,String referer)throws IOException{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setRequestProperty("User-Agent",UA);c.setRequestProperty("Referer",referer);c.setRequestProperty("Accept-Encoding","identity");return c;}
 private String expand(String url)throws Exception{HttpURLConnection c=connect(url,url);try{c.setInstanceFollowRedirects(false);for(int n=0;n<6;n++){int code=c.getResponseCode();if(code>=300 && code<400){String next=new URL(new URL(url),c.getHeaderField("Location")).toString();OnlineLink.Link parsed=OnlineLink.parse(next);if(!parsed.platform().equals(OnlineLink.parse(url).platform()))throw new IOException("分享链接跳转到其他平台");c.disconnect();url=parsed.url();c=connect(url,url);c.setInstanceFollowRedirects(false);}else{if(code>=400)throw new IOException("平台页面访问失败（"+code+"）");return url;}}throw new IOException("分享链接跳转次数过多");}finally{c.disconnect();}}
 private static String read(String url,String referer)throws IOException{HttpURLConnection c=connect(url,referer);try{if(c.getResponseCode()!=200)throw new IOException("平台访问失败（"+c.getResponseCode()+"）");try(InputStream input=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] block=new byte[16384];int n;while((n=input.read(block))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException("已取消");if(out.size()+n>4*1024*1024)throw new IOException("平台页面内容过大");out.write(block,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}}finally{c.disconnect();}}
}
