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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.RequestFactoryEditorDriver;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains utility methods for top-level driver implementations.
 * 
 * @param <R> the type of Record
 * @param <E> the type of Editor
 */
public abstract class AbstractRequestFactoryEditorDriver<R extends EntityProxy, E extends Editor<R>>
    implements RequestFactoryEditorDriver<R, E> {

  private RequestFactoryEditorDelegate<R, E> delegate;
  private E editor;
  private EventBus eventBus;
  private List<String> paths = new ArrayList<String>();
  private RequestFactory requestFactory;
  private RequestObject<?> saveRequest;

  public void edit(R object, RequestObject<?> saveRequest) {
    checkEditor();
    this.saveRequest = saveRequest;
    delegate = createDelegate();
    delegate.initialize(eventBus, requestFactory, "", object, editor,
        saveRequest);
  }

  @SuppressWarnings("unchecked")
  public <T> RequestObject<T> flush() {
    checkDelegate();
    checkSaveRequest();
    delegate.flush();
    return (RequestObject<T>) saveRequest;
  }

  public String[] getPaths() {
    return paths.toArray(new String[paths.size()]);
  }

  public void initialize(EventBus eventBus, RequestFactory requestFactory,
      E editor) {
    this.eventBus = eventBus;
    this.requestFactory = requestFactory;
    this.editor = editor;

    traverseEditors(paths);
  }

  protected abstract RequestFactoryEditorDelegate<R, E> createDelegate();

  protected E getEditor() {
    return editor;
  }

  protected abstract void traverseEditors(List<String> paths);

  private void checkDelegate() {
    if (delegate == null) {
      throw new IllegalStateException("Must call edit() first");
    }
  }

  private void checkEditor() {
    if (editor == null) {
      throw new IllegalStateException("Must call initialize() first");
    }
  }

  private void checkSaveRequest() {
    if (saveRequest == null) {
      throw new IllegalStateException(
          "edit() was called with a null RequestObject");
    }
  }
}
