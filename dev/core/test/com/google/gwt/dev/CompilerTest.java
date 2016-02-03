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
import com.google.gwt.dev.jjs.impl.JjsUtils;
import com.google.gwt.dev.util.UnitTestTreeLogger;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Test for {@link Compiler}.
 */
public class CompilerTest extends ArgProcessorTestBase {

  public static final String HELLO_MODULE = "com.google.gwt.sample.hello.Hello";
  public static final String HELLO_MODULE_STACKMODE_STRIP =
      "com.google.gwt.sample.hello.Hello_stackMode_strip";

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
          "public class ReferencesParent {",
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
          "    ReferencesParent referencesParent = new ReferencesParent();",
          "    PackagePrivateChild packagePrivateChild = new PackagePrivateChild();",
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

  private MockJavaResource simpleDialogResourceWithExport =
      JavaResourceBase.createMockJavaResource("com.foo.SimpleDialog",
          "package com.foo;",
          "import jsinterop.annotations.JsProperty;",
          "public class SimpleDialog {",
          "  @JsProperty(namespace = \"ns\")",
          "  public static int show;",
          "}");

  private MockJavaResource complexDialogResourceWithExport =
      JavaResourceBase.createMockJavaResource("com.foo.ComplexDialog",
          "package com.foo;",
          "import jsinterop.annotations.JsProperty;",
          "public class ComplexDialog {",
          "  @JsProperty(namespace = \"ns\")",
          "  public static int show;",
          "}");

  private MockJavaResource complexDialogResourceSansExport =
      JavaResourceBase.createMockJavaResource("com.foo.ComplexDialog",
          "package com.foo;",
          "public class ComplexDialog {",
          "  public static void show() {}",
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

  private MockJavaResource jsTypeBarResource =
      JavaResourceBase.createMockJavaResource(
          "com.foo.Bar",
          "package com.foo;",
          "import jsinterop.annotations.JsType;",
          "@JsType public class Bar {",
          "  void doInstanceBar() {}",
          "  public static void doStaticBaz() {}",
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

  MockJavaResource entryPointResourceForFoo =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "import com.foo.Foo;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() { new Foo(); }",
          "}");

  private MockJavaResource gwtCreateEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "import com.google.gwt.core.client.GWT;",
          "import jsinterop.annotations.JsType;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @JsType public static class MyJsType {}",
          "  @JsType public interface MyJsTypeInterface {}",
          "  public static class MyTypeImplementsJsType implements MyJsTypeInterface {}",
          "  @Override",
          "  public void onModuleLoad() {",
          "    GWT.create(MyJsType.class);",
          "    GWT.create(MyTypeImplementsJsType.class);",
          "  }",
          "}");

