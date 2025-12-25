package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ydbj extends Spider {

    private static final String siteUrl = "http://www.ydcqq.pw";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        for (Element element : doc.select("div.box.width-full")) {
          if (element.select("script") == null){
			String pic = element.select("img").attr("src");
            String url = element.select("div.videotitle > a").attr("href");
            String name = element.select("div.videotitle > a").text();
			String year = element.select("div.videodate").text();
            list.add(new Vod(url, name, pic,year));
		  }
        }
        return list;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"/ydc4_22", "/ydc4_28", "/ydc4_157", "/ydc4_24", "/ydc4_25", "/ydc4_29", "/ydc4_26", "/ydc4_33", "/ydc4_32", "/ydc4_36", "/ydc4_37"};
        String[] typeNameList = {"日本无码", "日本有码", "中文字幕", "亚洲国产", "欧美性爱", "强暴迷奸", "三级伦理", "SM另类", "怀旧老片", "坚屏视频", "自拍短片"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl + "/ydc4_22.jsp", getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = pg.equals("1") ? siteUrl  + tid + ".jsp" : siteUrl  + tid + "_" + pg + ".jsp";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string( list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(siteUrl.concat(ids.get(0)), getHeaders()));
        String name = doc.select("div.videocontentcell.titletablegreen6 > img").attr("title");
        String pic = doc.select("div.videocontentcell.titletablegreen6 > img").attr("src");
		String url = doc.select("a[title=HTML5(MP4)播放]").attr("href");
        String html = OkHttp.string(siteUrl.concat(url), getHeaders());
		doc = Jsoup.parse(html);
		String playUrl = "";
        for (Element element : doc.select("script")) {
          if (element.html().contains("function") && element.html().contains("var src") ){
			playUrl = Util.getVar(element.html(), "src").replace( "\\","");
			break;
		  }
        }
		
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("Ydbj");
        vod.setVodPlayUrl("播放$" + playUrl);
        return Result.string(vod);
    }


    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
