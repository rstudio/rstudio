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
package com.google.gwt.user.client.impl;

import com.google.gwt.user.client.Element;

/**
 * Safari implementation of {@link com.google.gwt.user.client.impl.DomImpl}.
 */
class DOMImplSafari extends DOMImplStandard {

  public native int getAbsoluteLeft(Element elem) /*-{
    var left = 0;
    while (elem) {
      left += elem.offsetLeft - elem.scrollLeft;

      // Safari bug: a top-level absolutely positioned element includes the
      // body's offset position already.
      var parent = elem.offsetParent;
      if (parent && (parent.tagName == 'BODY') &&
          (elem.style.position == 'absolute')) {
        return left;
      }
      
      elem = parent;
    }
    return left + $doc.body.scrollLeft;
  }-*/;

  public native int getAbsoluteTop(Element elem) /*-{
    var top = 0;
    while (elem) {
      top += elem.offsetTop - elem.scrollTop;

      // Safari bug: a top-level absolutely positioned element includes the
      // body's offset position already.
      var parent = elem.offsetParent;
      if (parent && (parent.tagName == 'BODY') &&
          (elem.style.position == 'absolute')) {
        return top;
      }
      
      elem = parent;
    }
    return top + $doc.body.scrollTop;
  }-*/;
}
