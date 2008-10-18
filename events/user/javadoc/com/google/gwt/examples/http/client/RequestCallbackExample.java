package com.google.gwt.examples.http.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.http.client.Response;

public class RequestCallbackExample implements RequestCallback {

  private static final int STATUS_CODE_OK = 200;

  public void onError(Request request, Throwable exception) {
    if (exception instanceof RequestTimeoutException) {
      // handle a request timeout
    } else {
      // handle other request errors
    }
  }

  public void onResponseReceived(Request request, Response response) {
    if (STATUS_CODE_OK == response.getStatusCode()) {
      // handle OK response from the server 
    } else {
      // handle non-OK response from the server
    }
  }
}
