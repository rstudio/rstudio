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

import java.util.HashMap;
import java.util.Map;

/**
 * A default implementation of the {@link Cell} interface used for editable
 * cells that need to save view data state for specific values.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 *
 * @param <C> the type that this Cell represents
 * @param <V> the data type of the view data state
 */
public abstract class AbstractEditableCell<C, V> implements Cell<C> {

  /**
   * The map of value keys to the associated view data.
   */
  private final Map<Object, V> viewDataMap = new HashMap<Object, V>();

  /**
   * Clear the view data associated with the specified key.
   *
   * @param key the key identifying the row value
   */
  public void clearViewData(Object key) {
    if (key != null) {
      viewDataMap.remove(key);
    }
  }

  public abstract boolean consumesEvents();

  public boolean dependsOnSelection() {
    return false;
  }

  /**
   * Get the view data associated with the specified key.
   *
   * @param key the key identifying the row object
   * @return the view data, or null if none has been set
   */
  public V getViewData(Object key) {
    return (key == null) ? null : viewDataMap.get(key);
  }

  public abstract void onBrowserEvent(Element parent, C value, Object key,
      NativeEvent event, ValueUpdater<C> valueUpdater);

  public abstract void render(C value, Object key, StringBuilder sb);

  public void setValue(Element parent, C value, Object key) {
    StringBuilder sb = new StringBuilder();
    render(value, key, sb);
    parent.setInnerHTML(sb.toString());
  }

  /**
   * Associate view data with the specified key. If the key is null, the view
   * data will be ignored.
   *
   * @param key the key of the view data
   * @param viewData the view data to associate
   */
  public void setViewData(Object key, V viewData) {
    if (key == null) {
      return;
    }

    if (viewData == null) {
      clearViewData(key);
    } else {
      viewDataMap.put(key, viewData);
    }
  }
}
