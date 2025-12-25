package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.net.URLEncoder;
import java.net.URL;
import java.net.MalformedURLException;

public class Javmenu extends Spider {

    private static final String siteUrl = "https://mrzyx.xyz/zh";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        for (Element element : doc.select("div.card.my-1")) {
          if (!element.html().contains("onclick")){
			  String pic = element.select("img").attr("data-src");
              String url = element.select("div.card-body > a").attr("href");
              String name = element.select("div.card-body > a > h5").text() + element.select("div.card-body > a > p").text();
			  String year = element.select("div.card-body > a > span").text();
              list.add(new Vod(url, name, pic,year));
		  }
        }
        return list;
    }
    
    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"censored", "uncensored", "western", "fc2", "chinese"};
        String[] typeNameList = {"有码", "无码", "欧美", "FC2", "国产"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }
  
    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = pg.equals("1")? siteUrl.concat("/").concat(tid).concat("/online") : siteUrl.concat("/").concat(tid).concat("/online?page=").concat(pg);
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String html = OkHttp.string(ids.get(0), getHeaders());
		Document doc = Jsoup.parse(html);
        String url = "" ;
        String name = doc.select("h1.display-5 strong").text();
        String pic = doc.select("video.player0").attr("data-poster");
		int i=1;
		for (Element element : doc.select("script")) {
            if(element.html().contains("m3u8.push")){
			    url = url.isEmpty()? "线路" + i + "$"+element.html().replace("m3u8.push(\"","").replace("\");","") : url + "#线路" + i + "$"+element.html().replace("m3u8.push(\"","").replace("\");","");
			    i++;
            }
		}
		
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("Javmenu");
        vod.setVodPlayUrl(url);
		return Result.string(vod);
    }



    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
