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
    list.flush();
  }

  /**
   * Returns an unmodifiable, live view of the Editors managed by the
   * ListEditor.
   * 
   * @return a List of {@link Editor Editors} of type E
   */
  public List<E> getEditors() {
    if (list == null) {
      throw new IllegalStateException("Must call EditorDriver.edit() first");
    }
    return Collections.unmodifiableList(list.getEditors());
  }

  /**
   * Returns a live view of the ListEditor's backing data. The structure of the
   * List may be mutated arbitrarily, subject to the limitations of the backing
   * List, but the elements themselves should not be mutated except through
   * {@link #getEditors()} to avoid data inconsistency.
   * 
   * <pre>
   * ListEditor&lt;Foo, MyFooEditor> listEditor = ListEditor.of(...);
   * listEditor.setValue(listOfFoo); // Usually called by EditorDriver
   * listEditor.getList().set(1, new Foo());
   * listEditor.getEditors().get(1).getFooFieldEditor().setValue(....);
   * </pre>
   * 
   * @return a live view of the ListEditor's backing data
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
   * 
   * @param value a List of data objects of type T
   */
  public void setValue(List<T> value) {
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
