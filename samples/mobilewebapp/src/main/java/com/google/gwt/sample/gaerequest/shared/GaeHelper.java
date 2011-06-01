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
package com.google.gwt.sample.gaerequest.shared;

/**
 * Helper class for sharing values on the server and client.
 */
public interface GaeHelper {

  /**
   * The placeholder token added to the login URL. The client replaces the token
   * with the current href, which only the client knows.
   */
  /* Prefixed with http:// to ensure that GAE doesn't automatically prefix it. */
  String REDIRECT_URL_TOKEN = "http%3A%2F%2FREDIRECTURL";
}
