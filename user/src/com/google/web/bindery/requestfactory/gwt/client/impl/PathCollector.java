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
package com.google.web.bindery.requestfactory.gwt.client.impl;

import com.google.web.bindery.autobean.shared.ValueCodex;
import com.google.gwt.editor.client.EditorContext;
import com.google.gwt.editor.client.EditorVisitor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects all non-value-type paths in an editor hierarchy for use with
 * {@link com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver#getPaths()}.
 */
class PathCollector extends EditorVisitor {
  /**
   * Use a set in the case of aliased editors, so we don't repeat path entries.
   */
  private final Set<String> paths = new LinkedHashSet<String>();

  public List<String> getPaths() {
    return new ArrayList<String>(paths);
  }

  @Override
  public <T> boolean visit(EditorContext<T> ctx) {
    String path = ctx.getAbsolutePath();
    if (path.length() > 0) {
      if (ValueCodex.canDecode(ctx.getEditedType())) {
        /*
         * If there's an @Path("foo.bar.valueField") annotation, we want to
         * collect the containing "foo.bar" path.
         */
        int dotPosition = path.lastIndexOf('.');
        if (dotPosition > 0) {
          String parentPath = path.substring(0, dotPosition);
          paths.add(parentPath);
        }
      } else {
        // Always collect @Path("foo.bar.baz") field, when baz isn't a value
        paths.add(path);
      }
    }
    if (ctx.asCompositeEditor() != null) {
      ctx.traverseSyntheticCompositeEditor(this);
    }
    return true;
  }
}
