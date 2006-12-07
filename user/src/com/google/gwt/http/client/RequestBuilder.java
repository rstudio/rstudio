/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.http.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.impl.HTTPRequestImpl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Builder for constructing {@link com.google.gwt.http.client.Request} objects.
 * 
 * <p>
 * By default, this builder is restricted to building HTTP GET and POST requests
 * due to a bug in Safari's implementation of the <code>XmlHttpRequest</code>
 * object.
 * </p>
 * 
 * <p>
 * Please see <a href="http://bugs.webkit.org/show_bug.cgi?id=3812">
 * http://bugs.webkit.org/show_bug.cgi?id=3812</a> for more details.
 * </p>
 * 
 * <h3>Required Module</h3>
 * Modules that use this class should inherit
 * <code>com.google.gwt.http.HTTP</code>.
 * 
 * {@gwt.include com/google/gwt/examples/http/InheritsExample.gwt.xml}
 * 
 */
public class RequestBuilder {
  /**
   * HTTP request method constants.
   */
  public static final class Method {
    private final String name;

    private Method(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }

  /**
   * Specifies that the HTTP GET method should be used.
   */
  public static final Method GET = new Method("GET");

  /**
   * Specifies that the HTTP POST method should be used.
   */
  public static final Method POST = new Method("POST");

  private static final HTTPRequestImpl httpRequest = (HTTPRequestImpl) GWT.create(HTTPRequestImpl.class);

  /*
   * Map of header name to value that will be added to the JavaScript
   * XmlHttpRequest object before sending a request.
   */
  private Map headers;

  /*
   * HTTP method to use when opening an JavaScript XmlHttpRequest object
   */
  private String httpMethod;

  /*
   * Password to use when opening an JavaScript XmlHttpRequest object
   */
  private String password;

  /*
   * Timeout in milliseconds before the request timeouts and fails.
   */
  private int timeoutMillis;

  /*
   * URL to use when opening an JavaScript XmlHttpRequest object.
   */
  private String url;

  /*
   * User to use when opening an JavaScript XmlHttpRequest object
   */
  private String user;

  /**
   * Creates a builder using the parameters for configuration.
   * 
   * @param httpMethod HTTP method to use for the request
   * @param url URL that has already has already been encoded. Please see
   *          {@link com.google.gwt.http.client.URL#encode(String)} and
   *          {@link com.google.gwt.http.client.URL#encodeComponent(String)} for
   *          how to do this.
   * @throws IllegalArgumentException if the httpMethod or URL are empty
   * @throws NullPointerException if the httpMethod or the URL are null
   */
  public RequestBuilder(Method httpMethod, String url) {
    this((httpMethod == null) ? null : httpMethod.toString(), url);
  }

  /**
   * Creates a builder using the parameters values for configuration.
   * 
   * @param httpMethod HTTP method to use for the request
   * @param url URL that has already has already been URL encoded. Please see
   *          {@link com.google.gwt.http.client.URL#encode(String)} and
   *          {@link com.google.gwt.http.client.URL#encodeComponent(String)} for
   *          how to do this.
   * @throws IllegalArgumentException if the httpMethod or URL are empty
   * @throws NullPointerException if the httpMethod or the URL are null
   * 
   * <p>
   * <b>WARNING:</b>This method is provided in order to allow the creation of
   * HTTP request other than GET and POST to be made. If this is done, the
   * developer must accept that the behavior on Safari is undefined.
   * </p>
   */
  protected RequestBuilder(String httpMethod, String url) {

    StringValidator.throwIfEmptyOrNull("httpMethod", httpMethod);
    StringValidator.throwIfEmptyOrNull("url", url);

    this.httpMethod = httpMethod;
    this.url = url;
  }

