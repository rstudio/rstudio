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

import java.util.Set;

/**
 * Describes an editor whose behavior is not altered by the value being
 * displayed.
 * 
 * @param <T> the type of object the editor displays.
 */
public interface Editor<T> {

  /**
   * Used by the EditorDriver to traverse sub-editors. Plays nicely with
   * UiFieldMapper.
   * 
   * @return the Editor associated with the path, or <code>null</code> if there
   *         is no such sub-Editor.
   */
  Editor<?> getEditorForPath(String path);

  /**
   * Returns the paths that the editor intends to display.
   */
  Set<String> getPaths();

}