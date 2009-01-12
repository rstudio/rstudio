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

import java.util.EventListener;

/**
 * Event listener interface for 'change' events.
 * 
 * @deprecated we have now separated dom and logical change events. Use {@link com.google.gwt.event.dom.client.ChangeHandler} if the
 *             change listener was triggered by a dom change event or
 *             {@link com.google.gwt.event.logical.shared.ValueChangeHandler} if
 *             it was based on a logical event
 */
@Deprecated
public interface ChangeListener extends EventListener {

  /**
   * Fired when a widget changes, where 'change' is defined by the widget
   * sending the event.
   * 
   * @param sender the widget that has changed
   * 
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.ChangeHandler#onChange(com.google.gwt.event.dom.client.ChangeEvent)}
   *             if the change listener was triggered by a dom change event or
   *             {@link com.google.gwt.event.logical.shared.ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   *             if it was based on a logical event
   */
  @Deprecated
  void onChange(Widget sender);
}
