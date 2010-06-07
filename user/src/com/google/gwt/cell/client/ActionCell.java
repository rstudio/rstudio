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

/**
 * A cell that renders a button and takes a delegate to perform actions on
 * mouseUp.
 * 
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 * 
 * @param <C> the type that this Cell represents
 */
public class ActionCell<C> extends AbstractCell<C> {

  /**
   * The delegate that will handle events from the cell.
   * 
   * @param <T> the type that this delegate acts on
   */
  public static interface Delegate<T> {
    void execute(T object);
  }

  private final String message;
  private final Delegate<C> delegate;

  /**
   * Construct a new {@link ActionCell}.
   * 
   * @param message the message to display on the button
   * @param delegate the delegate that will handle events
   */
  public ActionCell(String message, Delegate<C> delegate) {
    this.message = message;
    this.delegate = delegate;
  }

  @Override
  public boolean consumesEvents() {
    return true;
  }

  @Override
  public Void onBrowserEvent(Element parent, C value, Object viewData,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
    if ("click".equals(event.getType())) {
      delegate.execute(value);
    }
    return null;
  }

  @Override
  public void render(C value, Object viewData, StringBuilder sb) {
    sb.append("<button>" + message + "</button>");
  }
}
