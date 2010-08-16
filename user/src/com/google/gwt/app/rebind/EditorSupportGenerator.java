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
package com.google.gwt.app.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.TakesValue;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Generates implementations of {@link
 * com.google.gwt.app.client.EditorSupport EditorSupport} and its
 * nested interfaces.
 */
public class EditorSupportGenerator extends Generator {

  private interface Matcher {
    boolean matches(JClassType classType) throws UnableToCompleteException;
  }

  private class SuperInterfaceType {
    private final JClassType recordType;
    private final JClassType viewType;

    SuperInterfaceType(JClassType interfaceType, TreeLogger logger)
        throws UnableToCompleteException {
      JClassType superInterfaces[] = interfaceType.getImplementedInterfaces();
      JClassType superinterfaceType = superInterfaces[0];
      if (superinterfaceType.isInterface() == null
          || superinterfaceType.isParameterized() == null) {
        logger.log(TreeLogger.ERROR, "The superclass of "
            + superinterfaceType.getQualifiedSourceName()
            + " is either not an interface or not a generic type");
        throw new UnableToCompleteException();
      }

      JParameterizedType parameterizedType = superinterfaceType.isParameterized();
      JClassType typeParameters[] = parameterizedType.getTypeArgs();
      recordType = typeParameters[0];
      viewType = typeParameters[1];
    }
  }

  Map<JField, JClassType> uiPropertyFields;
  JClassType takesValueType;
  JClassType hasTextType;

  JClassType jrecordType;
  JClassType stringType;

  private String capitalize(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  @Override
  public String generate(TreeLogger logger, GeneratorContext generatorContext,
      String interfaceName) throws UnableToCompleteException {
    // The TypeOracle knows about all types in the type system
    TypeOracle typeOracle = generatorContext.getTypeOracle();

    // Get a reference to the type that the generator should implement
    JClassType interfaceType = typeOracle.findType(interfaceName);

    // Ensure that the requested type exists
    if (interfaceType == null) {
      logger.log(TreeLogger.ERROR, "Could not find requested typeName: "
          + interfaceName);
      throw new UnableToCompleteException();
    }
    if (interfaceType.isInterface() == null) {
      // The incoming type wasn't a plain interface, we don't support
      // abstract base classes
      logger.log(TreeLogger.ERROR, interfaceType.getQualifiedSourceName()
          + " is not an interface.", null);
      throw new UnableToCompleteException();
    }

    SuperInterfaceType superinterfaceType = new SuperInterfaceType(
        interfaceType, logger);
    String implName = getImplName(superinterfaceType);
    String packageName = interfaceType.getPackage().getName();

    PrintWriter out = generatorContext.tryCreate(logger, packageName, implName);
    // If an implementation already exists, we don't need to do any work
    if (out != null) {
      generateOnce(logger, generatorContext, out, interfaceType, packageName,
          implName, superinterfaceType);
    }

    return packageName + "." + implName;
  }

  private void generateOnce(TreeLogger logger,
      GeneratorContext generatorContext, PrintWriter out,
      JClassType interfaceType, String packageName, String implName,
      SuperInterfaceType superinterfaceType) throws UnableToCompleteException {

    logger = logger.branch(TreeLogger.DEBUG, String.format(
        "Generating implementation of %s", interfaceType.getName()));
    JClassType recordType = superinterfaceType.recordType;
    JClassType viewType = superinterfaceType.viewType;
    uiPropertyFields = getUiPropertyFields(viewType, recordType,
        generatorContext.getTypeOracle(), logger);

    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        packageName, implName);
    f.addImport(ValueChangeEvent.class.getName());
    f.addImport(ValueChangeHandler.class.getName());
    f.addImport(superinterfaceType.recordType.getQualifiedSourceName());
    f.addImport(Property.class.getName());
    f.addImport(DivElement.class.getName());
    f.addImport(Document.class.getName());
    f.addImport(SpanElement.class.getName());
    f.addImport(FontWeight.class.getName().replace("$", "."));

    f.addImport(HashSet.class.getName());
    f.addImport(Map.class.getName());
    f.addImport(Set.class.getName());
    for (JClassType valueTypes : uiPropertyFields.values()) {
      if (valueTypes.isParameterized() == null) {
        continue;
      }
      String typeName = valueTypes.isParameterized().getTypeArgs()[0].getQualifiedSourceName();
      if (!typeName.startsWith("java.lang")) {
        f.addImport(typeName);
      }
    }
    f.addImplementedInterface(interfaceType.getName());

    SourceWriter sw = f.createSourceWriter(generatorContext, out);
    sw.println();

    takesValueType = generatorContext.getTypeOracle().findType(
        TakesValue.class.getName());
    hasTextType = generatorContext.getTypeOracle().findType(
        HasText.class.getName());
    stringType = generatorContext.getTypeOracle().findType(
        String.class.getName());
    jrecordType = generatorContext.getTypeOracle().findType(
        Record.class.getName());
    writeGetPropertiesMethod(sw, recordType);
    writeInit(sw, viewType, recordType);
    writeIsChangedMethod(sw, recordType, viewType);
    writeSetEnabledMethod(sw, viewType);
    writeSetValueMethod(sw, recordType, viewType, logger);
    writeShowErrorsMethod(sw, viewType);

    sw.outdent();
    sw.println("}");
    generatorContext.commit(logger, out);
  }

