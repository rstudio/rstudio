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
package com.google.gwt.bikeshed.cells.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A cell that renders a button and takes a delegate to perform actions on
 * mouseUp.
 *
 * @param <C> the type that this Cell represents
 */
public class ActionCell<C> extends Cell<C, Void> {
  /**
   * @param <T> the type that this delegate acts on
   */
  public interface Delegate<T> {
    void execute(T object);
  }

  private final String message;
  private final Delegate<C> delegate;

  /**
   * @param message
   * @param delegate
   */
  public ActionCell(String message, Delegate<C> delegate) {
    this.message = message;
    this.delegate = delegate;
  }

  @Override
  public Void onBrowserEvent(Element parent, C value, Void viewData,
      NativeEvent event, ValueUpdater<C, Void> valueUpdater) {
    if ("mouseup".equals(event.getType())) {
      delegate.execute(value);
    }
    return null;
  }

  @Override
  public void render(C value, Void viewData, StringBuilder sb) {
    sb.append("<button>" + message + "</button>");
  }
}