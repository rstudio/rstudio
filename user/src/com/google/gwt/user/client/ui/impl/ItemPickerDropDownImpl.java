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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * Suggestion picker drop-down, used in the implementation of
 * {@link com.google.gwt.user.client.ui.SuggestBox}.
 */
public class ItemPickerDropDownImpl extends PopupPanel {
  private final AbstractItemPickerImpl picker;
  private final HasFocus owner;

  public ItemPickerDropDownImpl(final HasFocus owner, AbstractItemPickerImpl picker) {
    super(true);
    setWidget(picker);
    this.picker = picker;
    this.owner = owner;

    picker.addChangeListener(new ChangeListener() {
      public void onChange(Widget sender) {
        hide();
      }
    });
  }

  /**
   * Shows the popup, by default <code>show</code> selects the first item and
   * displays itself under it's owner.
   */
  public void show() {
    showBelow((UIObject) owner);
  }

  /**
   * Shows the popup below the given UI object. By default, first item is
   * selected in the item picker.
   * <p>
   * Note, if the popup would not be visible on the browser, than the popup's
   * position may be adjusted.
   * </p>
   * 
   * @param showBelow the <code>UIObject</code> beneath which the popup should
   *          be shown
   */
  public void showBelow(UIObject showBelow) {
    // A drop down with 0 items should never show itself.
    if (picker.getItemCount() == 0) {
      hide();
      return;
    }

    // Initialize the picker to the first element.
    picker.setSelectedIndex(0);

    // Show must be called first, as otherwise getOffsetWidth is not correct. As
    // the adjustment is very fast, the user experience is not effected by this
    // call.
    super.show();

    // Calculate left.
    int left = showBelow.getAbsoluteLeft();
    int windowRight = Window.getClientWidth() + Window.getScrollLeft();
    int overshootLeft = Math.max(0, (left + getOffsetWidth()) - windowRight);
    left = left - overshootLeft;

    // Calculate top.
    int top = showBelow.getAbsoluteTop() + showBelow.getOffsetHeight();
    int windowBottom = Window.getScrollTop() + Window.getClientHeight();
    int overshootTop = Math.max(0, (top + getOffsetHeight()) - windowBottom);
    top = top - overshootTop;

    // Set the popup position.
    setPopupPosition(left, top);
    super.show();
  }
}
