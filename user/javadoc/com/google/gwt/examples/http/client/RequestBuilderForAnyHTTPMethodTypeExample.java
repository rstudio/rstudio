package com.google.gwt.examples.http.client;

import com.google.gwt.http.client.RequestBuilder;

public class RequestBuilderForAnyHTTPMethodTypeExample extends RequestBuilder {
  
  /**
   * Constructor that allows a developer to override the HTTP method 
   * restrictions imposed by the RequestBuilder class.  Note if you override the 
   * RequestBuilder's HTTP method restrictions in this manner, your application 
   * may not work correctly on Safari browsers.
   * 
   * @param httpMethod any non-null, non-empty string is considered valid
   * @param url any non-null, non-empty string is considered valid
   *
   * @throws IllegalArgumentException if httpMethod or url are empty
   * @throws NullPointerException if httpMethod or url are null
   */
  public RequestBuilderForAnyHTTPMethodTypeExample(String httpMethod, String url) {
    super(httpMethod, url);
  }
}