  private Collection<JMethod> getAccessibleMethods(JClassType classType) {
    boolean isInterface = false;
    if (classType.isInterface() != null) {
      isInterface = true;
    }
    Map<String, JMethod> methodsBySignature = new HashMap<String, JMethod>();
    LinkedList<JClassType> classesToBeProcessed = new LinkedList<JClassType>();
    classesToBeProcessed.add(classType);
    JClassType tempClassType = null;
    while (classesToBeProcessed.peek() != null) {
      tempClassType = classesToBeProcessed.remove();
      JMethod declaredMethods[] = tempClassType.getMethods();
      for (JMethod method : declaredMethods) {
        if (method.isPrivate()) {
          continue;
        }
        String signature = method.getJsniSignature();
        JMethod existing = methodsBySignature.put(signature, method);
        if (existing != null) {
          // decide which implementation to keep
          if (existing.getEnclosingType().isAssignableTo(
              method.getEnclosingType())) {
            methodsBySignature.put(signature, existing);
          }
        }
      }
      if (isInterface) {
        classesToBeProcessed.addAll(Arrays.asList(tempClassType.getImplementedInterfaces()));
      } else {
        classesToBeProcessed.add(tempClassType.getSuperclass());
      }
    }
    return methodsBySignature.values();
  }

  private Map<String, JField> getAccessiblePropertyFields(JClassType classType) {
    boolean isInterface = false;
    if (classType.isInterface() != null) {
      isInterface = true;
    }
    Map<String, JField> fieldsByName = new HashMap<String, JField>();
    LinkedList<JClassType> classesToBeProcessed = new LinkedList<JClassType>();
    classesToBeProcessed.add(classType);
    JClassType tempClassType = null;
    while (classesToBeProcessed.peek() != null) {
      tempClassType = classesToBeProcessed.remove();
      JField declaredFields[] = tempClassType.getFields();
      for (JField field : declaredFields) {
        JClassType fieldType = field.getType().isClass();
        JParameterizedType parameterizedType = null;
        if (fieldType != null) {
          parameterizedType = fieldType.isParameterized();
        }
        if (field.isPrivate()
            || parameterizedType == null
            || !(parameterizedType.getBaseType().getQualifiedSourceName().equals(Property.class.getName()))) {
          continue;
        }
        JField existing = fieldsByName.put(field.getName(), field);
        if (existing != null) {
          if (existing.getEnclosingType().isAssignableTo(
              field.getEnclosingType())) {
            fieldsByName.put(field.getName(), existing);
          }
        }
      }
      if (isInterface) {
        classesToBeProcessed.addAll(Arrays.asList(tempClassType.getImplementedInterfaces()));
      } else {
        classesToBeProcessed.add(tempClassType.getSuperclass());
      }
    }
    return fieldsByName;
  }

