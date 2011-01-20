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

import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * A mock implementation of {@link EditorDelegate}.
 * 
 * @param <T> the type being edited
 */
public class MockEditorDelegate<T> implements EditorDelegate<T> {
  private static final HandlerRegistration FAKE_REGISTRATION = new HandlerRegistration() {
    public void removeHandler() {
    }
  };

  private boolean dirty;
  private String path = "";

  /**
   * Returns a zero-length string or the last value passed to {@link #setPath}.
   */
  public String getPath() {
    return path;
  }

  /**
   * Returns {@code false} or the last value passed to
   * {@link #setDirty(boolean)}.
   */
  public boolean isDirty() {
    return dirty;
  }

  /**
   * No-op.
   */
  public void recordError(String message, Object value, Object userData) {
  }

  /**
   * Records the value of {@code dirty} which can be retrieved from
   * {@link #isDirty()}.
   */
  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  /**
   * Controls the return value of {@link #getPath()}.
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Returns a no-op HandlerRegistration instance.
   */
  public HandlerRegistration subscribe() {
    return FAKE_REGISTRATION;
  }
}
