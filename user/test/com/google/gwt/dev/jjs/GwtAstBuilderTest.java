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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompileModule;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Massive test for {@link com.google.gwt.dev.jjs.impl.GwtAstBuilder}.
 */
public class GwtAstBuilderTest extends TestCase {
  /*
   * Reuse the module and compilation state between tests, because it takes a
   * long time to build them. This is fine as long as we don't mutate them.
   */

  private static CompilationState compilationState;
  private static PrintWriterTreeLogger logger;
  private static ModuleDef module;

  private static synchronized CompilationState getCompilationState()
      throws UnableToCompleteException {
    if (compilationState == null) {
      compilationState = CompileModule.buildGwtAst(getLogger(), getTestModule());
    }
    return compilationState;
  }

  private static synchronized TreeLogger getLogger() {
    if (logger == null) {
      logger = new PrintWriterTreeLogger(new PrintWriter(System.err, true));
      logger.setMaxDetail(TreeLogger.ERROR);
    }
    return logger;
  }

  private static synchronized ModuleDef getTestModule() throws UnableToCompleteException {
    if (module == null) {
      module =
          ModuleDefLoader.createSyntheticModule(getLogger(),
              "com.google.gwt.dev.jjs.CompilerSuite.GwtAstBuilderTest", new String[]{
                  "com.google.gwt.junit.JUnit", "com.google.gwt.dev.jjs.CompilerSuite"}, false);
    }
    return module;
  }

  /**
   * Tests source compatibility between
   * {@link com.google.gwt.dev.jjs.impl.GwtAstBuilder} and
   * {@link com.google.gwt.dev.jjs.impl.GenerateJavaAST}.
   */
  public void testGwtAstBuilder() throws UnableToCompleteException {
    CompilationState compilationState = getCompilationState();
    assertFalse(compilationState.hasErrors());
    JProgram jprogram =
        CompileModule.buildGenerateJavaAst(getLogger(), getTestModule(), compilationState);

    Map<String, JDeclaredType> compStateTypes = new HashMap<String, JDeclaredType>();
    for (CompilationUnit unit : compilationState.getCompilationUnits()) {
      for (JDeclaredType type : unit.getTypes()) {
        compStateTypes.put(type.getName(), type);
      }
    }

    for (JDeclaredType genJavaAstType : jprogram.getDeclaredTypes()) {
      String typeName = genJavaAstType.getName();
      if ("com.google.gwt.core.client.JavaScriptObject".equals(typeName)) {
        // Known mismatch; genJavaAst version implements all JSO interfaces.
        continue;
      }
      if (typeName.startsWith("com.google.gwt.lang.asyncloaders")) {
        // GwtAstBuilder doesn't build these; added later.
        continue;
      }
      if ("com.google.gwt.dev.jjs.test.B$1".equals(typeName)) {
        // Known mismatch; genJavaAst is "wrong".
        continue;
      }
      if (typeName.startsWith("com.google.gwt.dev.jjs.test.CoverageTest$Inner$1")) {
        // Known mismatch; two different emulation paths do the same thing.
        continue;
      }
      JDeclaredType compStateType = compStateTypes.get(typeName);
      assertNotNull("No matching prebuilt type for '" + typeName + "'", compStateType);
      String oldSource = genJavaAstType.toSource();
      String newSource = compStateType.toSource();
      assertEquals("Mismatched output for '" + typeName + "'", oldSource, newSource);
    }
  }

  /**
   * Test that serialization doesn't crash and produces the same source tree.
   */
  public void testSerialization() throws UnableToCompleteException, IOException,
      ClassNotFoundException {
    CompilationState compilationState = getCompilationState();
    assertFalse(compilationState.hasErrors());
    for (CompilationUnit unit : compilationState.getCompilationUnits()) {
      Map<String, JDeclaredType> compStateTypes = new HashMap<String, JDeclaredType>();
      for (JDeclaredType type : unit.getTypes()) {
        compStateTypes.put(type.getName(), type);
      }
      byte[] bytes = unit.getTypesSerialized();
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      List<JDeclaredType> deserializedTypes = JProgram.deserializeTypes(ois);
      assertEquals(compStateTypes.size(), deserializedTypes.size());
      for (JDeclaredType deserializedType : deserializedTypes) {
        String typeName = deserializedType.getName();
        JDeclaredType compStateType = compStateTypes.get(typeName);
        assertNotNull("No matching prebuilt type for '" + typeName + "'", compStateType);
        String oldSource = compStateType.toSource();
        String newSource = deserializedType.toSource();
        assertEquals("Mismatched output for '" + typeName + "'", oldSource, newSource);
      }
    }
  }
}
