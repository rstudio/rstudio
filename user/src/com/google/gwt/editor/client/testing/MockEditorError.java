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

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;

/**
 * A trivial implementation of {@link EditorError}. Most methods return
 * <code>null</code>.
 */
public class MockEditorError implements EditorError {

  public String getAbsolutePath() {
    return null;
  }

  public Editor<?> getEditor() {
    return null;
  }

  public String getMessage() {
    return null;
  }

  public String getPath() {
    return null;
  }

  public Object getUserData() {
    return null;
  }

  public Object getValue() {
    return null;
  }

  /**
   * Always returns <code>false</code>.
   */
  public boolean isConsumed() {
    return false;
  }

  /**
   * No-op.
   */
  public void setConsumed(boolean consumed) {
  }
}
