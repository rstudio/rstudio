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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.Document;

/**
 * Native implementation associated with
 * {@link com.google.gwt.xml.client.XMLParser}.
 */
public abstract class XMLParserImpl {

  private static XMLParserImpl impl = GWT.create(XMLParserImpl.class);

  public static XMLParserImpl getInstance() {
    return impl;
  }

  static native JavaScriptObject appendChild(JavaScriptObject jsObject,
      JavaScriptObject newChildJs) /*-{
    return jsObject.appendChild(newChildJs);
  }-*/;

  static native void appendData(JavaScriptObject jsObject, String arg) /*-{
    jsObject.appendData(arg);
  }-*/;

  static native JavaScriptObject cloneNode(JavaScriptObject jsObject,
      boolean deep) /*-{
    return jsObject.cloneNode(deep);
  }-*/;

  static native JavaScriptObject createCDATASection(JavaScriptObject jsObject,
      String data) /*-{
    return jsObject.createCDATASection(data);
  }-*/;

  static native JavaScriptObject createComment(JavaScriptObject jsObject,
      String data) /*-{
    return jsObject.createComment(data);
  }-*/;

  static native JavaScriptObject createDocumentFragment(
      JavaScriptObject jsObject) /*-{
    return jsObject.createDocumentFragment();
  }-*/;

  static native JavaScriptObject createElement(JavaScriptObject jsObject,
      String tagName) /*-{
    return jsObject.createElement(tagName);
  }-*/;

  static native JavaScriptObject createProcessingInstruction(
      JavaScriptObject jsObject, String target, String data) /*-{
    return jsObject.createProcessingInstruction(target, data);
  }-*/;

  static native JavaScriptObject createTextNode(JavaScriptObject jsObject,
      String data) /*-{
    return jsObject.createTextNode(data);
  }-*/;

  static native void deleteData(JavaScriptObject jsObject, int offset, int count) /*-{
    jsObject.deleteData(offset, count);
  }-*/;

  static native String getAttribute(JavaScriptObject o, String name) /*-{
    return o.getAttribute(name);
  }-*/;

  static native JavaScriptObject getAttributeNode(JavaScriptObject o,
      String name) /*-{
    return o.getAttributeNode(name);
  }-*/;

  static native JavaScriptObject getAttributes(JavaScriptObject t) /*-{
    return t.attributes;
  }-*/;

  static native JavaScriptObject getChildNodes(JavaScriptObject t) /*-{
    return t.childNodes;
  }-*/;

  static native String getData(JavaScriptObject o) /*-{
    return o.data;
  }-*/;

  static native JavaScriptObject getDocumentElement(JavaScriptObject o) /*-{
    return o.documentElement;
  }-*/;

  static JavaScriptObject getElementById(JavaScriptObject document, String id) {
     return impl.getElementByIdImpl(document, id);
   }

  static JavaScriptObject getElementsByTagName(JavaScriptObject o,
      String tagName) {
    return impl.getElementsByTagNameImpl(o, tagName);
  }

  static native int getLength(JavaScriptObject o) /*-{
    return o.length;
  }-*/;

  static native String getName(JavaScriptObject o) /*-{
    return o.name;
  }-*/;

  static native JavaScriptObject getNamedItem(JavaScriptObject t, String name) /*-{
    return t.getNamedItem(name);
  }-*/;

  static native String getNamespaceURI(JavaScriptObject jsObject) /*-{
    return jsObject.namespaceURI;
  }-*/;

  static native JavaScriptObject getNextSibling(JavaScriptObject o) /*-{
    return o.nextSibling;
  }-*/;

  static native String getNodeName(JavaScriptObject o) /*-{
    return o.nodeName;
  }-*/;

  static native short getNodeType(JavaScriptObject jsObject) /*-{
    var out = jsObject.nodeType;
    return (out == null) ? -1 : out;
  }-*/;

  static native String getNodeValue(JavaScriptObject o) /*-{
    return o.nodeValue;
  }-*/;

  static native JavaScriptObject getOwnerDocument(JavaScriptObject o) /*-{
    return o.ownerDocument;
  }-*/;

  static native JavaScriptObject getParentNode(JavaScriptObject o) /*-{
    return o.parentNode;
  }-*/;

  static String getPrefix(JavaScriptObject jsObject) {
    return impl.getPrefixImpl(jsObject);
  }

  static native JavaScriptObject getPreviousSibling(JavaScriptObject o) /*-{
    return o.previousSibling;
  }-*/;

