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

/**
 * Allows invalid Editor state to be propagated through an Editor hierarchy.
 * Instances of EditorError are typically created as a side effect of calling
 * {@link EditorDelegate#recordError}.
 * 
 * @see com.google.gwt.editor.client.testing.MockEditorError
 */
public interface EditorError {
  /**
   * Returns the absolute path location of the error, relative to the object
   * that was passed into the EditorDriver.
   * 
   * @return the absolute path as a String
   */
  String getAbsolutePath();

  /**
   * Returns the Editor that holds the invalid value.
   * 
   * @return the Editor instance
   */
  Editor<?> getEditor();

  /**
   * Returns a message associated with the error.
   * 
   * @return the error message as a String
   */
  String getMessage();

  /**
   * Returns the path of the error relative to the Editor receiving the error.
   * If the error concerns the Editor that is receiving the error, this method
   * will return an empty string.
   * 
   * @return the error path as a String
   */
  String getPath();

  /**
   * Returns the object passed into {@link EditorDelegate#recordError}.
   * 
   * @return the user data Object
   */
  Object getUserData();

  /**
   * Returns the value that triggered the error.
   * 
   * @return the error value Object
   */
  Object getValue();

  /**
   * Indicates whether or not the EditorError will be propagated to the
   * enclosing Editor.
   * 
   * @return {@code true} if the error will not be propagated
   * @see #setConsumed(boolean)
   */
  boolean isConsumed();

  /**
   * Indicates whether or not the EditorError will be propagated to the
   * enclosing Editor.
   * 
   * @param consumed {@code true} if the error will not be propagated
   * @see #isConsumed()
   */
  void setConsumed(boolean consumed);
}
