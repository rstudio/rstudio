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
 * IE6-specific implementation of
 * {@link com.google.gwt.user.client.ui.impl.TextBoxImpl}.
 */
public class TextBoxImplIE6 extends TextBoxImpl {

  public native int getCursorPos(Element elem) /*-{
    try {
      var tr = elem.document.selection.createRange();
      if (tr.parentElement().uniqueID != elem.uniqueID)
        return -1;
      return -tr.move("character", -65535);
    }
    catch (e) {
      return 0;
    }
  }-*/;

  public native int getSelectionLength(Element elem) /*-{
    try {
      var tr = elem.document.selection.createRange();
      if (tr.parentElement().uniqueID != elem.uniqueID)
        return 0;
      return tr.text.length;
    }
    catch (e) {
      return 0;
    }
  }-*/;

  public native int getTextAreaCursorPos(Element elem) /*-{
    try {
      var tr = elem.document.selection.createRange();
      var tr2 = tr.duplicate();
      tr2.moveToElementText(elem);
      tr.setEndPoint('EndToStart', tr2);
      return tr.text.length;
    }
    catch (e) {
      return 0;
    }
  }-*/;

  public native void setSelectionRange(Element elem, int pos, int length) /*-{
    try {
      var tr = elem.createTextRange();
      tr.collapse(true);
      tr.moveStart('character', pos);
      tr.moveEnd('character', length);
      tr.select();
    }
    catch (e) {
    }
  }-*/;

}
