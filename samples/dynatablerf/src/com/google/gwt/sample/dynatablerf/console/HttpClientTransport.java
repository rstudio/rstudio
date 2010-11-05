/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.dynatablerf.console;

import com.google.gwt.dev.util.Util;
import com.google.gwt.requestfactory.shared.RequestTransport;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

/**
 * This is a simple implementation of {@link RequestTransport} that uses
 * HttpClient. It is not suitable for production use, but demonstrates the
 * minimum functionality necessary to implement a custom RequestTransport.
 */
class HttpClientTransport implements RequestTransport {
  private final URI uri;

  public HttpClientTransport(URI uri) {
    this.uri = uri;
  }

  @Override
  public void send(String payload, TransportReceiver receiver) {
    HttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost();
    post.setHeader("Content-Type", "application/json;charset=UTF-8");
    post.setURI(uri);
    Throwable ex;
    try {
      post.setEntity(new StringEntity(payload, "UTF-8"));
      HttpResponse response = client.execute(post);
      if (200 == response.getStatusLine().getStatusCode()) {
        String contents = Util.readStreamAsString(response.getEntity().getContent());
        receiver.onTransportSuccess(contents);
      } else {
        receiver.onTransportFailure(response.getStatusLine().getReasonPhrase());
      }
      return;
    } catch (UnsupportedEncodingException e) {
      ex = e;
    } catch (ClientProtocolException e) {
      ex = e;
    } catch (IOException e) {
      ex = e;
    }
    receiver.onTransportFailure(ex.getMessage());
  }
}