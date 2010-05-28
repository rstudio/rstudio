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

/**
 * An editable text cell. Click to edit, escape to cancel, return to commit.
 * 
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 * 
 * Important TODO: This cell still treats its value as HTML for rendering
 * purposes, which is entirely wrong. It should be able to treat it as a proper
 * string (especially since that's all the user can enter).
 */
public class EditTextCell extends AbstractCell<String> {

  @Override
  public boolean consumesEvents() {
    return true;
  }

  @Override
  public String onBrowserEvent(Element parent, String value, Object viewData,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    if (viewData != null) {
      return editEvent(parent, value, (String) viewData, event, valueUpdater);
    }
    return nonEditEvent(parent, value, event);
  }

  @Override
  public void render(String value, Object viewData, StringBuilder sb) {
    if (viewData != null) {
      sb.append("<input type='text' value='" + viewData + "'></input>");
    } else {
      sb.append(value);
    }
  }

  protected String edit(Element parent, String value) {
    setValue(parent, value, value);
    InputElement input = (InputElement) parent.getFirstChild();
    input.focus();
    input.select();
    return value;
  }

  private String cancel(Element parent, String value) {
    setValue(parent, value, null);
    return null;
  }

  private String commit(Element parent, ValueUpdater<String> valueUpdater) {
    InputElement input = (InputElement) parent.getFirstChild();
    String value = input.getValue();
    valueUpdater.update(value);
    return cancel(parent, value);
  }

  private String editEvent(Element parent, String value, String viewData,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    if ("keydown".equals(event.getType())) {
      if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
        return commit(parent, valueUpdater);
      }
      if (event.getKeyCode() == KeyCodes.KEY_ESCAPE) {
        return cancel(parent, value);
      }
    }
    return viewData;
  }

  private String nonEditEvent(Element parent, String value, NativeEvent event) {
    if ("click".equals(event.getType())) {
      return edit(parent, value);
    }
    return null;
  }
}
