/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.converter;

import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssVisitor;

/**
 * This visitor allows to visit new nodes created specifically for the conversion.
 */
public class ExtendedCssVisitor extends CssVisitor {

  /**
   * @param x   the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssElse x, Context ctx) {
  }

  /**
   * @param x   the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssElse x, Context ctx) {
    return true;
  }

  /**
   * @param x   the node being visited
   * @param ctx the context for the visit
   */
  public void endVisit(CssElIf x, Context ctx) {
  }

  /**
   * @param x   the node being visited
   * @param ctx the context for the visit
   */
  public boolean visit(CssElIf x, Context ctx) {
    return true;
  }
}
