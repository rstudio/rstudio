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
package com.google.gwt.user.rebind;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.AsyncProxy;
import com.google.gwt.user.client.AsyncProxy.AllowNonVoid;
import com.google.gwt.user.client.AsyncProxy.ConcreteType;
import com.google.gwt.user.client.AsyncProxy.DefaultValue;
import com.google.gwt.user.client.impl.AsyncProxyBase;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Generates implementation of AsyncProxy interfaces.
 */
public class AsyncProxyGenerator extends Generator {

  @Override
  public String generate(TreeLogger logger, GeneratorContext generatorContext,
      String typeName) throws UnableToCompleteException {

    // The TypeOracle knows about all types in the type system
    TypeOracle typeOracle = generatorContext.getTypeOracle();

    // Get a reference to the type that the generator should implement
    JClassType asyncProxyType = typeOracle.findType(AsyncProxy.class.getName());
    JClassType asyncProxyBaseType = typeOracle.findType(AsyncProxyBase.class.getName());
    JClassType sourceType = typeOracle.findType(typeName);

    // Ensure that the requested type exists
    if (sourceType == null) {
      logger.log(TreeLogger.ERROR, "Could not find requested typeName");
      throw new UnableToCompleteException();
    } else if (sourceType.isInterface() == null) {
      // The incoming type wasn't a plain interface, we don't support
      // abstract base classes
      logger.log(TreeLogger.ERROR, sourceType.getQualifiedSourceName()
          + " is not an interface.", null);
      throw new UnableToCompleteException();
    }

    JClassType concreteType = getConcreteType(logger, typeOracle, sourceType);
    JClassType paramType = getParamType(logger, asyncProxyType, sourceType);

    validate(logger, sourceType, concreteType, paramType);

    // com.foo.Bar$Proxy -> com_foo_Bar_ProxyImpl
    String generatedSimpleSourceName = sourceType.getQualifiedSourceName().replace(
        '.', '_').replace('$', '_')
        + "Impl";

    // Begin writing the generated source.
    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        sourceType.getPackage().getName(), generatedSimpleSourceName);
    String createdClassName = f.getCreatedClassName();

    // The generated class needs to be able to determine the module base URL
    f.addImport(GWT.class.getName());
    f.addImport(RunAsyncCallback.class.getName());

    // Used by the map methods
    f.setSuperclass(asyncProxyBaseType.getQualifiedSourceName() + "<"
        + paramType.getQualifiedSourceName() + ">");

    // The whole point of this exercise
    f.addImplementedInterface(sourceType.getQualifiedSourceName());

    // All source gets written through this Writer
    PrintWriter out = generatorContext.tryCreate(logger,
        sourceType.getPackage().getName(), generatedSimpleSourceName);

