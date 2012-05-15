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
import com.google.gwt.editor.client.ValueAwareEditor;

/**
 * A lightweight peer to {@link Initializer} which simply resets the values in
 * the editor and delegate hiererchy.
 */
public class Refresher extends EditorVisitor {
  @Override
  public <Q> boolean visit(EditorContext<Q> ctx) {
    Q toSet = ctx.getFromModel();
    @SuppressWarnings("unchecked")
    AbstractEditorDelegate<Q, ?> delegate = (AbstractEditorDelegate<Q, ?>) ctx.getEditorDelegate();
    if (delegate != null) {
      delegate.setObject(delegate.ensureMutable(toSet));
      delegate.setDirty(false);
    }
    ValueAwareEditor<Q> asValue = ctx.asValueAwareEditor();
    if (asValue != null) {
      // Call setValue for ValueAware, non-leaf editors
      asValue.setValue(toSet);
    } else {
      LeafValueEditor<Q> asLeaf = ctx.asLeafValueEditor();
      if (asLeaf != null) {
        // Call setvalue for LeafValueEditors.
        asLeaf.setValue(toSet);
      }
    }
    // CompositeEditor's setValue should create sub-editors and attach them to
    // the EditorChain, which will traverse them. Returning true here for a
    // CompositeEditor would then traverse it twice. See issue 7038.
    return ctx.asCompositeEditor() == null;
  }
}