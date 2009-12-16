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
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.rebind.MortalLogger;

import java.util.ArrayList;
import java.util.Collection;
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

  /**
   * Global map of field classes. This serves as a cache so each class is only
   * processed once.
   */
  private static final Map<JClassType, OwnerFieldClass> FIELD_CLASSES = new HashMap<JClassType, OwnerFieldClass>();

  /**
   * Gets or creates the descriptor for the given field class.
   *
   * @param forType the field type to get a descriptor for
   * @param logger TODO
   * @return the descriptor
   */
  public static OwnerFieldClass getFieldClass(JClassType forType,
      MortalLogger logger) throws UnableToCompleteException {
    OwnerFieldClass clazz = FIELD_CLASSES.get(forType);
    if (clazz == null) {
      clazz = new OwnerFieldClass(forType, logger);
      FIELD_CLASSES.put(forType, clazz);
    }
    return clazz;
  }

  private final JClassType rawType;
  private final Map<String, JMethod> setters = new HashMap<String, JMethod>();
  private Set<String> ambiguousSetters;
  private JConstructor uiConstructor;
  private final MortalLogger logger;

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
    // TODO(rjrjr) This fails for CheckBox#setValue(Boolean) because it
    // also finds CheckBox#setValue(Boolean, Boolean). Must fix for 2.0,
    // when CheckBox#setChecked will go away and CheckBox#setValue must be used

    if (ambiguousSetters != null && ambiguousSetters.contains(propertyName)) {
      logger.die("Ambiguous setter requested: " + rawType.getName() + "."
          + propertyName);
    }

    return setters.get(propertyName);
  }

  /**
   * Returns the constructor annotated with @UiConstructor, or null if none
   * exists.
   */
  public JConstructor getUiConstructor() {
    return uiConstructor;
  }

  /**
   * Given a collection of setters for the same property, picks which one to
   * use. Not having a proper setter is not an error unless of course the user
   * tries to use it.
   *
   * @param propertySetters the collection of setters
   * @return the setter to use, or null if none is good enough
   */
  private JMethod disambiguateSetters(Collection<JMethod> propertySetters) {
    if (propertySetters.size() == 1) {
      return propertySetters.iterator().next();
    }

    // Pick the string setter, if there's one
    for (JMethod method : propertySetters) {
      JParameter[] parameters = method.getParameters();
      if (parameters.length == 1
          && parameters[0].getType().getQualifiedSourceName().equals(
              "java.lang.String")) {
        return method;
      }
    }

    // Check if all setters aren't just the same one being overridden in parent
    // classes.
    JMethod firstMethod = null;
    for (JMethod method : propertySetters) {
      if (firstMethod == null) {
        firstMethod = method;
        continue;
      }

      // If the method is not the same as the first one, there's still an
      // ambiguity. Being equal means having the same parameter types.
      if (!sameParameterTypes(method, firstMethod)) {
        return null;
      }
    }

    return firstMethod;
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
      propertyName = propertyName.substring(0, 1).toLowerCase()
          + propertyName.substring(1);

      Collection<JMethod> propertyMethods = allSetters.get(propertyName);
      if (propertyMethods == null) {
        propertyMethods = new ArrayList<JMethod>();
        allSetters.put(propertyName, propertyMethods);
      }

      propertyMethods.add(method);
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
    for (String propertyName : allSetters.keySet()) {
      Collection<JMethod> propertySetters = allSetters.get(propertyName);
      JMethod setter = disambiguateSetters(propertySetters);

      // If no setter could be disambiguated for this property, add it to the
      // set of ambiguous setters. This is later consulted if and only if the
      // setter is used.
      if (setter == null) {
        if (ambiguousSetters == null) {
          ambiguousSetters = new HashSet<String>();
        }

        ambiguousSetters.add(propertyName);
      }

      setters.put(propertyName, setter);
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
        && method.getName().startsWith("set")
        && method.getName().length() > 3
        && method.getReturnType() == JPrimitiveType.VOID;
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

    // No types were different
    return true;
  }
}
