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
 * is used by Editors which operate on ordered data, sich as {@link ListEditor}.
 * 
 * @param <E> the type of Editor required
 * @see com.google.gwt.editor.client.testing.FakeEditorSource
 */
public abstract class EditorSource<E extends Editor<?>> {
  /**
   * Create a new Editor.
   * 
   * @param index the position at which the new Editor should be displayed
   */
  public abstract E create(int index);

  /**
   * Create multiple Editors. This method may be optionally overridden to
   * provide a more efficient means of creating Editors in bulk.
   * 
   * @param count the number of Editors desired
   * @param index the position at which the new Editors should be displayed
   */
  public List<E> create(int count, int index) {
    List<E> toReturn = new ArrayList<E>(count);
    for (int i = 0; i < count; i++) {
      toReturn.add(create(index + i));
    }
    return toReturn;
  }

  /**
   * Called when an Editor no longer requires a sub-Editor. The default
   * implementation is a no-op.
   */
  public void dispose(E subEditor) {
  }

  /**
   * Re-order a sub-Editor. The default implementation is a no-op.
   */
  public void setIndex(E editor, int index) {
  }
}