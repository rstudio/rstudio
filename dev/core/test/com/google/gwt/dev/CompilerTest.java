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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.UnstableNestedAnonymousGenerator.OutputVersion;
import com.google.gwt.dev.cfg.EntryMethodHolderGenerator;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResource;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.Files;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Test for {@link Compiler}.
 */
public class CompilerTest extends ArgProcessorTestBase {

  public static final String HELLO_MODULE = "com.google.gwt.sample.hello.Hello";
  public static final String HELLO_MODULE_STACKMODE_STRIP =
      "com.google.gwt.sample.hello.Hello_stackMode_strip";
  private final Compiler.ArgProcessor argProcessor;
  private final CompilerOptionsImpl options = new CompilerOptionsImpl();

  private MockJavaResource packagePrivateParentResource =
      JavaResourceBase.createMockJavaResource("com.foo.PackagePrivateParent",
          "package com.foo;",
          "public abstract class PackagePrivateParent {",
          "  abstract void someFunction();",
          "}");

  private MockJavaResource packagePrivateChildResource =
      JavaResourceBase.createMockJavaResource("com.foo.PackagePrivateChild",
          "package com.foo;",
          "public class PackagePrivateChild extends PackagePrivateParent {",
          "  @Override",
          "  void someFunction() {}",
          "}");

  private MockJavaResource referencesParentResource =
      JavaResourceBase.createMockJavaResource("com.foo.ReferencesParent",
          "package com.foo;",
          "public abstract class ReferencesParent {",
          "  void run() {",
          "    PackagePrivateParent packagePrivateParent = null;",
          "    packagePrivateParent.someFunction();",
          "  }",
          "}");

