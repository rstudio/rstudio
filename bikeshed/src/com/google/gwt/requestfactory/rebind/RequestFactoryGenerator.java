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
package com.google.gwt.requestfactory.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.requestfactory.client.gen.ClientRequestObject;
import com.google.gwt.requestfactory.client.impl.AbstractListJsonRequestObject;
import com.google.gwt.requestfactory.client.impl.RequestFactoryJsonImpl;
import com.google.gwt.requestfactory.shared.ServerOperation;
import com.google.gwt.requestfactory.shared.impl.RequestDataManager;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.PrintWriterManager;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.RecordChangedEvent;
import com.google.gwt.valuestore.shared.impl.RecordImpl;
import com.google.gwt.valuestore.shared.impl.RecordJsoImpl;
import com.google.gwt.valuestore.shared.impl.RecordSchema;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Generates implementations of
 * {@link com.google.gwt.requestfactory.shared.RequestFactory RequestFactory}
 * and its nested interfaces.
 */
public class RequestFactoryGenerator extends Generator {

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

    String packageName = interfaceType.getPackage().getName();
    PrintWriterManager printWriters = new PrintWriterManager(generatorContext,
        logger, packageName);
    // the replace protects against inner classes
    String implName = interfaceType.getName().replace('.', '_') + "Impl";
    PrintWriter out = printWriters.tryToMakePrintWriterFor(implName);

    // If an implementation already exists, we don't need to do any work
    if (out != null) {
      generateOnce(logger, generatorContext, printWriters, out, interfaceType,
          packageName, implName);
    }

