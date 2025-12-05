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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavBus extends Spider {

    private static final String siteUrl = "https://javbus.sbs";
    private static final String cateUrl = siteUrl + "/vod/type/id/";
    private static final String detailUrl = siteUrl + "/vod/detail/id/";
    private static final String searchUrl = siteUrl + "/vod/search/?wd=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"1","2","3","4","5","6","7","all"};
        String[] typeNameList = {"中字一区","中字二区","中字三区","中字四区","中字五区","传媒映画","日本AV","全部"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("div.myui-vodlist__box")) {
            String pic = element.select("a.myui-vodlist__thumb").attr("data-original");
            String url = element.select("a.myui-vodlist__thumb").attr("href");
            String name = element.select("a.myui-vodlist__thumb").attr("title");
            if (pic.endsWith(".gif") || name.isEmpty()) continue;
            String id = url.split("/")[4];
            list.add(new Vod(id, name, pic));
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
		if (tid == "all"){
			String target = siteUrl + "/vod/show/id/all/page/" + Integer.parseInt(pg)+ "/";
			Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
			//Document doc = Jsoup.parse(doca.select("ul.myui-vodlist.clearfix").first().outerHtml());
			for (Element element : doc.select("li.col-md-4.col-sm-3.col-xs-2")) {
				String pic = element.select("a.myui-vodlist__thumb").attr("data-original");
				String url = element.select("a.myui-vodlist__thumb").attr("href");
				String name = element.select("a.myui-vodlist__thumb").attr("title");
				if (pic.endsWith(".gif") || name.isEmpty()) continue;
				String id = url.split("/")[4];
				list.add(new Vod(id, name, pic));
			}			
		} else {
			String target = cateUrl + tid + "/page/" + Integer.parseInt(pg)+ "/";
			Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
			//Document doc = Jsoup.parse(doca.select("ul.myui-vodlist.clearfix").first().outerHtml());
			for (Element element : doc.select("li.col-md-4.col-sm-3.col-xs-2")) {
				String pic = element.select("a.myui-vodlist__thumb").attr("data-original");
				String url = element.select("a.myui-vodlist__thumb").attr("href");
				String name = element.select("a.myui-vodlist__thumb").attr("title");
				if (pic.endsWith(".gif") || name.isEmpty()) continue;
				String id = url.split("/")[4];
				list.add(new Vod(id, name, pic));
			}
		}
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)).concat("/"), getHeaders()));
        String name = doc.select("h1.title").text();
        String pic = doc.select("img.lazyload").attr("data-original");
		String url = doc.select("div.myui-content__operate > a").attr("href");
		doc = Jsoup.parse(OkHttp.string(siteUrl.concat(url), getHeaders()));
		//String text = String.valueOf(doc);
        //String pattern = "var\\s+uul\\s*=\\s*'([^']+\\$[^']+)'";
        //Pattern regex = Pattern.compile(pattern);
        //Matcher matcher = regex.matcher(text);
		//String urlall = "";
        //if (matcher.find()) {
         //   urlall.concat(matcher.group(1)).concat("#");
        //}
        //url = urlall.substring(0, urlall.length() - 1);
		url =  Util.getVar(doc.html(), "uul");
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("JavBus");
        vod.setVodPlayUrl(url);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)).concat("/").concat("&submit="), getHeaders()));
        for (Element element : doc.select("li.active.clearfix")) {
            String pic = element.select("div.thumb > a").attr("data-original");
            String url = element.select("div.thumb > a").attr("href");
            String name = element.select("div.thumb > a").attr("title");
            String id = url.split("/")[4];
            list.add(new Vod(id, name, pic));
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
