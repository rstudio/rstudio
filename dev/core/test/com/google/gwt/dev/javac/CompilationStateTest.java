/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dev.javac.Dependencies.Ref;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests {@link CompilationState}.
 */
public class CompilationStateTest extends CompilationStateTestBase {

  private static final MockJavaResource FOO_DIFF_API =
      new MockJavaResource("test.Foo") {
        @Override
        public CharSequence getContent() {
          StringBuffer code = new StringBuffer();
          code.append("package test;\n");
          code.append("public class Foo {\n");
          code.append("  public String value() { return \"Foo\"; }\n");
          code.append("  public String value2() { return \"Foo2\"; }\n");
          code.append("}\n");
          return code;
        }
      };

  private static final MockJavaResource FOO_SAME_API =
      new MockJavaResource("test.Foo") {
        @Override
        public CharSequence getContent() {
          StringBuffer code = new StringBuffer();
          code.append("package test;\n");
          code.append("public class Foo {\n");
          code.append("  public String value() { return \"Foo2\"; }\n");
          code.append("}\n");
          return code;
        }
      };

  private static String parseJs(String js) throws Exception {
    List<JsStatement> parsed =
        JsParser.parse(SourceOrigin.UNKNOWN, JsRootScope.INSTANCE,
            new StringReader(js));
    TextOutput text = new DefaultTextOutput(false);
    JsVisitor generator = new JsSourceGenerationVisitor(text);
    generator.acceptList(parsed);
    return text.toString();
  }

  public void testAddGeneratedCompilationUnit() {
    validateCompilationState();

    // Add a unit and ensure it shows up.
    addGeneratedUnits(JavaResourceBase.FOO);
    validateCompilationState(Shared.getTypeName(JavaResourceBase.FOO));

    rebuildCompilationState();
    validateCompilationState();
  }

  /* test that a generated unit, if unchanged, is reused */
  public void testCaching() {
    testCaching(JavaResourceBase.FOO);
  }

  /* test that multiple generated units, if unchanged, are reused */
  public void testCachingOfMultipleUnits() {
    testCaching(JavaResourceBase.BAR, JavaResourceBase.FOO);
  }

  public void testCompileError() {
    oracle.add(JavaResourceBase.BAR);
    rebuildCompilationState();

    CompilationUnit badUnit =
        state.getCompilationUnitMap().get(
            Shared.getTypeName(JavaResourceBase.BAR));
    assertTrue(badUnit.isError());

    Set<CompilationUnit> goodUnits =
        new HashSet<CompilationUnit>(state.getCompilationUnits());
    goodUnits.remove(badUnit);
    assertUnitsChecked(goodUnits);
  }

  public void testCompileWithGeneratedUnits() {
    assertUnitsChecked(state.getCompilationUnits());
    addGeneratedUnits(JavaResourceBase.FOO);
    assertUnitsChecked(state.getCompilationUnits());
  }

  public void testCompileWithGeneratedUnitsError() {
    assertUnitsChecked(state.getCompilationUnits());
    addGeneratedUnits(JavaResourceBase.BAR);

    CompilationUnit badUnit =
        state.getCompilationUnitMap().get(
            Shared.getTypeName(JavaResourceBase.BAR));
    assertTrue(badUnit.isError());

    Set<CompilationUnit> goodUnits =
        new HashSet<CompilationUnit>(state.getCompilationUnits());
    goodUnits.remove(badUnit);
    assertUnitsChecked(goodUnits);
  }

  public void testCompileWithGeneratedUnitsErrorAndDepedentGeneratedUnit() {
    assertUnitsChecked(state.getCompilationUnits());
    MockJavaResource badFoo =
        new MockJavaResource(Shared.getTypeName(JavaResourceBase.FOO)) {
          @Override
          public CharSequence getContent() {
            return "compilation error LOL!";
          }
        };
    oracle.add(badFoo);
    rebuildCompilationState();
    addGeneratedUnits(JavaResourceBase.BAR);

    CompilationUnit badUnit =
        state.getCompilationUnitMap().get(Shared.getTypeName(badFoo));
    assertTrue(badUnit.isError());
    CompilationUnit invalidUnit =
        state.getCompilationUnitMap().get(
            Shared.getTypeName(JavaResourceBase.BAR));
    assertTrue(invalidUnit.isError());

    Set<CompilationUnit> goodUnits =
        new HashSet<CompilationUnit>(state.getCompilationUnits());
    goodUnits.remove(badUnit);
    goodUnits.remove(invalidUnit);
    assertUnitsChecked(goodUnits);
  }

