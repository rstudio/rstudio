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
package com.google.gwt.editor.client.adapters;

import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.user.client.TakesValue;

import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * Adapts the {@link TakesValue} interface to the Editor framework.
 * 
 * @param <T> the type of value to be edited
 */
public class TakesValueEditor<T> implements LeafValueEditor<T>,
    ValueAwareEditor<T> {

  public static <T> TakesValueEditor<T> of(TakesValue<T> peer) {
    return new TakesValueEditor<T>(peer);
  }
  
  private EditorDelegate<T> delegate;
  private final TakesValue<T> peer;

  protected TakesValueEditor(TakesValue<T> peer) {
    this.peer = peer;
  }

  public void flush() {
    peer.getValue();
  }

  public T getValue() {
    return peer.getValue();
  }

  public void onPropertyChange(String... paths) {
  }

  public void setDelegate(EditorDelegate<T> delegate) {
    this.delegate = delegate;
  }

  public void setValue(T value) {
    peer.setValue(delegate.ensureMutable(value));
  }

  public void showErrors(Set<ConstraintViolation<T>> violations) {
  }

  protected EditorDelegate<T> getDelegate() {
    return delegate;
  }

  protected TakesValue<T> getPeer() {
    return peer;
  }
}
