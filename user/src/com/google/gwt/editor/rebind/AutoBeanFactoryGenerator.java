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
package com.google.gwt.editor.rebind;

import com.google.gwt.core.client.impl.WeakMapping;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.client.AutoBean;
import com.google.gwt.editor.client.AutoBeanUtils;
import com.google.gwt.editor.client.AutoBeanVisitor;
import com.google.gwt.editor.client.AutoBeanVisitor.PropertyContext;
import com.google.gwt.editor.client.impl.AbstractAutoBean;
import com.google.gwt.editor.client.impl.AbstractAutoBean.OneShotContext;
import com.google.gwt.editor.client.impl.AbstractAutoBeanFactory;
import com.google.gwt.editor.rebind.model.AutoBeanFactoryMethod;
import com.google.gwt.editor.rebind.model.AutoBeanFactoryModel;
import com.google.gwt.editor.rebind.model.AutoBeanMethod;
import com.google.gwt.editor.rebind.model.AutoBeanMethod.Action;
import com.google.gwt.editor.rebind.model.AutoBeanType;
import com.google.gwt.editor.rebind.model.ModelUtils;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * Generates implementations of AutoBeanFactory.
 */
public class AutoBeanFactoryGenerator extends Generator {

  private GeneratorContext context;
  private TreeLogger logger;
  private AutoBeanFactoryModel model;

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    this.context = context;
    this.logger = logger;

    TypeOracle oracle = context.getTypeOracle();
    JClassType toGenerate = oracle.findType(typeName).isInterface();
    if (toGenerate == null) {
      logger.log(TreeLogger.ERROR, typeName + " is not an interface type");
      throw new UnableToCompleteException();
    }

    String packageName = toGenerate.getPackage().getName();
    String simpleSourceName = toGenerate.getName().replace('.', '_') + "Impl";
    PrintWriter pw = context.tryCreate(logger, packageName, simpleSourceName);
    if (pw == null) {
      return packageName + "." + simpleSourceName;
    }

