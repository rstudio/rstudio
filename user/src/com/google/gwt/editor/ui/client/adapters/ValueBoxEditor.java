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
package com.google.gwt.editor.ui.client.adapters;

import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.HasEditorDelegate;
import com.google.gwt.editor.client.adapters.TakesValueEditor;
import com.google.gwt.user.client.ui.ValueBoxBase;

import java.text.ParseException;

/**
 * Adapts the {@link ValueBoxBase} interface to the Editor framework. This
 * adapter uses {@link ValueBoxBase#getValueOrThrow()} to report parse errors to
 * the Editor framework.
 * 
 * @param <T> the type of value to be edited
 */
public class ValueBoxEditor<T> extends TakesValueEditor<T> implements
    HasEditorDelegate<T> {

  public static <T> ValueBoxEditor<T> of(ValueBoxBase<T> valueBox) {
    return new ValueBoxEditor<T>(valueBox);
  }

  private EditorDelegate<T> delegate;
  private final ValueBoxBase<T> peer;
  private T value;

  protected ValueBoxEditor(ValueBoxBase<T> peer) {
    super(peer);
    this.peer = peer;
  }

  public EditorDelegate<T> getDelegate() {
    return delegate;
  }

  /**
   * Calls {@link ValueBoxBase#getValueOrThrow()}. If a {@link ParseException}
   * is thrown, it will be available through
   * {@link com.google.gwt.editor.client.EditorError#getUserData()
   * EditorError.getUserData()}.
   */
  @Override
  public T getValue() {
    try {
      value = peer.getValueOrThrow();
    } catch (ParseException e) {
      // TODO i18n
      getDelegate().recordError("Bad value (" + peer.getText() + ")",
          peer.getText(), e);
    }
    return value;
  }

  public void setDelegate(EditorDelegate<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setValue(T value) {
    peer.setValue(this.value = value);
  }
}
