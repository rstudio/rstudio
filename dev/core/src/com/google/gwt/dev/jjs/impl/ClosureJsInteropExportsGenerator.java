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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.js.JsUtils;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for handling @JsExport code generation for Closure formatted code.
 * <p>
 * In closure formatted mode, there are additional restrictions due to the way goog.provide() works:
 * <li> You can't goog.provide something more than once
 * <li> Enclosing namespaces must be setup and handled before enclosed namespaces
 * <li> If the exported namespace is a @JsType with methods, than the namespace will need to have
 * a function assigned to it, instead of an object literal returned by goog.provide.
 * <p>
 * In general, this implies code like the following:
 * <pre>
 * goog.provide('dotted.parent.namespace')
 * dotted.parent.namespace = constructor (synthesized one or the exported one)
 * dotted.parent.namespace.member = blah;
 * goog.provide('dotted.parent.namespace.enclosed')
 * dotted.parent.namespace.enclosed = (synthesized one or the exported one)
 * dotted.parent.namespace.enclosed.member = blah;
 * </pre>
 */
class ClosureJsInteropExportsGenerator implements JsInteropExportsGenerator {

  private final List<JsStatement> exportStmts;
  private final Map<HasName, JsName> names;
  private final Set<String> providedNamespaces = new HashSet<String>();

  public ClosureJsInteropExportsGenerator(List<JsStatement> exportStmts,
      Map<HasName, JsName> names) {
    this.exportStmts = exportStmts;
    this.names = names;
  }

  /**
   * Ensures that a @JsType without exported constructor is still defined by goog.provide using the
   * synthesized constructor so that type declarations like
   * {@code /* @returns {number} * / Foo.prototype.method;} that are added by linker works.
   */
  @Override
  public void exportType(JDeclaredType x) {
    // Note that synthesized constructors use the name of the declared types.
    generateExport(x.getQualifiedExportName(), x.getQualifiedExportName(), x, x.getSourceInfo());
  }

  /*
   * Exports a member as:
   *  goog.provide('foo.bar');
   *  foo.bar.memberName = <obfuscated-member-name>;
   * or constructor as:
   *  goog.provide('foo.bar.ClassSimpleName');
   *  foo.bar.ClassSimpleName = <obfuscated-ctor-name>;
   */
  @Override
  public void exportMember(JMember member) {
    // TODO(goktug): fix export namespace for constructor to be same as qualified export name.
    String namespace = member instanceof JConstructor ? member.getQualifiedExportName()
        : member.getExportNamespace();

    generateExport(namespace, member.getQualifiedExportName(), member, member.getSourceInfo());
  }

  private void generateExport(String exportNamespace, String qualifiedExportName, HasName nameRef,
      SourceInfo sourceInfo) {
    // goog.provide("a.b.c")
    ensureGoogProvide(exportNamespace, sourceInfo);
    // a.b.c = a_b_c_obf
    generateAssignment(nameRef, qualifiedExportName, sourceInfo);
  }

  private void ensureGoogProvide(String namespace, SourceInfo info) {
    if (!providedNamespaces.add(namespace) || namespace.isEmpty()) {
      return;
    }

    JsNameRef provideFuncRef = JsUtils.createQualifier("goog.provide", info);
    JsInvocation provideCall = new JsInvocation(info);
    provideCall.setQualifier(provideFuncRef);
    provideCall.getArguments().add(new JsStringLiteral(info, namespace));
    exportStmts.add(provideCall.makeStmt());
  }

  private void generateAssignment(HasName nameRef, String exportName, SourceInfo sourceInfo) {
    JsExpression lhs = createExportQualifier(exportName, sourceInfo);
    JsNameRef rhs = names.get(nameRef).makeRef(sourceInfo);
    exportStmts.add(JsUtils.createAssignment(lhs, rhs).makeStmt());
  }

  private static JsExpression createExportQualifier(String namespace, SourceInfo sourceInfo) {
    return JsUtils.createQualifier(namespace.isEmpty() ? "window" : namespace, sourceInfo);
  }
}
