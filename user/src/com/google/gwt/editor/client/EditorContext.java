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
package com.google.gwt.editor.client;

/**
 * Describes an Editor within an Editor hierarchy.
 * 
 * @param <T> The type of data edited by the Editor
 * @see com.google.gwt.editor.client.testing.FakeEditorContext
 */
public interface EditorContext<T> {
  String ROOT_PATH = "";

  /**
   * Returns a non-null value if the editor returned by {@link #getEditor()}
   * implements {@link CompositeEditor}.
   */
  CompositeEditor<T, ?, ?> asCompositeEditor();

  /**
   * Returns a non-null value if the editor returned by {@link #getEditor()}
   * implements {@link HasEditorDelegate}.
   */
  HasEditorDelegate<T> asHasEditorDelegate();

  /**
   * Returns a non-null value if the editor returned by {@link #getEditor()}
   * implements {@link HasEditorErrors}.
   */
  HasEditorErrors<T> asHasEditorErrors();

  /**
   * Returns a non-null value if the editor returned by {@link #getEditor()}
   * implements {@link LeafValueEditor}.
   */
  LeafValueEditor<T> asLeafValueEditor();

  /**
   * Returns a non-null value if the editor returned by {@link #getEditor()}
   * implements {@link ValueAwareEditor}.
   */
  ValueAwareEditor<T> asValueAwareEditor();

  /**
   * Returns {@code true} if {@link #setInModel(Object)} can be called
   * successfully.
   */
  boolean canSetInModel();

  /**
   * Returns {@code value} cast to the type accepted by the Editor or throws a
   * {@link ClassCastException}.
   * 
   * @param value any value, including {@code null}
   * @return {@code value} cast to the {@code T} type
   * @throws ClassCastException if {@code} value is not assignable to the type
   *           {@code T}
   */
  T checkAssignment(Object value);

  /**
   * Returns the absolute path of the Editor within the hierarchy. This method
   * should be preferred to calling {@code getEditorDelegate().getPath()}
   * becasue not all {@link LeafValueEditor LeafValueEditors} are guaranteed to
   * have an associated delegate.
   */
  String getAbsolutePath();

  /**
   * Returns the {@code T} type.
   */
  Class<T> getEditedType();

  /**
   * Returns the associated Editor.
   */
  Editor<T> getEditor();

  /**
   * Returns the {@link EditorDelegate} associated with the current Editor,
   * which may be {@code null} for {@link LeafValueEditor LeafValueEditors}.
   */
  EditorDelegate<T> getEditorDelegate();

  /**
   * Returns the value to be edited by the current editor.
   */
  T getFromModel();

  /**
   * Sets a new value in the data hierarchy being edited. The
   * {@link #checkAssignment(Object)} method may be used to avoid an unsafe
   * generic cast.
   */
  void setInModel(T data);

  /**
   * Traverse an editor created by
   * {@link CompositeEditor#createEditorForTraversal()} that reflects an
   * uninitialized instance of a composite sub-editor. This can be used to
   * examine the internal structure of a {@link CompositeEditor} even if there
   * are no data elements being edited by that editor.
   * 
   * @throws IllegalStateException if the current Editor is not a
   *           CompositeEditor
   */
  void traverseSyntheticCompositeEditor(EditorVisitor visitor);
}