    // If an implementation already exists, we don't need to do any work
    if (out != null) {
      SourceWriter sw = f.createSourceWriter(generatorContext, out);

      // The invocation of runAsync, with a unique class for the split point
      sw.println("protected void doAsync0() {");
      sw.indent();
      sw.println("GWT.runAsync(new RunAsyncCallback() {");
      sw.indentln("public void onFailure(Throwable caught) {doFailure0(caught);}");
      sw.indentln("public void onSuccess() {setInstance0(doCreate0());}");
      sw.println("});");
      sw.outdent();
      sw.println("}");

      // Field for callback, plus getter and setter
      String proxyCallback = "ProxyCallback<"
          + paramType.getQualifiedSourceName() + ">";
      sw.println("private " + proxyCallback + " callback;");
      sw.println("public void setProxyCallback(" + proxyCallback
          + " callback) {this.callback = callback;}");
      sw.println("protected " + proxyCallback
          + " getCallback0() {return callback;}");

      // doCreate0() method to invoke GWT.create()
      sw.println("private " + paramType.getQualifiedSourceName()
          + " doCreate0() {");
      sw.indent();
      sw.println("return GWT.create(" + concreteType.getQualifiedSourceName()
          + ".class);");
      sw.outdent();
      sw.println("}");

      boolean allowNonVoid = sourceType.getAnnotation(AllowNonVoid.class) != null;
      for (JMethod method : paramType.getOverridableMethods()) {
        DefaultValue defaults = getDefaultValue(sourceType, method);

        if (method.getReturnType() != JPrimitiveType.VOID && !allowNonVoid) {
          logger.log(TreeLogger.ERROR, "The method " + method.getName()
              + " returns a type other than void, but "
              + sourceType.getQualifiedSourceName() + " does not define the "
              + AllowNonVoid.class.getSimpleName() + " annotation.");
          throw new UnableToCompleteException();
        }

        // Print the method decl
        sw.print("public " + method.getReturnType().getQualifiedSourceName()
            + " " + method.getName() + "(");
        for (Iterator<JParameter> i = Arrays.asList(method.getParameters()).iterator(); i.hasNext();) {
          JParameter param = i.next();
          sw.print("final " + param.getType().getQualifiedSourceName() + " "
              + param.getName());
          if (i.hasNext()) {
            sw.print(", ");
          }
        }
        sw.println(") {");
        {
          sw.indent();

          // Try a direct dispatch if we have a proxy instance
          sw.println("if (getProxiedInstance() != null) {");
          {
            sw.indent();
            if (method.getReturnType() != JPrimitiveType.VOID) {
              sw.print("return ");
            }
            writeInvocation(sw, "getProxiedInstance()", method);
            sw.outdent();
          }

          // Otherwise queue up a parameterized command object
          sw.println("} else {");
          {
            sw.indent();
            sw.println("enqueue0(new ParamCommand<"
                + paramType.getQualifiedSourceName() + ">() {");
            {
              sw.indent();
              sw.println("public void execute("
                  + paramType.getQualifiedSourceName() + " t) {");
              {
                sw.indent();
                writeInvocation(sw, "t", method);
                sw.outdent();
              }
              sw.println("}");
              sw.outdent();
            }
            sw.println("});");

            if (method.getReturnType() != JPrimitiveType.VOID) {
              sw.println("return "
                  + getDefaultExpression(defaults, method.getReturnType())
                  + ";");
            }

            sw.outdent();
          }
          sw.println("}");
          sw.outdent();
        }
        sw.println("}");
      }

      sw.commit(logger);
    }

