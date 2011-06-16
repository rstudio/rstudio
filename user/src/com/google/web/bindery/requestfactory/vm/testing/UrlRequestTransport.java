/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.web.bindery.requestfactory.vm.testing;

import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.RequestTransport;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

import org.json.Cookie;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * A trivial implementation of RequestTransport that uses a
 * {@link HttpURLConnection}. Details of the connection can be amended by
 * overriding {@link #configureConnection(HttpURLConnection)}.
 * <p>
 * This implementation only supports {@code http} and {@code https} URLs. It has
 * primitive support for recording and playing back cookies, but does not
 * implement expiration processing.
 * <p>
 * Developers who wish to build a more production-ready client should consider
 * using a RequestTRansport based around Apache HttpClient instead.
 */
public class UrlRequestTransport implements RequestTransport {
  private static final int CONNECT_TIMEOUT = 30000;
  private static final int READ_TIMEOUT = 60000;

  private final Map<String, String> cookies = new HashMap<String, String>();
  private final URL url;

  /**
   * Construct a new UrlRequestTransport.
   * 
   * @param url the URL to connect to
   * @throws IllegalArgumentException if the url's protocol is not {@code http}
   *           or {@code https}
   */
  public UrlRequestTransport(URL url) {
    this.url = url;
    String proto = url.getProtocol().toLowerCase();
    if (!proto.equals("http") && !proto.equals("https")) {
      throw new IllegalArgumentException("Only http and https URLs supported");
    }
  }

  /**
   * Provides access to the cookies that will be sent for subsequent requests.
   */
  public Map<String, String> getCookies() {
    return cookies;
  }

  @Override
  public void send(String payload, TransportReceiver receiver) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) url.openConnection();
      configureConnection(connection);

      OutputStream out = connection.getOutputStream();
      out.write(payload.getBytes("UTF-8"));
      out.close();

      int status = connection.getResponseCode();
      if (status != HttpURLConnection.HTTP_OK) {
        ServerFailure failure = new ServerFailure(status + " " + connection.getResponseMessage());
        receiver.onTransportFailure(failure);
        return;
      }

      List<String> cookieHeaders = connection.getHeaderFields().get("Set-Cookie");
      if (cookieHeaders != null) {
        for (String header : cookieHeaders) {
          try {
            JSONObject cookie = Cookie.toJSONObject(header);
            String name = cookie.getString("name");
            String value = cookie.getString("value");
            String domain = cookie.optString("Domain");
            if (domain == null || url.getHost().endsWith(domain)) {
              String path = cookie.optString("Path");
              if (path == null || url.getPath().startsWith(path)) {
                cookies.put(name, value);
              }
            }
          } catch (JSONException ignored) {
          }
        }
      }

      String encoding = connection.getContentEncoding();
      InputStream in = connection.getInputStream();
      if ("gzip".equalsIgnoreCase(encoding)) {
        in = new GZIPInputStream(in);
      } else if ("deflate".equalsIgnoreCase(encoding)) {
        in = new InflaterInputStream(in);
      } else if (encoding != null) {
        receiver.onTransportFailure(new ServerFailure("Unknown server encoding " + encoding));
        return;
      }

      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int read = in.read(buffer);
      while (read != -1) {
        bytes.write(buffer, 0, read);
        read = in.read(buffer);
      }
      in.close();

      String received = new String(bytes.toByteArray(), "UTF-8");
      receiver.onTransportSuccess(received);
    } catch (IOException e) {
      ServerFailure failure = new ServerFailure(e.getMessage(), e.getClass().getName(), null, true);
      receiver.onTransportFailure(failure);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  protected void configureConnection(HttpURLConnection connection) throws IOException {
    connection.setDoInput(true);
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    connection.setUseCaches(false);
    connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
    connection.setRequestProperty("Content-Type", RequestFactory.JSON_CONTENT_TYPE_UTF8);
    connection.setRequestProperty("Host", url.getHost());
    connection.setRequestProperty("User-Agent", UrlRequestTransport.class.getCanonicalName());
    connection.setConnectTimeout(CONNECT_TIMEOUT);
    connection.setReadTimeout(READ_TIMEOUT);

    if (!cookies.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      boolean needsSemi = false;
      for (Map.Entry<String, String> entry : cookies.entrySet()) {
        if (needsSemi) {
          sb.append("; ");
        } else {
          needsSemi = true;
        }
        sb.append(entry.getKey()).append("=").append(entry.getValue());
      }
      connection.setRequestProperty("Cookie", sb.toString());
    }
  }
}
