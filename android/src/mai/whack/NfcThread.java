/**
 */
package mai.whack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.Uri;

/**
 * @author thanhhaipmai@gmail.com (Thanh Hai Mai)
 */
public class NfcThread extends Thread {
  @Override
  public void run() {

  }

  public static String getStringContent(String uri) {
    HttpClient client = new DefaultHttpClient();
    HttpGet request = new HttpGet();
    try {
      Uri.Builder b = Uri.parse(uri).buildUpon();
      request.setURI(new URI(uri));
    } catch (URISyntaxException e) {
      return "";
    }
    HttpResponse response;
    StringBuilder sb = new StringBuilder();
    try {
      response = client.execute(request);
      InputStream ips;
      ips = response.getEntity().getContent();
      BufferedReader buf = null;
      buf = new BufferedReader(new InputStreamReader(ips, "UTF-8"));

      String s = null;
      while (true) {
        s = buf.readLine();
        if (s == null || s.length() == 0) {
          break;
        }
        sb.append(s);
      }
      buf.close();
      ips.close();
    } catch (ClientProtocolException e) {
      return "";
    } catch (UnsupportedEncodingException e) {
      return "";
    } catch (IOException e) {
      return "";
    } catch (IllegalStateException e) {
      return "";
    }
    return sb.toString();
  }
}
