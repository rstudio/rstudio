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
import com.google.gwt.user.client.ui.RichTextArea.FontSize;
import com.google.gwt.user.client.ui.RichTextArea.Justification;

/**
 * Basic rich text platform implementation.
 */
public class RichTextAreaImplStandard extends RichTextAreaImpl implements
    RichTextArea.BasicFormatter, RichTextArea.ExtendedFormatter {

  /**
   * Holds a cached copy of any user setHTML/setText actions until the real
   * text area is fully initialized.  Becomes <code>null</code> after init.
   */
  private Element beforeInitPlaceholder = DOM.createDiv();

  public native Element createElement() /*-{
    return $doc.createElement('iframe');
  }-*/;

  public void createLink(String url) {
    execCommand("CreateLink", url);
  }

  public String getBackColor() {
    return queryCommandValue("BackColor");
  }

  public String getForeColor() {
    return queryCommandValue("ForeColor");
  }

  public final String getHTML() {
    return beforeInitPlaceholder == null ? getHTMLImpl() : DOM.getInnerHTML(beforeInitPlaceholder);
  }

  public final String getText() {
    return beforeInitPlaceholder == null ? getTextImpl() : DOM.getInnerText(beforeInitPlaceholder);
  }

  public native void hookEvents(RichTextArea owner) /*-{
    this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.__listener = owner;
  }-*/;

  public native void initElement() /*-{
    // Some browsers don't like setting designMode until slightly _after_
    // the iframe becomes attached to the DOM. Any non-zero timeout will do
    // just fine.
    var _this = this;
    setTimeout(function() {
      // Turn on design mode.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.designMode = 'On';

      // Initialize event handling.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::initEvents()();
      
      // Send notification that the iframe has reached design mode.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
    }, 1);
  }-*/;

  public void insertHorizontalRule() {
    execCommand("InsertHorizontalRule", null);
  }

  public void insertImage(String url) {
    execCommand("InsertImage", url);
  }

  public void insertOrderedList() {
    execCommand("InsertOrderedList", null);
  }

  public void insertUnorderedList() {
    execCommand("InsertUnorderedList", null);
  }

  public boolean isBasicEditingSupported() {
    return true;
  }

  public boolean isBold() {
    return queryCommandState("Bold");
  }

  public boolean isExtendedEditingSupported() {
    return true;
  }

  public boolean isItalic() {
    return queryCommandState("Italic");
  }

  public boolean isStrikethrough() {
    return queryCommandState("Strikethrough");
  }

  public boolean isSubscript() {
    return queryCommandState("Subscript");
  }

  public boolean isSuperscript() {
    return queryCommandState("Superscript");
  }

  public boolean isUnderlined() {
    return queryCommandState("Underline");
  }

  public void leftIndent() {
    execCommand("Outdent", null);
  }

  public void removeFormat() {
    execCommand("RemoveFormat", null);
  }

  public void removeLink() {
    execCommand("Unlink", "false");
  }

  public void rightIndent() {
    execCommand("Indent", null);
  }

  public void selectAll() {
    execCommand("SelectAll", null);
  }

  public void setBackColor(String color) {
    execCommand("BackColor", color);
  }

  public native void setFocus(boolean focused) /*-{
    if (focused) {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.focus();
    } else {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.blur();
    } 
  }-*/;

  public void setFontName(String name) {
    execCommand("FontName", name);
  }

  public void setFontSize(FontSize fontSize) {
    execCommand("FontSize", Integer.toString(fontSize.getNumber()));
  }

  public void setForeColor(String color) {
    execCommand("ForeColor", color);
  }

  public final void setHTML(String html) {
    if (beforeInitPlaceholder == null) {
      setHTMLImpl(html);
    } else {
      DOM.setInnerHTML(beforeInitPlaceholder, html);
    }
  }

  public void setJustification(Justification justification) {
    if (justification == Justification.CENTER) {
      execCommand("JustifyCenter", null);
    } else if (justification == Justification.LEFT) {
      execCommand("JustifyLeft", null);
    } else if (justification == Justification.RIGHT) {
      execCommand("JustifyRight", null);
    }
  }

  public final void setText(String text) {
    if (beforeInitPlaceholder == null) {
      setTextImpl(text);
    } else {
      DOM.setInnerText(beforeInitPlaceholder, text);
    }
  }

  public void toggleBold() {
    execCommand("Bold", "false");
  }

  public void toggleItalic() {
    execCommand("Italic", "false");
  }

  public void toggleStrikethrough() {
    execCommand("Strikethrough", "false");
  }

  public void toggleSubscript() {
    execCommand("Subscript", "false");
  }

  public void toggleSuperscript() {
    execCommand("Superscript", "false");
  }

  public void toggleUnderline() {
    execCommand("Underline", "False");
  }

  protected native String getHTMLImpl() /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerHTML;
  }-*/;

  protected native String getTextImpl() /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.textContent;
  }-*/;

  protected native void setHTMLImpl(String html) /*-{
    this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerHTML = html;
  }-*/;

  protected native void setTextImpl(String text) /*-{
    this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.textContent = text;
  }-*/;

  void execCommand(String cmd, String param) {
    if (isRichEditingActive(elem)) {
      // When executing a command, focus the iframe first, since some commands
      // don't take properly when it's not focused.
      setFocus(true);
      execCommandAssumingFocus(cmd, param);
    }
  }

  native void execCommandAssumingFocus(String cmd, String param) /*-{
    this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.execCommand(cmd, false, param);
  }-*/;

  native void initEvents() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var handler = function(evt) {
      if (elem.__listener) {
        elem.__listener.@com.google.gwt.user.client.ui.RichTextArea::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
      }
    };

    var wnd = elem.contentWindow;
    wnd.addEventListener('keydown', handler, true);
    wnd.addEventListener('keyup', handler, true);
    wnd.addEventListener('keypress', handler, true);
    wnd.addEventListener('mousedown', handler, true);
    wnd.addEventListener('mouseup', handler, true);
    wnd.addEventListener('mousemove', handler, true);
    wnd.addEventListener('mouseover', handler, true);
    wnd.addEventListener('mouseout', handler, true);
    wnd.addEventListener('click', handler, true);
  }-*/;

  native boolean isRichEditingActive(Element e) /*-{
    return ((e.contentWindow.document.designMode).toUpperCase()) == 'ON';
  }-*/;

  void onElementInitialized() {
    // When the iframe is ready, ensure cached content is set.
    setHTMLImpl(DOM.getInnerHTML(beforeInitPlaceholder));
    beforeInitPlaceholder = null;
  }

  boolean queryCommandState(String cmd) {
    if (isRichEditingActive(elem)) {
      // When executing a command, focus the iframe first, since some commands
      // don't take properly when it's not focused.
      setFocus(true);
      return queryCommandStateAssumingFocus(cmd);
    } else {
      return false;
    }
  }

  native boolean queryCommandStateAssumingFocus(String cmd) /*-{
    return !!this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.queryCommandState(cmd);
  }-*/;

  String queryCommandValue(String cmd) {
    // When executing a command, focus the iframe first, since some commands
    // don't take properly when it's not focused.
    setFocus(true);
    return queryCommandValueAssumingFocus(cmd);
  }

  native String queryCommandValueAssumingFocus(String cmd) /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.queryCommandValue(cmd);
  }-*/;
}
