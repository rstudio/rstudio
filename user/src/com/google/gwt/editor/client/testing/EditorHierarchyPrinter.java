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
package com.google.gwt.editor.client.testing;

import com.google.gwt.editor.client.EditorContext;
import com.google.gwt.editor.client.EditorDriver;
import com.google.gwt.editor.client.EditorVisitor;

/**
 * A utility class that creates a string representation of an Editor hierarchy
 * for testing purposes.
 */
public final class EditorHierarchyPrinter extends EditorVisitor {
  private static final String INDENT = "  ";
  private static final String SPACE = " ";

  /**
   * Produce a string representation of the Editor hierarchy being controlled by
   * {@code driver}.
   */
  public static String toString(EditorDriver<?> driver) {
    StringBuilder sb = new StringBuilder();
    driver.accept(new EditorHierarchyPrinter(sb));
    return sb.toString();
  }

  private int level = 0;
  private final StringBuilder sb;

  private EditorHierarchyPrinter(StringBuilder out) {
    this.sb = out;
  }

  @Override
  public <T> void endVisit(EditorContext<T> ctx) {
    level--;
  }

  @Override
  public <T> boolean visit(EditorContext<T> ctx) {
    println(ctx.getAbsolutePath());
    data(ctx.getEditedType().getName());
    data(ctx.getEditor().getClass().getName());
    data("Implements: " //
        + ctx.asCompositeEditor() == null ? "" : "CompositeEditor " //
        + ctx.asHasEditorDelegate() == null ? "" : "HasEditorDelegate " //
        + ctx.asHasEditorErrors() == null ? "" : "HasEditorErrors " //
        + ctx.asLeafValueEditor() == null ? "" : "LeafValueEditor " //
        + ctx.asValueAwareEditor() == null ? "" : "ValueAwareEditor ");
    level++;
    return true;
  }

  private void data(String msg) {
    println(SPACE + msg);
  }

  private void println(String msg) {
    for (int i = 0; i < level; i++) {
      sb.append(INDENT);
    }
    sb.append(msg);
    sb.append("\n");
  }
}
