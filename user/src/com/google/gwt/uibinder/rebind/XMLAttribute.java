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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.uibinder.parsers.FieldReferenceConverter;

import org.w3c.dom.Attr;

/**
 * Like {@link XMLElement}, a wrapper around {@link Attr} to keep
 * parser writers out of trouble.
 */
public class XMLAttribute {
  private XMLElement xmlElem;
  private Attr w3cAttr;

  XMLAttribute(XMLElement element, Attr attr) {
    this.xmlElem = element;
    this.w3cAttr = attr;
  }

  public String consumeRawValue() {
    return xmlElem.consumeRawAttribute(w3cAttr.getName());
  }
  
  public String getLocalName() {
    return w3cAttr.getLocalName();
  }

  public String getName() {
    return w3cAttr.getName();
  }

  public String getNamespaceUri() {
    return w3cAttr.getNamespaceURI();
  }

  public boolean hasComputedValue() {
    return FieldReferenceConverter.hasFieldReferences(w3cAttr.getValue());
  }

  public boolean hasToken() {
    return Tokenator.hasToken(w3cAttr.getValue());
  }

  public boolean isConsumed() {
    return !xmlElem.hasAttribute(w3cAttr.getName());
  }

  @Override
  public String toString() {
    return w3cAttr.getName() + "='" + w3cAttr.getValue() + "'";
  }
}