  /**
   * returns the name of the Impl class.
   */
  private String getImplName(SuperInterfaceType superinterfaceType) {
    return superinterfaceType.viewType.getName() + "_EditorSupport_Impl";
  }

  private JMethod getPropertyFunction(JClassType recordType,
      String propertyFunctionName) {

    for (JMethod method : getAccessibleMethods(recordType)) {
      if (method.getName().equals(propertyFunctionName)
          && (method.getParameters() == null || method.getParameters().length == 0)) {
        return method;
      }
    }
    return null;
  }

  private String getPropertyFunctionName(String name, TreeLogger logger)
      throws UnableToCompleteException {
    if (name == null || name.length() < 1) {
      logger.log(TreeLogger.ERROR, "UiField name " + name
          + " is either null or less than a character long");
      throw new UnableToCompleteException();
    }
    return "get" + name.substring(0, 1).toUpperCase()
        + name.substring(1, name.length());
  }

  private String getSuffix(JMethod method) {
    JClassType returnType = (JClassType) method.getReturnType();
    if (returnType.isAssignableTo(stringType)) {
      return "";
    }
    if (returnType.isAssignableTo(jrecordType)) {
      return ".getId()+\"\"";
    }
    return ".toString()";
  }

  /*
   * Returns the JClassType that is a super-interface of field.getType() and
   * matches Matcher. null if no such type.
   */
  private JClassType getSuperInterface(JField uiField, Matcher matcher,
      TreeLogger logger) throws UnableToCompleteException {
    JClassType classType = uiField.getType().isClass();
    if (classType == null) {
      logger.log(TreeLogger.ERROR, "The uiField " + uiField.getName()
          + " is not a class type");
      throw new UnableToCompleteException();
    }
    LinkedList<JClassType> classesToBeProcessed = new LinkedList<JClassType>();
    classesToBeProcessed.add(classType);
    JClassType tempClassType = null;
    while (classesToBeProcessed.peek() != null) {
      tempClassType = classesToBeProcessed.remove();
      if (matcher.matches(tempClassType)) {
        return tempClassType;
      }
      JClassType superclassType = tempClassType.getSuperclass();
      if (superclassType != null) {
        classesToBeProcessed.add(tempClassType.getSuperclass());
      }
      JClassType interfaces[] = tempClassType.getImplementedInterfaces();
      if (interfaces != null && interfaces.length > 0) {
        classesToBeProcessed.addAll(Arrays.asList(tempClassType.getImplementedInterfaces()));
      }
    }
    return null;
  }

