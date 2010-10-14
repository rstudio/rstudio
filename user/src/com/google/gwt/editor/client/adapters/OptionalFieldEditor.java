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

import com.google.gwt.editor.client.CompositeEditor;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.LeafValueEditor;

/**
 * This adapter can be used when a type being edited has an optional field that
 * may be nullified or reassigned as part of the editing process. This consumer
 * of this adapter will typically expose it via the
 * {@link com.google.gwt.editor.client.IsEditor IsEditor} interface:
 * 
 * <pre>
 * class FooSelector extends Composite implements IsEditor&lt;OptionalFieldEditor&lt;Foo, FooEditor>> {
 *   private OptionalFieldEditor&lt;Foo, FooEditor> editor = OptionalFieldEditor.of(new FooEditor());
 *   public OptionalFieldEditor&lt;Foo, FooEditor> asEditor() {
 *     return editor;
 *   }
 * }
 * </pre>
 * 
 * @param <T> The type of data being managed
 * @param <E> The type of Editor
 */
public class OptionalFieldEditor<T, E extends Editor<T>> implements
    CompositeEditor<T, T, E>, LeafValueEditor<T> {

  /**
   * Construct an OptionalFieldEditor backed by the given sub-Editor.
   * 
   * @param <T> The type of data being managed
   * @param <E> The type of Editor
   * @param subEditor the sub-Editor that will be attached to the Editor
   *          hierarchy
   * @return a new instance of OptionalFieldEditor
   */
  public static <T, E extends Editor<T>> OptionalFieldEditor<T, E> of(
      E subEditor) {
    return new OptionalFieldEditor<T, E>(subEditor);
  }

  private EditorChain<T, E> chain;
  private T currentValue;
  private final E subEditor;

  /**
   * Construct an OptionalFieldEditor backed by the given sub-Editor.
   *
   * @param subEditor the sub-Editor that will be attached to the Editor
   *          hierarchy
   */
  protected OptionalFieldEditor(E subEditor) {
    this.subEditor = subEditor;
  }

  /**
   * Returns the sub-Editor that the OptionalFieldEditor was constructed
   * with.
   *
   * @return an {@link Editor} of type E
   */
  public E createEditorForTraversal() {
    return subEditor;
  }

  public void flush() {
    currentValue = chain.getValue(subEditor);
  }

  /**
   * Returns an empty string because there is only ever one sub-editor used.
   */
  public String getPathElement(E subEditor) {
    return "";
  }

  public T getValue() {
    return currentValue;
  }

  public void onPropertyChange(String... paths) {
  }

  public void setDelegate(EditorDelegate<T> delegate) {
  }

  public void setEditorChain(EditorChain<T, E> chain) {
    this.chain = chain;
  }

  public void setValue(T value) {
    if (currentValue != null && value == null) {
      chain.detach(subEditor);
    }
    currentValue = value;
    if (value != null) {
      chain.attach(value, subEditor);
    }
  }

}
