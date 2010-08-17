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

import com.google.gwt.app.place.PlaceHistoryHandlerWithFactory;
import com.google.gwt.app.place.PlaceTokenizer;
import com.google.gwt.app.place.Prefix;
import com.google.gwt.app.place.WithTokenizers;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

class PlaceHistoryGeneratorContext {
  static PlaceHistoryGeneratorContext create(TreeLogger logger,
      GeneratorContext generatorContext, String interfaceName)
      throws UnableToCompleteException {
    TypeOracle typeOracle = generatorContext.getTypeOracle();
    JClassType stringType = requireType(typeOracle, String.class);
    JClassType placeTokenizerType = requireType(typeOracle,
        PlaceTokenizer.class);
    JClassType placeHistoryHandlerWithFactoryType = requireType(typeOracle,
        PlaceHistoryHandlerWithFactory.class);

    JClassType factoryType;

    JClassType interfaceType = typeOracle.findType(interfaceName);
    if (interfaceType == null) {
      logger.log(TreeLogger.ERROR, "Could not find requested typeName: "
          + interfaceName);
      throw new UnableToCompleteException();
    }

    if (interfaceType.isInterface() == null) {
      logger.log(TreeLogger.ERROR, interfaceType.getQualifiedSourceName()
          + " is not an interface.", null);
      throw new UnableToCompleteException();
    }

    factoryType = findFactoryType(placeHistoryHandlerWithFactoryType,
        interfaceType);

    String implName = interfaceType.getName().replace(".", "_") + "Impl";

    return new PlaceHistoryGeneratorContext(logger, generatorContext,
        interfaceType, factoryType, stringType, placeTokenizerType,
        placeHistoryHandlerWithFactoryType,
        interfaceType.getPackage().getName(), implName);
  }

  private static JClassType findFactoryType(
      JClassType placeHistoryHandlerWithFactoryType, JClassType interfaceType) {
    JClassType superInterfaces[] = interfaceType.getImplementedInterfaces();

    for (JClassType superInterface : superInterfaces) {
      JParameterizedType parameterizedType = superInterface.isParameterized();
      if (parameterizedType != null
          && parameterizedType.getBaseType().equals(
              placeHistoryHandlerWithFactoryType)) {
        return parameterizedType.getTypeArgs()[0];
      }
    }

    return null;
  }

