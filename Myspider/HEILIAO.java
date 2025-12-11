package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.CgImageUtil;
import com.github.catvod.utils.Util;

import org.json.JSONObject;
import org.json.JSONArray;
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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HEILIAO extends Spider {
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

    private static final String siteUrl = "https://heiliao.com";
    private static final String detailUrl = siteUrl + "/archives/";
    private static final String searchUrl = siteUrl + "/index/search_article";

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
        
        for (Element element : doc.select("div.video-item ")) {
            String url = element.select("a").attr("href");
            String name = element.select("h3.title").text();
            
            if (url.isEmpty() || name.isEmpty()) {
                continue;
            }
            
            String id = url.split("/")[2];
            Matcher matcher = Pattern.compile("loadImg\\(this\\s*,\\s*[\"']([^\"']+?)[\"']\\)")
                .matcher(String.valueOf(element.select("div.placeholder-img")));
            
            String imageUrl = matcher.find() ? matcher.group(1) : "";
            if (imageUrl.contains(".gif")) {
                continue;
            }
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
		Document doc = Jsoup.parse(OkHttp.string(siteUrl+"/", getHeaders()));
        for (Element element : doc.select("a.slider-item ")) {
            String typeId = element.attr("href");
			if( typeId.equals("/")) continue;
            String typeName = element.select("a").text();
            classes.add(new Class(typeId, typeName));
        }
        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = siteUrl +  tid ; 
        if( !pg.equals("1")) target = siteUrl +  tid + "page/" + pg +"/";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception { 
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)).concat("/"), getHeaders()));
        String playUrl = "";
        int index = 1;
        for (Element element : doc.select("div.dplayer")) {
            String play = element.attr("config");
            JSONObject jsonObject = new JSONObject(play);
            JSONObject video = jsonObject.getJSONObject("video");
            if (playUrl == ""){
                playUrl = "视频" + index + "$" + video.get("url");
            }else {
                playUrl = playUrl + "#视频" + index + "$" + video.get("url");
            }
            index++;
        }
        String name = doc.select("meta[name=description]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String year = doc.select("meta[property=article:published_time]").attr("content");

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodYear(year);
        vod.setVodName(name);
        vod.setVodPlayFrom("HEILIAO");
        vod.setVodPlayUrl(playUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick,String pg) throws Exception {
        String searchstring = postString(searchUrl,key,pg);
        return searchVods(searchstring);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
	
    private static String postString(String url, String key, String pg) throws Exception {
        String postData = "word=" + URLEncoder.encode(key, "UTF-8") + "&page=" + pg;
        
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        
        try {
            // ==================== 1. 超时设置 ====================
            conn.setConnectTimeout(15000);    // 连接超时 15秒
            conn.setReadTimeout(30000);       // 读取超时 30秒
            
            // ==================== 2. 请求方法 ====================
            conn.setRequestMethod("POST");
            conn.setInstanceFollowRedirects(true); // 自动处理重定向
            
            // ==================== 3. 请求头设置 ====================
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(postData.getBytes("UTF-8").length));
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Connection", "keep-alive");
            
            // ==================== 4. 发送请求体 ====================
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }
            
            // ==================== 5. 处理响应 ====================
            int responseCode = conn.getResponseCode();
            String responseBody;
            
            if (responseCode >= 200 && responseCode < 300) {
                // 成功响应
                responseBody = readResponse(conn.getInputStream());
            } else if (responseCode >= 300 && responseCode < 400) {
                // 重定向（通常自动处理，这里记录日志）
                String redirectUrl = conn.getHeaderField("Location");
                throw new IOException("请求被重定向到: " + redirectUrl + " (状态码: " + responseCode + ")");
            } else {
                // 错误响应 - 读取错误流获取更多信息
                String errorResponse = readResponse(conn.getErrorStream());
                throw new IOException("HTTP " + responseCode + " - " + errorResponse);
            }
            
            return responseBody;
            
        } catch (Exception e) {
            // 记录日志（实际项目中应该用日志框架）
            System.err.println("POST请求失败: " + url + " - " + e.getMessage());
            throw e; // 重新抛出异常
            
        } finally {
            // ==================== 6. 清理资源 ====================
            // 注意：不要在这里关闭流，readResponse应该负责关闭
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * 读取响应流并确保关闭
     */
    private static String readResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            StringBuilder response = new StringBuilder();
            char[] buffer = new char[8192];
            int charsRead;
            
            while ((charsRead = reader.read(buffer)) != -1) {
                response.append(buffer, 0, charsRead);
            }
            
            return response.toString();
        }
    }

    private static String searchVods(String data){
    	List<Vod> list = new ArrayList<>();
		try {
    		JSONObject searchObject = new JSONObject(data);
        	JSONArray searchResult = new JSONArray();
    		if(searchObject.getJSONObject("msg").equals("ok")) {
    			searchResult = searchObject.getJSONObject("data").getJSONArray("list");
				for (int i = 0; i < searchResult.length(); i++) {
        	    	JSONObject item = searchResult.getJSONObject(i);
        	    	String name = "" + item.get("title");
    				String pic = "" + item.get("thumb");
    				String id = "" + item.get("id");
    				list.add(new Vod(id, name, pic));
        		}
    		}
    		return Result.string(list);
		} catch (JSONException e) {
			return Result.string(list);
		}
    }
}
