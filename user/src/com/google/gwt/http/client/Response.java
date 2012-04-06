/*
 * Copyright 2008 Google Inc.
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

/**
 * Wrapper which provides access to the components of an HTTP response.
 * 
 * <h3>Required Module</h3>
 * Modules that use this class should inherit
 * <code>com.google.gwt.http.HTTP</code>.
 * 
 * {@gwt.include com/google/gwt/examples/http/InheritsExample.gwt.xml}
 */
public abstract class Response {

  public static final int SC_ACCEPTED = 202;
  public static final int SC_BAD_GATEWAY = 502;
  public static final int SC_BAD_REQUEST = 400;
  public static final int SC_CONFLICT = 409;
  public static final int SC_CONTINUE = 100;
  public static final int SC_CREATED = 201;
  public static final int SC_EXPECTATION_FAILED = 417;
  public static final int SC_FORBIDDEN = 403;
  public static final int SC_GATEWAY_TIMEOUT = 504;
  public static final int SC_GONE = 410;
  public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;
  public static final int SC_INTERNAL_SERVER_ERROR = 500;
  public static final int SC_LENGTH_REQUIRED = 411;
  public static final int SC_METHOD_NOT_ALLOWED = 405;
  public static final int SC_MOVED_PERMANENTLY = 301;
  public static final int SC_MOVED_TEMPORARILY = 302;
  public static final int SC_MULTIPLE_CHOICES = 300;
  public static final int SC_NO_CONTENT = 204;
  public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;
  public static final int SC_NOT_ACCEPTABLE = 406;
  public static final int SC_NOT_FOUND = 404;
  public static final int SC_NOT_IMPLEMENTED = 501;
  public static final int SC_NOT_MODIFIED = 304;
  public static final int SC_OK = 200;
  public static final int SC_PARTIAL_CONTENT = 206;
  public static final int SC_PAYMENT_REQUIRED = 402;
  public static final int SC_PRECONDITION_FAILED = 412;
  public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
  public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;
  public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
  public static final int SC_RESET_CONTENT = 205;
  public static final int SC_SEE_OTHER = 303;
  public static final int SC_SERVICE_UNAVAILABLE = 503;
  public static final int SC_SWITCHING_PROTOCOLS = 101;
  public static final int SC_TEMPORARY_REDIRECT = 307;
  public static final int SC_UNAUTHORIZED = 401;
  public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
  public static final int SC_USE_PROXY = 305;

  /**
   * Returns the value of the requested header or null if the header was not
   * specified.
   * 
   * @param header the header to query for
   * @return the value of response header
   * 
   * @throws IllegalArgumentException if the header name is empty
   * @throws NullPointerException if the header name is null
   */
  public abstract String getHeader(String header);

  /**
   * Returns an array of HTTP headers associated with this response.
   * 
   * @return array of HTTP headers; returns zero length array if there are no
   *         headers
   */
  public abstract Header[] getHeaders();

  /**
   * Returns all headers as a single string. The individual headers are
   * delimited by a CR (U+000D) LF (U+000A) pair. An individual header is
   * formatted according to <a href="http://ietf.org/rfc/rfc2616"> RFC 2616</a>.
   * 
   * @return all headers as a single string delimited by CRLF pairs
   */
  public abstract String getHeadersAsString();

  /**
   * Returns the HTTP status code that is part of this response.
   * 
   * @return the HTTP status code
   */
  public abstract int getStatusCode();

  /**
   * Returns the HTTP status message text.
   * 
   * @return the HTTP status message text
   */
  public abstract String getStatusText();

  /**
   * Returns the text associated with the response.
   * 
   * @return the response text
   */
  public abstract String getText();
}
