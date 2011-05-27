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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.view.client.ProvidesKey;

import java.util.List;

/**
 * An animation used to phase in a value in a {@link CellTable}.
 * 
 * @param <T> the data type of items
 */
public abstract class PhaseAnimation<T> extends Animation {

  /**
   * The duration of the animation used to phase in new rows.
   */
  private static final int DEFAULT_DURATION = 4000;

  /**
   * A {@link PhaseAnimation} for {@link CellList}.
   * 
   * @param <T> the data type of items
   */
  public static class CellListPhaseAnimation<T> extends PhaseAnimation<T> {

    private final CellList<T> cellList;

    /**
     * Construct a new {@link PhaseAnimation}.
     * 
     * @param cellList the {@link CellList} to animate
     * @param value the value to phase in
     * @param keyProvider the {@link ProvidesKey}
     */
    public CellListPhaseAnimation(CellList<T> cellList, T value,
        ProvidesKey<T> keyProvider) {
      super(value, keyProvider);
      this.cellList = cellList;
    }

    @Override
    protected T getDisplayedItem(int index) {
      return cellList.getVisibleItem(index);
    }

    @Override
    protected List<T> getDisplayedItems() {
      return cellList.getVisibleItems();
    }

    @Override
    protected Element getRowElement(int index) {
      return cellList.getRowElement(index);
    }
  }

  /**
   * A {@link PhaseAnimation} for {@link CellTable}.
   * 
   * @param <T> the data type of items
   */
  public static class CellTablePhaseAnimation<T> extends PhaseAnimation<T> {

    private final CellTable<T> cellTable;

    /**
     * Construct a new {@link PhaseAnimation}.
     * 
     * @param cellTable the {@link CellTable} to animate
     * @param value the value to phase in
     * @param keyProvider the {@link ProvidesKey}
     */
    public CellTablePhaseAnimation(CellTable<T> cellTable, T value,
        ProvidesKey<T> keyProvider) {
      super(value, keyProvider);
      this.cellTable = cellTable;
    }

    @Override
    protected T getDisplayedItem(int index) {
      return cellTable.getVisibleItem(index);
    }

    @Override
    protected List<T> getDisplayedItems() {
      return cellTable.getVisibleItems();
    }

    @Override
    protected Element getRowElement(int index) {
      return cellTable.getRowElement(index);
    }
  }

  private final Object key;
  private final ProvidesKey<T> keyProvider;
  private int lastRowIndex = -1;

  /**
   * Construct a new {@link CellTablePhaseAnimation}.
   * 
   * @param value the value to phase in
   * @param keyProvider the {@link ProvidesKey}
   */
  public PhaseAnimation(T value, ProvidesKey<T> keyProvider) {
    this.key = keyProvider.getKey(value);
    this.keyProvider = keyProvider;
  }

  /**
   * Run the animation using the default duration.
   */
  public void run() {
    run(DEFAULT_DURATION);
  }

  /**
   * Get the item at the specified index.
   * 
   * @param index the index
   * @return the item
   */
  protected abstract T getDisplayedItem(int index);

  /**
   * Get a list of all displayed items.
   * 
   * @return the list of items
   */
  protected abstract List<T> getDisplayedItems();

  /**
   * Get the row element at the specified index.
   * 
   * @param index the row index
   * @return the element
   */
  protected abstract Element getRowElement(int index);

  @Override
  protected void onComplete() {
    Element elem = getItemElement();
    if (elem != null) {
      elem.getStyle().clearBackgroundColor();
    }
  }

  @Override
  protected void onUpdate(double progress) {
    Element elem = getItemElement();
    if (elem != null) {
      int r = 255;
      int g = 200 + (int) (55.0 * progress);
      int b = 0 + (int) (255.0 * progress);
      elem.getStyle().setBackgroundColor("rgb(" + r + "," + g + "," + b + ")");
    }
  }

  /**
   * Get the {@link Element} of the value within the table.
   * 
   * @return the element, or null if not found
   */
  private Element getItemElement() {
    // Check if the cached row index is still valid.
    if (lastRowIndex >= 0) {
      T value = getDisplayedItem(lastRowIndex);
      if (value == null || !key.equals(keyProvider.getKey(value))) {
        lastRowIndex = -1;
      }
    }

    // Find the index of the row element.
    if (lastRowIndex < 0) {
      List<T> items = getDisplayedItems();
      for (int i = 0; i < items.size(); i++) {
        T item = items.get(i);
        if (item != null && key.equals(keyProvider.getKey(item))) {
          lastRowIndex = i;
          break;
        }
      }
    }

    // Return the row element.
    return lastRowIndex < 0 ? null : getRowElement(lastRowIndex);
  }
}
