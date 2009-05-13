/*
 * Copyright 2006 Google Inc.
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

/**
 * A widget that implements this interface sources the events defined by the
 * {@link com.google.gwt.user.client.ui.ChangeListener} interface.
 * 
 * @deprecated we have now separated dom and logical change events. Use
 * {@link com.google.gwt.event.dom.client.HasChangeHandlers} if the
 * change listener was triggered by a dom change event or
 * {@link com.google.gwt.event.logical.shared.HasValueChangeHandlers}
 * if it was based on a logical event
 */
@Deprecated
public interface SourcesChangeEvents {

  /**
   * Adds a listener interface to receive change events.
   * 
   * @param listener the listener interface to add
   * @deprecated use addChangeHandler or addValueChangeHandler instead
   */
  @Deprecated
  void addChangeListener(ChangeListener listener);

  /**
   * Removes a previously added listener interface.
   * 
   * @param listener the listener interface to remove
   *
   * @deprecated Use the
   * {@link com.google.gwt.event.shared.HandlerRegistration#removeHandler}
   * method on the object returned by
   * {@link com.google.gwt.event.dom.client.HasChangeHandlers#addChangeHandler}
   * instead
   */
  @Deprecated
  void removeChangeListener(ChangeListener listener);
}
