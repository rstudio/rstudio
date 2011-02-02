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
package com.google.gwt.editor.client.impl;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;

/**
 * Used by {@link AbstractEditorDelegate#recordError()}.
 */
class SimpleError implements EditorError {
  private final String absolutePath;
  private boolean consumed;
  private final Editor<?> editor;
  private final String message;
  private final Object value;
  private final Object userData;
  private int pathPrefixLength;

  SimpleError(AbstractEditorDelegate<?, ?> delegate, String message,
      Object value, Object userData) {
    this.absolutePath = delegate.getPath();
    this.editor = delegate.getEditor();
    this.message = message;
    this.value = value == null ? delegate.getObject() : value;
    this.userData = userData;
  }

  /**
   * Used to construct an error for an Editor that does not have a delegate.
   */
  SimpleError(AbstractEditorDelegate<?, ?> delegate, String message,
      Object value, Object userData, String extraPath, Editor<?> leafEditor) {
    assert extraPath != null && extraPath.length() > 0;
    this.absolutePath = delegate.getPath() + extraPath;
    this.editor = leafEditor;
    this.message = message;
    this.value = value;
    this.userData = userData;
  }

  public String getAbsolutePath() {
    return absolutePath;
  }

  public Editor<?> getEditor() {
    return editor;
  }

  public String getMessage() {
    return message;
  }

  public String getPath() {
    return absolutePath.substring(pathPrefixLength);
  }

  public Object getUserData() {
    return userData;
  }

  public Object getValue() {
    return value;
  }

  public boolean isConsumed() {
    return consumed;
  }

  public void setConsumed(boolean consumed) {
    this.consumed = consumed;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return getMessage() + " @ " + getPath();
  }

  void setPathPrefixLength(int pathPrefixLength) {
    this.pathPrefixLength = pathPrefixLength;
  }
}