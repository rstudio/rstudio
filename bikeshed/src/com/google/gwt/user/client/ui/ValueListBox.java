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
package com.google.gwt.user.client.ui;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link HasValue} variation on {@link ListBox}.
 *
 * @param <T> the value type
 */
public class ValueListBox<T> extends Composite implements HasValue<T>,
    HasValueMap<T> {

  private ArrayList<T> indexToValue = new ArrayList<T>();
  private Map<T, Integer> valueToIndex = new HashMap<T, Integer>();

  // TODO ValueSetListBox<T> extends Composite implements HasValue<Set<T>>,
  // HasValueMap<T>

  public ValueListBox() {
    initWidget(new ListBox(false));
  }

  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<T> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  public T getValue() {
    int selectedIndex = getListBox().getSelectedIndex();
    if (selectedIndex > -1) {
      return indexToValue.get(selectedIndex);
    }

    return null;
  }

  public void setValue(T value) {
    setValue(value, false);
  }

  /**
   * Set the value, or clear it if the given value is not in the value map.
   */
  public void setValue(T value, boolean fireEvents) {
    T oldValue = getValue();
    ListBox listBox = getListBox();
    Integer index = valueToIndex.get(value);
    if (index == null) {
      listBox.setSelectedIndex(-1);
    } else {
      listBox.setSelectedIndex(index);
    }
    if (fireEvents) {
      ValueChangeEvent.fireIfNotEqual(this, oldValue, value);
    }
  }

  public void setValues(Map<? extends T, String> values) {
    ListBox listBox = getListBox();

    indexToValue.clear();
    valueToIndex.clear();
    listBox.clear();
    int i = 0;
    for (T key : values.keySet()) {
      indexToValue.add(key);
      valueToIndex.put(key, i++);
      listBox.addItem(values.get(key));
    }
  }

  public void setVisibleItemCount(int size) {
    getListBox().setVisibleItemCount(size);
  }

  @Override
  protected void initWidget(Widget widget) {
    super.initWidget(widget);
    getListBox().addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        ValueChangeEvent.fire(ValueListBox.this, getValue());
      }
    });
  }

  /**
   * @return
   */
  private ListBox getListBox() {
    return (ListBox) getWidget();
  }
}
