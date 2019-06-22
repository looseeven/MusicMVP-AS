package com.tw.music;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.tw.john.TWUtil;
import android.util.Log;

import com.tw.music.bean.MusicName;
import com.tw.music.bean.Record;
import com.tw.music.presenter.MusicPresenter;
import com.tw.music.utils.CollectionUtils;
import com.tw.music.utils.SharedPreferencesUtils;

public class TWMusic extends TWUtil {
    private static TWMusic mTW = new TWMusic();
    private static int mCount = 0;
    public static final int REQUEST_SOURCE = 0x9e11;  //切源
    public static final int REQUEST_MEDIA = 0x0502; //切源
    public static final int REQUEST_SERVICE = 0x9e00;
    /**
     * 用于收到服务的按键处理
     */
    public static final int RETURN_MUSIC = 0x9e03;
    /**
     * 用于收到热拔插的处理
     */
    public static final int RETURN_MOUNT = 0x9e1f;

    /**
     * 用于存放随机模式的播放数组
     */
    public int[] mRPlaylist;
    public int mCurrentRIndex;
    /**
     * 播放歌曲在其所在的列表中的位置
     */
    public int mCurrentIndex;
    /**
     * 播放进度
     */
    public int mCurrentPos;
    /**
     * 两个播放模式
     */
    public int mShuffle;
    public int mRepeat = 1;

    /**
     * 播放文件所在文件夹路径
     */
    public String mCurrentAPath;
    /**
     * 播放文件路径
     */
    public String mCurrentPath;
    /**
     * 播放歌曲的歌词路径
     */
    public String mCurrentLrcViewPath;
    /**
     * ID3
     */
    public String mCurrentArtist;
    public String mCurrentAlbum;
    public String mCurrentSong;

    /**
     * 播放列表
     */
    public Record mPlaylistRecord;
    /**
     * SD列表
     */
    public Record mSDRecord;
    /**
     * USB列表
     */
    public Record mUSBRecord;
    /**
     * 本地列表
     */
    public Record mMediaRecord;
    /**
     * 临时播放目录 用于播放列表/sd/usb/inand/收藏
     */
    public Record mCList;
    /**
     * 收藏列表
     */
    public Record mLikeRecord;//用于存放收藏列表的目录

    /**
     * 用于存放所有路径目录的收藏
     */
    public ArrayList<MusicName> likeMusic = new ArrayList<MusicName>();
    /**
     * 用于存放所有SD有关音乐的目录 SD1 2 3
     */
    public ArrayList<Record> mSDRecordArrayList = new ArrayList<Record>();
    /**
     * 用于存放所有USB有关音乐的目录 USB1 2 3
     */
    public ArrayList<Record> mUSBRecordArrayList = new ArrayList<Record>();

    public static final int ACTIVITY_RUSEME = 0x03;
    public static final int ACTIVITY_PAUSE = 0x83;
    public int mSDRecordLevel = 0; //为SD的序列号1 2 3
    public int mUSBRecordLevel = 0; //为USB的序列号1 2 3

    public Bitmap mAlbumArt;
    private int mService = 0;

    public static TWMusic open() {
        if (mCount++ == 0) {
            if (mTW.open(new short[]{(short) 0x0202, (short) 0x0304, (short) 0x9e03, (short) 0x9e1f}) != 0) {
                mCount--;
                return null;
            }
            mTW.mPlaylistRecord = new Record("Playlist", 0, 0);
            mTW.mLikeRecord = new Record("LIKE", 4, 0);
            mTW.start();
            mTW.resume();
            mTW.loadFile(mTW.mPlaylistRecord, mTW.mCurrentPath);
            mTW.toRPlaylist(mTW.mCurrentIndex);
            mTW.initRecord();
            mTW.setIndex(mTW.mCurrentPath);
        }
        return mTW;
    }

    public void close() {
        if (mCount > 0) {
            if (--mCount == 0) {
                stop();
                super.close();
            }
        }
    }


    public void requestSource(int source) {
        write(REQUEST_SOURCE, (1 << 7) | (1 << 6), source);
    }

    public void requestSource(boolean is) {
        requestSource(is ? 0x03 : 0x83);
    }

    public void requestService(int activity) {
        mService = activity;
        write(REQUEST_SERVICE, activity);
    }

