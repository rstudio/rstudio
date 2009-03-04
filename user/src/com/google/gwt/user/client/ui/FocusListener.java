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
 * Event listener for focus events.
 * 
 * @deprecated Use {@link com.google.gwt.event.dom.client.FocusHandler} and/or
 *             {@link com.google.gwt.event.dom.client.BlurHandler} instead
 */
@Deprecated
public interface FocusListener extends EventListener {

  /**
   * Fired when a widget receives keyboard focus.
   * 
   * @param sender the widget receiving focus.
   * @deprecated Use
   *             {@link com.google.gwt.event.dom.client.FocusHandler#onFocus(com.google.gwt.event.dom.client.FocusEvent)}
   *             instead
   */
  @Deprecated
  void onFocus(Widget sender);

  /**
   * Fired when a widget loses keyboard focus.
   * 
   * @param sender the widget losing focus.
   * @deprecated Use
   *             {@link com.google.gwt.event.dom.client.BlurHandler#onBlur(com.google.gwt.event.dom.client.BlurEvent)}
   *             instead
   */
  @Deprecated
  void onLostFocus(Widget sender);
}
