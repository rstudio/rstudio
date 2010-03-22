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
package com.google.gwt.bikeshed.list.client;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A table column header.
 * 
 * @param <H> the {#link Cell} type
 */
public class Header<H> {
  private final Cell<H, Void> cell;
  private ValueUpdater<H, Void> updater;
  private H value;

  public Header(Cell<H, Void> cell) {
    this.cell = cell;
  }

  public H getValue() {
    return value;
  }

  public void onBrowserEvent(Element elem, NativeEvent event) {
    cell.onBrowserEvent(elem, value, null, event, updater);
  }

  public void render(StringBuilder sb) {
    cell.render(value, null, sb);
  }

  public void setUpdater(ValueUpdater<H, Void> updater) {
    this.updater = updater;
  }

  public void setValue(H value) {
    this.value = value;
  }
}
