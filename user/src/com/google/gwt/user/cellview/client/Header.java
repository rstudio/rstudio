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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * A table column header or footer.
 *
 * @param <H> the {@link Cell} type
 */
public abstract class Header<H> {

  private final Cell<H> cell;

  private ValueUpdater<H> updater;

  /**
   * Construct a Header with a given {@link Cell}.
   * 
   * @param cell the {@link Cell} responsible for rendering items in the header
   */
  public Header(Cell<H> cell) {
    this.cell = cell;
  }

  /**
   * Return the {@link Cell} responsible for rendering items in the header.
   *
   * @return the header Cell
   */
  public Cell<H> getCell() {
    return cell;
  }

  /**
   * Return the header value.
   *
   * @return the header value
   */
  public abstract H getValue();

  /**
   * Handle a browser event that took place within the header.
   *
   * @param elem the parent Element
   * @param event the native browser event
   */
  public void onBrowserEvent(Element elem, NativeEvent event) {
    H value = getValue();
    cell.onBrowserEvent(elem, value, getKey(), event, updater);
  }

  /**
   * Render the header.
   * 
   * @param sb a {@link SafeHtmlBuilder} to render into
   */
  public void render(SafeHtmlBuilder sb) {
    cell.render(getValue(), getKey(), sb);
  }

  /**
   * Set the {@link ValueUpdater}.
   * 
   * @param updater the value updater to use
   */
  public void setUpdater(ValueUpdater<H> updater) {
    this.updater = updater;
  }

  /**
   * Get the key for the header value. By default, the key is the same as the
   * value. Override this method to return a custom key.
   *
   * @return the key associated with the value
   */
  protected Object getKey() {
    return getValue();
  }
}
