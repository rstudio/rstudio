/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.impl.FragmentExtractor;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsProgramFragment;
import com.google.gwt.dev.js.ast.JsSeedIdOf;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.collect.HashMap;

import java.util.List;
import java.util.Map;

/**
 * A version of {@link JsSourceGenerationVisitor} that records a
 * {@link SizeBreakdown} as it goes.
 */
public class JsSourceGenerationVisitorWithSizeBreakdown extends
    JsSourceGenerationVisitor {

  private JavaToJavaScriptMap map;
  private JsName billedAncestor; // non-null when an ancestor is also being billed
  private TextOutput out;
  private final Map<JsName, Integer> sizeMap = new HashMap<JsName, Integer>();

  public JsSourceGenerationVisitorWithSizeBreakdown(TextOutput out,
      JavaToJavaScriptMap javaToJavaScriptMap) {
    super(out);
    this.out = out;
    this.map = javaToJavaScriptMap;
  }

  public SizeBreakdown getSizeBreakdown() {
    return new SizeBreakdown(out.getPosition(), sizeMap);
  }

  public Map<Range, SourceInfo> getSourceInfoMap() {
    // override if your child class creates sourceinfo
    return null;
  }

  @Override
  public boolean visit(JsBlock x, JsContext ctx) {
    printJsBlock(x, false, true);
    return false;
  }

  @Override
  public boolean visit(JsProgram x, JsContext ctx) {
    // Descend naturally.
    return true;
  }

  @Override
  public boolean visit(JsProgramFragment x, JsContext ctx) {
    // Descend naturally.
    return true;
  }

  @Override
  public boolean visit(JsSeedIdOf x, JsContext ctx) {
    out.print(String.valueOf(x.getSeedId()));
    return false;
  }

  @Override
  protected final <T extends JsVisitable> T doAccept(T node) {
    JsName newName = nameToBillTo(node, billedAncestor != null);
    return generateAndBill(node, newName);
  }

  @Override
  protected <T extends JsVisitable> void doAcceptList(List<T> collection) {
    for (T t : collection) {
      doAccept(t);
    }
  }

  @Override
  protected <T extends JsVisitable> void doAcceptWithInsertRemove(
      List<T> collection) {
    for (T t : collection) {
      doAccept(t);
    }
  }

  /**
   * Generate some JavaScript and bill the number of characters generated to the given name.
   */
  protected <T extends JsVisitable> T generateAndBill(T node, JsName nameToBillTo) {
    if (nameToBillTo == null) {
      return super.doAccept(node);
    } else {
      int start = out.getPosition();

      JsName savedAncestor = billedAncestor;
      billedAncestor = nameToBillTo;
      T retValue = super.doAccept(node);
      billedAncestor = savedAncestor;

      billChars(nameToBillTo, out.getPosition() - start);
      return retValue;
    }
  }

  protected JDeclaredType getDirectlyEnclosingType(JsName nameToBillTo) {
    if (nameToBillTo == null) {
      return null;
    }

    JDeclaredType type = map.nameToType(nameToBillTo);
    if (type != null) {
      return type;
    }

    JMethod method = map.nameToMethod(nameToBillTo);
    if (method != null) {
      return method.getEnclosingType();
    }

    JField field = map.nameToField(nameToBillTo);
    if (field != null) {
      return field.getEnclosingType();
    }

    return null;
  }

  private void billChars(JsName nameToBillTo, int chars) {
    Integer oldSize = sizeMap.get(nameToBillTo);
    if (oldSize == null) {
      oldSize = 0;
    }
    sizeMap.put(nameToBillTo, oldSize + chars);
  }

  /**
   * Returns the type, function, or variable name where this node's character count
   * should be added, or null to bill to nobody.
   */
  private JsName nameToBillTo(JsVisitable node, boolean isAncestorBilled) {
    if (node instanceof JsStatement) {
      JsStatement stat = (JsStatement) node;
      JClassType type = map.typeForStatement(stat);
      if (type != null) {
        return map.nameForType(type);
      }

      JMethod method = FragmentExtractor.methodFor(stat, map);
      if (method != null) {
        return map.nameForMethod(method);
      }

      return null;

    } else if (node instanceof JsVar) {
      return isAncestorBilled ? null : ((JsVar) node).getName(); // handle top-level vars
    }

    return null;
  }

}
