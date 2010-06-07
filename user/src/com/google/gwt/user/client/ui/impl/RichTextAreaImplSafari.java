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

/**
 * Safari rich text platform implementation.
 */
public class RichTextAreaImplSafari extends RichTextAreaImplStandard {

  @Override
  public void setBackColor(String color) {
    // Webkit uses 'BackColor' for the *entire area's* background. 'HiliteColor'
    // does what we actually want.
    execCommand("HiliteColor", color);
  }

  @Override
  protected native String getTextImpl() /*-{
    return this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.body.innerText;
  }-*/;

  @Override
  protected native void hookEvents() /*-{
    var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
    var wnd = elem.contentWindow;

    elem.__gwt_handler = function(evt) {
      if (elem.__listener) {
        if (@com.google.gwt.user.client.impl.DOMImpl::isMyListener(Ljava/lang/Object;)(elem.__listener)) {
          @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, elem, elem.__listener);
        }
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
    wnd.onfocus = function(evt) {
      if (elem.__listener) {
        @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, elem, elem.__listener);
      }
    };

    wnd.onblur = function(evt) {
      if (elem.__listener) {
        @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, elem, elem.__listener);
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

    elem.__gwt_handler = null;

    elem.onfocus = null;
    elem.onblur = null;
  }-*/;
}
