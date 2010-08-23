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
package com.google.gwt.safehtml.shared;

/**
 * Sanitizes untrusted HTML.
 * 
 * Note on usage: SafeHtml should be used to ensure user input is not executed 
 * in the browser. SafeHtml should not be used to sanitize input before sending 
 * it to the server.
 * 
 */
public interface HtmlSanitizer {

  /**
   * Sanitizes a string into {@code SafeHtml}.
   *
   * @param html String containing untrusted HTML.
   * @return Contents of {@code html}, sanitized according to the
   *     policy implemented by this sanitizer.
   */
  SafeHtml sanitize(String html);
}
