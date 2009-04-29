/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dom.client;

class DOMImplIE8 extends DOMImplTrident {

  @Override
  public int getScrollLeft(Element elem) {
    if (isRTL(elem)) {
      // IE8 returns increasingly *positive* values as you scroll left in RTL.
      return -super.getScrollLeft(elem);
    }
    return super.getScrollLeft(elem);
  }

  @Override
  public void setScrollLeft(Element elem, int left) {
    if (isRTL(elem)) {
      // IE8 returns increasingly *positive* values as you scroll left in RTL.
      left = -left;
    }
    super.setScrollLeft(elem, left);
  }
}