    public int getService() {
        return mService;
    }

    public void media(int type, int cindex, int tindex, int ctime, int percent) {
        write(REQUEST_MEDIA, (tindex << 16) | (cindex & 0xffff), (type << 31) | ((percent & 0x7f) << 24) | (ctime & 0xffffff));
    }

    private void resume() {
        try {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/data/tw/music"));
                mCurrentAPath = br.readLine();
                mCurrentIndex = Integer.valueOf(br.readLine());
                mCurrentPos = Integer.valueOf(br.readLine());
                mShuffle = Integer.valueOf(br.readLine());
                mRepeat = Integer.valueOf(br.readLine());
                mRepeat = Integer.valueOf(br.readLine());
            } catch (Exception e) {
            } finally {
                if (br != null) {
                    br.close();
                    br = null;
                }
            }
            if (mRepeat < 1) {
                mRepeat = 1;
            }
            if (mCurrentAPath != null) {
                mCurrentPath = mCurrentAPath.substring(0, mCurrentAPath.lastIndexOf("/"));
            }
        } catch (Exception e) {
        }
    }

    public void loadFile(Record record, String path) {
        if ((record != null) && (path != null)) {
            File[] file = new File(path).listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    String n = f.getName().toUpperCase(Locale.ENGLISH);
                    if (f.isFile() && !n.startsWith(".") &&
                            (n.endsWith(".MP3") ||
//									n.endsWith(".WMA") ||
                                    n.endsWith(".AAC") ||
                                    n.endsWith(".OGG") ||
                                    n.endsWith(".PCM") ||
                                    n.endsWith(".M4A") ||
//									n.endsWith(".AC3") ||
                                    n.endsWith(".EC3") ||
                                    n.endsWith(".DTSHD") ||
                                    n.endsWith(".MKA") ||
//									n.endsWith(".RA") ||
                                    n.endsWith(".WAV") ||
                                    n.endsWith(".CD") ||
                                    n.endsWith(".AMR") ||
                                    n.endsWith(".MP2") ||
                                    n.endsWith(".APE") ||
                                    n.endsWith(".DTS") ||
                                    n.endsWith(".FLAC") ||
                                    n.endsWith(".MIDI") ||
                                    n.endsWith(".MID") ||
                                    n.endsWith(".MPC") ||
                                    n.endsWith(".TTA") ||
                                    n.endsWith(".ASX") ||
                                    n.endsWith(".AIFF") ||
                                    n.endsWith(".AU"))) {
                        return true;
                    }
                    return false;
                }
            });
            record.mCLength = 0;
            if (file != null) {
                record.setLength(file.length);
                for (File f : file) {
                    String n = f.getName();
                    record.add(n.substring(0, n.lastIndexOf(".")), f.getAbsolutePath());
                }
            }
        }
    }

    public Record loadCollectFile(Context c, Record record) {
        record = mPlaylistRecord;
        record.mLName = null;
        record.mCLength = 0;
        int i = 0;
        for (MusicName f : mPlaylistRecord.mLName) {
            if (SharedPreferencesUtils.getBooleanPref(c, "music", f.mName)) {
                i++;
            }
        }
        record.setLength(i);
        for (MusicName f : mPlaylistRecord.mLName) {
            if (SharedPreferencesUtils.getBooleanPref(c, "music", f.mName)) {
                record.add(f);
            }
        }
        return record;
    }

    private int getPRandom(int index, int length) {
        int p;
        int j;
        while (((p = (int) (Math.random() * length)) == 0) && (index == 0)) ;
        for (j = p; j < length; j++) {
            if (mRPlaylist[j] == 0) {
                break;
            }
        }
        if (j == length) {
            for (j = 1; j < p; j++) {
                if (mRPlaylist[j] == 0) {
                    break;
                }
            }
        }
        return j;
    }

    public void toRPlaylist(int index) {
        mRPlaylist = null;
        mCurrentRIndex = 0;
        if (mPlaylistRecord != null) {
            int length = mPlaylistRecord.mCLength;
            if (length > 0) {
                mRPlaylist = new int[length];
                if (index >= length) {
                    index = 0;
                }
                mRPlaylist[0] = index;
                if (length > 1) {
                    for (int i = index + 1; i < length; i++) {
                        int p = i - index;
                        if (mShuffle != 0) {
                            p = getPRandom(index, length);
                        }
                        mRPlaylist[p] = i;
                    }
                    for (int i = 0; i < index; i++) {
                        int p = i + length - index;
                        if (mShuffle != 0) {
                            p = getPRandom(index, length);
                        }
                        mRPlaylist[p] = i;
                    }
                }
            }
        }
    }

    public boolean loadFileIsHas(String path) {
        if ((path != null)) {
            File[] file = new File(path).listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    String n = f.getName().toUpperCase(Locale.ENGLISH);
                    if (f.isFile()
                            && !n.startsWith(".")
                            && (n.endsWith(".MP3") || n.endsWith(".AAC")
                            || n.endsWith(".OGG")
                            || n.endsWith(".PCM")
                            || n.endsWith(".M4A")
                            || n.endsWith(".EC3")
                            || n.endsWith(".DTSHD")
                            || n.endsWith(".MKA")
                            || n.endsWith(".WAV")
                            || n.endsWith(".CD")
                            || n.endsWith(".AMR")
                            || n.endsWith(".MP2")
                            || n.endsWith(".APE")
                            || n.endsWith(".DTS")
                            || n.endsWith(".FLAC")
                            || n.endsWith(".MIDI")
                            || n.endsWith(".MID")
                            || n.endsWith(".MPC")
                            || n.endsWith(".TTA")
                            || n.endsWith(".ASX")
                            || n.endsWith(".AIFF")
                            || n.endsWith(".AU"))) {
                        return true;
                    }
                    return false;
                }
            });
            if (file != null && file.length > 0) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private void initRecord() {
        try {
            mSDRecord = new Record("SD", 1, 0);
            File[] fileSD = new File("/storage").listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    String n = f.getName();
                    if (f.canRead() && f.isDirectory() && n.startsWith("extsd")) {
                        return true;
                    }
                    return false;
                }
            });
            if (fileSD != null) {
                for (File f : fileSD) {
                    addRecordSD(f.getAbsolutePath());
                }
            }
            mUSBRecord = new Record("USB", 2, 0);
            File[] fileUSB = new File("/storage").listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    String n = f.getName();
                    if (f.canRead() && f.isDirectory() && n.startsWith("usb")) {
                        return true;
                    }
                    return false;
                }
            });
            if (fileUSB != null) {
                for (File f : fileUSB) {
                    addRecordUSB(f.getAbsolutePath());
                }
            }
            mMediaRecord = new Record("iNand", 3, 0);
            loadVolume(mMediaRecord, "/mnt/sdcard/iNand");
            mCList = mPlaylistRecord;
            if (mCList.mCLength == 0) {
                if (mSDRecordArrayList.size() > 0) {
                    mCList = mSDRecordArrayList.get(0);
                } else {
                    mCList = mSDRecord;
                }
                if (mCList.mCLength == 0) {
                    if (mUSBRecordArrayList.size() > 0) {
                        mCList = mUSBRecordArrayList.get(0);
                    } else {
                        mCList = mUSBRecord;
                    }
                    if (mCList.mCLength == 0) {
                        mCList = mMediaRecord;
                        if (mCList.mCLength == 0) {
                            mCList = mPlaylistRecord;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.i("md", "initRecord " + e.toString());
        }
    }

    public void loadVolume(Record record, String volume) {
        if ((record != null) && (volume != null)) {
            try {
                BufferedReader br = null;
                try {
                    String xpath = null;
                    if (volume.startsWith("/storage/usb") || volume.startsWith("/storage/extsd")) {
                        xpath = "/data/tw/" + volume.substring(9);
                    } else {
                        xpath = volume + "/DCIM";
                    }
                    br = new BufferedReader(new FileReader(xpath + "/.music"));
                    String path = null;
                    ArrayList<MusicName> l = new ArrayList<MusicName>();
                    while ((path = br.readLine()) != null) {
                        File f = new File(volume + "/" + path);
                        if (f.canRead() && f.isDirectory()) {
                            String n = f.getName();
                            String p = f.getAbsolutePath();
                            if (n.equals(".")) {
                                String p2 = p.substring(0, p.lastIndexOf("/"));
                                String p3 = p2.substring(p2.lastIndexOf("/") + 1);
                                if (loadFileIsHas(p)) {
                                    l.add(new MusicName(p3, p));
                                }
                            } else {
                                if (loadFileIsHas(p)) {
                                    l.add(new MusicName(n, p));
                                }
                            }
                        }
                    }
                    record.setLength(l.size());
                    for (MusicName n : l) {
                        record.add(n);
                    }
                    l.clear();
                } catch (Exception e) {
                } finally {
                    if (br != null) {
                        br.close();
                        br = null;
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public void addRecordSD(String path) {
        for (Record r : mSDRecordArrayList) {
            if (path.equals(r.mName)) {
                return;
            }
        }
        Record r = new Record(path, 1, 0);
        loadVolume(r, path);
        mSDRecordArrayList.add(r);
        if ((mCList != null) && mCList.mName.equals("SD")) {
            mCList = mSDRecordArrayList.get(0);
        }
    }

    public void addRecordUSB(String path) {
        for (Record r : mUSBRecordArrayList) {
            if (path.equals(r.mName)) {
                return;
            }
        }
        Record r = new Record(path, 2, 0);
        loadVolume(r, path);
        mUSBRecordArrayList.add(r);
        if ((mCList != null) && mCList.mName.equals("USB")) {
            mCList = mUSBRecordArrayList.get(0);
        }
    }

    public void removeRecordSD(String path) {
        for (Record r : mSDRecordArrayList) {
            if (path.equals(r.mName)) {
                Record t = mCList;
                if (mCList.mLevel == 1) {
                    t = mCList.mPrev;
                }
                String s = t.mName;
                r.clearRecord();
                mSDRecordArrayList.remove(r);
                if (mSDRecordLevel >= mSDRecordArrayList.size()) {
                    mSDRecordLevel = mSDRecordArrayList.size() - 1;
                    if (mSDRecordLevel < 0) {
                        mSDRecordLevel = 0;
                    }
                }
                if (path.equals(s)) {
                    if (mSDRecordArrayList.size() > 0) {
                        mCList = mSDRecordArrayList.get(mSDRecordLevel);
                    } else {
                        mCList = mSDRecord;
                    }
                }
                return;
            }
        }
    }

    public void removeRecordUSB(String path) {
        for (Record r : mUSBRecordArrayList) {
            if (path.equals(r.mName)) {
                Record t = mCList;
                if (mCList.mLevel == 1) {
                    t = mCList.mPrev;
                }
                String s = t.mName;
                r.clearRecord();
                mUSBRecordArrayList.remove(r);
                if (mUSBRecordLevel >= mUSBRecordArrayList.size()) {
                    mUSBRecordLevel = mUSBRecordArrayList.size() - 1;
                    if (mUSBRecordLevel < 0) {
                        mUSBRecordLevel = 0;
                    }
                }
                if (path.equals(s)) {
                    if (mUSBRecordArrayList.size() > 0) {
                        mCList = mUSBRecordArrayList.get(mUSBRecordLevel);
                    } else {
                        mCList = mUSBRecord;
                    }
                }
                return;
            }
        }
    }

    /**
     * 根据播放路径拿到临时播放目录
     */
    private void setIndex(String args) {
        if (args == null) {
            return;
        }
        if (args.contains("/mnt/sdcard/iNand")) {
            mCList = mMediaRecord;
            mCList.mIndex = 3;
        } else if (args.contains("/storage/usb")) {
            if (mUSBRecordArrayList.size() > 0) {
                if (mUSBRecordLevel >= mUSBRecordArrayList.size()) {
                    mUSBRecordLevel = 0;
                }
                mCList = mUSBRecordArrayList.get(mUSBRecordLevel);
            } else {
                mCList = mUSBRecord;
            }
        } else if (args.contains("/storage/extsd")) {
            if (mSDRecordArrayList.size() > 0) {
                if (mSDRecordLevel >= mSDRecordArrayList.size()) {
                    mSDRecordLevel = 0;
                }
                mCList = mSDRecordArrayList.get(mSDRecordLevel);
            } else {
                mCList = mSDRecord;
            }
        } else {
            mCList = mPlaylistRecord;
            mCList.mIndex = 0;
        }
        if (MusicPresenter.isCollectMusic){
            mCList.mIndex = 4;
        }
    }
}
