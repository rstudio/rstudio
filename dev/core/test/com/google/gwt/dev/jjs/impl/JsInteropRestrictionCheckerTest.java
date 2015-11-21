/*
 * Copyright 2015 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for the JsInteropRestrictionChecker.
 */
public class JsInteropRestrictionCheckerTest extends OptimizerTestBase {
  @Override
  public void setUp() {
    sourceLevel = SourceLevel.JAVA8;
  }

  // TODO: eventually test this for default methods in Java 8.
  public void testCollidingAccidentalOverrideConcreteMethodFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Foo {",
        "  void doIt(Foo foo);",
        "}",
        "@JsType",
        "public static interface Bar {",
        "  void doIt(Bar bar);",
        "}",
        "public static class ParentBuggy {",
        "  public void doIt(Foo foo) {}",
        "  public void doIt(Bar bar) {}",
        "}",
        "public static class Buggy extends ParentBuggy implements Foo, Bar {",
        "}");

    assertBuggyFails(
        "Line 14: 'void EntryPoint.ParentBuggy.doIt(EntryPoint.Bar)' "
            + "(exposed by 'EntryPoint.Buggy') and "
            + "'void EntryPoint.ParentBuggy.doIt(EntryPoint.Foo)' (exposed by 'EntryPoint.Buggy') "
            + "cannot both use the same JavaScript name 'doIt'.");
  }

  public void testCollidingAccidentalOverrideAbstractMethodFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Foo {",
        "  void doIt(Foo foo);",
        "}",
        "@JsType",
        "public static interface Bar {",
        "  void doIt(Bar bar);",
        "}",
        "public static abstract class Baz implements Foo, Bar {",
        "  public abstract void doIt(Foo foo);",
        "  public abstract void doIt(Bar bar);",
        "}",
        "public static class Buggy {}  // Unrelated class");

    assertBuggyFails(
        "Line 14: 'void EntryPoint.Baz.doIt(EntryPoint.Bar)' and "
            + "'void EntryPoint.Baz.doIt(EntryPoint.Foo)' cannot both use the same "
            + "JavaScript name 'doIt'.");
  }

  public void testCollidingAccidentalOverrideHalfAndHalfFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static interface Foo {",
        "  void doIt(Foo foo);",
        "}",
        "@JsType",
        "public static interface Bar {",
        "   void doIt(Bar bar);",
        "}",
        "public static class ParentParent {",
        "  public void doIt(Bar x) {}",
        "}",
        "@JsType",
        "public static class Parent extends ParentParent {",
        "  public void doIt(Foo x) {}",
        "}",
        "public static class Buggy extends Parent implements Bar {}");

    assertBuggyFails(
        "Line 12: 'void EntryPoint.ParentParent.doIt(EntryPoint.Bar)' "
            + "(exposed by 'EntryPoint.Buggy') and 'void EntryPoint.Parent.doIt(EntryPoint.Foo)' "
            + "cannot both use the same JavaScript name 'doIt'.");
  }

  public void testCollidingFieldExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
       "public static class Buggy {",
        "  @JsProperty",
        "  public static final int show = 0;",
        "  @JsProperty(name = \"show\")",
        "  public static final int display = 0;",
        "}");

    assertBuggyFails(
        "Line 8: 'int EntryPoint.Buggy.display' cannot be exported because the global "
            + "name 'test.EntryPoint.Buggy.show' is already taken by "
            + "'int EntryPoint.Buggy.show'.");
  }

  public void testJsPropertyNonGetterStyleSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty(name = \"x\") int x();",
        "  @JsProperty(name = \"x\") void x(int x);",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyGetterStyleSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty static int getStaticX(){ return 0;}",
        "  @JsProperty static void setStaticX(int x){}",
        "  @JsProperty int getX();",
        "  @JsProperty void setX(int x);",
        "  @JsProperty boolean isY();",
        "  @JsProperty void setY(boolean y);",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyIncorrectGetterStyleFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty int isX();",
        "  @JsProperty int getY(int x);",
        "  @JsProperty void getZ();",
        "  @JsProperty void setX(int x, int y);",
        "  @JsProperty void setY();",
        "  @JsProperty int setZ(int z);",
        "  @JsProperty static void setStatic(){}",
        "}");

    assertBuggyFails(
        "Line 7: JsProperty 'int EntryPoint.Buggy.isX()' cannot have a non-boolean return.",
        "Line 8: JsProperty 'int EntryPoint.Buggy.getY(int)' should have a correct setter "
            + "or getter signature.",
        "Line 9: JsProperty 'void EntryPoint.Buggy.getZ()' should have a correct setter "
            + "or getter signature.",
        "Line 10: JsProperty 'void EntryPoint.Buggy.setX(int, int)' should have a correct setter "
            + "or getter signature.",
        "Line 11: JsProperty 'void EntryPoint.Buggy.setY()' should have a correct setter "
            + "or getter signature.",
        "Line 12: JsProperty 'int EntryPoint.Buggy.setZ(int)' should have a correct setter "
            + "or getter signature.",
        "Line 13: JsProperty 'void EntryPoint.Buggy.setStatic()' should have a correct setter "
            + "or getter signature.");
  }

  public void testJsPropertyNonGetterStyleFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty boolean hasX();",
        "  @JsProperty int x();",
        "  @JsProperty void x(int x);",
        "}");

    assertBuggyFails(
        "Line 7: JsProperty 'boolean EntryPoint.Buggy.hasX()' should either follow Java Bean "
        + "naming conventions or provide a name.",
        "Line 8: JsProperty 'int EntryPoint.Buggy.x()' should either follow Java Bean "
        + "naming conventions or provide a name.",
        "Line 9: JsProperty 'void EntryPoint.Buggy.x(int)' should either follow Java Bean "
        + "naming conventions or provide a name.");
  }

  public void testCollidingJsPropertiesTwoGettersFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Buggy {",
        "  @JsProperty",
        "  boolean isX();",
        "  @JsProperty",
        "  boolean getX();",
        "}");

    assertBuggyFails(
        "Line 10: 'boolean EntryPoint.Buggy.getX()' and 'boolean EntryPoint.Buggy.isX()' "
            + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsPropertiesTwoSettersFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Buggy {",
        "  @JsProperty",
        "  void setX(boolean x);",
        "  @JsProperty",
        "  void setX(int x);",
        "}");

    assertBuggyFails(
        "Line 10: 'void EntryPoint.Buggy.setX(int)' and 'void EntryPoint.Buggy.setX(boolean)' "
            + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsMethodAndJsPropertyGetterFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static interface IBuggy {",
        "  @JsMethod",
        "  boolean x(boolean foo);",
        "  @JsProperty",
        "  int getX();",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean x(boolean foo) {return false;}",
        "  public int getX() {return 0;}",
        "}");

    assertBuggyFails(
        "Line 9: 'int EntryPoint.IBuggy.getX()' and 'boolean EntryPoint.IBuggy.x(boolean)' "
            + "cannot both use the same JavaScript name 'x'.",
        "Line 13: 'int EntryPoint.Buggy.getX()' and 'boolean EntryPoint.Buggy.x(boolean)' "
            + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsMethodAndJsPropertySetterFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static interface IBuggy {",
        "  @JsMethod",
        "  boolean x(boolean foo);",
        "  @JsProperty",
        "  void setX(int a);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean x(boolean foo) {return false;}",
        "  public void setX(int a) {}",
        "}");

    assertBuggyFails(
        "Line 9: 'void EntryPoint.IBuggy.setX(int)' and 'boolean EntryPoint.IBuggy.x(boolean)' "
            + "cannot both use the same JavaScript name 'x'.",
        "Line 13: 'void EntryPoint.Buggy.setX(int)' and 'boolean EntryPoint.Buggy.x(boolean)' "
            + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingPropertyAccessorExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsProperty",
        "  public static void setDisplay(int x) {}",
        "  @JsProperty(name = \"display\")",
        "  public static void setDisplay2(int x) {}",
        "}");

    assertBuggyFails(
        "Line 8: 'void EntryPoint.Buggy.setDisplay2(int)' cannot be exported because the global "
            + "name 'test.EntryPoint.Buggy.display' is already taken "
            + "by 'void EntryPoint.Buggy.setDisplay(int)'.");
  }

  public void testCollidingMethodExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod",
        "  public static void show() {}",
        "  @JsMethod(name = \"show\")",
        "  public static void display() {}",
        "}");

    assertBuggyFails(
        "Line 8: 'void EntryPoint.Buggy.display()' cannot be exported because the global name "
            + "'test.EntryPoint.Buggy.show' is already taken "
            + "by 'void EntryPoint.Buggy.show()'.");
  }

  public void testCollidingMethodToPropertyAccessorExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsProperty",
        "  public static void setShow(int x) {}",
        "  @JsMethod",
        "  public static void show() {}",
        "}");

    assertBuggyFails(
        "Line 9: 'void EntryPoint.Buggy.show()' cannot be exported because the global name "
            + "'test.EntryPoint.Buggy.show' is already taken by "
            + "'void EntryPoint.Buggy.setShow(int)'.");
  }

  public void testCollidingMethodToFieldExportsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod",
        "  public static void show() {}",
        "  @JsProperty",
        "  public static final int show = 0;",
        "}");

    assertBuggyFails(
        "Line 7: 'void EntryPoint.Buggy.show()' cannot be exported because the global name "
            + "'test.EntryPoint.Buggy.show' is already taken by "
            + "'int EntryPoint.Buggy.show'.");
  }

  public void testCollidingMethodToFieldJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show() {}",
        "  public final int show = 0;",
        "}");

    assertBuggyFails(
        "Line 6: 'void EntryPoint.Buggy.show()' and 'int EntryPoint.Buggy.show' cannot both use "
            + "the same JavaScript name 'show'.");
  }

  public void testCollidingMethodToMethodJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show(int x) {}",
        "  public void show() {}",
        "}");

    assertBuggyFails(
        "Line 7: 'void EntryPoint.Buggy.show()' and 'void EntryPoint.Buggy.show(int)' cannot both "
            + "use the same JavaScript name 'show'.");
  }

  public void testCollidingSubclassExportedFieldToFieldJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassExportedFieldToMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassExportedMethodToMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassFieldToExportedFieldJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassFieldToExportedMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassFieldToFieldJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggyFails(
        "Line 10: 'int EntryPoint.Buggy.foo' and 'int EntryPoint.ParentBuggy.foo' cannot both use "
            + "the same JavaScript name 'foo'.");
  }

  public void testCollidingSubclassFieldToMethodJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggyFails(
        "Line 10: 'void EntryPoint.Buggy.foo(int)' and 'int EntryPoint.ParentBuggy.foo' cannot "
            + "both use the same JavaScript name 'foo'.");
  }

  public void testCollidingSubclassMethodToExportedMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingSubclassMethodToMethodInterfaceJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public interface IBuggy1 {",
        "  void show();",
        "}",
        "@JsType",
        "public interface IBuggy2 {",
        "  void show(boolean b);",
        "}",
        "public static class Buggy implements IBuggy1 {",
        "  public void show() {}",
        "}",
        "public static class Buggy2 extends Buggy implements IBuggy2 {",
        "  public void show(boolean b) {}",
        "}");

    assertBuggyFails(
        "Line 16: 'void EntryPoint.Buggy2.show(boolean)' and 'void EntryPoint.Buggy.show()' cannot "
            + "both use the same JavaScript name 'show'.");
  }

  public void testCollidingSubclassMethodToMethodJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public void foo(int a) {}",
        "}");

    assertBuggyFails(
        "Line 10: 'void EntryPoint.Buggy.foo(int)' and 'void EntryPoint.ParentBuggy.foo()' "
            + "cannot both use the same JavaScript name 'foo'.");
  }

  public void testCollidingSubclassMethodToMethodTwoLayerInterfaceJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public interface IParentBuggy1 {",
        "  void show();",
        "}",
        "public interface IBuggy1 extends IParentBuggy1 {",
        "}",
        "@JsType",
        "public interface IParentBuggy2 {",
        "  void show(boolean b);",
        "}",
        "public interface IBuggy2 extends IParentBuggy2 {",
        "}",
        "public static class Buggy implements IBuggy1 {",
        "  public void show() {}",
        "}",
        "public static class Buggy2 extends Buggy implements IBuggy2 {",
        "  public void show(boolean b) {}",
        "}");

    assertBuggyFails(
        "Line 20: 'void EntryPoint.Buggy2.show(boolean)' and 'void EntryPoint.Buggy.show()' "
            + "cannot both use the same JavaScript name 'show'.");
  }

  public void testNonCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static interface Comparable<T> {",
        "  int compareTo(T other);",
        "}",
        "@JsType",
        "public static class Enum<E extends Enum<E>> implements Comparable<E> {",
        "  public int compareTo(E other) {return 0;}",
        "}",
        "public static class Buggy {}");

    assertBuggySucceeds();
  }

  public void testCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Comparable<T> {",
        "  int compareTo(T other);",
        "}",
        "@JsType",
        "public static class Enum<E extends Enum<E>> implements Comparable<E> {",
        "  public int compareTo(E other) {return 0;}",
        "}",
        "public static class Buggy {}");

    assertBuggySucceeds();
  }

  public void testSpecializeReturnTypeInImplementorSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "interface I {",
        "  I m();",
        "}",
        "@JsType",
        "public static class Buggy implements I {",
        "  public Buggy m() { return null; } ",
        "}");

    assertBuggySucceeds();
  }

  public void testSpecializeReturnTypeInSubclassSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class S {",
        "  public S m() { return null; }",
        "}",
        "@JsType",
        "public static class Buggy extends S {",
        "  public Buggy m() { return null; } ",
        "}");

    assertBuggySucceeds();
  }

  public void testCollidingTwoLayerSubclassFieldToFieldJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentParentBuggy {",
        "  public int foo = 55;",
        "}",
        "public static class ParentBuggy extends ParentParentBuggy {",
        "  public int foo = 55;",
        "}",
        "@JsType",
        "public static class Buggy extends ParentBuggy {",
        "  public int foo = 110;",
        "}");

    assertBuggyFails(
        "Line 13: 'int EntryPoint.Buggy.foo' and 'int EntryPoint.ParentParentBuggy.foo' cannot "
            + "both use the same JavaScript name 'foo'.");
  }

  public void testShadowedSuperclassJsMethodFails() {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  @JsMethod private void foo() {}",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  @JsMethod private void foo() {}",
        "}");

    assertBuggyFails(
        "Line 8: 'void EntryPoint.Buggy.foo()' and 'void EntryPoint.ParentBuggy.foo()' cannot "
            + "both use the same JavaScript name 'foo'.");
  }

  public void testRenamedSuperclassJsMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  public void foo() {}",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  @JsMethod(name = \"bar\") public void foo() {}",
        "}");

    assertBuggyFails("Line 10: 'void EntryPoint.Buggy.foo()' cannot be assigned a different "
        + "JavaScript name than the method it overrides.");
  }

  public void testRenamedSuperInterfaceJsMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "@JsType",
        "public interface ParentBuggy {",
        "  void foo();;",
        "}",
        "public interface Buggy extends ParentBuggy {",
        "  @JsMethod(name = \"bar\") void foo();",
        "}");

    assertBuggyFails("Line 10: 'void EntryPoint.Buggy.foo()' cannot be assigned a different "
        + "JavaScript name than the method it overrides.");
  }

  public void testAccidentallyRenamedSuperInterfaceJsMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "@JsType",
        "public interface IBuggy {",
        "  void foo();",
        "}",
        "@JsType",
        "public static class ParentBuggy {",
        "  @JsMethod(name = \"bar\") public void foo() {}",
        "}",
        "public static class Buggy extends ParentBuggy implements IBuggy {",
        "}");

    assertBuggyFails("Line 11: 'void EntryPoint.ParentBuggy.foo()' (exposed by 'EntryPoint.Buggy') "
        + "cannot be assigned a different JavaScript name than the method it overrides.");
  }

  public void testRenamedSuperclassJsPropertyFails() {
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class ParentBuggy {",
        "  @JsProperty public int getFoo() { return 0; }",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  @JsProperty(name = \"bar\") public int getFoo() { return 0; }",
        "}");

    assertBuggyFails("Line 8: 'int EntryPoint.Buggy.getFoo()' "
        + "cannot be assigned a different JavaScript name than the method it overrides.");
  }

  public void testJsPropertyDifferentFlavourInSubclassFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static class ParentBuggy {",
        "  @JsProperty public boolean isFoo() { return false; }",
        "}",
        "public static class Buggy extends ParentBuggy {",
        "  @JsProperty public boolean getFoo() { return false;}",
        "}");

    assertBuggyFails(
        "Line 10: 'boolean EntryPoint.Buggy.getFoo()' and 'boolean EntryPoint.ParentBuggy.isFoo()' "
            + "cannot both use the same JavaScript name 'foo'.");
  }

  public void testConsistentPropertyTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  public int getFoo();",
        "  @JsProperty",
        "  public void setFoo(int value);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public int getFoo() {return 0;}",
        "  public void setFoo(int value) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testInconsistentGetSetPropertyTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  public int getFoo();",
        "  @JsProperty",
        "  public void setFoo(Integer value);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public int getFoo() {return 0;}",
        "  public void setFoo(Integer value) {}",
        "}");

    assertBuggyFails(
        "Line 10: JsProperty setter 'void EntryPoint.IBuggy.setFoo(Integer)' and "
            + "getter 'int EntryPoint.IBuggy.getFoo()' cannot have inconsistent types.",
        "Line 14: JsProperty setter 'void EntryPoint.Buggy.setFoo(Integer)' and "
            + "getter 'int EntryPoint.Buggy.getFoo()' cannot have inconsistent types.");
  }

  public void testInconsistentIsSetPropertyTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  public boolean isFoo();",
        "  @JsProperty",
        "  public void setFoo(Object value);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean isFoo() {return false;}",
        "  public void setFoo(Object value) {}",
        "}");

    assertBuggyFails(
        "Line 10: JsProperty setter 'void EntryPoint.IBuggy.setFoo(Object)' and "
            + "getter 'boolean EntryPoint.IBuggy.isFoo()' cannot have inconsistent types.",
        "Line 14: JsProperty setter 'void EntryPoint.Buggy.setFoo(Object)' and "
            + "getter 'boolean EntryPoint.Buggy.isFoo()' cannot have inconsistent types.");
  }

  public void testJsPropertySuperCallFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public int getX() { return 5; }",
        "}",
        "@JsType public static class Buggy extends Super {",
        "  public int m() { return super.getX(); }",
        "}");

    assertBuggyFails(
        "Line 9: Cannot call property accessor 'int EntryPoint.Super.getX()' via super.");
  }

  public void testJsPropertyCallSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public int getX() { return 5; }",
        "}",
        "@JsType public static class Buggy extends Super {",
        "  public int m() { return getX(); }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyAccidentalSuperCallSucceeds()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public int getX() { return 5; }",
        "}",
        "@JsType public interface Interface {",
        "  @JsProperty int getX();",
        "}",

        "@JsType public static class Buggy extends Super implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyOverrideSucceeds()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "  @JsProperty public void setX(int x) {  }",
        "  @JsProperty public int getX() { return 5; }",
        "}",

        "@JsType public static class Buggy extends Super {",
        "  @JsProperty public void setX(int x) {  }",
        "}");

    assertBuggySucceeds();
  }

  public void testMixingJsMethodJsPropertyFails()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class Super {",
        "  @JsMethod public int getY() { return 5; }",
        "  @JsProperty public void setZ(int z) {}",
        "}",

        "public static class Buggy extends Super {",
        "  @JsProperty(name = \"getY\") public int getY() { return 6; }",
        "  @JsMethod(name = \"z\") public void setZ(int z) {}",
        "}");

    assertBuggyFails(
        "Line 10: 'int EntryPoint.Buggy.getY()' and 'int EntryPoint.Super.getY()' cannot "
            + "both use the same JavaScript name 'getY'.",
        "Line 11: 'void EntryPoint.Buggy.setZ(int)' and 'void EntryPoint.Super.setZ(int)' cannot "
           + "both use the same JavaScript name 'z'.");
  }

  public void testMultiplePrivateConstructorsExportSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  private Buggy() {}",
        "  private Buggy(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testMultiplePublicConstructorsAllDelegatesToJsConstructorSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  @JsIgnore",
        "  public Buggy(int a) {",
        "    this();",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testMultipleConstructorsNotAllDelegatedToJsConstructorFails()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  private Buggy(int a) {",
        "    new Buggy();",
        "  }",
        "}");

    assertBuggyFails(
        "Line 6: Constructor 'EntryPoint.Buggy.EntryPoint$Buggy()' can be a JsConstructor only if "
            + "all constructors in the class are delegating to it.");
  }

  public void testMultiplePublicConstructorsExportFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  public Buggy(int a) {",
        "    this();",
        "  }",
        "}");

    assertBuggyFails(
        "Line 5: More than one JsConstructor exists for EntryPoint.Buggy.",
        "Line 7: 'EntryPoint.Buggy.EntryPoint$Buggy(int)' cannot be exported because the global "
            + "name 'test.EntryPoint.Buggy' is already taken by "
            + "'EntryPoint.Buggy.EntryPoint$Buggy()'.");
  }

  public void testNonCollidingAccidentalOverrideSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Foo {",
        "  void doIt(Object foo);",
        "}",
        "public static class ParentParent {",
        "  public void doIt(String x) {}",
        "}",
        "@JsType",
        "public static class Parent extends ParentParent {",
        "  public void doIt(Object x) {}",
        "}",
        "public static class Buggy extends Parent implements Foo {}");

    assertBuggySucceeds();
  }

  public void testJsNameInvalidNamesFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType(name = \"a.b.c\") public static class Buggy {",
        "   @JsMethod(name = \"34s\") public void m() {}",
        "   @JsProperty(name = \"s^\") public int  m;",
        "   @JsProperty(name = \"\") public int n;",
        "}");

    assertBuggyFails(
        "Line 6: 'EntryPoint.Buggy' has invalid name 'a.b.c'.",
        "Line 7: 'void EntryPoint.Buggy.m()' has invalid name '34s'.",
        "Line 8: 'int EntryPoint.Buggy.m' has invalid name 's^'.",
        "Line 9: 'int EntryPoint.Buggy.n' cannot have an empty name.");
  }

  public void testJsNameInvalidNamespacesFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "@JsType(namespace = \"a.b.\") public static class Buggy {",
        "   @JsMethod(namespace = \"34s\") public static void m() {}",
        "   @JsProperty(namespace = \"s^\") public static int  n;",
        "   @JsMethod(namespace = \"\") public static void o() {}",
        "   @JsProperty(namespace = \"\") public int p;",
        "   @JsMethod(namespace = \"a\") public void q() {}",
        "}");

    assertBuggyFails(
        "Line 6: 'EntryPoint.Buggy' has invalid namespace 'a.b.'.",
        "Line 7: 'void EntryPoint.Buggy.m()' has invalid namespace '34s'.",
        "Line 8: 'int EntryPoint.Buggy.n' has invalid namespace 's^'.",
        "Line 9: 'void EntryPoint.Buggy.o()' cannot have an empty namespace.",
        "Line 10: Instance member 'int EntryPoint.Buggy.p' cannot declare a namespace.",
        "Line 11: Instance member 'void EntryPoint.Buggy.q()' cannot declare a namespace.");
  }

  public void testJsNameGlobalNamespacesSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetImport("jsinterop.annotations.JsPackage");
    addSnippetClassDecl(
        "@JsType(namespace = JsPackage.GLOBAL) public static class Buggy {",
        "   @JsMethod(namespace = JsPackage.GLOBAL) public static void m() {}",
        "   @JsProperty(namespace = JsPackage.GLOBAL) public static int  n;",
        "}");

    assertBuggySucceeds();
  }

  public void testSingleJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public static void show1() {}",
        "  public void show2() {}",
        "}");

    assertBuggySucceeds();
  }

  public void testJsFunctionWithNoExtendsSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "@JsFunction",
        "public interface Buggy {",
        "  void foo();",
        "}");

    assertBuggySucceeds();
  }

  public void testJsFunctionExtendsInterfaceFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "interface AnotherInterface {}",
        "@JsFunction",
        "public interface Buggy extends AnotherInterface {",
        "  void foo();",
        "}");

    assertBuggyFails("Line 6: JsFunction 'EntryPoint.Buggy' cannot extend other interfaces.");
  }

  public void testJsFunctionExtendedByInterfaceFails() throws Exception {
    addAll(jsFunctionInterface);

    addSnippetClassDecl("public interface Buggy extends MyJsFunctionInterface {}");

    assertBuggyFails(
        "Line 3: 'EntryPoint.Buggy' cannot extend JsFunction 'MyJsFunctionInterface'.");
  }

  public void testJsFunctionMarkedAsJsTypeFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "@JsFunction @JsType",
        "public interface Buggy {",
        "  void foo();",
        "}");

    assertBuggyFails(
        "Line 6: 'EntryPoint.Buggy' cannot be both a JsFunction and a JsType at the same time.");
  }

  public void testJsFunctionImplementationWithSingleInterfaceSucceeds() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "public static class Buggy implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsFunctionImplementationWithMultipleSuperInterfacesFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "interface AnotherInterface {}",
        "public static class Buggy implements MyJsFunctionInterface, AnotherInterface {",
        "  public int foo(int x) { return 0; }",
        "  public int bar(int x) { return 0; }",
        "}");

    assertBuggyFails("Line 4: JsFunction implementation 'EntryPoint.Buggy' cannot "
        + "implement more than one interface.");
  }

  public void testJsFunctionImplementationWithSuperClassFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "public static class BaseClass {}",
        "public static class Buggy extends BaseClass implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}");

    assertBuggyFails("Line 4: JsFunction implementation 'EntryPoint.Buggy' cannot "
        + "extend a class.");
  }

  public void testJsFunctionImplementationWithSubclassesFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "public static class BaseClass implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}",
        "public static class Buggy extends BaseClass  {",
        "}");

    assertBuggyFails("Line 6: 'EntryPoint.Buggy' cannot extend "
        + "JsFunction implementation 'EntryPoint.BaseClass'.");
  }

  public void testJsFunctionImplementationMarkedAsJsTypeFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}");

    assertBuggyFails(
        "Line 5: 'EntryPoint.Buggy' cannot be both a JsFunction implementation and a JsType "
            + "at the same time.");
  }

  public void testJsFunctionStaticInitializerFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetClassDecl(
        "public static String someString() { return \"hello\"; }",
        "@JsFunction public interface Buggy {",
        "  static String s = someString();",
        "  void m();",
        "}");

    assertBuggyFails(
        "Line 6: JsFunction 'EntryPoint.Buggy' cannot have static initializer.");
  }

  public void testNativeJsTypeStaticInitializerFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  static {  int x = 1; }",
        "}",
        "@JsType(isNative=true) public static class Buggy2 {",
        "  static {  Object.class.getName(); }",
        "}");

    assertBuggyFails(
        "Line 4: Native JsType 'EntryPoint.Buggy' cannot have static initializer.",
        "Line 7: Native JsType 'EntryPoint.Buggy2' cannot have static initializer.");
  }

  public void testNativeJsTypeInstanceInitializerFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  { Object.class.getName(); }",
        "}",
        "@JsType(isNative=true) public static class Buggy2 {",
        "  { int x = 1; }",
        "}");

    assertBuggyFails(
        "Line 4: Native JsType 'EntryPoint.Buggy' cannot have initializer.",
        "Line 7: Native JsType 'EntryPoint.Buggy2' cannot have initializer.");
  }

  public void testNativeJsTypeNonEmptyConstructorFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  public Buggy(int n) {",
        "    n++;",
        "  }",
        "}");

    assertBuggyFails(
        "Line 5: Native JsType constructor 'EntryPoint.Buggy.EntryPoint$Buggy(int)' cannot have "
            + "non-empty method body.");
  }

  public void testNativeJsTypeImplicitSuperSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public Super() {",
        "  }",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  public Buggy(int n) {",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExplicitSuperSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public Super(int x) {",
        "  }",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  public Buggy(int n) {",
        "    super(n);",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExplicitSuperWithEffectSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public Super(int x) {",
        "  }",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  public Buggy(int n) {",
        "    super(n++);",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeInterfaceInInstanceofFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface IBuggy {}",
        "@JsType public static class Buggy {",
        "  public Buggy() { if (new Object() instanceof IBuggy) {} }",
        "}");

    assertBuggyFails("Line 6: Cannot do instanceof against native JsType interface "
        + "'EntryPoint.IBuggy'.");
  }

  public void testNativeJsTypeEnumFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public enum Buggy { A, B }");

    assertBuggyFails(
        "Line 4: Enum 'EntryPoint.Buggy' cannot be a native JsType.");
  }

  public void testInnerNativeJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public class Buggy { }");

    assertBuggyFails(
        "Line 4: Non static inner class 'EntryPoint.Buggy' cannot be a native JsType.");
  }

  public void testInnerJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@SuppressWarnings(\"unusable-by-js\") @JsType public class Buggy { }");

    assertBuggySucceeds();
  }

  public void testLocalJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public class Buggy { void m() { @JsType class Local {} } }");

    assertBuggyFails(
        "Line 4: Local class 'EntryPoint.Buggy.1Local' cannot be a JsType.");
  }

  public void testNativeJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeImplementsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy implements Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeInterfaceImplementsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Super {",
        "}",
        "@JsType(isNative=true) public interface Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExtendsJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType 'EntryPoint.Buggy' can only extend native JsType classes.");
  }

  public void testNativeJsTypeImplementsJsTypeInterfaceFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "@JsType(isNative=true) public static class Buggy implements Interface {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType ''EntryPoint.Buggy'' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsJsTypeInterfaceFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "@JsType(isNative=true) public interface Buggy extends Interface {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType ''EntryPoint.Buggy'' can only extend native JsType interfaces.");
  }

  public void testNativeJsTypeImplementsNonJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy implements Super {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType ''EntryPoint.Buggy'' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsNonJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Super {",
        "}",
        "@JsType(isNative=true) public interface Buggy extends Super {",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType ''EntryPoint.Buggy'' can only extend native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceDefenderMethodsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Buggy {",
        "  default void someMethod(){}",
        "  @JsOverlay",
        "  default void someOverlayMethod(){}",
        "}");

    assertBuggyFails(
        "Line 6: Native JsType method 'void EntryPoint.Buggy.someMethod()' should be native "
            + "or abstract.",
        "Line 8: JsOverlay method 'void EntryPoint.Buggy.someOverlayMethod()' cannot be "
            + "non-final nor native.");
  }

  public void testJsOverlayOnNativeJsTypeMemberSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public static final int f = 2;",
        "  @JsOverlay public static void m() { }",
        "  @JsOverlay public static void m(int x) { }",
        "  @JsOverlay private static void m(boolean x) { }",
        "  @JsOverlay public final void n() { }",
        "  @JsOverlay public final void n(int x) { }",
        "  @JsOverlay private final void n(boolean x) { }",
        "  @JsOverlay final void o() { }",
        "  @JsOverlay protected final void p() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayImplementingInterfaceMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface IBuggy {",
        "  void m();",
        "}",
        "@JsType(isNative=true) public static class Buggy implements IBuggy {",
        "  @JsOverlay public void m() { }",
        "}");

    assertBuggyFails("Line 9: JsOverlay method 'void EntryPoint.Buggy.m()' cannot override a "
        + "supertype method.");
  }

  public void testJsOverlayOverridingSuperclassMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public native void m();",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "  @JsOverlay public void m() { }",
        "}");

    assertBuggyFails(
        "Line 9: JsOverlay method 'void EntryPoint.Buggy.m()' cannot override a supertype method.");
  }

  public void testJsOverlayOnNonFinalMethodAndNonCompileTimeConstantFieldFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public static int f1 = 2;",
        "  @JsOverlay public final int f2 = 2;",
        "  @JsOverlay public void m() { }",
        "}");

    assertBuggyFails(
        "Line 5: Native JsType 'EntryPoint.Buggy' cannot have initializer.",
        "Line 6: JsOverlay field 'int EntryPoint.Buggy.f1' can only be a compile time constant.",
        "Line 7: JsOverlay field 'int EntryPoint.Buggy.f2' can only be a compile time constant.",
        "Line 8: JsOverlay method 'void EntryPoint.Buggy.m()' cannot be non-final nor native.");
  }

  public void testJsOverlayOnNativeMethodFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Buggy {",
        "  @JsOverlay public static final native void m1();",
        "  @JsOverlay public static final native void m2()/*-{}-*/;",
        "  @JsOverlay public final native void m3();",
        "  @JsOverlay public final native void m4()/*-{}-*/;",
        "}");

    assertBuggyFails(
        "Line 6: JsOverlay method 'void EntryPoint.Buggy.m1()' cannot be non-final nor native.",
        "Line 7: JSNI method 'void EntryPoint.Buggy.m2()' is not allowed in a native JsType.",
        "Line 8: JsOverlay method 'void EntryPoint.Buggy.m3()' cannot be non-final nor native.",
        "Line 9: JSNI method 'void EntryPoint.Buggy.m4()' is not allowed in a native JsType.");
  }

  public void testJsOverlayOnJsoMethodSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "public static class Buggy extends JavaScriptObject {",
        "  protected Buggy() { }",
        "  @JsOverlay public final void m() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testImplicitJsOverlayOnJsoMethodSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "public static class Buggy extends JavaScriptObject {",
        "  protected Buggy() { }",
        "  public final void m() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testJsOverlayOnNonNativeJsTypeFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsOverlay");
    addSnippetClassDecl(
        "@JsType public static class Buggy {",
        "  @JsOverlay public static final int f = 2;",
        "  @JsOverlay public final void m() { };",
        "}");

    assertBuggyFails(
        "Line 6: JsOverlay 'int EntryPoint.Buggy.f' can only be declared in a native type.",
        "Line 7: JsOverlay 'void EntryPoint.Buggy.m()' can only be declared in a native type.");
  }

  public void testJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "}",
        "@JsType public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeExtendsNonJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class Super {",
        "}",
        "@JsType public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeImplementsNativeJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "@JsType public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeImplementsNonJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Interface {",
        "}",
        "@JsType public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeIntefaceExtendsNativeJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "@JsType public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testJsTypeInterfaceExtendsNonJsTypeInterfaceSucceeds()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public interface Interface {",
        "}",
        "@JsType public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeExtendsNaiveJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "}",
        "@JsType(isNative=true) public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeBadMembersFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsIgnore");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "  public static final int s = 42;",
        "  public int f = 42;",
        "  @JsIgnore public Buggy() { }",
        "  @JsIgnore public int x;",
        "  @JsIgnore public native void n();",
        "  public void o() {}",
        "  public native void p() /*-{}-*/;",
        "}");

    assertBuggyFails(
        "Line 5: Native JsType 'EntryPoint.Buggy' cannot have initializer.",
        "Line 6: Native JsType field 'int EntryPoint.Buggy.s' cannot have initializer.",
        "Line 7: Native JsType field 'int EntryPoint.Buggy.f' cannot have initializer.",
        "Line 8: Native JsType member 'EntryPoint.Buggy.EntryPoint$Buggy()' cannot have @JsIgnore.",
        "Line 9: Native JsType member 'int EntryPoint.Buggy.x' cannot have @JsIgnore.",
        "Line 10: Native JsType member 'void EntryPoint.Buggy.n()' cannot have @JsIgnore.",
        "Line 11: Native JsType method 'void EntryPoint.Buggy.o()' should be native or abstract.",
        "Line 12: JSNI method 'void EntryPoint.Buggy.p()' is not allowed in a native JsType.");
  }

  public void testNativeMethodOnJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod public native void m();",
        "  @JsMethod public native void n() /*-{}-*/;",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) abstract static class Buggy {",
        "  public static native void m();",
        "  protected static native void m(Object o);",
        "  private static native void m(String o);",
        "  public Buggy() { }",
        "  protected Buggy(Object o) { }",
        "  private Buggy(String o) { }",
        "  public native void n();",
        "  protected native void n(Object o);",
        "  private native void n(String o);",
        "  public abstract void o();",
        "  protected abstract void o(Object o);",
        "  abstract void o(String o);",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeFieldsSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "  public static int f1;",
        "  protected static int f2;",
        "  private static int f3;",
        "  public int f4;",
        "  protected int f5;",
        "  private int f6;",
        "}");

    assertBuggySucceeds();
  }

  public void testNativeJsTypeDefaultConstructorSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) static class Buggy {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@SuppressWarnings(\"unusable-by-js\")",
        "@JsType(isNative=true) public static class Super {",
        "  public native void m(Object o);",
        "  public native void m(Object[] o);",
        "}",
        "@JsType public static class Buggy extends Super {",
        "  public void n(Object o) { }",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodOverloadsFails() {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public native void m(Object o);",
        "  public native void m(int o);",
        "}",
        "public static class Buggy extends Super {",
        "  public void m(Object o) { }",
        "}");

    assertBuggyFails(
        "Line 9: 'void EntryPoint.Buggy.m(Object)' and 'void EntryPoint.Super.m(int)' "
            + "cannot both use the same JavaScript name 'm'.");
  }

  public void testNonJsTypeWithNativeStaticMethodOverloadsSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod public static native void m(Object o);",
        "  @JsMethod public static native void m(int o);",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeWithNativeInstanceMethodOverloadsFails() throws Exception {
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsMethod public native void m(Object o);",
        "  @JsMethod public void m(int o) { }",
        "}");

    assertBuggyFails(
        "Line 6: 'void EntryPoint.Buggy.m(int)' and 'void EntryPoint.Buggy.m(Object)' "
            + "cannot both use the same JavaScript name 'm'.");
  }

  public void testNonJsTypeExtendsJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public static class Super {",
        "}",
        "public static class Buggy extends Super {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeImplementsJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeInterfaceExtendsJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType public interface Interface {",
        "}",
        "public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public static class Super {",
        "  public native void m();",
        "}",
        "public static class Buggy extends Super {",
        "  public void m() { }",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeImplementsNativeJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "public static class Buggy implements Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testNonJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType(isNative=true) public interface Interface {",
        "}",
        "public interface Buggy extends Interface {",
        "}");

    assertBuggySucceeds();
  }

  public void testUnusableByJsSuppressionSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl("public static class A {}");
    addSnippetClassDecl(
        "@JsType @SuppressWarnings(\"unusable-by-js\")", // SuppressWarnings on the class.
        "public static class B {",
        "  public A field;",
        "  public A t0(A a, A b) { return null; }",
        "}");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  @SuppressWarnings(\"unusable-by-js\") public A field;", // add SuppressWarnings to field.
        "  @SuppressWarnings({\"unusable-by-js\", \"unused\"})", // test multiple warnings.
        "  public A t0(A a, A b) { return null; }", // add SuppressWarnings to the method.
        "  public void t1(",
        "    @SuppressWarnings(\"unusable-by-js\")A a,",
        "    @SuppressWarnings(\"unusable-by-js\")A b",
        "  ) {}", // add SuppressWarnings to parameters.
        "}");

    assertBuggySucceeds();
  }

  public void testUsableByJsTypesSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsExport");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetImport("com.google.gwt.core.client.JavaScriptObject");
    addSnippetClassDecl(
        "@JsType public static class A {}",
        "@JsType public static interface I {}",
        "@JsFunction public static interface FI {void foo();}",
        "public static class C extends JavaScriptObject {protected C(){}}",
        "@JsType public static class Buggy {",
        "  public void f1(boolean a, int b, double c) {}", // primitive types work fine.
        "  public void f2(Boolean a, Double b, String c) {}", // unboxed types work fine.
        "  public void f3(A a) {}", // JsType works fine.
        "  public void f4(I a) {}", // JsFunction works fine.
        "  public void f5(FI a) {}", // JsFunction works fine.
        "  public void f6(C a) {}", // JavaScriptObject works fine.
        "  public void f7(Object a) {}", // Java Object works fine.
        "  public void f8(boolean[] a) {}", // array of primitive types work fine.
        "  public void f9(Boolean[] a, Double[] b, String[] c) {}", // array of unboxed types.
        "  public void f10(A[] a) {}", // array of JsType works fine.
        "  public void f11(FI[] a) {}", // array of JsFunction works fine.
        "  public void f12(C[][] a) {}", // array of JavaScriptObject works fine.
        "}");
    assertBuggySucceeds();
  }

  public void testUnusableByJsNotExportedMembersSucceeds() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "public static class A {}",
        "@JsType public static class Buggy {",
        "  private A field;", // private field.
        "  private A f1(A a) { return null; }", // private method.
        "}");
    assertBuggySucceeds();
  }

  public void testUnusuableByJsWarns() throws Exception {
    addSnippetImport("jsinterop.annotations.JsFunction");
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetImport("jsinterop.annotations.JsMethod");
    addSnippetImport("jsinterop.annotations.JsProperty");
    addSnippetClassDecl(
        "public static class A {}",
        "@JsType public static interface I {}",
        "public static class B implements I {}",
        "public static class C {", // non-jstype class with JsMethod
        "  @JsMethod",
        "  public static void fc1(A a) {}", // JsMethod
        "}",
        "public static class D {", // non-jstype class with JsProperty
        "  @JsProperty",
        "  public static A a;", // JsProperty
        "}",
        "@JsFunction public static interface FI  { void f(A a); }", // JsFunction method is checked.
        "@JsType public static class Buggy {",
        "  public A f;", // exported field
        "  public A f1(A a) { return null; }", // regular class fails.
        "  public A[] f2(A[] a) { return null; }", // array of regular class fails.
        "  public long f3(long a) { return 1l; }", // long fails.
        // non-JsType class that implements a JsType interface fails.
        "  public B f4(B a) { return null; }",
        "  public void f5(Object[][] a) {}", // Object[][] fails.
        "  public void f6(Object[] a) {}", // Object[] also fails.
        "}");

    assertBuggySucceeds(
        "Line 12: [unusable-by-js] Type of parameter 'a' in "
            + "'void EntryPoint.C.fc1(EntryPoint.A)' is not usable by but exposed to JavaScript.",
        "Line 16: [unusable-by-js] Type of 'EntryPoint.A EntryPoint.D.a' is not usable by but "
            + "exposed to JavaScript.",
        "Line 18: [unusable-by-js] Type of parameter 'a' in "
            + "'void EntryPoint.FI.f(EntryPoint.A)' is not usable by but exposed to JavaScript.",
        "Line 20: [unusable-by-js] Type of 'EntryPoint.A EntryPoint.Buggy.f' is not usable by but "
            + "exposed to JavaScript.",
        "Line 21: [unusable-by-js] Return type of 'EntryPoint.A EntryPoint.Buggy.f1(EntryPoint.A)' "
            + "is not usable by but exposed to JavaScript.",
        "Line 21: [unusable-by-js] Type of parameter 'a' in "
            + "'EntryPoint.A EntryPoint.Buggy.f1(EntryPoint.A)' is not usable by but "
            + "exposed to JavaScript.",
        "Line 22: [unusable-by-js] Return type of "
            + "'EntryPoint.A[] EntryPoint.Buggy.f2(EntryPoint.A[])' is not usable by but "
            + "exposed to JavaScript.",
        "Line 22: [unusable-by-js] Type of parameter 'a' in "
            + "'EntryPoint.A[] EntryPoint.Buggy.f2(EntryPoint.A[])' is not usable by but "
            + "exposed to JavaScript.",
        "Line 23: [unusable-by-js] Return type of 'long EntryPoint.Buggy.f3(long)' is not "
            + "usable by but exposed to JavaScript.",
        "Line 23: [unusable-by-js] Type of parameter 'a' in "
            + "'long EntryPoint.Buggy.f3(long)' is not usable by but exposed to JavaScript.",
        "Line 24: [unusable-by-js] Return type of 'EntryPoint.B EntryPoint.Buggy.f4(EntryPoint.B)' "
            + "is not usable by but exposed to JavaScript.",
        "Line 24: [unusable-by-js] Type of parameter 'a' in "
            + "'EntryPoint.B EntryPoint.Buggy.f4(EntryPoint.B)' is not usable by but "
            + "exposed to JavaScript.",
        "Line 25: [unusable-by-js] Type of parameter 'a' in "
            + "'void EntryPoint.Buggy.f5(Object[][])' is not usable by but exposed to JavaScript.",
        "Line 26: [unusable-by-js] Type of parameter 'a' in "
            + "'void EntryPoint.Buggy.f6(Object[])' is not usable by but exposed to JavaScript.");
  }

  public void testUnusableByJsAccidentalOverrideSuppressionWarns()
      throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static interface Foo {",
        "  @SuppressWarnings(\"unusable-by-js\") ",
        "  void doIt(Class foo);",
        "}",
        "public static class Parent {",
        "  public void doIt(Class x) {}",
        "}",
        "public static class Buggy extends Parent implements Foo {}");

    assertBuggySucceeds(
        "Line 10: [unusable-by-js] Type of parameter 'x' in "
            + "'void EntryPoint.Parent.doIt(Class)' (exposed by 'EntryPoint.Buggy') is not usable "
            + "by but exposed to JavaScript.");
  }

  private static final MockJavaResource jsFunctionInterface = new MockJavaResource(
      "test.MyJsFunctionInterface") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package test;\n");
      code.append("import jsinterop.annotations.JsFunction;\n");
      code.append("@JsFunction public interface MyJsFunctionInterface {\n");
      code.append("int foo(int x);\n");
      code.append("}\n");
      return code;
    }
  };

  public final void assertBuggySucceeds(String... expectedWarnings)
      throws Exception {
    List<String> allWarnings = Lists.newArrayList();
    if (expectedWarnings.length > 0) {
      allWarnings.add("Warnings in test/EntryPoint.java");
      allWarnings.addAll(Arrays.asList(expectedWarnings));
    }
    Result result = assertCompileSucceeds("Buggy buggy = null;",
        allWarnings.toArray(new String[0]));
    assertNotNull(result.findClass("test.EntryPoint$Buggy"));
  }

  public final void assertBuggyFails(String... expectedErrors) {
    assertTrue(expectedErrors.length > 0);

    List<String> allErrors = Lists.newArrayList();
    allErrors.add("Errors in test/EntryPoint.java");
    allErrors.addAll(Arrays.asList(expectedErrors));
    assertCompileFails("Buggy buggy = null;", allErrors.toArray(new String[0]));
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    try {
      JsInteropRestrictionChecker.exec(logger, program, new MinimalRebuildCache());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return false;
  }
}