  /*
   * test if things work correctly when a generated unit can't be reused, but
   * another generated unit it depends on can be reused
   */
  public void testComplexCacheInvalidation() {
    testCachingOverMultipleRefreshes(new MockJavaResource[]{
        JavaResourceBase.FOO, JavaResourceBase.BAR},
        new MockJavaResource[]{
            JavaResourceBase.FOO,
            new TweakedMockJavaResource(JavaResourceBase.BAR)},
        Collections.singleton(JavaResourceBase.FOO.getTypeName()));

  }

  public void testInitialization() {
    assertUnitsChecked(state.getCompilationUnits());
  }

  public void testInvalidation() {
    testCachingOverMultipleRefreshes(
        new MockJavaResource[]{JavaResourceBase.FOO},
        new MockJavaResource[]{new TweakedMockJavaResource(JavaResourceBase.FOO)},
        Collections.<String> emptySet());
  }

  public void testInvalidationNonstructuralDep() {
    testCachingOverMultipleRefreshes(new MockJavaResource[]{
        JavaResourceBase.FOO, JavaResourceBase.BAR}, new MockJavaResource[]{
        FOO_SAME_API, JavaResourceBase.BAR},
        Collections.singleton(JavaResourceBase.BAR.getTypeName()));
  }

  public void testInvalidationOfMultipleUnits() {
    testCachingOverMultipleRefreshes(new MockJavaResource[]{
        JavaResourceBase.FOO, JavaResourceBase.BAR}, new MockJavaResource[]{
        new TweakedMockJavaResource(JavaResourceBase.FOO),
        new TweakedMockJavaResource(JavaResourceBase.BAR)},
        Collections.<String> emptySet());
  }

  public void testInvalidationStructuralDep() {
    testCachingOverMultipleRefreshes(new MockJavaResource[]{
        JavaResourceBase.FOO, JavaResourceBase.BAR}, new MockJavaResource[]{
        FOO_DIFF_API, JavaResourceBase.BAR}, Collections.<String> emptySet());
  }

  public void testInvalidationWhenSourceUnitsChange() {
    /*
     * Steps: (i) Check compilation state. (ii) Add generated units. (iii)
     * Change unit in source oracle. (iv) Refresh oracle. (v) Add same generated
     * units. (v) Check that there is no reuse.
     */
    validateCompilationState();
    oracle.add(JavaResourceBase.FOO);
    rebuildCompilationState();

    // add generated units
    addGeneratedUnits(JavaResourceBase.BAR);
    assertUnitsChecked(state.getCompilationUnits());
    CompilationUnit oldBar =
        state.getCompilationUnitMap().get(JavaResourceBase.BAR.getTypeName());
    assertNotNull(oldBar);

    // change unit in source oracle
    oracle.replace(FOO_DIFF_API);
    rebuildCompilationState();

    /*
     * Add same generated units. Verify that the original units are not used.
     */
    addGeneratedUnits(JavaResourceBase.BAR);
    assertUnitsChecked(state.getCompilationUnits());

    CompilationUnit newBar =
        state.getCompilationUnitMap().get(JavaResourceBase.BAR.getTypeName());
    assertNotNull(newBar);
    assertNotSame(oldBar, newBar);
  }

  public void testMethodArgs() {
    MockJavaResource resource = new MockJavaResource("test.MethodArgsTest") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public abstract class MethodArgsTest {\n");
        code.append("  public abstract void anAbstractMethod(String aArg1);\n");
        code.append("  public native void aNativeMethod(String nArg1, int nArg2);\n");
        code.append("  public void aMethod(String mArg1, int mArg2, char mArg3) {}\n");
        code.append("  public void aNoArgMethod() {}\n");
        code.append("}\n");
        return code;
      }
    };

