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
package com.google.gwt.xml.client.impl;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class implements the methods for standard browsers that use the
 * DOMParser model of XML parsing.
 */
class XMLParserImplStandard extends XMLParserImpl {

  protected static native JavaScriptObject createDOMParser() /*-{
    return new DOMParser();
  }-*/;

  protected final JavaScriptObject domParser = createDOMParser();

  protected native JavaScriptObject createDocumentImpl() /*-{
    return document.implementation.createDocument("", "", null);
  }-*/;

  protected native JavaScriptObject getElementByIdImpl(
      JavaScriptObject document, String id) /*-{
    return document.getElementById(id);
  }-*/;

  protected native JavaScriptObject getElementsByTagNameImpl(
      JavaScriptObject o, String tagName) /*-{
    return o.getElementsByTagNameNS("*",tagName);
  }-*/;

  protected String getPrefixImpl(JavaScriptObject jsObject) {
    String fullName = XMLParserImpl.getNodeName(jsObject);
    if (fullName != null && fullName.indexOf(":") != -1) {
      return fullName.split(":", 2)[0];
    }
    return null;
  }

  protected native JavaScriptObject importNodeImpl(JavaScriptObject jsObject,
      JavaScriptObject importedNode, boolean deep) /*-{
    var out = jsObject.importNode(importedNode, deep);
    return (out == null) ? null : out;
  }-*/;

  protected native JavaScriptObject parseImpl(String contents) /*-{
    var domParser = this.@com.google.gwt.xml.client.impl.XMLParserImplStandard::domParser;
    var result = domParser.parseFromString(contents,"text/xml");
    var roottag = result.documentElement;
    if ((roottag.tagName == "parsererror") && 
        (roottag.namespaceURI ==
        "http://www.mozilla.org/newlayout/xml/parsererror.xml")) {
      throw new Error(roottag.firstChild.data);
    }
    return result;
  }-*/;

}
