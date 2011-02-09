/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.editor.client.impl;

/**
 * An implementation of {@code EditorContext} that is used as a starting point
 * by EditorDrivers.
 * 
 * @param <T> the type of data being edited
 */
public class RootEditorContext<T> extends AbstractEditorContext<T> {

  private final Class<T> editedType;
  private final T value;

  public RootEditorContext(AbstractEditorDelegate<T, ?> editorDelegate,
      Class<T> editedType, T value) {
    super(editorDelegate.getEditor(), editorDelegate.getPath());
    setEditorDelegate(editorDelegate);
    this.editedType = editedType;
    this.value = value;
  }

  @Override
  public boolean canSetInModel() {
    return true;
  }

  /**
   * Returns {@code value}. There's no way to actually implement this check in
   * GWT client-side code. In order to prevent users from having to check for
   * the root context in a visitor to avoid a special case, we'll just return
   * the object. If the value really isn't assignable to T, a call to
   * {@link #setInModel(Object)} will fail in the generated code, which has an
   * explicit cast.
   */
  @Override
  @SuppressWarnings("unchecked")
  public T checkAssignment(Object value) {
    return (T) value;
  }

  @Override
  public Class<T> getEditedType() {
    return editedType;
  }

  @Override
  public T getFromModel() {
    return value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setInModel(T data) {
    ((AbstractEditorDelegate<T, ?>) getEditorDelegate()).setObject(data);
  }
}
