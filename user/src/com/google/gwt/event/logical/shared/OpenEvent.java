/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.event.logical.shared;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Represents a open event.
 * 
 * @param <T> the type being opened
 */
public class OpenEvent<T> extends GwtEvent<OpenHandler<T>> {

  /**
   * Handler type.
   */
  private static Type<OpenHandler<?>> TYPE;

  /**
   * Fires a open event on all registered handlers in the handler manager.If no
   * such handlers exist, this method will do nothing.
   * 
   * @param <T> the target type
   * @param source the source of the handlers
   * @param target the target
   */
  public static <T> void fire(HasOpenHandlers<T> source, T target) {
    if (TYPE != null) {
      OpenEvent<T> event = new OpenEvent<T>(target);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   * 
   * @return returns the handler type
   */
  public static Type<OpenHandler<?>> getType() {
    if (TYPE == null) {
      TYPE = new Type<OpenHandler<?>>();
    }
    return TYPE;
  }

  private final T target;

  /**
   * Creates a new open event.
   * 
   * @param target the ui object being opened
   */
  protected OpenEvent(T target) {
    this.target = target;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final Type<OpenHandler<T>> getAssociatedType() {
    return (Type) TYPE;
  }

  /**
   * Gets the target.
   * 
   * @return the target
   */
  public T getTarget() {
    return target;
  }

  // Because of type erasure, our static type is
  // wild carded, yet the "real" type should use our I param.

  @Override
  protected void dispatch(OpenHandler<T> handler) {
    handler.onOpen(this);
  }
}
