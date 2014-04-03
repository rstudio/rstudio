/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.arg.OptionCheckedMode;

/**
 * Verifies that we can convert final methods to static methods,
 * optionally with null checks.
 */
public class MakeCallsStaticTest extends OptimizerTestBase {

  private boolean addNullCheck = false;

  public void testMakeStaticUnchecked() throws Exception {
    addSnippetClassDecl("final String answer() { return \"42\"; }");
    optimize("String", "return new EntryPoint().answer();")
        .intoString("return EntryPoint.$answer(new EntryPoint());");
  }

  public void testMakeStaticChecked() throws Exception {
    addNullCheck = true;
    addSnippetClassDecl("final String answer() { return \"42\"; }");
    optimize("String", "return new EntryPoint().answer();")
        .intoString("return EntryPoint.$answer(Exceptions.checkNotNull(new EntryPoint()));");
  }

  public void testUnstatifiableMethod() throws Exception {
    addSnippetClassDecl("final String answer() { return \"42\"; }");
    optimize("String",
        "return com.google.gwt.core.client.impl.Impl.getNameOf(\"@test.EntryPoint::answer()\");")
        .intoString("return /* JNameOf */\"answer\";");
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    OptionCheckedMode option = new OptionCheckedMode() {
      @Override
      public boolean shouldAddRuntimeChecks() {
        return addNullCheck;
      }

      @Override
      public void setAddRuntimeChecks(boolean enabled) {
        throw new UnsupportedOperationException();
      }
    };

    return MakeCallsStatic.exec(option, program).didChange();
  }
}
