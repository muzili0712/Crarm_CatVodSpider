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
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ROU223 extends Spider {

    private static final String siteUrl = "http://223rou.com";
    private static final String searchUrl = "https://ser.m3u8111222333.com/.netlify/functions/search";
    private int totalpage = 1;

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        Integer count = 0;
        for (Element element : doc.select("div.menu.clearfix dl dd")) {
            if ( count >= 0 && count <= 15){
                String href = element.select("a").attr("href").replace("index.html","");
                String text = element.text();
                classes.add(new Class(href, text));
            }
            count ++;
        }
        doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("ul.row.col5.clearfix > li")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.select("a").attr("href");
                String name = element.select("a").attr("title");
                String id = url;
                list.add(new Vod(id, name,siteUrl + pic));
            } catch (Exception e) {
            }
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = "";
        Document doc = null;
        if (pg.isEmpty() || pg.equals("1")) {
            target = siteUrl + tid + "index.html";
            doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
            String nextpage = doc.select("div.pagination > span > a").first().attr("href").replace(tid,"").replace("list_","").replace(".html","");
            totalpage = 1 + Integer.parseInt(nextpage);
        } else {
            String nextpg = String.valueOf( totalpage + 1 - Integer.parseInt(pg) );
            target = siteUrl + tid + "list_" + nextpg + ".html";
            doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        }
        for (Element element : doc.select("ul.row.col5.clearfix li")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.select("a").attr("href");
                String name = element.select("a").attr("title");
                String id = url;
                list.add(new Vod(id, name, siteUrl + pic));
            } catch (Exception e) {
            }
        }

        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(siteUrl.concat(ids.get(0)), getHeaders()));
        String name = doc.select("title").text().split("-")[0];
        String url =  Util.getVar(doc.html(), "playUrl").replace("+@movivecom@+","vmyjhl.com");
            
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodName(name);
        vod.setVodPic(siteUrl);
        vod.setVodPlayFrom("223ROU");
        vod.setVodPlayUrl("播放$" + url);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String poststring = "{\"highlight\":{\"fragment_size\":200,\"number_of_fragments\":1,\"fields\":{\"data_name\":{},\"data_intro\":{}}},\"_source\":[\"id\",\"data_name\",\"data_intro\",\"data_actor\",\"class_dir\",\"data_picbig\",\"year\",\"month\",\"day\"],\"aggs\":{\"class_name\":"{\"terms\":{\"field\":\"class_name\",\"size\":30}}},\"query\":{\"bool\":{\"must\":[{\"multi_match\":{\"query\":"+key+ ,\"fields\":[\"data_name\",\"data_intro\",\"data_actor\"]}}]}},\"sort\":[],\"size\":20}";
        JSONObject postjson = new JSONObject(poststring);
        String result = OkHttp.post(searchUrl, postjson);
		List<Vod> list = searchVods(result);
        return Result.string(list);
    }
	
	private static List<ArticleData> searchVods(String data){
    	List<ArticleData> list = new ArrayList<>();
		try {
    		JSONObject resultObject = new JSONObject(data);
        	JSONArray resultarray = new JSONArray();
    		resultarray = resultObject.getJSONObject("hits").getJSONArray("hits");
			for (int i = 0; i < resultarray.length(); i++) {
        	   	JSONObject item = resultarray.getJSONObject(i);
				String id = siteUrl + "/htm/" + item.getJSONObject("_source").get("year") + "/" + item.getJSONObject("_source").get("month") + "/" + item.getJSONObject("_source").get("day") + item.get("_id") +".html";
        	   	String name = "" + item.getJSONObject("_source").get("data_name");
    			String pic = siteUrl + item.getJSONObject("_source").get("data_picbig");
    			list.add(new ArticleData(id, name, pic));
        	}
    		return list;
		} catch (Exception  e) {
			return list;
		}
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
