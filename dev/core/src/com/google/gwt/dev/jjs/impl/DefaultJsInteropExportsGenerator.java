/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import static com.google.gwt.dev.js.JsUtils.createAssignment;

import com.google.gwt.dev.javac.JsInteropUtil;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;

import java.util.List;

/**
 * Responsible for handling @JsExport code generation for non-Closure formatted code.
 * <p>
 * Generally, export of global namespaced members looks like this:
 * <pre>
 * _ = provide('dotted.namespace')
 * _.memberName = original
 * </pre>
 * Essentially members are aliased into a global namespace.
 */
class DefaultJsInteropExportsGenerator implements JsInteropExportsGenerator {

  private final List<JsStatement> exportStmts;
  private final JsName globalTemp;
  private final JsName provideFuncionName;
  private String lastExportedNamespace;

  public DefaultJsInteropExportsGenerator(List<JsStatement> exportStmts, JsName globalTemp,
      JsName provideFunctionName) {
    this.exportStmts = exportStmts;
    this.globalTemp = globalTemp;
    this.provideFuncionName = provideFunctionName;
  }

  @Override
  public void exportType(JDeclaredType x) {
    // non-Closure mode doesn't do anything special to export types
  }

  /*
   * Exports a member as
   *  _ = provide('foo.bar.ExportNamespace')
   *  _.memberName = RHS
   *
   * TODO(goktug): optimizing provide calls shouldn't be difficult as exports are now sorted.
   */
  @Override
  public void exportMember(JMember x, JsExpression bridgeMethodOrAlias) {
    if (x.getJsName().isEmpty()) {
      assert x instanceof JConstructor;

      // _ = provide('foo.bar.ExportNamespace', ExportedConstructor)
      ensureProvideNamespace(x, bridgeMethodOrAlias);
      return;
    }

    // _ = provide('foo.bar.ExportNamespace', null)
    ensureProvideNamespace(x, null);

    // _.memberName = RHS
    JsNameRef lhs = new JsNameRef(x.getSourceInfo(), x.getJsName());
    lhs.setQualifier(globalTemp.makeRef(x.getSourceInfo()));
    exportStmts.add(createAssignment(lhs, bridgeMethodOrAlias).makeStmt());
  }

  private void ensureProvideNamespace(JMember member, JsExpression ctor) {
    String namespace = member.getJsNamespace();
    assert !JsInteropUtil.isWindow(namespace);
    namespace = JsInteropUtil.isGlobal(namespace) ? "" : namespace;
    if (namespace.equals(lastExportedNamespace)) {
      return;
    }
    lastExportedNamespace = namespace;

    // _ = JCHSU.provide('foo.bar')
    SourceInfo sourceInfo = member.getSourceInfo();
    JsInvocation provideCall = new JsInvocation(sourceInfo);
    provideCall.setQualifier(provideFuncionName.makeRef(sourceInfo));
    provideCall.getArguments().add(new JsStringLiteral(sourceInfo, namespace));
    if (ctor != null) {
      provideCall.getArguments().add(ctor);
    }
    exportStmts.add(createAssignment(globalTemp.makeRef(sourceInfo), provideCall).makeStmt());
  }
}
