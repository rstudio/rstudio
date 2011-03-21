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
package com.google.gwt.xml.client.impl;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class is Safari implementation of the XMLParser interface.
 */
public class XMLParserImplSafari extends XMLParserImplStandard {

  private static boolean safari2LevelWebKit = (getWebKitVersion() <= 420);

  public static boolean isSafari2LevelWebKit() {
    return safari2LevelWebKit;
  }
  
  private static native int getWebKitVersion() /*-{
    var result = / AppleWebKit\/([\d]+)/.exec(navigator.userAgent);
    return ((result) ? parseInt(result[1]) : 0) || 0;
  }-*/;
  
  private static void throwDOMParseException(String message) {
    throw new DOMParseException(message);
  }
  
  @Override
  protected native JavaScriptObject getElementsByTagNameImpl(JavaScriptObject o,
      String tagName) /*-{
    return o.getElementsByTagName(tagName);
  }-*/;
  
  @Override
  protected native JavaScriptObject importNodeImpl(JavaScriptObject jsObject,
      JavaScriptObject importedNode, boolean deep) /*-{
    // Works around a Safari2 issue where importing a node will steal attributes
    // from the original.
    if (@com.google.gwt.xml.client.impl.XMLParserImplSafari::isSafari2LevelWebKit()()) {
      importedNode = importedNode.cloneNode(deep);
    }
    return jsObject.importNode(importedNode, deep);
  }-*/;
  
  /**
   * <html><body><parsererror style="white-space: pre; border: 2px solid #c77;
   * padding: 0 1em 0 1em; margin: 1em; background-color: #fdd; color: black" >
   * <h3>This page contains the following errors:</h3>
   * <div style="font-family:monospace;font-size:12px" >error on line 1 at
   * column 2: xmlParseStartTag: invalid element name </div>
   * <h3>Below is a rendering of the page up to the first error.</h3>
   * </parsererror></body></html> is all you get from Safari. Hope that nobody
   * wants to send one of those error reports over the wire to be parsed by
   * safari...
   * 
   * @param contents contents
   * @return parsed JavaScript object
   * @see com.google.gwt.xml.client.impl.XMLParserImpl#parseImpl(java.lang.String)
   */
  @Override
  protected native JavaScriptObject parseImpl(String contents) /*-{
    var domParser =
      this.@com.google.gwt.xml.client.impl.XMLParserImplStandard::domParser;
    var result = domParser.parseFromString(contents,"text/xml");
    var parseerrors = result.getElementsByTagName("parsererror");
    if (parseerrors.length > 0) {
      var err = parseerrors.item(0);
      if (err.parentNode.tagName == 'body') {
        @com.google.gwt.xml.client.impl.XMLParserImplSafari::throwDOMParseException(Ljava/lang/String;)(err.childNodes[1].innerHTML);
      }
    } 
    return result;
  }-*/;
}
