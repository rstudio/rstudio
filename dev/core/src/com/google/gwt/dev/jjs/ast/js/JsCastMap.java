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
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;

import java.util.List;

/**
 * A low-level node representing a castable type map.
 */
public class JsCastMap extends JsonArray {

  /**
   * A low-level node representing a query type for cast/instanceof.
   */
  public static class JsQueryType extends JIntLiteral {
    private final JType queryType;

    public JsQueryType(SourceInfo sourceInfo, JType queryType, int queryId) {
      super(sourceInfo, queryId);
      this.queryType = queryType;
    }

    public int getQueryId() {
      return getValue();
    }

    public JType getQueryType() {
      return queryType;
    }

    public void traverse(JVisitor visitor, Context ctx) {
      if (visitor.visit(this, ctx)) {
      }
      visitor.endVisit(this, ctx);
    }
  }

  public JsCastMap(SourceInfo sourceInfo, List<JsQueryType> queryTypes, JClassType jsoType) {
    super(sourceInfo, jsoType);
    getExprs().addAll(queryTypes);
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(getExprs());
    }
    visitor.endVisit(this, ctx);
  }
}
