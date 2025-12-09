package com.github.catvod.spider.tools;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import com.github.catvod.net.OkHttp;

/**
 * 图片解密工具类 - 模拟前端解密逻辑
 */
public class ImageDecryptor {
    
    // 加密默认配置
    private static final Map<String, String> CRYPTO_DATA = new HashMap<String, String>() {{
        put("mode", "CBC");
        put("padding", "PKCS5Padding");
        put("media_key", "f5d965df75336270");
        put("media_iv", "97b60394abc2fbe1");
        put("key", "2acf7e91e9864673");
        put("iv", "1c29882d3ddfcfd6");
        put("sign_key", "5589d41f92a597d016b037ac37db243d");
    }};
	
	// 加密自定义配置
	public ImageDecryptor(String mode,String padding,String media_key,String media_iv,String key,String iv,String sign_key){
        if ( mode != null ) CRYPTO_DATA.put("mode",mode) ;
        if ( padding != null ) CRYPTO_DATA.put("padding", padding) ;
        if ( media_key != null  ) CRYPTO_DATA.put("media_key", media_key) ;
        if ( media_iv != null  ) CRYPTO_DATA.put("media_iv", media_iv) ;
        if ( key != null  ) CRYPTO_DATA.put("key", key) ;
        if ( iv != null  ) CRYPTO_DATA.put("iv", iv ) ;
        if ( sign_key != null  ) CRYPTO_DATA.put("sign_key", sign_key) ;
	}
    
    /**
     * 主要解密方法
     * @param encryptedImageData 加密的图片数据（Base64字符串）
     * @return 解密后的图片Base64编码
     * @throws Exception 解密异常
     */
    public static String decryptImageToBase64(String encryptedImageData) throws Exception {

        
        // 1. 解密图片
        String decryptedBase64 = decryptImage(encryptedImageData);
        
        // 2. 验证解密结果
        if (decryptedBase64 == null || decryptedBase64.isEmpty()) {
            throw new Exception("解密失败，返回空结果");
        }
        
        // 3. 检测图片类型并返回完整的Data URL
        String mimeType = detectImageType(decryptedBase64);
        
        return decryptedBase64;
    }
    
    /**
     * 图片解密核心方法 - 对应前端的 DecryptImage 函数
     * @param encryptedData 加密的Base64数据
     * @return 解密后的Base64数据
     * @throws Exception 解密异常
     */
    public static String decryptImage(String encryptedData) throws Exception {
        try {
            // 1. 准备密钥和IV
            byte[] keyBytes = hexStringToByteArray(CRYPTO_DATA.get("media_key"));
            byte[] ivBytes = hexStringToByteArray(CRYPTO_DATA.get("media_iv"));
            
            // 2. 创建AES解密器
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            
            Cipher cipher = Cipher.getInstance("AES/" + CRYPTO_DATA.get("mode")+"/"+ CRYPTO_DATA.get("padding") );
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            // 3. 解密数据
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            // 4. 转换为Base64字符串
            return Base64.getEncoder().encodeToString(decryptedBytes);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("图片解密失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检测图片类型 - 根据Base64数据的前缀判断
     * @param base64Data Base64编码的图片数据
     * @return 图片MIME类型
     */
    public static String detectImageType(String base64Data) {
        if (base64Data == null || base64Data.length() < 10) {
            return "image/jpeg";
        }
        
        // 取前30个字符进行判断
        String prefix = base64Data.substring(0, Math.min(30, base64Data.length()));
        
        if (prefix.startsWith("iVBORw0KGgo")) {
            return "image/png";
        } else if (prefix.startsWith("/9j/") || prefix.startsWith("/9j/4")) {
            return "image/jpeg";
        } else if (prefix.startsWith("R0lGODlh")) {
            return "image/gif";
        } else if (prefix.startsWith("UklGR")) {
            return "image/webp";
        } else if (prefix.startsWith("Qk")) {
            return "image/bmp";
        } else {
            return "image/jpeg"; // 默认
        }
    }
    
    /**
     * 从URL下载图片并解密 - 完整的流程
     * @param imageUrl 图片URL
     * @return 解密后的图片Data URL
     * @throws Exception 下载或解密异常
     */
    public static String downloadAndDecryptImage(String imageUrl) throws Exception {
        
        // 1. 检查是否为CDN图片
        if (!isCdnImage(imageUrl)) {
            System.out.println("非CDN图片，直接返回URL");
            return "url(\"" + imageUrl + "\")";
        }
        
        // 2. 下载图片（这里需要网络请求，实际使用时需要添加HTTP客户端）
        byte[] imageData = downloadImageFromUrl(imageUrl);
        System.out.println("下载完成，数据大小: " + imageData.length + " 字节");
        
        // 3. 转换为Base64
        String encryptedBase64 = Base64.getEncoder().encodeToString(imageData);
        System.out.println("转换为Base64，长度: " + encryptedBase64.length());
        
        // 4. 解密图片
        String decryptedBase64 = decryptImage(encryptedBase64);
        
        // 5. 获取文件扩展名
        String extension = getFileExtensionFromUrl(imageUrl);
        String mimeType = getMimeTypeFromExtension(extension);
        
        // 6. 返回完整的Data URL
        return "data:image/" + mimeType + ";base64," + decryptedBase64;
    }
    
    /**
     * 检查是否为CDN图片 - 对应前端的 is_cdnimg 函数
     */
    private static boolean isCdnImage(String path) {
        if (path == null) {
            return false;
        }
        
        if (path.contains("/xiao/")) {
            return true;
        }
        if (path.contains("/usr/")) {
            return true;
        }
        if (path.contains("/upload_01/")) {
            return true;
        }
        return path.contains("/upload/upload/");
    }
    
    /**
     * 从URL获取文件扩展名
     */
    private static String getFileExtensionFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "jpg";
        }
        
        // 移除查询参数
        String cleanUrl = url.split("\\?")[0];
        
        // 获取扩展名
        String[] parts = cleanUrl.split("\\.");
        if (parts.length > 1) {
            return parts[parts.length - 1].toLowerCase();
        }
        
        return "jpg"; // 默认
    }
    
    /**
     * 根据扩展名获取MIME类型
     */
    private static String getMimeTypeFromExtension(String extension) {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("jpg", "jpeg");
        mimeTypes.put("jpeg", "jpeg");
        mimeTypes.put("png", "png");
        mimeTypes.put("gif", "gif");
        mimeTypes.put("webp", "webp");
        mimeTypes.put("bmp", "bmp");
        mimeTypes.put("svg", "svg+xml");
        
        return mimeTypes.getOrDefault(extension.toLowerCase(), "jpeg");
    }
    
    /**
     * 十六进制字符串转字节数组
     */
    private static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * 从URL下载图片（简化版，实际使用时需要网络库）
     */
    private static byte[] downloadImageFromUrl(String url) throws Exception {
        // 这里需要实际的HTTP客户端实现
        // 例如使用 HttpClient、OkHttp 或 URLConnection
        
        String result = OkHttp.string(url);
        
        return result.getBytes();
        //
    }
    

}
