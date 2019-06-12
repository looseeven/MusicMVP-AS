package com.tw.music.utils;

import java.io.File;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.tw.music.bean.MusicName;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class CollectionUtils {
	private static final String COLLECTION = "CollectionMusicList"; 
	private static final String COLLECTIONTAG = "TAG";
	private static SharedPreferences mSP;
			
	/**
	 * 查询收藏列表
	 * @param mMusicName 需要装载的收藏列表
	 */
	public static ArrayList<MusicName> getCollectionMusicList(Context context,ArrayList<MusicName> mMusicName){
		try {
			mMusicName.clear();
			mSP = context.getSharedPreferences(COLLECTIONTAG, Context.MODE_PRIVATE);
        	String jsonString = mSP.getString(COLLECTIONTAG, "");
    		JSONObject json = new JSONObject(jsonString);
    		JSONArray ja1 = json.getJSONArray(COLLECTION);
    		for(int i = 0; i < ja1.length();i++){
    			String name = ja1.getString(i);
    			String path = ja1.getString(++i);
    			File musicFile = new File(path);
    			if(musicFile .exists()){ 
    				mMusicName.add(new MusicName(name, path));
    			}
    		}
    	} catch (Exception e) {
//    		e.printStackTrace();
    	}
		return mMusicName;
    }

	/**
	 * @param mMusicName 需要保存的收藏列表
	 */
	public static void saveCollectionMusicList(Context context,ArrayList<MusicName> mMusicName){
		try {
			mSP = context.getSharedPreferences(COLLECTIONTAG, Context.MODE_PRIVATE);
   		 	JSONObject jo = new JSONObject();
			 JSONArray jsonarr = new JSONArray();
			 int index = 0;
			 for(MusicName item2 : mMusicName){
				 jsonarr.put(index, item2.mName);
				 jsonarr.put(++index, item2.mPath);
				 index++;
			 }
			 jo.put(COLLECTION, jsonarr);
			 mSP.edit().putString(COLLECTIONTAG, jo.toString()).commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	/**
	 * 查询歌曲是否在收藏列表
	 * @param mCurrentAPath 查询的歌曲路径
	 * @param mMusicName 需要查询所在的收藏列表
	 * @return
	 */
	public static boolean itBeenCollected(Context context ,String mCurrentAPath,ArrayList<MusicName> mMusicName){
		try {
			for(MusicName mMusic : mMusicName){
				if(mMusic.mPath.equals(mCurrentAPath)){
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * 添加歌曲到收藏列表
	 * @param Path 需要收藏的歌曲路径
	 * @param mMusicList 收藏列表
	 */
	public static ArrayList<MusicName> addMusicToCollectionList(MusicName mMusicName,ArrayList<MusicName> mMusicList){
		try {
			mMusicList.add(mMusicName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mMusicList;
	}
	
	/**
	 * 从收藏列表中删除
	 * @param Path 需要删除的歌曲路径
	 * @param mMusicName 收藏列表
	 */
	public static ArrayList<MusicName> removeMusicFromCollectionList(String Path,ArrayList<MusicName> mMusicName){
		try {
			int index = 0;
			for(MusicName mMusic : mMusicName){
				if(mMusic.mPath.equals(Path)){
					mMusicName.remove(index);
				}
				index ++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mMusicName;
	}
}
