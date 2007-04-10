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

/**
 * A Node that represents a Java while loop.
 */
public class WhileLoop implements Statements {

  StatementsList body;

  String test;

  /**
   * Creates a new while loop with <code>test</code> as the test Expression.
   * The WhileLoop has an empty body.
   *
   * @param test An Expression that must be of type boolean. Must be non-null.
   */
  public WhileLoop(String test) {
    this.test = test;
    this.body = new StatementsList();
  }

  public List getStatements() {
    return body.getStatements();
  }

  public String toCode() {
    return "while ( " + test + " ) {\n" +
        body.toCode() + "\n" +
        "}\n";
  }
}
