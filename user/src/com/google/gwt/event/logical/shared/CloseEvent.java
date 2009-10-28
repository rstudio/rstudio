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
 * Represents a close event.
 * 
 * @param <T> the type being closed
 */
public class CloseEvent<T> extends GwtEvent<CloseHandler<T>> {

  /**
   * Handler type.
   */
  private static Type<CloseHandler<?>> TYPE;

  /**
   * Fires a close event on all registered handlers in the handler manager. If
   * no such handlers exist, this method will do nothing.
   * 
   * @param <T> the target type
   * @param source the source of the handlers
   * @param target the target
   */
  public static <T> void fire(HasCloseHandlers<T> source, T target) {
    fire(source, target, false);
  }

  /**
   * Fires a close event on all registered handlers in the handler manager.
   * 
   * @param <T> the target type
   * @param source the source of the handlers
   * @param target the target
   * @param autoClosed was the target closed automatically
   */
  public static <T> void fire(HasCloseHandlers<T> source, T target,
      boolean autoClosed) {
    if (TYPE != null) {
      CloseEvent<T> event = new CloseEvent<T>(target, autoClosed);
      source.fireEvent(event);
    }
  }

  /**
   * Gets the type associated with this event.
   * 
   * @return returns the handler type
   */
  public static Type<CloseHandler<?>> getType() {
    return TYPE != null ? TYPE : (TYPE = new Type<CloseHandler<?>>());
  }

  private final T target;

  private final boolean autoClosed;

  /**
   * Creates a new close event.
   * 
   * @param target the target
   * @param autoClosed whether it is auto closed
   */
  protected CloseEvent(T target, boolean autoClosed) {
    this.autoClosed = autoClosed;
    this.target = target;
  }

  // The instance knows its of type T, but the TYPE
  // field itself does not, so we have to do an unsafe cast here.
  @SuppressWarnings("unchecked")
  @Override
  public final Type<CloseHandler<T>> getAssociatedType() {
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

  /**
   * Was the target automatically closed?
   * 
   * @return auto closed
   */
  public boolean isAutoClosed() {
    return autoClosed;
  }

  @Override
  protected void dispatch(CloseHandler<T> handler) {
    handler.onClose(this);
  }
}
