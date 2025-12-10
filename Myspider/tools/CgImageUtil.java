package com.github.catvod.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;

import android.util.Base64;



public class CgImageUtil {
	    
    public static final String CBC_PKCS_7_PADDING = "AES/CBC/PKCS7Padding";
    public static final String ECB_PKCS_7_PADDING = "AES/ECB/PKCS5Padding";

    private static String aesDecrypt(String word ,String keyString, String ivString, String padding ) {
        try {
            //Security.addProvider(new BouncyCastleProvider());

            byte[] srcBytes = Base64.decode(word, Base64.DEFAULT);
            byte[] keyBytes = keyString.getBytes("UTF-8");
            byte[] ivBytes = ivString.getBytes("UTF-8");

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance(padding);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));

            byte[] decryptedBytes = cipher.doFinal(srcBytes);
            return Base64.encodeToString(decryptedBytes,Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String loadBackgroundImage(String bgUrl,String keyString, String ivString, String padding ) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(bgUrl)
                    .build();
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (body != null) {
                byte[] imageBytes = body.bytes();
				if ( isUnCreptedImg(imageBytes) ) return "url(\"" + bgUrl + "\")";
                String base64Str = Base64.encodeToString(imageBytes,Base64.DEFAULT);
                System.out.println(base64Str);
                String decryptedStr = aesDecrypt(base64Str,keyString,ivString,padding);
                // 将解密后的数据拼接为Data URL
                String[] ary = bgUrl.split("\\.");
                String base64st = "data:image/" + ary[ary.length - 1] + ";base64," + decryptedStr;
                return base64st;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    /**
     * 通过字节数据检测图片格式
     */
    private static boolean isUnCreptedImg(byte[] data) {

        return isJPEG(data) || isPNG(data) || isGIF(data) || isWebP(data) || isBMP(data) || isTIFF(data) || isICO(data);

    }
	
	// Java检测JPEG
	private static boolean isJPEG(byte[] data) {
		if (data.length < 4) return false;
		return (data[0] & 0xFF) == 0xFF && 
           (data[1] & 0xFF) == 0xD8 && 
           (data[2] & 0xFF) == 0xFF && 
           ((data[3] & 0xFF) == 0xE0 || 
            (data[3] & 0xFF) == 0xE1 ||
            (data[3] & 0xFF) == 0xE8);
	}

	// Java检测PNG
	private static boolean isPNG(byte[] data) {
		if (data.length < 8) return false;
		return data[0] == (byte)0x89 && 
           data[1] == 0x50 && // 'P'
           data[2] == 0x4E && // 'N'
           data[3] == 0x47 && // 'G'
           data[4] == 0x0D && // CR
           data[5] == 0x0A && // LF
           data[6] == 0x1A && // EOF
           data[7] == 0x0A;   // LF
	}
	
	// Java检测GIF
	private static boolean isGIF(byte[] data) {
		if (data.length < 6) return false;
		return (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46 && 
            data[3] == 0x38 && 
           (data[4] == 0x37 || data[4] == 0x39) && // 87a或89a
            data[5] == 0x61);
	}

	// Java检测WebP
	private static boolean isWebP(byte[] data) {
		if (data.length < 12) return false;
		// 检查RIFF头
		boolean isRiff = data[0] == 0x52 && data[1] == 0x49 && 
                     data[2] == 0x46 && data[3] == 0x46;
		// 检查WEBP标识
		boolean isWebP = data[8] == 0x57 && data[9] == 0x45 && 
                     data[10] == 0x42 && data[11] == 0x50;
		return isRiff && isWebP;
	}

	// Java检测BMP
	private static boolean isBMP(byte[] data) {
		if (data.length < 2) return false;
		return data[0] == 0x42 && data[1] == 0x4D; // "BM"
	}

	// Java检测TIFF
	private static boolean isTIFF(byte[] data) {
		if (data.length < 4) return false;
		// 小端序
		boolean littleEndian = data[0] == 0x49 && data[1] == 0x49 && 
								data[2] == 0x2A && data[3] == 0x00;
		// 大端序
		boolean bigEndian = data[0] == 0x4D && data[1] == 0x4D && 
							data[2] == 0x00 && data[3] == 0x2A;
		return littleEndian || bigEndian;
	}

	// Java检测SVG
	private static boolean isSVG(String content) {
		return content.trim().startsWith("<?xml") || 
			content.contains("<svg") || 
			content.contains("xmlns=\"http://www.w3.org/2000/svg\"");
	}

	// Base64检测
	private static boolean isSVGBase64(String base64) {
		String decoded = new String(Base64.getDecoder().decode(base64));
		return isSVG(decoded);
	}

	// Java检测ICO
	private static boolean isICO(byte[] data) {
		if (data.length < 4) return false;
		return data[0] == 0x00 && data[1] == 0x00 && 
           data[2] == 0x01 && data[3] == 0x00;
	}

}
