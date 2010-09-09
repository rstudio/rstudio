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
package com.google.gwt.editor.client.testing;

import com.google.gwt.editor.client.adapters.EditorSource;

import java.util.HashMap;
import java.util.Map;

/**
 * A trivial implementation of {@link EditorSource} that creates
 * {@link FakeLeafValueEditor} instances.
 * 
 * @param <T> the type being edited
 */
public class FakeEditorSource<T> extends EditorSource<FakeLeafValueEditor<T>> {
  /**
   * Return value for {@link #getLastKnownPosition} if the editor has been
   * passed into {@link #dispose}.
   */
  public static final int DISPOSED = -2;

  /**
   * Return value for {@link #getLastKnownPosition} if the editor was not
   * created by this FakeEditorSource.
   */
  public static final int UNKNOWN = -1;
  private final Map<FakeLeafValueEditor<T>, Integer> lastKnownPosition = new HashMap<FakeLeafValueEditor<T>, Integer>();

  @Override
  public FakeLeafValueEditor<T> create(int index) {
    FakeLeafValueEditor<T> toReturn = new FakeLeafValueEditor<T>();
    lastKnownPosition.put(toReturn, index);
    return toReturn;
  }

  @Override
  public void dispose(FakeLeafValueEditor<T> subEditor) {
    lastKnownPosition.put(subEditor, DISPOSED);
  }

  public int getLastKnownPosition(FakeLeafValueEditor<T> editor) {
    return lastKnownPosition.containsKey(editor)
        ? lastKnownPosition.get(editor) : UNKNOWN;
  }

  @Override
  public void setIndex(FakeLeafValueEditor<T> editor, int index) {
    lastKnownPosition.put(editor, index);
  }
}
