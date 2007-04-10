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
 * A Node that represents a for loop.
 */
public class ForLoop implements Statements {

  StatementsList body;

  String initializer;

  String label;

  String step;

  String test;

  /**
   * Creates a ForLoop with a null body.
   *
   */
  public ForLoop(String initializer, String test, String step) {
    this(initializer, test, step, null);
  }

  /**
   * Constructs a new ForLoop node.
   *
   * @param initializer The initializer Expression.
   * @param test        The test Expression.
   * @param step        The step Expression. May be null.
   * @param statements The statements for the body of the loop.
   * May be null.
   */
  public ForLoop(String initializer, String test, String step,
      Statements statements) {
    this.initializer = initializer;
    this.test = test;
    this.step = step;
    this.body = new StatementsList();

    if (statements != null) {
      body.getStatements().add(statements);
    }
  }

  public List getStatements() {
    return body.getStatements();
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String toCode() {
    String loop = "for ( " + initializer + "; " + test + "; " + step + " ) {\n"
        +
        body.toCode() + "\n" +
        "}\n";

    return label != null ? label + ": " + loop : loop;
  }
}
