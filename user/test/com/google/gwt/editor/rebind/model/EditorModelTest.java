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
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.editor.client.CompositeEditor;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDriver;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.HasEditorDelegate;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.editor.client.adapters.SimpleEditor;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.HasText;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;

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
    public CharSequence getContent() {
      return code;
    }
  }

  /**
   * Loads the actual source of a type. This should be used only for types
   * directly tested by this test. Note that use of this class requires your
   * source files to be on your classpath.
   */
  private static class RealJavaResource extends
      MockJavaResource {
    public RealJavaResource(Class<?> clazz) {
      super(clazz.getName());
    }

    @Override
    public CharSequence getContent() {
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

  /**
   * Verify that we correctly descend into a subeditor of a CompositeEditor that
   * also is a LeafValueEditor (as is the case of OptionalFieldEditor).
   */
  public void testCompositeAndLeafValueEditor()
      throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.CompositeAndLeafEditorDriver"), rfedType);

    assertEquals(types.findType("t.CompositeAndLeafEditorDriver.AProxy"),
        m.getProxyType());
    assertEquals(types.findType("t.CompositeAndLeafEditorDriver.AEditor"),
        m.getEditorType());

    EditorData[] data = m.getEditorData();
    assertEquals(1, data.length);

    assertTrue(data[0].isCompositeEditor());

    EditorData composed = data[0].getComposedData();
    assertEquals(types.findType("t.CompositeAndLeafEditorDriver.BProxy"),
        composed.getEditedType());
    assertEquals(types.findType("t.CompositeAndLeafEditorDriver.BEditor"),
        composed.getEditorType());

    // Nonsensical for the optional editor to have any data
    EditorData[] optionalEditorData = m.getEditorData(data[0].getEditorType());
    assertEquals(0, optionalEditorData.length);

    // Make sure we have EditorData for the sub-editor
    EditorData[] subEditorData = m.getEditorData(composed.getEditorType());
    assertEquals(1, subEditorData.length);
  }

  public void testCompositeDriver() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.CompositeEditorDriver"), rfedType);

    EditorData[] data = m.getEditorData();
    assertEquals(9, data.length);

    String[] paths = new String[data.length];
    String[] expressions = new String[data.length];
    for (int i = 0, j = paths.length; i < j; i++) {
      paths[i] = data[i].getPath();
      expressions[i] = data[i].getExpression();
    }
    assertEquals(Arrays.asList("address", "address.city", "address.street", "person", "person.has",
        "person.is", "person.lastModified", "person.name", "person.readonly"), Arrays.asList(paths));
    // address is a property, person is a method in CompositeEditor
    assertEquals(Arrays.asList("address", "address.city", "address.street", "person()",
        "person().has", "person().is", "person().lastModified", "person().name",
        "person().readonly"), Arrays.asList(expressions));
    assertTrue(data[0].isDelegateRequired());
    assertFalse(data[0].isLeafValueEditor() || data[0].isValueAwareEditor());
    assertTrue(data[3].isDelegateRequired());
    assertFalse(data[3].isLeafValueEditor() || data[3].isValueAwareEditor());
    int fieldNum = 4;
    checkPersonHasHas(data[fieldNum++]);
    checkPersonIsIs(data[fieldNum++]);
    checkPersonLastModified(data[fieldNum++]);
    checkPersonName(data[fieldNum++]);
    checkPersonReadonly(data[fieldNum++]);
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
    assertEquals("true", fields[0].getBeanOwnerGuard("object"));
    assertEquals(".getName()", fields[0].getGetterExpression());
    assertEquals("address.street", fields[1].getPath());
    assertEquals(".getAddress()", fields[1].getBeanOwnerExpression());
    assertEquals("object.getAddress() != null",
        fields[1].getBeanOwnerGuard("object"));
    assertEquals(".getStreet()", fields[1].getGetterExpression());
    assertEquals("setStreet", fields[1].getSetterName());
    assertEquals("street", fields[1].getPropertyName());
    assertTrue(fields[1].isDeclaredPathNested());
    assertEquals(types.findType("t.AddressProxy"),
        fields[1].getPropertyOwnerType());
  }

  /**
   * Make sure we find all field-based editors.
   */
  public void testFieldEditors() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.PersonEditorDriver"), rfedType);
    EditorData[] fields = m.getEditorData();
    assertEquals(5, fields.length);

    int fieldNum = 0;
    // hasHas
    checkPersonHasHas(fields[fieldNum++]);
    // isIs
    checkPersonIsIs(fields[fieldNum++]);
    // lastModified
    checkPersonLastModified(fields[fieldNum++]);
    // name
    checkPersonName(fields[fieldNum++]);
    // readonly
    checkPersonReadonly(fields[fieldNum++]);
  }

  public void testFlatData() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.CompositeEditorDriver"), rfedType);

    assertNotNull(m.getEditorData(types.getJavaLangObject()));
    assertEquals(0, m.getEditorData(types.getJavaLangObject()).length);

    EditorData[] composite = m.getEditorData(types.findType("t.CompositeEditor"));
    assertEquals(2, composite.length);
    assertEquals("address", composite[0].getPropertyName());
    assertEquals("person", composite[1].getPropertyName());

    EditorData[] person = m.getEditorData(types.findType("t.PersonEditor"));
    assertEquals(5, person.length);
    int fieldNum = 0;
    assertEquals("has", person[fieldNum++].getPropertyName());
    assertEquals("is", person[fieldNum++].getPropertyName());
    assertEquals("lastModified", person[fieldNum++].getPropertyName());
    assertEquals("name", person[fieldNum++].getPropertyName());
    assertEquals("readonly", person[fieldNum++].getPropertyName());

    EditorData[] address = m.getEditorData(types.findType("t.AddressEditor"));
    assertEquals("city", address[0].getPropertyName());
    assertEquals("street", address[1].getPropertyName());
  }

  /**
   * Tests a plain IsEditor that allows the editor instance to be swapped in by
   * a view object.
   */
  public void testIsEditor() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.UsesIsEditorDriver"), rfedType);

    EditorData[] data = m.getEditorData();
    assertNotNull(data);
    assertEquals(2, data.length);
    assertEquals(Arrays.asList("b", "b.string"),
        Arrays.asList(data[0].getPath(), data[1].getPath()));
    assertEquals(
        Arrays.asList("bEditor().asEditor()", "stringEditor()"),
        Arrays.asList(data[0].getSimpleExpression(),
            data[1].getSimpleExpression()));
  }

  /**
   * Tests the case where an IsEditor also implements the Editor interface.
   */
  public void testIsEditorAndEditor() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.UsesIsEditorAndEditorDriver"), rfedType);

    EditorData[] data = m.getEditorData();
    assertNotNull(data);
    assertEquals(4, data.length);
    assertEquals(Arrays.asList("b", "b.string", "b", "b.string"),
        Arrays.asList(data[0].getPath(), data[1].getPath(), data[2].getPath(),
            data[3].getPath()));
    assertEquals(Arrays.asList("bEditor().asEditor()",
        "bEditor().asEditor().coEditor()", "bEditor()",
        "bEditor().viewEditor()"), Arrays.asList(data[0].getExpression(),
        data[1].getExpression(), data[2].getExpression(),
        data[3].getExpression()));
    assertEquals(
        Arrays.asList(true, false, true, false),
        Arrays.asList(data[0].isDelegateRequired(),
            data[1].isDelegateRequired(), data[2].isDelegateRequired(),
            data[3].isDelegateRequired()));
  }

  public void testListDriver() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.ListEditorDriver"), rfedType);
    assertEquals(types.findType("t.PersonProxy"), m.getProxyType());
    assertEquals(types.findType("t.ListEditor"), m.getEditorType());

    EditorData data = m.getRootData();
    assertTrue(data.isCompositeEditor());

    EditorData composed = data.getComposedData();
    assertEquals(types.findType("t.AddressProxy"), composed.getEditedType());
    assertEquals(types.findType("t.AddressEditor"), composed.getEditorType());

    // Nonsensical for the list editor to have any data
    EditorData[] listEditorData = m.getEditorData(m.getEditorType());
    assertEquals(0, listEditorData.length);

    // Make sure we have EditorData for the sub-editor
    EditorData[] subEditorData = m.getEditorData(composed.getEditorType());
    assertEquals(2, subEditorData.length);
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
    builder.expectError(
        EditorModel.noGetterMessage("missing",
            types.findType("t.MissingGetterEditorDriver.AProxy")), null);
    builder.expectError(
        EditorModel.noGetterMessage("yetAgain",
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
    builder.expectError(
        EditorModel.unexpectedInputTypeMessage(rfedType,
            types.getJavaLangObject()), null);
    builder.expectError(EditorModel.mustExtendMessage(rfedType), null);
    builder.expectError(
        EditorModel.tooManyInterfacesMessage(types.findType("t.TooManyInterfacesEditorDriver")),
        null);
    builder.expectError(EditorModel.foundPrimitiveMessage(JPrimitiveType.LONG,
        "", "lastModified.foo"), null);
    builder.expectError(EditorModel.poisonedMessage(), null);
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
    try {
      new EditorModel(testLogger,
          types.findType("t.PersonEditorWithBadPrimitiveAccessDriver"),
          rfedType);
      fail("Should have thrown exception");
    } catch (UnableToCompleteException expected) {
    }
    testLogger.assertCorrectLogEntries();
  }

  public void testUnparameterizedEditor() {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    builder.expectError(
        EditorModel.noEditorParameterizationMessage(
            types.findType(Editor.class.getName()),
            types.findType(SimpleEditor.class.getName()).isGenericType().getRawType()),
        null);
    UnitTestTreeLogger testLogger = builder.createLogger();
    try {
      new EditorModel(testLogger,
          types.findType("t.UnparameterizedEditorEditorDriver"), rfedType);
      fail("Should have thrown exception");
    } catch (UnableToCompleteException expecetd) {
    }
    testLogger.assertCorrectLogEntries();
  }

  /**
   * Verify that {@code @Path("")} is valid.
   */
  public void testZeroLengthPath() throws UnableToCompleteException {
    EditorModel m = new EditorModel(logger,
        types.findType("t.PersonEditorWithAliasedSubEditorsDriver"), rfedType);
    EditorData[] fields = m.getEditorData();
    assertEquals(12, fields.length);
  }

  private void checkPersonHasHas(EditorData editorField) {
    assertNotNull(editorField);
    assertEquals(types.findType(SimpleEditor.class.getName()),
        editorField.getEditorType().isParameterized().getBaseType());
    assertTrue(editorField.isLeafValueEditor());
    assertFalse(editorField.isDelegateRequired());
    assertFalse(editorField.isValueAwareEditor());
    assertEquals(".hasHas()", editorField.getGetterExpression());
    assertEquals("setHas", editorField.getSetterName());
  }

  private void checkPersonIsIs(EditorData editorField) {
    assertNotNull(editorField);
    assertEquals(types.findType(SimpleEditor.class.getName()),
        editorField.getEditorType().isParameterized().getBaseType());
    assertTrue(editorField.isLeafValueEditor());
    assertFalse(editorField.isDelegateRequired());
    assertFalse(editorField.isValueAwareEditor());
    assertEquals(".isIs()", editorField.getGetterExpression());
    assertEquals("setIs", editorField.getSetterName());
  }

  private void checkPersonLastModified(EditorData editorField) {
    assertNotNull(editorField);
    assertEquals(types.findType(SimpleEditor.class.getName()),
        editorField.getEditorType().isParameterized().getBaseType());
    assertTrue(editorField.isLeafValueEditor());
    assertFalse(editorField.isDelegateRequired());
    assertFalse(editorField.isValueAwareEditor());
    assertEquals(".getLastModified()", editorField.getGetterExpression());
    assertEquals("setLastModified", editorField.getSetterName());
  }

  private void checkPersonName(EditorData editorField) {
    assertNotNull(editorField);
    assertEquals(types.findType(SimpleEditor.class.getName()),
        editorField.getEditorType().isParameterized().getBaseType());
    assertTrue(editorField.isLeafValueEditor());
    assertFalse(editorField.isDelegateRequired());
    assertFalse(editorField.isValueAwareEditor());
    assertEquals(".getName()", editorField.getGetterExpression());
    assertEquals("setName", editorField.getSetterName());
  }

  /**
   * @param editorField
   */
  private void checkPersonReadonly(EditorData editorField) {
    assertNotNull(editorField);
    assertEquals(types.findType(SimpleEditor.class.getName()),
        editorField.getEditorType().isParameterized().getBaseType());
    assertTrue(editorField.isLeafValueEditor());
    assertFalse(editorField.isDelegateRequired());
    assertFalse(editorField.isValueAwareEditor());
    assertEquals(".getReadonly()", editorField.getGetterExpression());
    assertNull(editorField.getSetterName());
  }

  @SuppressWarnings("deprecation")
  private Set<Resource> getDeprecatedResources() {
    return Collections.<Resource> singleton(new EmptyMockJavaResource(
        com.google.web.bindery.requestfactory.shared.Violation.class));
  }
  
  private Set<Resource> getJavaResources() {
    MockJavaResource[] javaFiles = {new MockJavaResource("t.AddressProxy") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("interface AddressProxy extends EntityProxy {\n");
        code.append("String getCity();\n");
        code.append("void setCity(String city);\n");
        code.append("String getStreet();\n");
        code.append("void setStreet(String street);\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.AddressEditor") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("class AddressEditor implements Editor<AddressProxy> {\n");
        code.append("public SimpleEditor<String> city;\n");
        code.append("public SimpleEditor<String> street;\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.CompositeProxy") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("interface CompositeProxy extends EntityProxy {\n");
        code.append("AddressProxy getAddress();\n");
        code.append("PersonProxy getPerson();\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.CompositeEditor") {
      @Override
      public CharSequence getContent() {
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
      public CharSequence getContent() {
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
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("interface CyclicEditorDriver extends"
            + " RequestFactoryEditorDriver<CyclicEditorDriver.AProxy,"
            + " CyclicEditorDriver.AEditor> {\n");
        code.append("  interface AProxy extends EntityProxy { BProxy getB(); }");
        code.append("  interface BProxy extends EntityProxy { AProxy getA(); }");
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
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("interface DottedPathEditorDriver extends"
            + " RequestFactoryEditorDriver<PersonProxy,"
            + " DottedPathEditorDriver.PersonEditor> {\n");
        code.append("  interface PersonEditor extends Editor<PersonProxy> {");
        code.append("  SimpleEditor<String> nameEditor();");
        code.append("  @Editor.Path(\"address.street\")");
        code.append("  SimpleEditor<String> streetEditor();");
        code.append("  }");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.ListEditor") {
        // Tests error-detection when the editor graph isn't a DAG
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + CompositeEditor.class.getName() + ";\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("interface ListEditor extends CompositeEditor<PersonProxy, AddressProxy, AddressEditor>, Editor<PersonProxy> {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.ListEditorDriver") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("interface ListEditorDriver extends RequestFactoryEditorDriver<PersonProxy, ListEditor> {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.MissingGetterEditorDriver") {
        // Tests error-detection when the editor structure doesn't match the
        // proxy
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("interface MissingGetterEditorDriver extends"
            + " RequestFactoryEditorDriver<MissingGetterEditorDriver.AProxy,"
            + " MissingGetterEditorDriver.AEditor> {\n");
        code.append("  interface AProxy extends EntityProxy {}");
        code.append("  interface AEditor extends Editor<AProxy> {");
        code.append("    SimpleEditor<String> missingEditor();");
        code.append("    SimpleEditor<String> yetAgain();");
        code.append("  }");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonProxy") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("interface PersonProxy extends EntityProxy {\n");
        code.append("AddressProxy getAddress();\n");
        code.append("String getName();\n");
        code.append("long getLastModified();\n");
        code.append("String getReadonly();\n");
        code.append("boolean hasHas();\n");
        code.append("boolean isIs();\n");
        code.append("void setHas(boolean has);\n");
        code.append("void setIs(boolean is);\n");
        code.append("void setName(String name);\n");
        code.append("void setLastModified(long value);\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditor") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("class PersonEditor implements Editor<PersonProxy> {\n");
        code.append("SimpleEditor<Boolean> has;\n");
        code.append("SimpleEditor<Boolean> is;\n");
        code.append("SimpleEditor<Long> lastModified;\n");
        code.append("public SimpleEditor<String> name;\n");
        code.append("SimpleEditor<String> readonly;\n");
        code.append("public static SimpleEditor ignoredStatic;\n");
        code.append("private SimpleEditor<String> ignoredPrivate;\n");
        code.append("@Editor.Ignore public SimpleEditor<String> ignoredPublic;\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditorWithAliasedSubEditors") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("class PersonEditorWithAliasedSubEditors implements Editor<PersonProxy> {\n");
        code.append("@Path(\"\") PersonEditor e1;\n");
        code.append("@Path(\"\") PersonEditor e2;\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditorWithAliasedSubEditorsDriver") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("interface PersonEditorWithAliasedSubEditorsDriver extends"
            + " RequestFactoryEditorDriver<PersonProxy, t.PersonEditorWithAliasedSubEditors> {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditorWithBadPrimitiveAccess") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("class PersonEditorWithBadPrimitiveAccess implements Editor<PersonProxy> {\n");
        code.append("@Path(\"lastModified.foo\") SimpleEditor<String> bad;\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditorWithBadPrimitiveAccessDriver") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("interface PersonEditorWithBadPrimitiveAccessDriver extends"
            + " RequestFactoryEditorDriver<PersonProxy, t.PersonEditorWithBadPrimitiveAccess> {\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditorUsingMethods") {
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("abstract class PersonEditorUsingMethods implements Editor<PersonProxy> {\n");
        code.append("public abstract SimpleEditor<String> nameEditor();\n");
        code.append("protected abstract SimpleEditor<String> readonlyEditor();\n");
        code.append("public static SimpleEditor<String> ignoredStatic() {return null;}\n");
        code.append("private SimpleEditor<String> ignoredPrivate() {return null;}\n");
        code.append("@Editor.Ignore public abstract SimpleEditor<String> ignoredPublic();\n");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.PersonEditorDriver") {
      @Override
      public CharSequence getContent() {
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
      public CharSequence getContent() {
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
      public CharSequence getContent() {
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
      public CharSequence getContent() {
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
    }, new MockJavaResource("t.UnparameterizedEditorEditorDriver") {
        // Tests error-detection when the editor structure doesn't match the
        // proxy
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("interface UnparameterizedEditorEditorDriver extends"
            + " RequestFactoryEditorDriver<UnparameterizedEditorEditorDriver.AProxy,"
            + " UnparameterizedEditorEditorDriver.AEditor> {\n");
        code.append("  interface AProxy extends EntityProxy {}");
        code.append("  interface AEditor extends Editor<AProxy> {");
        code.append("    SimpleEditor needsParameterization();");
        code.append("  }");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.UsesIsEditorDriver") {
        // Tests error-detection when the editor structure doesn't match the
        // proxy
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + IsEditor.class.getName() + ";\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("interface UsesIsEditorDriver extends"
            + " RequestFactoryEditorDriver<UsesIsEditorDriver.AProxy,"
            + " UsesIsEditorDriver.AEditor> {\n");
        code.append("  interface AProxy extends EntityProxy { BProxy getB();}");
        code.append("  interface BProxy extends EntityProxy { String getString();}");
        code.append("  interface AEditor extends Editor<AProxy> {");
        code.append("    BView bEditor();");
        code.append("  }");
        code.append("  interface BView extends IsEditor<BEditor> {");
        code.append("    @Editor.Path(\"string\") BEditor unseen();");
        code.append("  }");
        code.append("  interface BEditor extends Editor<BProxy> {");
        code.append("    SimpleEditor<String> stringEditor();");
        code.append("  }");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.UsesIsEditorAndEditorDriver") {
        // Tests error-detection when the editor structure doesn't match the
        // proxy
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + HasEditorErrors.class.getName() + ";\n");
        code.append("import " + IsEditor.class.getName() + ";\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("interface UsesIsEditorAndEditorDriver extends"
            + " RequestFactoryEditorDriver<UsesIsEditorAndEditorDriver.AProxy,"
            + " UsesIsEditorAndEditorDriver.AEditor> {\n");
        code.append("  interface AProxy extends EntityProxy { BProxy getB();}");
        code.append("  interface BProxy extends EntityProxy { String getString();}");
        code.append("  interface AEditor extends Editor<AProxy> {");
        code.append("    BView bEditor();");
        code.append("  }");
        code.append("  interface BView extends IsEditor<BEditor>, Editor<BProxy>, HasEditorErrors {");
        code.append("    @Editor.Path(\"string\") SimpleEditor<String> viewEditor();");
        code.append("  }");
        code.append("  interface BEditor extends Editor<BProxy> {");
        code.append("    @Editor.Path(\"string\") SimpleEditor<String> coEditor();");
        code.append("  }");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("t.CompositeAndLeafEditorDriver") {
      /*
       * Tests that we descend into sub-editor of a CompositeEditor that also is
       * a LeafValueEditor (this is the case for the
       * c.g.g.editor.client.adapters.OptionalFieldEditor). Also test that any
       * editor-like fields within the LeafValueEditor are ignored.
       */
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package t;\n");
        code.append("import " + Editor.class.getName() + ";\n");
        code.append("import " + IsEditor.class.getName() + ";\n");
        code.append("import " + EntityProxy.class.getName() + ";\n");
        code.append("import " + RequestFactoryEditorDriver.class.getName()
            + ";\n");
        code.append("import " + SimpleEditor.class.getName() + ";\n");
        code.append("import " + CompositeEditor.class.getName() + ";\n");
        code.append("import " + LeafValueEditor.class.getName() + ";\n");
        code.append("interface CompositeAndLeafEditorDriver extends"
            + " RequestFactoryEditorDriver<CompositeAndLeafEditorDriver.AProxy,"
            + " CompositeAndLeafEditorDriver.AEditor> {\n");
        code.append("  interface AProxy extends EntityProxy { BProxy getB();}");
        code.append("  interface BProxy extends EntityProxy { String getString();}");
        code.append("  interface AEditor extends Editor<AProxy> {");
        code.append("    OptionalBEditor bEditor();");
        code.append("  }");
        code.append("  interface OptionalBEditor extends CompositeEditor<BProxy, BProxy, BEditor>, LeafValueEditor<BProxy> {");
        code.append("    LeafValueEditor<String> ignored();");
        code.append("  }");
        code.append("  interface BEditor extends Editor<BProxy> {");
        code.append("    @Editor.Path(\"string\") SimpleEditor<String> coEditor();");
        code.append("  }");
        code.append("}");
        return code;
      }
    }, new MockJavaResource("java.util.List") {
        // Tests a Driver interface that extends more than RFED
      @Override
      public CharSequence getContent() {
        StringBuilder code = new StringBuilder();
        code.append("package java.util;\n");
        code.append("public interface List<T> {\n");
        code.append("}");
        return code;
      }
    }};

    Set<Resource> toReturn = new HashSet<Resource>(Arrays.asList(javaFiles));
    toReturn.addAll(Arrays.asList(new Resource[] {
        new RealJavaResource(CompositeEditor.class),
        new EmptyMockJavaResource(ConstraintViolation.class),
        new RealJavaResource(Editor.class),
        new EmptyMockJavaResource(EditorDriver.class),
        new RealJavaResource(EditorError.class),
        new EmptyMockJavaResource(EntityProxy.class),
        new EmptyMockJavaResource(EventBus.class),
        new EmptyMockJavaResource(com.google.web.bindery.event.shared.EventBus.class),
        new EmptyMockJavaResource(HasEditorDelegate.class),
        new EmptyMockJavaResource(HasEditorErrors.class),
        new RealJavaResource(HasText.class),
        new RealJavaResource(IsEditor.class),
        new EmptyMockJavaResource(Iterable.class),
        new RealJavaResource(LeafValueEditor.class),
        new EmptyMockJavaResource(RequestFactory.class),
        new RealJavaResource(RequestFactoryEditorDriver.class),
        new EmptyMockJavaResource(Request.class),
        new EmptyMockJavaResource(RequestContext.class),
        new RealJavaResource(SimpleEditor.class),
        new RealJavaResource(TakesValue.class),
        new EmptyMockJavaResource(ValueAwareEditor.class)}));
    toReturn.addAll(getDeprecatedResources());
    toReturn.addAll(Arrays.asList(JavaResourceBase.getStandardResources()));
    return toReturn;
  }
}
