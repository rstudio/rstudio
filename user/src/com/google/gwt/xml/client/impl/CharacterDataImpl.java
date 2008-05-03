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

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.CharacterData;
import com.google.gwt.xml.client.DOMException;

/**
 * This class implements the CharacterData interface.
 */
abstract class CharacterDataImpl extends NodeImpl implements
    CharacterData {

  protected CharacterDataImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This function delegates to the native method <code>appendData</code> in
   * XMLParserImpl.
   */
  public void appendData(String arg) {
    try {
      XMLParserImpl.appendData(this.getJsObject(), arg);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>deleteData</code> in
   * XMLParserImpl.
   */
  public void deleteData(int offset, int count) {
    try {
      XMLParserImpl.deleteData(this.getJsObject(), offset, count);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>getData</code> in
   * XMLParserImpl.
   */
  public String getData() {
    return XMLParserImpl.getData(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>getLength</code> in
   * XMLParserImpl.
   */
  public int getLength() {
    return XMLParserImpl.getLength(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>insertData</code> in
   * XMLParserImpl.
   */
  public void insertData(int offset, String arg) {
    try {
      XMLParserImpl.insertData(this.getJsObject(), offset, arg);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>replaceData</code> in
   * XMLParserImpl.
   */
  public void replaceData(int offset, int count, String arg) {
    try {
      XMLParserImpl.replaceData(this.getJsObject(), offset, count, arg);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>setData</code> in
   * XMLParserImpl.
   */
  public void setData(String data) {
    try {
      XMLParserImpl.setData(this.getJsObject(), data);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_MODIFICATION_ERR, e, this);
    }
  }

  /**
   * This function delegates to the native method <code>substringData</code>
   * in XMLParserImpl.
   */
  public String substringData(final int offset, final int count) {
    try {
      return XMLParserImpl.substringData(this.getJsObject(), offset, count);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_ACCESS_ERR, e, this);
    }
  }
}
