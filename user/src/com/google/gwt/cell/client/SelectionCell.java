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
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.SelectElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A {@link Cell} used to render a drop-down list.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 */
public class SelectionCell extends AbstractEditableCell<String, String> {

  private HashMap<String, Integer> indexForOption = new HashMap<
      String, Integer>();
  private final List<String> options;

  /**
   * Construct a new {@link SelectionCell} with the specified options.
   *
   * @param options the options in the cell.s
   */
  public SelectionCell(List<String> options) {
    this.options = new ArrayList<String>(options);
    int index = 0;
    for (String option : options) {
      indexForOption.put(option, index++);
    }
  }

  @Override
  public boolean consumesEvents() {
    return true;
  }

  @Override
  public void onBrowserEvent(Element parent, String value, Object key,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    String type = event.getType();
    if ("change".equals(type)) {
      SelectElement select = parent.getFirstChild().cast();
      String newValue = options.get(select.getSelectedIndex());
      setViewData(key, newValue);
      if (valueUpdater != null) {
        valueUpdater.update(newValue);
      }
    }
  }

  @Override
  public void render(String value, Object key, StringBuilder sb) {
    // Get the view data.
    String viewData = getViewData(key);
    if (viewData != null && viewData.equals(value)) {
      clearViewData(key);
      viewData = null;
    }

    int selectedIndex = getSelectedIndex(viewData == null ? value : viewData);
    sb.append("<select>");
    int index = 0;
    for (String option : options) {
      if (index++ == selectedIndex) {
        sb.append("<option selected='selected'>");
      } else {
        sb.append("<option>");
      }
      sb.append(option);
      sb.append("</option>");
    }
    sb.append("</select>");
  }

  private int getSelectedIndex(String value) {
    Integer index = indexForOption.get(value);
    if (index == null) {
      return -1;
    }
    return index.intValue();
  }
}
