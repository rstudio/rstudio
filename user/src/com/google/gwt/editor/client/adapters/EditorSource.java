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

import com.google.gwt.editor.client.Editor;

import java.util.ArrayList;
import java.util.List;

/**
 * An entity capable of creating and destroying instances of Editors. This type
 * is used by Editors which operate on ordered data, such as {@link ListEditor}.
 * 
 * @param <E> the type of Editor required
 * @see com.google.gwt.editor.client.testing.FakeEditorSource
 */
public abstract class EditorSource<E extends Editor<?>> {
  /**
   * Create a new Editor.
   * 
   * @param index the position at which the new Editor should be displayed
   * @return an {@link Editor} of type E
   */
  public abstract E create(int index);

  /**
   * Create multiple Editors. This method may be optionally overridden to
   * provide a more efficient means of creating Editors in bulk.
   * 
   * @param count the number of Editors desired
   * @param index the position at which the new Editors should be displayed
   * @return a List of {@link Editor}s of type E
   */
  public List<E> create(int count, int index) {
    assert index >= 0;
    List<E> toReturn = new ArrayList<E>(count);
    for (int i = 0; i < count; i++) {
      toReturn.add(create(index + i));
    }
    return toReturn;
  }

  /**
   * Creates a temporary sub-Editor to use for traversal.
   * <p>
   * For backwards compatibility with GWT 2.5.0 and earlier, the default implementation calls
   * {@code create(0)} and {@link #dispose(Editor) disposes} the editor right away.
   * 
   * @return an {@link Editor} of type E
   * @see ListEditor#createEditorForTraversal()
   * @see com.google.gwt.editor.client.EditorContext#traverseSyntheticCompositeEditor
   */
  public E createEditorForTraversal() {
    E toReturn = create(0);
    dispose(toReturn);
    return toReturn;
  }

  /**
   * Called when an Editor no longer requires a sub-Editor. The default
   * implementation is a no-op.
   *
   * @param subEditor an {@link Editor} of type E
   */
  public void dispose(E subEditor) {
  }

  /**
   * Re-order a sub-Editor. The default implementation is a no-op.
   *
   * @param editor an {@link Editor} of type E
   * @param index the index of the Editor
   */
  public void setIndex(E editor, int index) {
    assert index >= 0;
  }
}