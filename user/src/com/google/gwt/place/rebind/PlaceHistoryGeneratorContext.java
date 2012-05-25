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
package com.google.gwt.place.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.place.shared.PlaceHistoryMapperWithFactory;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import com.google.gwt.place.shared.WithTokenizers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

class PlaceHistoryGeneratorContext {
  /**
   * Creates a {@link PlaceHistoryGeneratorContext} for the given
   * {@link PlaceHistoryMapper} sub-interface.
   * 
   * @return a {@link PlaceHistoryGeneratorContext}, or <code>null</code> if the
   *         generator should not run (i.e. <code>interfaceName</code> is not an
   *         interface)
   * @throws UnableToCompleteException if the type denoted by
   *           <code>interfaceName</code> cannot be found in
   *           <code>typeOracle</code>
   */
  static PlaceHistoryGeneratorContext create(TreeLogger logger,
      TypeOracle typeOracle, String interfaceName)
      throws UnableToCompleteException {
    JClassType stringType = requireType(typeOracle, String.class);
    JClassType placeTokenizerType = requireType(typeOracle,
        PlaceTokenizer.class);
    JClassType placeHistoryMapperWithFactoryType = requireType(typeOracle,
        PlaceHistoryMapperWithFactory.class);

    JClassType factoryType;

    JClassType interfaceType = typeOracle.findType(interfaceName);
    if (interfaceType == null) {
      logger.log(TreeLogger.ERROR, "Could not find requested typeName: "
          + interfaceName);
      throw new UnableToCompleteException();
    }

    if (interfaceType.isInterface() == null) {
      return null;
    }

    factoryType = findFactoryType(placeHistoryMapperWithFactoryType,
        interfaceType);

    String implName = interfaceType.getName().replace(".", "_") + "Impl";

    return new PlaceHistoryGeneratorContext(logger, typeOracle, interfaceType,
        factoryType, stringType, placeTokenizerType,
        interfaceType.getPackage().getName(), implName);
  }

