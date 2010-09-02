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
package com.google.gwt.editor.rebind.model;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.editor.client.adapters.StringEditor;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.RequestFactoryEditorDriver;
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.Record;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.HasText;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Test case for {@link EditorModel} that uses mock CompilationStates.
 */
public class EditorModelTest extends TestCase {

  /**
   * Constructs an empty interface representation of a type.
   */
  private static class EmptyMockJavaResource extends MockJavaResource {
    private final StringBuilder code = new StringBuilder();

    public EmptyMockJavaResource(Class<?> clazz) {
      super(clazz.getName());

      code.append("package ").append(clazz.getPackage().getName()).append(";\n");
      code.append("public interface ").append(clazz.getSimpleName());

      int numParams = clazz.getTypeParameters().length;
      if (numParams != 0) {
        code.append("<");
        for (int i = 0; i < numParams; i++) {
          if (i != 0) {
            code.append(",");
          }
          code.append("T").append(i);
        }
        code.append(">");
      }

      code.append("{}\n");
    }

    @Override
    protected CharSequence getContent() {
      return code;
    }
  }

  /**
   * Loads the actual source of a type. This should be used only for types
   * directly tested by this test.
   */
  private static class RealJavaResource extends MockJavaResource {
    public RealJavaResource(Class<?> clazz) {
      super(clazz.getName());
    }

