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

/**
 * Exception thrown when the {@link RequestBuilder} attempts to make a request
 * to a URL which violates the <a
 * href="http://en.wikipedia.org/wiki/Same_origin_policy">Same-Origin Security
 * Policy</a>.
 * <p>
 * Note: this exception will only be thrown by browsers that do not support
 * <a href="http://www.w3.org/TR/cors">CORS</a>, i.e. Internet Explorer up to
 * version 9. All other browsers will instead return a
 * {@link Response#getStatusCode() status code} of 0. Internet Explorer can
 * also be configured to allow such cross-origin requests without error and
 * without using CORS.
 * 
 * <h3>Required Module</h3>
 * Modules that use this class should inherit
 * <code>com.google.gwt.http.HTTP</code>.
 * 
 * {@gwt.include com/google/gwt/examples/http/InheritsExample.gwt.xml}
 */
public class RequestPermissionException extends RequestException {

  /**
   * URL which caused this exception to be thrown.
   */
  private final String url;

  /**
   * Constructs an instance of this class for the given URL.
   * 
   * @param url the URL which cannot be accessed
   */
  public RequestPermissionException(String url) {
    super("The URL " + url
        + " is invalid or violates the same-origin security restriction");

    this.url = url;
  }

  /**
   * Returns the URL which we cannot access.
   * 
   * @return the URL which we cannot access.
   */
  public String getURL() {
    return url;
  }
}
