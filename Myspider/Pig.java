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

import java.net.URL;
import java.net.MalformedURLException;

public class Pig extends Spider {

    private static final String siteUrl = "https://pigav.com/";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        for (Element element : doc.select("div.media")) {
            String pic = element.select("span").attr("data-bgsrc");
            String url = element.select("a").attr("href");
            String name = element.select("a").attr("title");
            String id = url.replace(siteUrl, "");
            list.add(new Vod(id, name, pic));
        }
        return list;
    }
    
    @Override
    public String homeContent(boolean filter) throws Exception {
        
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
		classes.add(new Class("最新av線上看", "最新11"));
        for (Element element1 : doc.select("ul.menu > li")) {
			String typeId = "";
			String typeName = "";
            String classUrl = element1.selectFirst("a").attr("href").replace(siteUrl, "");
            String className = element1.selectFirst("a").text();
			if (classUrl.contains("dbro.news")) break;
	        for (Element element : element1.select("ul.sub-menu > li > a")) {
				typeId = element.attr("href").replace(siteUrl, "");
				typeName = "【" + className + "】" + element.text();
				classes.add(new Class(typeId, typeName));
			}
            if(typeId.isEmpty()) classes.add(new Class(classUrl, className));
        }
        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = siteUrl.concat(tid).concat("/page/").concat(pg);
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String html = OkHttp.string(siteUrl.concat(ids.get(0)), getHeaders());
		Document doc = Jsoup.parse(html);
        String url = doc.select("source").attr("src");
        String name = doc.select("h1.is-title").text();
        String pic = doc.select("video.video-js").attr("poster");
		String fullurl = "";
		String uuid = "";
		String host ="";
		String config = "";
		if (url.isEmpty()){
			fullurl = doc.select("div.post-content iframe").attr("src");
			uuid = fullurl.split("/")[15].split("?")[0];
			//try {
			//    URL urlurl = new URL(fullurl);
			//    host = urlurl.getProtocol() + "://" + urlurl.getHost();
			//} catch (MalformedURLException e) {
			//    System.err.println("URL格式错误: " + e.getMessage());
			//}
			//config = OkHttp.string(host.concat("/api/v1/videos/").concat(uuid), getHeaders());
			//String regex = "\"playlistUrl\"\\s*:\\s*\"([^\"]+)\"";
			//Pattern pattern = Pattern.compile(regex);
			//Matcher matcher = pattern.matcher(config);
			//url = matcher.find()?matcher.group(1):"";
		}
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("朱古力");
        vod.setVodPlayUrl("播放$"+fullurl);
		vod.setVodContent("html:"  + html.replace("<","[").replace("</","").replace(">","]"));
        return Result.string(vod);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
