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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation used by various cell based widgets.
 */
abstract class CellBasedWidgetImpl {

  /**
   * The singleton impl instance.
   */
  private static CellBasedWidgetImpl impl;

  /**
   * Get the singleton instance.
   *
   * @return the {@link CellBasedWidgetImpl} instance
   */
  public static CellBasedWidgetImpl get() {
    if (impl == null) {
      impl = GWT.create(CellBasedWidgetImpl.class);
    }
    return impl;
  }

  /**
   * The set of natively focusable elements.
   */
  final Set<String> focusableTypes;

  CellBasedWidgetImpl() {
    focusableTypes = new HashSet<String>();
    focusableTypes.add("select");
    focusableTypes.add("input");
    focusableTypes.add("textarea");
    focusableTypes.add("option");
    focusableTypes.add("button");
    focusableTypes.add("label");
  }

  /**
   * Check if an element is focusable. If an element is focusable, the cell
   * widget should not steal focus from it.
   * 
   * @param elem the element
   * @return true if the element is focusable, false if not
   */
  public boolean isFocusable(Element elem) {
    return focusableTypes.contains(elem.getTagName().toLowerCase())
        || elem.getTabIndex() >= 0;
  }

  /**
   * Process an event on a target cell.
   *
   * @param widget the {@link Widget} on which the event occurred
   * @param event the event to handle
   */
  public void onBrowserEvent(Widget widget, Event event) {
  }

  /**
   * Takes in an html string and processes it, adding support for events.
   *
   * @param html the html string to process
   * @return the processed html string
   */
  public SafeHtml processHtml(SafeHtml html) {
    return html;
  }

  /**
   * Reset focus on an element.
   *
   * @param command the command to execute when resetting focus
   */
  public void resetFocus(ScheduledCommand command) {
    command.execute();
  }

  /**
   * Sink events on the widget.
   *
   * @param widget the {@link Widget} that will handle the events
   * @param typeNames the names of the events to sink
   */
  public final void sinkEvents(Widget widget, Set<String> typeNames) {
    if (typeNames == null) {
      return;
    }

    int eventsToSink = 0;
    for (String typeName : typeNames) {
      int typeInt = Event.getTypeInt(typeName);
      if (typeInt < 0) {
        widget.sinkBitlessEvent(typeName);
      } else {
        typeInt = sinkEvent(widget, typeName);
        if (typeInt > 0) {
          eventsToSink |= typeInt;
        }
      }
    }
    if (eventsToSink > 0) {
      widget.sinkEvents(eventsToSink);
    }
  }

  /**
   * Get the event bits to sink for an event type.
   *
   * @param widget the {@link Widget} that will handle the events
   * @param typeName the name of the event to sink
   * @return the event bits to sink, or -1 if no events to sink
   */
  protected int sinkEvent(Widget widget, String typeName) {
    return Event.getTypeInt(typeName);
  }
}