    // Return the name of the concrete class
    return createdClassName;
  }

  private JClassType getConcreteType(TreeLogger logger, TypeOracle typeOracle,
      JClassType sourceType) throws UnableToCompleteException {
    JClassType concreteType;
    ConcreteType concreteTypeAnnotation = sourceType.getAnnotation(ConcreteType.class);
    if (concreteTypeAnnotation == null) {
      logger.log(TreeLogger.ERROR, "AsnycProxy subtypes must specify a "
          + ConcreteType.class.getSimpleName() + " annotation.");
      throw new UnableToCompleteException();
    }

    String concreteTypeName = concreteTypeAnnotation.value().getName().replace(
        '$', '.');
    concreteType = typeOracle.findType(concreteTypeName);

    if (concreteType == null) {
      logger.log(TreeLogger.ERROR,
          "Unable to find concrete type; is it in the GWT source path?");
      throw new UnableToCompleteException();
    }
    return concreteType;
  }

  /**
   * Returns a useful default expression for a type. It is an error to pass
   * JPrimitiveType.VOID into this method.
   */
  private String getDefaultExpression(DefaultValue defaultValue, JType type) {
    if (!(type instanceof JPrimitiveType)) {
      return "null";
    } else if (type == JPrimitiveType.BOOLEAN) {
      return String.valueOf(defaultValue.booleanValue());
    } else if (type == JPrimitiveType.BYTE) {
      return String.valueOf(defaultValue.byteValue());
    } else if (type == JPrimitiveType.CHAR) {
      return String.valueOf((int) defaultValue.charValue());
    } else if (type == JPrimitiveType.DOUBLE) {
      return String.valueOf(defaultValue.doubleValue());
    } else if (type == JPrimitiveType.FLOAT) {
      return String.valueOf(defaultValue.floatValue()) + "F";
    } else if (type == JPrimitiveType.INT) {
      return String.valueOf(defaultValue.intValue());
    } else if (type == JPrimitiveType.LONG) {
      return String.valueOf(defaultValue.longValue());
    } else if (type == JPrimitiveType.SHORT) {
      return String.valueOf(defaultValue.shortValue());
    } else if (type == JPrimitiveType.VOID) {
      assert false : "Should not pass VOID into this method";
    }
    assert false : "Should never reach here";
    return null;
  }

  private DefaultValue getDefaultValue(JClassType sourceType, JMethod method) {
    DefaultValue toReturn = method.getAnnotation(DefaultValue.class);
    if (toReturn == null) {
      toReturn = sourceType.getAnnotation(DefaultValue.class);
    }

    if (toReturn == null) {
      JClassType proxyType = sourceType.getOracle().findType(
          AsyncProxy.class.getName());
      toReturn = proxyType.getAnnotation(DefaultValue.class);
    }

    assert toReturn != null : "Could not find any DefaultValue instance";
    return toReturn;
  }

  private JClassType getParamType(TreeLogger logger, JClassType asyncProxyType,
      JClassType sourceType) throws UnableToCompleteException {
    JClassType paramType = null;
    for (JClassType intr : sourceType.getImplementedInterfaces()) {
      JParameterizedType isParameterized = intr.isParameterized();
      if (isParameterized == null) {
        continue;
      }
      if (isParameterized.getBaseType().equals(asyncProxyType)) {
        paramType = isParameterized.getTypeArgs()[0];
        break;
      }
    }

    if (paramType == null) {
      logger.log(TreeLogger.ERROR, "Unable to determine parameterization type.");
      throw new UnableToCompleteException();
    }
    return paramType;
  }

  private void validate(TreeLogger logger, JClassType sourceType,
      JClassType concreteType, JClassType paramType)
      throws UnableToCompleteException {
    // sourceType may not define any methods
    if (sourceType.getMethods().length > 0) {
      logger.log(TreeLogger.ERROR,
          "AsyncProxy subtypes may not define any additional methods");
      throw new UnableToCompleteException();
    }

    // Make sure that sourceType implements paramType to assignments work
    if (!sourceType.isAssignableTo(paramType)) {
      logger.log(TreeLogger.ERROR, "Expecting "
          + sourceType.getQualifiedSourceName() + " to implement "
          + paramType.getQualifiedSourceName());
      throw new UnableToCompleteException();
    }

    // Make sure that concreteType is assignable to paramType
    if (!concreteType.isAssignableTo(paramType)) {
      logger.log(TreeLogger.ERROR, "Expecting concrete type"
          + concreteType.getQualifiedSourceName() + " to implement "
          + paramType.getQualifiedSourceName());
      throw new UnableToCompleteException();
    }

    // Must be able to GWT.create()
    if (concreteType.isMemberType() && !concreteType.isStatic()) {
      logger.log(TreeLogger.ERROR, "Expecting concrete type "
          + concreteType.getQualifiedSourceName() + " to be static.");
      throw new UnableToCompleteException();
    }
  }

  /**
   * Given a method and a qualifier expression, construct an invocation of the
   * method using its default parameter names.
   */
  private void writeInvocation(SourceWriter sw, String qualifier, JMethod method) {
    sw.print(qualifier + "." + method.getName() + "(");

    for (Iterator<JParameter> i = Arrays.asList(method.getParameters()).iterator(); i.hasNext();) {
      JParameter param = i.next();
      sw.print(param.getName());
      if (i.hasNext()) {
        sw.print(", ");
      }
    }

    sw.println(");");
  }
}
