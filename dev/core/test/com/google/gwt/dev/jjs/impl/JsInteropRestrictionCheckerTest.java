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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests for the JsInteropRestrictionChecker.
 */
public class JsInteropRestrictionCheckerTest extends OptimizerTestBase {

  // TODO: eventually test this for default methods in Java 8.
  public void testCollidingAccidentalOverrideConcreteMethodFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "Method 'test.EntryPoint$Buggy.doIt(Ltest/EntryPoint$Bar;)V' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the member name 'doIt' is already taken.");
  }

  public void testCollidingAccidentalOverrideAbstractMethodFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "Method 'test.EntryPoint$Baz.doIt(Ltest/EntryPoint$Bar;)V' can't be exported in type "
        + "'test.EntryPoint$Baz' because the member name 'doIt' is already taken.");
  }

  public void testCollidingAccidentalOverrideHalfAndHalfFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "Method 'test.EntryPoint$Parent.doIt(Ltest/EntryPoint$Foo;)V' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the member name 'doIt' is already taken.");
  }

  public void testCollidingFieldExportsFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsExport(\"show\")",
        "  public static final int show = 0;",
        "  @JsExport(\"show\")",
        "  public static final int display = 0;",
        "}");

    assertBuggyFails(
        "Member 'test.EntryPoint$Buggy.display' can't be exported because the "
        + "global name 'test.EntryPoint.Buggy.show' is already taken.");
  }

  public void testJsPropertyGetterStyleSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty int getX();",
        "  @JsProperty void setX(int x);",
        "  @JsProperty boolean isY();",
        "  @JsProperty void setY(boolean y);",
        "}");

    assertBuggySucceeds();
  }

  public void testJsPropertyIncorrectGetterStyleFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty int isX();",
        "  @JsProperty int getY(int x);",
        "  @JsProperty void getZ();",
        "  @JsProperty void setX(int x, int y);",
        "  @JsProperty void setY();",
        "  @JsProperty int setZ(int z);",
        "}");

    assertBuggyFails(
        "There can't be non-booelean return for the JsProperty 'is' getter"
        + " 'test.EntryPoint$Buggy.isX()I'.",
        "There can't be void return type or any parameters for the JsProperty getter"
        + " 'test.EntryPoint$Buggy.getY(I)I'.",
        "There can't be void return type or any parameters for the JsProperty getter"
        + " 'test.EntryPoint$Buggy.getZ()V'.",
        "There needs to be single parameter and void return type for the JsProperty setter"
        + " 'test.EntryPoint$Buggy.setX(II)V'.",
        "There needs to be single parameter and void return type for the JsProperty setter"
        + " 'test.EntryPoint$Buggy.setY()V'.",
        "There needs to be single parameter and void return type for the JsProperty setter"
        + " 'test.EntryPoint$Buggy.setZ(I)I'.");
  }

  public void testJsPropertyNonGetterStyleFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public interface Buggy {",
        "  @JsProperty boolean hasX();",
        "  @JsProperty int x();",
        "  @JsProperty void x(int x);",
        "}");

    assertBuggyFails(
        "JsProperty 'test.EntryPoint$Buggy.hasX()Z' doesn't follow Java Bean naming conventions.",
        "JsProperty 'test.EntryPoint$Buggy.x()I' doesn't follow Java Bean naming conventions.",
        "JsProperty 'test.EntryPoint$Buggy.x(I)V' doesn't follow Java Bean naming conventions.");
  }

  public void testCollidingJsPropertiesTwoGettersFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  boolean isX();",
        "  @JsProperty",
        "  boolean getX();",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean isX() {return false;}",
        "  public boolean getX() {return false;}",
        "}");

    assertBuggyFails(
        "There can't be more than one getter for JsProperty 'x' in type 'test.EntryPoint$IBuggy'.",
        "There can't be more than one getter for JsProperty 'x' in type 'test.EntryPoint$Buggy'.");
  }

  public void testCollidingJsPropertiesTwoSettersFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  void setX(boolean x);",
        "  @JsProperty",
        "  void setX(int x);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public void setX(boolean x) {}",
        "  public void setX(int x) {}",
        "}");

    assertBuggyFails(
        "There can't be more than one setter for JsProperty 'x' in type 'test.EntryPoint$IBuggy'.",
        "There can't be more than one setter for JsProperty 'x' in type 'test.EntryPoint$Buggy'.");
  }

  // TODO: duplicate this check with two @JsType interfaces.
  public void testCollidingJsTypeAndJsPropertyGetterFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  boolean x(boolean foo);",
        "  @JsProperty",
        "  int getX();",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean x(boolean foo) {return false;}",
        "  public int getX() {return 0;}",
        "}");

    assertBuggyFails(
        "The JsType member 'test.EntryPoint$IBuggy.x(Z)Z' and JsProperty "
        + "'test.EntryPoint$IBuggy.getX()I' can't both be named 'x' in "
        + "type 'test.EntryPoint$IBuggy'.",
        "The JsType member 'test.EntryPoint$Buggy.x(Z)Z' and JsProperty "
        + "'test.EntryPoint$Buggy.getX()I' can't both be named 'x' in "
        + "type 'test.EntryPoint$Buggy'.");
  }

  public void testCollidingJsTypeAndJsPropertySetterFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  boolean x(boolean foo);",
        "  @JsProperty",
        "  void setX(int a);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public boolean x(boolean foo) {return false;}",
        "  public void setX(int a) {}",
        "}");

    assertBuggyFails(
        "The JsType member 'test.EntryPoint$IBuggy.x(Z)Z' and JsProperty "
        + "'test.EntryPoint$IBuggy.setX(I)V' can't both be named 'x' in "
        + "type 'test.EntryPoint$IBuggy'.",
        "The JsType member 'test.EntryPoint$Buggy.x(Z)Z' and JsProperty "
        + "'test.EntryPoint$Buggy.setX(I)V' can't both be named 'x' in "
        + "type 'test.EntryPoint$Buggy'.");
  }

  public void testCollidingMethodExportsFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsExport(\"show\")",
        "  public static void show() {}",
        "  @JsExport(\"show\")",
        "  public static void display() {}",
        "}");

    assertBuggyFails(
        "Member 'test.EntryPoint$Buggy.display()V' can't be exported "
        + "because the global name 'test.EntryPoint.Buggy.show' is already taken.");
  }

  public void testCollidingMethodToFieldExportsFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsExport(\"show\")",
        "  public static void show() {}",
        "  @JsExport(\"show\")",
        "  public static final int display = 0;",
        "}");

    assertBuggyFails(
        "Member 'test.EntryPoint$Buggy.show()V' can't be exported because the "
        + "global name 'test.EntryPoint.Buggy.show' is already taken.");
  }

  public void testCollidingMethodToFieldJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show() {}",
        "  public final int show = 0;",
        "}");

    assertBuggyFails(
        "Method 'test.EntryPoint$Buggy.show()V' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the member name 'show' is already taken.");
  }

  public void testCollidingMethodToMethodJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show(int x) {}",
        "  public void show() {}",
        "}");

    assertBuggyFails(
        "Method 'test.EntryPoint$Buggy.show()V' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the member name 'show' is already taken.");
  }

  public void testCollidingSubclassExportedFieldToFieldJsTypeSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "Field 'test.EntryPoint$ParentBuggy.foo' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the member name 'foo' is already taken.");
  }

  public void testCollidingSubclassFieldToMethodJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "Field 'test.EntryPoint$ParentBuggy.foo' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the member name 'foo' is already taken.");
  }

  public void testCollidingSubclassMethodToExportedMethodJsTypeSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "Method 'test.EntryPoint$Buggy.show()V' can't be exported in type "
        + "'test.EntryPoint$Buggy2' because the member name 'show' is already taken.");
  }

  public void testCollidingSubclassMethodToMethodJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "Method 'test.EntryPoint$ParentBuggy.foo()V' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the member name 'foo' is already taken.");
  }

  public void testCollidingSubclassMethodToMethodTwoLayerInterfaceJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "Method 'test.EntryPoint$Buggy.show()V' can't be exported in type "
        + "'test.EntryPoint$Buggy2' because the member name 'show' is already taken.");
  }

  public void testCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
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

  public void testCollidingTwoLayerSubclassFieldToFieldJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "Field 'test.EntryPoint$ParentParentBuggy.foo' can't be exported in type "
        + "'test.EntryPoint$Buggy' because the member name 'foo' is already taken.");
  }

  public void testConsistentPropertyTypeSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
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
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
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
        "The setter and getter for JsProperty 'foo' in type 'test.EntryPoint$IBuggy' "
        + "must have consistent types.",
        "The setter and getter for JsProperty 'foo' in type 'test.EntryPoint$Buggy' "
        + "must have consistent types.");
  }

  public void testInconsistentIsSetPropertyTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
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
        "The setter and getter for JsProperty 'foo' in type 'test.EntryPoint$IBuggy' "
        + "must have consistent types.",
        "The setter and getter for JsProperty 'foo' in type 'test.EntryPoint$Buggy' "
        + "must have consistent types.");
  }

  public void testJsPropertyInNonJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsProperty",
        "  public int getX() {return 0;}",
        "}");

    assertBuggyFails("Method 'getX' can't be a JsProperty since 'test.EntryPoint$Buggy' "
        + "is not an interface.");
  }

  public void testJsPropertyInTransitiveNonJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface ParentExported {",
        "}",
        "public static interface Exported extends ParentExported {",
        "  @JsProperty",
        "  public int getX();",
        "}",
        "public static class Buggy {} // Unrelated class");

    assertBuggyFails("Method 'getX' can't be a JsProperty since interface "
        + "'test.EntryPoint$Exported' is not a JsType.");
  }

  public void testJsTypeInterfaceExtendsNonJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetClassDecl(
        "public static interface IBuggyParent {}",
        "@JsType",
        "public static interface IBuggy extends IBuggyParent {}",
        "public static class Buggy {} // Unrelated class");

    assertBuggySucceeds("JsType interface 'test.EntryPoint$IBuggy' extends non-JsType "
        + "interface 'test.EntryPoint$IBuggyParent'. This is not recommended.");
  }

  public void testMultiplePrivateConstructorsExportSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "@JsExport",
        "public static class Buggy {",
        "  private Buggy() {}",
        "  private Buggy(int a) {}",
        "}");

    assertBuggySucceeds();
  }

  public void testMultiplePublicConstructorsAllDelegatesToExportedConstructorSucceeds()
      throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetImport("com.google.gwt.core.client.js.JsNoExport");
    addSnippetClassDecl(
        "@JsExport",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  @JsNoExport",
        "  public Buggy(int a) {",
        "    this();",
        "  }",
        "}");

    assertBuggySucceeds();
  }

  public void testMultipleConstructorsNotAllDelegatedToExportedConstructorFails()
      throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetImport("com.google.gwt.core.client.js.JsNoExport");
    addSnippetClassDecl(
        "@JsExport",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  private Buggy(int a) {",
        "    new Buggy();",
        "  }",
        "}");

    assertBuggyFails(
        "Constructor 'test.EntryPoint$Buggy.EntryPoint$Buggy() <init>' can only be exported if all "
        + "constructors in the class are delegating to it.");
  }

  public void testMultiplePublicConstructorsExportFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "@JsExport",
        "public static class Buggy {",
        "  public Buggy() {}",
        "  public Buggy(int a) {}",
        "}");

    assertBuggyFails(
        "More than one constructor exported for test.EntryPoint$Buggy.",
        "Constructor 'test.EntryPoint$Buggy.EntryPoint$Buggy() <init>' can only be exported if all "
        + "constructors in the class are delegating to it.",
        "Member 'test.EntryPoint$Buggy.EntryPoint$Buggy(I) <init>' can't be "
        + "exported because the global name 'test.EntryPoint.Buggy' is already taken.");
  }

  public void testNonCollidingAccidentalOverrideSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
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
        "public static class Buggy extends Parent implements Foo {}");

    assertBuggySucceeds();
  }

  public void testSingleConstructortExportWithNameFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsExport(\"Create\")",
        "  public Buggy() {}",
        "}");

    assertBuggyFails(
        "Constructor 'test.EntryPoint$Buggy.EntryPoint$Buggy() <init>' cannot have an export "
        + "name.");
  }

  public void testSingleExportSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsExport(\"show\")",
        "  public static void show() {}",
        "}");

    assertBuggySucceeds();
  }

  public void testSingleJsTypeSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show() {}",
        "}");

    assertBuggySucceeds();
  }

  public void testJsFunctionWithNoExtendsSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsFunction");
    addSnippetClassDecl(
        "@JsFunction",
        "public interface Buggy {",
        "  void foo();",
        "}");

    assertBuggySucceeds();
  }

  public void testJsFunctionExtendsInterfaceFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsFunction");
    addSnippetClassDecl(
        "interface AnotherInterface {}",
        "@JsFunction",
        "public interface Buggy extends AnotherInterface {",
        "  void foo();",
        "}");

    assertBuggyFails("JsFunction 'test.EntryPoint$Buggy' cannot extend other interfaces.");
  }

  public void testJsFunctionExtendedByInterfaceFails() throws Exception {
    addAll(jsFunctionInterface);

    addSnippetClassDecl("public interface Buggy extends MyJsFunctionInterface {}");

    assertBuggyFails("JsFunction 'test.MyJsFunctionInterface' cannot be extended by other "
        + "interfaces:\n\ttest.EntryPoint$Buggy");
  }

  public void testJsFunctionMarkedAsJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsFunction");
    addSnippetClassDecl(
        "@JsFunction @JsType",
        "public interface Buggy {",
        "  void foo();",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy' cannot be both a JsFunction and a JsType at the same time.");
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

    assertBuggyFails("JsFunction implementation 'test.EntryPoint$Buggy' cannot implement more than "
        + "one interface.");
  }

  public void testJsFunctionImplementationWithSuperClassFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "public static class BaseClass {}",
        "public static class Buggy extends BaseClass implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}");

    assertBuggyFails("JsFunction implementation 'test.EntryPoint$Buggy' cannot extend a class.");
  }

  public void testJsFunctionImplementationWithSubclassesFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetClassDecl(
        "public static class BaseClass implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}",
        "public static class Buggy extends BaseClass  {",
        "}");

    assertBuggyFails("Implementation of JsFunction 'test.EntryPoint$BaseClass' cannot be extended "
        + "by other classes:\n\ttest.EntryPoint$Buggy");
  }

  public void testJsFunctionImplementationMarkedAsJsTypeFails() throws Exception {
    addAll(jsFunctionInterface);
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy implements MyJsFunctionInterface {",
        "  public int foo(int x) { return 0; }",
        "}");

    assertBuggyFails(
        "'test.EntryPoint$Buggy' cannot be both a JsFunction implementation and a JsType at the "
        + "same time.");
  }

  private static final MockJavaResource jsFunctionInterface = new MockJavaResource(
      "test.MyJsFunctionInterface") {
    @Override
    public CharSequence getContent() {
      StringBuilder code = new StringBuilder();
      code.append("package test;\n");
      code.append("import com.google.gwt.core.client.js.JsFunction;\n");
      code.append("@JsFunction public interface MyJsFunctionInterface {\n");
      code.append("int foo(int x);\n");
      code.append("}\n");
      return code;
    }
  };

  public final void assertBuggySucceeds(String... expectedWarnings)
      throws UnableToCompleteException {
    Result result = assertCompileSucceeds("Buggy buggy = null;", expectedWarnings);
    assertNotNull(result.findClass("test.EntryPoint$Buggy"));
  }

  public final void assertBuggyFails(String... expectedErrors) {
    assertTrue(expectedErrors.length > 0);
    assertCompileFails("Buggy buggy = null;", expectedErrors);
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    try {
      JsInteropRestrictionChecker.exec(logger, program, new MinimalRebuildCache());
    } catch (UnableToCompleteException e) {
      throw new RuntimeException(e);
    }
    return false;
  }
}
