/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderContext;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Descriptor for a class which can be used as a @UiField. This is usually a
 * widget, but can also be a resource (such as Messages or an ImageBundle). Also
 * notice that the existence of an OwnerFieldClass doesn't mean the class is
 * actually present as a field in the owner.
 */
public class OwnerFieldClass {
  
  private static final int DEFAULT_COST = 4;
  private static final Map<String, Integer> TYPE_RANK;
  static {
    HashMap<String, Integer> tmpTypeRank = new HashMap<String, Integer>();
    tmpTypeRank.put("java.lang.String", 1);
    tmpTypeRank.put("boolean", 2);
    tmpTypeRank.put("byte", 2);
    tmpTypeRank.put("char", 2);
    tmpTypeRank.put("double", 2);
    tmpTypeRank.put("float", 2);
    tmpTypeRank.put("int", 2);
    tmpTypeRank.put("long", 2);
    tmpTypeRank.put("short", 2);
    tmpTypeRank.put("java.lang.Boolean", 3);
    tmpTypeRank.put("java.lang.Byte", 3);
    tmpTypeRank.put("java.lang.Character", 3);
    tmpTypeRank.put("java.lang.Double", 3);
    tmpTypeRank.put("java.lang.Float", 3);
    tmpTypeRank.put("java.lang.Integer", 3);
    tmpTypeRank.put("java.lang.Long", 3);
    tmpTypeRank.put("java.lang.Short", 3);
    TYPE_RANK = Collections.unmodifiableMap(tmpTypeRank);
  }
  
  /**
   * Gets or creates the descriptor for the given field class.
   *
   * @param forType the field type to get a descriptor for
   * @param logger TODO
   * @param context 
   * @return the descriptor
   */
  public static OwnerFieldClass getFieldClass(JClassType forType,
      MortalLogger logger, UiBinderContext context)
      throws UnableToCompleteException {
    OwnerFieldClass clazz = context.getOwnerFieldClass(forType);
    if (clazz == null) {
      clazz = new OwnerFieldClass(forType, logger);
      context.putOwnerFieldClass(forType, clazz);
    }
    return clazz;
  }

  private Set<String> ambiguousSetters;
  private final MortalLogger logger;
  private final JClassType rawType;
  private final Map<String, JMethod> setters = new HashMap<String, JMethod>();
  /**
   * Mapping from all of the @UiChild tags to their corresponding methods and
   * limits on being called.
   */
  private final Map<String, Pair<JMethod, Integer>> uiChildren = new HashMap<String, Pair<JMethod, Integer>>();

  private JConstructor uiConstructor;
 
  /**
   * Default constructor. This is package-visible for testing only.
   *
   * @param forType the type of the field class
   * @param logger
   * @throws UnableToCompleteException if the class is not valid
   */
  OwnerFieldClass(JClassType forType, MortalLogger logger)
      throws UnableToCompleteException {
    this.rawType = forType;
    this.logger = logger;

    findUiConstructor(forType);
    findSetters(forType);
    findUiChildren(forType);
  }

  /**
   * Returns the field's raw type.
   */
  public JClassType getRawType() {
    return rawType;
  }

  /**
   * Finds the setter method for a given property.
   *
   * @param propertyName the name of the property
   * @return the setter method, or null if none exists
   */
  public JMethod getSetter(String propertyName)
      throws UnableToCompleteException {
   
    if (ambiguousSetters != null && ambiguousSetters.contains(propertyName)) {
      logger.die("Ambiguous setter requested: " + rawType.getName() + "."
          + propertyName);
    }

    return setters.get(propertyName);
  }

  /**
   * Returns a list of methods annotated with @UiChild.
   * 
   * @return a list of all add child methods
   */
  public Map<String, Pair<JMethod, Integer>> getUiChildMethods() {
    return uiChildren;
  }

  /**
   * Returns the constructor annotated with @UiConstructor, or null if none
   * exists.
   */
  public JConstructor getUiConstructor() {
    return uiConstructor;
  }

