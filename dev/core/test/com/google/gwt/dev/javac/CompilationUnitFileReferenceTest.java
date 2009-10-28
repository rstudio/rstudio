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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.javac.impl.MockResourceOracle;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test if all fileReferences in a compilationUnit are recorded correctly.
 */
public class CompilationUnitFileReferenceTest extends TestCase {

  public static final MockJavaResource MEMBER_INNER_SUBCLASS = new MockJavaResource(
      "test.OuterSubclass") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class OuterSubclass extends Outer {\n");
      code.append("  public String value() { return \"OuterSubclass\"; }\n");
      code.append("  public class MemberInnerSubclass extends MemberInner {\n");
      code.append("    public String value() { return \"MemberInnerSubclass\"; }\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource OUTER = new MockJavaResource(
      "test.Outer") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class Outer {\n");
      code.append("  public String value() { return \"Outer\"; }\n");
      code.append("  public static class StaticInner {\n");
      code.append("    public String value() { return \"StaticInner\"; }\n");
      code.append("  }\n");
      code.append("  public class MemberInner {\n");
      code.append("    public String value() { return \"MemberInner\"; }\n");
      code.append("  }\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource STATIC_INNER_SUBCLASS = new MockJavaResource(
      "test.StaticInnerSubclass") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class StaticInnerSubclass extends Outer.StaticInner {\n");
      code.append("  public String value() { return \"StaticInnerSubclass\"; }\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource TOP = new MockJavaResource("test.Top") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class Top {\n");
      code.append("  public String value() { return \"Top\"; }\n");
      code.append("}\n");
      code.append("class Top2 extends Top {\n");
      code.append("  public String value() { return \"Top2\"; }\n");
      code.append("}\n");
      return code;
    }
  };

  public static final MockJavaResource TOP3 = new MockJavaResource("test.Top3") {
    @Override
    protected CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package test;\n");
      code.append("public class Top3 extends Top2 {\n");
      code.append("  public String value() { return \"Top3\"; }\n");
      code.append("}\n");
      return code;
    }
  };

  /**
   * This map contains the hand-computed set of references we expect each of the
   * test compilation units to have. The actual set of references computed by
   * {@link CompilationState} will be checked against this set.
   */
  private static final Map<String, Set<String>> EXPECTED_DEPENDENCIES = new HashMap<String, Set<String>>();

  static {
    // Setup EXPECTED_DEPENDENCIES with hand-computed data.
    initializeExpectedDependency(JavaResourceBase.FOO, JavaResourceBase.STRING);
    initializeExpectedDependency(JavaResourceBase.BAR, JavaResourceBase.STRING,
        JavaResourceBase.FOO);

    // TOP has a self-reference
    initializeExpectedDependency(TOP, JavaResourceBase.STRING, TOP);
    initializeExpectedDependency(TOP3, JavaResourceBase.STRING, TOP);

    initializeExpectedDependency(OUTER, JavaResourceBase.STRING);
    initializeExpectedDependency(MEMBER_INNER_SUBCLASS,
        JavaResourceBase.STRING, OUTER);

    initializeExpectedDependency(OUTER, JavaResourceBase.STRING);
    initializeExpectedDependency(STATIC_INNER_SUBCLASS,
        JavaResourceBase.STRING, OUTER);
  }

  /**
   * Tweak this if you want to see the log output.
   */
  private static TreeLogger createTreeLogger() {
    boolean reallyLog = false;
    if (reallyLog) {
      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ALL);
      return logger;
    }
    return TreeLogger.NULL;
  }

  private static void initializeExpectedDependency(Resource source,
      Resource... targets) {
    Set<String> targetSet = new HashSet<String>();
    for (Resource target : targets) {
      targetSet.add(target.getLocation());
    }
    EXPECTED_DEPENDENCIES.put(source.getLocation(), targetSet);
  }

  private MockResourceOracle oracle = new MockResourceOracle(
      JavaResourceBase.getStandardResources());

  private CompilationState state = new CompilationState(createTreeLogger(),
      oracle);

  public void testBinaryBindingsWithMemberInnerClass() {
    testBinaryBindings(OUTER, MEMBER_INNER_SUBCLASS);
  }

  public void testBinaryBindingsWithMultipleTopLevelClasses() {
    testBinaryBindings(TOP, TOP3);
  }

  public void testBinaryBindingsWithSimpleUnits() {
    testBinaryBindings(JavaResourceBase.FOO, JavaResourceBase.BAR);
  }

  public void testBinaryBindingsWithStaticInnerClass() {
    testBinaryBindings(OUTER, STATIC_INNER_SUBCLASS);
  }

  public void testSourceBindingsWithMemberInnerClass() {
    testSourceBindings(OUTER, MEMBER_INNER_SUBCLASS);
  }

  public void testSourceBindingsWithMultipleTopLevelClasses() {
    testSourceBindings(TOP, TOP3);
  }

  public void testSourceBindingsWithSimpleUnits() {
    testSourceBindings(JavaResourceBase.FOO, JavaResourceBase.BAR);
  }

  public void testSourceBindingsWithStaticInnerClass() {
    testSourceBindings(OUTER, STATIC_INNER_SUBCLASS);
  }

  public void testWithGeneratedUnits() {
    state.addGeneratedCompilationUnits(createTreeLogger(),
        copyAsGeneratedUnits(JavaResourceBase.BAR, JavaResourceBase.FOO));
    assertRefsMatchExpectedRefs(JavaResourceBase.BAR, JavaResourceBase.FOO);
  }

  public void testWithMixedUnits() {
    oracle.add(JavaResourceBase.FOO);
    state.refresh(createTreeLogger());
    state.addGeneratedCompilationUnits(createTreeLogger(),
        copyAsGeneratedUnits(JavaResourceBase.BAR));
    assertRefsMatchExpectedRefs(JavaResourceBase.BAR, JavaResourceBase.FOO);
  }

  private void assertRefsMatchExpectedRefs(Resource... files) {
    for (Resource sourceFile : files) {
      Set<String> sourceFileRefs = state.getCompilationUnitMap().get(
          SourceFileCompilationUnit.getTypeName(sourceFile)).getFileNameRefs();
      Set<String> expectedSourceFileRefs = EXPECTED_DEPENDENCIES.get(sourceFile.getLocation());
      assertEquals(expectedSourceFileRefs, sourceFileRefs);
    }
  }

  /**
   * Returns copies of units as generated units for testing interactions with
   * generated units.
   */
  private Set<CompilationUnit> copyAsGeneratedUnits(Resource... sourceFiles) {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    for (Resource sourceFile : sourceFiles) {
      units.add(new SourceFileCompilationUnit(sourceFile) {
        @Override
        public boolean isGenerated() {
          return true;
        }
      });
    }
    return units;
  }

  /**
   * Independently compiles each file in order to force each subsequent unit to
   * have only binary references to the previous unit(s). This tests the binary
   * reference matching in {@link CompilationState}.
   */
  private void testBinaryBindings(Resource... files) {
    for (Resource sourceFile : files) {
      oracle.add(sourceFile);
      state.refresh(createTreeLogger());
    }
    assertRefsMatchExpectedRefs(files);
  }

  /**
   * Compiles all files together so that all units will have source references
   * to each other. This tests the source reference matching in
   * {@link CompilationState}.
   */
  private void testSourceBindings(Resource... files) {
    for (Resource sourceFile : files) {
      oracle.add(sourceFile);
    }
    state.refresh(createTreeLogger());
    assertRefsMatchExpectedRefs(files);
  }
}
