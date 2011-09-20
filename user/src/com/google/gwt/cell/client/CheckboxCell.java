/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.cell.client;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * A {@link Cell} used to render a checkbox. The value of the checkbox may be
 * toggled using the ENTER key as well as via mouse click.
 */
public class CheckboxCell extends AbstractEditableCell<Boolean, Boolean> {

  /**
   * An html string representation of a checked input box.
   */
  private static final SafeHtml INPUT_CHECKED = SafeHtmlUtils.fromSafeConstant("<input type=\"checkbox\" tabindex=\"-1\" checked/>");

  /**
   * An html string representation of an unchecked input box.
   */
  private static final SafeHtml INPUT_UNCHECKED = SafeHtmlUtils.fromSafeConstant("<input type=\"checkbox\" tabindex=\"-1\"/>");

  private final boolean dependsOnSelection;
  private final boolean handlesSelection;

  /**
   * Construct a new {@link CheckboxCell}.
   */
  public CheckboxCell() {
    this(false);
  }

  /**
   * Construct a new {@link CheckboxCell} that optionally controls selection.
   *
   * @param isSelectBox true if the cell controls the selection state
   * @deprecated use {@link #CheckboxCell(boolean, boolean)} instead
   */
  @Deprecated
  public CheckboxCell(boolean isSelectBox) {
    this(isSelectBox, isSelectBox);
  }

  /**
   * Construct a new {@link CheckboxCell} that optionally controls selection.
   *
   * @param dependsOnSelection true if the cell depends on the selection state
   * @param handlesSelection true if the cell modifies the selection state
   */
  public CheckboxCell(boolean dependsOnSelection, boolean handlesSelection) {
    super(BrowserEvents.CHANGE, BrowserEvents.KEYDOWN);
    this.dependsOnSelection = dependsOnSelection;
    this.handlesSelection = handlesSelection;
  }

  @Override
  public boolean dependsOnSelection() {
    return dependsOnSelection;
  }

  @Override
  public boolean handlesSelection() {
    return handlesSelection;
  }

  @Override
  public boolean isEditing(Context context, Element parent, Boolean value) {
    // A checkbox is never in "edit mode". There is no intermediate state
    // between checked and unchecked.
    return false;
  }

  @Override
  public void onBrowserEvent(Context context, Element parent, Boolean value, 
      NativeEvent event, ValueUpdater<Boolean> valueUpdater) {
    String type = event.getType();

    boolean enterPressed = BrowserEvents.KEYDOWN.equals(type)
        && event.getKeyCode() == KeyCodes.KEY_ENTER;
    if (BrowserEvents.CHANGE.equals(type) || enterPressed) {
      InputElement input = parent.getFirstChild().cast();
      Boolean isChecked = input.isChecked();

      /*
       * Toggle the value if the enter key was pressed and the cell handles
       * selection or doesn't depend on selection. If the cell depends on
       * selection but doesn't handle selection, then ignore the enter key and
       * let the SelectionEventManager determine which keys will trigger a
       * change.
       */
      if (enterPressed && (handlesSelection() || !dependsOnSelection())) {
        isChecked = !isChecked;
        input.setChecked(isChecked);
      }

      /*
       * Save the new value. However, if the cell depends on the selection, then
       * do not save the value because we can get into an inconsistent state.
       */
      if (value != isChecked && !dependsOnSelection()) {
        setViewData(context.getKey(), isChecked);
      } else {
        clearViewData(context.getKey());
      }

      if (valueUpdater != null) {
        valueUpdater.update(isChecked);
      }
    }
  }

  @Override
  public void render(Context context, Boolean value, SafeHtmlBuilder sb) {
    // Get the view data.
    Object key = context.getKey();
    Boolean viewData = getViewData(key);
    if (viewData != null && viewData.equals(value)) {
      clearViewData(key);
      viewData = null;
    }

    if (value != null && ((viewData != null) ? viewData : value)) {
      sb.append(INPUT_CHECKED);
    } else {
      sb.append(INPUT_UNCHECKED);
    }
  }
}