  private MockJavaResource dialogEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    SimpleDialog simpleDialog = new SimpleDialog();",
          "    ComplexDialog complexDialog = new ComplexDialog();",
          "  }",
          "}");

  private MockJavaResource brokenGwtCreateEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "import com.google.gwt.core.client.GWT;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public class TestEntryPoint implements EntryPoint {",
          "  public static class MyJso extends JavaScriptObject {",
          "    protected MyJso() {}",
          "  }",
          "  @Override",
          "  public void onModuleLoad() {",
          "    GWT.create(MyJso.class);",
          "  }",
          "}");

  private MockJavaResource topResource =
      JavaResourceBase.createMockJavaResource("com.foo.top",
          "package com.foo;",
          "abstract class Top {",
          "  Object run() {",
          "    return \"\";",
          "  }",
          "}");

  private MockJavaResource middleResource =
      JavaResourceBase.createMockJavaResource("com.foo.Middle",
          "package com.foo;",
          "public abstract class Middle extends Top {",
          "  abstract String run();",
          "}");

  private MockJavaResource bottomResource =
      JavaResourceBase.createMockJavaResource("com.foo.Bottom",
          "package com.foo;",
          "public class Bottom extends Middle {",
          "  // ",
          "  private final class Value {}",
          "  String run() {",
          "    Value value = new Value();",
          "    return \"\";",
          "  }",
          "}");

  private MockJavaResource overriddenMethodChainEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    Bottom bottom = new Bottom();",
          "    ((Top)bottom).run();",
          "  }",
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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    FooResourceGenerator.runCount = 0;
    BarReferencesFooGenerator.runCount = 0;
    CauseStringRebindGenerator.runCount = 0;
    CauseShortRebindGenerator.runCount = 0;
  }

  public void testAllValidArgs() {
    CompilerOptionsImpl options = new CompilerOptionsImpl();
    Compiler.ArgProcessor argProcessor = new Compiler.ArgProcessor(options);

    assertProcessSuccess(argProcessor, new String[] {"-logLevel", "DEBUG", "-style",
        "PRETTY", "-ea", "-gen", "myGen",
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
    assertTrue(options.shouldClusterSimilarFunctions());
    assertTrue(options.shouldInlineLiteralParameters());
    assertTrue(options.shouldOptimizeDataflow());
    assertTrue(options.shouldOrdinalizeEnums());
    assertTrue(options.shouldRemoveDuplicateFunctions());
    assertTrue(options.isIncrementalCompileEnabled());

    assertEquals(SourceLevel.JAVA7, options.getSourceLevel());

    assertEquals(2, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
    assertEquals("my.Module", options.getModuleNames().get(1));
  }

  public void testDefaultArgs() {
    CompilerOptionsImpl options = new CompilerOptionsImpl();
    Compiler.ArgProcessor argProcessor = new Compiler.ArgProcessor(options);

    assertProcessSuccess(argProcessor, new String[]{"c.g.g.h.H"});

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
    CompilerOptionsImpl options = new CompilerOptionsImpl();
    Compiler.ArgProcessor argProcessor = new Compiler.ArgProcessor(options);

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
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.4"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.5"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.6"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.6_26"));

    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.7"));
    assertEquals(SourceLevel.JAVA8, SourceLevel.getBestMatchingVersion("1.8"));
    assertEquals(SourceLevel.JAVA8, SourceLevel.getBestMatchingVersion("1.9"));

    // not proper version strings => default to JAVA7.
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.6u3"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.6b3"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.7b3"));
  }

  /**
   * Verify that a compile with a @JsType at least compiles successfully.
   */
  public void testGwtCreateJsTypeRebindResult() throws Exception {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compileToJs(compilerOptions, Files.createTempDir(), "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, gwtCreateEntryPointResource),
        new MinimalRebuildCache(), emptySet, JsOutputOption.OBFUSCATED);
  }

   /**
   * Test that some lightly referenced interface through a @JsFunction is included in the output.
   */
  public void testReferenceThroughJsFunction() throws Exception {
    MockJavaResource someJsFunction =
        JavaResourceBase.createMockJavaResource(
            "com.foo.SomeJsFunction",
            "package com.foo;",
            "import jsinterop.annotations.JsFunction;",
            "@JsFunction",
            "public interface SomeJsFunction {",
            "  void m();",
            "}");

    MockJavaResource jsFunctionInterfaceImplementation =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Impl",
            "package com.foo;",
            "public class Impl implements SomeJsFunction {",
            "  public void m() { SomeInterface.class.getName(); } ",
            "}");

    MockJavaResource someInterface =
        JavaResourceBase.createMockJavaResource(
            "com.foo.SomeInterface",
            "package com.foo;",
            "public interface SomeInterface {",
            "}");

    MockJavaResource testEntryPoint =
        JavaResourceBase.createMockJavaResource(
            "com.foo.TestEntryPoint",
            "package com.foo;",
            "import com.google.gwt.core.client.EntryPoint;",
            "public class TestEntryPoint implements EntryPoint {",
            "  private static native void f(SomeJsFunction f) /*-{}-*/;",
            "  public void onModuleLoad() {",
                // Create Impl and pass it to JS but do not explicitly call m
            "    f(new Impl());",
            "  }",
            "}");

    MockResource moduleResource =
        JavaResourceBase.createMockResource(
            "com/foo/TestEntryPoint.gwt.xml",
            "<module>",
            "  <source path=''/>",
            "  <entry-point class='com.foo.TestEntryPoint'/>",
            "</module>");

    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    String js = compileToJs(compilerOptions, Files.createTempDir(), testEntryPoint.getTypeName(),
        Lists.newArrayList(moduleResource, testEntryPoint, someJsFunction,
            jsFunctionInterfaceImplementation, someInterface),
        new MinimalRebuildCache(), emptySet, JsOutputOption.DETAILED);
    // Make sure the referenced class literals ends up being included in the resulting JS.
    String classliteralHolderVarName =
        JjsUtils.mangleMemberName("com.google.gwt.lang.ClassLiteralHolder",
            JjsUtils.classLiteralFieldNameFromJavahTypeSignatureName(
                JjsUtils.javahSignatureFromName(someInterface.getTypeName())));
    assertTrue(js.contains("var " + classliteralHolderVarName + " = "));
  }

  /**
   * Tests that changing Js namespace name on an exported method comes out accurately.
   *
   * <p>An unrelated and non-updated @JsType is also included in each compile to verify that updated
   * exports do not forget non-edited items in a recompile.
   */
  public void testChangeJsNamespaceOnMethod() throws Exception {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    MockJavaResource jsNamespaceFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsType;",
            "@JsType public class Foo {",
            "  @JsMethod(namespace=\"spazz\") public static void doStaticBar() {}",
            "}");

    MockJavaResource regularFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "import jsinterop.annotations.JsType;",
            "@JsType public class Foo {",
            "  public static void doStaticBar() {}",
            "}");

    checkRecompiledModifiedApp(
        compilerOptions,
        "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, emptyEntryPointResource, jsTypeBarResource),
        regularFooResource,
        jsNamespaceFooResource,
        stringSet("com.foo.Bar", "com.foo.Foo"),
        JsOutputOption.DETAILED);
  }

  /**
   * Tests that changing Js namespace on a class comes out accurately.
   *
   * <p>An unrelated and non-updated @JsType is also included in each compile to verify that updated
   * exports do not forget non-edited items in a recompile.
   */
  public void testChangeJsNamespaceOnClass() throws Exception {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    MockJavaResource jsNamespaceFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "import jsinterop.annotations.JsType;",
            "@JsType(namespace=\"spazz\") public class Foo {",
            "  public static void doStaticBar() {}",
            "}");

    MockJavaResource regularFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "import jsinterop.annotations.JsType;",
            "@JsType public class Foo {",
            "  public static void doStaticBar() {}",
            "}");

    checkRecompiledModifiedApp(
        compilerOptions,
        "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, emptyEntryPointResource, jsTypeBarResource),
        regularFooResource,
        jsNamespaceFooResource,
        stringSet("com.foo.Bar", "com.foo.Foo"),
        JsOutputOption.DETAILED);
  }

  /**
   * Tests that changing @JsFunction name on an interface comes out accurately.
   *
   * <p>An unrelated and non-updated @JsType is also included in each compile to verify that updated
   * exports do not forget non-edited items in a recompile.
   */
  public void testChangeJsFunction() throws Exception {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    MockJavaResource jsFunctionIFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.IFoo",
            "package com.foo;",
            "import jsinterop.annotations.JsFunction;",
            "@JsFunction public interface IFoo {",
            "  int foo(int x);",
            "}");

    MockJavaResource regularIFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.IFoo",
            "package com.foo;",
            "public interface IFoo {",
            "  int foo(int x);",
            "}");

    MockJavaResource fooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "public class Foo implements IFoo {",
            "  @Override public int foo(int x) { return 0; }",
            "}");

    checkRecompiledModifiedApp(
        compilerOptions,
        "com.foo.SimpleModule",
        Lists.newArrayList(
            simpleModuleResource, entryPointResourceForFoo, fooResource, jsTypeBarResource),
        regularIFooResource,
        jsFunctionIFooResource,
        stringSet("com.foo.Bar", "com.foo.Foo", "com.foo.IFoo", "com.foo.TestEntryPoint"),
        JsOutputOption.DETAILED);
  }

  /**
   * Tests that toggling JsProperty methods in an interface comes out accurately.
   *
   * <p>An unrelated and non-updated @JsType is also included in each compile to verify that updated
   * exports do not forget non-edited items in a recompile.
   */
  public void testChangeJsProperty() throws Exception {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    MockJavaResource jsPropertyIFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.IFoo",
            "package com.foo;",
            "import jsinterop.annotations.JsProperty;",
            "import jsinterop.annotations.JsType;",
            "@JsType public interface IFoo {",
            "  @JsProperty int getX();",
            "  @JsProperty int getY();",
            "}");

    MockJavaResource regularIFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.IFoo",
            "package com.foo;",
            "import jsinterop.annotations.JsType;",
            "@JsType public interface IFoo {",
            "  int getX();",
            "  int getY();",
            "}");

    MockJavaResource fooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "public class Foo implements IFoo {",
            "  @Override public int getX() { return 0; }",
            "  @Override public int getY() { return 0; }",
            "}");

    checkRecompiledModifiedApp(
        compilerOptions,
        "com.foo.SimpleModule",
        Lists.newArrayList(
            simpleModuleResource, entryPointResourceForFoo, fooResource, jsTypeBarResource),
        regularIFooResource,
        jsPropertyIFooResource,
        stringSet("com.foo.Bar", "com.foo.Foo", "com.foo.IFoo", "com.foo.TestEntryPoint"),
        JsOutputOption.DETAILED);
  }

  /**
   * Tests that adding a @JsType annotation on a class comes out accurately and that removing it
   * comes out accurately as well.
   *
   * <p>An unrelated and non-updated @JsType is also included in each compile to verify that updated
   * exports do not forget non-edited items in a recompile.
   */
  public void testChangeJsType() throws Exception {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    MockJavaResource jsTypeFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "import jsinterop.annotations.JsType;",
            "@JsType public class Foo {",
            "  void doInstanceBar() {}",
            "  public static void doStaticBar() {}",
            "}");

    MockJavaResource regularFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo", "package com.foo;", "public class Foo {}");

    checkRecompiledModifiedApp(
        compilerOptions,
        "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, emptyEntryPointResource, jsTypeBarResource),
        regularFooResource,
        jsTypeFooResource,
        stringSet("com.foo.Bar", "com.foo.Foo"),
        JsOutputOption.DETAILED);
  }

  /**
   * Tests that changing a prototype on a @JsType annotated class comes out accurately.
   *
   * <p>An unrelated and non-updated @JsType is also included in each compile to verify that updated
   * exports do not forget non-edited items in a recompile.
   */
  public void testChangeJsTypeNative() throws Exception {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    MockJavaResource nativeFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) public class Foo {",
            "  public static native void doStaticBar();",
            "}");

    MockJavaResource regularFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "import jsinterop.annotations.JsType;",
            "@JsType public class Foo {",
            "  public static void doStaticBar() {}",
            "}");

    checkRecompiledModifiedApp(
        compilerOptions,
        "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, emptyEntryPointResource, jsTypeBarResource),
        regularFooResource,
        nativeFooResource,
        stringSet("com.foo.Bar", "com.foo.Foo"),
        JsOutputOption.DETAILED);
  }

  /**
   * Tests that adding a @JsIgnore annotation on a method comes out accurately and that removing
   * it comes out accurately as well.
   *
   * <p>An unrelated and non-updated @JsType is also included in each compile to verify that updated
   * exports do not forget non-edited items in a recompile.
   */
  public void testChangeJsIgnore() throws Exception {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    MockJavaResource jsIgnoreFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "import jsinterop.annotations.JsIgnore;",
            "import jsinterop.annotations.JsType;",
            "@JsType public class Foo {",
            "  @JsIgnore public static void doStaticBar() {}",
            "}");

    MockJavaResource regularFooResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.Foo",
            "package com.foo;",
            "import jsinterop.annotations.JsType;",
            "@JsType public class Foo {",
            "  public static void doStaticBar() {}",
            "}");

    checkRecompiledModifiedApp(
        compilerOptions,
        "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, emptyEntryPointResource, jsTypeBarResource),
        regularFooResource,
        jsIgnoreFooResource,
        stringSet("com.foo.Bar", "com.foo.Foo"),
        JsOutputOption.DETAILED);
  }

  public void testJsInteropNameCollision() throws Exception {
    MinimalRebuildCache minimalRebuildCache = new MinimalRebuildCache();
    File applicationDir = Files.createTempDir();
    CompilerOptions compilerOptions = new CompilerOptionsImpl();

    // Simple compile with one dialog.alert() export succeeds.
    compileToJs(compilerOptions, applicationDir, "com.foo.SimpleModule", Lists.newArrayList(
        simpleModuleResource, dialogEntryPointResource, simpleDialogResourceWithExport,
        complexDialogResourceSansExport), minimalRebuildCache, emptySet, JsOutputOption.OBFUSCATED);

    try {
      // Exporting a second dialog.alert() fails with an exported name collision.
      compileToJs(compilerOptions, applicationDir, "com.foo.SimpleModule",
          Lists.<MockResource> newArrayList(complexDialogResourceWithExport), minimalRebuildCache,
          emptySet, JsOutputOption.OBFUSCATED);
      fail("Compile should have failed");
    } catch (UnableToCompleteException e) {
      // success
    }

    // Reverting to just a single dialog.alert() starts succeeding again.
    compileToJs(compilerOptions, applicationDir, "com.foo.SimpleModule",
        Lists.<MockResource>newArrayList(complexDialogResourceSansExport), minimalRebuildCache,
        stringSet("com.foo.SimpleDialog", "com.foo.ComplexDialog", "com.foo.TestEntryPoint"),
        JsOutputOption.OBFUSCATED);
  }

  public void testGwtCreateJsoRebindResult() throws Exception {
    try {
      compileToJs(Files.createTempDir(), "com.foo.SimpleModule",
          Lists.newArrayList(simpleModuleResource, brokenGwtCreateEntryPointResource),
          new MinimalRebuildCache(), emptySet, JsOutputOption.OBFUSCATED);
      fail("Compile should have failed");
    } catch (UnableToCompleteException e) {
      // success
    }
  }

  public void testNonZeroArgConstructorEntryPoint() throws Exception {
    MockResource moduleResource =
        JavaResourceBase.createMockResource(
            "com/foo/NonZeroArgConstructor.gwt.xml",
            "<module>",
            "  <source path=''/>",
            "  <entry-point class='com.foo.NonZeroArgConstructorEntryPoint'/>",
            "</module>");

    MockJavaResource entryPointResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.NonZeroArgConstructorEntryPoint",
            "package com.foo;",
            "import com.google.gwt.core.client.EntryPoint;",
            "public class NonZeroArgConstructorEntryPoint implements EntryPoint {",
            "  public NonZeroArgConstructorEntryPoint(String s) {",
            "  }",
            "  public void onModuleLoad() {",
            "  }",
            "}");

    MinimalRebuildCache minimalRebuildCache = new MinimalRebuildCache();
    File applicationDir = Files.createTempDir();
    CompilerOptions compilerOptions = new CompilerOptionsImpl();

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    builder.expectError(Pattern.compile("Errors in .*"), null);
    builder.expectError("Line 3: Rebind result 'com.foo.NonZeroArgConstructorEntryPoint' "
        + "has no default (zero argument) constructors", null);
    UnitTestTreeLogger errorLogger = builder.createLogger();
    try {
      // Simple compile with one dialog.alert() export succeeds.

      compileToJs(errorLogger, compilerOptions, applicationDir, "com.foo.NonZeroArgConstructor",
          Lists.newArrayList(moduleResource, entryPointResource), minimalRebuildCache,
          emptySet, JsOutputOption.OBFUSCATED);
      fail("Compile should have failed");
    } catch (UnableToCompleteException expected) {
      errorLogger.assertCorrectLogEntries();
    }
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

  public void testSuccessfulCompile_jsoClassLiteralOrder() throws Exception {
    // Crafted resource to make sure the a native subclass is compiled before the JSO class,
    // In the case of native sublcasses the class hierarchy does not match the class literal
    // hierarchy.
    MockJavaResource nativeClassAndSubclass =
        JavaResourceBase.createMockJavaResource(
            "com.foo.MyNativeSubclass",
            "package com.foo;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true)",
            "class NativeClass {",
            "}",
            "public class MyNativeSubclass extends NativeClass {",
            "}");

    MockJavaResource testEntryPoint =
        JavaResourceBase.createMockJavaResource(
            "com.foo.MyEntryPoint",
            "package com.foo;",
            "import com.foo.MyNativeSubclass;",
            "public class MyEntryPoint extends MyNativeSubclass {",
            "  public void onModuleLoad() {",
            "    Object o = new Object();",
            "    if (MyNativeSubclass.class.getName() == null) ",
            "      o = new MyNativeSubclass();",
            // Make .clazz reachable so that class literals are emmitted with the respective
            // classses.
            "    o.getClass();",
            "  }",
            "}");

    MockResource moduleResource =
        JavaResourceBase.createMockResource(
            "com/foo/MyEntryPoint.gwt.xml",
            "<module>",
            "  <source path=''/>",
            "  <entry-point class='com.foo.MyEntryPoint'/>",
            "</module>");

    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    // Make sure it compiles successfully with no assertions
    compilerOptions.setEnableAssertions(true);
    compilerOptions.setGenerateJsInteropExports(true);
    compilerOptions.setOutput(JsOutputOption.PRETTY);
    compilerOptions.setOptimizationLevel(9);
    assertCompileSucceeds(compilerOptions, testEntryPoint.getTypeName(),
        Lists.newArrayList(moduleResource, nativeClassAndSubclass, testEntryPoint));
  }

  // TODO(stalcup): add recompile tests for file deletion.

  public void testIncrementalRecompile_noop() throws UnableToCompleteException, IOException,
      InterruptedException {
    checkIncrementalRecompile_noop(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_noop(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_dateStampChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_dateStampChange(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_dateStampChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_invalidatePreamble() throws UnableToCompleteException,
      IOException, InterruptedException {
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();

    // Perform a first compile.
    compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, emptyEntryPointResource),
        relinkMinimalRebuildCache, null, JsOutputOption.OBFUSCATED);
    // On first compile nothing is explicitly stale, only implicitly stale.
    assertEquals(0, relinkMinimalRebuildCache.getStaleTypeNames().size());

    // Recompile with a deep change that invalidates the preamble.
    relinkMinimalRebuildCache.markSourceFileStale("java/lang/Object.java");
    compileToJs(relinkApplicationDir, "com.foo.SimpleModule", Lists.<MockResource> newArrayList(),
        relinkMinimalRebuildCache, null, JsOutputOption.OBFUSCATED);
    // Show that preamble invalidation marks everything stale.
    assertTrue(relinkMinimalRebuildCache.getProcessedStaleTypeNames().size() > 100);

    // Recompile again with a tiny change. Prove that it's not stuck repeatedly invalidating the
    // whole world.
    compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource> newArrayList(emptyEntryPointResource), relinkMinimalRebuildCache, null,
        JsOutputOption.OBFUSCATED);
    // Since J2CL requires a @JsMethod in Number.java, String.java, etc. and since JsInterop types
    // are fully traversed (so that correct exports can be regenerated) Number is fully traversed
    // and so shows up in the processed list.
    Set<String> staleTypeNames =
        new HashSet<>(relinkMinimalRebuildCache.getProcessedStaleTypeNames());
    staleTypeNames.remove("java.lang.Boolean");
    staleTypeNames.remove("java.lang.Double");
    staleTypeNames.remove("java.lang.Number");
    staleTypeNames.remove("java.lang.String");
    // Show that only this little change is stale, not the whole world.
    assertEquals(2, staleTypeNames.size());
  }

  public void testIncrementalRecompile_bridgeMethodOverrideChain()
      throws UnableToCompleteException, IOException, InterruptedException {
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();

    // Perform a first compile.
    compileToJs(relinkApplicationDir, "com.foo.SimpleModule", Lists.newArrayList(
        simpleModuleResource, overriddenMethodChainEntryPointResource, topResource, middleResource,
        bottomResource), relinkMinimalRebuildCache, null, JsOutputOption.OBFUSCATED);
    // On first compile nothing is explicitly stale, only implicitly stale.
    assertEquals(0, relinkMinimalRebuildCache.getStaleTypeNames().size());

    // Recompile with a change to Bottom.
    relinkMinimalRebuildCache.markSourceFileStale("com/foo/Bottom.java");
    compileToJs(relinkApplicationDir, "com.foo.SimpleModule", Lists.<MockResource> newArrayList(),
        relinkMinimalRebuildCache, null, JsOutputOption.OBFUSCATED);
    // Show that the third level bridge method override of Top.run() is seen to be live and thus
    // makes type com.foo.Bottom$Value live.
    assertTrue(
        relinkMinimalRebuildCache.getProcessedStaleTypeNames().contains("com.foo.Bottom$Value"));
  }

  public void testIncrementalRecompile_classLiteralNewReference()
      throws UnableToCompleteException, IOException, InterruptedException {
    checkIncrementalRecompile_classLiteralNewReference(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_classLiteralNewReference(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_primitiveClassLiteralReference()
      throws UnableToCompleteException, IOException, InterruptedException {
    checkIncrementalRecompile_primitiveClassLiteralReference(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_primitiveClassLiteralReference(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_superClassOrder()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Linked output is sorted alphabetically except that super-classes come before sub-classes. If
    // on recompile a sub-class -> super-class relationship is lost then a sub-class with an
    // alphabetically earlier name might start linking out before the super-class.
    checkIncrementalRecompile_superClassOrder(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_superClassOrder(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_superFromStaleInner()
      throws UnableToCompleteException, IOException, InterruptedException {
    checkIncrementalRecompile_superFromStaleInner(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_superFromStaleInner(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_deterministicUiBinder() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_deterministicUiBinder(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_deterministicUiBinder(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_uiBinderCssChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_uiBinderCssChange(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_uiBinderCssChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_unstableGeneratorReferencesModifiedType()
      throws UnableToCompleteException, IOException, InterruptedException {
    checkIncrementalRecompile_unstableGeneratorReferencesModifiedType(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_unstableGeneratorReferencesModifiedType(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_functionSignatureChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty/Obfuscated output since the JsIncrementalNamer's
    // behavior is order dependent, and while still correct, will come out different in a recompile
    // with this change versus a from scratch compile with this change.
    checkIncrementalRecompile_functionSignatureChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_compileTimeConstantChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_compileTimeConstantChange(JsOutputOption.DETAILED);
    checkIncrementalRecompile_compileTimeConstantChange(JsOutputOption.OBFUSCATED);
  }

  public void testIncrementalRecompile_transitivelyFoldableConstant()
      throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_transitivelyFoldableConstant(JsOutputOption.DETAILED);
    checkIncrementalRecompile_transitivelyFoldableConstant(JsOutputOption.OBFUSCATED);
  }

  public void testIncrementalRecompile_packagePrivateDispatch() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkIncrementalRecompile_packagePrivateOverride(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_packagePrivateOverride(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_prettyOutput()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Nominal tests for pretty output. Pretty and Obfuscated output share most of the same code
    // paths.
    checkIncrementalRecompile_typeHierarchyChange(JsOutputOption.PRETTY);
    checkIncrementalRecompile_unreachableIncompatibleChange(JsOutputOption.PRETTY);
  }

  public void testIncrementalRecompile_regularClassMadeIntoJsoClass()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Not testing recompile equality with Pretty/Obfuscated output since the JsIncrementalNamer's
    // behavior is order dependent, and while still correct, will come out different in a recompile
    // with this change versus a from scratch compile with this change.
    checkIncrementalRecompile_regularClassMadeIntoJsoClass(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_unreachableIncompatibleChange()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Not testing recompile equality with Pretty/Obfuscated output since the JsIncrementalNamer's
    // behavior is order dependent, and while still correct, will come out different in a recompile
    // with this change versus a from scratch compile with this change.
    checkIncrementalRecompile_unreachableIncompatibleChange(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_unreachableIncompatibleChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_typeHierarchyChange()
      throws UnableToCompleteException, IOException, InterruptedException {
    checkIncrementalRecompile_typeHierarchyChange(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_typeHierarchyChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_defaultMethod()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Tests that default method on superclasses are correctly constructed
    checkIncrementalRecompile_defaultMethod(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_defaultMethod(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_devirtualizeUnchangedJso()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Tests that a JSO calls through interfaces are correctly devirtualized when compiling per file
    // and the JSOs nor their single impl interfaces are not stale.
    checkIncrementalRecompile_devirtualizeUnchangedJso(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_devirtualizeUnchangedJso(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_devirtualizeString()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Tests that String calls through interfaces are correctly devirtualized when compiling per
    // file and neither String nor CharSequence interface are stale.
    checkIncrementalRecompile_devirtualizeString(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_devirtualizeString(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_devirtualizeComparable()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Tests that Doublecalls through interfaces are correctly devirtualized when compiling per
    // file and neither String nor CharSequence interface are stale.
    checkIncrementalRecompile_devirtualizeComparable(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_devirtualizeComparable(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_multipleClassGenerator()
      throws UnableToCompleteException, IOException, InterruptedException {
    // Tests that a Generated type that is not directly referenced from the rebound GWT.create()
    // call is still marked stale, regenerated, retraversed and output as JS.
    checkIncrementalRecompile_multipleClassGenerator(JsOutputOption.OBFUSCATED);
    checkIncrementalRecompile_multipleClassGenerator(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_singleJsoIntfDispatchChange()
      throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty/Obfuscated output since the JsIncrementalNamer's
    // behavior is order dependent, and while still correct, will come out different in a recompile
    // with this change versus a from scratch compile with this change.
    checkIncrementalRecompile_singleJsoIntfDispatchChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_dualJsoIntfDispatchChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty/Obfuscated output since the JsIncrementalNamer's
    // behavior is order dependent, and while still correct, will come out different in a recompile
    // with this change versus a from scratch compile with this change.
    checkIncrementalRecompile_dualJsoIntfDispatchChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_generatorInputResourceChange() throws IOException,
      UnableToCompleteException, InterruptedException {
    // Not testing recompile equality with Pretty/Obfuscated output since the JsIncrementalNamer's
    // behavior is order dependent, and while still correct, will come out different in a recompile
    // with this change versus a from scratch compile with this change.
    checkIncrementalRecompile_generatorInputResourceChange(JsOutputOption.DETAILED);
  }

  public void testIncrementalRecompile_invalidatedGeneratorOutputRerunsGenerator()
      throws UnableToCompleteException, IOException, InterruptedException {
    // BarReferencesFoo Generator hasn't run yet.
    assertEquals(0, BarReferencesFooGenerator.runCount);

    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    List<MockResource> sharedResources =
        Lists.newArrayList(barReferencesFooGeneratorModuleResource, generatorEntryPointResource);
    JsOutputOption output = JsOutputOption.OBFUSCATED;

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
    JsOutputOption output = JsOutputOption.OBFUSCATED;

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
        Lists.<MockResource>newArrayList(), relinkMinimalRebuildCache, emptySet, output);

    // Since there were no changes Generators were not run again.
    assertEquals(1, CauseStringRebindGenerator.runCount);
    assertEquals(1, CauseShortRebindGenerator.runCount);
    assertEquals(1, FooResourceGenerator.runCount);

    // Recompile with a modified resource, which should invalidate the output of the
    // FooResourceGenerator and cascade the invalidate the Generators that triggered
    // FooResourceGenerator.
    compileToJs(compilerOptions, relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource>newArrayList(modifiedClassNameToGenerateResource),
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
    JsOutputOption output = JsOutputOption.OBFUSCATED;

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
    JsOutputOption output = JsOutputOption.OBFUSCATED;

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
        Lists.<MockResource>newArrayList(), relinkMinimalRebuildCache, emptySet, output);

    assertTrue(originalJs.equals(relinkedJs));
  }

  private void checkIncrementalRecompile_defaultMethod(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    // Tests that when a superclass has a "inherits" a default method,
    MockJavaResource interfaceWithDefaultMethod =
        JavaResourceBase.createMockJavaResource(
            "com.foo.InterfaceWithDefaultMethod",
            "package com.foo;",
            "interface InterfaceWithDefaultMethod {",
            "  default String m() {return null; }",
            "}");

    MockJavaResource classImplementingInterfaceWithDefaultMethod =
        JavaResourceBase.createMockJavaResource(
            "com.foo.classImplementingInterfaceWithDefaultMethod",
            "package com.foo;",
            "public class classImplementingInterfaceWithDefaultMethod",
            "    implements InterfaceWithDefaultMethod {",
            "}");

    MockJavaResource aSubclass =
        JavaResourceBase.createMockJavaResource(
            "com.foo.ASubclass",
            "package com.foo;",
            "public class ASubclass extends classImplementingInterfaceWithDefaultMethod {",
            "  public String someMethod() {return super.m(); }",
            "}");

    MockResource moduleResource =
        JavaResourceBase.createMockResource(
            "com/foo/DefaultMethod.gwt.xml",
            "<module>",
            "  <source path=''/>",
            "  <entry-point class='com.foo.DefaultMethodEntryPoint'/>",
            "</module>");

    MockJavaResource entryPointResource =
        JavaResourceBase.createMockJavaResource(
            "com.foo.DefaultMethodEntryPoint",
            "package com.foo;",
            "import com.google.gwt.core.client.EntryPoint;",
            "public class DefaultMethodEntryPoint implements EntryPoint {",
            "  public void onModuleLoad() {",
            "    new ASubclass().someMethod();",
            "  }",
            "}");

    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);
    compilerOptions.setSourceLevel(SourceLevel.JAVA8);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.DefaultMethod",
        Lists.newArrayList(moduleResource, entryPointResource, aSubclass,
            classImplementingInterfaceWithDefaultMethod, interfaceWithDefaultMethod),
        aSubclass, aSubclass, stringSet("com.foo.ASubclass", "com.foo.DefaultMethodEntryPoint"),
        output);
  }

  public void checkIncrementalRecompile_classLiteralNewReference(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    MockJavaResource interfaceA =
        JavaResourceBase.createMockJavaResource(
            "com.foo.A",
            "package com.foo;",
            "interface A {",
            " static String b = \"\";",
            "}");

    MockJavaResource classBSansLiteralReference =
        JavaResourceBase.createMockJavaResource(
            "com.foo.B",
            "package com.foo;",
            "public class B {",
            "}");

    MockJavaResource classBWithLiteralReference =
        JavaResourceBase.createMockJavaResource(
            "com.foo.B",
            "package com.foo;",
            "public class B {",
            "  public B() { Class c = A.class; }",
            "}");

    MockJavaResource classC =
        JavaResourceBase.createMockJavaResource(
            "com.foo.C",
            "package com.foo;",
            "import com.google.gwt.core.client.EntryPoint;",
            "public class C implements EntryPoint {",
            "  public void onModuleLoad() {",
            "    if (A.b == null) ",
            "      new B();",
            "  }",
            "}");

    MockResource moduleResource =
        JavaResourceBase.createMockResource(
            "com/foo/ClassLiteralReference.gwt.xml",
            "<module>",
            "  <source path=''/>",
            "  <entry-point class='com.foo.C'/>",
            "</module>");

    checkRecompiledModifiedApp("com.foo.ClassLiteralReference", Lists.newArrayList(
            moduleResource, interfaceA, classC),
        classBSansLiteralReference, classBWithLiteralReference,
        stringSet("com.foo.B", "com.foo.C"), output);
  }

  public void checkIncrementalRecompile_primitiveClassLiteralReference(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    MockJavaResource classA =
        JavaResourceBase.createMockJavaResource(
            "com.foo.A",
            "package com.foo;",
            "public class A {",
            "  public A() { ",
            "    Class c = void.class; ",
            "    c = int.class; ",
            "    c = boolean.class;",
            "    c = short.class;",
            "    c = byte.class;",
            "    c = long.class;",
            "    c = float.class;",
            "    c = double.class;",
            "  }",
            "}");

    MockJavaResource classB =
        JavaResourceBase.createMockJavaResource(
            "com.foo.B",
            "package com.foo;",
            "public class B {",
            "  public B() {  }",
            "  public void m () { new A(); }",
            "}");

    MockJavaResource classC =
        JavaResourceBase.createMockJavaResource(
            "com.foo.C",
            "package com.foo;",
            "import com.google.gwt.core.client.EntryPoint;",
            "public class C implements EntryPoint {",
            "  public void onModuleLoad() {",
            "      new B().m();",
            "  }",
            "}");

    MockResource moduleResource =
        JavaResourceBase.createMockResource(
            "com/foo/PrimitiveClassLiteralReference.gwt.xml",
            "<module>",
            "  <source path=''/>",
            "  <entry-point class='com.foo.C'/>",
            "</module>");

    checkRecompiledModifiedApp("com.foo.PrimitiveClassLiteralReference", Lists.newArrayList(
            moduleResource, classA, classB),
        classC, classC,
        stringSet(getEntryMethodHolderTypeName("com.foo.PrimitiveClassLiteralReference"),
            "com.foo.C"), output);
  }

  private void checkIncrementalRecompile_dateStampChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();

    String originalJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, simpleModelEntryPointResource, simpleModelResource,
            constantsModelResource),
        relinkMinimalRebuildCache, emptySet, output);

    // Compile again with the same source but a new date stamp on SimpleModel and reusing the
    // minimalRebuildCache.
    String relinkedJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource>newArrayList(simpleModelResource), relinkMinimalRebuildCache,
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
        stringSet(interfaceOneResource.getTypeName(), "com.foo.SuperFromStaleInnerEntryPoint$B",
            "com.foo.SuperFromStaleInnerEntryPoint$B$1"),
        output);
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
    String binderImpl = "com.foo.MyWidget_BinderImpl";
    checkRecompiledModifiedApp("com.foo.UiBinderTestModule",
        Lists.newArrayList(uiBinderTestModuleResource, uiBinderTestEntryPointResource, myWidget),
        myWidgetWithWhiteStyleUiXml, myWidgetWithGreyStyleUiXml,
        stringSet("com.foo.MyWidget",
            binderImpl, binderImpl + "_GenBundle_default_InlineClientBundleGenerator",
            binderImpl + "_GenBundle_default_InlineClientBundleGenerator$1",
            binderImpl + "_GenBundle_default_InlineClientBundleGenerator$styleInitializer",
            binderImpl + "_TemplateImpl",
            binderImpl + "$Widgets", binderImpl + "$Template"), output);
  }

  private void checkIncrementalRecompile_packagePrivateOverride(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule", Lists.newArrayList(
            simpleModuleResource, packagePrivateDispatchEntryPointResource,
            packagePrivateParentResource, referencesParentResource),
        packagePrivateChildResource, packagePrivateChildResource,
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
    checkRecompiledModifiedApp("com.foo.SimpleModule", Lists.newArrayList(
            simpleModuleResource, simpleModelEntryPointResource, constantsModelResource),
        simpleModelResource, modifiedFunctionSignatureSimpleModelResource,
        stringSet("com.foo.TestEntryPoint", "com.foo.SimpleModel"), output);
  }

  private void checkIncrementalRecompile_compileTimeConstantChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    checkRecompiledModifiedApp("com.foo.SimpleModule", Lists.newArrayList(
            simpleModuleResource, simpleModelEntryPointResource, simpleModelResource),
        constantsModelResource, modifiedConstantsModelResource,
        stringSet("com.foo.SimpleModel", "com.foo.Constants"), output);
  }

  private void checkIncrementalRecompile_transitivelyFoldableConstant(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    // Tests that constants that are provided by types are only referenced by reference only types
    // (hence not traversed) are still available for constant propagation.
    checkRecompiledModifiedApp("com.foo.TransitivelyFoldableConstantModule", Lists.newArrayList(
            transitivelyFoldableConstantModuleResource, classOneResource, classTwoResource),
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

  private void checkIncrementalRecompile_devirtualizeComparable(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {

    final MockResource devirtualizeComparableModuleResource =
        JavaResourceBase.createMockResource("com/foo/DevirtualizeComparableModule.gwt.xml",
            "<module>",
            "<source path=''/>",
            "<entry-point class='com.foo.DevirtualizeComparableEntryPoint'/>",
            "</module>");

    final MockJavaResource devirtualizeComparableEntryPointResource =
        JavaResourceBase.createMockJavaResource("com.foo.DevirtualizeComparableEntryPoint",
            "package com.foo;",
            "import com.google.gwt.core.client.EntryPoint;",
            "public class DevirtualizeComparableEntryPoint implements EntryPoint {",
            "  @Override",
            "  public void onModuleLoad() {",
            "    Comparable c = (Double) 0.1;",
            "    c.compareTo(c);",
            "  }",
            "}");

    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.DevirtualizeComparableModule",
        Lists.newArrayList(devirtualizeComparableModuleResource),
        devirtualizeComparableEntryPointResource, devirtualizeComparableEntryPointResource,
        stringSet("com.foo.DevirtualizeComparableEntryPoint",
            getEntryMethodHolderTypeName("com.foo.DevirtualizeComparableModule")),
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
        nonJsoFooResource), classNameToGenerateResource, modifiedClassNameToGenerateResource,
        stringSet("com.foo.TestEntryPoint", "com.foo.HasCustomContent",
            "com.foo.FooReplacementTwo"), outputOption);
  }

  private void assertCompileSucceeds(CompilerOptions options, String moduleName,
      List<MockResource> applicationResources) throws Exception {
    File compileWorkDir = Utility.makeTemporaryDirectory(null, moduleName);
    final CompilerOptionsImpl compilerOptions = new CompilerOptionsImpl(options);
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.ERROR);

    String oldPersistentUnitCacheValue =
        System.setProperty(UnitCacheSingleton.GWT_PERSISTENTUNITCACHE, "false");
    try {

      File applicationDir = Files.createTempDir();
      Thread.sleep(1001);
      // We might be reusing the same application dir but we want to make sure that the output dir
      // is clean to avoid confusion when returning the output JS.
      File outputDir = new File(applicationDir.getPath() + File.separator + moduleName);
      if (outputDir.exists()) {
        Util.recursiveDelete(outputDir, true);
      }

      // Fake out the resource loader to read resources both from the normal classpath as well as
      // this new application directory.
      ResourceLoader resourceLoader = ResourceLoaders.forClassLoader(Thread.currentThread());
      resourceLoader =
          ResourceLoaders.forPathAndFallback(ImmutableList.of(applicationDir), resourceLoader);

      // Setup options to perform a per-file compile, output to this new application directory and
      // compile the given module.
      compilerOptions.setGenerateJsInteropExports(true);
      compilerOptions.setWarDir(applicationDir);
      compilerOptions.setModuleNames(ImmutableList.of(moduleName));

      // Write the Java/XML/etc resources that make up the test application.
      for (MockResource applicationResource : applicationResources) {
        writeResourceTo(applicationResource, applicationDir);
      }

      // Cause the module to be cached with a reference to the prefixed resource loader so that the
      // compile process will see those resources.
      ModuleDefLoader.clearModuleCache();
      ModuleDefLoader.loadFromResources(logger, moduleName, resourceLoader, true);

      options.addModuleName(moduleName);
      options.setWarDir(new File(compileWorkDir, "war"));
      options.setExtraDir(new File(compileWorkDir, "extra"));

      // Run the compiler once here.
      new Compiler(options).run(logger);
    } finally {
      Util.recursiveDelete(compileWorkDir, false);
      if (oldPersistentUnitCacheValue == null) {
        System.clearProperty(UnitCacheSingleton.GWT_PERSISTENTUNITCACHE);
      } else {
        System.setProperty(UnitCacheSingleton.GWT_PERSISTENTUNITCACHE, oldPersistentUnitCacheValue);
      }
    }
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

  /**
   * Compiles an initial application with version 1 of file Foo, then recompiles using version 2 of
   * file Foo. Lastly it performs a final from scratch compile using version 2 of file Foo and
   * verifies that the recompile and the full compile (both of which used version 2 of file Foo)
   * come out the same.
   */
  private void checkRecompiledModifiedApp(
      String moduleName,
      List<MockResource> sharedResources,
      MockResource originalResource,
      MockResource modifiedResource,
      Set<String> expectedStaleTypeNamesOnModify,
      JsOutputOption output)
      throws IOException, UnableToCompleteException, InterruptedException {
    checkRecompiledModifiedApp(new CompilerOptionsImpl(), moduleName, sharedResources,
        originalResource, modifiedResource, expectedStaleTypeNamesOnModify, output);
  }

  /**
   * Compiles an initial application with version 1 of file Foo, then recompiles using version 2 of
   * file Foo. Lastly it performs a final from scratch compile using version 2 of file Foo and
   * verifies that the recompile and the full compile (both of which used version 2 of file Foo)
   * come out the same.
   */
  private void checkRecompiledModifiedApp(
      CompilerOptions compilerOptions,
      String moduleName,
      List<MockResource> sharedResources,
      MockResource originalResource,
      MockResource modifiedResource,
      Set<String> expectedStaleTypeNamesOnModify,
      JsOutputOption output)
      throws IOException, UnableToCompleteException, InterruptedException {
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
    boolean wasNotModified = modifiedResource == originalResource;
    assertTrue("Resource " + modifiedResource.getPath()  + " was " +
        (wasNotModified ? "NOT " : "") + "modified but the output was " +
        (wasNotModified ? "" : "NOT ") + "different.",
        wasNotModified  == originalAppFromScratchJs.equals(modifiedAppRelinkedJs));

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

    PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.ERROR);
    return compileToJs(logger, compilerOptions, applicationDir, moduleName, applicationResources,
        minimalRebuildCache, expectedProcessedStaleTypeNames, output);
  }

  private String compileToJs(TreeLogger logger, CompilerOptions compilerOptions, File applicationDir,
      String moduleName, List<MockResource> applicationResources,
      MinimalRebuildCache minimalRebuildCache, Set<String> expectedProcessedStaleTypeNames,
      JsOutputOption output) throws IOException, UnableToCompleteException, InterruptedException {
    // Make sure we're using a MemoryUnitCache.
    System.setProperty(UnitCacheSingleton.GWT_PERSISTENTUNITCACHE, "false");
    // Wait 1 second so that any new file modification times are actually different.
    Thread.sleep(1001);
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
    compilerOptions.setGenerateJsInteropExports(true);
    compilerOptions.setWarDir(applicationDir);
    compilerOptions.setModuleNames(ImmutableList.of(moduleName));
    compilerOptions.setOutput(output);

    // Write the Java/XML/etc resources that make up the test application.
    for (MockResource applicationResource : applicationResources) {
      writeResourceTo(applicationResource, applicationDir);
    }

    // Cause the module to be cached with a reference to the prefixed resource loader so that the
    // compile process will see those resources.
    ModuleDefLoader.clearModuleCache();
    ModuleDefLoader.loadFromResources(logger, moduleName, resourceLoader, true);

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

    if (outputJsFile == null) {
      throw new UnableToCompleteException();
    }

    if (expectedProcessedStaleTypeNames != null) {
      Set<String> staleTypeNames =
          new HashSet<>(minimalRebuildCache.getProcessedStaleTypeNames());
      // Since J2CL requires a @JsMethod in Number.java, String.java, etc.  and since JsInterop
      // types are fully traversed (so that correct exports can be regenerated) Number is fully
      // traversed and so shows up in the processed list.
      staleTypeNames.remove("java.lang.Boolean");
      staleTypeNames.remove("java.lang.Double");
      staleTypeNames.remove("java.lang.Number");
      staleTypeNames.remove("java.lang.String");
      assertEquals(expectedProcessedStaleTypeNames, staleTypeNames);
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
