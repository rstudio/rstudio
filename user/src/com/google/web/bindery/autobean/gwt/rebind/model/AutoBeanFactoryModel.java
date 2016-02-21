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
package com.google.web.bindery.autobean.gwt.rebind.model;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.editor.rebind.model.ModelUtils;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanFactory.Category;
import com.google.web.bindery.autobean.shared.AutoBeanFactory.NoWrap;
import com.google.web.bindery.autobean.shared.impl.EnumMap.ExtraEnums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class AutoBeanFactoryModel {
  private static final JType[] EMPTY_JTYPE = new JType[0];

  private final JGenericType autoBeanInterface;
  private final JClassType autoBeanFactoryInterface;
  private final Map<JEnumConstant, String> allEnumConstants = new LinkedHashMap<JEnumConstant, String>();
  private final List<JClassType> categoryTypes;
  private final List<JClassType> noWrapTypes;
  private final TreeLogger logger;
  private final List<AutoBeanFactoryMethod> methods = new ArrayList<AutoBeanFactoryMethod>();
  private final List<JMethod> objectMethods;
  private final TypeOracle oracle;
  private final Map<JClassType, AutoBeanType> peers = new LinkedHashMap<JClassType, AutoBeanType>();
  private boolean poisoned;

  /**
   * Accumulates bean types that are reachable through the type graph.
   */
  private Set<JClassType> toCalculate = new LinkedHashSet<JClassType>();

  public AutoBeanFactoryModel(TreeLogger logger, JClassType factoryType)
      throws UnableToCompleteException {
    this.logger = logger;
    oracle = factoryType.getOracle();
    autoBeanInterface = oracle.findType(AutoBean.class.getCanonicalName()).isGenericType();
    autoBeanFactoryInterface = oracle.findType(
        AutoBeanFactory.class.getCanonicalName()).isInterface();

    /*
     * We want to allow the user to override some of the useful Object methods,
     * so we'll extract them here.
     */
    JClassType objectType = oracle.getJavaLangObject();
    objectMethods = Arrays.asList(
        objectType.findMethod("equals", new JType[] {objectType}),
        objectType.findMethod("hashCode", EMPTY_JTYPE),
        objectType.findMethod("toString", EMPTY_JTYPE));

    // Process annotations
    {
      Category categoryAnnotation = factoryType.getAnnotation(Category.class);
      if (categoryAnnotation != null) {
        categoryTypes = new ArrayList<JClassType>(
            categoryAnnotation.value().length);
        processClassArrayAnnotation(categoryAnnotation.value(), categoryTypes);
      } else {
        categoryTypes = null;
      }

      noWrapTypes = new ArrayList<JClassType>();
      noWrapTypes.add(oracle.findType(AutoBean.class.getCanonicalName()));
      NoWrap noWrapAnnotation = factoryType.getAnnotation(NoWrap.class);
      if (noWrapAnnotation != null) {
        processClassArrayAnnotation(noWrapAnnotation.value(), noWrapTypes);
      }

      ExtraEnums extraEnumsAnnotation = factoryType.getAnnotation(ExtraEnums.class);
      if (extraEnumsAnnotation != null) {
        for (Class<?> clazz : extraEnumsAnnotation.value()) {
          JEnumType asEnum = oracle.findType(clazz.getCanonicalName()).isEnum();
          assert asEnum != null;
          for (JEnumConstant value : asEnum.getEnumConstants()) {
            allEnumConstants.put(value, AutoBeanMethod.getEnumName(value));
          }
        }
      }
    }

    for (JMethod method : factoryType.getOverridableMethods()) {
      if (method.getEnclosingType().equals(autoBeanFactoryInterface)) {
        // Ignore methods in AutoBeanFactory
        continue;
      }

      JClassType returnType = method.getReturnType().isInterface();
      if (returnType == null) {
        poison("The return type of method %s is a primitive type",
            method.getName());
        continue;
      }

      // AutoBean<FooIntf> blah() --> beanType = FooIntf
      JClassType beanType = ModelUtils.findParameterizationOf(
          autoBeanInterface, returnType)[0];
      if (beanType.isInterface() == null) {
        poison("The %s parameterization is not an interface",
            beanType.getQualifiedSourceName());
        continue;
      }

      // AutoBean<FooIntf> blah(FooIntfSub foo) --> toWrap = FooIntfSub
      JClassType toWrap;
      if (method.getParameters().length == 0) {
        toWrap = null;
      } else if (method.getParameters().length == 1) {
        toWrap = method.getParameters()[0].getType().isClassOrInterface();
        if (!beanType.isAssignableFrom(toWrap)) {
          poison(
              "The %s parameterization %s is not assignable from the delegate"
                  + " type %s", autoBeanInterface.getSimpleSourceName(),
              toWrap.getQualifiedSourceName());
          continue;
        }
      } else {
        poison("Unexpected parameters in method %s", method.getName());
        continue;
      }

      AutoBeanType autoBeanType = getAutoBeanType(beanType);

      // Must wrap things that aren't simple interfaces
      if (!autoBeanType.isSimpleBean() && toWrap == null) {
        if (categoryTypes != null) {
          poison("The %s parameterization is not simple and the following"
              + " methods did not have static implementations:",
              beanType.getQualifiedSourceName());
          for (AutoBeanMethod missing : autoBeanType.getMethods()) {
            if (missing.getAction().equals(JBeanMethod.CALL)
                && missing.getStaticImpl() == null) {
              poison(missing.getMethod().getReadableDeclaration());
            }
          }
        } else {
          poison("The %s parameterization is not simple, but the %s method"
              + " does not provide a delegate",
              beanType.getQualifiedSourceName(), method.getName());
        }
        continue;
      }

      AutoBeanFactoryMethod.Builder builder = new AutoBeanFactoryMethod.Builder();
      builder.setAutoBeanType(autoBeanType);
      builder.setMethod(method);
      methods.add(builder.build());
    }

    while (!toCalculate.isEmpty()) {
      Set<JClassType> examine = toCalculate;
      toCalculate = new LinkedHashSet<JClassType>();
      for (JClassType beanType : examine) {
        getAutoBeanType(beanType);
      }
    }

    if (poisoned) {
      die("Unable to complete due to previous errors");
    }
  }

  public Collection<AutoBeanType> getAllTypes() {
    return Collections.unmodifiableCollection(peers.values());
  }

  public List<JClassType> getCategoryTypes() {
    return categoryTypes;
  }

  public Map<JEnumConstant, String> getEnumTokenMap() {
    return Collections.unmodifiableMap(allEnumConstants);
  }

  public List<AutoBeanFactoryMethod> getMethods() {
    return Collections.unmodifiableList(methods);
  }

  public AutoBeanType getPeer(JClassType beanType) {
    beanType = ModelUtils.ensureBaseType(beanType);
    return peers.get(beanType);
  }

  private List<AutoBeanMethod> computeMethods(JClassType beanType) {
    List<JMethod> toExamine = new ArrayList<JMethod>();
    toExamine.addAll(Arrays.asList(beanType.getInheritableMethods()));
    toExamine.addAll(objectMethods);
    List<AutoBeanMethod> toReturn = new ArrayList<AutoBeanMethod>(
        toExamine.size());
    for (JMethod method : toExamine) {
      if (method.isPrivate()) {
        // Ignore private methods
        continue;
      }
      AutoBeanMethod.Builder builder = new AutoBeanMethod.Builder();
      builder.setMethod(method);

      // See if this method shouldn't have its return type wrapped
      // TODO: Allow class return types?
      JClassType classReturn = method.getReturnType().isInterface();
      if (classReturn != null) {
        maybeCalculate(classReturn);
        if (noWrapTypes != null) {
          for (JClassType noWrap : noWrapTypes) {
            if (noWrap.isAssignableFrom(classReturn)) {
              builder.setNoWrap(true);
              break;
            }
          }
        }
      }

      // GET, SET, or CALL
      JBeanMethod action = JBeanMethod.which(method);
      builder.setAction(action);
      if (JBeanMethod.CALL.equals(action)) {
        JMethod staticImpl = findStaticImpl(beanType, method);
        if (staticImpl == null && objectMethods.contains(method)) {
          // Don't complain about lack of implementation for Object methods
          continue;
        }
        builder.setStaticImp(staticImpl);
      }

      AutoBeanMethod toAdd = builder.build();

      // Collect referenced enums
      if (toAdd.hasEnumMap()) {
        allEnumConstants.putAll(toAdd.getEnumMap());
      }

      // See if parameterizations will pull in more types
      if (toAdd.isCollection()) {
        maybeCalculate(toAdd.getElementType());
      } else if (toAdd.isMap()) {
        maybeCalculate(toAdd.getKeyType());
        maybeCalculate(toAdd.getValueType());
      }

      toReturn.add(toAdd);
    }
    return toReturn;
  }

  private void die(String message) throws UnableToCompleteException {
    poison(message);
    throw new UnableToCompleteException();
  }

  /**
   * Find <code>Object __intercept(AutoBean&lt;?> bean, Object value);</code> in
   * the category types.
   */
  private JMethod findInterceptor(JClassType beanType) {
    if (categoryTypes == null) {
      return null;
    }
    for (JClassType category : categoryTypes) {
      for (JMethod method : category.getOverloads("__intercept")) {
        // Ignore non-static, non-public methods
        // TODO: Implement visibleFrom() to allow package-protected categories
        if (!method.isStatic() || !method.isPublic()) {
          continue;
        }

        JParameter[] params = method.getParameters();
        if (params.length != 2) {
          continue;
        }
        if (!methodAcceptsAutoBeanAsFirstParam(beanType, method)) {
          continue;
        }
        JClassType value = params[1].getType().isClassOrInterface();
        if (value == null) {
          continue;
        }
        if (!oracle.getJavaLangObject().isAssignableTo(value)) {
          continue;
        }
        return method;
      }
    }
    return null;
  }

  /**
   * Search the category types for a static implementation of an interface
   * method. Given the interface method declaration:
   *
   * <pre>
   * Foo bar(Baz baz);
   * </pre>
   *
   * this will search the types in {@link #categoryTypes} for the following
   * method:
   *
   * <pre>
   * public static Foo bar(AutoBean&lt;Intf> bean, Baz baz) {}
   * </pre>
   */
  private JMethod findStaticImpl(JClassType beanType, JMethod method) {
    if (categoryTypes == null) {
      return null;
    }

    for (JClassType category : categoryTypes) {
      // One extra argument for the AutoBean
      JParameter[] methodParams = method.getParameters();
      int requiredArgs = methodParams.length + 1;
      overload : for (JMethod overload : category.getOverloads(method.getName())) {
        if (!overload.isStatic() || !overload.isPublic()) {
          // Ignore non-static, non-public methods
          continue;
        }

        JParameter[] overloadParams = overload.getParameters();
        if (overloadParams.length != requiredArgs) {
          continue;
        }

        if (!methodAcceptsAutoBeanAsFirstParam(beanType, overload)) {
          // Ignore if the first parameter is a primitive or not assignable
          continue;
        }

        // Match the rest of the parameters
        for (int i = 1; i < requiredArgs; i++) {
          JType methodType = methodParams[i - 1].getType();
          JType overloadType = overloadParams[i].getType();
          if (methodType.equals(overloadType)) {
            // Match; exact, the usual case
          } else if (methodType.isClassOrInterface() != null
              && overloadType.isClassOrInterface() != null
              && methodType.isClassOrInterface().isAssignableTo(
                  overloadType.isClassOrInterface())) {
            // Match; assignment-compatible
          } else {
            // No match, keep looking
            continue overload;
          }
        }
        return overload;
      }
    }
    return null;
  }

  private AutoBeanType getAutoBeanType(JClassType beanType) {
    beanType = ModelUtils.ensureBaseType(beanType);
    AutoBeanType toReturn = peers.get(beanType);
    if (toReturn == null) {
      AutoBeanType.Builder builder = new AutoBeanType.Builder();
      builder.setOwnerFactory(this);
      builder.setPeerType(beanType);
      builder.setMethods(computeMethods(beanType));
      builder.setInterceptor(findInterceptor(beanType));
      if (noWrapTypes != null) {
        for (JClassType noWrap : noWrapTypes) {
          if (noWrap.isAssignableFrom(beanType)) {
            builder.setNoWrap(true);
            break;
          }
        }
      }
      toReturn = builder.build();
      peers.put(beanType, toReturn);
    }
    return toReturn;
  }

  /**
   * Enqueue a type in {@link #toCalculate} if {@link #peers} does not already
   * contain an entry.
   */
  private void maybeCalculate(JClassType type) {
    if (type.isInterface() == null || ModelUtils.isValueType(oracle, type)) {
      return;
    }
    if (!peers.containsKey(type)) {
      toCalculate.add(type);
    }
  }

  private boolean methodAcceptsAutoBeanAsFirstParam(JClassType beanType,
      JMethod method) {
    JParameter[] params = method.getParameters();
    if (params.length == 0) {
      return false;
    }
    JClassType paramAsClass = params[0].getType().isClassOrInterface();

    // First parameter is a primitive
    if (paramAsClass == null) {
      return false;
    }

    // Check using base types to account for erasure semantics
    JParameterizedType expectedFirst = oracle.getParameterizedType(
        autoBeanInterface,
        new JClassType[] {ModelUtils.ensureBaseType(beanType)});
    return expectedFirst.isAssignableTo(paramAsClass);
  }

  private void poison(String message, Object... args) {
    logger.log(TreeLogger.ERROR, String.format(message, args));
    poisoned = true;
  }

  private void processClassArrayAnnotation(Class<?>[] classes,
      Collection<JClassType> accumulator) {
    for (Class<?> clazz : classes) {
      JClassType category = oracle.findType(clazz.getCanonicalName());
      if (category == null) {
        poison("Could not find @%s type %s in the TypeOracle",
            Category.class.getSimpleName(), clazz.getCanonicalName());
        continue;
      } else if (!category.isPublic()) {
        poison("Category type %s is not public",
            category.getQualifiedSourceName());
        continue;
      } else if (!category.isStatic() && category.isMemberType()) {
        poison("Category type %s must be static",
            category.getQualifiedSourceName());
        continue;
      }
      accumulator.add(category);
    }
  }
}