  private MockJavaResource packagePrivateDispatchEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    ReferencesParent referencesParent = null;",
          "    PackagePrivateChild packagePrivateChild = null;",
          "  }",
          "}");

  private MockJavaResource jsoOne =
      JavaResourceBase.createMockJavaResource("com.foo.JsoOne",
          "package com.foo;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public class JsoOne extends JavaScriptObject {",
          "  protected JsoOne() {",
          "  }",
          "}");

  private MockJavaResource jsoTwo_before =
      JavaResourceBase.createMockJavaResource("com.foo.JsoTwo",
          "package com.foo;",
          "public class JsoTwo {",
          "  protected JsoTwo() {",
          "  }",
          "}");

  private MockJavaResource jsoTwo_after =
      JavaResourceBase.createMockJavaResource("com.foo.JsoTwo",
          "package com.foo;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public class JsoTwo extends JavaScriptObject {",
          "  protected JsoTwo() {",
          "  }",
          "}");

  private MockJavaResource someClassReferringToJsoOneArrays =
      JavaResourceBase.createMockJavaResource("com.foo.SomeClassReferringToJsoOneArrays",
          "package com.foo;",
          "public class SomeClassReferringToJsoOneArrays {",
          "  public static Object createJsoOneArray() { return new JsoOne[30]; }",
          "}");

  private MockJavaResource someClassReferringToJsoTwoArrays =
      JavaResourceBase.createMockJavaResource("com.foo.SomeClassReferringToJsoTwoArrays",
          "package com.foo;",
          "public class SomeClassReferringToJsoTwoArrays {",
          "  public static Object createJsoTwoArray() { return new JsoTwo[30]; }",
          "}");

  private MockJavaResource jsoArrayTestEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    Object o1 = SomeClassReferringToJsoOneArrays.createJsoOneArray();",
          "    Object o2 = SomeClassReferringToJsoTwoArrays.createJsoTwoArray();",
          "  }",
          "}");

  private MockJavaResource simpleModelEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    SimpleModel simpleModel1 = new SimpleModel();",
          "    SimpleModel simpleModel2 = new SimpleModel();",
          "    simpleModel2.copyFrom(simpleModel1);",
          "  }",
          "}");

  private MockJavaResource simpleModelResource =
      JavaResourceBase.createMockJavaResource("com.foo.SimpleModel",
          "package com.foo;",
          "public class SimpleModel {",
          "  private int value = Constants.CONSTANT;",
          "  public void copyFrom(Object object) {}",
          "}");

  private MockJavaResource modifiedFunctionSignatureSimpleModelResource =
      JavaResourceBase.createMockJavaResource("com.foo.SimpleModel",
          "package com.foo;",
          "public class SimpleModel {",
          "  private int value = Constants.CONSTANT;",
          "  public void copyFrom(SimpleModel that) {}",
          "}");

  private MockResource simpleModuleResource =
      JavaResourceBase.createMockResource("com/foo/SimpleModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.TestEntryPoint'/>",
          "</module>");

  private MockJavaResource constantsModelResource =
      JavaResourceBase.createMockJavaResource("com.foo.Constants",
          "package com.foo;",
          "public class Constants {",
          "  public static final int CONSTANT = 0;",
          "}");

  private MockJavaResource modifiedConstantsModelResource =
      JavaResourceBase.createMockJavaResource("com.foo.Constants",
          "package com.foo;",
          "public class Constants {",
          "  public static final int CONSTANT = 2;",
          "}");

  private MockResource unstableGeneratorModuleResource = JavaResourceBase.createMockResource(
      "com/foo/SimpleModule.gwt.xml",
      "<module>",
      "<source path=''/>",
      "<entry-point class='com.foo.TestEntryPoint'/>",
      "<generate-with class='com.google.gwt.dev.UnstableNestedAnonymousGenerator'>",
      "  <when-type-is class='java.lang.Object' />",
      "</generate-with>",
      "</module>");

  private MockResource resourceReadingGeneratorModuleResource =
      JavaResourceBase.createMockResource("com/foo/SimpleModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.TestEntryPoint'/>",
          "<generate-with class='com.google.gwt.dev.FooResourceGenerator'>",
          "  <when-type-is class='java.lang.Object' />",
          "</generate-with>",
          "</module>");

  private MockResource barReferencesFooGeneratorModuleResource =
      JavaResourceBase.createMockResource("com/foo/SimpleModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.TestEntryPoint'/>",
          "<generate-with class='com.google.gwt.dev.BarReferencesFooGenerator'>",
          "  <when-type-is class='java.lang.Object' />",
          "</generate-with>",
          "</module>");

  private MockResource cascadingGeneratorModuleResource =
      JavaResourceBase.createMockResource("com/foo/SimpleModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.TestEntryPoint'/>",
          "<generate-with class='com.google.gwt.dev.CauseStringRebindGenerator'>",
          "  <when-type-is class='java.lang.Object' />",
          "</generate-with>",
          "<generate-with class='com.google.gwt.dev.CauseShortRebindGenerator'>",
          "  <when-type-is class='java.lang.String' />",
          "</generate-with>",
          "<generate-with class='com.google.gwt.dev.FooResourceGenerator'>",
          "  <when-type-is class='java.lang.Short' />",
          "</generate-with>",
          "</module>");

  private MockResource multipleClassGeneratorModuleResource =
      JavaResourceBase.createMockResource("com/foo/SimpleModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.TestEntryPoint'/>",
          "<generate-with class='com.google.gwt.dev.MultipleClassGenerator'>",
          "  <when-type-is class='java.lang.Object' />",
          "</generate-with>",
          "</module>");

  private MockResource classNameToGenerateResource =
      JavaResourceBase.createMockResource("com/foo/generatedClassName.txt",
          "FooReplacementOne");

  private MockResource modifiedClassNameToGenerateResource =
      JavaResourceBase.createMockResource("com/foo/generatedClassName.txt",
          "FooReplacementTwo");

  private MockJavaResource generatorEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "import com.google.gwt.core.client.GWT;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    GWT.create(Object.class);",
          "  }",
          "}");

  private MockJavaResource referencesBarAndGeneratorEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "import com.google.gwt.core.client.GWT;",
          "public class TestEntryPoint implements EntryPoint {",
          "  Bar bar = new Bar();",
          "  @Override",
          "  public void onModuleLoad() {",
          "    GWT.create(Object.class);",
          "  }",
          "}");

  private MockJavaResource superClassOrderEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    SuperClass superClass = new SuperClass();",
          "    ASubClass aSubClass = new ASubClass();",
          "  }",
          "}");

  private MockJavaResource referencedBySuperClassResource =
      JavaResourceBase.createMockJavaResource("com.foo.ReferencedBySuperClass",
          "package com.foo;",
          "public class ReferencedBySuperClass {}");

  private MockJavaResource superClassResource =
      JavaResourceBase.createMockJavaResource("com.foo.SuperClass",
          "package com.foo;",
          "public class SuperClass {",
          "  ReferencedBySuperClass referencedBySuperClass = new ReferencedBySuperClass();",
          "}");

  private MockJavaResource aSubClassResource =
      JavaResourceBase.createMockJavaResource("com.foo.ASubClass",
          "package com.foo;",
          "public class ASubClass extends SuperClass {}");

  private MockJavaResource modifiedSuperEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    Object d = new ModelD();",
          "    if (d instanceof ModelA) {",
          "      // ModelD extends ModelA;",
          "    } else {",
          "      // ModelD does not ModelA;",
          "    }",
          "  }",
          "}");

  private MockJavaResource modelAResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelA",
          "package com.foo;",
          "public class ModelA {}");

  private MockJavaResource modelBResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelB",
          "package com.foo;",
          "public class ModelB extends ModelA {}");

  private MockJavaResource modelCResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelC",
          "package com.foo;",
          "public class ModelC {}");

  private MockJavaResource modifiedSuperModelCResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelC",
          "package com.foo;",
          "public class ModelC extends ModelA {}");

  private MockJavaResource modelDResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelD",
          "package com.foo;",
          "public class ModelD extends ModelC {}");

  private MockJavaResource modifiedJsoIntfDispatchEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    Caller.call(this);",
          "  }",
          "",
          "  public void runTest(FooInterface fooInterface) {",
          "    fooInterface.run();",
          "  }",
          "}");

  private MockJavaResource callerResource =
      JavaResourceBase.createMockJavaResource("com.foo.Caller",
          "package com.foo;",
          "public class Caller {",
          "  public static void call(TestEntryPoint testEntryPoint) {",
          "    testEntryPoint.runTest(Foo.createFoo());",
          "  }",
          "}");

  private MockJavaResource sameContentDifferentTimeFooResource = JavaResourceBase
      .createMockJavaResource("com.foo.Foo",
          "package com.foo;",
          "public class Foo {}");

  private MockJavaResource fooInterfaceResource =
      JavaResourceBase.createMockJavaResource("com.foo.FooInterface",
          "package com.foo;",
          "public interface FooInterface {",
          "  void run();",
          "}");

  private MockJavaResource jsoFooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Foo",
          "package com.foo;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public final class Foo extends JavaScriptObject implements FooInterface {",
          "  public static native Foo createFoo() /*-{",
          "    return {};",
          "  }-*/;",
          "",
          "  protected Foo() {}",
          "",
          "  @Override",
          "  public void run() {}",
          "}");

  private MockJavaResource nonJsoFooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Foo",
          "package com.foo;",
          "public class Foo implements FooInterface {",
          "  public static Foo createFoo() {",
          "    return new Foo();",
          "  }",
          "",
          "  @Override",
          "  public void run() {}",
          "}");

  private MockJavaResource fooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Foo",
          "package com.foo;",
          "public class Foo {}");

  private MockJavaResource barReferencesFooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Bar",
          "package com.foo;",
          "public class Bar {",
          "  Foo foo = new Foo();",
          "}");

  private MockJavaResource nonCompilableFooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Foo",
          "package com.foo;",
          "public class Foo {",
          "  public void run() {",
          "    // Not available in GWT.",
          "    String.format(\"asdf\");",
          "  }",
          "}");

  private MockJavaResource emptyEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {}",
          "}");

  private MockJavaResource bazResource =
      JavaResourceBase.createMockJavaResource("com.foo.Baz",
          "package com.foo;",
          "public class Baz {}");

  private MockJavaResource regularFooImplemetorResource =
      JavaResourceBase.createMockJavaResource("com.foo.FooImplementor",
          "package com.foo;",
          "public class FooImplementor implements FooInterface {",
          "  @Override",
          "  public void run() {}",
          "}");

  private MockJavaResource simpleFactory =
      JavaResourceBase.createMockJavaResource("com.foo.SimpleFactory",
          "package com.foo;",
          "public class SimpleFactory {",
          "  public static SimpleIntf getJso() {",
          "    return getJsoImpl();",
          "  };",
          "  public static native SimpleJso getJsoImpl() /*-{",
          "    return null;",
          "  }-*/;",
          "}");

  private MockJavaResource simpleJso =
      JavaResourceBase.createMockJavaResource("com.foo.SimpleJso",
          "package com.foo;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public class SimpleJso extends JavaScriptObject implements SimpleIntf {",
          "  protected SimpleJso() {",
          "  }",
          "  public final void method() {",
          "  }",
          "}");

  private MockJavaResource simpleIntf =
      JavaResourceBase.createMockJavaResource("com.foo.SimpleIntf",
          "package com.foo;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public interface SimpleIntf {",
          "  public void method();",
          "}");

  private MockJavaResource jsoTestEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    SimpleFactory.getJso().method();",
          "  }",
          "}");

  private MockResource jsoTestModuleResource =
      JavaResourceBase.createMockResource("com/foo/SimpleModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.TestEntryPoint'/>",
          "</module>");

  private MockResource myWidgetUiXml =
      JavaResourceBase.createMockResource("com/foo/MyWidget.ui.xml",
          "<ui:UiBinder xmlns:ui='urn:ui:com.google.gwt.uibinder'",
          "    xmlns:g='urn:import:com.google.gwt.user.client.ui'>",
          "<g:HTMLPanel>",
          "  Hello, <g:ListBox ui:field='myListBox' visibleItemCount='1'/>.",
          "</g:HTMLPanel>",
          "</ui:UiBinder>");

  private MockResource myWidgetWithWhiteStyleUiXml =
      JavaResourceBase.createMockResource("com/foo/MyWidget.ui.xml",
          "<ui:UiBinder xmlns:ui='urn:ui:com.google.gwt.uibinder'",
          "    xmlns:g='urn:import:com.google.gwt.user.client.ui'>",
          "  <ui:style>",
          "  .title {",
          "    background-color: white;",
          "  }",
          "  </ui:style>",
          "  <g:HTMLPanel><g:ListBox ui:field='myListBox' visibleItemCount='1'/></g:HTMLPanel>",
          "</ui:UiBinder>");

  private MockResource myWidgetWithGreyStyleUiXml =
      JavaResourceBase.createMockResource("com/foo/MyWidget.ui.xml",
          "<ui:UiBinder xmlns:ui='urn:ui:com.google.gwt.uibinder'",
          "    xmlns:g='urn:import:com.google.gwt.user.client.ui'>",
          "  <ui:style>",
          "  .title {",
          "    background-color: grey;",
          "  }",
          "  </ui:style>",
          "  <g:HTMLPanel><g:ListBox ui:field='myListBox' visibleItemCount='1'/></g:HTMLPanel>",
          "</ui:UiBinder>");

  private MockJavaResource myWidget =
      JavaResourceBase.createMockJavaResource("com.foo.MyWidget",
          "package com.foo;",
          "import com.google.gwt.core.client.GWT;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "import com.google.gwt.uibinder.client.UiBinder;",
          "import com.google.gwt.uibinder.client.UiField;",
          "import com.google.gwt.user.client.ui.Composite;",
          "import com.google.gwt.user.client.ui.ListBox;",
          "import com.google.gwt.user.client.ui.Widget;",
          "public class MyWidget extends Composite {",
          "  interface Binder extends UiBinder<Widget, MyWidget> {",
          "  }",
          "  private static final Binder binder = GWT.create(Binder.class);",
          "  @UiField ListBox myListBox;",
          "  public MyWidget() {",
          "    init();",
          "  }",
          "  protected void init() {",
          "    initWidget(binder.createAndBindUi(this));",
          "    myListBox.addItem(\"One\");",
          "  }",
          "}");

  private MockJavaResource uiBinderTestEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "import com.google.gwt.dom.client.Node;",
          "import com.google.gwt.user.client.ui.RootPanel;",
          "public class TestEntryPoint implements EntryPoint {",
          "  private Node node;",
          "  private MyWidget widget;",
          "  @Override",
          "  public void onModuleLoad() {",
          "    node = null; widget = new MyWidget();",
          "    RootPanel.get().add(widget);",
          "  }",
          "}");

  private MockResource uiBinderTestModuleResource =
      JavaResourceBase.createMockResource("com/foo/UiBinderTestModule.gwt.xml",
          "<module>",
          "  <inherits name='com.google.gwt.core.Core'/>",
          "  <inherits name='com.google.gwt.user.User' />",
          "  <source path=''/>",
          "  <set-property name='user.agent' value='safari'/>",
          "  <entry-point class='com.foo.TestEntryPoint'/>",
          "</module>");

  private MockResource devirtualizeStringModuleResource =
      JavaResourceBase.createMockResource("com/foo/DevirtualizeStringModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.DevirtualizeStringEntryPoint'/>",
          "</module>");

  private MockJavaResource devirtualizeStringEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.DevirtualizeStringEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class DevirtualizeStringEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    CharSequence seq = \"A\";",
          "    seq.subSequence(0,1);",
          "  }",
          "}");

  private MockResource transitivelyFoldableConstantModuleResource =
      JavaResourceBase.createMockResource(
          "com/foo/TransitivelyFoldableConstantModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.TransitivelyFoldableConstantEntryPoint'/>",
          "</module>");

  private MockJavaResource transitivelyFoldableConstantEntryPointResource =
      JavaResourceBase.createMockJavaResource(
          "com.foo.TransitivelyFoldableConstantEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TransitivelyFoldableConstantEntryPoint",
          "    implements EntryPoint {",
          "  static final int CONST = 1 + ClassOne.CONST;",
          "  @Override",
          "  public void onModuleLoad() {",
          "    int c = CONST;",
          "  }",
          "}");

  private MockJavaResource classOneResource =
      JavaResourceBase.createMockJavaResource(
          "com.foo.ClassOne",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class ClassOne {",
          "  static final int CONST = 1 + ClassTwo.CONST;",
          "}");

  private MockJavaResource classTwoResource =
      JavaResourceBase.createMockJavaResource(
          "com.foo.ClassTwo",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class ClassTwo {",
          "  static final int CONST = 1 + 3;",
          "}");

  private MockResource superFromStaleInnerModuleResource =
      JavaResourceBase.createMockResource(
          "com/foo/SuperFromStaleInnerModule.gwt.xml",
          "<module>",
          "  <source path=''/>",
          "  <entry-point class='com.foo.SuperFromStaleInnerEntryPoint'/>",
          "</module>");

  private MockJavaResource superFromStaleInnerEntryPointResource =
      JavaResourceBase.createMockJavaResource(
          "com.foo.SuperFromStaleInnerEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class SuperFromStaleInnerEntryPoint",
          "    implements EntryPoint {",
          "  static class A { int f() { return 1; } }",
          "  static class B extends A {",
          "    int f() { return 2; }",
          "    void g() {",
          "      new InterfaceOne() {",
          "        public int m() { return 2 + B.super.f(); }",
          "      }.m();",
          "    }",
          "  }",
          "  @Override",
          "  public void onModuleLoad() {",
          "    new B().g();",
          "  }",
          "}");

  private MockJavaResource interfaceOneResource =
      JavaResourceBase.createMockJavaResource(
          "com.foo.InterfaceOne",
          "package com.foo;",
          "interface InterfaceOne {",
          "  int m();",
          "}");

  private MockResource helloModuleResource =
      JavaResourceBase.createMockResource(
          "com/foo/Hello.gwt.xml",
          "<module>",
          "  <inherits name='com.google.gwt.user.User'/>",
          "  <source path=''/>",
          "  <set-property name='user.agent' value='safari'/>",
          "  <entry-point class='com.foo.HelloEntryPoint'/>",
          "</module>");

  private MockJavaResource helloEntryPointResource =
      JavaResourceBase.createMockJavaResource(
          "com.foo.HelloEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "import com.google.gwt.event.dom.client.ClickEvent;",
          "import com.google.gwt.event.dom.client.ClickHandler;",
          "import com.google.gwt.user.client.Window;",
          "import com.google.gwt.user.client.ui.Button;",
          "import com.google.gwt.user.client.ui.RootPanel;",
          "public class HelloEntryPoint implements EntryPoint {",
          "  public void onModuleLoad() {",
          "    Button b = new Button(\"Click me\", new ClickHandler() {",
          "        public void onClick(ClickEvent event) {",
          "          Window.alert(\"Hello, AJAX\");",
          "        }",
          "      });",
          "    RootPanel.get().add(b);" ,
          "  }",
          "}");

  private Set<String> emptySet = stringSet();

  public CompilerTest() {
    argProcessor = new Compiler.ArgProcessor(options);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    FooResourceGenerator.runCount = 0;
    BarReferencesFooGenerator.runCount = 0;
    CauseStringRebindGenerator.runCount = 0;
    CauseShortRebindGenerator.runCount = 0;
  }

  public void testAllValidArgs() {
    assertProcessSuccess(argProcessor, new String[] {"-logLevel", "DEBUG", "-style",
        "PRETTY", "-ea", "-XdisableAggressiveOptimization", "-gen", "myGen",
        "-war", "myWar", "-workDir", "myWork", "-extra", "myExtra", "-incremental",
        "-localWorkers", "2", "-sourceLevel", "1.7", "c.g.g.h.H", "my.Module"});

    assertEquals(new File("myGen").getAbsoluteFile(),
        options.getGenDir().getAbsoluteFile());
    assertEquals(new File("myWar"), options.getWarDir());
    assertEquals(new File("myWork"), options.getWorkDir());
    assertEquals(new File("myExtra"), options.getExtraDir());

    assertEquals(2, options.getLocalWorkers());

    assertEquals(TreeLogger.DEBUG, options.getLogLevel());
    assertEquals(JsOutputOption.PRETTY, options.getOutput());
    assertTrue(options.isEnableAssertions());
    assertFalse(options.shouldClusterSimilarFunctions());
    assertFalse(options.shouldInlineLiteralParameters());
    assertFalse(options.shouldOptimizeDataflow());
    assertFalse(options.shouldOrdinalizeEnums());
    assertFalse(options.shouldRemoveDuplicateFunctions());
    assertTrue(options.isIncrementalCompileEnabled());

    assertEquals(SourceLevel.JAVA7, options.getSourceLevel());

    assertEquals(2, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
    assertEquals("my.Module", options.getModuleNames().get(1));
  }

  public void testDefaultArgs() {
    assertProcessSuccess(argProcessor, new String[] {"c.g.g.h.H"});

    assertEquals(null, options.getGenDir());
    assertEquals(new File("war").getAbsoluteFile(),
        options.getWarDir().getAbsoluteFile());
    assertEquals(null, options.getWorkDir());
    assertEquals(null, options.getExtraDir());

    assertEquals(TreeLogger.INFO, options.getLogLevel());
    assertEquals(JsOutputOption.OBFUSCATED, options.getOutput());
    assertFalse(options.isEnableAssertions());
    assertTrue(options.shouldClusterSimilarFunctions());
    assertTrue(options.shouldInlineLiteralParameters());
    assertTrue(options.shouldOptimizeDataflow());
    assertTrue(options.shouldOrdinalizeEnums());
    assertTrue(options.shouldRemoveDuplicateFunctions());
    assertFalse(options.isIncrementalCompileEnabled());

    assertEquals(1, options.getLocalWorkers());

    assertEquals(1, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
  }

  public void testForbiddenArgs() {
    assertProcessFailure(argProcessor, "Unknown argument", new String[]{"-out", "www"});
    assertProcessFailure(argProcessor, "Source level must be one of",
        new String[]{"-sourceLevel", "ssss"});
    assertProcessFailure(argProcessor, "Source level must be one of",
        new String[]{"-sourceLevel", "1.5"});
  }

  /**
   * Tests ordering for emum {@link SourceLevel}.
   */
  public void testSourceLevelOrdering() {
    SourceLevel[] sourceLevels = SourceLevel.values();
    SourceLevel previousSourceLevel = sourceLevels[0];
    for (int i = 1; i < sourceLevels.length; i++) {
      assertTrue(Utility.versionCompare(previousSourceLevel.getStringValue(),
          sourceLevels[i].getStringValue()) < 0);
      previousSourceLevel = sourceLevels[i];
    }
  }

  public void testSourceLevelSelection() {
    // We are not able to compile to less that Java 6 so, we might as well do Java7 on
    // these cases.
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.4"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.5"));

    assertEquals(SourceLevel.JAVA6, SourceLevel.getBestMatchingVersion("1.6"));
    assertEquals(SourceLevel.JAVA6, SourceLevel.getBestMatchingVersion("1.6_26"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.7"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.8"));

    // not proper version strings => default to JAVA7.
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.6u3"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.6b3"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.7b3"));
  }

  public void testDeterministicBuild_Draft_StackModeStrip() throws
      UnableToCompleteException, IOException {
    assertDeterministicBuild(HELLO_MODULE_STACKMODE_STRIP, 0);
  }

  public void testDeterministicBuild_Optimized_StackModeStrip() throws
      UnableToCompleteException, IOException {
    assertDeterministicBuild(HELLO_MODULE_STACKMODE_STRIP, 9);
  }

  public void testDeterministicBuild_Draft() throws UnableToCompleteException, IOException {
    assertDeterministicBuild(HELLO_MODULE, 0);
  }

  public void testDeterministicBuild_Optimized() throws UnableToCompleteException, IOException {
    assertDeterministicBuild(HELLO_MODULE, 9);
  }

  // TODO(stalcup): add recompile tests for file deletion.

  public void testIncrementalRecompile_noop() throws UnableToCompleteException, IOException,
      InterruptedException {
    checkIncrementalRecompile_noop(JsOutputOption.PRETTY);
    checkIncrementalRecompile_noop(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_dateStampChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_dateStampChange(JsOutputOption.PRETTY);
    checkIncrementalRecompile_dateStampChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_invalidatePreamble() throws UnableToCompleteException,
      IOException, InterruptedException {
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();

    // Perform a first compile.
    compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, emptyEntryPointResource),
        relinkMinimalRebuildCache, null, JsOutputOption.PRETTY);
    // On first compile nothing is explicitly stale, only implicitly stale.
    assertEquals(0, relinkMinimalRebuildCache.getStaleTypeNames().size());

    // Recompile with a deep change that invalidates the preamble.
    relinkMinimalRebuildCache.markSourceFileStale("java/lang/Object.java");
    compileToJs(relinkApplicationDir, "com.foo.SimpleModule", Lists.<MockResource> newArrayList(),
        relinkMinimalRebuildCache, null, JsOutputOption.PRETTY);
    // Show that preamble invalidation marks everything stale.
    assertTrue(relinkMinimalRebuildCache.getProcessedStaleTypeNames().size() > 100);

    // Recompile again with a tiny change. Prove that it's not stuck repeatedly invalidating the
    // whole world.
    compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource> newArrayList(emptyEntryPointResource), relinkMinimalRebuildCache, null,
        JsOutputOption.PRETTY);
    // Show that only this little change is stale, not the whole world.
    assertEquals(2, relinkMinimalRebuildCache.getProcessedStaleTypeNames().size());
  }

  public void testIncrementalRecompile_superClassOrder() throws UnableToCompleteException,
      IOException,
      InterruptedException {
    // Linked output is sorted alphabetically except that super-classes come before sub-classes. If
    // on recompile a sub-class -> super-class relationship is lost then a sub-class with an
    // alphabetically earlier name might start linking out before the super-class.
    checkIncrementalRecompile_superClassOrder(JsOutputOption.PRETTY);
    checkIncrementalRecompile_superClassOrder(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_superFromStaleInner() throws UnableToCompleteException,
      IOException,
      InterruptedException {
    checkIncrementalRecompile_superFromStaleInner(JsOutputOption.PRETTY);
    checkIncrementalRecompile_superFromStaleInner(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_deterministicUiBinder() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_deterministicUiBinder(JsOutputOption.PRETTY);
    checkIncrementalRecompile_deterministicUiBinder(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_uiBinderCssChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_uiBinderCssChange(JsOutputOption.PRETTY);
    checkIncrementalRecompile_uiBinderCssChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_unstableGeneratorReferencesModifiedType()
      throws UnableToCompleteException, IOException, InterruptedException {
    checkIncrementalRecompile_unstableGeneratorReferencesModifiedType(JsOutputOption.PRETTY);
    checkIncrementalRecompile_unstableGeneratorReferencesModifiedType(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_functionSignatureChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty output since the Pretty namer's behavior is order
    // dependent, and while still correct, will come out different in a recompile with this change
    // versus a from scratch compile with this change.
    checkIncrementalRecompile_functionSignatureChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_compileTimeConstantChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_compileTimeConstantChange(JsOutputOption.DETAILED);
    checkIncrementalRecompile_compileTimeConstantChange(JsOutputOption.PRETTY);
  }

  public void testIncrementalRecompile_transitivelyFoldableConstant()
      throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_transitivelyFoldableConstant(JsOutputOption.DETAILED);
    checkIncrementalRecompile_transitivelyFoldableConstant(JsOutputOption.PRETTY);
  }

  public void testIncrementalRecompile_packagePrivateDispatch() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_packagePrivateOverride(JsOutputOption.PRETTY);
    checkIncrementalRecompile_packagePrivateOverride(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_regularClassMadeIntoJsoClass()
      throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty output since the Pretty namer's behavior is order
    // dependent, and while still correct, will come out different in a recompile with this change
    // versus a from scratch compile with this change.
    checkIncrementalRecompile_regularClassMadeIntoJsoClass(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_unreachableIncompatibleChange()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Foo class is uncompilable but unreachable, so the first compile should succeed. Modifying it
    // should still succeed since staleness marking should be smart enough to not force it to be
    // unnecessarily traversed.
    checkIncrementalRecompile_unreachableIncompatibleChange(JsOutputOption.PRETTY);
    checkIncrementalRecompile_unreachableIncompatibleChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_typeHierarchyChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_typeHierarchyChange(JsOutputOption.PRETTY);
    checkIncrementalRecompile_typeHierarchyChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_devirtualizeUnchangedJso() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Tests that a JSO calls through interfaces are correctly devirtualized when compiling per file
    // and the JSOs nor their single impl interfaces are not stale.
    checkIncrementalRecompile_devirtualizeUnchangedJso(JsOutputOption.PRETTY);
    checkIncrementalRecompile_devirtualizeUnchangedJso(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_devirtualizeString() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Tests that String calls through interfaces are correctly devirtualized when compiling per
    // file and neither String nor CharSequence interface are stale.
    checkIncrementalRecompile_devirtualizeString(JsOutputOption.PRETTY);
    checkIncrementalRecompile_devirtualizeString(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_multipleClassGenerator() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Tests that a Generated type that is not directly referenced from the rebound GWT.create()
    // call is still marked stale, regenerated, retraversed and output as JS.
    checkIncrementalRecompile_multipleClassGenerator(JsOutputOption.PRETTY);
    checkIncrementalRecompile_multipleClassGenerator(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_singleJsoIntfDispatchChange()
      throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty output since the Pretty namer's behavior is order
    // dependent, and while still correct, will come out different in a recompile with this change
    // versus a from scratch compile with this change.
    checkIncrementalRecompile_singleJsoIntfDispatchChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_dualJsoIntfDispatchChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty output since the Pretty namer's behavior is order
    // dependent, and while still correct, will come out different in a recompile with this change
    // versus a from scratch compile with this change.
    checkIncrementalRecompile_dualJsoIntfDispatchChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_generatorInputResourceChange() throws IOException,
      UnableToCompleteException, InterruptedException {
    // Not testing recompile equality with Pretty output since the Pretty namer's behavior is order
    // dependent, and while still correct, will come out different in a recompile with this change
    // versus a from scratch compile with this change.
    checkIncrementalRecompile_generatorInputResourceChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_invalidatedGeneratorOutputRerunsGenerator()
      throws UnableToCompleteException, IOException, InterruptedException {
    // BarReferencesFoo Generator hasn't run yet.
    assertEquals(0, BarReferencesFooGenerator.runCount);

    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    List<MockResource> sharedResources =
        Lists.newArrayList(barReferencesFooGeneratorModuleResource, generatorEntryPointResource);
    JsOutputOption output = JsOutputOption.PRETTY;

    List<MockResource> originalResources = Lists.newArrayList(sharedResources);
    originalResources.add(fooResource);

    // Compile the app with original files, modify a file and do a per-file recompile.
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.SimpleModule", originalResources,
        relinkMinimalRebuildCache, emptySet, output);

    // BarReferencesFoo Generator has now been run once.
    assertEquals(1, BarReferencesFooGenerator.runCount);

    // Recompile with no changes, which should not trigger any Generator runs.
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource>newArrayList(), relinkMinimalRebuildCache, emptySet, output);

    // Since there were no changes BarReferencesFoo Generator was not run again.
    assertEquals(1, BarReferencesFooGenerator.runCount);

    // Recompile with a modified Foo class, which should invalidate Bar which was generated by a
    // GWT.create() call in the entry point.
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource>newArrayList(fooResource), relinkMinimalRebuildCache,
        stringSet("com.foo.TestEntryPoint", "com.foo.Foo", "com.foo.Bar"), output);

    // BarReferencesFoo Generator was run again.
    assertEquals(2, BarReferencesFooGenerator.runCount);
  }

  public void testIncrementalRecompile_invalidatedGeneratorOutputRerunsCascadedGenerators()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Generators haven't run yet.
    assertEquals(0, CauseStringRebindGenerator.runCount);
    assertEquals(0, CauseShortRebindGenerator.runCount);
    assertEquals(0, FooResourceGenerator.runCount);

    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    List<MockResource> sharedResources = Lists.newArrayList(cascadingGeneratorModuleResource,
        generatorEntryPointResource, classNameToGenerateResource);
    JsOutputOption output = JsOutputOption.PRETTY;

    List<MockResource> originalResources = Lists.newArrayList(sharedResources);

    // Compile the app with original files.
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.SimpleModule", originalResources,
        relinkMinimalRebuildCache, emptySet, output);

    // Generators have now been run once.
    assertEquals(1, CauseStringRebindGenerator.runCount);
    assertEquals(1, CauseShortRebindGenerator.runCount);
    assertEquals(1, FooResourceGenerator.runCount);

    // Recompile with no changes, which should not trigger any Generator runs.
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource> newArrayList(), relinkMinimalRebuildCache, emptySet, output);

    // Since there were no changes Generators were not run again.
    assertEquals(1, CauseStringRebindGenerator.runCount);
    assertEquals(1, CauseShortRebindGenerator.runCount);
    assertEquals(1, FooResourceGenerator.runCount);

    // Recompile with a modified resource, which should invalidate the output of the
    // FooResourceGenerator and cascade the invalidate the Generators that triggered
    // FooResourceGenerator.
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource> newArrayList(modifiedClassNameToGenerateResource),
        relinkMinimalRebuildCache, stringSet("com.foo.TestEntryPoint", "com.foo.Baz$InnerBaz",
            "com.foo.Bar", "com.foo.HasCustomContent", "com.foo.FooReplacementTwo"), output);

    // Generators were run again.
    assertEquals(2, CauseStringRebindGenerator.runCount);
    assertEquals(2, CauseShortRebindGenerator.runCount);
    assertEquals(2, FooResourceGenerator.runCount);
  }

  public void testIncrementalRecompile_carriesOverGeneratorArtifacts()
      throws UnableToCompleteException,
      IOException, InterruptedException {
    // Foo Generator hasn't run yet.
    assertEquals(0, FooResourceGenerator.runCount);

    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    List<MockResource> sharedResources = Lists.newArrayList(resourceReadingGeneratorModuleResource,
        referencesBarAndGeneratorEntryPointResource, classNameToGenerateResource,
        barReferencesFooResource);
    JsOutputOption output = JsOutputOption.PRETTY;

    List<MockResource> originalResources = Lists.newArrayList(sharedResources);
    originalResources.add(fooResource);

    // Compile the app with original files.
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.SimpleModule", originalResources,
        relinkMinimalRebuildCache, emptySet, output);

    // Foo Generator has now been run once.
    assertEquals(1, FooResourceGenerator.runCount);
    // The bar.txt artifact was output.
    File barFile = new File(relinkApplicationDir.getPath() + File.separator + "com.foo.SimpleModule"
        + File.separator + "bar.txt");
    assertTrue(barFile.exists());

    // Recompile with just 1 file change, which should not trigger any Generator runs.
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource> newArrayList(fooResource), relinkMinimalRebuildCache,
        stringSet("com.foo.Foo", "com.foo.Bar"), output);

    // Foo Generator was not run again.
    assertEquals(1, FooResourceGenerator.runCount);
    // But the bar.txt artifact was still output.
    barFile = new File(relinkApplicationDir.getPath() + File.separator + "com.foo.SimpleModule"
        + File.separator + "bar.txt");
    assertTrue(barFile.exists());
  }

  /**
   * Regression test for UnifyAST assertion failure problem in incremental SDM.
   */
  public void testIncrementalRecompile_unifyASTAssertionRegression()
      throws UnableToCompleteException, IOException, InterruptedException {

    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    List<MockResource> originalResources = Lists.newArrayList(helloEntryPointResource,
        helloModuleResource);
    JsOutputOption output = JsOutputOption.PRETTY;

    // Compile the app with original files.
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.Hello",
        originalResources, relinkMinimalRebuildCache, emptySet, output);
  }

  private void checkIncrementalRecompile_noop(JsOutputOption output) throws UnableToCompleteException,
      IOException, InterruptedException {
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();

    String originalJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule", Lists
        .newArrayList(simpleModuleResource, simpleModelEntryPointResource, simpleModelResource,
            constantsModelResource),
        relinkMinimalRebuildCache, emptySet, output);

    // Compile again with absolutely no file changes and reusing the minimalRebuildCache.
    String relinkedJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource> newArrayList(), relinkMinimalRebuildCache, emptySet, output);

    assertTrue(originalJs.equals(relinkedJs));
  }

  private void checkIncrementalRecompile_dateStampChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();

    String originalJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule", Lists
        .newArrayList(simpleModuleResource, simpleModelEntryPointResource, simpleModelResource,
            constantsModelResource),
        relinkMinimalRebuildCache, emptySet, output);

    // Compile again with the same source but a new date stamp on SimpleModel and reusing the
    // minimalRebuildCache.
    String relinkedJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource> newArrayList(simpleModelResource), relinkMinimalRebuildCache,
        stringSet("com.foo.TestEntryPoint", "com.foo.SimpleModel"), output);

    assertTrue(originalJs.equals(relinkedJs));
  }

  private void checkIncrementalRecompile_superClassOrder(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    // Mark ReferencedBySuperClassResource modified, so that SuperClass becomes stale. This will
    // result in SuperClass's indexing being rebuilt but NOT ASubClasses's. If there is a bug in the
    // index updates then the link output ordering will change.
    checkRecompiledModifiedApp("com.foo.SimpleModule", Lists.newArrayList(simpleModuleResource,
            superClassOrderEntryPointResource, referencedBySuperClassResource, superClassResource,
            aSubClassResource), referencedBySuperClassResource, referencedBySuperClassResource,
        stringSet("com.foo.SuperClass", "com.foo.ReferencedBySuperClass"), output);
  }

  private void checkIncrementalRecompile_superFromStaleInner(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    // Makes sure that type ids for (effectively static) super dispatches, i.e. of the form
    // super.m() or X.this.m(), are computed also for classes only referenced by stale classes.
    checkRecompiledModifiedApp("com.foo.SuperFromStaleInnerModule",
        Lists.newArrayList(superFromStaleInnerModuleResource,
            superFromStaleInnerEntryPointResource), interfaceOneResource, interfaceOneResource,
        stringSet("com.foo.SuperFromStaleInnerEntryPoint$B$1", "com.foo.InterfaceOne"), output);
  }

  private void checkIncrementalRecompile_deterministicUiBinder(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();

    checkRecompiledModifiedApp(compilerOptions, "com.foo.UiBinderTestModule", Lists.newArrayList(
        uiBinderTestModuleResource, uiBinderTestEntryPointResource, myWidgetUiXml), myWidget,
        myWidget, stringSet("com.foo.MyWidget", "com.foo.MyWidget$Binder", "com.foo.TestEntryPoint",
            "com.foo.MyWidget_BinderImpl", "com.foo.MyWidget_BinderImpl$Widgets"), output);
  }

  private void checkIncrementalRecompile_uiBinderCssChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    // Switches from a white styled widget to a grey styled widget in the CSS in the style tag
    // nested in the .ui.xml template file.
    checkRecompiledModifiedApp("com.foo.UiBinderTestModule",
        Lists.newArrayList(uiBinderTestModuleResource, uiBinderTestEntryPointResource, myWidget),
        myWidgetWithWhiteStyleUiXml, myWidgetWithGreyStyleUiXml,
        stringSet("com.foo.MyWidget",
            "com.foo.MyWidget_BinderImpl$Template",
            "com.foo.MyWidget_BinderImpl_GenBundle_default_InlineClientBundleGenerator",
            "com.foo.MyWidget_BinderImpl_GenBundle_default_InlineClientBundleGenerator$1",
            "com.foo.MyWidget_BinderImpl_TemplateImpl",
            "com.foo.MyWidget_BinderImpl_GenBundle_default_InlineClientBundleGenerator$styleInitializer",
            "com.foo.MyWidget_BinderImpl", "com.foo.MyWidget_BinderImpl$Widgets"), output);
  }

  private void checkIncrementalRecompile_packagePrivateOverride(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule", Lists.newArrayList(
        simpleModuleResource, packagePrivateDispatchEntryPointResource,
        packagePrivateParentResource, referencesParentResource), packagePrivateChildResource,
        packagePrivateChildResource,
        stringSet("com.foo.PackagePrivateChild", "com.foo.TestEntryPoint"), output);
  }

  private void checkIncrementalRecompile_regularClassMadeIntoJsoClass(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule", Lists.newArrayList(
        simpleModuleResource, jsoArrayTestEntryPointResource, someClassReferringToJsoOneArrays,
        someClassReferringToJsoTwoArrays, jsoOne), jsoTwo_before, jsoTwo_after,
        stringSet("com.foo.JsoTwo", "com.foo.SomeClassReferringToJsoTwoArrays"), output);
  }

  private void checkIncrementalRecompile_unstableGeneratorReferencesModifiedType(
      JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    // Make sure that the recompile with changes and the full compile with changes have the same
    // output from the unstable generator, so that it is safe to make byte for byte output
    // comparisons.
    UnstableNestedAnonymousGenerator.outputVersionOrder.addAll(Lists.newArrayList(
        // first compile
        OutputVersion.A,
        // recompile with changes
        OutputVersion.B,
        // full compile with changes
        OutputVersion.B));
    checkRecompiledModifiedApp("com.foo.SimpleModule",
        Lists.newArrayList(generatorEntryPointResource, unstableGeneratorModuleResource),
        fooResource, sameContentDifferentTimeFooResource, stringSet(
            "com.foo.NestedAnonymousClasses$1", "com.foo.NestedAnonymousClasses",
            "com.foo.NestedAnonymousClasses$ClassTwo", "com.foo.NestedAnonymousClasses$ClassOne",
            "com.foo.TestEntryPoint", "com.foo.NestedAnonymousClasses$1$1", "com.foo.Foo"), output);
  }

  private void checkIncrementalRecompile_functionSignatureChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    checkRecompiledModifiedApp("com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, simpleModelEntryPointResource,
            constantsModelResource),
        simpleModelResource, modifiedFunctionSignatureSimpleModelResource,
        stringSet("com.foo.TestEntryPoint", "com.foo.SimpleModel"), output);
  }

  private void checkIncrementalRecompile_compileTimeConstantChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    checkRecompiledModifiedApp("com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, simpleModelEntryPointResource,
            simpleModelResource),
        constantsModelResource, modifiedConstantsModelResource,
        stringSet("com.foo.SimpleModel", "com.foo.Constants"), output);
  }

  private void checkIncrementalRecompile_transitivelyFoldableConstant(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    // Tests that constants that are provided by types are only referenced by reference only types
    // (hence not traversed) are still available for constant propagation.
    checkRecompiledModifiedApp("com.foo.TransitivelyFoldableConstantModule",
        Lists.newArrayList(transitivelyFoldableConstantModuleResource, classOneResource,
            classTwoResource),
        transitivelyFoldableConstantEntryPointResource,
        transitivelyFoldableConstantEntryPointResource,
        stringSet("com.foo.TransitivelyFoldableConstantEntryPoint",
            getEntryMethodHolderTypeName("com.foo.TransitivelyFoldableConstantModule")),
        output);
  }

  private void checkIncrementalRecompile_unreachableIncompatibleChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    checkRecompiledModifiedApp("com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, emptyEntryPointResource), nonCompilableFooResource,
        nonCompilableFooResource, stringSet(), output);
  }

  private void checkIncrementalRecompile_typeHierarchyChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    checkRecompiledModifiedApp("com.foo.SimpleModule", Lists.newArrayList(simpleModuleResource,
        modifiedSuperEntryPointResource, modelAResource, modelBResource, modelDResource),
        modelCResource, modifiedSuperModelCResource,
        stringSet("com.foo.TestEntryPoint", "com.foo.ModelC", "com.foo.ModelD"), output);
  }

  private void checkIncrementalRecompile_singleJsoIntfDispatchChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule", Lists.newArrayList(
        simpleModuleResource, modifiedJsoIntfDispatchEntryPointResource, callerResource,
        fooInterfaceResource), nonJsoFooResource, jsoFooResource,
        stringSet("com.foo.TestEntryPoint", "com.foo.Foo", "com.foo.Caller"), output);
  }

  private void checkIncrementalRecompile_devirtualizeUnchangedJso(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule",
        Lists.newArrayList(jsoTestModuleResource, simpleFactory, simpleIntf, simpleJso),
        jsoTestEntryPointResource, jsoTestEntryPointResource, stringSet("com.foo.TestEntryPoint",
        getEntryMethodHolderTypeName("com.foo.SimpleModule")), output);
  }

  private void checkIncrementalRecompile_devirtualizeString(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.DevirtualizeStringModule",
        Lists.newArrayList(devirtualizeStringModuleResource),
        devirtualizeStringEntryPointResource, devirtualizeStringEntryPointResource,
        stringSet("com.foo.DevirtualizeStringEntryPoint",
            getEntryMethodHolderTypeName("com.foo.DevirtualizeStringModule")),
        output);
  }

  private void checkIncrementalRecompile_multipleClassGenerator(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule",
        Lists.newArrayList(multipleClassGeneratorModuleResource, generatorEntryPointResource),
        bazResource, bazResource,
        stringSet("com.foo.Baz", "com.foo.TestEntryPoint", "com.foo.Bar"), output);
  }

  private void checkIncrementalRecompile_dualJsoIntfDispatchChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule", Lists.newArrayList(
        simpleModuleResource, modifiedJsoIntfDispatchEntryPointResource, callerResource,
        fooInterfaceResource, regularFooImplemetorResource), nonJsoFooResource, jsoFooResource,
        stringSet("com.foo.TestEntryPoint", "com.foo.Foo", "com.foo.Caller"), output);
  }

  private void checkIncrementalRecompile_generatorInputResourceChange(JsOutputOption outputOption)
      throws IOException, UnableToCompleteException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule", Lists.newArrayList(
        resourceReadingGeneratorModuleResource, generatorEntryPointResource, fooInterfaceResource,
        nonJsoFooResource), classNameToGenerateResource, modifiedClassNameToGenerateResource, Sets.<
        String> newHashSet("com.foo.TestEntryPoint", "com.foo.HasCustomContent",
        "com.foo.FooReplacementTwo"), outputOption);
  }

  private void assertDeterministicBuild(String topLevelModule, int optimizationLevel)
      throws UnableToCompleteException, IOException {

    final CompilerOptionsImpl options = new CompilerOptionsImpl();
    options.setOptimizationLevel(optimizationLevel);

    File firstCompileWorkDir = Utility.makeTemporaryDirectory(null, "hellowork");
    File secondCompileWorkDir = Utility.makeTemporaryDirectory(null, "hellowork");
    String oldPersistentUnitCacheValue =
        System.setProperty(UnitCacheSingleton.GWT_PERSISTENTUNITCACHE, "false");
    try {
      options.addModuleName(topLevelModule);
      options.setWarDir(new File(firstCompileWorkDir, "war"));
      options.setExtraDir(new File(firstCompileWorkDir, "extra"));
      TreeLogger logger = TreeLogger.NULL;

      // Run the compiler once here.
      new Compiler(options).run(logger);
      Set<String> firstTimeOutput =
          Sets.newHashSet(new File(options.getWarDir() + "/hello").list());

      options.setWarDir(new File(secondCompileWorkDir, "war"));
      options.setExtraDir(new File(secondCompileWorkDir, "extra"));
      // Run the compiler for a second time here.
      new Compiler(options).run(logger);
      Set<String> secondTimeOutput =
          Sets.newHashSet(new File(options.getWarDir() + "/hello").list());

      // It is only necessary to check that the filenames in the output directory are the same
      // because the names of the files for the JavaScript outputs are the hash of its contents.
      assertEquals("First and second compile produced different outputs", firstTimeOutput,
          secondTimeOutput);
    } finally {
      if (oldPersistentUnitCacheValue == null) {
        System.clearProperty(UnitCacheSingleton.GWT_PERSISTENTUNITCACHE);
      } else {
        System.setProperty(UnitCacheSingleton.GWT_PERSISTENTUNITCACHE, oldPersistentUnitCacheValue);
      }
      Util.recursiveDelete(firstCompileWorkDir, false);
      Util.recursiveDelete(secondCompileWorkDir, false);
    }
  }

  private void checkRecompiledModifiedApp(String moduleName, List<MockResource> sharedResources,
      MockResource originalResource, MockResource modifiedResource,
      Set<String> expectedStaleTypeNamesOnModify, JsOutputOption output) throws IOException,
      UnableToCompleteException, InterruptedException {
    checkRecompiledModifiedApp(new CompilerOptionsImpl(), moduleName, sharedResources,
        originalResource, modifiedResource, expectedStaleTypeNamesOnModify, output);
  }

  private void checkRecompiledModifiedApp(CompilerOptions compilerOptions, String moduleName,
      List<MockResource> sharedResources, MockResource originalResource,
      MockResource modifiedResource, Set<String> expectedStaleTypeNamesOnModify,
      JsOutputOption output) throws IOException, UnableToCompleteException, InterruptedException {
    List<MockResource> originalResources = Lists.newArrayList(sharedResources);
    originalResources.add(originalResource);

    List<MockResource> modifiedResources = Lists.newArrayList(sharedResources);
    modifiedResources.add(modifiedResource);

    // Compile the app with original files, modify a file and do a per-file recompile.
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();
    String originalAppFromScratchJs = compileToJs(compilerOptions, relinkApplicationDir, moduleName,
        originalResources, relinkMinimalRebuildCache, emptySet, output);
    String modifiedAppRelinkedJs = compileToJs(compilerOptions, relinkApplicationDir, moduleName,
        Lists.<MockResource> newArrayList(modifiedResource), relinkMinimalRebuildCache,
        expectedStaleTypeNamesOnModify, output);

    // Compile the app from scratch with the modified file.
    MinimalRebuildCache fromScratchMinimalRebuildCache = new MinimalRebuildCache();
    File fromScratchApplicationDir = Files.createTempDir();
    String modifiedAppFromScratchJs = compileToJs(compilerOptions, fromScratchApplicationDir,
        moduleName, modifiedResources, fromScratchMinimalRebuildCache, emptySet, output);

    // If a resource contents were changed between the original compile and the relink compile
    // check that the output JS has also changed. If all resources have the same content (their
    // timestamps might have changed) then outputs should be the same.
    assertEquals(modifiedResource == originalResource,
        originalAppFromScratchJs.equals(modifiedAppRelinkedJs));

    // If per-file compiles properly avoids global-knowledge dependencies and correctly invalidates
    // referencing types when a type changes, then the relinked and from scratch JS will be
    // identical.
    assertTrue(modifiedAppRelinkedJs.equals(modifiedAppFromScratchJs));
  }

  private String compileToJs(File applicationDir, String moduleName,
      List<MockResource> applicationResources, MinimalRebuildCache minimalRebuildCache,
      Set<String> expectedStaleTypeNames, JsOutputOption output) throws IOException,
      UnableToCompleteException, InterruptedException {
    return compileToJs(new CompilerOptionsImpl(), applicationDir, moduleName, applicationResources,
        minimalRebuildCache, expectedStaleTypeNames, output);
  }

  private String compileToJs(CompilerOptions compilerOptions, File applicationDir,
      String moduleName, List<MockResource> applicationResources,
      MinimalRebuildCache minimalRebuildCache, Set<String> expectedProcessedStaleTypeNames,
      JsOutputOption output) throws IOException, UnableToCompleteException, InterruptedException {
    // Make sure we're using a MemoryUnitCache.
    System.setProperty(UnitCacheSingleton.GWT_PERSISTENTUNITCACHE, "false");
    // Wait 1 second so that any new file modification times are actually different.
    Thread.sleep(1001);
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.ERROR);
    // We might be reusing the same application dir but we want to make sure that the output dir is
    // clean to avoid confusion when returning the output JS.
    File outputDir = new File(applicationDir.getPath() + File.separator + moduleName);
    if (outputDir.exists()) {
      Util.recursiveDelete(outputDir, true);
    }

    // Fake out the resource loader to read resources both from the normal classpath as well as this
    // new application directory.
    ResourceLoader resourceLoader = ResourceLoaders.forClassLoader(Thread.currentThread());
    resourceLoader =
        ResourceLoaders.forPathAndFallback(ImmutableList.of(applicationDir), resourceLoader);

    // Setup options to perform a per-file compile, output to this new application directory and
    // compile the given module.
    compilerOptions.setIncrementalCompileEnabled(true);
    compilerOptions.setWarDir(applicationDir);
    compilerOptions.setModuleNames(ImmutableList.of(moduleName));
    compilerOptions.setOutput(output);

    CompilerContext compilerContext = new CompilerContext.Builder().options(compilerOptions)
        .minimalRebuildCache(minimalRebuildCache).build();

    // Write the Java/XML/etc resources that make up the test application.
    for (MockResource applicationResource : applicationResources) {
      writeResourceTo(applicationResource, applicationDir);
    }

    // Cause the module to be cached with a reference to the prefixed resource loader so that the
    // compile process will see those resources.
    ModuleDefLoader.clearModuleCache();
    ModuleDefLoader.loadFromResources(logger, compilerContext, moduleName, resourceLoader, true);

    // Run the compile.
    Compiler compiler = new Compiler(compilerOptions, minimalRebuildCache);
    compiler.run(logger);

    // Find, read and return the created JS.
    File outputJsFile = null;
    outputDir = new File(applicationDir.getPath() + File.separator + moduleName);
    if (outputDir.exists()) {
      for (File outputFile : outputDir.listFiles()) {
        if (outputFile.getPath().endsWith(".cache.js")) {
          outputJsFile = outputFile;
          break;
        }
      }
    }

    assertNotNull(outputJsFile);
    if (expectedProcessedStaleTypeNames != null) {
      assertEquals(expectedProcessedStaleTypeNames,
          minimalRebuildCache.getProcessedStaleTypeNames());
    }
    return Files.toString(outputJsFile, Charsets.UTF_8);
  }

  private String getEntryMethodHolderTypeName(String typeName) {
    return "com.google.gwt.lang." +
        EntryMethodHolderGenerator.getEntryMethodHolderTypeName(typeName);
  }

  private Set<String> stringSet(String... strings) {
    return Sets.newHashSet(strings);
  }

  private void writeResourceTo(MockResource mockResource, File applicationDir) throws IOException {
    File resourceFile =
        new File(applicationDir.getAbsolutePath() + File.separator + mockResource.getPath());
    resourceFile.getParentFile().mkdirs();
    Files.write(mockResource.getContent(), resourceFile, Charsets.UTF_8);
  }
}
