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

public class ROU223 extends Spider {

    private static final String siteUrl = "http://223rou.com";
    private static final String searchUrl = siteUrl + "/search.html?q=";

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
    // 1. 获取当前页码
    int currentPage = pg.isEmpty() || pg.equals("1") ? 1 : Integer.parseInt(pg);
    
    // 2. 获取总页数
    int totalPages = fetchTotalPages(tid);
    
    // 3. 构建目标URL
    String targetUrl;
    if (currentPage == 1) {
        // 第一页：直接访问 index.html
        targetUrl = siteUrl + tid + "index.html";
    } else {
        // 后续页：计算反向页码（根据你的网站逻辑）
        // 如果你的网站是正向分页：list_2.html, list_3.html...
        // 如果你的网站是反向分页：list_{totalPages-currentPage+1}.html
        
        // 方案A：正向分页（大多数网站）
        // targetUrl = siteUrl + tid + "list_" + currentPage + ".html";
        
        // 方案B：反向分页（你的代码逻辑）
        int reversePage = totalPages - currentPage + 1;
        targetUrl = siteUrl + tid + "list_" + reversePage + ".html";
    }
	Document doc = Jsoup.parse(OkHttp.string(targetUrl, getHeaders()));
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
	
private int fetchTotalPages(String tid) {
    try {
        String firstPageUrl = siteUrl + tid + "index.html";
        Document doc = Jsoup.parse(OkHttp.string(firstPageUrl, getHeaders()));
        
        // 方法1：通过分页链接获取最后一页的页码
        Elements paginationLinks = doc.select("div.pagination > span > a");
        if (!paginationLinks.isEmpty()) {
            Element lastPageLink = paginationLinks.first();
            String href = lastPageLink.attr("href");
            
            // 提取页码数字
            String pageNum = href.replace(tid, "")
                                 .replace("list_", "")
                                 .replace(".html", "")
                                 .trim();
            
            try {
                return Integer.parseInt(pageNum);
            } catch (NumberFormatException e) {
                // 如果解析失败，尝试其他方式
                System.err.println("解析页码失败: " + pageNum);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        return 1; // 发生异常时返回1页
    }
}

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(siteUrl.concat(ids.get(0)), getHeaders()));
        String name = doc.select("title").text().split("-")[0];
        String url =  Util.getVar(doc.html(), "playUrl").replace("+@movivecom@+","vmyjhl.com");
        //String pic = doc.select("div.player-poster.clickable").attr("style").split("\"")[1];
            
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
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(key), getHeaders()));
        for (Element element : doc.select("li.sui-result")) {
            try {
                String pic = element.select("img").attr("src");
                String url = element.select("div.sui-result__image > a").attr("href");
                String name = element.select("div.sui-result__image > a").attr("title").replace("<em>","").replace("</em>","");
                String id = url.replace(siteUrl,"");
                list.add(new Vod(id, name,pic));
            } catch (Exception e) {
            }
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