  /*
   * Return a map where a key is an uiField of type Property and the value is
   * its super-interface type, either HasText or HasValue.
   */
  private Map<JField, JClassType> getUiPropertyFields(JClassType viewType,
      JClassType recordType, TypeOracle typeOracle, final TreeLogger logger)
      throws UnableToCompleteException {
    JClassType propertyType = typeOracle.findType(Property.class.getName());
    final JClassType localTakesValueType = typeOracle.findType(TakesValue.class.getName());
    final JClassType localHasTextType = typeOracle.findType(HasText.class.getName());

    Map<String, JField> recordFieldNames = getAccessiblePropertyFields(recordType);
    Map<JField, JClassType> localUiPropertyFields = new HashMap<JField, JClassType>();
    for (final JField uiField : viewType.getFields()) {
      JField recordField = recordFieldNames.get(uiField.getName());
      if (uiField.getAnnotation(UiField.class) == null || recordField == null) {
        continue;
      }
      JParameterizedType parameterizedField = recordField.getType().isClass().isParameterized();
      if (parameterizedField == null
          || parameterizedField.getBaseType() != propertyType
          || parameterizedField.getTypeArgs().length != 1) {
        logger.log(TreeLogger.ERROR,
            "A property type must have exactly one type argument");
        throw new UnableToCompleteException();
      }
      final JClassType fieldTypeArg = parameterizedField.getTypeArgs()[0];
      JClassType takesValueSuperInterface = getSuperInterface(uiField,
          new Matcher() {

            public boolean matches(JClassType classType)
                throws UnableToCompleteException {
              JParameterizedType parameterizedType = classType.isParameterized();
              if (parameterizedType == null
                  || parameterizedType.getBaseType() != localTakesValueType
                  || parameterizedType.getTypeArgs().length != 1) {
                return false;
              }
              JClassType typeArg = parameterizedType.getTypeArgs()[0];
              if (typeArg != fieldTypeArg) {
                logger.log(TreeLogger.ERROR, "The type of value "
                    + typeArg.getName() + " UiField " + uiField
                    + " can receive does not match the type of property "
                    + fieldTypeArg.getName());
                throw new UnableToCompleteException();
              }
              return true;
            }

          }, logger);
      if (takesValueSuperInterface != null) {
        localUiPropertyFields.put(uiField, takesValueSuperInterface);
      } else {
        JClassType hasTextSuperInterface = getSuperInterface(uiField,
            new Matcher() {

              public boolean matches(JClassType classType) {
                return classType == localHasTextType;
              }

            }, logger);
        if (hasTextSuperInterface != null) {
          localUiPropertyFields.put(uiField, hasTextSuperInterface);
        } else {
          logger.log(TreeLogger.ERROR, "The UiField " + uiField
              + " does not have a HaxText or HasValue super-interface");
          throw new UnableToCompleteException();
        }
      }
    }
    return localUiPropertyFields;
  }

  /**
   * Write the implementation for the getProperties() method.
   */
  private void writeGetPropertiesMethod(SourceWriter sw, JClassType recordType) {
    sw.indent();
    sw.println("public Set<Property<?>> getProperties() {");
    sw.indent();
    sw.println("Set<Property<?>> rtn = new HashSet<Property<?>>();");
    for (JField field : recordType.getFields()) {
      if (field.getType().getQualifiedSourceName().equals(
          Property.class.getName())) {
        sw.println("rtn.add(" + recordType.getName() + "." + field.getName()
            + ");");
      }
    }
    sw.println("return rtn;");
    sw.outdent();
    sw.println("}");
    sw.outdent();
  }

  private void writeInit(SourceWriter sw, JClassType viewType,
      JClassType recordType) {
    sw.indent();
    sw.println("public void init(final " + viewType.getName() + " view) {");
    sw.indent();
    for (Entry<JField, JClassType> uiFieldEntry : uiPropertyFields.entrySet()) {
      if (uiFieldEntry.getValue() == hasTextType) {
        continue;
      }
      JParameterizedType parameterizedType = uiFieldEntry.getValue().isParameterized();
      if (parameterizedType == null) {
        continue;
      }
      String parameterName = parameterizedType.getTypeArgs()[0].getName();
      sw.println("view." + uiFieldEntry.getKey().getName()
          + ".addValueChangeHandler(new ValueChangeHandler<" + parameterName
          + ">() {");
      sw.indent();
      sw.println("public void onValueChange(ValueChangeEvent<" + parameterName
          + "> event) {");
      sw.indent();
      // recordField and uiFieldEntry have the same name
      sw.println("view.getValue().set"
          + capitalize(uiFieldEntry.getKey().getName()) + "(event.getValue());");
      sw.outdent();
      sw.println("}");
      sw.outdent();
      sw.println("});");
    }
    sw.outdent();
    sw.println("}");
    sw.outdent();
  }

