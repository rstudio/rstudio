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

import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanFactory;
import com.google.gwt.autobean.shared.AutoBeanFactory.Category;
import com.google.gwt.autobean.shared.AutoBeanFactory.NoWrap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.rebind.model.ModelUtils;
import com.google.gwt.requestfactory.client.impl.AbstractClientRequestFactory;
import com.google.gwt.requestfactory.rebind.model.ContextMethod;
import com.google.gwt.requestfactory.rebind.model.EntityProxyModel;
import com.google.gwt.requestfactory.rebind.model.EntityProxyModel.Type;
import com.google.gwt.requestfactory.rebind.model.RequestFactoryModel;
import com.google.gwt.requestfactory.rebind.model.RequestMethod;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.impl.AbstractRequest;
import com.google.gwt.requestfactory.shared.impl.AbstractRequestContext;
import com.google.gwt.requestfactory.shared.impl.AbstractRequestFactory;
import com.google.gwt.requestfactory.shared.impl.BaseProxyCategory;
import com.google.gwt.requestfactory.shared.impl.EntityProxyCategory;
import com.google.gwt.requestfactory.shared.impl.RequestData;
import com.google.gwt.requestfactory.shared.impl.ValueProxyCategory;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Generates implementations of
 * {@link com.google.gwt.requestfactory.shared.RequestFactory RequestFactory}
 * and its nested interfaces.
 */
public class RequestFactoryGenerator extends Generator {

  private GeneratorContext context;
  private TreeLogger logger;
  private RequestFactoryModel model;

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

