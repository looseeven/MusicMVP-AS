package com.tw.music.lrc;

import java.util.ArrayList;
import java.util.List;

public class LrcUtil {
    /**
     * 传入的参数为标准歌词字符串
     * @param lrcStr
     * @return
     */
    public static List<LrcBean> parseStr2List(String lrcStr) {
    	List<LrcBean> list = new ArrayList<>();
    	try {
            String lrcText = lrcStr.replaceAll("&#58;", ":")
                    .replaceAll("&#10;", "\n")
                    .replaceAll("&#46;", ".")
                    .replaceAll("&#32;", " ")
                    .replaceAll("&#45;", "-")
                    .replaceAll("&#13;", "\r")
                    .replaceAll("&#39;", "'");
            String[] split = lrcText.split("\n");
            for (int i = 0; i < split.length; i++) {
                String lrc = split[i];
                    if (lrc.contains(".")&&(lrc.startsWith("[0")||lrc.startsWith("[1")||lrc.startsWith("[2")||lrc.startsWith("[3")||lrc.startsWith("[4")||lrc.startsWith("[5")||lrc.startsWith("[6")||lrc.startsWith("[7")||lrc.startsWith("[8")||lrc.startsWith("[9"))) {
                        String min = lrc.substring(lrc.indexOf("[") + 1, lrc.indexOf("[") + 3);
                        String seconds = lrc.substring(lrc.indexOf(":") + 1, lrc.indexOf(":") + 3);
                        String mills = lrc.substring(lrc.indexOf(".") + 1, lrc.indexOf(".") + 3);
                        long startTime = Long.valueOf(min) * 60 * 1000 + Long.valueOf(seconds) * 1000 + Long.valueOf(mills) * 10;
                        String text = lrc.substring(lrc.indexOf("]") + 1);
                        if (text == null || "".equals(text)) {
                            text = "music";
                        }
                        LrcBean lrcBean = new LrcBean();
                        lrcBean.setStart(startTime);
                        lrcBean.setLrc(text);
                        list.add(lrcBean);
                        if (list.size() > 1) {
                            list.get(list.size() - 2).setEnd(startTime);
                        }
                        if (i == split.length - 1) {
                            list.get(list.size() - 1).setEnd(startTime + 100000);
                        }
                    }
				}
		} catch (Exception e) {
		}
    	 return list;
    }
}