    validateCompilationState();
    oracle.add(resource);
    rebuildCompilationState();
    validateCompilationState();
    Map<String, CompilationUnit> unitMap = state.getCompilationUnitMap();
    MethodArgNamesLookup methodArgs =
        unitMap.get(Shared.getTypeName(resource)).getMethodArgs();
    String[] methods = methodArgs.getMethods();
    assertEquals(3, methods.length);
    Arrays.sort(methods);
    assertEquals("test.MethodArgsTest.aMethod(Ljava/lang/String;IC)V",
        methods[0]);
    assertEquals("test.MethodArgsTest.aNativeMethod(Ljava/lang/String;I)V",
        methods[1]);
    assertEquals("test.MethodArgsTest.anAbstractMethod(Ljava/lang/String;)V",
        methods[2]);

    String[] names;
    names = methodArgs.lookup(methods[0]);
    assertEquals(3, names.length);
    assertEquals("mArg1", names[0]);
    assertEquals("mArg2", names[1]);
    assertEquals("mArg3", names[2]);

    names = methodArgs.lookup(methods[1]);
    assertEquals(2, names.length);
    assertEquals("nArg1", names[0]);
    assertEquals("nArg2", names[1]);

    names = methodArgs.lookup(methods[2]);
    assertEquals(1, names.length);
    assertEquals("aArg1", names[0]);
  }

  public void testSerializeCompilationUnit() throws Exception {

    MockJavaResource resource = new MockJavaResource("test.SerializationTest") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("public abstract class SerializationTest {\n");
        code.append("  public static native boolean getTrue() /*-{ return true; }-*/;\n");
        code.append("  public abstract String methodArgsTest(int arg1, Object arg2);");
        code.append("  public final String toString() { return \"SerializationTest\"; }\n");
        code.append("}\n");
        return code;
      }
    };

    String resourceTypeName = Shared.getTypeName(resource);
    validateCompilationState();
    oracle.add(resource);
    rebuildCompilationState();
    validateCompilationState();

    Map<String, CompilationUnit> unitMap = state.getCompilationUnitMap();
    validateSerializedTestUnit(resource, unitMap.get(resourceTypeName));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    for (CompilationUnit unit : unitMap.values()) {
      Util.writeObjectToStream(outputStream, unitMap.get(unit.getTypeName()));
    }

    int numUnits = unitMap.size();
    byte[] streamData = outputStream.toByteArray();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(streamData);
    Map<String, CompilationUnit> newUnitMap =
        new HashMap<String, CompilationUnit>();
    for (int i = 0; i < numUnits; i++) {
      CompilationUnit unit =
          Util.readStreamAsObject(inputStream, CompilationUnit.class);
      newUnitMap.put(unit.getTypeName(), unit);
    }

    // Validate all deserialized units against the original units
    assertEquals(unitMap.size(), newUnitMap.size());
    for (CompilationUnit unit : unitMap.values()) {
      CompilationUnit newUnit = newUnitMap.get(unit.getTypeName());
      validateSerializedUnit(unit, newUnit);
    }

    // Validate the SerializationTest resource with a specific test.
    validateSerializedTestUnit(resource, newUnitMap.get(resourceTypeName));
  }

  public void testSourceOracleAdd() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.add(JavaResourceBase.FOO);
    rebuildCompilationState();
    assertEquals(size + 1, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleBasic() {
    validateCompilationState();
  }

  public void testSourceOracleEmpty() {
    oracle = new MockResourceOracle();
    rebuildCompilationState();
    validateCompilationState();
  }

  public void testSourceOracleRemove() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.remove(JavaResourceBase.MAP.getPath());
    rebuildCompilationState();
    assertEquals(size - 1, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleReplace() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.replace(new TweakedMockJavaResource(JavaResourceBase.OBJECT));
    rebuildCompilationState();
    assertEquals(size, state.getCompilationUnits().size());
    validateCompilationState();
  }

  public void testSourceOracleReplaceWithSame() {
    validateCompilationState();

    int size = state.getCompilationUnits().size();
    oracle.replace(JavaResourceBase.OBJECT);
    rebuildCompilationState();
    assertEquals(size, state.getCompilationUnits().size());
    validateCompilationState();
  }

  private void testCaching(MockJavaResource... resources) {
    Set<String> reusedTypes = new HashSet<String>();
    for (MockJavaResource resource : resources) {
      reusedTypes.add(resource.getTypeName());
    }
    testCachingOverMultipleRefreshes(resources, resources, reusedTypes);
  }

  /**
   * Test caching logic for generated units during refreshes. Steps:
   * <ol>
   * <li>Verify that there were no generated units before</li>
   * <li>Add 'initialSet' generatedUnits over a refresh cycle</li>
   * <li>Add 'updatedSet' generatedUnits over a refresh cycle</li>
   * <li>Add 'updatedSet' generatedUnits over the second refresh cycle</li>
   * </ol>
   * 
   * @param initialSet CompilationUnits that are generated the first time.
   * @param updatedSet CompilationUnits that are generated the next time.
   * @param reusedTypes Main type of the units that can be reused between the
   *          initialSet and updatedSet.
   * @param numInvalidated Number of types invalidated from graveyardUnits.
   */
  private void testCachingOverMultipleRefreshes(MockJavaResource[] initialSet,
      MockJavaResource[] updatedSet, Set<String> reusedTypes) {

    // Add 'initialSet' generatedUnits on the first cycle.
    rebuildCompilationState();
    assertEquals(oracle.getResources().size(),
        state.getCompilationUnits().size());
    addGeneratedUnits(initialSet);
    Map<String, CompilationUnit> units1 =
        new HashMap<String, CompilationUnit>(state.getCompilationUnitMap());
    assertEquals(oracle.getResources().size() + initialSet.length,
        units1.size());
    assertUnitsChecked(units1.values());

    // Add 'updatedSet' generatedUnits on the second cycle.
    rebuildCompilationState();
    assertEquals(oracle.getResources().size(),
        state.getCompilationUnits().size());
    addGeneratedUnits(updatedSet);
    Map<String, CompilationUnit> units2 =
        new HashMap<String, CompilationUnit>(state.getCompilationUnitMap());
    assertEquals(oracle.getResources().size() + updatedSet.length,
        units2.size());
    assertUnitsChecked(units2.values());

    // Validate that only 'reusedTypes' are reused.
    for (MockJavaResource resource : updatedSet) {
      String typeName = resource.getTypeName();
      if (reusedTypes.contains(typeName)) {
        assertSame(units1.get(typeName), units2.get(typeName));
      } else {
        assertNotSame(units1.get(typeName), units2.get(typeName));
      }
    }

    // Add 'updatedSet' generatedUnits on the third cycle.
    rebuildCompilationState();
    assertEquals(oracle.getResources().size(),
        state.getCompilationUnits().size());
    addGeneratedUnits(updatedSet);
    Map<String, CompilationUnit> units3 =
        new HashMap<String, CompilationUnit>(state.getCompilationUnitMap());
    assertEquals(oracle.getResources().size() + updatedSet.length,
        units3.size());
    assertUnitsChecked(units3.values());

    // Validate that all generatedUnits are reused.
    for (MockJavaResource resource : updatedSet) {
      String typeName = resource.getTypeName();
      assertSame(units2.get(typeName), units3.get(typeName));
    }
  }

  private void validateSerializedTestUnit(MockJavaResource resource,
      CompilationUnit unit) throws Exception {
    assertNotNull(unit);
    assertEquals(resource.getLastModified(), unit.getLastModified());

    // dependencies
    Dependencies deps = unit.getDependencies();
    assertObjectInDeps(deps.qualified, deps.simple, "java.lang.Object",
        "Object");
    assertObjectInDeps(deps.qualified, deps.simple, "java.lang.String",
        "String");
    assertObjectInDeps(deps.qualified, deps.simple, "test.SerializationTest",
        "SerializationTest");

    // method args lookup
    MethodArgNamesLookup lookup = unit.getMethodArgs();
    String methods[] = lookup.getMethods();
    assertEquals(1, methods.length);
    assertEquals(
        "test.SerializationTest.methodArgsTest(ILjava/lang/Object;)Ljava/lang/String;",
        methods[0]);

    // JSNI methods
    List<JsniMethod> jsniMethods = unit.getJsniMethods();
    assertEquals(1, jsniMethods.size());
    JsniMethod jsniMethod = jsniMethods.get(0);
    String[] paramNames = jsniMethod.paramNames();
    assertEquals(0, paramNames.length);
    assertEquals("@test.SerializationTest::getTrue()", jsniMethod.name());
    JsFunction jsniFunction = jsniMethod.function();
    String origFunction = parseJs("function() { return true; }");
    String newFunction = parseJs("function() " + jsniFunction.getBody().toSource());
    assertEquals(origFunction, newFunction);

    // Compiled classes
    List<CompiledClass> compiledClasses =
        Lists.create(unit.getCompiledClasses());
    assertEquals(1, compiledClasses.size());
    CompiledClass compiledClass = compiledClasses.get(0);
    byte[] byteCode = compiledClass.getBytes();
    assertNotNull(byteCode);
    assertEquals("test", compiledClass.getPackageName());
    assertEquals("test/SerializationTest", compiledClass.getInternalName());
    assertSame(unit, compiledClass.getUnit());
  }

  private void assertObjectInDeps(Map<String, Ref> qualified,
      Map<String, Ref> simple, String qualifiedName, String simpleName) {
    Ref qualifiedRef = qualified.get(qualifiedName);
    assertNotNull(qualifiedRef);
    assertEquals(qualifiedName, qualifiedRef.getInternalName().replace("/", "."));
    Ref simpleRef = simple.get(simpleName);
    assertNotNull(simpleRef);
    assertEquals(qualifiedName,simpleRef.getInternalName().replace("/", "."));
  }

  private void validateSerializedUnit(CompilationUnit originalUnit,
      CompilationUnit newUnit) throws Exception {

    // Compare the compiled classes
    Map<String, CompiledClass> origMap = new HashMap<String, CompiledClass>();
    for (CompiledClass cc : originalUnit.getCompiledClasses()) {
      origMap.put(cc.getInternalName(), cc);
    }
    Map<String, CompiledClass> newMap = new HashMap<String, CompiledClass>();
    for (CompiledClass cc : newUnit.getCompiledClasses()) {
      newMap.put(cc.getInternalName(), cc);
    }
    assertEquals(origMap.size(), newMap.size());
    for (String name : origMap.keySet()) {
      assertEquals(name, newMap.get(name).getInternalName());
    }

    // Compare the dependencies
    Map<String, Ref> origQualified = originalUnit.getDependencies().qualified;
    Map<String, Ref> newQualified = newUnit.getDependencies().qualified;
    for (String name : origQualified.keySet()) {
      Ref origRef = origQualified.get(name);
      Ref newRef = newQualified.get(name);
      if (origRef == null) {
        assertNull(newRef);
      } else {
        assertNotNull(newRef);
        assertEquals(origRef.getSignatureHash(), newRef.getSignatureHash());
      }
    }

    // Compare JSNI Methods
    List<JsniMethod> origJsniMethods = originalUnit.getJsniMethods();
    Map<String, JsniMethod> newJsniMethods = new HashMap<String, JsniMethod>();
    for (JsniMethod jsniMethod : newUnit.getJsniMethods()) {
      newJsniMethods.put(jsniMethod.name(), jsniMethod);
    }
    for (JsniMethod origMethod : origJsniMethods) {
      JsniMethod newMethod = newJsniMethods.get(origMethod.name());
      assertNotNull(newMethod);
      assertEquals(origMethod.paramNames().length,
          newMethod.paramNames().length);
      for (int i = 0; i < origMethod.paramNames().length; i++) {
        assertEquals(origMethod.paramNames()[i], newMethod.paramNames()[i]);
      }
      assertEquals(parseJs("function() " + origMethod.function().getBody()),
          parseJs("function() " + newMethod.function().getBody()));
      // Need to test deserialization of origMethod.function()?
    }
  }

  /**
   * Java resource that modifies its content with a terminating newline.
   */
  public static class TweakedMockJavaResource extends MockJavaResource {

    private MockJavaResource original;

    public TweakedMockJavaResource(MockJavaResource original) {
      super(original.getTypeName());
      this.original = original;
    }

    @Override
    public CharSequence getContent() {
      return original.getContent() + "\n";
    }
  }
}
