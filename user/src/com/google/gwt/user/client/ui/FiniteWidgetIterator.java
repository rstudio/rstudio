/*
 * Copyright 2011 Google Inc.
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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator over a finite number of widgets, which are stored in a delegate
 * class (usually a widget panel).
 * 
 * <p>
 * In order to use this class, assign each widget in the panel an arbitrary
 * index. For example, {@link HeaderPanel} defines the header as index 0, the
 * content as index 1, and the footer as index 2. Construct a new
 * {@link FiniteWidgetIterator} with a {@link WidgetProvider} that provides the
 * child widgets.
 */
class FiniteWidgetIterator implements Iterator<Widget> {

  /**
   * Provides widgets to the iterator.
   */
  public static interface WidgetProvider {
    IsWidget get(int index);
  }

  private int index = -1;
  private final WidgetProvider provider;
  private final int widgetCount;

  /**
   * Construct a new {@link FiniteWidgetIterator}.
   * 
   * <p>
   * The widget count is the number of child widgets that the panel supports,
   * regardless of whether or not they are set.
   * 
   * @param provider the widget provider
   * @param widgetCount the finite number of widgets that can be provided
   */
  public FiniteWidgetIterator(WidgetProvider provider, int widgetCount) {
    this.provider = provider;
    this.widgetCount = widgetCount;
  }

  public boolean hasNext() {
    // Iterate over the remaining widgets until we find one.
    for (int i = index + 1; i < widgetCount; i++) {
      IsWidget w = provider.get(i);
      if (w != null) {
        return true;
      }
    }
    return false;
  }

  public Widget next() {
    // Iterate over the remaining widgets until we find one.
    for (int i = index + 1; i < widgetCount; i++) {
      index = i;
      IsWidget w = provider.get(i);
      if (w != null) {
        return w.asWidget();
      }
    }
    throw new NoSuchElementException();
  }

  public void remove() {
    if (index < 0 || index >= widgetCount) {
      throw new IllegalStateException();
    }
    IsWidget w = provider.get(index);
    if (w == null) {
      throw new IllegalStateException("Widget was already removed.");
    }
    w.asWidget().removeFromParent();
  }
}
