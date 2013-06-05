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

import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER_LIST_T;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER_STRING;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER_T;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER_WILDCARD;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.SELECT_HANDLER_T;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FocusWidget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Widget that has parameterized event handlers.
 *
 * @param <T> type for the value
 */
public class ValueChangeWidget<T> extends FocusWidget {

  enum HandlerType {
    HANDLER, HANDLER_WILDCARD, HANDLER_STRING, HANDLER_T, HANDLER_LIST_T, SELECT_HANDLER_T,
  }

  private Map<HandlerType, Integer> handlers = new HashMap<HandlerType, Integer>();

  protected ValueChangeWidget() {
    super(Document.get().createDivElement());
  }

  public HandlerRegistration addValueHandler(ValueChangeHandler handler) {
    return addHander(HANDLER);
  }

  public HandlerRegistration addValueHandlerT(ValueChangeHandler<T> handler) {
    return addHander(HANDLER_T);
  }

  public HandlerRegistration addValueHandlerListT(ValueChangeHandler<List<T>> handler) {
    return addHander(HANDLER_LIST_T);
  }

  public HandlerRegistration addValueHandlerString(ValueChangeHandler<String> handler) {
    return addHander(HANDLER_STRING);
  }

  public HandlerRegistration addValueHandlerWildCard(ValueChangeHandler<?> handler) {
    return addHander(HANDLER_WILDCARD);
  }

  public HandlerRegistration addSelectionHandlerT(SelectionHandler<T> handler) {
    return addHander(SELECT_HANDLER_T);
  }

  private HandlerRegistration addHander(HandlerType name) {
    handlers.put(name, getHandlerCount(name) + 1);
    return null;
  }

  public int getHandlerCount(HandlerType name) {
    Integer count = handlers.get(name);
    return count == null ? 0 : count;
  }
}
