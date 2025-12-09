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
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Jrkl extends Spider {

    private static final String siteUrl = "https://www.kanliao15.cyou";
    private static final String cateUrl = siteUrl + "/category/";
    private static final String detailUrl = siteUrl + "/archives/";
    private static final String searchUrl = siteUrl + "/search/";
	

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }


    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        for (Element element : doc.select("article")) {
            String pic = element.html();
            String url = element.select("a").attr("href");
            String name = element.select("h2.post-card-title").text();
            if (pic.contains(".gif") || name.contains("WIFI")) continue;
			
			Pattern pattern = Pattern.compile("loadBannerDirect\\s*\\(\\s*['\"]([^'\"]+)['\"]");
			Matcher matcher = pattern.matcher(pic);
			String picurl = matcher.find()?matcher.group(1):"";

            String id = url.split("/")[4];
            list.add(new Vod(id, name, siteUrl + picurl));
        }
        return list;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
		Document doc = Jsoup.parse(OkHttp.string(siteUrl+"/", getHeaders()));
        for (Element element : doc.select("div.category-list > ul > li")) {
            String typeId = element.select("a").attr("href").split("/")[4];
            String typeName = element.select("a").text();
            classes.add(new Class(typeId, typeName));
        }
        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = cateUrl  + tid + "/";
		if( !pg.equals("1")) target = cateUrl + tid + "/" + pg +"/";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);

        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)).concat("/"), getHeaders()));
        String name = doc.select("article.post > h1").text();
        String pic = siteUrl + doc.select("meta[itemprop=image]").attr("content");
        String html = doc.html();
        Pattern pattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher =  pattern.matcher(html);

        String playUrl = "";
        if (matcher.find()) {
            playUrl = matcher.group(1).replace("\\/", "/");
        }

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("Jrkl");
        vod.setVodPlayUrl("播放$" + playUrl);
		vod.setVodContent("ids.get(0):"+ ids.get(0)+ "--detailUrl:" + detailUrl.concat(ids.get(0)).concat("/") + "--playUrl:"+ playUrl+ "--html:" + html);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)), getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
