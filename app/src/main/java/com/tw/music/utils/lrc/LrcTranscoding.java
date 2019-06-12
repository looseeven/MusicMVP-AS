package com.tw.music.utils.lrc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

public class LrcTranscoding {
	public static String converfile(String filepath){
		File file=new File(filepath);
		FileInputStream fis=null;
		BufferedInputStream bis=null;
		BufferedReader reader=null;
		String text="";
		try{
			fis=new FileInputStream(file);
			bis=new BufferedInputStream(fis);
			bis.mark(4);
			byte[] first3bytes=new byte[3];
			//System.out.println("");
			//找到文档的前三个字节并自动判断文档类型。
			bis.read(first3bytes);
			bis.reset();
			if(first3bytes[0] == (byte) 0xEF && first3bytes[1] == (byte) 0xBB
					&& first3bytes[2] == (byte) 0xBF){//utf-8
				reader=new BufferedReader(new InputStreamReader(bis,"utf-8"));
				Log.i("md", "utf-8");

			}else if(first3bytes[0]==(byte)0xFF
					&&first3bytes[1]==(byte)0xFE){

				reader=new BufferedReader(
						new InputStreamReader(bis,"unicode"));
				Log.i("md", "unicode");
			}else if(first3bytes[0]==(byte)0xFE
					&&first3bytes[1]==(byte)0xFF){

				reader=new BufferedReader(new InputStreamReader(bis,
						"utf-16be"));
				Log.i("md", "utf-16be");
			}else if(first3bytes[0]==(byte)0xFF
					&&first3bytes[1]==(byte)0xFF){
				reader=new BufferedReader(new InputStreamReader(bis,
						"utf-16le"));
				Log.i("md", "utf-16le");
			}else{
				reader=new BufferedReader(new InputStreamReader(bis,"GBK"));
				Log.i("md", "GBK");
			}
			String str=reader.readLine();

			while(str!=null){
				text=text+str+ "\n";
				str=reader.readLine();
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(fis!=null){
				try{
					fis.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			if(bis!=null){
				try{
					bis.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
		return text;
	}
}
