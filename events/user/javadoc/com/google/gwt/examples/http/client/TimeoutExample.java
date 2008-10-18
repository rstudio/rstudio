package com.google.gwt.examples.http.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

public class TimeoutExample implements EntryPoint {
  public static void doGetWithTimeout(String url) {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

    try {
      /*
       * wait 2000 milliseconds for the request to complete
       */
      builder.setTimeoutMillis(2000);
      
      Request response = builder.sendRequest(null, new RequestCallback() {
        public void onError(Request request, Throwable exception) {
          if (exception instanceof RequestTimeoutException) {
            // handle a request timeout
          } else {
            // handle other request errors
          }
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
    doGetWithTimeout("/");
  }
}