    model = new RequestFactoryModel(logger, toGenerate);

    ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
        packageName, simpleSourceName);
    factory.setSuperclass(AbstractClientRequestFactory.class.getCanonicalName());
    factory.addImplementedInterface(typeName);
    SourceWriter sw = factory.createSourceWriter(context, pw);
    writeAutoBeanFactory(sw);
    writeContextMethods(sw);
    writeContextImplementations();
    writeTypeMap(sw);
    sw.commit(logger);

    return factory.getCreatedClassName();
  }

  private void writeAutoBeanFactory(SourceWriter sw) {
    // Map in static implementations of EntityProxy methods
    sw.println("@%s({%s.class, %s.class, %s.class})",
        Category.class.getCanonicalName(),
        EntityProxyCategory.class.getCanonicalName(),
        ValueProxyCategory.class.getCanonicalName(),
        BaseProxyCategory.class.getCanonicalName());
    // Don't wrap our id type, because it makes code grungy
    sw.println("@%s(%s.class)", NoWrap.class.getCanonicalName(),
        EntityProxyId.class.getCanonicalName());
    sw.println("interface Factory extends %s {",
        AutoBeanFactory.class.getCanonicalName());
    sw.indent();

    for (EntityProxyModel proxy : model.getAllProxyModels()) {
      // AutoBean<FooProxy> com_google_FooProxy();
      sw.println("%s<%s> %s();", AutoBean.class.getCanonicalName(),
          proxy.getQualifiedSourceName(),
          proxy.getQualifiedSourceName().replace('.', '_'));
    }
    sw.outdent();
    sw.println("}");

    // public static final Factory FACTORY = GWT.create(Factory.class);
    sw.println("public static final Factory FACTORY=%s.create(Factory.class);",
        GWT.class.getCanonicalName());

    // Write public accessor
    sw.println("@Override public Factory getAutoBeanFactory() { return FACTORY; }");
  }

  private void writeContextImplementations() {
    for (ContextMethod method : model.getMethods()) {
      PrintWriter pw = context.tryCreate(logger, method.getPackageName(),
          method.getSimpleSourceName());
      if (pw == null) {
        // Already generated
        continue;
      }

      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          method.getPackageName(), method.getSimpleSourceName());
      factory.setSuperclass(AbstractRequestContext.class.getCanonicalName());
      factory.addImplementedInterface(method.getImplementedInterfaceQualifiedSourceName());
      SourceWriter sw = factory.createSourceWriter(context, pw);

      // Constructor that accepts the parent RequestFactory
      sw.println("public %s(%s requestFactory) {super(requestFactory);}",
          method.getSimpleSourceName(),
          AbstractRequestFactory.class.getCanonicalName());

      // Write each Request method
      for (RequestMethod request : method.getRequestMethods()) {
        JMethod jmethod = request.getDeclarationMethod();
        String operation = jmethod.getEnclosingType().getQualifiedBinaryName()
            + "::" + jmethod.getName();

        // foo, bar, baz
        StringBuilder parameterArray = new StringBuilder();
        // final Foo foo, final Bar bar, final Baz baz
        StringBuilder parameterDeclaration = new StringBuilder();
        // <P extends Blah>
        StringBuilder typeParameterDeclaration = new StringBuilder();

        if (request.isInstance()) {
          // Leave a spot for the using() method to fill in later
          parameterArray.append(",null");
        }
        for (JTypeParameter param : jmethod.getTypeParameters()) {
          typeParameterDeclaration.append(",").append(
              param.getQualifiedSourceName());
        }
        for (JParameter param : jmethod.getParameters()) {
          parameterArray.append(",").append(param.getName());
          parameterDeclaration.append(",final ").append(
              param.getType().getParameterizedQualifiedSourceName()).append(" ").append(
              param.getName());
        }
        if (parameterArray.length() > 0) {
          parameterArray.deleteCharAt(0);
        }
        if (parameterDeclaration.length() > 0) {
          parameterDeclaration.deleteCharAt(0);
        }
        if (typeParameterDeclaration.length() > 0) {
          typeParameterDeclaration.deleteCharAt(0).insert(0, "<").append(">");
        }

        // public Request<Foo> doFoo(final Foo foo) {
        sw.println("public %s %s %s(%s) {", typeParameterDeclaration,
            jmethod.getReturnType().getParameterizedQualifiedSourceName(),
            jmethod.getName(), parameterDeclaration);
        sw.indent();
        // The implements clause covers InstanceRequest
        // class X extends AbstractRequest<Return> implements Request<Return> {
        sw.println("class X extends %s<%s> implements %s {",
            AbstractRequest.class.getCanonicalName(),
            request.getDataType().getParameterizedQualifiedSourceName(),
            jmethod.getReturnType().getParameterizedQualifiedSourceName());
        sw.indent();

        // public X() { super(FooRequestContext.this); }
        sw.println("public X() { super(%s.this);}",
            method.getSimpleSourceName());

        // This could also be gotten rid of by having only Request /
        // InstanceRequest
        sw.println("@Override public X with(String... paths) {super.with(paths); return this;}");

        // makeRequestData()
        sw.println("@Override protected %s makeRequestData() {",
            RequestData.class.getCanonicalName());
        // return new RequestData("Foo::bar", {parameters}, propertyRefs,
        // List.class, FooProxy.class);
        String elementType = request.isCollectionType()
            ? request.getCollectionElementType().getQualifiedSourceName()
                + ".class" : "null";
        String returnTypeBaseQualifiedName = ModelUtils.ensureBaseType(
            request.getDataType()).getQualifiedSourceName();
        sw.indentln(
            "return new %s(\"%s\", new Object[] {%s}, propertyRefs, %s.class, %s);",
            RequestData.class.getCanonicalName(), operation, parameterArray,
            returnTypeBaseQualifiedName, elementType);
        sw.println("}");

        // end class X{}
        sw.outdent();
        sw.println("}");

        // Instantiate, enqueue, and return
        sw.println("X x = new X();");
        // See comment in AbstractRequest.using(EntityProxy)
        if (!request.isInstance()) {
          sw.println("addInvocation(x);");
        }
        sw.println("return x;");
        sw.outdent();
        sw.println("}");
      }

      sw.commit(logger);
    }
  }

  private void writeContextMethods(SourceWriter sw) {
    for (ContextMethod method : model.getMethods()) {
      // public FooService foo() {
      sw.println("public %s %s() {", method.getQualifiedSourceName(),
          method.getMethodName());
      // return new FooServiceImpl(this);
      sw.indentln("return new %s(this);", method.getQualifiedSourceName());
      sw.println("}");
    }
  }

  private void writeTypeMap(SourceWriter sw) {
    sw.println("private static final %1$s<String, Class<?>> tokensToTypes"
        + " = new %1$s<String, Class<?>>();", HashMap.class.getCanonicalName());
    sw.println("private static final %1$s<Class<?>, String> typesToTokens"
        + " = new %1$s<Class<?>, String>();", HashMap.class.getCanonicalName());
    sw.println(
        "private static final %1$s<Class<?>> entityProxyTypes = new %1$s<Class<?>>();",
        HashSet.class.getCanonicalName());
    sw.println(
        "private static final %1$s<Class<?>> valueProxyTypes = new %1$s<Class<?>>();",
        HashSet.class.getCanonicalName());
    sw.println("static {");
    sw.indent();
    for (EntityProxyModel type : model.getAllProxyModels()) {
      // tokensToTypes.put("Foo", Foo.class);
      sw.println("tokensToTypes.put(\"%1$s\", %1$s.class);",
          type.getQualifiedSourceName());
      // typesToTokens.put(Foo.class, Foo);
      sw.println("typesToTokens.put(%1$s.class, \"%1$s\");",
          type.getQualifiedSourceName());
      // fooProxyTypes.add(MyFooProxy.class);
      sw.println("%s.add(%s.class);", type.getType().equals(Type.ENTITY)
          ? "entityProxyTypes" : "valueProxyTypes",
          type.getQualifiedSourceName());
    }
    sw.outdent();
    sw.println("}");

    // Write instance methods
    sw.println("@Override protected Class getTypeFromToken(String typeToken) {");
    sw.indentln("return tokensToTypes.get(typeToken);");
    sw.println("}");
    sw.println("@Override protected String getTypeToken(Class type) {");
    sw.indentln("return typesToTokens.get(type);");
    sw.println("}");
    sw.println("@Override public boolean isEntityType(Class<?> type) {");
    sw.indentln("return entityProxyTypes.contains(type);");
    sw.println("}");
    sw.println("@Override public boolean isValueType(Class<?> type) {");
    sw.indentln("return valueProxyTypes.contains(type);");
    sw.println("}");
  }
}
