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
 * A kind of {@link Statements} that represents a <code>for</code> loop.
 */
public class ForLoop implements Statements {

  private final StatementsList body;

  private final String initializer;

  private String label;

  private final String step;

  private final String test;

  /**
   * Creates a {@link ForLoop#ForLoop(String,String,String,Statements)} with a
   * null body.
   */
  public ForLoop(String initializer, String test, String step) {
    this(initializer, test, step, null);
  }

  /**
   * Constructs a new <code>ForLoop</code> {@link Node}.
   * 
   * @param initializer The textual initializer {@link Expression}.
   * @param test The textual test {@link Expression}.
   * @param step The textual step {@link Expression}. May be <code>null</code>.
   * @param statements The {@link Statements} for the body of the loop. May be
   *            <code>null</code>.
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

  public List<Statements> getStatements() {
    return body.getStatements();
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String toCode() {
    String loop = "for ( " + initializer + "; " + test + "; " + step + " ) {\n"
        + body.toCode() + "\n" + "}\n";

    return label != null ? label + ": " + loop : loop;
  }
}
