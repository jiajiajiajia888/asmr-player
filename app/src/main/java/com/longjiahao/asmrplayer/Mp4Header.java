package com.longjiahao.asmrplayer;
public final class Mp4Header {
 private Mp4Header(){}
 public static boolean valid(byte[] bytes){return bytes.length>=12 && bytes[4]=='f' && bytes[5]=='t' && bytes[6]=='y' && bytes[7]=='p';}
}
