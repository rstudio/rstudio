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
package com.google.web.bindery.requestfactory.gwt.rebind;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.rebind.model.ModelUtils;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.web.bindery.autobean.gwt.rebind.model.JBeanMethod;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanFactory.Category;
import com.google.web.bindery.autobean.shared.AutoBeanFactory.NoWrap;
import com.google.web.bindery.autobean.shared.impl.EnumMap.ExtraEnums;
import com.google.web.bindery.requestfactory.gwt.client.impl.AbstractClientRequestFactory;
import com.google.web.bindery.requestfactory.gwt.rebind.model.AcceptsModelVisitor;
import com.google.web.bindery.requestfactory.gwt.rebind.model.ContextMethod;
import com.google.web.bindery.requestfactory.gwt.rebind.model.EntityProxyModel;
import com.google.web.bindery.requestfactory.gwt.rebind.model.EntityProxyModel.Type;
import com.google.web.bindery.requestfactory.gwt.rebind.model.HasExtraTypes;
import com.google.web.bindery.requestfactory.gwt.rebind.model.ModelVisitor;
import com.google.web.bindery.requestfactory.gwt.rebind.model.RequestFactoryModel;
import com.google.web.bindery.requestfactory.gwt.rebind.model.RequestMethod;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.JsonRpcContent;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequest;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequestContext;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequestContext.Dialect;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequestFactory;
import com.google.web.bindery.requestfactory.shared.impl.BaseProxyCategory;
import com.google.web.bindery.requestfactory.shared.impl.EntityProxyCategory;
import com.google.web.bindery.requestfactory.shared.impl.RequestData;
import com.google.web.bindery.requestfactory.shared.impl.ValueProxyCategory;
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Generates implementations of
 * {@link com.google.web.bindery.requestfactory.shared.RequestFactory
 * RequestFactory} and its nested interfaces.
 */
public class RequestFactoryGenerator extends Generator {

  /**
   * Visits all types reachable from a RequestContext.
   */
  private static class AllReachableTypesVisitor extends RequestMethodTypesVisitor {
    private final RequestFactoryModel model;

    public AllReachableTypesVisitor(RequestFactoryModel model) {
      this.model = model;
    }

    @Override
    public boolean visit(ContextMethod x) {
      visitExtraTypes(x);
      return true;
    }

    @Override
    public boolean visit(EntityProxyModel x) {
      visitExtraTypes(x);
      return true;
    }

    @Override
    public boolean visit(RequestFactoryModel x) {
      visitExtraTypes(x);
      return true;
    }

    @Override
    void examineTypeOnce(JClassType type) {
      // Need this to handle List<Foo>, Map<Foo>
      JParameterizedType parameterized = type.isParameterized();
      if (parameterized != null) {
        for (JClassType arg : parameterized.getTypeArgs()) {
          maybeVisit(arg);
        }
      }
      JClassType base = ModelUtils.ensureBaseType(type);
      EntityProxyModel peer = model.getPeer(base);
      if (peer == null) {
        return;
      }
      peer.accept(this);
    }

    void visitExtraTypes(HasExtraTypes x) {
      if (x.getExtraTypes() != null) {
        for (EntityProxyModel extra : x.getExtraTypes()) {
          extra.accept(this);
        }
      }
    }
  }

  /**
   * Visits all types immediately referenced by methods defined in a
   * RequestContext.
   */
  private abstract static class RequestMethodTypesVisitor extends ModelVisitor {
    private final Set<JClassType> seen = new HashSet<JClassType>();

    @Override
    public void endVisit(RequestMethod x) {
      // Request<Foo> -> Foo
      maybeVisit(x.getDataType());
      // InstanceRequest<Proxy, Foo> -> Proxy
      if (x.getInstanceType() != null) {
        x.getInstanceType().accept(this);
      }
      // Request<Void> doSomething(Foo foo, Bar bar) -> Foo, Bar
      for (JType param : x.getDeclarationMethod().getParameterTypes()) {
        maybeVisit(param.isClassOrInterface());
      }
      // setFoo(Foo foo) -> Foo
      for (JMethod method : x.getExtraSetters()) {
        maybeVisit(method.getParameterTypes()[0].isClassOrInterface());
      }
    }

