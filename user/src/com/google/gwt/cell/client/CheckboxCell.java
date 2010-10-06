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

  private final boolean isSelectBox;

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
   */
  public CheckboxCell(boolean isSelectBox) {
    super("change", "keydown");
    this.isSelectBox = isSelectBox;
  }

  @Override
  public boolean dependsOnSelection() {
    return isSelectBox;
  }

  @Override
  public boolean handlesSelection() {
    return isSelectBox;
  }

  @Override
  public boolean isEditing(Element parent, Boolean value, Object key) {
    // A checkbox is never in "edit mode". There is no intermediate state
    // between checked and unchecked.
    return false;
  }

  @Override
  public void onBrowserEvent(Element parent, Boolean value, Object key,
      NativeEvent event, ValueUpdater<Boolean> valueUpdater) {
    String type = event.getType();

    boolean enterPressed = "keydown".equals(type)
        && event.getKeyCode() == KeyCodes.KEY_ENTER;
    if ("change".equals(type) || enterPressed) {
      InputElement input = parent.getFirstChild().cast();
      Boolean isChecked = input.isChecked();

      // If the enter key was pressed, toggle the value
      if (enterPressed) {
        isChecked = !isChecked;
        input.setChecked(isChecked);
      }

      setViewData(key, isChecked);
      if (valueUpdater != null) {
        valueUpdater.update(isChecked);
      }
    }
  }

  @Override
  public void render(Boolean value, Object key, SafeHtmlBuilder sb) {
    // Get the view data.
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
