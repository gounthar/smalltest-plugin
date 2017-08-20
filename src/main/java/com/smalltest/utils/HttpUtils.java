package com.smalltest.utils;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

public class HttpUtils {

  /**
   * Default socket timeout in seconds.
   */
  private static final Integer SOCKET_TIMEOUT = 60;

  public static UrlEncodedFormEntity toEncodedFormEntity(Object object) throws UnsupportedEncodingException {
    Map<String, String> props = JsonUtils.convertValue(object, Map.class);
    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    for (Map.Entry<String, String> entry : props.entrySet()) {
      nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
    }

    return new UrlEncodedFormEntity(nvps, "UTF-8");
  }

  public static ResponseEntity post(String url, Map<String, String> headers, HttpEntity entity)
    throws Exception {
    HttpPost request = new HttpPost(url);
    addHeader(request, headers);
    if (entity != null)
      request.setEntity(entity);
    return execute(request);
  }

  private static ResponseEntity readResponse(HttpResponse response) throws Exception {
    if (null == response) {
      throw new Exception("Response is null.");
    }
    HttpEntity entity = response.getEntity();
    InputStream entityContent = entity.getContent();
    String result = IOUtils.toString(entityContent);
    return new ResponseEntity(result, response.getStatusLine().getStatusCode());
  }

  private static ResponseEntity execute(HttpUriRequest request) throws Exception {
    HttpClient client = getHttpClient();
    HttpResponse response = null;
    ResponseEntity responseEntity;

    try {
      response = client.execute(request);
      responseEntity = readResponse(response);
    } finally {
      if (null != response)
        org.apache.http.client.utils.HttpClientUtils.closeQuietly(response);
    }

    return responseEntity;
  }

  private static void addHeader(HttpRequestBase httpRequestBase, Map<String, String> headers) {
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        httpRequestBase.addHeader(entry.getKey(), entry.getValue());
      }
    }
  }

  private static HttpClient getHttpClient() throws Exception {

    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
      .useSystemProperties();
    SSLConnectionSocketFactory sslSocketFactory = getSslSocketFactory();
    httpClientBuilder.setSSLSocketFactory(sslSocketFactory)
      .setConnectionReuseStrategy(new NoConnectionReuseStrategy());

    int timeout = SOCKET_TIMEOUT * 1000;
    httpClientBuilder.setDefaultRequestConfig(RequestConfig.custom()
      .setSocketTimeout(timeout)
      .setConnectTimeout(timeout)
      .setConnectionRequestTimeout(timeout)
      .build());
    return httpClientBuilder.build();
  }

  private static SSLConnectionSocketFactory getSslSocketFactory()
    throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = getSslContext();
    return new SSLConnectionSocketFactory(sslContext, ALLOW_ALL_HOSTNAME_VERIFIER);
  }

  private static SSLContext getSslContext() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    org.apache.http.ssl.SSLContextBuilder sslContextBuilder = new org.apache.http.ssl.SSLContextBuilder();
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    TrustStrategy trustStrategy = new TrustAllStrategy();
    sslContextBuilder.loadTrustMaterial(keyStore, trustStrategy);
    return sslContextBuilder.build();
  }

  /**
   * Trust all certificates.
   */
  public static class TrustAllStrategy implements TrustStrategy {

    @Override
    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      return true;
    }
  }
}