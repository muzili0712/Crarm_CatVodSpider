package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.CgImageUtil;
import com.github.catvod.utils.Util;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

public class Cg51 extends Spider {
    private static final String ivString = "97b60394abc2fbe1";
    private static final String keyString = "f5d965df75336270";
	private static final String padding = CgImageUtil.CBC_PKCS_7_PADDING;
    // 数据类
    private static class ArticleData {
        final String id;
        final String name;
        final String imageUrl;
    
        ArticleData(String id, String name, String imageUrl) {
            this.id = id;
            this.name = name;
            this.imageUrl = imageUrl;
        }
    }

    private static final String siteUrl = "https://carrier.ujaumgp.cc";
    private static final String cateUrl = siteUrl + "/category/";
    private static final String detailUrl = siteUrl + "/archives/";
    private static final String searchUrl = siteUrl + "/search?keywords=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }


    private List<Vod> parseVods(Document doc) {
        List<ArticleData> articlesData = extractArticlesData(doc);
        return processImagesInParallel(articlesData);
    }
    
    // 第一步：提取数据
    private List<ArticleData> extractArticlesData(Document doc) {
        List<ArticleData> dataList = new ArrayList<>();
        
        for (Element element : doc.select("article")) {
            String url = element.select("a").attr("href");
            String name = element.select(".post-card-title").text();
            
            if (url.isEmpty() || name.isEmpty()) {
                continue;
            }
            
            String id = url.split("/")[2];
            Matcher matcher = Pattern.compile("'(https?://[^']+)")
                .matcher(String.valueOf(element.select("script")));
            
            String imageUrl = matcher.find() ? matcher.group(1) : "";
            
            dataList.add(new ArticleData(id, name, imageUrl));
        }
        
        return dataList;
    }
    
    // 第二步：并行处理图片
    private List<Vod> processImagesInParallel(List<ArticleData> dataList) {
        ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(dataList.size(), 20)
        );
        
        Map<ArticleData, CompletableFuture<String>> futures = new LinkedHashMap<>();
        
        for (ArticleData data : dataList) {
            if (data.imageUrl.isEmpty() || data.imageUrl.contains(".gif") || data.name.isEmpty() ) {
                // 没有图片或是GIF，直接完成
                futures.put(data, CompletableFuture.completedFuture(data.imageUrl));
            } else {
                // 需要解密的图片
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return CgImageUtil.loadBackgroundImage(
                            data.imageUrl, keyString, ivString, padding
                        );
                    } catch (Exception e) {
                        System.err.println("解密失败: " + data.imageUrl);
                        return data.imageUrl; // 返回原始URL
                    }
                }, executor);
                
                futures.put(data, future);
            }
        }
        
        // 收集结果
        List<Vod> result = new ArrayList<>();
        for (Map.Entry<ArticleData, CompletableFuture<String>> entry : futures.entrySet()) {
            ArticleData data = entry.getKey();
            String imageUrl;
            
            try {
                imageUrl = entry.getValue().get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                imageUrl = data.imageUrl; // 超时或出错时使用原始URL
            }
            
            result.add(new Vod(data.id, data.name, imageUrl));
        }
        
        executor.shutdown();
        return result;
    }
    
    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
		Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("ul.menu.navbar-nav")) {
			String stringId = element.select("a").attr("href");
			String typeName = element.select("a").text();
            if( stringId.contains("category") && typeName.isEmpty() && !stringId.contains("mrdg")){
				String typeId = stringId.split("/")[2];
				classes.add(new Class(typeId, typeName));
			
			}
        }

        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = cateUrl + tid + "/" + pg + "/";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)), getHeaders()));
        String playUrl = "";
        int index = 1;
        for (Element element : doc.select("div.dplayer")) {
            String play = element.attr("data-config");
            JSONObject jsonObject = new JSONObject(play);
            JSONObject video = jsonObject.getJSONObject("video");
            if (playUrl == ""){
                playUrl = "视频" + index + "$" + video.get("url");
            }else {
                playUrl = playUrl + "#视频" + index + "$" + video.get("url");
            }
            index++;
        }
        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String year = doc.select("meta[property=video:release_date]").attr("content");

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