  private void writeIsChangedMethod(SourceWriter sw, JClassType recordType,
      JClassType viewType) {
    sw.indent();
    sw.println("public boolean isChanged(" + viewType.getName() + " view) {");
    sw.indent();
    for (Entry<JField, JClassType> uiFieldEntry : uiPropertyFields.entrySet()) {
      JField property = recordType.getField(uiFieldEntry.getKey().getName());
      if (property != null) {
        String getter = "getValue";
        if (uiFieldEntry.getValue() == hasTextType) {
          getter = "getText";
        }
        sw.println(String.format(
            "view.getValue().set%s(view.%s.%s());",
            capitalize(property.getName()),
            uiFieldEntry.getKey().getName(), getter));
      }
    }
    sw.println("return view.getValue().isChanged();");
    sw.outdent();
    sw.println("}");
    sw.outdent();
  }

  private void writeSetEnabledMethod(SourceWriter sw, JClassType viewType) {
    sw.indent();
    sw.println("public void setEnabled(" + viewType.getName()
        + " view, boolean enabled) {");
    sw.indent();
    sw.println("// Note that we require package protection, just like UiBinder does.");
    for (Entry<JField, JClassType> uiFieldEntry : uiPropertyFields.entrySet()) {
      if (uiFieldEntry.getValue() != takesValueType) {
        continue;
      }
      sw.println("view." + uiFieldEntry.getKey().getName()
          + ".setEnabled(enabled);");
    }
    sw.outdent();
    sw.println("}");
    sw.outdent();
  }

  private void writeSetValueMethod(SourceWriter sw, JClassType recordType,
      JClassType viewType, TreeLogger logger)
      throws UnableToCompleteException {
    // JClassType stringType = generatorContext.getTypeOracle().findType(
    // "java.lang.String");
    sw.indent();
    sw.println("public void setValue(" + viewType.getName() + " view, "
        + recordType.getName() + " record) {");
    sw.indent();

    for (Entry<JField, JClassType> uiFieldEntry : uiPropertyFields.entrySet()) {
      String propertyFunctionName = getPropertyFunctionName(
          uiFieldEntry.getKey().getName(), logger);
      JMethod propertyFunction = getPropertyFunction(recordType,
          propertyFunctionName);
      if (propertyFunction == null) {
        logger.log(TreeLogger.WARN,
            "Not generating setValue/setText for field "
                + uiFieldEntry.getKey().getName());
        continue;
      }
      String functionName = "setValue";
      if (hasTextType == uiFieldEntry.getValue()) {
        functionName = "setText";
      }
      String suffix = "";
      if ("setText".equals(functionName)) {
        suffix = getSuffix(propertyFunction);
      }
      sw.println("view." + uiFieldEntry.getKey().getName() + "." + functionName
          + "(record." + propertyFunctionName + "()" + suffix + ");");
    }
    sw.outdent();
    sw.println("}");
    sw.outdent();
  }

  private void writeShowErrorsMethod(SourceWriter sw, JClassType viewType) {
    sw.indent();
    sw.println("public void showErrors(" + viewType.getName()
        + " view, Map<String, String> errorMap) {");
    sw.indent();
    sw.println("view.errors.setInnerText(\"\");");
    sw.println("if (errorMap == null || errorMap.isEmpty()) {");
    sw.indent();
    sw.println("return;");
    sw.outdent();
    sw.println("}");
    sw.println();

    sw.println("Document doc = Document.get();");
    sw.println("for (Map.Entry<String, String> entry : errorMap.entrySet()) {");
    sw.println("  /*");
    sw.println("   * Note that we are careful not to use setInnerHtml, to ensure we don't");
    sw.println("   * render user created markup: xsite attack protection");
    sw.println("   */");
    sw.println("");
    sw.indent();
    sw.println("DivElement div = doc.createDivElement();");
    sw.println("div.setInnerText(\" \" + entry.getValue());");
    sw.println("");
    sw.println("SpanElement name = doc.createSpanElement();");
    sw.println("name.getStyle().setFontWeight(FontWeight.BOLD);");
    sw.println("name.setInnerText(entry.getKey());");
    sw.println("");
    sw.println("div.insertFirst(name);");
    sw.println("");
    sw.println("view.errors.appendChild(div);");
    sw.outdent();
    sw.println("}");

    sw.outdent();
    sw.println("}");
    sw.outdent();
  }

}
