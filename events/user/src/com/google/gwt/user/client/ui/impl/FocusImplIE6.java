/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.Element;

/**
 * Implementation of {@link com.google.gwt.user.client.ui.impl.FocusImpl}
 * for IE that traps invalid focus attempts to match other browsers.
 */
public class FocusImplIE6 extends FocusImpl {

  @Override
  public native void focus(Element elem) /*-{
    try {
      elem.focus();
    } catch (e) {
      // Only trap the exception if the attempt was mostly legit
      if (!elem || !elem.focus) {
        // Rethrow the probable NPE or invalid type
        throw e;
      }
    }
  }-*/;

}
