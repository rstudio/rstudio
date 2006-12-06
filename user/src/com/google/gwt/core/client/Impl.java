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
package com.google.gwt.core.client;

final class Impl {

  private static int sNextHashId = 0;

  protected static int getNextHashId() {
    return ++sNextHashId;
  }

  /*
   * We need a separate overload for JavaScriptObject, so that the hosted mode
   * JSNI invocation system will know to unwrap the Java object back to its
   * underlying JavaScript object.
   */
  static native int getHashCode(JavaScriptObject o) /*-{
    return (o == null) ? 0 : 
     (o.$H ? o.$H : (o.$H = @com.google.gwt.core.client.Impl::getNextHashId()()));
  }-*/;

  static native int getHashCode(Object o) /*-{
    return (o == null) ? 0 : 
     (o.$H ? o.$H : (o.$H = @com.google.gwt.core.client.Impl::getNextHashId()()));
  }-*/;

  static native String getModuleBaseURL() /*-{
    // this is intentionally not using $doc, because we want the module's own url
    var s = document.location.href;

    // Pull off any hash.
    var i = s.indexOf('#');
    if (i != -1)
      s = s.substring(0, i);

    // Pull off any query string.
    i = s.indexOf('?');
    if (i != -1)
      s = s.substring(0, i);

    // Rip off everything after the last slash.
    i = s.lastIndexOf('/');
    if (i != -1)
      s = s.substring(0, i);
      
    // Ensure a final slash if non-empty.
    return s.length > 0 ? s + "/" : "";
  }-*/;
}
