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
 * This class is the IE6 implementation of the XMLParser interface.
 */
class XMLParserImplIE6 extends XMLParserImpl {

  /**
   * Called from JSNI to select a DOM document; this is necessary due to
   * different versions of IE and Windows having different available DOM
   * implementations.
   */
  private static native JavaScriptObject selectDOMDocumentVersion() /*-{
    try { return new ActiveXObject("Msxml2.DOMDocument"); } catch (e) { }
    try { return new ActiveXObject("MSXML.DOMDocument"); } catch (e) { }
    try { return new ActiveXObject("MSXML3.DOMDocument"); } catch (e) { }
    try { return new ActiveXObject("Microsoft.XmlDom"); } catch (e) { }
    try { return new ActiveXObject("Microsoft.DOMDocument"); } catch (e) { }
  
    throw new Error("XMLParserImplIE6.createDocumentImpl: Could not find appropriate version of DOMDocument.");
  }-*/;

  @Override
  protected native JavaScriptObject createDocumentImpl() /*-{
    var doc = @com.google.gwt.xml.client.impl.XMLParserImplIE6::selectDOMDocumentVersion()();
    // preserveWhiteSpace is set to true here to prevent IE from throwing away
    // text nodes that consist of only whitespace characters. This makes it
    // act more like other browsers.
    doc.preserveWhiteSpace = true;
    doc.setProperty("SelectionNamespaces", "xmlns:xsl='http://www.w3.org/1999/XSL/Transform'");
    doc.setProperty("SelectionLanguage", "XPath");
    return doc;
  }-*/;

  @Override
  protected native JavaScriptObject getElementByIdImpl(JavaScriptObject o,
      String elementId) /*-{
    return o.nodeFromID(elementId);
  }-*/;

  @Override
  protected native JavaScriptObject getElementsByTagNameImpl(JavaScriptObject o,
      String tagName) /*-{
    return o.selectNodes(".//*[local-name()='" + tagName + "']");
  }-*/;

  @Override
  protected native String getPrefixImpl(JavaScriptObject jsObject) /*-{
    return jsObject.prefix;
  }-*/;

  @Override
  protected native JavaScriptObject importNodeImpl(JavaScriptObject o,
      JavaScriptObject importedNode, boolean deep) /*-{
    // IE6 does not seem to need or want nodes to be imported
    // as appends from different docs work perfectly
    // and this method is not supplied until MSXML5.0
    return importedNode;
  }-*/;
  
  @Override
  protected native JavaScriptObject parseImpl(String contents) /*-{
    var doc = this.@com.google.gwt.xml.client.impl.XMLParserImplIE6::createDocumentImpl()();
    if(!doc.loadXML(contents)) {
      var err = doc.parseError;
      throw new Error("line " + err.line + ", char " + err.linepos + ":" + err.reason);
    } else {
      return doc;
    }
  }-*/;

  @Override
  protected String toStringImpl(ProcessingInstructionImpl node) {
    return toStringImpl((NodeImpl) node);
  }
  
  @Override
  protected native String toStringImpl(NodeImpl node) /*-{
    var jsNode = node.@com.google.gwt.xml.client.impl.DOMItem::getJsObject()();
    return jsNode.xml;
  }-*/;
}
