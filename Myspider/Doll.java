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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.MessageDigest;


public class Doll extends Spider {

    private final String url = "https://hongkongdollvideo.com/";

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        for (Element div : doc.select("div.video-item")) {
            String id = div.select("a.video-title").attr("href").replace(url, "");
            String name = div.select("a.video-title").text();
            String pic1 = div.select("div.thumb > a > img").attr("data-src");
			String pic2 = div.select("div.thumb > a > img").attr("src");
			String pic = pic1.isEmpty() ? pic2 : pic1;
            String remark = div.select("div.date").text();
            list.add(new Vod(id, name, pic, remark));
        }
        return list;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(url));
        for (Element a : doc.select("ul.menu").get(0).select("li > a")) {
            String typeName = a.text();
            String typeId = a.attr("href");
            if (typeId.contains(url)) classes.add(new Class(typeId.replace(url, ""), typeName));
        }
        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = pg.equals("1") ? url + tid : url + tid + "/" + pg + ".html";
        Document doc = Jsoup.parse(OkHttp.string(target));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String html = OkHttp.string(url + ids.get(0));
        Document doc = Jsoup.parse(html);
        String pic = doc.select("meta[property=og:image]").attr("content");
        String name = doc.select("meta[property=og:title]").attr("content");
		Pattern pattern = Pattern.compile("var\\s+__PAGE__PARAMS__\\s*=\\s*\"([^\"]+)\"");
		Matcher matcher = pattern.matcher(doc.html());
		String page_params = matcher.find()?matcher.group(1):"";
		String token =decryptPAGE_PARAMS_Totoken(page_params);
		String playurl = decryptTokenToPlayurl(token);
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("Doll");
        vod.setVodPlayUrl("播放$" + playurl);
        return Result.string(vod);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).parse().click("document.getElementById('player-wrapper').click()").string();
    }

    @Override
    public boolean manualVideoCheck() throws Exception {
        return true;
    }

    @Override
    public boolean isVideoFormat(String url) throws Exception {
        return !url.contains("afcdn.net") && url.contains(".m3u8");
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return searchContent("search/" + key);
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        return searchContent("search/" + key + "/" + pg + ".html");
    }

    private String searchContent(String query) {
        Document doc = Jsoup.parse(OkHttp.string(url + query));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }
	

    private static String decryptPAGE_PARAMS_Totoken(String encryptedData) {
        if (encryptedData == null) {
            return "";
        }
        
        // 1. 先验证总长度
        if (encryptedData.length() < 64) {
            System.err.println("错误: 加密数据长度不足64位");
            return "";
        }
        
        try {
            // 2. 提取密钥和数据
            String key = encryptedData.substring(encryptedData.length() - 32);
            String dataToDecrypt = encryptedData.substring(0, encryptedData.length() - 32);
            
            // 3. 验证数据
            if (dataToDecrypt.isEmpty()) {
                System.err.println("错误: 加密数据部分为空");
                return "";
            }
            if (key.isEmpty()) {
                System.err.println("错误: 密钥部分为空");
                return "";
            }
            
            // 4. 验证16进制格式
            if (!dataToDecrypt.matches("[0-9a-fA-F]+")) {
                System.err.println("错误: 加密数据包含非16进制字符");
                return "";
            }
            if (dataToDecrypt.length() % 2 != 0) {
                System.err.println("错误: 加密数据长度不是偶数");
                return "";
            }
            
            // 5. 验证密钥格式（如果key也应该是16进制）
            if (!key.matches("[0-9a-fA-F]+")) {
                System.err.println("警告: 密钥可能不是16进制格式");
            }
            
            // 6. 改进的XOR解密
            StringBuilder result = new StringBuilder();
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            int keyLength = keyBytes.length;
            
            for (int i = 0; i < dataToDecrypt.length(); i += 2) {
                try {
                    String hexByte = dataToDecrypt.substring(i, i + 2);
                    int charCode = Integer.parseInt(hexByte, 16);
                    
                    // 使用密钥字节进行XOR
                    byte keyByte = keyBytes[(i / 2) % keyLength];
                    int decryptedCharCode = charCode ^ keyByte;
                    
                    // 检查字符是否在可打印范围内（可选）
                    if (decryptedCharCode >= 32 && decryptedCharCode <= 126) {
                        result.append((char) decryptedCharCode);
                    } else {
                        // 对于非打印字符，可以特殊处理
                        result.append((char) decryptedCharCode);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("错误: 无效的16进制字符在位置 " + i);
                    return "";
                } catch (StringIndexOutOfBoundsException e) {
                    System.err.println("错误: 数据格式异常");
                    return "";
                }
            }
            
            String decrypted = result.toString();
            
			Pattern pattern = Pattern.compile("embedUrl\":\"[^\"]+\\?token=([^\"]+)\"");
			Matcher matcher = pattern.matcher(decrypted);
			return matcher.find()?matcher.group(1):"";
            
        } catch (Exception e) {
            // 记录详细异常信息
            System.err.println("解密过程中发生未预期异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
    
    
    private static String decryptTokenToPlayurl(String token) throws Exception {
        // 参数验证
        if (token == null) {
            throw new IllegalArgumentException("token不能为null");
        }
        
        token = token.trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("token不能为空字符串");
        }
        
        // 验证最小长度
        if (token.length() < 10) {
            throw new IllegalArgumentException("token长度不足10位: " + token.length());
        }
        
        try {
            // 1. 提取时间戳（最后10位）
            String timestamp = token.substring(token.length() - 10);
            System.out.println("时间戳: " + timestamp);
            
            // 严格验证时间戳格式
            if (!timestamp.matches("\\d{10}")) {
                throw new IllegalArgumentException("时间戳必须是10位数字: " + timestamp);
            }
            
            // 2. 提取加密数据
            String encryptedData = token.substring(0, token.length() - 10);
            
            // 验证加密数据是否为空
            if (encryptedData.isEmpty()) {
                throw new IllegalArgumentException("加密数据部分为空");
            }
            
            // 3. 生成密钥
            String key = generateKey(timestamp);
            if (key == null || key.isEmpty()) {
                throw new IllegalStateException("生成的密钥为空");
            }
            System.out.println("解密密钥: " + key);
            
            // 4. XOR解密
            String decryptedJsonString = xorDecryptHex(encryptedData, key);
            if (decryptedJsonString == null || decryptedJsonString.isEmpty()) {
                throw new IllegalStateException("解密结果为空");
            }
            
			Pattern pattern = Pattern.compile("\"stream\":\"([^\"]+)\"");
			Matcher matcher = pattern.matcher(decryptedJsonString);
			return matcher.find()?matcher.group(1):"";
			
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("token格式异常，无法解析", e);
        } catch (IllegalArgumentException e) {
            // 重新抛出验证相关的异常
            throw e;
        } catch (Exception e) {
            // 包装其他异常
            throw new Exception("解密token时发生错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成解密密钥
     * 算法: md5(timestamp).substr(8, 16).reverse()
     */
    private static String generateKey(String timestamp) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(timestamp.getBytes(StandardCharsets.UTF_8));
        
        // 转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        // 取第8-24位（索引8到23，共16个字符）
        String partialKey = hexString.substring(8, 24);
        
        // 反转字符串
        return new StringBuilder(partialKey).reverse().toString();
    }
    
    /**
     * XOR解密十六进制字符串
     */
    private static String xorDecryptHex(String hexString, String key) {
        StringBuilder result = new StringBuilder();
        int keyLength = key.length();
        
        for (int i = 0; i < hexString.length(); i += 2) {
            // 获取两个字符的十六进制值
            String hexByte = hexString.substring(i, Math.min(i + 2, hexString.length()));
            int charCode = Integer.parseInt(hexByte, 16);
            
            // 计算密钥索引
            int keyIndex = (i / 2) % keyLength;
            int keyChar = key.charAt(keyIndex);
            
            // XOR解密
            int decryptedChar = charCode ^ keyChar;
            
            result.append((char) decryptedChar);
        }
        
        return result.toString();
    }
}
