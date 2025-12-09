package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;
import com.github.catvod.spider.tools.ImageDecryptor;

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

public class Hlbdy extends Spider {

    private static final String siteUrl = "https://across.odrepgn.cc";
    private static final String cateUrl = siteUrl + "/categories/";
    private static final String detailUrl = siteUrl + "/archives/";
    private static final String searchUrl = siteUrl + "/search/";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    @Override
    public String homeContent(boolean filter) {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(cateUrl, getHeaders()));
        for (Element element : doc.select("li.category-level-0.category-parent")) {
            String typeId = element.select("a").attr("href").split("/")[2];
            String typeName = element.select("a").text();
            classes.add(new Class(typeId, typeName));
        }
        doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("article > a")) {
            String picurl = element.select("script");
			if (picurl.indexOf(".gif") || name.isEmpty()) continue;
			Pattern pattern = Pattern.compile("loadBannerDirect\\s*\\(\\s*['\"]([^'\"]+)['\"]");
            Matcher matcher = pattern.matcher(picurl);
			picurl = matcher.find() ? matcher.group(1) : "";
			ImageDecryptor imagedecryptor = new ImageDecryptor(,,,,,,);
			String pic = imagedecryptor.downloadAndDecryptImage(picurl);
            String url = element.attr("href");
            String name = element.select("h2.post-card-title").text();
            String id = url.split("/")[2].replace(".html","");
            list.add(new Vod(id, name, pic));
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        List<Vod> list = new ArrayList<>();
        String target = cateUrl + tid + "/";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("article > a")) {
            String picurl = element.select("script");
            if (picurl.indexOf(".gif") || name.isEmpty()) continue;
			Pattern pattern = Pattern.compile("loadBannerDirect\\s*\\(\\s*['\"]([^'\"]+)['\"]");
            Matcher matcher = pattern.matcher(picurl);
			picurl = matcher.find() ? matcher.group(1) : "";
			ImageDecryptor imagedecryptor = new ImageDecryptor(,,,,,,);
			String pic = imagedecryptor.downloadAndDecryptImage(picurl);
            String url = element.attr("href");	
            String name = element.select("h2.post-card-title").text();
            String id = url.split("/")[2].replace(".html","");
            list.add(new Vod(id, name, pic));
        }
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)).concat("/"), getHeaders()));
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
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)).concat("/"), getHeaders()));
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
