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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;



public class Eighteen extends Spider {
	
    private static Map<String, String> cookies = new HashMap<>();
    private final String url = "https://mjv002.com/zh/";
    private final String starturl = "https://mjv002.com/zh/chinese_IamOverEighteenYearsOld/19/index.html";
	private String tmvarr ="";
	private String argdeqweqweqwe ="";
	private String tkeyString ="";
	private String tivString = "";
	private String txorcode = "";
	private String tsplitcode = "";
	private String tencryptedString = "";
	private String turlpre = "";
	private String tstage1="";
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
        String html = OkHttp.string(url + ids.get(0),getHeaders());
		Document doc = Jsoup.parse(html);
        Element wrap = doc.select("div.video-wrap").get(0);
        String name = wrap.select("div.archive-title > h1").text();
        String pic = wrap.select("div.player-wrap > img").attr("src");
		String frameurl = decryptFrameUrl(html);
		String urltext = "";//OkHttp.string(frameurl,getHeaders()));
		Pattern pattern = Pattern.compile("src\\s*:\\s*'([^']+)'");
        Matcher matcher = pattern.matcher(urltext);
		String url =  matcher.find()? matcher.group(1):"";
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("18AV");
        vod.setVodPlayUrl("播放$" + "--------------------tkeyString:" +tkeyString + "--------------------tivString:" +tivString + "--------------------txorcode:" +txorcode + "--------------------tsplitcode:" +tsplitcode + "--------------------tencryptedString:" +tencryptedString  + "--------------------turlpre:" +turlpre + "--------------------tstage1:" +tstage1);
		vod.setVodContent("frameurl:"+ frameurl + "--------------------urltext:" +urltext  + "--------------------tkeyString:" +tkeyString + "--------------------tivString:" +tivString + "--------------------txorcode:" +txorcode + "--------------------tsplitcode:" +tsplitcode + "--------------------tencryptedString:" +tencryptedString  + "--------------------turlpre:" +turlpre + "--------------------tstage1:" +tstage1);
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

	private String decryptFrameUrl(String html){
		String keyString ="";
		String ivString = "";
		String xorcode = "";
		String splitcode = "";
		String encryptedString = "";
		String urlpre = "";
		Document doc = Jsoup.parse(html);
		for(Element element : doc.select("script")){
			if(element.html().contains("argdeqweqweqwe")){
				argdeqweqweqwe = element.html();
				String regex = "hadeedg252\\s*=\\s*(\\d+)";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(element.html());
				xorcode = matcher.find()? matcher.group(1).trim():"";
				regex = "hcdeedg252\\s*=\\s*(\\d+)";
				pattern = Pattern.compile(regex);
				matcher = pattern.matcher(element.html());
				splitcode = matcher.find()? matcher.group(1).trim():"";
				regex = "argdeqweqweqwe\\s*=\\s*'([^']+)'";
				pattern = Pattern.compile(regex);
				matcher = pattern.matcher(element.html());
				keyString = matcher.find()? matcher.group(1).trim():"";
				regex = "hdddedg252\\s*=\\s*'([^']+)'";
				pattern = Pattern.compile(regex);
				matcher = pattern.matcher(element.html());
				ivString = matcher.find()? matcher.group(1).trim():"";
			}
			if(element.html().contains("mvarr[\'10_1\']")){
				tmvarr = element.html();
				String regex = "mvarr\\['10_1'\\]=\\[\\[(.*?)\\]\\s*,\\s*\\]";
				Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
				Matcher matcher = pattern.matcher(element.html());
				String mvarr = matcher.find()? matcher.group(1).trim():"";
				encryptedString = mvarr.split(",")[1].replace("\'","");
				urlpre = "https:" + mvarr.split(",")[3].replace("\'","");
			}
			if(!splitcode.isEmpty() && !encryptedString.isEmpty()) break;
		}
		tkeyString =keyString;
		tivString = ivString;
		txorcode = xorcode;
		tsplitcode = splitcode;
		tencryptedString = encryptedString;
		turlpre = urlpre;
		String stage1 = stage1Decrypt(encryptedString ,splitcode,xorcode);
		tstage1 = stage1;
		String urlend = aesDecrypt(stage1, keyString, ivString);
		return urlpre+urlend;
    }
	
	    /**
     * 第一阶段解密：自定义解密算法
     * 1. 根据splitcode生成分隔符
     * 2. 使用分隔符拆分字符串
     * 3. 将每个部分按base-splitcode解析为整数
     * 4. 与xorcode进行XOR运算
     * 5. 转换为字符
     */
    private String stage1Decrypt(String cipherText ,String splitintstring,String xorcodestring) {

        // 计算分隔符
		int splitcode = Integer.parseInt(splitintstring);
		int xorcode = Integer.parseInt(xorcodestring);
        splitcode = (splitcode <= 25) ? splitcode : splitcode % 25;
        
        char separator = (char) (splitcode + 97);
        
        // 分割字符串
        String[] parts = cipherText.split(String.valueOf(separator));
        
        List<Character> resultChars = new ArrayList<>();
        
        // 处理每个部分
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part == null || part.isEmpty()) {
                continue;
            }
            
            try {
                // 解析base-splitcode数字
                int num = Integer.parseInt(part, splitcode);
                
                // XOR操作
                int xorResult = num ^ xorcode;
                
                // 转换为字符
                char ch = (char) xorResult;
                
                resultChars.add(ch);
            } catch (NumberFormatException e) {
                System.out.println("警告: 无法解析部分 " + part + "，跳过");
            }
        }
        
        // 构建结果字符串
        StringBuilder result = new StringBuilder();
        for (char ch : resultChars) {
            result.append(ch);
        }
        String stage1Result = result.toString();
        return stage1Result;
    }

	
    /**
     * AES解密函数
     * 使用AES/CBC/PKCS5Padding模式
     */
    private String aesDecrypt(String encryptedBase64, String key, String iv) {
        
        try {
            // 检查输入
            if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
                throw new IllegalArgumentException("加密文本不能为空");
            }
            
            // 将Base64字符串转换为字节数组
            //byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
            byte[] encryptedBytes = encryptedBase64.getBytes("UTF-8");
            // 确保密钥长度为16、24或32字节（128、192或256位）
            byte[] keyBytes = key.getBytes("UTF-8");
            byte[] ivBytes = iv.getBytes("UTF-8");
            
            // 如果密钥长度不足，进行填充
            if (keyBytes.length < 16) {
                byte[] newKeyBytes = new byte[16];
                System.arraycopy(keyBytes, 0, newKeyBytes, 0, keyBytes.length);
                keyBytes = newKeyBytes;
            } else if (keyBytes.length > 16 && keyBytes.length < 24) {
                byte[] newKeyBytes = new byte[24];
                System.arraycopy(keyBytes, 0, newKeyBytes, 0, keyBytes.length);
                keyBytes = newKeyBytes;
            } else if (keyBytes.length > 24 && keyBytes.length < 32) {
                byte[] newKeyBytes = new byte[32];
                System.arraycopy(keyBytes, 0, newKeyBytes, 0, keyBytes.length);
                keyBytes = newKeyBytes;
            }
            
            // 创建密钥和IV
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            
            // 创建解密器
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            // 执行解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedText = new String(decryptedBytes, "UTF-8");
            
            return decryptedText;
            
        } catch (Exception e) {
            System.err.println("AES解密失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