    return packageName + "." + implName;
  }

  private String capitalize(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  private String ensureRecordType(TreeLogger logger,
      GeneratorContext generatorContext, PrintWriterManager printWriters,
      TypeOracle typeOracle, String packageName, JClassType publicRecordType)
      throws UnableToCompleteException {
    String recordImplTypeName = publicRecordType.getName() + "Impl";

    PrintWriter pw = printWriters.tryToMakePrintWriterFor(recordImplTypeName);
    if (pw != null) {
      logger = logger.branch(TreeLogger.INFO, "Generating "
          + publicRecordType.getName());

      ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
          packageName, recordImplTypeName);

      String eventTypeName = publicRecordType.getName() + "Changed";
      JClassType eventType = typeOracle.findType(packageName, eventTypeName);
      if (eventType == null) {
        logger.log(TreeLogger.ERROR, String.format(
            "Cannot find %s implementation %s.%s",
            RecordChangedEvent.class.getName(), packageName, eventTypeName));
        throw new UnableToCompleteException();
      }

      f.addImport(Property.class.getName());
      f.addImport(Record.class.getName());
      f.addImport(RecordImpl.class.getName());
      f.addImport(RecordJsoImpl.class.getName());
      f.addImport(RecordSchema.class.getName());

      f.addImport(Collections.class.getName());
      f.addImport(HashSet.class.getName());
      f.addImport(Set.class.getName());

      f.setSuperclass(RecordImpl.class.getSimpleName());
      f.addImplementedInterface(publicRecordType.getName());

      SourceWriter sw = f.createSourceWriter(generatorContext, pw);
      sw.println();
      sw.println(String.format(
          "public static class MySchema extends RecordSchema<%s> {",
          recordImplTypeName));

      sw.indent();
      sw.println("private final Set<Property<?>> allProperties;");
      sw.println("{");

      sw.indent();
      sw.println("Set<Property<?>> set = new HashSet<Property<?>>();");
      sw.println("set.addAll(super.allProperties());");

      JClassType propertyType;
      try {
        propertyType = typeOracle.getType(Property.class.getName());
      } catch (NotFoundException e) {
        throw new RuntimeException(e);
      }

      for (JField field : publicRecordType.getFields()) {
        if (propertyType.getErasedType() == field.getType().getErasedType()) {
          sw.println(String.format("set.add(%s);", field.getName()));
        }
      }

      sw.println("allProperties = Collections.unmodifiableSet(set);");
      sw.outdent();
      sw.println("}");

      sw.println();
      sw.println("public Set<Property<?>> allProperties() {");
      sw.indent();
      sw.println("return allProperties;");
      sw.outdent();
      sw.println("}");

      sw.println();
      sw.println("@Override");
      sw.println(String.format("public %s create(RecordJsoImpl jso) {",
          recordImplTypeName));
      sw.indent();
      sw.println(String.format("return new %s(jso);", recordImplTypeName));
      sw.outdent();
      sw.println("}");

      sw.println();
      sw.println("@Override");
      sw.println(String.format("public %s createChangeEvent(Record record) {",
          eventType.getName()));
      sw.indent();
      sw.println(String.format("return new %s((%s) record);",
          eventType.getName(), publicRecordType.getName()));
      sw.outdent();
      sw.println("}");

      sw.outdent();
      sw.println("}");

      sw.println();
      sw.println(String.format(
          "public static final RecordSchema<%s> SCHEMA = new MySchema();",
          recordImplTypeName));

      sw.println();
      sw.println(String.format("private %s(RecordJsoImpl jso) {",
          recordImplTypeName));
      sw.indent();
      sw.println("super(jso);");
      sw.outdent();
      sw.println("}");

      for (JField field : publicRecordType.getFields()) {
        JType fieldType = field.getType();
        if (propertyType.getErasedType() == fieldType.getErasedType()) {
          JParameterizedType parameterized = fieldType.isParameterized();
          if (parameterized == null) {
            logger.log(TreeLogger.ERROR, fieldType
                + " must have its param type set.");
            throw new UnableToCompleteException();
          }
          JClassType returnType = parameterized.getTypeArgs()[0];
          sw.println();
          sw.println(String.format("public %s get%s() {",
              returnType.getQualifiedSourceName(), capitalize(field.getName())));
          sw.indent();
          sw.println(String.format("return get(%s);", field.getName()));
          sw.outdent();
          sw.println("}");
        }
      }

      sw.outdent();
      sw.println("}");
    }

    return recordImplTypeName;
  }

  private void generateOnce(TreeLogger logger,
      GeneratorContext generatorContext, PrintWriterManager printWriters,
      PrintWriter out, JClassType interfaceType, String packageName,
      String implName) throws UnableToCompleteException {

    logger = logger.branch(TreeLogger.INFO, String.format(
        "Generating implementation of %s", interfaceType.getName()));

    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        packageName, implName);
    f.addImport(RequestFactoryJsonImpl.class.getName());
    f.addImport(interfaceType.getQualifiedSourceName());
    f.addImplementedInterface(interfaceType.getName());
    f.setSuperclass(RequestFactoryJsonImpl.class.getSimpleName());

    SourceWriter sw = f.createSourceWriter(generatorContext, out);
    sw.println();

    // the return types tell us what request selector interfaces
    // to implement
    Set<JClassType> requestSelectors = new LinkedHashSet<JClassType>();
    for (JMethod methodType : interfaceType.getMethods()) {
      JType returnType = methodType.getReturnType();
      if (null == returnType) {
        logger.log(TreeLogger.ERROR, String.format(
            "Illegal return type for %s. Methods of %s must return interfaces",
            methodType.getName(), interfaceType.getName()));
        throw new UnableToCompleteException();
      }
      JClassType asInterface = returnType.isInterface();
      if (null == asInterface) {
        logger.log(TreeLogger.ERROR, String.format(
            "Illegal return type for %s. Methods of %s must return interfaces",
            methodType.getName(), interfaceType.getName()));
        throw new UnableToCompleteException();
      }
      requestSelectors.add(asInterface);
    }
    // write a method for each sub-interface
    for (JClassType requestSelector : requestSelectors) {
      String simpleSourceName = requestSelector.getSimpleSourceName();
      sw.println("public " + simpleSourceName + " "
          + getMethodName(simpleSourceName) + "() {");
      sw.indent();
      sw.println("return new " + simpleSourceName + "Impl(this);");
      sw.outdent();
      sw.println("}");
      sw.println();
    }
    sw.outdent();
    sw.println("}");

    // generate an implementation for each request selector

    for (JClassType nestedInterface : requestSelectors) {
      String nestedImplName = nestedInterface.getName() + "Impl";
      PrintWriter pw = printWriters.makePrintWriterFor(nestedImplName);
      if (pw != null) {
        generateRequestSelectorImplementation(logger, generatorContext,
            printWriters, pw, nestedInterface, interfaceType, packageName,
            nestedImplName);
      }
    }
    printWriters.commit();
  }

  private void generateRequestSelectorImplementation(TreeLogger logger,
      GeneratorContext generatorContext, PrintWriterManager printWriters,
      PrintWriter out, JClassType interfaceType, JClassType mainType,
      String packageName, String implName) throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.INFO, String.format(
        "Generating implementation of %s", interfaceType.getName()));

    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        packageName, implName);
    f.addImport(ClientRequestObject.class.getName());
    f.addImport(AbstractListJsonRequestObject.class.getName());
    f.addImport(RequestDataManager.class.getName());

    JClassType returnType = getReturnType(logger, interfaceType);
    String returnImplTypeName =

    ensureRecordType(logger, generatorContext, printWriters,
        generatorContext.getTypeOracle(), returnType.getPackage().getName(),
        returnType);

    f.addImplementedInterface(interfaceType.getName());

    SourceWriter sw = f.createSourceWriter(generatorContext, out);
    sw.println();

    printRequestImplClass(sw, returnType, returnImplTypeName);

    sw.println("private final " + mainType.getName() + "Impl factory;");
    sw.println();
    // constructor for the class.
    sw.println("public " + implName + "(" + mainType.getName()
        + "Impl factory) {");
    sw.indent();
    sw.println("this.factory = factory;");
    sw.outdent();
    sw.println("}");
    sw.println();

    // write each method.
    for (JMethod method : interfaceType.getMethods()) {
      ServerOperation annotation = method.getAnnotation(ServerOperation.class);
      if (annotation == null) {
        logger.log(TreeLogger.ERROR, "no annotation on the service method "
            + method);
        throw new UnableToCompleteException();
      }
      sw.println(getMethodDeclaration(method) + " {");
      sw.indent();
      sw.println("return new RequestImpl() {");
      sw.indent();
      sw.println("public String getRequestData() {");
      sw.indent();
      sw.println("return " + ClientRequestObject.class.getSimpleName()
          + ".getRequestString(" + RequestDataManager.class.getSimpleName()
          + ".getRequestMap(\"" + annotation.value() + "\", "
          + getParametersAsString(method) + ", null));");
      sw.outdent();
      sw.println("}");
      sw.outdent();
      sw.println("};");
      sw.outdent();
      sw.println("}");
    }

    sw.outdent();
    sw.println("}");
  }

  /**
   * This method is very similar to {@link
   * com.google.gwt.core.ext.typeinfo.JMethod.getReadableDeclaration()}. The
   * only change is that each parameter is final.
   */
  private String getMethodDeclaration(JMethod method) {
    StringBuilder sb = new StringBuilder("public ");
    sb.append(method.getReturnType().getParameterizedQualifiedSourceName());
    sb.append(" ");
    sb.append(method.getName());
    sb.append("(");

    boolean needComma = false;
    for (JParameter param : method.getParameters()) {
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      sb.append("final "); // so that an anonymous class can refer it
      sb.append(param.getType().getParameterizedQualifiedSourceName());
      sb.append(" ");
      sb.append(param.getName());
    }
    sb.append(")");
    return sb.toString();
  }

  private String getMethodName(String simpleSourceName) {
    int length = simpleSourceName.length();
    assert length > 0;
    return Character.toLowerCase(simpleSourceName.charAt(0))
        + simpleSourceName.substring(1, length);
  }

  /**
   * Returns the string representation of the parameters to be passed to the
   * server side method.
   */
  private String getParametersAsString(JMethod method) {
    StringBuilder sb = new StringBuilder();
    for (JParameter parameter : method.getParameters()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(parameter.getName());
      if ("com.google.gwt.valuestore.shared.PropertyReference".equals(parameter.getType().getQualifiedBinaryName())) {
        sb.append(".get()");
      }
    }
    return "new Object[] {" + sb.toString() + "}";
  }

  /**
   * Inspect all the get methods that are returning a List of "domain type".
   * Return the domain type.
   * <p>
   * TODO: Lift the restriction that there be just one return type.
   */
  private JClassType getReturnType(TreeLogger logger, JClassType interfaceType)
      throws UnableToCompleteException {
    Set<JClassType> returnTypes = new LinkedHashSet<JClassType>();
    for (JMethod method : interfaceType.getMethods()) {
      JType returnType = method.getReturnType();
      if (returnType instanceof JParameterizedType) {
        for (JClassType typeArg : ((JParameterizedType) returnType).getTypeArgs()) {
          returnTypes.add(typeArg);
        }
      }
    }
    if (returnTypes.size() != 1) {
      logger.log(TreeLogger.ERROR, "Methods return objects of different types");
      throw new UnableToCompleteException();
    }
    return returnTypes.toArray(new JClassType[0])[0];
  }

  /**
   * Prints the RequestImpl class.
   */
  private void printRequestImplClass(SourceWriter sw, JClassType returnType,
      String returnImplTypeName) {
    sw.println("private abstract class RequestImpl extends "
        + AbstractListJsonRequestObject.class.getSimpleName() + "<"
        + returnType.getName() + ", RequestImpl> {");
    sw.println();
    sw.indent();
    sw.println("RequestImpl() {");
    sw.indent();
    sw.println("super(" + returnImplTypeName + ".SCHEMA, factory);");
    sw.outdent();
    sw.println("}");
    sw.println();
    sw.println("@Override");
    sw.println("protected RequestImpl getThis() {");
    sw.indent();
    sw.println("return this;");
    sw.outdent();
    sw.println("}");
    sw.outdent();
    sw.println("}");
    sw.println();
  }
}
