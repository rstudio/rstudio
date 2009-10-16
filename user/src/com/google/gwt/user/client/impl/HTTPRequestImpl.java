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
package com.google.gwt.user.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ResponseTextHandler;

/**
 * Native implementation associated with
 * {@link com.google.gwt.user.client.HTTPRequest}.
 */
public class HTTPRequestImpl {

  static JavaScriptObject nullFunc;
  
  public HTTPRequestImpl() {
    // TODO - consider moving this to clinit or creating it on the fly rather
    // than assigning it to a static
    nullFunc = JavaScriptObject.createFunction();
  }
  
  public boolean asyncGet(String url, ResponseTextHandler handler) {
    return asyncGet(null, null, url, handler);
  }

  public boolean asyncGet(String user, String pwd, String url,
      ResponseTextHandler handler) {
    return asyncGetImpl(user, pwd, url, handler);
  }

  public boolean asyncPost(String url, String postData,
      ResponseTextHandler handler) {
    return asyncPost(null, null, url, postData, handler);
  }

  public boolean asyncPost(String user, String pwd, String url,
      String postData, ResponseTextHandler handler) {
    return asyncPostImpl(user, pwd, url, postData, handler);
  }

  public JavaScriptObject createXmlHTTPRequest() {
    return doCreateXmlHTTPRequest();
  }

  /**
   * All the supported browsers except for IE instantiate it as shown.
   */
  protected native JavaScriptObject doCreateXmlHTTPRequest() /*-{
    return new XMLHttpRequest();
  }-*/;

  private native boolean asyncGetImpl(String user, String pwd, String url,
      ResponseTextHandler handler) /*-{
    var xmlHttp = this.@com.google.gwt.user.client.impl.HTTPRequestImpl::doCreateXmlHTTPRequest()();
    try {
      xmlHttp.open("GET", url, true);
      xmlHttp.setRequestHeader("Content-Type", "text/plain; charset=utf-8");
      xmlHttp.onreadystatechange = $entry(function() {
        if (xmlHttp.readyState == 4) {
          $wnd.setTimeout($entry(function() {
            xmlHttp.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
          }), 0);
          handler.@com.google.gwt.user.client.ResponseTextHandler::onCompletion(Ljava/lang/String;)(xmlHttp.responseText || "");
        }
      });
      xmlHttp.send('');
      return true;
    } catch (e) {
      xmlHttp.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
      return false;
    }
  }-*/;

  private native boolean asyncPostImpl(String user, String pwd, String url,
      String postData, ResponseTextHandler handler) /*-{
    var xmlHttp = this.@com.google.gwt.user.client.impl.HTTPRequestImpl::doCreateXmlHTTPRequest()();
    try {
      xmlHttp.open("POST", url, true);
      xmlHttp.setRequestHeader("Content-Type", "text/plain; charset=utf-8");
      xmlHttp.onreadystatechange = $entry(function() {
        if (xmlHttp.readyState == 4) {
          $wnd.setTimeout($entry(function() {
            xmlHttp.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
          }), 0);
          handler.@com.google.gwt.user.client.ResponseTextHandler::onCompletion(Ljava/lang/String;)(xmlHttp.responseText || "");
        }
      });
      xmlHttp.send(postData);
      return true;
    }
    catch (e) {
      xmlHttp.onreadystatechange = @com.google.gwt.user.client.impl.HTTPRequestImpl::nullFunc;
      return false;
    }
  }-*/;
}
