package com.lazooz.lbm.components;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;





import android.util.Base64;




public class MCrypt {
	
	private static MCrypt instance = null;

	public static MCrypt getInstance() {
	    if(instance == null) {
	       instance = new MCrypt();
	    }
	    return instance;
	 }

	public static void removeInstance() {
	    instance = null;
	 }
	
	
	
	 private String iv = "fedcba9876543210";//Dummy iv (CHANGE IT!)
     private IvParameterSpec ivspec;
     private SecretKeySpec keyspec;
     private Cipher cipher;
     
     private String mSecretKey = "0123456789abcdef";//Dummy secretKey (CHANGE IT!)
     
     public MCrypt()
     {
             ivspec = new IvParameterSpec(iv.getBytes());

             keyspec = new SecretKeySpec(mSecretKey.getBytes(), "AES");
             
             try {
                     cipher = Cipher.getInstance("AES/CBC/NoPadding");
             } catch (NoSuchAlgorithmException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
             } catch (NoSuchPaddingException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
             }
     }
     
     public byte[] encrypt(String text) throws Exception
     {
             if(text == null || text.length() == 0)
                     throw new Exception("Empty string");
             
             byte[] encrypted = null;
             
             byte[] data = text.getBytes("UTF-8");
             String base64 = Base64.encodeToString(data, Base64.DEFAULT);
             
             
             

             try {
                     cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);

                     encrypted = cipher.doFinal(padString(base64).getBytes());
             } catch (Exception e)
             {                       
                     throw new Exception("[encrypt] " + e.getMessage());
             }
             
             return encrypted;
     }
     
     public String decrypt(String code) throws Exception
     {
             if(code == null || code.length() == 0)
                     throw new Exception("Empty string");
             
             byte[] decrypted = null;
             String text = "";
             try {
                     cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
                     
                     decrypted = cipher.doFinal(hexToBytes(code));
                     
                     byte[] data = Base64.decode(decrypted, Base64.DEFAULT);
                     text = new String(data, "UTF-8");
                     
                     
                     
             } catch (Exception e)
             {
                     throw new Exception("[decrypt] " + e.getMessage());
             }
             return text;
     }
     

     
     public String bytesToHex(byte[] data)
     {
             if (data==null)
             {
                     return null;
             }
             
             int len = data.length;
             String str = "";
             for (int i=0; i<len; i++) {
                     if ((data[i]&0xFF)<16)
                             str = str + "0" + java.lang.Integer.toHexString(data[i]&0xFF);
                     else
                             str = str + java.lang.Integer.toHexString(data[i]&0xFF);
             }
             return str;
     }
     
             
     public byte[] hexToBytes(String str) {
             if (str==null) {
                     return null;
             } else if (str.length() < 2) {
                     return null;
             } else {
                     int len = str.length() / 2;
                     byte[] buffer = new byte[len];
                     for (int i=0; i<len; i++) {
                             buffer[i] = (byte) Integer.parseInt(str.substring(i*2,i*2+2),16);
                     }
                     return buffer;
             }
     }
     
     

     private String padString(String source)
     {
       char paddingChar = ' ';
       int size = 16;
       int x = source.length() % size;
       int padLength = size - x;

       for (int i = 0; i < padLength; i++)
       {
               source += paddingChar;
       }

       return source;
     }

	public String getSecretKey() {
		return mSecretKey;
	}

	public void setSecretKey(String secretKey) {
		mSecretKey = secretKey;
	}
}





