package com.google.gwt.examples.http.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

public class PostExample {
  public static void doPost(String url, String postData) {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);

    try {
      builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
      Request response = builder.sendRequest(postData, new RequestCallback() {

        public void onError(Request request, Throwable exception) {
          // code omitted for clarity
        }

        public void onResponseReceived(Request request, Response response) {
          // code omitted for clarity
        }
      });
    } catch (RequestException e) {
      Window.alert("Failed to send the request: " + e.getMessage());
    }
  }

  public void onModuleLoad() {
    doPost("/", "Hello World!");
  }
}
