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
 * A default implementation of the {@link Cell} interface.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 *
 * @param <C> the type that this Cell represents
 */
public abstract class AbstractCell<C> implements Cell<C> {

  public boolean consumesEvents() {
    return false;
  }

  public boolean dependsOnSelection() {
    return false;
  }

  public void onBrowserEvent(Element parent, C value, Object key,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
  }

  public abstract void render(C value, Object key, StringBuilder sb);

  public void setValue(Element parent, C value, Object key) {
    StringBuilder sb = new StringBuilder();
    render(value, key, sb);
    parent.setInnerHTML(sb.toString());
  }
}