    model = new AutoBeanFactoryModel(logger, toGenerate);

    ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
        packageName, simpleSourceName);
    factory.setSuperclass(AbstractAutoBeanFactory.class.getCanonicalName());
    factory.addImplementedInterface(typeName);
    SourceWriter sw = factory.createSourceWriter(context, pw);
    writeDynamicMethods(sw);
    writeMethods(sw);
    sw.commit(logger);

    return factory.getCreatedClassName();
  }

  private String getBaseMethodDeclaration(JMethod jmethod) {
    // Foo foo, Bar bar, Baz baz
    StringBuilder parameters = new StringBuilder();
    for (JParameter param : jmethod.getParameters()) {
      parameters.append(",").append(
          ModelUtils.getQualifiedBaseName(param.getType())).append(" ").append(
          param.getName());
    }
    if (parameters.length() > 0) {
      parameters = parameters.deleteCharAt(0);
    }

    StringBuilder throwsDeclaration = new StringBuilder();
    if (jmethod.getThrows().length > 0) {
      for (JType thrown : jmethod.getThrows()) {
        throwsDeclaration.append(". ").append(
            ModelUtils.getQualifiedBaseName(thrown));
      }
      throwsDeclaration.deleteCharAt(0);
      throwsDeclaration.insert(0, "throws ");
    }
    String returnName = ModelUtils.getQualifiedBaseName(jmethod.getReturnType());
    assert !returnName.contains("extends");
    return String.format("%s %s(%s) %s", returnName, jmethod.getName(),
        parameters, throwsDeclaration);
  }

  /**
   * Used by {@link #writeShim} to avoid duplicate declarations of Object
   * methods.
   */
  private boolean isObjectMethodImplementedByShim(JMethod jmethod) {
    String methodName = jmethod.getName();
    JParameter[] parameters = jmethod.getParameters();
    switch (parameters.length) {
      case 0:
        return methodName.equals("hashCode") || methodName.equals("toString");
      case 1:
        return methodName.equals("equals")
            && parameters[0].getType().equals(
                context.getTypeOracle().getJavaLangObject());
    }
    return false;
  }

  private void writeAutoBean(AutoBeanType type)
      throws UnableToCompleteException {
    PrintWriter pw = context.tryCreate(logger, type.getPackageNome(),
        type.getSimpleSourceName());
    if (pw == null) {
      // Previously-created
      return;
    }

    ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
        type.getPackageNome(), type.getSimpleSourceName());
    factory.setSuperclass(AbstractAutoBean.class.getCanonicalName() + "<"
        + type.getPeerType().getQualifiedSourceName() + ">");
    SourceWriter sw = factory.createSourceWriter(context, pw);

    writeShim(sw, type);

    // Instance initializer code to set the shim's association
    sw.println("{ %s.set(shim, %s.class.getName(), this); }",
        WeakMapping.class.getCanonicalName(), AutoBean.class.getCanonicalName());

    // Only simple wrappers have a default constructor
    if (type.isSimpleBean()) {
      // public FooIntfAutoBean() {}
      sw.println("public %s() {}", type.getSimpleSourceName());
    }

    // Clone constructor
    // public FooIntfAutoBean(FooIntfoAutoBean toClone, boolean deepClone) {
    sw.println("public %1$s(%1$s toClone, boolean deep) {",
        type.getSimpleSourceName());
    sw.indentln("super(toClone, deep);");
    sw.println("}");

    // Wrapping constructor
    // public FooIntfAutoBean(FooIntfo wrapped) {
    sw.println("public %s(%s wrapped) {", type.getSimpleSourceName(),
        type.getPeerType().getQualifiedSourceName());
    sw.indentln("super(wrapped);");
    sw.println("}");

    // public FooIntf as() {return shim;}
    sw.println("public %s as() {return shim;}",
        type.getPeerType().getQualifiedSourceName());

    // public FooIntfAutoBean clone(boolean deep) {
    sw.println("public %s clone(boolean deep) {", type.getQualifiedSourceName());
    // return new FooIntfAutoBean(this, deep);
    sw.indentln("return new %s(this, deep);", type.getSimpleSourceName());
    sw.println("}");

    if (type.isSimpleBean()) {
      writeCreateSimpleBean(sw, type);
    }
    writeTraversal(sw, type);
    sw.commit(logger);
  }

  /**
   * For interfaces that consist of nothing more than getters and setters,
   * create a map-based implementation that will allow the AutoBean's internal
   * state to be easily consumed.
   */
  private void writeCreateSimpleBean(SourceWriter sw, AutoBeanType type) {
    sw.println("@Override protected %s createSimplePeer() {",
        type.getPeerType().getQualifiedSourceName());
    sw.indent();
    // return new FooIntf() {
    sw.println("return new %s() {", type.getPeerType().getQualifiedSourceName());
    sw.indent();
    for (AutoBeanMethod method : type.getMethods()) {
      JMethod jmethod = method.getMethod();
      sw.println("public %s {", getBaseMethodDeclaration(jmethod));
      sw.indent();
      switch (method.getAction()) {
        case GET: {
          // Must handle de-boxing primitive types
          JPrimitiveType primitive = jmethod.getReturnType().isPrimitive();
          if (primitive != null) {
            // Object toReturn = values.get("foo");
            sw.println("Object toReturn = values.get(\"%s\");",
                method.getPropertyName());
            sw.println("if (toReturn == null) {");
            // return 0;
            sw.indentln("return %s;",
                primitive.getUninitializedFieldExpression());
            sw.println("} else {");
            // return (BoxedType) toReturn;
            sw.indentln("return (%s) toReturn;",
                primitive.getQualifiedBoxedSourceName());
            sw.println("}");
          } else {
            // return (ReturnType) values.get(\"foo\");
            sw.println("return (%s) values.get(\"%s\");",
                ModelUtils.getQualifiedBaseName(jmethod.getReturnType()),
                method.getPropertyName());
          }
        }
          break;
        case SET:
          // values.put("foo", parameter);
          sw.println("values.put(\"%s\", %s);", method.getPropertyName(),
              jmethod.getParameters()[0].getName());
          break;
        case CALL:
          // return com.example.Owner.staticMethod(Outer.this, param,
          // param);
          JMethod staticImpl = method.getStaticImpl();
          if (!jmethod.getReturnType().equals(JPrimitiveType.VOID)) {
            sw.print("return ");
          }
          sw.print("%s.%s(%s.this",
              staticImpl.getEnclosingType().getQualifiedSourceName(),
              staticImpl.getName(), type.getSimpleSourceName());
          for (JParameter param : jmethod.getParameters()) {
            sw.print(", %s", param.getName());
          }
          sw.println(");");
          break;
        default:
          throw new RuntimeException();
      }
      sw.outdent();
      sw.println("}");
    }
    sw.outdent();
    sw.println("};");
    sw.outdent();
    sw.println("}");
  }

  /**
   * Write an instance initializer block to populate the creators map.
   */
  private void writeDynamicMethods(SourceWriter sw) {
    sw.println("{");
    sw.indent();
    for (AutoBeanType type : model.getAllTypes()) {
      if (type.isNoWrap()) {
        continue;
      }
      sw.println("creators.put(%s.class, new Creator() {",
          type.getPeerType().getQualifiedSourceName());
      if (type.isSimpleBean()) {
        sw.indentln("public %1$s create() { return new %1$s(); }",
            type.getQualifiedSourceName());
      } else {
        sw.indentln("public %1$s create() { return null; }",
            type.getQualifiedSourceName());
      }
      // public FooAutoBean create(Object delegate) {
      // return new FooAutoBean((Foo) delegate); }
      sw.indentln("public %1$s create(Object delegate) {"
          + " return new %1$s((%2$s) delegate); }",
          type.getQualifiedSourceName(),
          type.getPeerType().getQualifiedSourceName());
      sw.println("});");
    }
    sw.outdent();
    sw.println("}");
  }

  private void writeMethods(SourceWriter sw) throws UnableToCompleteException {
    for (AutoBeanFactoryMethod method : model.getMethods()) {
      AutoBeanType autoBeanType = method.getAutoBeanType();

      writeAutoBean(autoBeanType);
      // public AutoBean<Foo> foo(FooSubtype wrapped) {
      sw.println("public %s %s(%s) {",
          method.getReturnType().getQualifiedSourceName(), method.getName(),
          method.isWrapper()
              ? (method.getWrappedType().getQualifiedSourceName() + " wrapped")
              : "");
      if (method.isWrapper()) {
        sw.indent();
        // AutoBean<Foo> toReturn = AutoBeanUtils.getAutoBean(wrapped);
        sw.println("%s toReturn = %s.getAutoBean(wrapped);",
            method.getReturnType().getParameterizedQualifiedSourceName(),
            AutoBeanUtils.class.getCanonicalName());
        sw.println("if (toReturn != null) {return toReturn;}");
        // return new FooAutoBean(wrapped);
        sw.println("return new %s(wrapped);",
            autoBeanType.getQualifiedSourceName());
        sw.outdent();
      } else {
        // return new FooAutoBean();
        sw.indentln("return new %s();", autoBeanType.getQualifiedSourceName());
      }
      sw.println("}");
    }
  }

  private void writeReturnWrapper(SourceWriter sw, AutoBeanType type,
      AutoBeanMethod method) throws UnableToCompleteException {
    if (!method.isValueType() && !method.isNoWrap()) {
      JMethod jmethod = method.getMethod();
      JClassType returnClass = jmethod.getReturnType().isClassOrInterface();
      AutoBeanType peer = model.getPeer(returnClass);

      sw.println("if (toReturn != null) {");
      sw.indent();
      sw.println("if (%s.this.isWrapped(toReturn)) {",
          type.getSimpleSourceName());
      sw.indentln("toReturn = %s.this.getFromWrapper(toReturn);",
          type.getSimpleSourceName());
      sw.println("} else {");
      sw.indent();
      if (peer != null) {
        // Make sure we generate the potentially unreferenced peer type
        writeAutoBean(peer);
        // toReturn = new FooAutoBean(toReturn).as();
        sw.println("toReturn = new %s(toReturn).as();",
            peer.getQualifiedSourceName());
      }
      sw.outdent();
      sw.println("}");

      sw.outdent();
      sw.println("}");
    }
    // Allow return values to be intercepted
    JMethod interceptor = type.getInterceptor();
    if (interceptor != null) {
      // toReturn = FooCategory.__intercept(FooAutoBean.this, toReturn);
      sw.println("toReturn = %s.%s(%s.this, toReturn);",
          interceptor.getEnclosingType().getQualifiedSourceName(),
          interceptor.getName(), type.getSimpleSourceName());
    }
  }

  /**
   * Create the shim instance of the AutoBean's peer type that lets us hijack
   * the method calls. Using a shim type, as opposed to making the AutoBean
   * implement the peer type directly, means that there can't be any conflicts
   * between methods in the peer type and methods declared in the AutoBean
   * implementation.
   */
  private void writeShim(SourceWriter sw, AutoBeanType type)
      throws UnableToCompleteException {
    // private final FooImpl shim = new FooImpl() {
    sw.println("private final %1$s shim = new %1$s() {",
        type.getPeerType().getQualifiedSourceName());
    sw.indent();
    for (AutoBeanMethod method : type.getMethods()) {
      JMethod jmethod = method.getMethod();
      String methodName = jmethod.getName();
      JParameter[] parameters = jmethod.getParameters();
      if (isObjectMethodImplementedByShim(jmethod)) {
        // Skip any methods declared on Object, since we have special handling
        continue;
      }

      // foo, bar, baz
      StringBuilder arguments = new StringBuilder();
      {
        for (JParameter param : parameters) {
          arguments.append(",").append(param.getName());
        }
        if (arguments.length() > 0) {
          arguments = arguments.deleteCharAt(0);
        }
      }

      sw.println("public %s {", getBaseMethodDeclaration(jmethod));
      sw.indent();

      // Use explicit enclosing this reference to avoid method conflicts
      sw.println("%s.this.checkWrapped();", type.getSimpleSourceName());

      switch (method.getAction()) {
        case GET:
          /*
           * The getter call will ensure that any non-value return type is
           * definitely wrapped by an AutoBean instance.
           */
          // Foo toReturn=FooAutoBean.this.get("getFoo", getWrapped().getFoo());
          sw.println(
              "%s toReturn = %3$s.this.get(\"%2$s\", getWrapped().%2$s());",
              ModelUtils.getQualifiedBaseName(jmethod.getReturnType()),
              methodName, type.getSimpleSourceName());

          // Non-value types might need to be wrapped
          writeReturnWrapper(sw, type, method);
          sw.println("return toReturn;");
          break;
        case SET:
          sw.println("%s.this.checkFrozen();", type.getSimpleSourceName());
          // getWrapped().setFoo(foo);
          sw.println("%s.this.getWrapped().%s(%s);",
              type.getSimpleSourceName(), methodName, parameters[0].getName());
          // FooAutoBean.this.set("setFoo", foo);
          sw.println("%s.this.set(\"%s\", %s);", type.getSimpleSourceName(),
              methodName, parameters[0].getName());
          break;
        case CALL:
          // XXX How should freezing and calls work together?
          // sw.println("checkFrozen();");
          if (JPrimitiveType.VOID.equals(jmethod.getReturnType())) {
            // getWrapped().doFoo(params);
            sw.println("%s.this.getWrapped().%s(%s);",
                type.getSimpleSourceName(), methodName, arguments);
            // call("doFoo", null, params);
            sw.println("%s.this.call(\"%s\", null%s %s);",
                type.getSimpleSourceName(), methodName, arguments.length() > 0
                    ? "," : "", arguments);
          } else {
            // Type toReturn = getWrapped().doFoo(params);
            sw.println(
                "%s toReturn = %s.this.getWrapped().%s(%s);",
                ModelUtils.ensureBaseType(jmethod.getReturnType()).getQualifiedSourceName(),
                type.getSimpleSourceName(), methodName, arguments);
            // Non-value types might need to be wrapped
            writeReturnWrapper(sw, type, method);
            // call("doFoo", toReturn, params);
            sw.println("%s.this.call(\"%s\", toReturn%s %s);",
                type.getSimpleSourceName(), methodName, arguments.length() > 0
                    ? "," : "", arguments);
            sw.println("return toReturn;");
          }
          break;
        default:
          throw new RuntimeException();
      }
      sw.outdent();
      sw.println("}");
    }

    // Delegate equals(), hashCode(), and toString() to wrapped object
    sw.println("@Override public boolean equals(Object o) {");
    sw.indentln("return this == o || getWrapped().equals(o);");
    sw.println("}");
    sw.println("@Override public int hashCode() {");
    sw.indentln("return getWrapped().hashCode();");
    sw.println("}");
    sw.println("@Override public String toString() {");
    sw.indentln("return getWrapped().toString();");
    sw.println("}");

    // End of shim field declaration and assignment
    sw.outdent();
    sw.println("};");
  }

  /**
   * Generate traversal logic.
   */
  private void writeTraversal(SourceWriter sw, AutoBeanType type) {
    sw.println(
        "@Override protected void traverseProperties(%s visitor, %s ctx) {",
        AutoBeanVisitor.class.getCanonicalName(),
        OneShotContext.class.getCanonicalName());
    sw.indent();
    for (AutoBeanMethod method : type.getMethods()) {
      if (!method.getAction().equals(Action.GET)) {
        continue;
      }

      AutoBeanMethod setter = null;
      // If it's not a simple bean type, try to find a real setter method
      if (!type.isSimpleBean()) {
        for (AutoBeanMethod maybeSetter : type.getMethods()) {
          if (maybeSetter.getAction().equals(Action.SET)
              && maybeSetter.getPropertyName().equals(method.getPropertyName())) {
            setter = maybeSetter;
            break;
          }
        }
      }

      // Make the PropertyContext that lets us call the setter
      String propertyContextName = method.getPropertyName() + "PropertyContext";
      sw.println("class %s implements %s {", propertyContextName,
          PropertyContext.class.getCanonicalName());
      sw.indent();
      sw.println("public boolean canSet() { return %s; }", type.isSimpleBean()
          || setter != null);
      // Will return the collection's element type or null if not a collection
      sw.println("public Class<?> getElementType() { return %s; }",
          method.isCollection()
              ? (method.getElementType().getQualifiedSourceName() + ".class")
              : "null");
      // Return the property type
      sw.println("public Class<?> getType() { return %s.class; }",
          method.getMethod().getReturnType().getQualifiedSourceName());
      sw.println("public void set(Object obj) { ");
      if (setter != null) {
        // Prefer the setter if one exists
        sw.indentln(
            "as().%s((%s) obj);",
            setter.getMethod().getName(),
            setter.getMethod().getParameters()[0].getType().getQualifiedSourceName());
      } else if (type.isSimpleBean()) {
        // Otherwise, fall back to a map assignment
        sw.indentln("values.put(\"%s\", obj);", method.getPropertyName());
      } else {
        sw.indentln("throw new UnsupportedOperationException(\"No setter\");");
      }
      sw.println("}");
      sw.outdent();
      sw.println("}");
      sw.println("%1$s %1$s = new %1$s();", propertyContextName);

      if (method.isValueType()) {
        // visitor.visitValueProperty("foo", as().getFoo(), ctx);
        sw.println("visitor.visitValueProperty(\"%s\", as().%s(), %s);",
            method.getPropertyName(), method.getMethod().getName(),
            propertyContextName);
        // visitor.endVisitValueProperty("foo", as().getFoo(), ctx);
        sw.println("visitor.endVisitValueProperty(\"%s\", as().%s(), %s);",
            method.getPropertyName(), method.getMethod().getName(),
            propertyContextName);
      } else {
        sw.println("{");
        sw.indent();
        // AbstractAutoBean auto=(cast)AutoBeanUtils.getAutoBean(as().getFoo());
        sw.println("%1$s auto = (%1$s) %2$s.getAutoBean(as().%3$s());",
            AbstractAutoBean.class.getCanonicalName(),
            AutoBeanUtils.class.getCanonicalName(),
            method.getMethod().getName());
        // if (visitor.visitReferenceProperty("foo", auto, ctx))
        sw.println("if (visitor.visitReferenceProperty(\"%s\", auto, %s))",
            method.getPropertyName(), propertyContextName);
        // Cycle-detection in AbstractAutoBean.traverse
        sw.indentln("if (auto != null) { auto.traverse(visitor, ctx); }");
        // visitor.endVisitorReferenceProperty("foo", auto' ctx);
        sw.println("visitor.endVisitReferenceProperty(\"%s\", auto, %s);",
            method.getPropertyName(), propertyContextName);
        sw.outdent();
        sw.println("}");
      }
    }
    sw.outdent();
    sw.println("}");
  }
}
