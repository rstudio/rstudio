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

import java.util.Collections;
import java.util.List;

/**
 * Manages a list of objects and their associated Editors.
 * <p>
 * The ListEditor will have no backing list until {@link #setValue(List)} is
 * called with a non-null value.
 * 
 * @param <T> The type of data being managed
 * @param <E> The type of Editor
 */
public class ListEditor<T, E extends Editor<T>> implements
    CompositeEditor<List<T>, T, E> {

  /**
   * Create a ListEditor backed by an EditorSource.
   * 
   * @param <T> The type of data being managed
   * @param <E> The type of Editor
   * @param source the EditorSource which will create sub-Editors
   * @return a new instance of ListEditor
   */
  public static <T, E extends Editor<T>> ListEditor<T, E> of(
      EditorSource<E> source) {
    return new ListEditor<T, E>(source);
  }

  private CompositeEditor.EditorChain<T, E> chain;
  private EditorSource<E> editorSource;
  private ListEditorWrapper<T, E> list;

  /**
   * Create a ListEditor backed by an EditorSource.
   * 
   * @param source the EditorSource which will create sub-Editors
   */
  protected ListEditor(EditorSource<E> source) {
    this.editorSource = source;
  }

  /**
   * Creates a temporary sub-Editor to use for traversal.
   * 
   * @return an {@link Editor} of type E
   */
  public E createEditorForTraversal() {
    E toReturn = editorSource.create(0);
    editorSource.dispose(toReturn);
    return toReturn;
  }

  public void flush() {
    if (list != null) {
      list.flush();
    }
  }

  /**
   * Returns an unmodifiable, live view of the Editors managed by the
   * ListEditor.
   * <p>
   * The returned list will be live until the next call to {@link #setValue(List)}
   * and shouldn't be used after that. Editors might (or might not) be reused
   * after a call to {@link #setValue(List)}.
   * <p>
   * If there is no backing list, an empty list will be returned.
   * 
   * @return a List of {@link Editor Editors} of type E
   */
  public List<E> getEditors() {
    if (list == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(list.getEditors());
  }

  /**
   * Returns a live view of the ListEditor's backing data.
   * <p>
   * The structure of the List may be mutated arbitrarily, subject to the
   * limitations of the backing List, but the elements themselves should not
   * be mutated except through {@link #getEditors()} to avoid data
   * inconsistency.
   * <p>
   * Returns null if there is no backing list, and edits cannot be made.
   * 
   * <pre>
   * ListEditor&lt;Foo, MyFooEditor> listEditor = ListEditor.of(...);
   * listEditor.setValue(listOfFoo); // Usually called by EditorDriver
   * listEditor.getList().set(1, new Foo());
   * listEditor.getEditors().get(1).getFooFieldEditor().setValue(....);
   * </pre>
   * 
   * @return a live view of the ListEditor's backing data, or <code>null</code>
   *         if there is no backing list.
   */
  public List<T> getList() {
    return list;
  }

  public String getPathElement(E subEditor) {
    return "[" + list.getEditors().indexOf(subEditor) + "]";
  }

  public void onPropertyChange(String... paths) {
  }

  public void setDelegate(EditorDelegate<List<T>> delegate) {
  }

  public void setEditorChain(CompositeEditor.EditorChain<T, E> chain) {
    this.chain = chain;
  }

  /**
   * Sets the ListEditor's backing data.
   * <p>
   * If a null is passed in, the ListEditor will have no backing list and edits
   * cannot be made.
   * 
   * @param value a List of data objects of type T
   */
  public void setValue(List<T> value) {
    if (list == null && value == null) {
      // fast exit
      return;
    }
    if (list != null && list.isSameValue(value)) {
      // setting the same value as the one being edited
      list.refresh();
      return;
    }

    if (list != null) {
      // Having entire value reset, so dump the wrapper gracefully
      list.detach();
    }
    if (value == null) {
      list = null;
    } else {
      list = new ListEditorWrapper<T, E>(value, chain, editorSource);
      list.attach();
    }
  }
}
