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
package com.google.gwt.editor.client;

/**
 * A visitor for examining an Editor hierarchy.
 */
public class EditorVisitor {
  /**
   * Exit an Editor. The default implementation is a no-op.
   * 
   * @param ctx contextual data about the current Editor
   */
  public <T> void endVisit(EditorContext<T> ctx) {
  }

  /**
   * Receive an Editor. The default implementation always returns {@code true}.
   * 
   * @param ctx contextual data about the current Editor
   * @return {@code true} if the visitor should visit any sub-editors of the
   *         current editor.
   */
  public <T> boolean visit(EditorContext<T> ctx) {
    return true;
  }
}
