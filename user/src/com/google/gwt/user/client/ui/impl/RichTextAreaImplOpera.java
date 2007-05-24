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

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RichTextArea;

/**
 * Opera implementation of rich-text editing.
 */
public class RichTextAreaImplOpera extends RichTextAreaImplStandard {

  private static boolean richTextSupported = detectEditingSupport();
  private static RichTextAreaImpl old;

  static {
    // If rich text is not supported by this version of Opera, create an
    // instance of the default impl and shunt all calls to it.
    if (!richTextSupported) {
      old = new RichTextAreaImpl();
    }
  }

  private static native boolean detectEditingSupport() /*-{
    return !!$doc.designMode;
  }-*/;

  public Element createElement() {
    if (old != null) {
      return old.createElement();
    }
    return super.createElement();
  }

  public Element getElement() {
    if (old != null) {
      return old.getElement();
    }
    return super.getElement();
  }

  public String getHTML() {
    if (old != null) {
      return old.getHTML();
    }
    return super.getHTML();
  }

  public String getText() {
    if (old != null) {
      return old.getText();
    }
    return super.getText();
  }

  public void hookEvents(RichTextArea owner) {
    if (old != null) {
      old.hookEvents(owner);
      return;
    }
    super.hookEvents(owner);
  }

  public void initElement() {
    if (old != null) {
      old.initElement();
      return;
    }
    super.initElement();
  }

  public boolean isBasicEditingSupported() {
    return richTextSupported;
  }

  public boolean isExtendedEditingSupported() {
    return richTextSupported;
  }

  public native void setFocus(boolean focused) /*-{
    // Opera needs the *iframe* focused, not its window.
    if (focused) {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.focus();
    } else {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.blur();
    }
  }-*/;

  public void setHTML(String html) {
    if (old != null) {
      old.setHTML(html);
      return;
    }
    super.setHTML(html);
  }

  public void setText(String text) {
    if (old != null) {
      old.setText(text);
      return;
    }
    super.setText(text);
  }

  public void unhookEvents() {
    if (old != null) {
      old.unhookEvents();
    }
    super.unhookEvents();
  }

  public void setBackColor(String color) {
    // Opera uses 'BackColor' for the *entire area's* background. 'HiliteColor'
    // does what we actually want.
    execCommand("HiliteColor", color);
  }
}
