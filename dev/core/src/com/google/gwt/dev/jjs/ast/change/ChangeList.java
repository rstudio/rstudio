/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.CanBeSetFinal;
import com.google.gwt.dev.jjs.ast.HasEnclosingType;
import com.google.gwt.dev.jjs.ast.HasSettableType;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.Mutator;

import java.util.ArrayList;
import java.util.List;

public class ChangeList extends ChangeBase {

  static String getEnclosingTypeString(String prefix, Object x) {
    if (x instanceof HasEnclosingType) {
      JReferenceType enclosingType = ((HasEnclosingType) x).getEnclosingType();
      if (enclosingType != null) {
        return prefix + " type '" + enclosingType.getName() + "'";
      }
    }
    return "";
  }

  static String getNodeString(JNode x) {
    if (x == null) {
      return String.valueOf(x);
    }
    return x.getClass().getName() + " '" + x + "'";
  }

  private final List/* <Change> */changes = new ArrayList/* <Change> */();

  private final String description;

  public ChangeList(String description) {
    this.description = description;
  }

  public void add(Change change) {
    changes.add(change);
  }

  public/* <N extends JNode> */void addAll(List/* <N> */x, int index,
      List/* <N> */list) {
    AddAll change = new AddAll/* <N> */(x, index, list);
    changes.add(change);
  }

  public void addExpression(JExpression x, List/* <JExpression> */list) {
    addNode(x, -1, list);
  }

  public void addExpression(Mutator x, List/* <JExpression> */list) {
    addNode(x, -1, list);
  }

  public void addMethod(JMethod x) {
    addNode(x, -1, x.getEnclosingType().methods);
  }

  public/* <N extends JNode> */void addNode(JNode x, int index, List/* <N> */list) {
    AddNode change = new AddNode/* <N> */(x, index, list);
    changes.add(change);
  }

  public/* <N extends JNode> */void addNode(Mutator/* <N> */x, int index,
      List/* <N> */list) {
    AddNodeMutator change = new AddNodeMutator/* <N> */(x, index, list);
    changes.add(change);
  }

  public void addStatement(JStatement x, int index, JBlock body) {
    addNode(x, index, body.statements);
  }

  public void addStatement(JStatement x, JBlock body) {
    addNode(x, -1, body.statements);
  }

  public void apply() {
    for (int i = 0; i < changes.size(); ++i) {
      Change change = (Change) changes.get(i);
      change.apply();
    }
  }

  public void changeType(HasSettableType x, JType type) {
    TypeChange change = new TypeChange(x, type);
    changes.add(change);
  }

  public/* <N extends JNode> */void clear(List/* <N> */list) {
    ClearList/* <N> */change = new ClearList/* <N> */(list);
    changes.add(change);
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    TreeLogger branch = logger.branch(type, description, null);
    for (int i = 0; i < changes.size(); ++i) {
      Change change = (Change) changes.get(i);
      change.describe(branch, type);
    }
  }

  public boolean empty() {
    return changes.size() == 0;
  }

  public void makeFinal(CanBeSetFinal x) {
    MakeFinal change = new MakeFinal(x);
    changes.add(change);
  }

  public void moveBody(JMethod sourceMethod, JMethod targetMethod) {
    JBlock source = sourceMethod.body;
    JBlock target = targetMethod.body;
    MoveBlock change = new MoveBlock(source, target);
    changes.add(change);
  }

  public void removeField(JField x) {
    removeNode(x, x.getEnclosingType().fields);
  }

  public void removeMethod(JMethod x) {
    removeNode(x, x.getEnclosingType().methods);
  }

  public/* <N extends JNode> */void removeNode(JNode x, List/* <N> */list) {
    RemoveNode/* <N> */change = new RemoveNode/* <N> */(x, list);
    changes.add(change);
  }

  public/* <N extends JNode> */void removeNode(Mutator/* <N> */x,
      List/* <N> */list) {
    RemoveNodeMutator/* <N> */change = new RemoveNodeMutator/* <N> */(x, list);
    changes.add(change);
  }

  public void removeType(JReferenceType x) {
    removeNode(x, x.getProgram().getDeclaredTypes());
  }

  public void replaceExpression(Mutator original, JExpression replace) {
    ReplaceNode change = new ReplaceNode/* <JExpression> */(original, replace);
    changes.add(change);
  }

  public void replaceExpression(Mutator original, Mutator replace) {
    ReplaceNodeMutator change = new ReplaceNodeMutator(original, replace);
    changes.add(change);
  }

}
