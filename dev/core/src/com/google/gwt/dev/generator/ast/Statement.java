/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.generator.ast;

import java.util.List;
import java.util.Arrays;

/**
 * A Node that represents a single Java statement.
 */
public class Statement extends BaseNode implements Statements {

  String code;

  Expression expression;

  private List list;

  /**
   * Creates a new statement from a String of code representing an Expression.
   * Automatically appends a semicolon to <code>code</code>.
   *
   * @param code An Expression. Should not end with a semicolon.
   */
  public Statement(String code) {
    this.code = code;
    this.list = Arrays.asList(new Statement[]{this});
  }

  /**
   * Creates a new statement from an Expression.
   *
   * @param expression A non-null Expression.
   */
  public Statement(Expression expression) {
    this.expression = expression;
    this.list = Arrays.asList(new Statement[]{this});
  }

  /**
   * Returns this single Statement as a List of Statements of size, one.
   *
   */
  public List getStatements() {
    return list;
  }

  public String toCode() {
    if (expression != null) {
      return expression.toCode() + ";";
    } else {
      return code + ";";
    }
  }
}
