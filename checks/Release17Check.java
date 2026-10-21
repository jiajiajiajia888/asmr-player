package com.longjiahao.asmrplayer;
public final class Release17Check {
 public static void main(String[] args)throws Exception{
  int checks=0;
  for(String url:new String[]{"https://v.douyin.com/cAcNPdLnN8E/","https://www.douyin.com/video/7682768822693567993","https://www.iesdouyin.com/share/video/7682768822693567993/"}){
   try{OnlineLink.parse(url);throw new AssertionError("抖音链接被接受");}catch(IllegalArgumentException expected){checks++;}
   try{new OnlineResolver().resolve(new OnlineLibrary.Entry(url,"旧抖音记录","douyin"),true);throw new AssertionError("抖音解析未关闭");}catch(java.io.IOException expected){checks++;}
   try{new OnlineResolver().resolve(new OnlineLibrary.Entry(url,"错误平台标签","bili"),false);throw new AssertionError("平台标签伪装进入解析");}catch(IllegalArgumentException expected){checks++;}
  }
  for(String url:new String[]{"https://b23.tv/abc","https://www.bilibili.com/video/BV1234567890/"}){if(!OnlineLink.parse(url).platform().equals("bili"))throw new AssertionError("B站识别改变");checks++;}
  if(new MediaRef("https://v.douyin.com/abc","旧记录",true,"douyin").playable())throw new AssertionError("旧记录可播放");checks++;
  if(!new MediaRef("content://provider/video/抖音下载.mp4","本地视频",true,"").playable())throw new AssertionError("本地视频受影响");checks++;
  if(!new MediaRef("content://provider/music/asmr.mp3","本地音乐",false,"").playable())throw new AssertionError("本地音乐受影响");checks++;
  if(!new MediaRef("https://b23.tv/abc","B站视频",true,"bili").playable())throw new AssertionError("B站播放受影响");checks++;
  System.out.println("RELEASE_17_CHECK_OK: "+checks+" cases");
 }
}
