
package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cg51 extends Spider {

    private static final String siteUrl = "https://carrier.ujaumgp.cc";
    private static final String cateUrl = siteUrl + "/category/";
    private static final String detailUrl = siteUrl + "/archives/";
    private static final String searchUrl = siteUrl + "/search?keywords=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }


    private String Base64ToImage(String ) {
        List<Vod> list = new ArrayList<>();

        return list;
    }
    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"wpcz","mrdg","rdsj","bkdg","whhl","xsxy","whmx"};
        String[] typeNameList = {"今日吃瓜","每日大瓜","热门吃瓜","必看大瓜","网红黑料","学生学校","明星黑料"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        List<Vod> list;
        for (Element element : doc.select("article")) {
            if( element.select("a").attr("onclick").isEmpty() ){
				String pic = element.select("div.blog-background").attr("style").replace("background-image: url(\"","").replace("\");","");
				String url = element.select("a").attr("href");
				String name = element.select("h2.post-card-title").text();
				String id = url.split("/")[2];
				list.add(new Vod(id, name, pic));
			}
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = cateUrl + tid + "/" + pg + "/"; 
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list;
        for (Element element : doc.select("article")) {
            if( element.select("a").attr("onclick").isEmpty() ){
				String pic = element.select("div.blog-background").attr("style").replace("background-image: url(\"","").replace("\");","");
				String url = element.select("a").attr("href");
				String name = element.select("h2.post-card-title").text();
				String id = url.split("/")[2];
				list.add(new Vod(id, name, pic));
			}
        }
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)).concat("/"), getHeaders()));
        String playUrl = "";
        int index = 1;
        for (Element element : doc.select("div.dplayer")) {
            String play = element.attr("data-config");
            JSONObject jsonObject = new JSONObject(play);
            JSONObject video = jsonObject.getJSONObject("video");
            if (playUrl == ""){
                playUrl = "第" + index + "集$" + video.get("url");
            }else {
                playUrl = playUrl + "#第" + index + "集$" + video.get("url");
            }
            index++;
        }
        String name = doc.select("meta[itemprop=headline]").attr("content");
        String pic = doc.select("meta[itemprop=image]").attr("content");
        String year = doc.select("meta[itemprop=datePublished]").attr("content");

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodYear(year);
        vod.setVodName(name);
        vod.setVodPlayFrom("Cg51");
        vod.setVodPlayUrl(playUrl);
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
