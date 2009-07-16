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

/**
 * IE6-specific implementation of
 * {@link com.google.gwt.user.client.ui.impl.TextBoxImpl}.
 */
public class TextBoxImplIE6 extends TextBoxImpl {

  @Override
  public native int getCursorPos(Element elem) /*-{
    try {
      var tr = elem.document.selection.createRange();
      if (tr.parentElement() !== elem)
        return -1;
      return -tr.move("character", -65535);
    }
    catch (e) {
      return 0;
    }
  }-*/;

  @Override
  public native int getSelectionLength(Element elem) /*-{
    try {
      var tr = elem.document.selection.createRange();
      if (tr.parentElement() !== elem)
        return 0;
      return tr.text.length;
    }
    catch (e) {
      return 0;
    }
  }-*/;

  /**
   * The text reported in the text range does not include newline characters at
   * the end of the selection. So, we need to create 2 ranges and subtract a
   * character from one until the lengths are different. At that point, we know
   * exactly how many \r\n were truncated from the selection.
   */
  @Override
  public native int getTextAreaCursorPos(Element elem) /*-{
    try {
      var tr = elem.document.selection.createRange();
      if (tr.parentElement() !== elem)
        return -1;
      var tr2 = tr.duplicate();
      tr2.moveToElementText(elem);
      tr2.setEndPoint('EndToStart', tr);
      var tr2Length = tr2.text.length; 

      // Subtract characters from the end to account for trimmed newlines.
      var offset = 0;
      var tr3 = tr2.duplicate();
      tr3.moveEnd('character', -1);
      var tr3Length = tr3.text.length;
      while (tr3Length == tr2Length && tr3.parentElement() == elem) {
        offset += 2;
        tr3.moveEnd('character', -1);
        tr3Length = tr3.text.length;
      }
      return tr2Length + offset;
    }
    catch (e) {
      return 0;
    }
  }-*/;

  @Override
  public native int getTextAreaSelectionLength(Element elem) /*-{
    try {
      var tr = elem.document.selection.createRange();
      if (tr.parentElement() !== elem)
        return 0;
      var trLength = tr.text.length;

      // Subtract characters from the end to account for trimmed newlines.
      var offset = 0;
      var tr2 = tr.duplicate();
      tr2.moveEnd('character', -1);
      var tr2Length = tr2.text.length;
      while (tr2Length == trLength && tr2.parentElement() == elem && tr.compareEndPoints('StartToEnd', tr2) <= 0) {
        offset += 2;
        tr2.moveEnd('character', -1);
        tr2Length = tr2.text.length;
      }
      return trLength + offset;
    }
    catch (e) {
      return 0;
    }
  }-*/;

  /**
   * Moving the start 1 character will move across a \r\n, but \r\n counts as
   * two characters, so we need to offset the position accordingly.
   */
  @Override
  public native void setSelectionRange(Element elem, int pos, int length) /*-{
    try {
      var tr = elem.createTextRange();
      var newlinesWithin = elem.value.substr(pos, length).match(/(\r\n)/gi);
      if (newlinesWithin != null) {
        length -= newlinesWithin.length;
      }
      var newlinesBefore = elem.value.substring(0, pos).match(/(\r\n)/gi);
      if (newlinesBefore != null) {
        pos -= newlinesBefore.length;
      }
      tr.collapse(true);
      tr.moveStart('character', pos);
      tr.moveEnd('character', length);
      tr.select();
    }
    catch (e) {
    }
  }-*/;

}
