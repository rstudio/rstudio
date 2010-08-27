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
package com.google.gwt.editor.client;

import java.util.Collections;
import java.util.Set;

/**
 * Used to edit non-object or immutable values.
 * 
 * @param <T> The type of primitive value
 */
public abstract class PrimitiveValueEditor<T> implements Editor<T> {

  /**
   * Always returns <code>null</code>.
   */
  public final ValueAwareEditor<?> getEditorForPath(String path) {
    return null;
  }

  /**
   * Always returns an empty set.
   */
  public final Set<String> getPaths() {
    return Collections.emptySet();
  }

  /**
   * Returns the value currently associated with the editor.
   */
  public abstract T getValue();

  /**
   * Sets the value to be displayed in the editor.
   */
  public abstract void setValue(T value);
}
