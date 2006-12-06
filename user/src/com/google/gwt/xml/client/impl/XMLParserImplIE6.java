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
 * This class is the IE6 implementation of the XMLParser interface.
 */
class XMLParserImplIE6 extends XMLParserImpl {

  protected native JavaScriptObject createDocumentImpl() /*-{
    var doc = new ActiveXObject("MSXML2.DOMDocument");
    doc.preserveWhiteSpace = true;
    doc.setProperty("SelectionNamespaces", "xmlns:xsl='http://www.w3.org/1999/XSL/Transform'");
    doc.setProperty("SelectionLanguage", "XPath");
    return doc;
  }-*/;

  protected native JavaScriptObject getElementByIdImpl(JavaScriptObject o,
      String elementId) /*-{
    var out = o.nodeFromID(elementId);
    return (out == null) ? null : out;
  }-*/;

  protected native JavaScriptObject getElementsByTagNameImpl(JavaScriptObject o,
      String tagName) /*-{
    return o.selectNodes(".//*[local-name()='" + tagName + "']");
  }-*/;

  protected native String getPrefixImpl(JavaScriptObject jsObject) /*-{
    return jsObject.prefix;
  }-*/;

  protected native JavaScriptObject importNodeImpl(JavaScriptObject o,
      JavaScriptObject importedNode, boolean deep) /*-{
    // IE6 does not seem to need or want nodes to be imported
    // as appends from different docs work perfectly
    // and this method is not supplied until MSXML5.0
    return importedNode;
  }-*/;
  
  protected native JavaScriptObject parseImpl(String contents) /*-{
    var doc = this.@com.google.gwt.xml.client.impl.XMLParserImplIE6::createDocumentImpl()();
    if(!doc.loadXML(contents)) {
      var err = doc.parseError;
      throw new Error("line " + err.line + ", char " + err.linepos + ":" + err.reason);
    } else {
      return doc;
    }
  }-*/;

}
