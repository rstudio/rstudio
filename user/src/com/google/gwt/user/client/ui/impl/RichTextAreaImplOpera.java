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
import com.google.gwt.user.client.ui.RichTextArea;

/**
 * Opera implementation of rich-text editing.
 */
public class RichTextAreaImplOpera extends RichTextAreaImplStandard {

  private static boolean supportsEditing;

  public Element createElement() {
    supportsEditing = detectEditingSupport();
    if (supportsEditing) {
      return super.createElement();
    }
    return DOM.createTextArea();
  }

  public String getHTML() {
    if (supportsEditing) {
      return super.getHTML();
    }

    // Unsupported fallback.
    return ((RichTextAreaImpl) this).getHTML();
  }

  public String getText() {
    if (supportsEditing) {
      return super.getText();
    }

    // Unsupported fallback.
    return ((RichTextAreaImpl) this).getText();
  }

  public void hookEvents(RichTextArea owner) {
    if (supportsEditing) {
      super.hookEvents(owner);
      return;
    }

    // Unsupported fallback.
    ((RichTextAreaImpl) this).hookEvents(owner);
  }

  public void initElement() {
    if (supportsEditing) {
      super.initElement();
      return;
    }

    // Unsupported fallback.
    ((RichTextAreaImpl) this).initElement();
  }

  public boolean isBasicEditingSupported() {
    return supportsEditing;
  }

  public boolean isFullEditingSupported() {
    return supportsEditing;
  }

  public void setBackColor(String color) {
    // Opera uses 'BackColor' for the *entire area's* background. 'HiliteColor'
    // does what we actually want.
    execCommand("HiliteColor", color);
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
    if (supportsEditing) {
      super.setHTML(html);
    }

    // Unsupported fallback.
    DOM.setAttribute(elem, "value", html);
  }

  public void setText(String text) {
    if (supportsEditing) {
      super.setText(text);
    }

    // Unsupported fallback.
    DOM.setAttribute(elem, "value", text);
  }

  public void unhookEvents(RichTextArea owner) {
    if (supportsEditing) {
      super.unhookEvents(owner);
      return;
    }

    // Unsupported fallback.
    ((RichTextAreaImpl) this).unhookEvents(owner);
  }

  void execCommand(String cmd, String param) {
    if (isRichEditingActive(elem)) {
      // Opera doesn't need focus for execCommand() to work, but focusing
      // the editor causes it to lose its selection, so we focus *after*
      // execCommand().
      execCommandAssumingFocus(cmd, param);
      setFocus(true);

      // TODO: Opera has now lost its selection. Figure out a way to restore it
      // reliably.
    }
  }

  boolean queryCommandState(String cmd) {
    // Opera doesn't need focus (and setting it dumps selection).
    return queryCommandStateAssumingFocus(cmd);
  }

  String queryCommandValue(String cmd) {
    // Opera doesn't need focus (and setting it dumps selection).
    return queryCommandValueAssumingFocus(cmd);
  }

  private native boolean detectEditingSupport() /*-{
    return !!$doc.designMode;
  }-*/;
}
