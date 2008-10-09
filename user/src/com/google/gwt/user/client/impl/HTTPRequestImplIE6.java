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
package com.google.gwt.user.client.impl;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Internet Explorer 6 implementation of {@link HTTPRequestImpl}.
 */
class HTTPRequestImplIE6 extends HTTPRequestImpl {

  @Override
  protected native JavaScriptObject doCreateXmlHTTPRequest() /*-{
    if ($wnd.XMLHttpRequest) {
      return new XMLHttpRequest();
    } else {
      try {
        return new ActiveXObject('MSXML2.XMLHTTP.3.0');
      } catch (e) {
        return new ActiveXObject("Microsoft.XMLHTTP");
      }
    }
  }-*/;
}
