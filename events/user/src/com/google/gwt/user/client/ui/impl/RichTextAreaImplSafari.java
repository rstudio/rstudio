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

  private static int webKitVersion = getWebKitVersion();

  /**
   * WebKit v420 began suppporting full rich text editing.
   */
  private static boolean extendedEditingSupported = (webKitVersion >= 420);

  /**
   * WebKit v420 changed BackColor to HiliteColor.
   */
  private static boolean useHiliteColor = (webKitVersion >= 420);

  /**
   * WebKit version up to *and including* 420 require CSS font-size values
   * (e.g. 'medium', 'x-large') rather than size numbers. All subsequent
   * versions use size numbers like other browsers.
   */
  private static boolean oldSchoolSizeValues = (webKitVersion <= 420);

  private static native int getWebKitVersion() /*-{
    var exp = / AppleWebKit\/([\d]+)/;
    var result = exp.exec(navigator.userAgent);
    if (result) {
      var version = parseInt(result[1]);
      if (version) {
        return version;
      }
    }

    // Intentionally conservative fallback.
    return 0;
  }-*/;;

  @Override
  public Element createElement() {
    return super.createElement();
  }

  @Override
  public native boolean isBold() /*-{
    return !!this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.__gwt_isBold;
  }-*/;

  @Override
  public boolean isExtendedEditingSupported() {
    return extendedEditingSupported;
  }

  @Override
  public native boolean isItalic() /*-{
    return !!this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.__gwt_isItalic;
  }-*/;

  @Override
  public native boolean isUnderlined() /*-{
    return !!this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.__gwt_isUnderlined;
  }-*/;

  @Override
  public void setBackColor(String color) {
    if (useHiliteColor) {
      execCommand("HiliteColor", color);
    } else {
      super.setBackColor(color);
    }
  }

  @Override
  public native void setFocus(boolean focused) /*-{
    // Safari needs the *iframe* focused, not its window.
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    if (focused) {
      elem.focus();
      if (elem.__gwt_restoreSelection) {
        elem.__gwt_restoreSelection();
      }
    } else {
      elem.blur();
    }
  }-*/;

  @Override
  public void setFontSize(FontSize fontSize) {
    if (oldSchoolSizeValues) {
      // Safari2 only accepts css-style 'small, medium, large, etc' values.
      // Setting these doesn't seem to hurt Safari3.
      int number = fontSize.getNumber();
      if ((number >= 0) && (number <= 7)) {
        execCommand("FontSize", sizeNumberCSSValues[number]);
      }
    } else {
      super.setFontSize(fontSize);
    }
  }

  @Override
  protected native String getTextImpl() /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerText;
  }-*/;

  @Override
  protected native void hookEvents() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var wnd = elem.contentWindow;
    var doc = wnd.document;

    // Create an expando on the element to hold the selection state.
    elem.__gwt_selection = { baseOffset:0, extentOffset:0, baseNode:null,
      extentNode:null };

    // A function for restoring the selection state.
    elem.__gwt_restoreSelection = function() {
      var sel = elem.__gwt_selection;

      // wnd.getSelection is not defined if the iframe isn't attached.
      if (wnd.getSelection) {
        wnd.getSelection().setBaseAndExtent(sel.baseNode, sel.baseOffset,
          sel.extentNode, sel.extentOffset);
      }
    };

    // Generic event dispatcher. Also stores selection state.
    elem.__gwt_handler = function(evt) {
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
        elem.__listener.@com.google.gwt.user.client.ui.RichTextArea::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
      }
    };

    wnd.addEventListener('keydown', elem.__gwt_handler, true);
    wnd.addEventListener('keyup', elem.__gwt_handler, true);
    wnd.addEventListener('keypress', elem.__gwt_handler, true);
    wnd.addEventListener('mousedown', elem.__gwt_handler, true);
    wnd.addEventListener('mouseup', elem.__gwt_handler, true);
    wnd.addEventListener('mousemove', elem.__gwt_handler, true);
    wnd.addEventListener('mouseover', elem.__gwt_handler, true);
    wnd.addEventListener('mouseout', elem.__gwt_handler, true);
    wnd.addEventListener('click', elem.__gwt_handler, true);

    // Focus/blur event handlers. For some reason, [add|remove]eventListener()
    // doesn't work on the iframe element (at least not for focus/blur). Don't
    // dispatch through the normal handler method, as some of the querying we do
    // there interferes with focus.
    elem.onfocus = function(evt) {
      if (elem.__listener) {
        elem.__listener.@com.google.gwt.user.client.ui.RichTextArea::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
      }
    };

    elem.onblur = function(evt) {
      if (elem.__listener) {
        elem.__listener.@com.google.gwt.user.client.ui.RichTextArea::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(evt);
      }
    };
  }-*/;

  @Override
  protected native void setTextImpl(String text) /*-{
    this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerText = text;
  }-*/;

  @Override
  protected native void unhookEvents() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var wnd = elem.contentWindow;

    wnd.removeEventListener('keydown', elem.__gwt_handler, true);
    wnd.removeEventListener('keyup', elem.__gwt_handler, true);
    wnd.removeEventListener('keypress', elem.__gwt_handler, true);
    wnd.removeEventListener('mousedown', elem.__gwt_handler, true);
    wnd.removeEventListener('mouseup', elem.__gwt_handler, true);
    wnd.removeEventListener('mousemove', elem.__gwt_handler, true);
    wnd.removeEventListener('mouseover', elem.__gwt_handler, true);
    wnd.removeEventListener('mouseout', elem.__gwt_handler, true);
    wnd.removeEventListener('click', elem.__gwt_handler, true);

    elem.__gwt_restoreSelection = null;
    elem.__gwt_handler = null;

    elem.onfocus = null;
    elem.onblur = null;
  }-*/;
}