  /**
   * Adds a setter for a given property to the given map of setters.
   *
   * @param allSetters the map of setters (keyed by property name)
   * @param propertyName the property name to use
   * @param method the setter to use
   */
  private void addSetter(Map<String, Collection<JMethod>> allSetters,
      String propertyName, JMethod method) {
    Collection<JMethod> propertyMethods = allSetters.get(propertyName);
    if (propertyMethods == null) {
      propertyMethods = new ArrayList<JMethod>();
      allSetters.put(propertyName, propertyMethods);
    }

    propertyMethods.add(method);
  }

  /**
   * Given a collection of setters for the same property, picks which one to
   * use. Not having a proper setter is not an error unless of course the user
   * tries to use it.
   *
   * @param propertyName the name of the property/setter.
   * @param propertySetters the collection of setters.
   * @return the setter to use, or null if none is good enough.
   */
  private JMethod disambiguateSetters(String propertyName, 
      Collection<JMethod> propertySetters) {
    
    // if only have one overload, there is no need to rank them.
    if (propertySetters.size() == 1) {
      return propertySetters.iterator().next();
    }
    
    // rank overloads and pick the one with minimum 'cost' of conversion.
    JMethod preferredMethod = null;
    int minRank = Integer.MAX_VALUE;
    for (JMethod method : propertySetters) {
      int rank = rankMethodOnParameters(method);
      if (rank < minRank) {
        minRank = rank;
        preferredMethod = method;
        ambiguousSetters.remove(propertyName);
      } else if (rank == minRank && 
          !sameParameterTypes(preferredMethod, method)) {
        // sameParameterTypes test is necessary because a setter can be 
        // overridden by a subclass and that is not considered ambiguous. 
        if (!ambiguousSetters.contains(propertyName)) {
          ambiguousSetters.add(propertyName);
        }
      }
    }
    
    // if the setter is ambiguous, return null.
    if (ambiguousSetters.contains(propertyName)) {
      return null;
    }
    
    // the setter is not ambiguous therefore return the preferred overload.
    return preferredMethod;
  }

  /**
   * Recursively finds all setters for the given class and its superclasses.
   *
   * @param fieldType the leaf type to look at
   * @return a multimap of property name to the setter methods
   */
  private Map<String, Collection<JMethod>> findAllSetters(JClassType fieldType) {
    Map<String, Collection<JMethod>> allSetters;

    // First, get all setters from the parent class, recursively.
    JClassType superClass = fieldType.getSuperclass();
    if (superClass != null) {
      allSetters = findAllSetters(superClass);
    } else {
      // Stop recursion - deepest level creates return value
      allSetters = new HashMap<String, Collection<JMethod>>();
    }

    JMethod[] methods = fieldType.getMethods();
    for (JMethod method : methods) {
      if (!isSetterMethod(method)) {
        continue;
      }

      // Take out "set"
      String propertyName = method.getName().substring(3);

      // turn "PropertyName" into "propertyName"
      String beanPropertyName = Introspector.decapitalize(propertyName);
      addSetter(allSetters, beanPropertyName, method);

      // keep backwards compatibility (i.e. hTML instead of HTML for setHTML)
      String legacyPropertyName = propertyName.substring(0, 1).toLowerCase()
          + propertyName.substring(1);
      if (!legacyPropertyName.equals(beanPropertyName)) {
        addSetter(allSetters, legacyPropertyName, method);
      }
    }

    return allSetters;
  }

  /**
   * Finds all setters in the class, and puts them in the {@link #setters}
   * field.
   *
   * @param fieldType the type of the field
   */
  private void findSetters(JClassType fieldType) {
    // Pass one - get all setter methods
    Map<String, Collection<JMethod>> allSetters = findAllSetters(fieldType);

    // Pass two - disambiguate
    ambiguousSetters = new HashSet<String>();
    for (String propertyName : allSetters.keySet()) {
      Collection<JMethod> propertySetters = allSetters.get(propertyName);
      JMethod setter = disambiguateSetters(propertyName, propertySetters);
      setters.put(propertyName, setter);
    }
    
    if (ambiguousSetters.size() == 0) {
      ambiguousSetters = null;
    }
  }

