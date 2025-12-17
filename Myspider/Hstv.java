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

public class Hstv extends Spider {

    private static final String siteUrl = "https://hsex.icu/";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        for (Element element : doc.select("div.thumbnail")) {
            String pic = element.select("div.image").attr("style").split("'")[1];
            String url = element.select("a[target=_self]").attr("href");
            String name = element.select("div.image").attr("title");
            String id = url.replace("video-", "").replace(".htm", "");
            list.add(new Vod(id, name, pic));
        }
        return list;
    }
    
    @Override
    public String homeContent(boolean filter) throws Exception {
        
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("ul.nav.navbar-nav a")) {
			      String typeId = element.attr("href").split("-")[0];
			      String typeName = element.text();
			      classes.add(new Class(typeId, typeName));
        }
        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = siteUrl.concat(tid).concat("-").concat(pg).concat(".htm");
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String html = OkHttp.string(siteUrl.concat("video-").concat(ids.get(0)).concat(".htm"), getHeaders());
		Document doc = Jsoup.parse(html);
        String url = "" ;
        String name = doc.select("h3.panel-title").text();
        String pic = doc.select("video").attr("poster");
		int i=1;
		for (Element element : doc.select("source[id=video-source]")) {
			url = url.isEmpty()? "视频" + i + "$"+element.attr("src") : url + "#视频" + i + "$"+element.attr("src");
			i++;
		}
		
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("Hstv");
        vod.setVodPlayUrl(url);
		    return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String target = siteUrl + "search.htm?search=" + URLEncoder.encode(key);
		return searchContent(target);
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        String target = siteUrl + "search-" + pg + ".htm?search=" + URLEncoder.encode(key);
		return searchContent(target);
    }
	
    private String searchContent(String target) {
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
