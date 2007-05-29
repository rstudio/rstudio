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
import com.google.gwt.user.client.ui.RichTextArea.FontSize;

/**
 * Safari rich text platform implementation.
 */
public class RichTextAreaImplSafari extends RichTextAreaImplStandard {

  private static final String[] sizeNumberCSSValues = new String[] {
      "medium", "xx-small", "x-small", "small", "medium", "large", "x-large",
      "xx-large"};

  public Element createElement() {
    Element elem = super.createElement();

    // Use this opportunity to check if this version of Safari has full rich
    // text support or not.
    capabilityTest(elem);
    return elem;
  }

  public native boolean isBold() /*-{
    return !!this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.__gwt_isBold;
  }-*/;

  public native boolean isExtendedEditingSupported() /*-{
    // __gwt_fullSupport is set in testCapability().
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.__gwt_fullSupport;
  }-*/;

  public native boolean isItalic() /*-{
    return !!this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.__gwt_isItalic;
  }-*/;

  public native boolean isUnderlined() /*-{
    return !!this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.__gwt_isUnderlined;
  }-*/;

  public native void setFocus(boolean focused) /*-{
    // Safari needs the *iframe* focused, not its window.
    if (focused) {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.focus();
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.__gwt_restoreSelection();
    } else {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.blur();
    }
  }-*/;

  public void setFontSize(FontSize fontSize) {
    // Safari only accepts css-style 'small, medium, large, etc' values.
    int number = fontSize.getNumber();
    if ((number >= 0) && (number <= 7)) {
      execCommand("FontSize", sizeNumberCSSValues[number]);
      return;
    }
  }

  protected native String getTextImpl() /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerText;
  }-*/;

  protected native void setTextImpl(String text) /*-{
    this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerText = text;
  }-*/;

  native void initEvents() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var wnd = elem.contentWindow;
    var doc = wnd.document;

    // Create an expando on the element to hold the selection state.
    elem.__gwt_selection = { baseOffset:0, extentOffset:0, baseNode:null, extentNode:null };

    // A function for restoring the selection state.
    elem.__gwt_restoreSelection = function() {
      var sel = elem.__gwt_selection;
      wnd.getSelection().setBaseAndExtent(sel.baseNode, sel.baseOffset, sel.extentNode, sel.extentOffset);
    };

    // Generic event dispatcher. Also stores selection state.
    var handler = function(evt) {
      // Store the editor's selection state.
      var s = wnd.getSelection();
      elem.__gwt_selection = {
        baseOffset:s.baseOffset,
        extentOffset:s.extentOffset,

        baseNode:s.baseNode,
        extentNode:s.extentNode
      };

      // Hang on to bold/italic/underlined states.
      elem.__gwt_isBold = doc.queryCommandState('Bold');
      elem.__gwt_isItalic = doc.queryCommandState('Italic');
      elem.__gwt_isUnderlined = doc.queryCommandState('Underline');

      // Dispatch the event.
      if (elem.__listener) {
        elem.__listener.
        @com.google.gwt.user.client.ui.RichTextArea::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
      }
    };

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

  private native void capabilityTest(Element elem) /*-{
    elem.__gwt_fullSupport = $doc.queryCommandSupported('insertimage');
  }-*/;
}
