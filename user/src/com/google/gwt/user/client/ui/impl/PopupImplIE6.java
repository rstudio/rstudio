/*
 * Copyright 2006 Google Inc.
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

/**
 * Internet Explorer 6 implementation of
 * {@link com.google.gwt.user.client.ui.impl.PopupImpl}.
 */
public class PopupImplIE6 extends PopupImpl {

  public native void onHide(Element popup) /*-{
    var frame = popup.__frame;
    frame.parentElement.removeChild(frame);
    popup.__frame = null;
    frame.__popup = null;
  }-*/;

  public native void onShow(Element popup) /*-{
    var frame = $doc.createElement('iframe');
    frame.scrolling = 'no';
    frame.frameBorder = 0;
    frame.style.position = 'absolute';

    popup.__frame = frame;
    frame.__popup = popup;
    frame.style.setExpression('left', 'this.__popup.offsetLeft');
    frame.style.setExpression('top', 'this.__popup.offsetTop');
    frame.style.setExpression('width', 'this.__popup.offsetWidth');
    frame.style.setExpression('height', 'this.__popup.offsetHeight');
    popup.parentElement.insertBefore(frame, popup);
  }-*/;
}
