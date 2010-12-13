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
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CommonResources;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasKeyProvider;
import com.google.gwt.view.client.ProvidesKey;

/**
 * A {@link Widget} that wraps a {@link Cell}.
 * 
 * @param <C> the type that the Cell represents
 */
public class CellWidget<C> extends Widget implements HasKeyProvider<C>,
    HasValue<C> {

  /**
   * Create the default element used to wrap the Cell. The default element is a
   * div with display set to inline-block.
   * 
   * @return the default wrapper element
   */
  private static Element createDefaultWrapperElement() {
    Element div = Document.get().createDivElement();
    div.setClassName(CommonResources.getInlineBlockStyle());
    return div;
  }

  /**
   * The cell being wrapped.
   */
  private final Cell<C> cell;

  /**
   * The key provider for the value.
   */
  private final ProvidesKey<C> keyProvider;

  /**
   * The current cell value.
   */
  private C value;

  /**
   * The {@link ValueUpdater} used to trigger value update events.
   */
  private final ValueUpdater<C> valueUpdater = new ValueUpdater<C>() {
    public void update(C value) {
      ValueChangeEvent.fire(CellWidget.this, value);
    }
  };

  /**
   * Construct a new {@link CellWidget} with the specified cell and an initial
   * value of <code>null</code>.
   * 
   * @param cell the cell to wrap
   */
  public CellWidget(Cell<C> cell) {
    this(cell, null, null);
  }

  /**
   * Construct a new {@link CellWidget} with the specified cell and key
   * provider, and an initial value of <code>null</code>.
   * 
   * @param cell the cell to wrap
   * @param keyProvider the key provider used to get keys from values
   */
  public CellWidget(Cell<C> cell, ProvidesKey<C> keyProvider) {
    this(cell, null, keyProvider);
  }

  /**
   * Construct a new {@link CellWidget} with the specified cell and initial
   * value.
   * 
   * @param cell the cell to wrap
   * @param initialValue the initial value of the Cell
   */
  public CellWidget(Cell<C> cell, C initialValue) {
    this(cell, initialValue, null);
  }

  /**
   * Construct a new {@link CellWidget} with the specified cell, initial value,
   * and key provider.
   * 
   * @param cell the cell to wrap
   * @param initialValue the initial value of the Cell
   * @param keyProvider the key provider used to get keys from values
   */
  public CellWidget(Cell<C> cell, C initialValue, ProvidesKey<C> keyProvider) {
    this(cell, initialValue, keyProvider, createDefaultWrapperElement());
  }

  /**
   * Creates a {@link CellWidget} with the specified cell, initial value, key
   * provider, using the specified element as the wrapper around the cell.
   * 
   * @param cell the cell to wrap
   * @param initialValue the initial value of the Cell
   * @param keyProvider the key provider used to get keys from values
   * @param elem the browser element to use
   */
  protected CellWidget(Cell<C> cell, C initialValue,
      ProvidesKey<C> keyProvider, Element elem) {
    this.cell = cell;
    this.keyProvider = keyProvider;
    setElement(elem);
    CellBasedWidgetImpl.get().sinkEvents(this, cell.getConsumedEvents());
    setValue(initialValue);
  }

  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<C> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  /**
   * Get the {@link Cell} wrapped by this widget.
   * 
   * @return the {@link Cell} being wrapped
   */
  public Cell<C> getCell() {
    return cell;
  }

  public ProvidesKey<C> getKeyProvider() {
    return keyProvider;
  }

  public C getValue() {
    return value;
  }

  @Override
  public void onBrowserEvent(Event event) {
    CellBasedWidgetImpl.get().onBrowserEvent(this, event);
    super.onBrowserEvent(event);

    // Forward the event to the cell.
    String eventType = event.getType();
    if (cell.getConsumedEvents().contains(eventType)) {
      cell.onBrowserEvent(createContext(), getElement(), value, event,
          valueUpdater);
    }
  }

  /**
   * Redraw the widget.
   */
  public void redraw() {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    cell.render(createContext(), value, sb);
    getElement().setInnerHTML(sb.toSafeHtml().asString());
  }

  /**
   * {@inheritDoc}
   * <p>
   * This method will redraw the widget if the new value does not equal the
   * existing value.
   * </p>
   */
  public void setValue(C value) {
    setValue(value, false, true);
  }

  /**
   * {@inheritDoc}
   * <p>
   * This method will redraw the widget if the new value does not equal the
   * existing value.
   * </p>
   */
  public void setValue(C value, boolean fireEvents) {
    setValue(value, fireEvents, true);
  }

  /**
   * Sets this object's value and optionally redraw the widget. Fires
   * {@link com.google.gwt.event.logical.shared.ValueChangeEvent} when
   * fireEvents is true and the new value does not equal the existing value.
   * Redraws the widget when redraw is true and the new value does not equal the
   * existing value.
   * 
   * @param value the object's new value
   * @param fireEvents fire events if true and value is new
   * @param redraw redraw the widget if true and value is new
   */
  public void setValue(C value, boolean fireEvents, boolean redraw) {
    C oldValue = getValue();
    if (value != oldValue && (oldValue == null || !oldValue.equals(value))) {
      this.value = value;
      if (redraw) {
        redraw();
      }
      if (fireEvents) {
        ValueChangeEvent.fire(this, value);
      }
    }
  }

  /**
   * Get the {@link Context} for the cell.
   */
  private Context createContext() {
    return new Context(0, 0, getKey(value));
  }

  /**
   * Get the key for the specified value.
   * 
   * @param value the value
   * @return the key
   */
  private Object getKey(C value) {
    return (keyProvider == null || value == null) ? value
        : keyProvider.getKey(value);
  }
}