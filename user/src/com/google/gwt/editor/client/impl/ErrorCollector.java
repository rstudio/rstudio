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
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.editor.client.HasEditorErrors;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Collects and propagates EditorErrors through an Editor hierarchy.
 */
class ErrorCollector extends EditorVisitor {
  private final Stack<List<EditorError>> errorStack = new Stack<List<EditorError>>();
  private String lastAddedPath;
  private List<EditorError> lastAdded;

  public ErrorCollector(List<EditorError> errors) {
    assert errors != null;
    errorStack.push(errors);
  }

  @Override
  public <T> void endVisit(EditorContext<T> ctx) {
    AbstractEditorDelegate<?, ?> delegate = (AbstractEditorDelegate<?, ?>) ctx.getEditorDelegate();
    if (delegate == null) {
      return;
    }

    // Collect errors
    List<EditorError> errors = delegate.getErrors();
    lastAdded = new ArrayList<EditorError>(errors);
    lastAddedPath = ctx.getAbsolutePath();
    errorStack.peek().addAll(errors);
    errors.clear();

    // Filter collected errors through an error-aware editor
    HasEditorErrors<T> asErrors = ctx.asHasEditorErrors();
    if (asErrors != null) {
      // Get the enclosing error domain
      List<EditorError> tryConsume = errorStack.pop();
      int prefixLength = ctx.getAbsolutePath().length();
      // Remove trailing dot in non-empty paths
      prefixLength = prefixLength == 0 ? 0 : prefixLength + 1;
      for (EditorError error : tryConsume) {
        ((SimpleError) error).setPathPrefixLength(prefixLength);
      }
      /*
       * Pass collected errors to the editor. Must pass empty error collection
       * to the editor so that it can clear any existing errors when problems
       * are fixed.
       */
      asErrors.showErrors(tryConsume);

      // Short-circuit if there are no existing errors
      if (!tryConsume.isEmpty()) {
        List<EditorError> accumulator = errorStack.peek();
        for (EditorError e : tryConsume) {
          // Pass unconsumed error to enclosing domain
          if (!e.isConsumed()) {
            accumulator.add(e);
          }
        }
      }
    }
  }

  @Override
  public <Q> boolean visit(EditorContext<Q> ctx) {
    // Create a new "domain" for each error-aware editor
    HasEditorErrors<Q> asErrors = ctx.asHasEditorErrors();
    if (asErrors != null) {
      /*
       * Aliased editors (like ValueBoxEditorDecorator) will see the errors for
       * an editor at the same path that it occupies. If the editor that we're
       * currently looking at has the same path as the last thing we just saw,
       * recycle the previous errors.
       */
      if (ctx.getAbsolutePath().equals(lastAddedPath)) {
        errorStack.peek().removeAll(lastAdded);
        errorStack.push(lastAdded);
      } else {
        errorStack.push(new ArrayList<EditorError>());
      }
    }
    return true;
  }
}
