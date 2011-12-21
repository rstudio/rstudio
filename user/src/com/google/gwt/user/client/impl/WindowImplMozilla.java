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
package com.google.gwt.user.client.impl;

/**
 * Mozilla implementation of {@link com.google.gwt.user.client.impl.WindowImpl}.
 */
public class WindowImplMozilla extends WindowImpl {

  /**
   * For Mozilla, reading from $wnd.location.hash decodes the fragment.
   * https://bugzilla.mozilla.org/show_bug.cgi?id=483304
   * https://bugzilla.mozilla.org/show_bug.cgi?id=135309
   * To avoid this bug, we use location.href instead.
   */
  @Override
  public native String getHash() /*-{
    var href = $wnd.location.href;
    var hashLoc = href.indexOf("#");
    return (hashLoc > 0) ? href.substring(hashLoc) : "";
  }-*/;

}