    abstract void examineTypeOnce(JClassType type);

    void maybeVisit(JClassType type) {
      if (type == null) {
        return;
      } else if (!seen.add(type)) {
        // Short-circuit to prevent type-loops
        return;
      }
      examineTypeOnce(type);
    }
  }

  private GeneratorContext context;
  private TreeLogger logger;
  private RequestFactoryModel model;

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
    String simpleSourceName = toGenerate.getName().replace('.', '_') + "Impl";
    PrintWriter pw = context.tryCreate(logger, packageName, simpleSourceName);
    if (pw == null) {
      return packageName + "." + simpleSourceName;
    }

    model = new RequestFactoryModel(logger, toGenerate);

    ClassSourceFileComposerFactory factory =
        new ClassSourceFileComposerFactory(packageName, simpleSourceName);
    factory.setSuperclass(AbstractClientRequestFactory.class.getCanonicalName());
    factory.addImplementedInterface(typeName);
    SourceWriter sw = factory.createSourceWriter(context, pw);
    writeAutoBeanFactory(sw, model.getAllProxyModels(), findExtraEnums(model));
    writeContextMethods(sw);
    writeContextImplementations();
    writeTypeMap(sw);
    sw.commit(logger);

    return factory.getCreatedClassName();
  }

  /**
   * Find enums that needed to be added to the EnumMap that are not referenced
   * by any of the proxies. This is necessary because the RequestFactory depends
   * on the AutoBeanCodex to serialize enum values, which in turn depends on the
   * AutoBeanFactory's enum map. That enum map only contains enum types
   * reachable from the AutoBean interfaces, which could lead to method
   * parameters being un-encodable.
   */
  private Set<JEnumType> findExtraEnums(AcceptsModelVisitor method) {
    final Set<JEnumType> toReturn = new LinkedHashSet<JEnumType>();
    final Set<JEnumType> referenced = new HashSet<JEnumType>();

    // Called from the adder visitor below on each EntityProxy seen
    final ModelVisitor remover = new AllReachableTypesVisitor(model) {
      @Override
      void examineTypeOnce(JClassType type) {
        JEnumType asEnum = type.isEnum();
        if (asEnum != null) {
          referenced.add(asEnum);
        }
        super.examineTypeOnce(type);
      }
    };

    // Add enums used by RequestMethods
    method.accept(new RequestMethodTypesVisitor() {
      @Override
      public boolean visit(EntityProxyModel x) {
        x.accept(remover);
        return false;
      }

      @Override
      void examineTypeOnce(JClassType type) {
        JEnumType asEnum = type.isEnum();
        if (asEnum != null) {
          toReturn.add(asEnum);
        }
      }
    });
    toReturn.removeAll(referenced);
    if (toReturn.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(toReturn);
  }

  /**
   * Find all EntityProxyModels reachable from a given ContextMethod.
   */
  private Set<EntityProxyModel> findReferencedEntities(ContextMethod method) {
    final Set<EntityProxyModel> models = new LinkedHashSet<EntityProxyModel>();
    method.accept(new AllReachableTypesVisitor(model) {
      @Override
      public void endVisit(EntityProxyModel x) {
        models.add(x);
        models.addAll(x.getSuperProxyTypes());
      }
    });
    return models;
  }

  private void writeAutoBeanFactory(SourceWriter sw, Collection<EntityProxyModel> models,
      Collection<JEnumType> extraEnums) {
    if (!extraEnums.isEmpty()) {
      StringBuilder extraClasses = new StringBuilder();
      for (JEnumType enumType : extraEnums) {
        if (extraClasses.length() > 0) {
          extraClasses.append(",");
        }
        extraClasses.append(enumType.getQualifiedSourceName()).append(".class");
      }
      sw.println("@%s({%s})", ExtraEnums.class.getCanonicalName(), extraClasses);
    }
    // Map in static implementations of EntityProxy methods
    sw.println("@%s({%s.class, %s.class, %s.class})", Category.class.getCanonicalName(),
        EntityProxyCategory.class.getCanonicalName(), ValueProxyCategory.class.getCanonicalName(),
        BaseProxyCategory.class.getCanonicalName());
    // Don't wrap our id type, because it makes code grungy
    sw.println("@%s(%s.class)", NoWrap.class.getCanonicalName(), EntityProxyId.class
        .getCanonicalName());
    sw.println("interface Factory extends %s {", AutoBeanFactory.class.getCanonicalName());
    sw.indent();

    for (EntityProxyModel proxy : models) {
      // AutoBean<FooProxy> com_google_FooProxy();
      sw.println("%s<%s> %s();", AutoBean.class.getCanonicalName(), proxy.getQualifiedSourceName(),
          proxy.getQualifiedSourceName().replace('.', '_'));
    }
    sw.outdent();
    sw.println("}");

    // public static final Factory FACTORY = GWT.create(Factory.class);
    sw.println("public static Factory FACTORY;", GWT.class.getCanonicalName());

    // Write public accessor
    sw.println("@Override public Factory getAutoBeanFactory() {");
    sw.indent();
    sw.println("if (FACTORY == null) {");
    sw.indentln("FACTORY = %s.create(Factory.class);", GWT.class.getCanonicalName());
    sw.println("}");
    sw.println("return FACTORY;");
    sw.outdent();
    sw.println("}");
  }

  private void writeContextImplementations() {
    for (ContextMethod method : model.getMethods()) {
      PrintWriter pw =
          context.tryCreate(logger, method.getPackageName(), method.getSimpleSourceName());
      if (pw == null) {
        // Already generated
        continue;
      }

      ClassSourceFileComposerFactory factory =
          new ClassSourceFileComposerFactory(method.getPackageName(), method.getSimpleSourceName());
      factory.setSuperclass(AbstractRequestContext.class.getCanonicalName());
      factory.addImplementedInterface(method.getImplementedInterfaceQualifiedSourceName());
      SourceWriter sw = factory.createSourceWriter(context, pw);

      // Constructor that accepts the parent RequestFactory
      sw.println("public %s(%s requestFactory) {super(requestFactory, %s.%s);}", method
          .getSimpleSourceName(), AbstractRequestFactory.class.getCanonicalName(), Dialect.class
          .getCanonicalName(), method.getDialect().name());

      Set<EntityProxyModel> models = findReferencedEntities(method);
      Set<JEnumType> extraEnumTypes = findExtraEnums(method);
      writeAutoBeanFactory(sw, models, extraEnumTypes);

      // Write each Request method
      for (RequestMethod request : method.getRequestMethods()) {
        JMethod jmethod = request.getDeclarationMethod();
        String operation = request.getOperation();

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
          typeParameterDeclaration.append(",").append(param.getQualifiedSourceName());
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
        sw.println("public %s %s %s(%s) {", typeParameterDeclaration, jmethod.getReturnType()
            .getParameterizedQualifiedSourceName(), jmethod.getName(), parameterDeclaration);
        sw.indent();
        // The implements clause covers InstanceRequest
        // class X extends AbstractRequest<Return> implements Request<Return> {
        sw.println("class X extends %s<%s> implements %s {", AbstractRequest.class
            .getCanonicalName(), request.getDataType().getParameterizedQualifiedSourceName(),
            jmethod.getReturnType().getParameterizedQualifiedSourceName());
        sw.indent();

        // public X() { super(FooRequestContext.this); }
        sw.println("public X() { super(%s.this);}", method.getSimpleSourceName());

        // This could also be gotten rid of by having only Request /
        // InstanceRequest
        sw.println("@Override public X with(String... paths) {super.with(paths); return this;}");

        // makeRequestData()
        sw.println("@Override protected %s makeRequestData() {", RequestData.class
            .getCanonicalName());
        String elementType =
            request.isCollectionType() ? request.getCollectionElementType()
                .getQualifiedSourceName()
                + ".class" : "null";
        String returnTypeBaseQualifiedName =
            ModelUtils.ensureBaseType(request.getDataType()).getQualifiedSourceName();
        // return new RequestData("ABC123", {parameters}, propertyRefs,
        // List.class, FooProxy.class);
        sw.indentln("return new %s(\"%s\", new Object[] {%s}, propertyRefs, %s.class, %s);",
            RequestData.class.getCanonicalName(), operation, parameterArray,
            returnTypeBaseQualifiedName, elementType);
        sw.println("}");

        /*
         * Only support extra properties in JSON-RPC payloads. Could add this to
         * standard requests to provide out-of-band data.
         */
        if (method.getDialect().equals(Dialect.JSON_RPC)) {
          for (JMethod setter : request.getExtraSetters()) {
            PropertyName propertyNameAnnotation = setter.getAnnotation(PropertyName.class);
            String propertyName =
                propertyNameAnnotation == null ? JBeanMethod.SET.inferName(setter)
                    : propertyNameAnnotation.value();
            String maybeReturn = JBeanMethod.SET_BUILDER.matches(setter) ? "return this;" : "";
            sw.println("%s { getRequestData().setNamedParameter(\"%s\", %s); %s}", setter
                .getReadableDeclaration(false, false, false, false, true), propertyName, setter
                .getParameters()[0].getName(), maybeReturn);
          }
        }

        // end class X{}
        sw.outdent();
        sw.println("}");

        // Instantiate, enqueue, and return
        sw.println("X x = new X();");

        if (request.getApiVersion() != null) {
          sw.println("x.getRequestData().setApiVersion(\"%s\");", Generator.escape(request
              .getApiVersion()));
        }

        // JSON-RPC payloads send their parameters in a by-name fashion
        if (method.getDialect().equals(Dialect.JSON_RPC)) {
          for (JParameter param : jmethod.getParameters()) {
            PropertyName annotation = param.getAnnotation(PropertyName.class);
            String propertyName = annotation == null ? param.getName() : annotation.value();
            boolean isContent = param.isAnnotationPresent(JsonRpcContent.class);
            if (isContent) {
              sw.println("x.getRequestData().setRequestContent(%s);", param.getName());
            } else {
              sw.println("x.getRequestData().setNamedParameter(\"%s\", %s);", propertyName, param
                  .getName());
            }
          }
        }

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
      sw.println("public %s %s() {", method.getQualifiedSourceName(), method.getMethodName());
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
    sw.println("private static final %1$s<Class<?>> entityProxyTypes = new %1$s<Class<?>>();",
        HashSet.class.getCanonicalName());
    sw.println("private static final %1$s<Class<?>> valueProxyTypes = new %1$s<Class<?>>();",
        HashSet.class.getCanonicalName());
    sw.println("static {");
    sw.indent();
    for (EntityProxyModel type : model.getAllProxyModels()) {
      // tokensToTypes.put("Foo", Foo.class);
      sw.println("tokensToTypes.put(\"%s\", %s.class);", OperationKey.hash(type
          .getQualifiedBinaryName()), type.getQualifiedSourceName());
      // typesToTokens.put(Foo.class, Foo);
      sw.println("typesToTokens.put(%s.class, \"%s\");", type.getQualifiedSourceName(),
          OperationKey.hash(type.getQualifiedBinaryName()));
      // fooProxyTypes.add(MyFooProxy.class);
      sw.println("%s.add(%s.class);", type.getType().equals(Type.ENTITY) ? "entityProxyTypes"
          : "valueProxyTypes", type.getQualifiedSourceName());
    }
    sw.outdent();
    sw.println("}");

    // Write instance methods
    sw.println("@Override public String getFactoryTypeToken() {");
    sw.indentln("return \"%s\";", model.getFactoryType().getQualifiedBinaryName());
    sw.println("}");
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
