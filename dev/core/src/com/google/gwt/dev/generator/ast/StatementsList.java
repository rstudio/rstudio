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
import java.util.ArrayList;
import java.util.Iterator;

/**
 * An implementation of <code>Statements</code> that is composed of a list of
 * <code>Statements</code>.
 */
public class StatementsList extends BaseNode implements Statements {

  List/*<Statements>*/ statements;

  /**
   * Creates a new StatementsList with no Statements.
   *
   */
  public StatementsList() {
    statements = new ArrayList();
  }

  /**
   * Returns the Statements that are in this list.
   *
   */
  public List getStatements() {
    return statements;
  }

  public String toCode() {
    StringBuffer code = new StringBuffer();
    for (Iterator it = statements.iterator(); it.hasNext();) {
      Statements stmts = (Statements) it.next();
      code.append(stmts.toCode()).append("\n");
    }
    return code.toString();
  }
}