  private static JClassType requireType(TypeOracle typeOracle, Class<?> clazz) {
    try {
      return typeOracle.getType(clazz.getName());
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  final JClassType stringType;

  final JClassType placeTokenizerType;
  final JClassType placeHistoryHandlerType;

  final TreeLogger logger;
  final GeneratorContext generatorContext;
  final JClassType interfaceType;
  final JClassType factoryType;

  final String implName;

  final String packageName;

  /**
   * All factory getters that can provide tokenizers, by prefix.
   */
  private LinkedHashMap<String, JMethod> tokenizerGetters;

  /**
   * All tokenizer types that must be GWT.create()d, by prefix.
   */
  private LinkedHashMap<String, JClassType> tokenizersWithNoGetters;

  /**
   * Cache of all tokenizer types, union of the entries in tokenizerGetters and
   * tokenizersWithNoGetters.
   */
  private LinkedHashMap<String, JClassType> tokenizerTypes;

  PlaceHistoryGeneratorContext(TreeLogger logger,
      GeneratorContext generatorContext, JClassType interfaceType,
      JClassType factoryType, JClassType stringType,
      JClassType placeTokenizerType, JClassType placeHistoryHandlerType,
      String packageName, String implName) {
    this.logger = logger;
    this.generatorContext = generatorContext;
    this.interfaceType = interfaceType;
    this.factoryType = factoryType;
    this.stringType = stringType;
    this.placeTokenizerType = placeTokenizerType;
    this.placeHistoryHandlerType = placeHistoryHandlerType;
    this.packageName = packageName;
    this.implName = implName;
  }

  public JClassType getPlaceType(String prefix)
      throws UnableToCompleteException {
    return getPlaceTypeForTokenizerType(getTokenizerType(prefix));
  }

  public Set<String> getPrefixes() throws UnableToCompleteException {
    return getTokenizerTypes().keySet();
  }

  public JMethod getTokenizerGetter(String prefix)
      throws UnableToCompleteException {
    return getTokenizerGetters().get(prefix);
  }

  public JClassType getTokenizerType(String prefix)
      throws UnableToCompleteException {

    JMethod getter = getTokenizerGetters().get(prefix);
    if (getter != null) {
      return getter.getReturnType().isClassOrInterface();
    }

    return getTokenizersWihoutGetters().get(prefix);
  }

  public Map<String, JClassType> getTokenizerTypes()
      throws UnableToCompleteException {
    if (tokenizerTypes == null) {
      tokenizerTypes = new LinkedHashMap<String, JClassType>();
      for (Entry<String, JMethod> entry : getTokenizerGetters().entrySet()) {
        tokenizerTypes.put(entry.getKey(),
            entry.getValue().getReturnType().isClassOrInterface());
      }
      for (Entry<String, JClassType> entry : getTokenizersWihoutGetters().entrySet()) {
        tokenizerTypes.put(entry.getKey(), entry.getValue());
      }
    }
    return tokenizerTypes;
  }

  public boolean hasNonFactoryTokenizer() throws UnableToCompleteException {
    return !getTokenizersWihoutGetters().isEmpty();
  }

  private JClassType getPlaceTypeForTokenizerType(JClassType tokenizerType)
      throws UnableToCompleteException {

    List<JClassType> implementedInterfaces = new ArrayList<JClassType>();

    JClassType isInterface = tokenizerType.isInterface();
    if (isInterface != null) {
      implementedInterfaces.add(isInterface);
    }

    implementedInterfaces.addAll(Arrays.asList(tokenizerType.getImplementedInterfaces()));

    JClassType rtn = placeTypeForInterfaces(implementedInterfaces);
    if (rtn == null) {
      logger.log(TreeLogger.ERROR, "Found no Place type for "
          + tokenizerType.getQualifiedSourceName());
      throw new UnableToCompleteException();
    }

    return rtn;
  }

  private String getPrefixForTokenizerGetter(JMethod method)
      throws UnableToCompleteException {
    Prefix annotation = method.getAnnotation(Prefix.class);
    if (annotation != null) {
      return annotation.value();
    }

    JClassType returnType = method.getReturnType().isClassOrInterface();
    return getPrefixForTokenizerType(returnType);
  }

  private String getPrefixForTokenizerType(JClassType returnType)
      throws UnableToCompleteException {
    Prefix annotation;
    annotation = returnType.getAnnotation(Prefix.class);
    if (annotation != null) {
      return annotation.value();
    }

    return getPlaceTypeForTokenizerType(returnType).getName();
  }

  private Map<String, JMethod> getTokenizerGetters()
      throws UnableToCompleteException {
    if (factoryType == null) {
      return Collections.emptyMap();
    }

    if (tokenizerGetters == null) {
      tokenizerGetters = new LinkedHashMap<String, JMethod>();

      /* Gets inherited methods, but not final ones */
      JMethod[] overridableMethods = factoryType.getOverridableMethods();
      /* So we pick up the finals here */
      JMethod[] methods = factoryType.getMethods();

      LinkedHashSet<JMethod> allMethods = new LinkedHashSet<JMethod>();
      allMethods.addAll(Arrays.asList(overridableMethods));
      for (JMethod method : methods) {
        if (method.isPublic()) {
          allMethods.add(method);
        }
      }

      for (JMethod method : allMethods) {
        if (method.getParameters().length > 0) {
          continue;
        }

        JClassType returnType = method.getReturnType().isClassOrInterface();

        if (returnType == null) {
          continue;
        }

        if (!placeTokenizerType.isAssignableFrom(returnType)) {
          continue;
        }

        String prefix = getPrefixForTokenizerGetter(method);
        if (tokenizerGetters.containsKey(prefix)) {
          logger.log(TreeLogger.ERROR, String.format(
              "Found duplicate place prefix \"%s\" in factory type %s, "
                  + "used by both %s and %s", prefix,
              factoryType.getQualifiedSourceName(),
              tokenizerGetters.get(prefix).getName(), method.getName()));
          throw new UnableToCompleteException();
        }
        tokenizerGetters.put(prefix, method);
      }
    }

    return tokenizerGetters;
  }

  private HashMap<String, JClassType> getTokenizersWihoutGetters()
      throws UnableToCompleteException {
    if (tokenizersWithNoGetters == null) {
      tokenizersWithNoGetters = new LinkedHashMap<String, JClassType>();

      for (JClassType tokenizerType : getWithTokenizerEntries()) {
        tokenizersWithNoGetters.put(getPrefixForTokenizerType(tokenizerType),
            tokenizerType);
      }
    }

    return tokenizersWithNoGetters;
  }

  private Set<JClassType> getWithTokenizerEntries() {
    WithTokenizers annotation = interfaceType.getAnnotation(WithTokenizers.class);
    if (annotation == null) {
      return Collections.emptySet();
    }

    LinkedHashSet<JClassType> rtn = new LinkedHashSet<JClassType>();
    for (Class<? extends PlaceTokenizer<?>> tokenizerClass : annotation.value()) {
      JClassType tokenizerType = generatorContext.getTypeOracle().findType(
          tokenizerClass.getCanonicalName());
      if (tokenizerType == null) {
        logger.log(TreeLogger.ERROR, String.format(
            "Error processing @%s, cannot find type %s",
            WithTokenizers.class.getSimpleName(),
            tokenizerClass.getCanonicalName()));
      }
      rtn.add(tokenizerType);
    }

    return rtn;
  }

  private JClassType placeTypeForInterfaces(Collection<JClassType> interfaces) {
    JClassType rtn = null;
    for (JClassType i : interfaces) {
      JParameterizedType parameterizedType = i.isParameterized();
      if (parameterizedType != null
          && placeTokenizerType.equals(parameterizedType.getBaseType())) {
        rtn = parameterizedType.getTypeArgs()[0];
      }
    }
    return rtn;
  }
}