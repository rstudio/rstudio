/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FocusWidget;

/**
 * A Widget that has an event parametrized by a wildcard.
 * Note that parameterizing events by wildcards is not good practice.
 *
 * @param <T> Type for the value. Note that the addValueChangeHandler does not use <T>, but ?
 *            instead. (That is exactly what the test is testing).
 */
public class WildcardValueChangeWidget<T> extends FocusWidget {

  T myValue;

  protected WildcardValueChangeWidget() {
    super(Document.get().createDivElement());
  }

  /**
   * Here is the key for the test. ValueChangeHandler is parameterized by ? and not <T>
   */
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<?> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  public T getValue() {
    return myValue;
  }

  public void setValue(T value) {
    myValue = value;
    fireEvent(new ValueChangeEvent<T>(value) { });
  }
}
