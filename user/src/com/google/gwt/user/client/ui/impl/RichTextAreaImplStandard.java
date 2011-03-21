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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.RichTextArea.FontSize;
import com.google.gwt.user.client.ui.RichTextArea.Justification;

/**
 * Basic rich text platform implementation.
 */
public abstract class RichTextAreaImplStandard extends RichTextAreaImpl implements
    RichTextArea.Formatter {

  /**
   * The message displayed when the formatter is used before the RichTextArea
   * is initialized.
   */
  private static final String INACTIVE_MESSAGE = "RichTextArea formatters " +
      "cannot be used until the RichTextArea is attached and focused.";

  /**
   * Holds a cached copy of any user setHTML/setText/setEnabled actions until
   * the real text area is fully initialized.  Becomes <code>null</code> after
   * init.
   */
  private Element beforeInitPlaceholder = DOM.createDiv();

  /**
   * Set to true when the {@link RichTextArea} is attached to the page and
   * {@link #initElement()} is called.  If the {@link RichTextArea} is detached
   * before {@link #onElementInitialized()} is called, this will be set to
   * false.  See issue 1897 for details.
   */
  protected boolean initializing;

  /**
   * Indicates that the text area should be focused as soon as it is loaded.
   */
  private boolean isPendingFocus;

  /**
   * True when the element has been attached.
   */
  private boolean isReady;

  @Override
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

  @Override
  public final String getHTML() {
    return beforeInitPlaceholder == null ? getHTMLImpl() : DOM.getInnerHTML(beforeInitPlaceholder);
  }

  @Override
  public final String getText() {
    return beforeInitPlaceholder == null ? getTextImpl() : DOM.getInnerText(beforeInitPlaceholder);
  }

  @Override
  public native void initElement()  /*-{
    // Most browsers don't like setting designMode until slightly _after_
    // the iframe becomes attached to the DOM. Any non-zero timeout will do
    // just fine.
    var _this = this;
    _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitializing()();
    setTimeout($entry(function() {
      // Turn on design mode.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.designMode = 'On';

      // Send notification that the iframe has reached design mode.
      _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
    }), 1);
  }-*/;

  public void insertHorizontalRule() {
    execCommand("InsertHorizontalRule", null);
  }

  public void insertHTML(String html) {
    execCommand("InsertHTML", html);
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

  public boolean isBold() {
    return queryCommandState("Bold");
  }

  @Override
  public boolean isEnabled() {
    return beforeInitPlaceholder == null ? isEnabledImpl()
        : !beforeInitPlaceholder.getPropertyBoolean("disabled");
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

  public void redo() {
    execCommand("Redo", "false");
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

  @Override
  public void setEnabled(boolean enabled) {
    if (beforeInitPlaceholder == null) {
      setEnabledImpl(enabled);
    } else {
      beforeInitPlaceholder.setPropertyBoolean("disabled", !enabled);
    }
  }

  @Override
  public void setFocus(boolean focused) {
    if (initializing) {
      // Issue 3503: if we focus before the iframe is in design mode, the text
      // caret will not appear.
      isPendingFocus = focused;
    } else {
      setFocusImpl(focused);
    }
  }

  public void setFontName(String name) {
    execCommand("FontName", name);
  }

  public void setFontSize(FontSize fontSize) {
    execCommand("FontSize", Integer.toString(fontSize.getNumber()));
  }

  public void setForeColor(String color) {
    execCommand("ForeColor", color);
  }

  @Override
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
    } else if (justification == Justification.FULL) {
      execCommand("JustifyFull", null);
    } else if (justification == Justification.LEFT) {
      execCommand("JustifyLeft", null);
    } else if (justification == Justification.RIGHT) {
      execCommand("JustifyRight", null);
    }
  }

  @Override
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

  public void undo() {
    execCommand("Undo", "false");
  }

  @Override
  public void uninitElement() {
    isReady = false;

    // Issue 1897: initElement uses a timeout, so its possible to call this
    // method after calling initElement, but before the event system is in
    // place.
    if (initializing) {
      initializing = false;
      return;
    }

    // Unhook all custom event handlers when the element is detached.
    unhookEvents();

    // Recreate the placeholder element and store the iframe's contents and the
    // enabled status in it. This is necessary because some browsers will wipe
    // the iframe's contents when it is removed from the DOM.
    String html = getHTML();
    boolean enabled = isEnabled();
    beforeInitPlaceholder = DOM.createDiv();
    DOM.setInnerHTML(beforeInitPlaceholder, html);
    setEnabled(enabled);
  }

  protected native String getHTMLImpl() /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerHTML;
  }-*/;

  protected native String getTextImpl() /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.textContent;
  }-*/;

  @Override
  protected native void hookEvents() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var wnd = elem.contentWindow;

    elem.__gwt_handler = $entry(function(evt) {
      if (elem.__listener) {
        if (@com.google.gwt.user.client.impl.DOMImpl::isMyListener(Ljava/lang/Object;)(elem.__listener)) {
          @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, elem, elem.__listener);
        }
      }
    });

    elem.__gwt_focusHandler = function(evt) {
      if (elem.__gwt_isFocused) {
        return;
      }

      elem.__gwt_isFocused = true;
      elem.__gwt_handler(evt);
    };

    elem.__gwt_blurHandler = function(evt) {
      if (!elem.__gwt_isFocused) {
        return;
      }

      elem.__gwt_isFocused = false;
      elem.__gwt_handler(evt);
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

    wnd.addEventListener('focus', elem.__gwt_focusHandler, true);
    wnd.addEventListener('blur', elem.__gwt_blurHandler, true);
  }-*/;

  protected native boolean isEnabledImpl() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    return elem.contentWindow.document.designMode.toUpperCase() == 'ON';
  }-*/;

  @Override
  protected void onElementInitialized() {
    // Issue 1897: This method is called after a timeout, during which time the
    // element might by detached.
    if (!initializing) {
      return;
    }
    initializing = false;
    isReady = true;

    // When the iframe is ready, ensure cached content is set.
    if (beforeInitPlaceholder != null) {
      setHTMLImpl(DOM.getInnerHTML(beforeInitPlaceholder));
      setEnabledImpl(isEnabled());
      beforeInitPlaceholder = null;
    }

    super.onElementInitialized();

    // Focus on the element now that it is initialized
    if (isPendingFocus) {
      isPendingFocus = false;
      setFocus(true);
    }
  }

  protected void onElementInitializing() {
    initializing = true;
    isPendingFocus = false;
  }

  protected native void setEnabledImpl(boolean enabled) /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    elem.contentWindow.document.designMode = enabled ? 'On' : 'Off';
  }-*/;

  protected native void setFocusImpl(boolean focused) /*-{
   if (focused) {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.focus();
    } else {
      this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.blur();
    }
  }-*/;

  protected native void setHTMLImpl(String html) /*-{
    this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerHTML = html;
  }-*/;

  protected native void setTextImpl(String text) /*-{
    this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.textContent = text;
  }-*/;

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

    wnd.removeEventListener('focus', elem.__gwt_focusHandler, true);
    wnd.removeEventListener('blur', elem.__gwt_blurHandler, true);

    elem.__gwt_handler = null;
    elem.__gwt_focusHandler = null;
    elem.__gwt_blurHandler = null;
  }-*/;

  void execCommand(String cmd, String param) {
    assert isReady : INACTIVE_MESSAGE;
    if (isReady) {
      // When executing a command, focus the iframe first, since some commands
      // don't take properly when it's not focused.
      setFocus(true);
      try {
        execCommandAssumingFocus(cmd, param);
      } catch (JavaScriptException e) {
        // In mozilla, editing throws a JS exception if the iframe is
        // *hidden, but attached*.
      }
    }
  }

  native void execCommandAssumingFocus(String cmd, String param) /*-{
    this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.execCommand(cmd, false, param);
  }-*/;

  boolean queryCommandState(String cmd) {
    if (isReady) {
      // When executing a command, focus the iframe first, since some commands
      // don't take properly when it's not focused.
      setFocus(true);
      try {
        return queryCommandStateAssumingFocus(cmd);
      } catch (JavaScriptException e) { 
        return false;
      }
    }
    return false;
  }

  native boolean queryCommandStateAssumingFocus(String cmd) /*-{
    return !!this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.queryCommandState(cmd);
  }-*/;

  String queryCommandValue(String cmd) {
    if (isReady) {
      // When executing a command, focus the iframe first, since some commands
      // don't take properly when it's not focused.
      setFocus(true);
      try {
        return queryCommandValueAssumingFocus(cmd);
      } catch (JavaScriptException e) {
        return "";
      }
    }
    return "";
  }

  native String queryCommandValueAssumingFocus(String cmd) /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.queryCommandValue(cmd);
  }-*/;
}
