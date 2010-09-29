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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HTML;

/**
 * Tests a customized event by allowing registering of
 * {@link CustomEvent.Handler}.
 *
 * @param <T> the type associated with the widget
 */
public class CustomEventWidget<T> extends HTML {

  private final T value;

  public CustomEventWidget(final T value) {
    this.value = value;
    addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        fireEvent(new CustomEvent<T>(value));
      }
    });
  }

  public HandlerRegistration addCustomHandler(CustomEvent.Handler<T> handler) {
    return addHandler(handler, CustomEvent.type);
  }
}