  /**
   * Scans the class to find all methods annotated with @UiChild.
   *
   * @param ownerType the type of the owner class
   * @throws UnableToCompleteException
   */
  private void findUiChildren(JClassType ownerType)
      throws UnableToCompleteException {
    while (ownerType != null) {
      JMethod[] methods = ownerType.getMethods();
      for (JMethod method : methods) {
        UiChild annotation = method.getAnnotation(UiChild.class);
        if (annotation != null) {
          String tag = annotation.tagname();
          int limit = annotation.limit();
          if (tag.equals("")) {
            String name = method.getName();
            if (name.startsWith("add")) {
              tag = name.substring(3).toLowerCase();
            } else {
              logger.die(method.getName()
                  + " must either specify a UiChild tagname or begin "
                  + "with \"add\".");
            }
          }
          JParameter[] parameters = method.getParameters();
          if (parameters.length == 0) {
            logger.die("%s must take at least one Object argument", method.getName());
          }
          JType type = parameters[0].getType();
          if (type.isClassOrInterface() == null) {
            logger.die("%s first parameter must be an object type, found %s", 
                method.getName(), type.getQualifiedSourceName());
          }
          uiChildren.put(tag, Pair.create(method, limit));
        }
      }
      ownerType = ownerType.getSuperclass();
    }
  }

  /**
   * Finds the constructor annotated with @UiConcontructor if there is one, and
   * puts it in the {@link #uiConstructor} field.
   *
   * @param fieldType the type of the field
   */
  private void findUiConstructor(JClassType fieldType)
      throws UnableToCompleteException {
    for (JConstructor ctor : fieldType.getConstructors()) {
      if (ctor.getAnnotation(UiConstructor.class) != null) {
        if (uiConstructor != null) {
          logger.die(fieldType.getName()
              + " has more than one constructor annotated with @UiConstructor");
        }
        uiConstructor = ctor;
      }
    }
  }

  /**
   * Checks whether the given method qualifies as a setter. This looks at the
   * method qualifiers, name and return type, but not at the parameter types.
   *
   * @param method the method to look at
   * @return whether it's a setter
   */
  private boolean isSetterMethod(JMethod method) {
    // All setter methods should be public void setSomething(...)
    return method.isPublic() && !method.isStatic()
        && method.getName().startsWith("set") && method.getName().length() > 3
        && method.getReturnType() == JPrimitiveType.VOID;
  }
  
  /**
   * Ranks given method based on parameter conversion cost. A lower rank is
   * preferred over a higher rank since it has a lower cost of conversion.
   * 
   * The ranking criteria is as follows:
   * 1) methods with fewer arguments are preferred. for instance:
   *    'setValue(int)' is preferred 'setValue(int, int)'.
   * 2) within a set of overloads with the same number of arguments:
   * 2.1) String has the lowest cost = 1
   * 2.2) primitive types, cost = 2
   * 2.3) boxed primitive types, cost = 3
   * 2.4) any (reference types, etc), cost = 4.
   * 3) if a setter is overridden by a subclass and have the exact same argument
   * types, it will not be considered ambiguous. 
   *  
   * The cost mapping is defined in 
   * {@link #TYPE_RANK typeRank }
   * @param method
   * @return the rank of the method.
   */
  private int rankMethodOnParameters(JMethod method) {
    JParameter[] params = method.getParameters();
    int rank = 0;
    for (int i = 0; i < Math.min(params.length, 10); i++) {
      JType paramType = params[i].getType();
      int cost = DEFAULT_COST;
      if (TYPE_RANK.containsKey(paramType.getQualifiedSourceName())) {
        cost = TYPE_RANK.get(paramType.getQualifiedSourceName());
      }
      assert (cost >= 0 && cost <= 0x07);
      rank = rank | (cost << (3 * i));
    }
    assert (rank >= 0);
    return rank;
  }
  
  /**
   * Checks whether two methods have the same parameter types.
   *
   * @param m1 the first method to compare
   * @param m2 the second method to compare
   * @return whether the methods have the same parameter types
   */
  private boolean sameParameterTypes(JMethod m1, JMethod m2) {
    JParameter[] p1 = m1.getParameters();
    JParameter[] p2 = m2.getParameters();

    if (p1.length != p2.length) {
      return false;
    }

    for (int i = 0; i < p1.length; i++) {
      JType type1 = p1[i].getType();
      JType type2 = p2[i].getType();

      if (!type1.equals(type2)) {
        return false;
      }
    }

    return true;
  }
}
