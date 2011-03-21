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
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.ValueAwareEditor;

import java.util.Stack;

/**
 * Copies data from the Editor hierarchy into the backing objects.
 */
class Flusher extends EditorVisitor {
  private final Stack<AbstractEditorDelegate<?, ?>> delegateStack = new Stack<AbstractEditorDelegate<?, ?>>();

  @Override
  public <Q> void endVisit(EditorContext<Q> ctx) {
    // Flush ValueAware editors
    ValueAwareEditor<Q> asValue = ctx.asValueAwareEditor();

    AbstractEditorDelegate<?, ?> delegate;
    if (ctx.getEditorDelegate() == null) {
      delegate = delegateStack.peek();
    } else {
      delegate = delegateStack.pop();
    }
    assert delegate != null;

    if (asValue != null) {
      if (delegate.shouldFlush()) {
        asValue.flush();
      }
    }

    // Pull value from LeafValueEditors and update edited object
    LeafValueEditor<Q> asLeaf = ctx.asLeafValueEditor();
    if (delegate.shouldFlush() && asLeaf != null && ctx.canSetInModel()) {
      ctx.setInModel(asLeaf.getValue());
    }
  }

  @Override
  public <Q> boolean visit(EditorContext<Q> ctx) {
    EditorDelegate<Q> editorDelegate = ctx.getEditorDelegate();
    if (editorDelegate != null) {
      delegateStack.push((AbstractEditorDelegate<?, ?>) editorDelegate);
    }
    return true;
  }
}