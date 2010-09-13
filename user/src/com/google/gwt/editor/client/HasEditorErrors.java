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

import java.util.List;

/**
 * Editors that wish to be notified about ConstraintViolations in the value
 * being edited should implement this interface.
 * 
 * @param <T> the type of object the editor displays.
 */
public interface HasEditorErrors<T> extends Editor<T> {

  /**
   * Called by the EditorDriver to propagate errors. May be called with a
   * zero-length list to indicate that any existing error condition should be
   * cleared.
   * <p>
   * An Editor may consume any errors reported by its sub-Editors by calling
   * {@link EditorError#setConsumed(boolean)}. Any unconsumed editors will be
   * reported up the Editor hierarchy and will eventually be emitted by the
   * EditorDriver.
   * 
   * @param errors an unmodifiable list of EditorErrors
   */
  void showErrors(List<EditorError> errors);
}
