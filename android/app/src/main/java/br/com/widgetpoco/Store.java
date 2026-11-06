package br.com.widgetpoco;
import android.content.*;import android.security.keystore.*;import android.util.Base64;import java.security.*;import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;import org.json.*;
final class Store {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("tim",0);}
 static void secret(Context c,String s)throws Exception{KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);if(!k.containsAlias("tim")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("tim",3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());g.generateKey();}Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(1,k.getKey("tim",null));prefs(c).edit().putString("iv",Base64.encodeToString(x.getIV(),2)).putString("session",Base64.encodeToString(x.doFinal(s.getBytes("UTF-8")),2)).apply();}
 static JSONObject secret(Context c)throws Exception{KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(2,k.getKey("tim",null),new GCMParameterSpec(128,Base64.decode(prefs(c).getString("iv",""),2)));return new JSONObject(new String(x.doFinal(Base64.decode(prefs(c).getString("session",""),2)),"UTF-8"));}
 static boolean hasCredentials(Context c){return prefs(c).contains("credentials");}
 static void credentials(Context c,String number,String password)throws Exception{
  KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);
  if(!k.containsAlias("tim")){secret(c,"{}");k.load(null);}
  Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(1,k.getKey("tim",null));
  JSONObject o=new JSONObject().put("number",number).put("password",password);
  prefs(c).edit().putString("credentialsIv",Base64.encodeToString(x.getIV(),2)).putString("credentials",Base64.encodeToString(x.doFinal(o.toString().getBytes("UTF-8")),2)).apply();
 }
 static JSONObject credentials(Context c)throws Exception{
  KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);Cipher x=Cipher.getInstance("AES/GCM/NoPadding");
  x.init(2,k.getKey("tim",null),new GCMParameterSpec(128,Base64.decode(prefs(c).getString("credentialsIv",""),2)));
  return new JSONObject(new String(x.doFinal(Base64.decode(prefs(c).getString("credentials",""),2)),"UTF-8"));
 }
}
