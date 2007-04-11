/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.RichTextArea;

/**
 * Base class for RichText platform implementations. The default version
 * simply creates a text area with no rich text support.
 */
public class RichTextAreaImpl {

  protected Element elem;

  public RichTextAreaImpl() {
    elem = createElement();
  }

  public Element getElement() {
    return elem;
  }

  public String getHTML() {
    return DOM.getAttribute(elem, "value");
  }

  public String getText() {
    return DOM.getAttribute(elem, "value");
  }

  public void hookEvents(RichTextArea owner) {
    DOM.setEventListener(elem, owner);
  }

  public void initElement() {
    DOM.sinkEvents(elem, Event.MOUSEEVENTS | Event.KEYEVENTS | Event.ONCHANGE
        | Event.ONCLICK);
  }

  public boolean isBasicEditingSupported() {
    return false;
  }

  public boolean isExtendedEditingSupported() {
    return false;
  }

  public native void setFocus(boolean focused) /*-{
    if (focused) {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.focus();
    } else {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.blur();
    } 
  }-*/;

  public void setHTML(String html) {
    DOM.setAttribute(elem, "value", html);
  }

  public void setText(String text) {
    DOM.setAttribute(elem, "value", text);
  }

  public void unhookEvents(RichTextArea owner) {
    DOM.setEventListener(elem, null);
  }

  protected Element createElement() {
    return DOM.createTextArea();
  }
}
