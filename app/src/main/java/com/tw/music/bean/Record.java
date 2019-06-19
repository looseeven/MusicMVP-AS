package com.tw.music.bean;

public class Record {
	public String mName;
	public int mIndex;
	public int mLevel;
	public MusicName[] mLName;
	public int mLength;
	public int mCLength;
	public Record mCNext;
	public Record mBNext;
	public Record mPrev;

	public Record(String name, int index, int level) {
		mName = name;
		mIndex = index;
		mLevel = level;
	}

	public Record(String name, int index, int level, Record prev) {
		mName = name;
		mIndex = index;
		mLevel = level;
		mPrev = prev;
	}

	public void clearRecord() {
		for(int i = 0; i < mCLength; i++) {
			mLName[i] = null;
		}
		mCLength = 0;
		mLName = null;
		mLength = 0;
		if(mCNext != null) {
			mCNext.clearRecord();
			mCNext = null;
		}
		if(mBNext != null) {
			mBNext.clearRecord();
			mBNext = null;
		}
	}

	public void setLength(int length) {
		if(mLength != length) {
			clearRecord();
			if (length > 0) {
    			mLName = new MusicName[length];
    			mLength = length;
			}
		}
	}

	public void setNext(Record next) {
		if(mCNext != next) {
			if((mBNext != null) && (mBNext != next)) {
				mBNext.clearRecord();
				mBNext = null;
			}
			mBNext = mCNext;
			mCNext = next;
		}
	}

	public Record getNext(int index) {
		if((mCNext != null) && (mCNext.mIndex == index)) {
			return mCNext;
		} else if((mBNext != null) && (mBNext.mIndex == index)) {
			return mBNext;
		} else {
			return null;
		}
	}

	public void add(String name, String path) {
		if(mCLength < mLength) {
			mLName[mCLength++] = new MusicName(name, path);
		}
	}

	public void add(MusicName name) {
		if(mCLength < mLength) {
			mLName[mCLength++] = name;
		}
	}

	public void copyLName(Record record) {
		setLength(record.mLength);
		mCLength = 0;
		for(MusicName n : record.mLName) {
			add(new MusicName(n));
		}
	}
}
