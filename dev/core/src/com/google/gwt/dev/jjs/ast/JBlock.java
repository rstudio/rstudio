// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of <code>JStatements</code>.
 */
public class JBlock extends JStatement {

  public List/*<JStatement>*/ statements = new ArrayList/*<JStatement>*/();

  public JBlock(JProgram program) {
    super(program);
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      for (int i = 0; i < statements.size(); ++i) {
        JStatement stmt = (JStatement) statements.get(i);
        stmt.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
