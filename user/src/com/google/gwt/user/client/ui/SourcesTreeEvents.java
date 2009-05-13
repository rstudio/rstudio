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
 * {@link TreeListener} interface.
 * 
 * @deprecated use
 * {@link com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers},
 * {@link com.google.gwt.event.logical.shared.HasOpenHandlers}
 * and
 * {@link com.google.gwt.event.dom.client.HasClickHandlers} instead
 */
@Deprecated
public interface SourcesTreeEvents {

  /**
   * Adds a listener interface to receive tree events.
   * 
   * @param listener the listener interface to add
   * @deprecated use
   * {@link com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers#addBeforeSelectionHandler},
   * {@link com.google.gwt.event.logical.shared.HasOpenHandlers#addOpenHandler}
   * and
   * {@link com.google.gwt.event.dom.client.HasClickHandlers#addClickHandler}
   * instead
   */
  @Deprecated
  void addTreeListener(TreeListener listener);

  /**
   * Removes a previously added listener interface.
   * 
   * @param listener the listener interface to remove
   * @deprecated Use the
   * {@link com.google.gwt.event.shared.HandlerRegistration#removeHandler}
   * method on the object returned by an add*Handler method instead
   */
  @Deprecated
  void removeTreeListener(TreeListener listener);
}
