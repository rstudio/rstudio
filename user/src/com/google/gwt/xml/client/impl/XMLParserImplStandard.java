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
 * This class implements the methods for standard browsers that use the
 * DOMParser model of XML parsing.
 */
class XMLParserImplStandard extends XMLParserImpl {

  protected static native JavaScriptObject createDOMParser() /*-{
    return new DOMParser();
  }-*/;

  protected final JavaScriptObject domParser = createDOMParser();

  @Override
  protected native JavaScriptObject createDocumentImpl() /*-{
    return document.implementation.createDocument("", "", null);
  }-*/;

  @Override
  protected native JavaScriptObject getElementByIdImpl(
      JavaScriptObject document, String id) /*-{
    return document.getElementById(id);
  }-*/;

  @Override
  protected native JavaScriptObject getElementsByTagNameImpl(
      JavaScriptObject o, String tagName) /*-{
    return o.getElementsByTagNameNS("*",tagName);
  }-*/;

  @Override
  protected String getPrefixImpl(JavaScriptObject jsObject) {
    String fullName = XMLParserImpl.getNodeName(jsObject);
    if (fullName != null && fullName.indexOf(":") != -1) {
      return fullName.split(":", 2)[0];
    }
    return null;
  }

  @Override
  protected native JavaScriptObject importNodeImpl(JavaScriptObject jsObject,
      JavaScriptObject importedNode, boolean deep) /*-{
    return jsObject.importNode(importedNode, deep);
  }-*/;

  @Override
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
 
  @Override
  protected String toStringImpl(ProcessingInstructionImpl node) {
    return toStringImpl((NodeImpl) node);
  }
  
  @Override
  protected native String toStringImpl(NodeImpl node) /*-{
    var jsNode = node.@com.google.gwt.xml.client.impl.DOMItem::getJsObject()();
    return new XMLSerializer().serializeToString(jsNode);
  }-*/;
}
