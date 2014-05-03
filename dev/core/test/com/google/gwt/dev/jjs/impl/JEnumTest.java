/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JEnumField;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JProgram;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests AST setup of Enums.
 */
public class JEnumTest extends JJSTestBase {

  @Override
  public void setUp() {
    sourceOracle.addOrReplace(new MockJavaResource("test.Simple") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public enum Simple {\n");
        code.append("  FOO, BAR, BAZ;\n");
        code.append("}\n");
        return code;
      }
    });
  }

  public void testBasic() throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "test.Simple.FOO.toString();");

    JDeclaredType simple = findDeclaredType(program, "test.Simple");
    Set<JEnumField> enumFields = new HashSet<JEnumField>();
    for (JField field : simple.getFields()) {
      if (field instanceof JEnumField) {
        enumFields.add((JEnumField) field);
      }
    }
    assertEquals(3, enumFields.size());
  }
}
