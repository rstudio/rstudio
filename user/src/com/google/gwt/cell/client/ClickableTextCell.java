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

/**
 * A {@link Cell} used to render text. Clicking on the call causes its
 * {@link ValueUpdater} to be called.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 */
public class ClickableTextCell extends AbstractCell<String> {

  public ClickableTextCell() {
    super("click");
  }

  @Override
  public void onBrowserEvent(Element parent, String value, Object key,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    String type = event.getType();
    if (valueUpdater != null && type.equals("click")) {
      valueUpdater.update(value);
    }
  }

  @Override
  public void render(String value, Object key, StringBuilder sb) {
    if (value != null) {
      sb.append(value);
    }
  }
}
