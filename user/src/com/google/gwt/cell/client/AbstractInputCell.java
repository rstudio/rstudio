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

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * An {@link AbstractCell} used to render input elements that can receive focus.
 * 
 * @param <C> the type that this Cell represents
 * @param <V> the data type of the view data state
 */
public abstract class AbstractInputCell<C, V> extends
    AbstractEditableCell<C, V> {

  /**
   * Get the events consumed by the input cell.
   * 
   * @param userEvents the events consumed by the subclass
   * @return the events
   */
  private static Set<String> getConsumedEventsImpl(Set<String> userEvents) {
    Set<String> events = new HashSet<String>();
    events.add(BrowserEvents.FOCUS);
    events.add(BrowserEvents.BLUR);
    events.add(BrowserEvents.KEYDOWN);
    if (userEvents != null && userEvents.size() > 0) {
      events.addAll(userEvents);
    }
    return events;
  }

  /**
   * Get the events consumed by the input cell.
   * 
   * @param userEvents the events consumed by the subclass
   * @return the events
   */
  private static Set<String> getConsumedEventsImpl(String... consumedEvents) {
    Set<String> userEvents = new HashSet<String>();
    if (consumedEvents != null) {
      for (String event : consumedEvents) {
        userEvents.add(event);
      }
    }
    return getConsumedEventsImpl(userEvents);
  }

  /**
   * The currently focused value key. Only one key can be focused at any time.
   */
  private Object focusedKey;

  /**
   * Construct a new {@link AbstractInputCell} with the specified consumed
   * events.
   * 
   * @param consumedEvents the events that this cell consumes
   */
  public AbstractInputCell(String... consumedEvents) {
    super(getConsumedEventsImpl(consumedEvents));
  }

  /**
   * Construct a new {@link AbstractInputCell} with the specified consumed
   * events.
   * 
   * @param consumedEvents the events that this cell consumes
   */
  public AbstractInputCell(Set<String> consumedEvents) {
    super(getConsumedEventsImpl(consumedEvents));
  }

  @Override
  public boolean isEditing(Context context, Element parent, C value) {
    return focusedKey != null && focusedKey.equals(context.getKey());
  }

  @Override
  public void onBrowserEvent(Context context, Element parent, C value,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
    super.onBrowserEvent(context, parent, value, event, valueUpdater);

    // Ignore events that don't target the input.
    Element target = event.getEventTarget().cast();
    if (!getInputElement(parent).isOrHasChild(target)) {
      return;
    }

    String eventType = event.getType();
    if (BrowserEvents.FOCUS.equals(eventType)) {
      focusedKey = context.getKey();
    } else if (BrowserEvents.BLUR.equals(eventType)) {
      focusedKey = null;
    }
  }

  @Override
  public boolean resetFocus(Context context, Element parent, C value) {
    if (isEditing(context, parent, value)) {
      getInputElement(parent).focus();
      return true;
    }
    return false;
  }

  /**
   * Call this method when editing is complete.
   * 
   * @param parent the parent Element
   * @param value the value associated with the cell
   * @param key the unique key associated with the row object
   * @param valueUpdater the value update to fire
   */
  protected void finishEditing(Element parent, C value, Object key,
      ValueUpdater<C> valueUpdater) {
    focusedKey = null;
    getInputElement(parent).blur();
  }

  /**
   * Get the input element.
   * 
   * @param parent the cell parent element
   * @return the input element
   */
  protected Element getInputElement(Element parent) {
    return parent.getFirstChildElement();
  }

  @Override
  protected void onEnterKeyDown(Context context, Element parent, C value,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
    Element input = getInputElement(parent);
    Element target = event.getEventTarget().cast();
    Object key = context.getKey();
    if (getInputElement(parent).isOrHasChild(target)) {
      finishEditing(parent, value, key, valueUpdater);
    } else {
      focusedKey = key;
      input.focus();
    }
  }
}
