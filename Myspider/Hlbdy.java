package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;
//import com.github.catvod.spider.tools.ImageDecryptor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//黑料不打烊
public class Hlbdy extends Spider {

    private static final String siteUrl = "https://across.odrepgn.cc";
    private static final String cateUrl = siteUrl + "/category/";
    private static final String detailUrl = siteUrl + "/archives/";
    private static final String searchUrl = siteUrl + "/search/";


    @Override
    public String homeContent(boolean filter)  throws Exception  {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(siteUrl+"/"));
        for (Element element : doc.select("div.category-list > ul > li")) {
            String typeId = element.select("a").attr("href").split("/")[2];
            String typeName = element.select("a").text();
            classes.add(new Class(typeId, typeName));
        }
        for (Element element : doc.select("article")) {
            String picurl = element.select("script").html();
            String name = element.select("h2.post-card-title").text();
			if ( picurl.contains(".gif") || name.isEmpty()) continue;
			//Pattern pattern = Pattern.compile("loadBannerDirect\\s*\\(\\s*['\"]([^'\"]+)['\"]");
            //Matcher matcher = pattern.matcher(picurl);
			//picurl = matcher.find() ? matcher.group(1) : "";
			//ImageDecryptor imagedecryptor = new ImageDecryptor("","","","","","","");
			//String pic = imagedecryptor.downloadAndDecryptImage(picurl);
            String url = element.attr("href");
            String id = url.split("/")[2].replace(".html","");
            list.add(new Vod(id, name, "pp"));
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception  {
        List<Vod> list = new ArrayList<>();
        String target = cateUrl  + tid + "/";
		if( !pg.equals("1")) target = cateUrl + tid + "/" + pg +"/";
        Document doc = Jsoup.parse(OkHttp.string(target));
        for (Element element : doc.select("article")) {
            String picurl = element.select("script").html();
            String name = element.select("h2.post-card-title").text();
            if (picurl.contains(".gif") || name.isEmpty()) continue;
			//Pattern pattern = Pattern.compile("loadBannerDirect\\s*\\(\\s*['\"]([^'\"]+)['\"]");
            //Matcher matcher = pattern.matcher(picurl);
			//picurl = matcher.find() ? matcher.group(1) : "";
			//ImageDecryptor imagedecryptor = new ImageDecryptor("","","","","","","");
			//String pic= imagedecryptor.downloadAndDecryptImage(picurl);
            String url = element.attr("href");	
            String id = url.split("/")[2].replace(".html","");
            list.add(new Vod(id, name, "pp"));
        }
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)).concat(".html")));
        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String year = doc.select("span.inactive-color").get(0).text();
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodYear(year.replace("上市於 ", ""));
        vod.setVodName(name);
        vod.setVodPlayFrom("Jable");
        vod.setVodPlayUrl("播放$" + Util.getVar(doc.html(), "hlsUrl"));
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) {
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)).concat("/")));
        for (Element element : doc.select("div.video-img-box")) {
            String pic = element.select("img").attr("data-src");
            String url = element.select("a").attr("href");
            String name = element.select("div.detail > h6").text();
            String id = url.split("/")[4];
            list.add(new Vod(id, name, pic));
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
