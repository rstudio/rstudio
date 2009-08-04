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

import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.impl.FragmentExtractor;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsProgramFragment;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
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

  private final JavaToJavaScriptMap map;
  private JsName nameToBillTo;
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
  
  @Override
  public boolean visit(JsBlock x, JsContext<JsStatement> ctx) {
    printJsBlock(x, false, true);
    return false;
  }

  @Override
  public boolean visit(JsProgram x, JsContext<JsProgram> ctx) {
    // Descend naturally.
    return true;
  }

  @Override
  public boolean visit(JsProgramFragment x, JsContext<JsProgramFragment> ctx) {
    // Descend naturally.
    return true;
  }

  @Override
  protected <T extends JsVisitable<T>> T doAccept(T node) {
    JsName newName = nameToBillTo(node);
    if (newName == null) {
      return super.doAccept(node);
    } else {
      JsName oldName = nameToBillTo;
      nameToBillTo = newName;
      int start = out.getPosition();
      T retValue = super.doAccept(node);
      billChars(nameToBillTo, out.getPosition() - start);
      nameToBillTo = oldName;
      return retValue;
    }
  }

  @Override
  protected <T extends JsVisitable<T>> void doAcceptList(List<T> collection) {
    for (T t : collection) {
      doAccept(t);
    }
  }

  @Override
  protected <T extends JsVisitable<T>> void doAcceptWithInsertRemove(
      List<T> collection) {
    for (T t : collection) {
      doAccept(t);
    }
  }

  private void billChars(JsName nameToBillTo, int chars) {
    Integer oldSize = sizeMap.get(nameToBillTo);
    if (oldSize == null) {
      oldSize = 0;
    }
    sizeMap.put(nameToBillTo, oldSize + chars);
  }

  private JsName nameToBillTo(JsVisitable<?> node) {
    if (node instanceof JsStatement) {
      JsStatement stat = (JsStatement) node;
      JReferenceType type = map.typeForStatement(stat);
      if (type != null) {
        return map.nameForType(type);
      }

      JMethod method = FragmentExtractor.methodFor(stat, map);
      if (method != null) {
        return map.nameForMethod(method);
      }
    }

    if (node instanceof JsVar) {
      if (nameToBillTo == null) {
        return ((JsVar) node).getName();
      }
    }

    return null;
  }

}
