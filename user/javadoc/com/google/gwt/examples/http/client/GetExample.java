package com.google.gwt.examples.http.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

public class GetExample implements EntryPoint {
  public static final int STATUS_CODE_OK = 200;
  
  public static void doGet(String url) {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

    try {
      Request response = builder.sendRequest(null, new RequestCallback() {
        public void onError(Request request, Throwable exception) {
          // Code omitted for clarity
        }

        public void onResponseReceived(Request request, Response response) {
          // Code omitted for clarity
        }
      });
    } catch (RequestException e) {
      // Code omitted for clarity
    }
  }

  public void onModuleLoad() {
    doGet("/");
  }
}
