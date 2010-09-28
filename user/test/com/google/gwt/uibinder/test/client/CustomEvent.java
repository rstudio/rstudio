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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * A custom event test class.
 *
 * @param <T> the type associated with the event
 */
public class CustomEvent<T> extends GwtEvent<CustomEvent.Handler<T>> {

  /**
   * The handler for the custom event.
   */
  public interface Handler<T> extends EventHandler {
    void onEvent(CustomEvent<T> event);
  }

  public static Type<Handler<?>> type;

  static {
    type = new Type<Handler<?>>();
  }

  private T value;

  public CustomEvent(T value) {
    this.value = value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Type<Handler<T>> getAssociatedType() {
    return (Type) type;
  }

  public T getValue() {
    return value;
  }

  @Override
  protected void dispatch(Handler<T> handler) {
    handler.onEvent(this);
  }
}