  static native boolean getSpecified(JavaScriptObject o) /*-{
    return o.specified;
  }-*/;

  static native String getTagName(JavaScriptObject o) /*-{
    return o.tagName;
  }-*/;

  static native String getTarget(JavaScriptObject o) /*-{
    return o.target;
  }-*/;

  static native String getValue(JavaScriptObject o) /*-{
    return o.value;
  }-*/;

  static native boolean hasAttributes(JavaScriptObject jsObject) /*-{
    return jsObject.attributes.length != 0;
  }-*/;

  static native boolean hasChildNodes(JavaScriptObject jsObject) /*-{
    return jsObject.hasChildNodes();
  }-*/;

  static JavaScriptObject importNode(JavaScriptObject jsObject,
      JavaScriptObject importedNode, boolean deep) {
    return impl.importNodeImpl(jsObject, importedNode, deep);
  }

  static native JavaScriptObject insertBefore(JavaScriptObject jsObject,
      JavaScriptObject newChildJs, JavaScriptObject refChildJs) /*-{
    return jsObject.insertBefore(newChildJs, refChildJs);
  }-*/;

  static native void insertData(JavaScriptObject jsObject, int offset,
      String arg) /*-{
    jsObject.insertData(offset, arg);
  }-*/;

  static native JavaScriptObject item(JavaScriptObject t, int index) /*-{
    if (index >= t.length) {
      return null;
    }
    return t.item(index);   
  }-*/;

  static native void normalize(JavaScriptObject jsObject) /*-{
    jsObject.normalize();
  }-*/;

  static native void removeAttribute(JavaScriptObject jsObject, String name) /*-{
    jsObject.removeAttribute(name);
  }-*/;

  static native JavaScriptObject removeChild(JavaScriptObject jsObject,
      JavaScriptObject oldChildJs) /*-{
    return jsObject.removeChild(oldChildJs);
  }-*/;

  static native JavaScriptObject removeNamedItem(JavaScriptObject jsObject,
      String name) /*-{
    return jsObject.removeNamedItem(name);
  }-*/;

  static native JavaScriptObject replaceChild(JavaScriptObject jsObject,
      JavaScriptObject newChildJs, JavaScriptObject oldChildJs) /*-{
    return jsObject.replaceChild(newChildJs, oldChildJs);
  }-*/;

  static native void replaceData(JavaScriptObject jsObject, int offset,
      int count, String arg) /*-{
    jsObject.replaceData(offset, count, arg);
  }-*/;

  static native void setAttribute(JavaScriptObject jsObject, String name,
      String value) /*-{
    jsObject.setAttribute(name, value);
  }-*/;

  static native void setData(JavaScriptObject jsObject, String data) /*-{
    jsObject.data = data;
  }-*/;

  static native JavaScriptObject setNamedItem(JavaScriptObject jsObject,
      JavaScriptObject arg) /*-{
    return jsObject.setNamedItem(arg);
  }-*/;

  static native void setNodeValue(JavaScriptObject jsObject, String nodeValue) /*-{
    jsObject.nodeValue = nodeValue;
  }-*/;

  static native JavaScriptObject splitText(JavaScriptObject jsObject, int offset) /*-{
    return jsObject.splitText(offset);
  }-*/;

  static native String substringData(JavaScriptObject o, int offset, int count) /*-{
    return o.substringData(offset, count);
  }-*/;

  /**
   * Not globally instantable.
   */
  XMLParserImpl() {
  }

  public final Document createDocument() {
    return (Document) NodeImpl.build(createDocumentImpl());
  }

  public final Document parse(String contents) {
    try {
      return (Document) NodeImpl.build(parseImpl(contents));
    } catch (JavaScriptException e) {
      throw new DOMParseException(contents, e);
    }
  }

  public boolean supportsCDATASection() {
    return true;
  }

  protected abstract JavaScriptObject createDocumentImpl();

  protected abstract JavaScriptObject getElementByIdImpl(
      JavaScriptObject document, String id);

  protected abstract JavaScriptObject getElementsByTagNameImpl(
      JavaScriptObject o, String tagName);

  protected abstract String getPrefixImpl(JavaScriptObject jsObject);

  protected abstract JavaScriptObject importNodeImpl(JavaScriptObject jsObject,
      JavaScriptObject importedNode, boolean deep);

  protected abstract JavaScriptObject parseImpl(String contents);
  
  abstract String toStringImpl(ProcessingInstructionImpl node);
  
  abstract String toStringImpl(NodeImpl node);
}
