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
package com.google.web.bindery.autobean.gwt.rebind;

import com.google.web.bindery.autobean.gwt.client.impl.AbstractAutoBeanFactory;
import com.google.web.bindery.autobean.gwt.client.impl.ClientPropertyContext;
import com.google.web.bindery.autobean.gwt.client.impl.JsniCreatorMap;
import com.google.web.bindery.autobean.gwt.rebind.model.AutoBeanFactoryMethod;
import com.google.web.bindery.autobean.gwt.rebind.model.AutoBeanFactoryModel;
import com.google.web.bindery.autobean.gwt.rebind.model.AutoBeanMethod;
import com.google.web.bindery.autobean.gwt.rebind.model.AutoBeanType;
import com.google.web.bindery.autobean.gwt.rebind.model.JBeanMethod;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor;
import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.impl.AbstractAutoBean;
import com.google.web.bindery.autobean.shared.impl.AbstractAutoBean.OneShotContext;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.impl.WeakMapping;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.rebind.model.ModelUtils;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates implementations of AutoBeanFactory.
 */
public class AutoBeanFactoryGenerator extends Generator {

  private GeneratorContext context;
  private String simpleSourceName;
  private TreeLogger logger;
  private AutoBeanFactoryModel model;

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    this.context = context;
    this.logger = logger;

    TypeOracle oracle = context.getTypeOracle();
    JClassType toGenerate = oracle.findType(typeName).isInterface();
    if (toGenerate == null) {
      logger.log(TreeLogger.ERROR, typeName + " is not an interface type");
      throw new UnableToCompleteException();
    }

    String packageName = toGenerate.getPackage().getName();
    simpleSourceName = toGenerate.getName().replace('.', '_') + "Impl";
    PrintWriter pw = context.tryCreate(logger, packageName, simpleSourceName);
    if (pw == null) {
      return packageName + "." + simpleSourceName;
    }

    model = new AutoBeanFactoryModel(logger, toGenerate);

    ClassSourceFileComposerFactory factory =
        new ClassSourceFileComposerFactory(packageName, simpleSourceName);
    factory.setSuperclass(AbstractAutoBeanFactory.class.getCanonicalName());
    factory.addImplementedInterface(typeName);
    SourceWriter sw = factory.createSourceWriter(context, pw);
    for (AutoBeanType type : model.getAllTypes()) {
      writeAutoBean(type);
    }
    writeDynamicMethods(sw);
    writeEnumSetup(sw);
    writeMethods(sw);
    sw.commit(logger);

