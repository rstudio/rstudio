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
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.ProcessingInstruction;

/**
 * This class implements the XML DOM ProcessingInstruction interface.
 */
class ProcessingInstructionImpl extends NodeImpl implements
    ProcessingInstruction {

  protected ProcessingInstructionImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This function delegates to the native method <code>getData</code> in
   * XMLParserImpl.
   */
  public String getData() {
    return XMLParserImpl.getData(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>getTarget</code> in
   * XMLParserImpl.
   */
  public String getTarget() {
    return XMLParserImpl.getTarget(this.getJsObject());
  }

  /**
   * This function delegates to the native method <code>setData</code> in
   * XMLParserImpl.
   */
  public void setData(String data) {
    try {
      XMLParserImpl.setData(this.getJsObject(), data);
    } catch (JavaScriptException e) {
      throw new DOMNodeException(DOMException.INVALID_CHARACTER_ERR, e, this);
    }
  }
  
  @Override
  public String toString() {
    return XMLParserImpl.getInstance().toStringImpl(this);
  }
}
