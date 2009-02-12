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
 * An adapter to simplify focus event listeners that do not need all events
 * defined on the FocusListener interface.
 * 
 * @deprecated Use {@link com.google.gwt.event.dom.client.FocusHandler} and
 *             {@link com.google.gwt.event.dom.client.BlurHandler} instead
 */
@Deprecated
public abstract class FocusListenerAdapter implements FocusListener {

  public void onFocus(Widget sender) {
  }

  public void onLostFocus(Widget sender) {
  }
}
