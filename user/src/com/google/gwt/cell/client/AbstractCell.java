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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

  /**
   * The unmodifiable set of events consumed by this cell.
   */
  private Set<String> consumedEvents;

  /**
   * Construct a new {@link AbstractCell} with the specified consumed events.
   * The input arguments are passed by copy.
   *
   * @param consumedEvents the events that this cell consumes
   */
  public AbstractCell(String... consumedEvents) {
    Set<String> events = null;
    if (consumedEvents != null && consumedEvents.length > 0) {
      events = new HashSet<String>();
      for (String event : consumedEvents) {
        events.add(event);
      }
    }
    init(events);
  }

  /**
   * Construct a new {@link AbstractCell} with the specified consumed events.
   *
   * @param consumedEvents the events that this cell consumes
   */
  public AbstractCell(Set<String> consumedEvents) {
    init(consumedEvents);
  }

  public boolean dependsOnSelection() {
    return false;
  }

  public Set<String> getConsumedEvents() {
    return consumedEvents;
  }

  public boolean handlesSelection() {
    return false;
  }

  /**
   * Returns false. Subclasses that support editing should override this method
   * to return the current editing status.
   */
  public boolean isEditing(Element element, C value, Object key) {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * This method is a no-op in {@link AbstractCell}. If you override this method
   * to add support for events, remember to pass the event types that the cell
   * expects into the constructor.
   */
  public void onBrowserEvent(Element parent, C value, Object key,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
  }

  public abstract void render(C value, Object key, StringBuilder sb);

  public void setValue(Element parent, C value, Object key) {
    StringBuilder sb = new StringBuilder();
    render(value, key, sb);
    parent.setInnerHTML(sb.toString());
  }

  /**
   * Initialize the cell.
   *
   * @param consumedEvents the events that the cell consumes
   */
  private void init(Set<String> consumedEvents) {
    if (consumedEvents != null) {
      this.consumedEvents = Collections.unmodifiableSet(consumedEvents);
    }
  }
}
