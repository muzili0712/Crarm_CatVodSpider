package com.github.catvod.spider;

import android.content.Context;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Eighteen extends Spider {
	
    private static Map<String, String> cookies = new HashMap<>();
    private final String url = "https://mjv002.com/zh/";
    private final String starturl = "https://mjv002.com/zh/chinese_IamOverEighteenYearsOld/19/index.html";

    /**
     * 获取请求头（包含 Cookie）
     */
    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        
        // 添加 Cookie
        if (!cookies.isEmpty()) {
            StringBuilder cookieBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                cookieBuilder.append(entry.getKey())
                           .append("=")
                           .append(entry.getValue())
                           .append("; ");
            }
            headers.put("Cookie", cookieBuilder.toString().trim());
        }
        
        return headers;
    }

    @Override
    public void init(Context context, String extend) throws Exception {
		
		List<String> setCookieHeaders = OkHttp.newCall(starturl,getHeaders()).headers("Set-Cookie");
		for (String header : setCookieHeaders) {
            try {
                // 解析 Cookie：name=value; expires=...; path=...; domain=...
                String[] parts = header.split(";");
                String[] nameValue = parts[0].split("=", 2);
                if (nameValue.length == 2) {
                    String name = nameValue[0].trim();
                    String value = nameValue[1].trim();
                    cookies.put(name, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(url,getHeaders()));
        for (Element a : doc.select("ul.animenu__nav > li > a")) {
            String typeName = a.text();
            String typeId = a.attr("href").replace(url, "");
            if (!typeId.contains("random/all/")) continue;
            if (typeName.contains("18H")) break;
            classes.add(new Class(typeId, typeName));
        }
        for (Element div : doc.select("div.post")) {
            String id = div.select("a").attr("href").replace(url, "");
            String name = div.select("h3").text();
            String pic = div.select("a > img").attr("src");
            String remark = div.select("div.meta").text();
            list.add(new Vod(id, name, pic, remark));
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        tid = tid.replace("random", "list");
        tid = tid.replace("index", pg);
        Document doc = Jsoup.parse(OkHttp.string(url + tid,getHeaders()));
        for (Element div : doc.select("div.post")) {
            String id = div.select("a").attr("href").replace(url, "");
            String name = div.select("h3").text();
            String pic = div.select("a > img").attr("src");
            String remark = div.select("div.meta").text();
            list.add(new Vod(id, name, pic, remark));
        }
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(url + ids.get(0),getHeaders()));
        Element wrap = doc.select("div.video-wrap").get(0);
        String name = wrap.select("div.archive-title > h1").text();
        String pic = wrap.select("div.player-wrap > img").attr("src");
		String url = "https" + wrap.select("div.mvspan_2 > div > iframe").attr("src");
		String urltext = Jsoup.parse(OkHttp.string(url,getHeaders())).html();
		Pattern pattern = Pattern.compile("src\\s*:\\s*'([^']+)'");
        Matcher matcher = pattern.matcher(urltext);
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("18AV");
        vod.setVodPlayUrl("播放$" + matcher.group(1));
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return searchContent(key, "1");
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        return searchContent(key, pg);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().parse().url(url + id).string();
    }

    private String searchContent(String key, String pg) {
        HashMap<String, String> params = new HashMap<>();
        params.put("search_keyword", key);
        params.put("search_type", "fc");
        params.put("op", "search");
        String res = OkHttp.post(url + "searchform_search/all/" + pg + ".html", params); 
        List<Vod> list = new ArrayList<>();
        for (Element div : Jsoup.parse(res).select("div.post")) {
            String id = div.select("a").attr("href").replace(url, "");
            String name = div.select("h3").text();
            String pic = div.select("a > img").attr("src");
            String remark = div.select("div.meta").text();
            list.add(new Vod(id, name, pic, remark));
        }
        return Result.string(list);
    }
}