    return factory.getCreatedClassName();
  }

  /**
   * Flattens a parameterized type into a simple list of types.
   */
  private void createTypeList(List<JType> accumulator, JType type) {
    accumulator.add(type);
    JParameterizedType hasParams = type.isParameterized();
    if (hasParams != null) {
      for (JClassType arg : hasParams.getTypeArgs()) {
        createTypeList(accumulator, arg);
      }
    }
  }

  private String getBaseMethodDeclaration(JMethod jmethod) {
    // Foo foo, Bar bar, Baz baz
    StringBuilder parameters = new StringBuilder();
    for (JParameter param : jmethod.getParameters()) {
      parameters.append(",").append(ModelUtils.getQualifiedBaseSourceName(param.getType())).append(
          " ").append(param.getName());
    }
    if (parameters.length() > 0) {
      parameters = parameters.deleteCharAt(0);
    }

    StringBuilder throwsDeclaration = new StringBuilder();
    if (jmethod.getThrows().length > 0) {
      for (JType thrown : jmethod.getThrows()) {
        throwsDeclaration.append(". ").append(ModelUtils.getQualifiedBaseSourceName(thrown));
      }
      throwsDeclaration.deleteCharAt(0);
      throwsDeclaration.insert(0, "throws ");
    }
    String returnName = ModelUtils.getQualifiedBaseSourceName(jmethod.getReturnType());
    assert !returnName.contains("extends");
    return String.format("%s %s(%s) %s", returnName, jmethod.getName(), parameters,
        throwsDeclaration);
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
            && parameters[0].getType().equals(context.getTypeOracle().getJavaLangObject());
    }
    return false;
  }

  private void writeAutoBean(AutoBeanType type) throws UnableToCompleteException {
    PrintWriter pw = context.tryCreate(logger, type.getPackageNome(), type.getSimpleSourceName());
    if (pw == null) {
      // Previously-created
      return;
    }

    ClassSourceFileComposerFactory factory =
        new ClassSourceFileComposerFactory(type.getPackageNome(), type.getSimpleSourceName());
    factory.setSuperclass(AbstractAutoBean.class.getCanonicalName() + "<"
        + type.getPeerType().getQualifiedSourceName() + ">");
    SourceWriter sw = factory.createSourceWriter(context, pw);

    writeShim(sw, type);

    // Instance initializer code to set the shim's association
    sw.println("{ %s.set(shim, %s.class.getName(), this); }", WeakMapping.class.getCanonicalName(),
        AutoBean.class.getCanonicalName());

    // Only simple wrappers have a default constructor
    if (type.isSimpleBean()) {
      // public FooIntfAutoBean(AutoBeanFactory factory) {}
      sw.println("public %s(%s factory) {super(factory);}", type.getSimpleSourceName(),
          AutoBeanFactory.class.getCanonicalName());
    }

    // Wrapping constructor
    // public FooIntfAutoBean(AutoBeanFactory factory, FooIntfo wrapped) {
    sw.println("public %s(%s factory, %s wrapped) {", type.getSimpleSourceName(),
        AutoBeanFactory.class.getCanonicalName(), type.getPeerType().getQualifiedSourceName());
    sw.indentln("super(wrapped, factory);");
    sw.println("}");

    // public FooIntf as() {return shim;}
    sw.println("public %s as() {return shim;}", type.getPeerType().getQualifiedSourceName());

    // public Class<Intf> getType() {return Intf.class;}
    sw.println("public Class<%1$s> getType() {return %1$s.class;}", ModelUtils.ensureBaseType(
        type.getPeerType()).getQualifiedSourceName());

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
    sw.println("@Override protected %s createSimplePeer() {", type.getPeerType()
        .getQualifiedSourceName());
    sw.indent();
    // return new FooIntf() {
    sw.println("return new %s() {", type.getPeerType().getQualifiedSourceName());
    sw.indent();
    sw.println("private final %s data = %s.this.data;", Splittable.class.getCanonicalName(), type
        .getQualifiedSourceName());
    for (AutoBeanMethod method : type.getMethods()) {
      JMethod jmethod = method.getMethod();
      JType returnType = jmethod.getReturnType();
      sw.println("public %s {", getBaseMethodDeclaration(jmethod));
      sw.indent();
      switch (method.getAction()) {
        case GET: {
          String castType;
          if (returnType.isPrimitive() != null) {
            castType = returnType.isPrimitive().getQualifiedBoxedSourceName();
            // Boolean toReturn = Other.this.getOrReify("foo");
            sw.println("%s toReturn = %s.this.getOrReify(\"%s\");", castType, type
                .getSimpleSourceName(), method.getPropertyName());
            // return toReturn == null ? false : toReturn;
            sw.println("return toReturn == null ? %s : toReturn;", returnType.isPrimitive()
                .getUninitializedFieldExpression());
          } else if (returnType.equals(context.getTypeOracle().findType(
              Splittable.class.getCanonicalName()))) {
            sw.println("return data.isNull(\"%1$s\") ? null : data.get(\"%1$s\");", method
                .getPropertyName());
          } else {
            // return (ReturnType) Outer.this.getOrReify(\"foo\");
            castType = ModelUtils.getQualifiedBaseSourceName(returnType);
            sw.println("return (%s) %s.this.getOrReify(\"%s\");", castType, type
                .getSimpleSourceName(), method.getPropertyName());
          }
        }
          break;
        case SET:
        case SET_BUILDER: {
          JParameter param = jmethod.getParameters()[0];
          // Other.this.setProperty("foo", parameter);
          sw.println("%s.this.setProperty(\"%s\", %s);", type.getSimpleSourceName(), method
              .getPropertyName(), param.getName());
          if (JBeanMethod.SET_BUILDER.equals(method.getAction())) {
            sw.println("return this;");
          }
          break;
        }
        case CALL:
          // return com.example.Owner.staticMethod(Outer.this, param,
          // param);
          JMethod staticImpl = method.getStaticImpl();
          if (!returnType.equals(JPrimitiveType.VOID)) {
            sw.print("return ");
          }
          sw.print("%s.%s(%s.this", staticImpl.getEnclosingType().getQualifiedSourceName(),
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
    List<JClassType> privatePeers = new ArrayList<JClassType>();
    sw.println("@Override protected void initializeCreatorMap(%s map) {", JsniCreatorMap.class
        .getCanonicalName());
    sw.indent();
    for (AutoBeanType type : model.getAllTypes()) {
      if (type.isNoWrap()) {
        continue;
      }
      String classLiteralAccessor;
      JClassType peer = type.getPeerType();
      String peerName = ModelUtils.ensureBaseType(peer).getQualifiedSourceName();
      if (peer.isPublic()) {
        classLiteralAccessor = peerName + ".class";
      } else {
        privatePeers.add(peer);
        classLiteralAccessor = "classLit_" + peerName.replace('.', '_') + "()";
      }
      // map.add(Foo.class, getConstructors_com_foo_Bar());
      sw.println("map.add(%s, getConstructors_%s());", classLiteralAccessor, peerName.replace('.',
          '_'));
    }
    sw.outdent();
    sw.println("}");

    /*
     * Create a native method for each peer type that isn't public since Java
     * class literal references are scoped.
     */
    for (JClassType peer : privatePeers) {
      String peerName = ModelUtils.ensureBaseType(peer).getQualifiedSourceName();
      sw.println("private native Class<?> classLit_%s() /*-{return @%s::class;}-*/;", peerName
          .replace('.', '_'), peerName);
    }

    /*
     * Create a method that returns an array containing references to the
     * constructors.
     */
    String factoryJNIName =
        context.getTypeOracle().findType(AutoBeanFactory.class.getCanonicalName())
            .getJNISignature();
    for (AutoBeanType type : model.getAllTypes()) {
      String peerName = ModelUtils.ensureBaseType(type.getPeerType()).getQualifiedSourceName();
      String peerJNIName = ModelUtils.ensureBaseType(type.getPeerType()).getJNISignature();
      /*-
       * private native JsArray<JSO> getConstructors_com_foo_Bar() {
       *   return [
       *     BarProxyImpl::new(ABFactory),
       *     BarProxyImpl::new(ABFactory, DelegateType)
       *   ];
       * }
       */
      sw.println("private native %s<%s> getConstructors_%s() /*-{", JsArray.class
          .getCanonicalName(), JavaScriptObject.class.getCanonicalName(), peerName
          .replace('.', '_'));
      sw.indent();
      sw.println("return [");
      if (type.isSimpleBean()) {
        sw.indentln("@%s::new(%s),", type.getQualifiedSourceName(), factoryJNIName);
      } else {
        sw.indentln(",");
      }
      sw.indentln("@%s::new(%s%s)", type.getQualifiedSourceName(), factoryJNIName, peerJNIName);
      sw.println("];");
      sw.outdent();
      sw.println("}-*/;");
    }
  }

  private void writeEnumSetup(SourceWriter sw) {
    // Make the deobfuscation model
    Map<String, List<JEnumConstant>> map = new HashMap<String, List<JEnumConstant>>();
    for (Map.Entry<JEnumConstant, String> entry : model.getEnumTokenMap().entrySet()) {
      List<JEnumConstant> list = map.get(entry.getValue());
      if (list == null) {
        list = new ArrayList<JEnumConstant>();
        map.put(entry.getValue(), list);
      }
      list.add(entry.getKey());
    }

    sw.println("@Override protected void initializeEnumMap() {");
    sw.indent();
    for (Map.Entry<JEnumConstant, String> entry : model.getEnumTokenMap().entrySet()) {
      // enumToStringMap.put(Enum.FOO, "FOO");
      sw.println("enumToStringMap.put(%s.%s, \"%s\");", entry.getKey().getEnclosingType()
          .getQualifiedSourceName(), entry.getKey().getName(), entry.getValue());
    }
    for (Map.Entry<String, List<JEnumConstant>> entry : map.entrySet()) {
      String listExpr;
      if (entry.getValue().size() == 1) {
        JEnumConstant e = entry.getValue().get(0);
        // Collections.singletonList(Enum.FOO)
        listExpr =
            String.format("%s.<%s<?>> singletonList(%s.%s)", Collections.class.getCanonicalName(),
                Enum.class.getCanonicalName(), e.getEnclosingType().getQualifiedSourceName(), e
                    .getName());
      } else {
        // Arrays.asList(Enum.FOO, OtherEnum.FOO, ThirdEnum,FOO)
        StringBuilder sb = new StringBuilder();
        boolean needsComma = false;
        sb.append(String.format("%s.<%s<?>> asList(", Arrays.class.getCanonicalName(), Enum.class
            .getCanonicalName()));
        for (JEnumConstant e : entry.getValue()) {
          if (needsComma) {
            sb.append(",");
          }
          needsComma = true;
          sb.append(e.getEnclosingType().getQualifiedSourceName()).append(".").append(e.getName());
        }
        sb.append(")");
        listExpr = sb.toString();
      }
      sw.println("stringsToEnumsMap.put(\"%s\", %s);", entry.getKey(), listExpr);
    }
    sw.outdent();
    sw.println("}");
  }

  private void writeMethods(SourceWriter sw) throws UnableToCompleteException {
    for (AutoBeanFactoryMethod method : model.getMethods()) {
      AutoBeanType autoBeanType = method.getAutoBeanType();
      // public AutoBean<Foo> foo(FooSubtype wrapped) {
      sw.println("public %s %s(%s) {", method.getReturnType().getQualifiedSourceName(), method
          .getName(), method.isWrapper()
          ? (method.getWrappedType().getQualifiedSourceName() + " wrapped") : "");
      if (method.isWrapper()) {
        sw.indent();
        // AutoBean<Foo> toReturn = AutoBeanUtils.getAutoBean(wrapped);
        sw.println("%s toReturn = %s.getAutoBean(wrapped);", method.getReturnType()
            .getParameterizedQualifiedSourceName(), AutoBeanUtils.class.getCanonicalName());
        sw.println("if (toReturn != null) {return toReturn;}");
        // return new FooAutoBean(Factory.this, wrapped);
        sw.println("return new %s(%s.this, wrapped);", autoBeanType.getQualifiedSourceName(),
            simpleSourceName);
        sw.outdent();
      } else {
        // return new FooAutoBean(Factory.this);
        sw.indentln("return new %s(%s.this);", autoBeanType.getQualifiedSourceName(),
            simpleSourceName);
      }
      sw.println("}");
    }
  }

  private void writeReturnWrapper(SourceWriter sw, AutoBeanType type, AutoBeanMethod method)
      throws UnableToCompleteException {
    if (!method.isValueType() && !method.isNoWrap()) {
      JMethod jmethod = method.getMethod();
      JClassType returnClass = jmethod.getReturnType().isClassOrInterface();
      AutoBeanType peer = model.getPeer(returnClass);

      sw.println("if (toReturn != null) {");
      sw.indent();
      sw.println("if (%s.this.isWrapped(toReturn)) {", type.getSimpleSourceName());
      sw.indentln("toReturn = %s.this.getFromWrapper(toReturn);", type.getSimpleSourceName());
      sw.println("} else {");
      sw.indent();
      if (peer != null) {
        // toReturn = new FooAutoBean(getFactory(), toReturn).as();
        sw.println("toReturn = new %s(getFactory(), toReturn).as();", peer.getQualifiedSourceName());
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
      sw.println("toReturn = %s.%s(%s.this, toReturn);", interceptor.getEnclosingType()
          .getQualifiedSourceName(), interceptor.getName(), type.getSimpleSourceName());
    }
  }

  /**
   * Create the shim instance of the AutoBean's peer type that lets us hijack
   * the method calls. Using a shim type, as opposed to making the AutoBean
   * implement the peer type directly, means that there can't be any conflicts
   * between methods in the peer type and methods declared in the AutoBean
   * implementation.
   */
  private void writeShim(SourceWriter sw, AutoBeanType type) throws UnableToCompleteException {
    // private final FooImpl shim = new FooImpl() {
    sw.println("private final %1$s shim = new %1$s() {", type.getPeerType()
        .getQualifiedSourceName());
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

      switch (method.getAction()) {
        case GET:
          /*
           * The getter call will ensure that any non-value return type is
           * definitely wrapped by an AutoBean instance.
           */
          sw.println("%s toReturn = %s.this.getWrapped().%s();", ModelUtils
              .getQualifiedBaseSourceName(jmethod.getReturnType()), type.getSimpleSourceName(),
              methodName);

          // Non-value types might need to be wrapped
          writeReturnWrapper(sw, type, method);
          sw.println("return toReturn;");
          break;
        case SET:
        case SET_BUILDER:
          // getWrapped().setFoo(foo);
          sw.println("%s.this.getWrapped().%s(%s);", type.getSimpleSourceName(), methodName,
              parameters[0].getName());
          // FooAutoBean.this.set("setFoo", foo);
          sw.println("%s.this.set(\"%s\", %s);", type.getSimpleSourceName(), methodName,
              parameters[0].getName());
          if (JBeanMethod.SET_BUILDER.equals(method.getAction())) {
            sw.println("return this;");
          }
          break;
        case CALL:
          // XXX How should freezing and calls work together?
          // sw.println("checkFrozen();");
          if (JPrimitiveType.VOID.equals(jmethod.getReturnType())) {
            // getWrapped().doFoo(params);
            sw.println("%s.this.getWrapped().%s(%s);", type.getSimpleSourceName(), methodName,
                arguments);
            // call("doFoo", null, params);
            sw.println("%s.this.call(\"%s\", null%s %s);", type.getSimpleSourceName(), methodName,
                arguments.length() > 0 ? "," : "", arguments);
          } else {
            // Type toReturn = getWrapped().doFoo(params);
            sw.println("%s toReturn = %s.this.getWrapped().%s(%s);", ModelUtils.ensureBaseType(
                jmethod.getReturnType()).getQualifiedSourceName(), type.getSimpleSourceName(),
                methodName, arguments);
            // Non-value types might need to be wrapped
            writeReturnWrapper(sw, type, method);
            // call("doFoo", toReturn, params);
            sw.println("%s.this.call(\"%s\", toReturn%s %s);", type.getSimpleSourceName(),
                methodName, arguments.length() > 0 ? "," : "", arguments);
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
    List<AutoBeanMethod> referencedSetters = new ArrayList<AutoBeanMethod>();
    sw.println("@Override protected void traverseProperties(%s visitor, %s ctx) {",
        AutoBeanVisitor.class.getCanonicalName(), OneShotContext.class.getCanonicalName());
    sw.indent();
    sw.println("%s bean;", AbstractAutoBean.class.getCanonicalName());
    sw.println("Object value;");
    sw.println("%s propertyContext;", ClientPropertyContext.class.getCanonicalName());
    // Local variable ref cleans up emitted js
    sw.println("%1$s as = as();", type.getPeerType().getQualifiedSourceName());

    for (AutoBeanMethod method : type.getMethods()) {
      if (!method.getAction().equals(JBeanMethod.GET)) {
        continue;
      }

      AutoBeanMethod setter = null;
      // If it's not a simple bean type, try to find a real setter method
      if (!type.isSimpleBean()) {
        for (AutoBeanMethod maybeSetter : type.getMethods()) {
          boolean isASetter =
              maybeSetter.getAction().equals(JBeanMethod.SET)
                  || maybeSetter.getAction().equals(JBeanMethod.SET_BUILDER);
          if (isASetter && maybeSetter.getPropertyName().equals(method.getPropertyName())) {
            setter = maybeSetter;
            break;
          }
        }
      }

      // The type of property influences the visitation
      String valueExpression =
          String.format("bean = (%1$s) %2$s.getAutoBean(as.%3$s());", AbstractAutoBean.class
              .getCanonicalName(), AutoBeanUtils.class.getCanonicalName(), method.getMethod()
              .getName());
      String visitMethod;
      String visitVariable = "bean";
      if (method.isCollection()) {
        visitMethod = "Collection";
      } else if (method.isMap()) {
        visitMethod = "Map";
      } else if (method.isValueType()) {
        valueExpression = String.format("value = as.%s();", method.getMethod().getName());
        visitMethod = "Value";
        visitVariable = "value";
      } else {
        visitMethod = "Reference";
      }
      sw.println(valueExpression);

      // Map<List<Foo>, Bar> --> Map, List, Foo, Bar
      List<JType> typeList = new ArrayList<JType>();
      createTypeList(typeList, method.getMethod().getReturnType());
      assert typeList.size() > 0;

      /*
       * Make the PropertyContext that lets us call the setter. We allow
       * multiple methods to be bound to the same property (e.g. to allow JSON
       * payloads to be interpreted as different types). The leading underscore
       * allows purely numeric property names, which are valid JSON map keys.
       */
      // propertyContext = new CPContext(.....);
      sw.println("propertyContext = new %s(", ClientPropertyContext.class.getCanonicalName());
      sw.indent();
      // The instance on which the context is nominally operating
      sw.println("as,");
      // Produce a JSNI reference to a setter function to call
      {
        if (setter != null) {
          // Call a method that returns a JSNI reference to the method to call
          // setFooMethodReference(),
          sw.println("%sMethodReference(as),", setter.getMethod().getName());
          referencedSetters.add(setter);
        } else {
          // Create a function that will update the values map
          // CPContext.beanSetter(FooBeanImpl.this, "foo");
          sw.println("%s.beanSetter(%s.this, \"%s\"),", ClientPropertyContext.Setter.class
              .getCanonicalName(), type.getSimpleSourceName(), method.getPropertyName());
        }
      }
      if (typeList.size() == 1) {
        sw.println("%s.class", ModelUtils.ensureBaseType(typeList.get(0)).getQualifiedSourceName());
      } else {
        // Produce the array of parameter types
        sw.print("new Class<?>[] {");
        boolean first = true;
        for (JType lit : typeList) {
          if (first) {
            first = false;
          } else {
            sw.print(", ");
          }
          sw.print("%s.class", ModelUtils.ensureBaseType(lit).getQualifiedSourceName());
        }
        sw.println("},");

        // Produce the array of parameter counts
        sw.print("new int[] {");
        first = true;
        for (JType lit : typeList) {
          if (first) {
            first = false;
          } else {
            sw.print(", ");
          }
          JParameterizedType hasParam = lit.isParameterized();
          if (hasParam == null) {
            sw.print("0");
          } else {
            sw.print(String.valueOf(hasParam.getTypeArgs().length));
          }
        }
        sw.println("}");
      }
      sw.outdent();
      sw.println(");");

      // if (visitor.visitReferenceProperty("foo", value, ctx))
      sw.println("if (visitor.visit%sProperty(\"%s\", %s, propertyContext)) {", visitMethod, method
          .getPropertyName(), visitVariable);
      if (!method.isValueType()) {
        // Cycle-detection in AbstractAutoBean.traverse
        sw.indentln("if (bean != null) { bean.traverse(visitor, ctx); }");
      }
      sw.println("}");
      // visitor.endVisitorReferenceProperty("foo", value, ctx);
      sw.println("visitor.endVisit%sProperty(\"%s\", %s, propertyContext);", visitMethod, method
          .getPropertyName(), visitVariable);
    }
    sw.outdent();
    sw.println("}");

    for (AutoBeanMethod method : referencedSetters) {
      JMethod jmethod = method.getMethod();
      assert jmethod.getParameters().length == 1;

      /*-
       * Setter setFooMethodReference(Object instance) {
       *   return instance.@com.example.Blah::setFoo(Lcom/example/Foo;);
       * }
       */
      sw.println("public static native %s %sMethodReference(Object instance) /*-{",
          ClientPropertyContext.Setter.class.getCanonicalName(), jmethod.getName());
      sw.indentln("return instance.@%s::%s(%s);", jmethod.getEnclosingType()
          .getQualifiedSourceName(), jmethod.getName(), jmethod.getParameters()[0].getType()
          .getJNISignature());
      sw.println("}-*/;");
    }
  }
}
