/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

abstract class AbstractMembers {

  protected final JClassType classType;
  private JMethod[] cachedOverridableMethods;

  public AbstractMembers(JClassType classType) {
    this.classType = classType;
  }

  public JConstructor findConstructor(JType[] paramTypes) {
    JConstructor[] ctors = getConstructors();
    for (int i = 0; i < ctors.length; i++) {
      JConstructor candidate = ctors[i];
      if (candidate.hasParamTypes(paramTypes)) {
        return candidate;
      }
    }
    return null;
  }

  public abstract JField findField(String name);

  public JMethod findMethod(String name, JType[] paramTypes) {
    JMethod[] overloads = getOverloads(name);
    for (int i = 0; i < overloads.length; i++) {
      JMethod candidate = overloads[i];
      if (candidate.hasParamTypes(paramTypes)) {
        return candidate;
      }
    }
    return null;
  }

  public JClassType findNestedType(String typeName) {
    String[] parts = typeName.split("\\.");
    return findNestedTypeImpl(parts, 0);
  }

  public JConstructor getConstructor(JType[] paramTypes)
      throws NotFoundException {
    JConstructor result = findConstructor(paramTypes);
    if (result == null) {
      throw new NotFoundException();
    }
    return result;
  }

  public JConstructor[] getConstructors() {
    return doGetConstructors().toArray(TypeOracle.NO_JCTORS);
  }

  public JField getField(String name) {
    JField field = findField(name);
    assert (field != null);
    return field;
  }

  public abstract JField[] getFields();

  public JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException {
    JMethod result = findMethod(name, paramTypes);
    if (result == null) {
      throw new NotFoundException();
    }
    return result;
  }

  public abstract JMethod[] getMethods();

  public JClassType getNestedType(String typeName) throws NotFoundException {
    JClassType result = findNestedType(typeName);
    if (result == null) {
      throw new NotFoundException();
    }
    return result;
  }

  public JClassType[] getNestedTypes() {
    return doGetNestedTypes().values().toArray(TypeOracle.NO_JCLASSES);
  }

  public abstract JMethod[] getOverloads(String name);

  public JMethod[] getOverridableMethods() {
    if (cachedOverridableMethods == null) {
      Map<String, JMethod> methodsBySignature = new TreeMap<String, JMethod>();
      getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(methodsBySignature);
      if (classType.isClass() != null) {
        getOverridableMethodsOnSuperclassesAndThisClass(methodsBySignature);
      }
      int size = methodsBySignature.size();
      if (size == 0) {
        cachedOverridableMethods = TypeOracle.NO_JMETHODS;
      } else {
        Collection<JMethod> leafMethods = methodsBySignature.values();
        cachedOverridableMethods = leafMethods.toArray(new JMethod[size]);
      }
    }
    return cachedOverridableMethods;
  }

  protected abstract void addConstructor(JConstructor ctor);

  protected abstract void addField(JField field);

  protected abstract void addMethod(JMethod method);

  protected abstract List<JConstructor> doGetConstructors();

  protected abstract Map<String, JClassType> doGetNestedTypes();

  protected JClassType findNestedTypeImpl(String[] typeName, int index) {
    JClassType found = doGetNestedTypes().get(typeName[index]);
    if (found == null) {
      return null;
    } else if (index < typeName.length - 1) {
      return found.findNestedTypeImpl(typeName, index + 1);
    } else {
      return found;
    }
  }

  protected void getOverridableMethodsOnSuperclassesAndThisClass(
      Map<String, JMethod> methodsBySignature) {
    assert (classType.isClass() != null);

    // Recurse first so that more derived methods will clobber less derived
    // methods.
    JClassType superClass = classType.getSuperclass();
    if (superClass != null) {
      superClass.getOverridableMethodsOnSuperclassesAndThisClass(methodsBySignature);
    }

    JMethod[] declaredMethods = getMethods();
    for (int i = 0; i < declaredMethods.length; i++) {
      JMethod method = declaredMethods[i];

      // Ensure that this method is inherited.
      if (method.isPrivate() || method.isStatic()) {
        // We don't inherit this method, so skip it.
        continue;
      }

      String sig = computeInternalSignature(method);

      // Ensure that this method is overridable.
      if (method.isFinal()) {
        // We cannot override this method, but it might override another method, so remove any possibly overridden method.
        methodsBySignature.remove(sig);
      } else {
        // We can override this method, so record it.
        methodsBySignature.put(sig, method);
      }
    }
  }

  /**
   * Gets the methods declared in interfaces that this type extends. If this
   * type is a class, its own methods are not added. If this type is an
   * interface, its own methods are added. Used internally by
   * {@link #getOverridableMethods()}.
   * 
   * @param methodsBySignature
   */
  protected void getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map<String, JMethod> methodsBySignature) {
    // Recurse first so that more derived methods will clobber less derived
    // methods.
    JClassType[] superIntfs = classType.getImplementedInterfaces();
    for (int i = 0; i < superIntfs.length; i++) {
      JClassType superIntf = superIntfs[i];
      superIntf.getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(methodsBySignature);
    }

    if (classType.isInterface() == null) {
      // This is not an interface, so we're done after having visited its
      // implemented interfaces.
      return;
    }

    JMethod[] declaredMethods = getMethods();
    for (int i = 0; i < declaredMethods.length; i++) {
      JMethod method = declaredMethods[i];

      String sig = computeInternalSignature(method);
      JMethod existing = methodsBySignature.get(sig);
      if (existing != null) {
        JClassType existingType = existing.getEnclosingType();
        JClassType thisType = method.getEnclosingType();
        if (thisType.isAssignableFrom(existingType)) {
          // The existing method is in a more-derived type, so don't replace it.
          continue;
        }
      }
      methodsBySignature.put(sig, method);
    }
  }

  protected JClassType getParentType() {
    return classType;
  }

  private String computeInternalSignature(JMethod method) {
    StringBuffer sb = new StringBuffer();
    sb.setLength(0);
    sb.append(method.getName());
    JParameter[] params = method.getParameters();
    for (int j = 0; j < params.length; j++) {
      JParameter param = params[j];
      sb.append("/");
      sb.append(param.getType().getErasedType().getQualifiedSourceName());
    }
    return sb.toString();
  }

}