    @Override
    protected CharSequence getContent() {
      String resourceName = getTypeName().replace('.', '/') + ".java";
      InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
          resourceName);
      return Util.readStreamAsString(stream);
    }
  }

  private static TreeLogger createCompileLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private TreeLogger logger;
  private JClassType rfedType;
  private TypeOracle types;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    logger = createCompileLogger();
    CompilationState state = CompilationStateBuilder.buildFrom(logger,
        getJavaResources());
    types = state.getTypeOracle();
    rfedType = types.findType(RequestFactoryEditorDriver.class.getName());
    assertNotNull(rfedType);
  }

  /**
   * Test the simple getters on the Model object.
   */
  public void testBasicAttributes() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.PersonEditorDriver"), rfedType);

    assertEquals(types.findType("t.PersonEditor"), m.getEditorType());
    assertEquals(types.findType("t.PersonProxy"), m.getProxyType());
  }

  public void testCompositeDriver() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.CompositeEditorDriver"), rfedType);

    EditorData[] data = m.getEditorData();
    assertEquals(6, data.length);

    String[] paths = new String[data.length];
    String[] expressions = new String[data.length];
    for (int i = 0, j = paths.length; i < j; i++) {
      paths[i] = data[i].getPath();
      expressions[i] = data[i].getExpression();
    }
    assertEquals(Arrays.asList("address", "address.city", "address.street",
        "person", "person.name", "person.readonly"), Arrays.asList(paths));
    // address is a property, person is a method in CompositeEditor
    assertEquals(Arrays.asList("address", "address.city", "address.street",
        "person()", "person().name", "person().readonly"),
        Arrays.asList(expressions));
    assertTrue(data[0].isBeanEditor());
    assertFalse(data[0].isLeafValueEditor() || data[0].isValueAwareEditor());
    assertTrue(data[3].isBeanEditor());
    assertFalse(data[3].isLeafValueEditor() || data[3].isValueAwareEditor());
    checkPersonName(data[4]);
    checkPersonReadonly(data[5]);
  }

  public void testCyclicDriver() {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    builder.expectError(
        EditorModel.cycleErrorMessage(
            types.findType("t.CyclicEditorDriver.AEditor"), "<Root Object>",
            "b.a"), null);
    builder.expectError(EditorModel.poisonedMessage(), null);
    UnitTestTreeLogger testLogger = builder.createLogger();
    try {
      new EditorModel(testLogger, types.findType("t.CyclicEditorDriver"),
          rfedType);
      fail("Should have complained about cycle");
    } catch (UnableToCompleteException expected) {
    }
    testLogger.assertCorrectLogEntries();
  }

  public void testDottedPath() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.DottedPathEditorDriver"), rfedType);
    EditorData[] fields = m.getEditorData();
    assertEquals(2, fields.length);
    assertEquals("name", fields[0].getPath());
    assertFalse(fields[0].isDeclaredPathNested());
    assertEquals("", fields[0].getBeanOwnerExpression());
    assertEquals("getName", fields[0].getGetterName());
    assertEquals("address.street", fields[1].getPath());
    assertEquals(".getAddress()", fields[1].getBeanOwnerExpression());
    assertEquals("getStreet", fields[1].getGetterName());
    assertEquals("setStreet", fields[1].getSetterName());
    assertEquals("street", fields[1].getPropertyName());
    assertTrue(fields[1].isDeclaredPathNested());
  }

  /**
   * Make sure we find all field-based editors.
   */
  public void testFieldEditors() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.PersonEditorDriver"), rfedType);
    EditorData[] fields = m.getEditorData();
    assertEquals(2, fields.length);

    // name
    checkPersonName(fields[0]);
    // readonly
    checkPersonReadonly(fields[1]);
  }

  public void testFlatData() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.CompositeEditorDriver"), rfedType);

    assertNull(m.getEditorData(types.getJavaLangObject()));

    EditorData[] composite = m.getEditorData(types.findType("t.CompositeEditor"));
    assertEquals(2, composite.length);
    assertEquals("address", composite[0].getPropertyName());
    assertEquals("person", composite[1].getPropertyName());

    EditorData[] person = m.getEditorData(types.findType("t.PersonEditor"));
    assertEquals(2, person.length);
    assertEquals("name", person[0].getPropertyName());
    assertEquals("readonly", person[1].getPropertyName());

    EditorData[] address = m.getEditorData(types.findType("t.AddressEditor"));
    assertEquals("city", address[0].getPropertyName());
    assertEquals("street", address[1].getPropertyName());
  }

  /**
   * Make sure we can find all method-based editors.
   */
  public void testMethodEditors() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.PersonEditorDriverUsingMethods"), rfedType);
    EditorData[] fields = m.getEditorData();
    assertEquals(2, fields.length);

    // nameEditor()
    checkPersonName(fields[0]);
    checkPersonReadonly(fields[1]);
  }

  /**
   * Tests the case where an Editor wants to editor a property that is not
   * provided by its associated Proxy type.
   */
  public void testMissingGetter() {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    builder.expectError(EditorModel.noGetterMessage("missing",
        types.findType("t.MissingGetterEditorDriver.AProxy")), null);
    builder.expectError(EditorModel.noGetterMessage("yetAgain",
        types.findType("t.MissingGetterEditorDriver.AProxy")), null);
    builder.expectError(EditorModel.poisonedMessage(), null);
    UnitTestTreeLogger testLogger = builder.createLogger();
    try {
      new EditorModel(testLogger,
          types.findType("t.MissingGetterEditorDriver"), rfedType);
      fail("Should have thrown exception");
    } catch (UnableToCompleteException expecetd) {
    }
    testLogger.assertCorrectLogEntries();
  }

  /**
   * Tests the sanity-check error messages emitted by the constructor.
   */
  public void testSanityErrorMessages() {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    builder.expectError(EditorModel.unexpectedInputTypeMessage(rfedType,
        types.getJavaLangObject()), null);
    builder.expectError(EditorModel.mustExtendMessage(rfedType), null);
    builder.expectError(
        EditorModel.tooManyInterfacesMessage(types.findType("t.TooManyInterfacesEditorDriver")),
        null);
    UnitTestTreeLogger testLogger = builder.createLogger();

    try {
      new EditorModel(testLogger, types.getJavaLangObject(), rfedType);
      fail("Should have thrown exception");
    } catch (UnableToCompleteException expected) {
    }
    try {
      new EditorModel(testLogger, rfedType, rfedType);
      fail("Should have thrown exception");
    } catch (UnableToCompleteException expected) {
    }
    try {
      new EditorModel(testLogger,
          types.findType("t.TooManyInterfacesEditorDriver"), rfedType);
      fail("Should have thrown exception");
    } catch (UnableToCompleteException expected) {
    }
    testLogger.assertCorrectLogEntries();
  }

  private void checkPersonName(EditorData editorField) {
    assertNotNull(editorField);
    assertEquals(types.findType(StringEditor.class.getName()),
        editorField.getEditorType());
    assertTrue(editorField.isLeafValueEditor());
    assertFalse(editorField.isBeanEditor());
    assertFalse(editorField.isValueAwareEditor());
    assertEquals("getName", editorField.getGetterName());
    assertEquals("setName", editorField.getSetterName());
  }

  /**
   * @param editorField
   */
  private void checkPersonReadonly(EditorData editorField) {
    assertNotNull(editorField);
    assertEquals(types.findType(StringEditor.class.getName()),
        editorField.getEditorType());
    assertTrue(editorField.isLeafValueEditor());
    assertFalse(editorField.isBeanEditor());
    assertFalse(editorField.isValueAwareEditor());
    assertEquals("getReadonly", editorField.getGetterName());
    assertNull(editorField.getSetterName());
  }

  private Set<Resource> getJavaResources() {
    MockJavaResource[] javaFiles = {new MockJavaResource("t.AddressProxy") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Record.class.getName() + ";\n");
        code.append("interface AddressProxy extends Record {\n");
        code.append("String getCity();\n");
        code.append("void setCity(String city);\n");
        code.append("String getStreet();\n");
        code.append("void setStreet(String street);\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.AddressEditor") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + StringEditor.class.getName() + ";\n");
        code.append("class AddressEditor implements Editor<AddressProxy> {\n");
        code.append("public StringEditor city;\n");
        code.append("public StringEditor street;\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.CompositeProxy") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Record.class.getName() + ";\n");
        code.append("interface CompositeProxy extends Record {\n");
        code.append("AddressProxy getAddress();\n");
        code.append("PersonProxy getPerson();\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.CompositeEditor") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("class CompositeEditor implements Editor<CompositeProxy> {\n");
        code.append("AddressEditor address;\n");
        code.append("PersonEditor person(){return null;}\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.CompositeEditorDriver") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("interface CompositeEditorDriver extends RequestFactoryEditorDriver<CompositeProxy, CompositeEditor> {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.CyclicEditorDriver") {
      // Tests error-detection when the editor graph isn't a DAG
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + Record.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("interface CyclicEditorDriver extends"
            + " RequestFactoryEditorDriver<CyclicEditorDriver.AProxy,"
            + " CyclicEditorDriver.AEditor> {\n");
        code.append("  interface AProxy extends Record { BProxy getB(); }");
        code.append("  interface BProxy extends Record { AProxy getA(); }");
        code.append("  interface AEditor extends Editor<AProxy> {");
        code.append("    BEditor bEditor();");
        code.append("  }");
        code.append("  interface BEditor extends Editor<BProxy> {");
        code.append("    AEditor aEditor();");
        code.append("  }");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.DottedPathEditorDriver") {
      // Tests error-detection when the editor graph isn't a DAG
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + Record.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("import " + StringEditor.class.getName() + ";\n");
        code.append("interface DottedPathEditorDriver extends"
            + " RequestFactoryEditorDriver<PersonProxy,"
            + " DottedPathEditorDriver.PersonEditor> {\n");
        code.append("  interface PersonEditor extends Editor<PersonProxy> {");
        code.append("  StringEditor nameEditor();");
        code.append("  @Editor.Path(\"address.street\")");
        code.append("  StringEditor streetEditor();");
        code.append("  }");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.MissingGetterEditorDriver") {
      // Tests error-detection when the editor structure doesn't match the proxy
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + Record.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("import " + StringEditor.class.getName() + ";\n");
        code.append("interface MissingGetterEditorDriver extends"
            + " RequestFactoryEditorDriver<MissingGetterEditorDriver.AProxy,"
            + " MissingGetterEditorDriver.AEditor> {\n");
        code.append("  interface AProxy extends Record {}");
        code.append("  interface AEditor extends Editor<AProxy> {");
        code.append("    StringEditor missingEditor();");
        code.append("    StringEditor yetAgain();");
        code.append("  }");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonProxy") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Record.class.getName() + ";\n");
        code.append("interface PersonProxy extends Record {\n");
        code.append("AddressProxy getAddress();\n");
        code.append("String getName();\n");
        code.append("void setName(String name);\n");
        code.append("String getReadonly();\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditor") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + StringEditor.class.getName() + ";\n");
        code.append("class PersonEditor implements Editor<PersonProxy> {\n");
        code.append("public StringEditor name;\n");
        code.append("StringEditor readonly;\n");
        code.append("public static StringEditor ignoredStatic;\n");
        code.append("private StringEditor ignoredPrivate;\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditorUsingMethods") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + StringEditor.class.getName() + ";\n");
        code.append("abstract class PersonEditorUsingMethods implements Editor<PersonProxy> {\n");
        code.append("public abstract StringEditor nameEditor();\n");
        code.append("protected abstract StringEditor readonlyEditor();\n");
        code.append("public static StringEditor ignoredStatic() {return null;}\n");
        code.append("private StringEditor ignoredPrivate() {return null;}\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditorDriver") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("interface PersonEditorDriver extends"
            + " RequestFactoryEditorDriver<PersonProxy, PersonEditor> {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditorDriverUsingMethods") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("interface PersonEditorDriverUsingMethods extends"
            + " RequestFactoryEditorDriver<PersonProxy, PersonEditorUsingMethods> {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonRequestFactory") {
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + RequestFactory.class.getName() + ";\n");
        code.append("interface PersonRequestFactory extends RequestFactory {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.TooManyInterfacesEditorDriver") {
      // Tests a Driver interface that extends more than RFED
      @Override
      protected CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("interface TooManyInterfacesEditorDriver extends"
            + " RequestFactoryEditorDriver<CompositeProxy, CompositeEditor>,"
            + " java.io.Serializable {\n");
        code.append("}");
        return code;
      }
    }};

    Set<Resource> toReturn = new HashSet<Resource>(Arrays.asList(javaFiles));
    toReturn.addAll(Arrays.asList(new Resource[] {
        new RealJavaResource(Editor.class),
        new EmptyMockJavaResource(EventBus.class),
        new RealJavaResource(HasText.class),
        new RealJavaResource(LeafValueEditor.class),
        new EmptyMockJavaResource(Property.class),
        new EmptyMockJavaResource(Record.class),
        new EmptyMockJavaResource(RequestFactory.class),
        new RealJavaResource(RequestFactoryEditorDriver.class),
        new EmptyMockJavaResource(RequestObject.class),
        new RealJavaResource(StringEditor.class),
        new RealJavaResource(TakesValue.class),
        new EmptyMockJavaResource(ValueAwareEditor.class),}));
    toReturn.addAll(Arrays.asList(JavaResourceBase.getStandardResources()));
    return toReturn;
  }
}
