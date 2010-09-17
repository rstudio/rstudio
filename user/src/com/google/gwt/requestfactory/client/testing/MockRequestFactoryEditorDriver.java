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
package com.google.gwt.requestfactory.client.testing;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.RequestFactoryEditorDriver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.Violation;

import java.util.Collections;
import java.util.List;

/**
 * A no-op implementation of {@link RequestFactoryEditorDriver} that records its
 * inputs.
 * 
 * @param <P> the Proxy type being edited
 * @param <E> the Editor type
 */
public class MockRequestFactoryEditorDriver<P, E extends Editor<P>> implements
    RequestFactoryEditorDriver<P, E> {
  private static final String[] EMPTY_STRING = new String[0];

  private EventBus eventBus;
  private E editor;
  private P proxy;
  private RequestObject<?> saveRequest;
  private RequestFactory requestFactory;

  /**
   * Records its arguments.
   */
  public void display(P proxy) {
    this.proxy = proxy;
  }

  /**
   * Records its arguments.
   */
  public void edit(P proxy, RequestObject<?> saveRequest) {
    this.proxy = proxy;
    this.saveRequest = saveRequest;
  }

  /**
   * Returns <code>null</code> or the last value recorded.
   */
  @SuppressWarnings("unchecked")
  public <T> RequestObject<T> flush() {
    return (RequestObject<T>) saveRequest;
  }

  /**
   * Returns <code>null</code> or the last value recorded.
   */
  public E getEditor() {
    return editor;
  }

  /**
   * Returns an empty list.
   */
  public List<EditorError> getErrors() {
    return Collections.emptyList();
  }

  /**
   * Returns <code>null</code> or the last value recorded.
   */
  public EventBus getEventBus() {
    return eventBus;
  }

  /**
   * Returns a zero-length array.
   */
  public String[] getPaths() {
    return EMPTY_STRING;
  }

  /**
   * Returns <code>null</code> or the last value recorded.
   */
  public P getProxy() {
    return proxy;
  }

  /**
   * Returns <code>null</code> or the last value recorded.
   */
  public RequestFactory getRequestFactory() {
    return requestFactory;
  }

  /**
   * Returns <code>null</code> or the last value recorded.
   */
  public RequestObject<?> getSaveRequest() {
    return saveRequest;
  }

  /**
   * Returns <code>false</code>.
   */
  public boolean hasErrors() {
    return false;
  }

  /**
   * Records its arguments.
   */
  public void initialize(EventBus eventBus, RequestFactory requestFactory,
      E editor) {
    this.eventBus = eventBus;
    this.requestFactory = requestFactory;
    this.editor = editor;
  }

  /**
   * A no-op method that always returns false.
   */
  public boolean setViolations(Iterable<Violation> errors) {
    return false;
  }
}