  /**
   * Sends an HTTP request based on the current builder configuration. If no
   * request headers have been set, the header "Content-Type" will be used with
   * a value of "text/plain; charset=utf-8".
   * 
   * @param requestData the data to send as part of the request
   * @param callback the response handler to be notified when the request fails
   *          or completes
   * @return a {@link Request} object that can be used to track the request
   */
  public Request sendRequest(String requestData, RequestCallback callback)
      throws RequestException {
    JavaScriptObject xmlHttpRequest = httpRequest.createXmlHTTPRequest();

    String openError = XMLHTTPRequest.open(xmlHttpRequest, httpMethod, url,
        true, user, password);
    if (openError != null) {
      throw new RequestPermissionException(url);
    }

    setHeaders(xmlHttpRequest);

    Request request = new Request(xmlHttpRequest, timeoutMillis, callback);

    String sendError = XMLHTTPRequest.send(xmlHttpRequest, request,
        requestData, callback);
    if (sendError != null) {
      throw new RequestException(sendError);
    }

    return request;
  }

  /**
   * Sets a request header with the given name and value. If a header with the
   * specified name has already been set then the new value overwrites the
   * current value.
   * 
   * @param header the name of the header
   * @param value the value of the header
   * 
   * @throws NullPointerException if header or value are null
   * @throws IllegalArgumentException if header or value are the empty string
   */
  public void setHeader(String header, String value) {
    StringValidator.throwIfEmptyOrNull("header", header);
    StringValidator.throwIfEmptyOrNull("value", value);

    if (headers == null) {
      headers = new HashMap();
    }

    headers.put(header, value);
  }

  /**
   * Sets the password to use in the request URL. This is ignored if there is no
   * user specified.
   * 
   * @param password password to use in the request URL
   * 
   * @throws IllegalArgumentException if the password is empty
   * @throws NullPointerException if the password is null
   */
  public void setPassword(String password) {
    StringValidator.throwIfEmptyOrNull("password", password);

    this.password = password;
  }

  /**
   * Sets the number of milliseconds to wait for a request to complete. Should
   * the request timeout, the
   * {@link com.google.gwt.http.client.RequestCallback#onError(Request, Throwable)}
   * method will be called on the callback instance given to the
   * {@link com.google.gwt.http.client.RequestBuilder#sendRequest(String, RequestCallback)}
   * method. The callback method will receive an instance of the
   * {@link com.google.gwt.http.client.RequestTimeoutException} class as its
   * {@link java.lang.Throwable} argument.
   * 
   * @param timeoutMillis number of milliseconds to wait before canceling the
   *          request, a value of zero disables timeouts
   * 
   * @throws IllegalArgumentException if the timeout value is negative
   */
  public void setTimeoutMillis(int timeoutMillis) {
    if (timeoutMillis < 0) {
      throw new IllegalArgumentException("Timeouts cannot be negative");
    }

    this.timeoutMillis = timeoutMillis;
  }

  /**
   * Sets the user name that will be used in the request URL.
   * 
   * @param user user name to use
   * @throws IllegalArgumentException if the user is empty
   * @throws NullPointerException if the user is null
   */
  public void setUser(String user) {
    StringValidator.throwIfEmptyOrNull("user", user);

    this.user = user;
  }

  /*
   * Internal method that actually sets our cached headers on the underlying
   * JavaScript XmlHttpRequest object. If there are no headers set, then we set
   * the "Content-Type" to "text/plain; charset=utf-8". This is really lining us
   * up for integration with RPC.
   */
  private void setHeaders(JavaScriptObject xmlHttpRequest)
      throws RequestException {
    if (headers != null && headers.size() > 0) {
      Set entrySet = headers.entrySet();
      Iterator iter = entrySet.iterator();
      while (iter.hasNext()) {
        Map.Entry header = (Map.Entry) iter.next();
        String errorMessage = XMLHTTPRequest.setRequestHeader(xmlHttpRequest,
            (String) header.getKey(), (String) header.getValue());
        if (errorMessage != null) {
          throw new RequestException(errorMessage);
        }
      }
    } else {
      XMLHTTPRequest.setRequestHeader(xmlHttpRequest, "Content-Type",
          "text/plain; charset=utf-8");
    }
  }
}
