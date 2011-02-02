/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.editor.client.EditorContext;
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.editor.client.LeafValueEditor;

import java.util.HashMap;
import java.util.Map;

class DirtCollector extends EditorVisitor {
  public boolean dirty;
  private final Map<LeafValueEditor<?>, Object> leafValues = new HashMap<LeafValueEditor<?>, Object>();

  @Override
  public <T> void endVisit(EditorContext<T> ctx) {
    LeafValueEditor<T> editor = ctx.asLeafValueEditor();
    if (editor != null) {
      leafValues.put(editor, editor.getValue());
    }
    @SuppressWarnings("unchecked")
    AbstractEditorDelegate<T, ?> delegate = (AbstractEditorDelegate<T, ?>) ctx.getEditorDelegate();
    if (delegate != null) {
      dirty |= delegate.isDirty();
    }
  }

  public Map<LeafValueEditor<?>, Object> getLeafValues() {
    return leafValues;
  }

  /**
   * Returns {@code true} if
   * {@link com.google.gwt.editor.client.EditorDelegate#setDirty(boolean)} was
   * used.
   */
  public boolean isDirty() {
    return dirty;
  }
}