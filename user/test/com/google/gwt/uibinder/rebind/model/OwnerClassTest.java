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
package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderContext;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for the owner class descriptor.
 */
public class OwnerClassTest extends TestCase {

  private static TreeLogger createCompileLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private TypeOracle types;
  private JClassType labelType;
  private JClassType buttonType;
  private JClassType clickEventType;
  private JClassType mouseOverEventType;
  private UiBinderContext uiBinderCtx;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    CompilationState state = CompilationStateBuilder.buildFrom(
        createCompileLogger(), getJavaResources());
    types = state.getTypeOracle();
    uiBinderCtx = new UiBinderContext();
    labelType = types.findType("com.google.gwt.user.client.ui.Label");
    buttonType = types.findType("com.google.gwt.user.client.ui.Button");
    clickEventType = types.findType("com.google.gwt.event.dom.client.ClickEvent");
    mouseOverEventType = types.findType("com.google.gwt.event.dom.client.MouseOverEvent");
  }

  private Set<Resource> getJavaResources() {
    MockJavaResource[] javaFiles = {
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.EmptyOwnerClass") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("public class EmptyOwnerClass {\n");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.BadUiFactoryClass") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.uibinder.client.UiFactory;\n");
            code.append("public class BadUiFactoryClass {\n");
            code.append("  @UiFactory int thisShouldntWork() { return 0; }\n");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.UiFieldsClass") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.user.client.ui.Button;\n");
            code.append("import com.google.gwt.user.client.ui.Label;\n");
            code.append("import com.google.gwt.uibinder.client.UiField;\n");
            code.append("public class UiFieldsClass {\n");
            code.append("  @UiField Label label1;");
            code.append("  @UiField(provided=true) Button button1;");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.UiHandlersClass") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.event.dom.client.ClickEvent;\n");
            code.append("import com.google.gwt.event.dom.client.MouseOverEvent;\n");
            code.append("import com.google.gwt.uibinder.client.UiHandler;\n");
            code.append("public class UiHandlersClass {\n");
            code.append("  @UiHandler(\"myField\") void onMyFieldClicked(ClickEvent ev) {}");
            code.append("  @UiHandler( {\"myField\", \"myOtherField\"}) void onMouseOver(MouseOverEvent ev){}");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.BadUiFieldsClass") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.uibinder.client.UiField;\n");
            code.append("public class BadUiFieldsClass {\n");
            code.append("  @UiField int thisShouldntWork;");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.UiFactoryClass") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.uibinder.client.UiFactory;\n");
            code.append("import com.google.gwt.user.client.ui.Label;\n");
            code.append("public class UiFactoryClass {");
            code.append("  @UiFactory");
            code.append("  Label createLabel() { return null; }");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource("com.google.gwt.uibinder.rebind.model.Abstract") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("public class Abstract<T> {");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.WildcardWidgetFactory") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.uibinder.client.UiFactory;\n");
            code.append("public class WildcardWidgetFactory {");
            code.append("  @UiFactory");
            code.append("  Abstract<?> createOne() { return null; }");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.ParamterizedWidgetFactory") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.uibinder.client.UiFactory;\n");
            code.append("public class ParamterizedWidgetFactory {");
            code.append("  @UiFactory");
            code.append("  Abstract<String> createOne() { return null; }");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.TooManyGenerics") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.uibinder.client.UiFactory;\n");
            code.append("public class TooManyGenerics {");
            code.append("  @UiFactory");
            code.append("  Abstract<?> createSomething() { return null; }");
            code.append("  @UiFactory");
            code.append("  Abstract<String> createStringThing() { return null; }");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.DuplicateUiFactoryClass") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.uibinder.client.UiFactory;\n");
            code.append("import com.google.gwt.user.client.ui.Label;\n");
            code.append("public class DuplicateUiFactoryClass {");
            code.append("  @UiFactory");
            code.append("  Label labelFactory1() { return null; }");
            code.append("  @UiFactory");
            code.append("  Label labelFactory2() { return null; }");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.ParentUiBinderClass") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.event.dom.client.MouseOverEvent;\n");
            code.append("import com.google.gwt.uibinder.client.UiField;\n");
            code.append("import com.google.gwt.uibinder.client.UiFactory;\n");
            code.append("import com.google.gwt.uibinder.client.UiHandler;\n");
            code.append("import com.google.gwt.user.client.ui.Label;\n");
            code.append("public class ParentUiBinderClass {");
            code.append("  @UiField Label label1;");
            code.append("  @UiFactory Label createLabel() { return null; }");
            code.append("  @UiHandler(\"label1\")");
            code.append("  void onLabelMouseOver(MouseOverEvent e) { }");
            code.append("}\n");
            return code;
          }
        },
        new MockJavaResource(
            "com.google.gwt.uibinder.rebind.model.ChildUiBinderClass") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.uibinder.rebind.model;\n");
            code.append("import com.google.gwt.event.dom.client.ClickEvent;\n");
            code.append("import com.google.gwt.uibinder.client.UiField;\n");
            code.append("import com.google.gwt.uibinder.client.UiFactory;\n");
            code.append("import com.google.gwt.uibinder.client.UiHandler;\n");
            code.append("import com.google.gwt.user.client.ui.Button;\n");
            code.append("public class ChildUiBinderClass extends ParentUiBinderClass {");
            code.append("  @UiField(provided = true) Button button1;");
            code.append("  @UiFactory Button createButton() { return null; }");
            code.append("  @UiHandler(\"button1\")");
            code.append("  void onButtonClicked(ClickEvent e) { }");
            code.append("}\n");
            return code;
          }
        },};

    Set<Resource> rtn = new HashSet<Resource>(UiJavaResources.getUiResources());
    rtn.addAll(Arrays.asList(javaFiles));
    return rtn;
  }

  @SuppressWarnings("deprecation")
  public void testOwnerClass_empty() throws UnableToCompleteException {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.EmptyOwnerClass");
    OwnerClass ownerClass = new OwnerClass(ownerType, MortalLogger.NULL,
        uiBinderCtx);

    assertNull(ownerClass.getUiFactoryMethod(labelType));
    assertNull(ownerClass.getUiField("fieldName"));
    assertNull(ownerClass.getUiFieldForType(labelType));
    assertTrue(ownerClass.getUiFields().isEmpty());
    assertTrue(ownerClass.getUiHandlers().isEmpty());
  }

  public void testOwnerClass_uiFactory() throws UnableToCompleteException {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.UiFactoryClass");
    OwnerClass ownerClass = new OwnerClass(ownerType, MortalLogger.NULL,
        uiBinderCtx);

    JMethod uiFactoryMethod = ownerClass.getUiFactoryMethod(labelType);
    assertNotNull(uiFactoryMethod);
    assertEquals("createLabel", uiFactoryMethod.getName());
    assertEquals(labelType, uiFactoryMethod.getReturnType());
    JParameter[] parameters = uiFactoryMethod.getParameters();
    assertNotNull(parameters);
    assertEquals(0, parameters.length);
  }

  public void testParameterizedWidgets() throws UnableToCompleteException {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.ParamterizedWidgetFactory");
    JClassType abstractType = types.findType("com.google.gwt.uibinder.rebind.model.Abstract");
    OwnerClass ownerClass = new OwnerClass(ownerType, MortalLogger.NULL,
        uiBinderCtx);

    JMethod expected = ownerType.findMethod("createOne", new JType[] {});
    JMethod uiFactoryMethod = ownerClass.getUiFactoryMethod(abstractType);

    assertEquals(expected, uiFactoryMethod);
  }

  public void testWildcardWidgets() throws UnableToCompleteException {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.WildcardWidgetFactory");
    JClassType abstractType = types.findType("com.google.gwt.uibinder.rebind.model.Abstract");
    OwnerClass ownerClass = new OwnerClass(ownerType, MortalLogger.NULL,
        uiBinderCtx);

    JMethod expected = ownerType.findMethod("createOne", new JType[] {});
    JMethod uiFactoryMethod = ownerClass.getUiFactoryMethod(abstractType);

    assertEquals(expected, uiFactoryMethod);
  }

  public void testHowSuckyWeReallyAreWithGenerics() {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.TooManyGenerics");

    try {
      new OwnerClass(ownerType, MortalLogger.NULL, uiBinderCtx);
      fail();
    } catch (UnableToCompleteException e) {
      /* pass */
    }
  }

  public void testOwnerClass_uiFactoryBadType() {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.BadUiFactoryClass");
    try {
      new OwnerClass(ownerType, MortalLogger.NULL, uiBinderCtx);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }
  }

  public void testOwnerClass_uiFactoryDuplicateType() {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.DuplicateUiFactoryClass");
    try {
      new OwnerClass(ownerType, MortalLogger.NULL, uiBinderCtx);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }
  }

  @SuppressWarnings("deprecation")
  public void testOwnerClass_uiFields() throws UnableToCompleteException {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.UiFieldsClass");
    OwnerClass ownerClass = new OwnerClass(ownerType, MortalLogger.NULL,
        uiBinderCtx);

    OwnerField labelField = ownerClass.getUiField("label1");
    OwnerField labelField2 = ownerClass.getUiFieldForType(labelType);
    assertNotNull(labelField);
    assertNotNull(labelField2);
    assertEquals(labelField, labelField2);
    assertFalse(labelField.isProvided());
    assertEquals(labelType, labelField.getType().getRawType());
    assertEquals("label1", labelField.getName());

    OwnerField buttonField = ownerClass.getUiField("button1");
    OwnerField buttonField2 = ownerClass.getUiFieldForType(buttonType);
    assertNotNull(buttonField);
    assertNotNull(buttonField2);
    assertEquals(buttonField, buttonField2);
    assertTrue(buttonField.isProvided());
    assertEquals(buttonType, buttonField.getType().getRawType());
    assertEquals("button1", buttonField.getName());

    Collection<OwnerField> uiFields = ownerClass.getUiFields();
    Set<OwnerField> uiFieldSet = new HashSet<OwnerField>(uiFields);
    Set<OwnerField> expectedFieldSet = new HashSet<OwnerField>();
    expectedFieldSet.add(labelField);
    expectedFieldSet.add(buttonField);
    assertEquals(expectedFieldSet, uiFieldSet);
  }

  public void testOwnerClass_uiFieldsBadType() {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.BadUiFieldsClass");
    try {
      new OwnerClass(ownerType, MortalLogger.NULL, uiBinderCtx);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }
  }

  public void testOwnerClass_uiHandlers() throws UnableToCompleteException {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.UiHandlersClass");
    OwnerClass ownerClass = new OwnerClass(ownerType, MortalLogger.NULL,
        uiBinderCtx);

    // Assert the two expected handlers are there
    List<JMethod> uiHandlers = ownerClass.getUiHandlers();
    assertEquals(2, uiHandlers.size());
    JMethod clickMethod = null, mouseOverMethod = null;

    // Don't care about ordering
    for (JMethod method : uiHandlers) {
      if (method.getName().equals("onMyFieldClicked")) {
        clickMethod = method;
      } else if (method.getName().equals("onMouseOver")) {
        mouseOverMethod = method;
      }
    }

    assertNotNull(clickMethod);
    assertNotNull(mouseOverMethod);

    // Check the click handler
    JParameter[] clickParams = clickMethod.getParameters();
    assertEquals(1, clickParams.length);
    assertEquals(clickEventType, clickParams[0].getType());
    assertTrue(clickMethod.isAnnotationPresent(UiHandler.class));
    UiHandler clickAnnotation = clickMethod.getAnnotation(UiHandler.class);
    String[] clickFields = clickAnnotation.value();
    assertEquals(1, clickFields.length);
    assertEquals("myField", clickFields[0]);

    // Check the mouse over handler
    JParameter[] mouseOverParams = mouseOverMethod.getParameters();
    assertEquals(1, mouseOverParams.length);
    assertEquals(mouseOverEventType, mouseOverParams[0].getType());
    assertTrue(mouseOverMethod.isAnnotationPresent(UiHandler.class));
    UiHandler mouseOverAnnotation = mouseOverMethod.getAnnotation(UiHandler.class);
    String[] mouseOverFields = mouseOverAnnotation.value();
    assertEquals(2, mouseOverFields.length);
    assertEquals("myField", mouseOverFields[0]);
    assertEquals("myOtherField", mouseOverFields[1]);
  }

  @SuppressWarnings("deprecation")
  public void testOwnerClass_withParent() throws UnableToCompleteException {
    JClassType ownerType = types.findType("com.google.gwt.uibinder.rebind.model.ChildUiBinderClass");
    OwnerClass ownerClass = new OwnerClass(ownerType, MortalLogger.NULL,
        uiBinderCtx);

    // Test fields
    OwnerField labelField = ownerClass.getUiField("label1");
    OwnerField labelField2 = ownerClass.getUiFieldForType(labelType);
    assertNotNull(labelField);
    assertNotNull(labelField2);
    assertEquals(labelField, labelField2);
    assertFalse(labelField.isProvided());
    assertEquals(labelType, labelField.getType().getRawType());
    assertEquals("label1", labelField.getName());

    OwnerField buttonField = ownerClass.getUiField("button1");
    OwnerField buttonField2 = ownerClass.getUiFieldForType(buttonType);
    assertNotNull(buttonField);
    assertNotNull(buttonField2);
    assertEquals(buttonField, buttonField2);
    assertTrue(buttonField.isProvided());
    assertEquals(buttonType, buttonField.getType().getRawType());
    assertEquals("button1", buttonField.getName());

    Collection<OwnerField> uiFields = ownerClass.getUiFields();
    Set<OwnerField> uiFieldSet = new HashSet<OwnerField>(uiFields);
    Set<OwnerField> expectedFieldSet = new HashSet<OwnerField>();
    expectedFieldSet.add(labelField);
    expectedFieldSet.add(buttonField);
    assertEquals(expectedFieldSet, uiFieldSet);

    // Test factories
    JMethod labelFactoryMethod = ownerClass.getUiFactoryMethod(labelType);
    assertNotNull(labelFactoryMethod);
    assertEquals("createLabel", labelFactoryMethod.getName());
    assertEquals(labelType, labelFactoryMethod.getReturnType());
    JParameter[] labelParams = labelFactoryMethod.getParameters();
    assertNotNull(labelParams);
    assertEquals(0, labelParams.length);

    JMethod buttonFactoryMethod = ownerClass.getUiFactoryMethod(labelType);
    assertNotNull(buttonFactoryMethod);
    assertEquals("createLabel", buttonFactoryMethod.getName());
    assertEquals(labelType, buttonFactoryMethod.getReturnType());
    JParameter[] buttonParams = buttonFactoryMethod.getParameters();
    assertNotNull(buttonParams);
    assertEquals(0, buttonParams.length);

    // Test handlers
    List<JMethod> uiHandlers = ownerClass.getUiHandlers();
    assertEquals(2, uiHandlers.size());
    JMethod clickMethod = null, mouseOverMethod = null;

    for (JMethod method : uiHandlers) {
      if (method.getName().equals("onButtonClicked")) {
        clickMethod = method;
      } else if (method.getName().equals("onLabelMouseOver")) {
        mouseOverMethod = method;
      }
    }

    assertNotNull(clickMethod);
    assertNotNull(mouseOverMethod);

    JParameter[] clickParams = clickMethod.getParameters();
    assertEquals(1, clickParams.length);
    assertEquals(clickEventType, clickParams[0].getType());
    assertTrue(clickMethod.isAnnotationPresent(UiHandler.class));
    UiHandler clickAnnotation = clickMethod.getAnnotation(UiHandler.class);
    String[] clickFields = clickAnnotation.value();
    assertEquals(1, clickFields.length);
    assertEquals("button1", clickFields[0]);

    JParameter[] mouseOverParams = mouseOverMethod.getParameters();
    assertEquals(1, mouseOverParams.length);
    assertEquals(mouseOverEventType, mouseOverParams[0].getType());
    assertTrue(mouseOverMethod.isAnnotationPresent(UiHandler.class));
    UiHandler mouseOverAnnotation = mouseOverMethod.getAnnotation(UiHandler.class);
    String[] mouseOverFields = mouseOverAnnotation.value();
    assertEquals(1, mouseOverFields.length);
    assertEquals("label1", mouseOverFields[0]);
  }
}