  private static JClassType findFactoryType(
      JClassType placeHistoryMapperWithFactoryType, JClassType interfaceType) {
    JClassType superInterfaces[] = interfaceType.getImplementedInterfaces();

    for (JClassType superInterface : superInterfaces) {
      JParameterizedType parameterizedType = superInterface.isParameterized();
      if (parameterizedType != null
          && parameterizedType.getBaseType().equals(
              placeHistoryMapperWithFactoryType)) {
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

  final TreeLogger logger;
  final TypeOracle typeOracle;
  final JClassType interfaceType;
  final JClassType factoryType;

  final String implName;

  final String packageName;

  /**
   * All tokenizers, either as a {@link JMethod} for factory getters or as a
   * {@link JClassType} for types that must be GWT.create()d, by prefix.
   */
  private HashMap<String, Object> tokenizers;

  /**
   * All place types and the prefix of their associated tokenizer, ordered from
   * most-derived to least-derived type (and falling back to the natural
   * ordering of their names).
   */
  private TreeMap<JClassType, String> placeTypes = new TreeMap<JClassType, String>(
      new MostToLeastDerivedPlaceTypeComparator());

  PlaceHistoryGeneratorContext(TreeLogger logger, TypeOracle typeOracle,
      JClassType interfaceType, JClassType factoryType, JClassType stringType,
      JClassType placeTokenizerType, String packageName, String implName) {
    this.logger = logger;
    this.typeOracle = typeOracle;
    this.interfaceType = interfaceType;
    this.factoryType = factoryType;
    this.stringType = stringType;
    this.placeTokenizerType = placeTokenizerType;
    this.packageName = packageName;
    this.implName = implName;
  }

  public Set<JClassType> getPlaceTypes() throws UnableToCompleteException {
    ensureInitialized();
    return placeTypes.keySet();
  }

  public String getPrefix(JClassType placeType)
      throws UnableToCompleteException {
    ensureInitialized();
    return placeTypes.get(placeType);
  }

  public Set<String> getPrefixes() throws UnableToCompleteException {
    ensureInitialized();
    return tokenizers.keySet();
  }

  public JMethod getTokenizerGetter(String prefix)
      throws UnableToCompleteException {
    ensureInitialized();
    Object tokenizerGetter = tokenizers.get(prefix);
    if (tokenizerGetter instanceof JMethod) {
      return (JMethod) tokenizerGetter;
    }
    return null;
  }

  public JClassType getTokenizerType(String prefix)
      throws UnableToCompleteException {
    ensureInitialized();
    Object tokenizerType = tokenizers.get(prefix);
    if (tokenizerType instanceof JClassType) {
      return (JClassType) tokenizerType;
    }
    return null;
  }

  void ensureInitialized() throws UnableToCompleteException {
    if (tokenizers == null) {
      assert placeTypes.isEmpty();
      tokenizers = new HashMap<String, Object>();
      initTokenizerGetters();
      initTokenizersWithoutGetters();
    }
  }

  private void addPlaceTokenizer(Object tokenizerClassOrGetter, String prefix,
      JClassType tokenizerType) throws UnableToCompleteException {
    if (prefix.contains(":")) {
      logger.log(TreeLogger.ERROR, String.format(
          "Found place prefix \"%s\" containing separator char \":\", on %s",
          prefix, getLogMessage(tokenizerClassOrGetter)));
      throw new UnableToCompleteException();
    }
    if (tokenizers.containsKey(prefix)) {
      logger.log(TreeLogger.ERROR, String.format(
          "Found duplicate place prefix \"%s\" on %s, already seen on %s",
          prefix, getLogMessage(tokenizerClassOrGetter),
          getLogMessage(tokenizers.get(prefix))));
      throw new UnableToCompleteException();
    }
    JClassType placeType = getPlaceTypeForTokenizerType(tokenizerType);
    if (placeTypes.containsKey(placeType)) {
      logger.log(
          TreeLogger.ERROR,
          String.format(
              "Found duplicate tokenizer's place type \"%s\" on %s, already seen on %s",
              placeType.getQualifiedSourceName(),
              getLogMessage(tokenizerClassOrGetter),
              getLogMessage(tokenizers.get(placeTypes.get(placeType)))));
      throw new UnableToCompleteException();
    }
    tokenizers.put(prefix, tokenizerClassOrGetter);
    placeTypes.put(placeType, prefix);
  }

  private String getLogMessage(Object methodOrClass) {
    if (methodOrClass instanceof JMethod) {
      JMethod method = (JMethod) methodOrClass;
      return method.getEnclosingType().getQualifiedSourceName() + "#"
          + method.getName() + "()";
    }
    JClassType classType = (JClassType) methodOrClass;
    return classType.getQualifiedSourceName();
  }

  private JClassType getPlaceTypeForTokenizerType(JClassType tokenizerType)
      throws UnableToCompleteException {
    JClassType rtn = placeTypeForInterfaces(tokenizerType.getFlattenedSupertypeHierarchy());
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

  private Set<JClassType> getWithTokenizerEntries() {
    WithTokenizers annotation = interfaceType.getAnnotation(WithTokenizers.class);
    if (annotation == null) {
      return Collections.emptySet();
    }

    LinkedHashSet<JClassType> rtn = new LinkedHashSet<JClassType>();
    for (Class<? extends PlaceTokenizer<?>> tokenizerClass : annotation.value()) {
      JClassType tokenizerType = typeOracle.findType(tokenizerClass.getCanonicalName());
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

  private void initTokenizerGetters() throws UnableToCompleteException {
    if (factoryType != null) {

      // TODO: include non-public methods that are nevertheless accessible
      // to the interface (package-scoped);
      // Add a isCallable(JClassType) method to JAbstractMethod?
      for (JMethod method : factoryType.getInheritableMethods()) {
        if (!method.isPublic()) {
          continue;
        }
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

        addPlaceTokenizer(method, getPrefixForTokenizerGetter(method),
            method.getReturnType().isClassOrInterface());
      }
    }
  }

  private void initTokenizersWithoutGetters() throws UnableToCompleteException {
    for (JClassType tokenizerType : getWithTokenizerEntries()) {
      addPlaceTokenizer(tokenizerType,
          getPrefixForTokenizerType(tokenizerType), tokenizerType);
    }
  }

  private JClassType placeTypeForInterfaces(Collection<? extends JClassType> interfaces) {
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
