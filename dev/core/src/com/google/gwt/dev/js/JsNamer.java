/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.dev.cfg.ConfigProps;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameOf;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.HashSet;
import java.util.Set;

/**
 * A class that allocates unique identifiers for JsNames.
 */
public abstract class JsNamer {

  private static Set<JsName> collectReferencedNames(JsProgram program) {
    final Set<JsName> referenced = new HashSet<JsName>();
    new JsVisitor() {
      @Override
      public void endVisit(JsForIn x, JsContext ctx) {
        reference(x.getIterVarName());
      }

      @Override
      public void endVisit(JsFunction x, JsContext ctx) {
        reference(x.getName());
      }

      @Override
      public void endVisit(JsLabel x, JsContext ctx) {
        reference(x.getName());
      }

      @Override
      public void endVisit(JsNameOf x, JsContext ctx) {
        reference(x.getName());
      }

      @Override
      public void endVisit(JsNameRef x, JsContext ctx) {
        reference(x.getName());
      }

      @Override
      public void endVisit(JsParameter x, JsContext ctx) {
        reference(x.getName());
      }

      @Override
      public void endVisit(JsVars.JsVar x, JsContext ctx) {
        reference(x.getName());
      }

      private void reference(JsName name) {
        if (name != null) {
          referenced.add(name);
        }
      }
    }.accept(program);
    return referenced;
  }

  protected final JsProgram program;

  protected final Set<JsName> referenced;

  protected final ReservedNames reserved;

  public JsNamer(JsProgram program, ConfigProps config) {
    this.program = program;
    referenced = collectReferencedNames(program);
    reserved = new ReservedNames(config);
  }

  protected final void execImpl() {
    reset();
    visit(program.getScope());
    reset();
    visit(program.getObjectScope());
  }

  protected abstract void reset();

  protected abstract void visit(JsScope scope);
}
