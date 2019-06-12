package com.tw.music.bean;

public class MusicName {
	public String mName;
	public String mPath;

	public MusicName(String name, String path) {
		mName = name;
		mPath = path;
	}

	public MusicName(MusicName name) {
		mName = name.mName;
		mPath = name.mPath;
	}
}
