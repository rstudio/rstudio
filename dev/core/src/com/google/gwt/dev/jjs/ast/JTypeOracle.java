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
package com.google.gwt.dev.jjs.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Oracle that can answer questions regarding the types in a program.
 */
public class JTypeOracle {

  private final Map/* <JInterfaceType, Set<JClassType>> */couldBeImplementedMap = new IdentityHashMap();

  private final Map/* <JClassType, Set<JInterfaceType>> */couldImplementMap = new IdentityHashMap();

  private final Set/* <JReferenceType> */hasClinitSet = new HashSet();

  private final Map/* <JClassType, Set<JInterfaceType>> */implementsMap = new IdentityHashMap();

  private final Set/* <JReferenceType> */instantiatedTypes = new HashSet();

  private final Map/* <JInterfaceType, Set<JClassType>> */isImplementedMap = new IdentityHashMap();

  private JClassType javaLangObject = null;

  private final JProgram program;

  private final Map/* <JClassType, Set<JClassType>> */subClassMap = new IdentityHashMap();

  private final Map/* <JInterfaceType, Set<JInterfaceType>> */subInterfaceMap = new IdentityHashMap();

  private final Map/* <JClassType, Set<JClassType>> */superClassMap = new IdentityHashMap();

  private final Map/* <JInterfaceType, Set<JInterfaceType>> */superInterfaceMap = new IdentityHashMap();

  private final Map/* <JMethod, Map<JClassType, Set<JMethod>>> */virtualUpRefMap = new IdentityHashMap();

  public JTypeOracle(JProgram program) {
    this.program = program;
  }

  public boolean canTheoreticallyCast(JReferenceType type, JReferenceType qType) {
    JClassType jlo = program.getTypeJavaLangObject();
    if (type == qType || type == jlo) {
      return true;
    }

    if (canTriviallyCast(type, qType)) {
      return true;
    }

    if (type instanceof JArrayType) {

      JArrayType aType = (JArrayType) type;
      if (qType instanceof JArrayType) {
        JArrayType qaType = (JArrayType) qType;
        JType leafType = aType.getLeafType();
        JType qLeafType = qaType.getLeafType();
        int dims = aType.getDims();
        int qDims = qaType.getDims();

        // null[] or Object[] -> int[][] might work, other combinations won't
        if (dims < qDims && leafType != program.getTypeJavaLangObject()
            && !(leafType instanceof JNullType)) {
          return false;
        }

        if (dims == qDims) {
          if (leafType instanceof JReferenceType
              && qLeafType instanceof JReferenceType) {
            return canTheoreticallyCast((JReferenceType) leafType,
                (JReferenceType) qLeafType);
          }
        }
      }

    } else if (type instanceof JClassType) {

      JClassType cType = (JClassType) type;
      if (qType instanceof JClassType) {
        return getOrCreate(subClassMap, cType).contains(qType);
      } else if (qType instanceof JInterfaceType) {
        return getOrCreate(couldImplementMap, cType).contains(qType);
      }
    } else if (type instanceof JInterfaceType) {

      JInterfaceType iType = (JInterfaceType) type;
      if (qType instanceof JClassType) {
        return getOrCreate(couldBeImplementedMap, iType).contains(qType);
      }
    } else if (type instanceof JNullType) {
    }

    return true;
  }

  public boolean canTriviallyCast(JReferenceType type, JReferenceType qType) {
    JClassType jlo = program.getTypeJavaLangObject();
    if (type == qType || qType == jlo) {
      return true;
    }

    if (type instanceof JArrayType) {

      JArrayType aType = (JArrayType) type;
      if (qType instanceof JArrayType) {
        JArrayType qaType = (JArrayType) qType;
        JType leafType = aType.getLeafType();
        JType qLeafType = qaType.getLeafType();
        int dims = aType.getDims();
        int qDims = qaType.getDims();

        // int[][] -> Object[] or null[] trivially true
        if (dims > qDims
            && (qLeafType == jlo || qLeafType instanceof JNullType)) {
          return true;
        }

        if (dims == qDims) {
          if (leafType instanceof JReferenceType
              && qLeafType instanceof JReferenceType) {
            return canTriviallyCast((JReferenceType) leafType,
                (JReferenceType) qLeafType);
          }
        }
      }
    } else if (type instanceof JClassType) {

      JClassType cType = (JClassType) type;
      if (qType instanceof JClassType) {
        return getOrCreate(superClassMap, cType).contains(qType);
      } else if (qType instanceof JInterfaceType) {
        return getOrCreate(implementsMap, cType).contains(qType);
      }
    } else if (type instanceof JInterfaceType) {

      JInterfaceType iType = (JInterfaceType) type;
      if (qType instanceof JInterfaceType) {
        return getOrCreate(superInterfaceMap, iType).contains(qType);
      }
    } else if (type instanceof JNullType) {

      return true;
    }

    return false;
  }

  public void computeAfterAST() {
    recomputeClinits();
  }

  public void computeBeforeAST() {
    javaLangObject = program.getTypeJavaLangObject();
    superClassMap.clear();
    subClassMap.clear();
    superInterfaceMap.clear();
    subInterfaceMap.clear();
    implementsMap.clear();
    couldImplementMap.clear();
    isImplementedMap.clear();
    couldBeImplementedMap.clear();

    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = (JReferenceType) program.getDeclaredTypes().get(i);
      if (type instanceof JClassType) {
        recordSuperSubInfo((JClassType) type);
      } else {
        recordSuperSubInfo((JInterfaceType) type);
      }
    }
    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = (JReferenceType) program.getDeclaredTypes().get(i);
      if (type instanceof JClassType) {
        computeImplements((JClassType) type);
      }
    }
    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = (JReferenceType) program.getDeclaredTypes().get(i);
      if (type instanceof JClassType) {
        computeCouldImplement((JClassType) type);
      }
    }
    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = (JReferenceType) program.getDeclaredTypes().get(i);
      if (type instanceof JClassType) {
        computeVirtualUpRefs((JClassType) type);
      }
    }
  }

  public JMethod[] getAllVirtualOverrides(JMethod method) {
    Set/* <JMethod> */results = new HashSet/* <JMethod> */();
    Map/* <JClassType, Set<JMethod>> */overrideMap = getOrCreateMap(
        virtualUpRefMap, method);
    for (Iterator it = overrideMap.keySet().iterator(); it.hasNext();) {
      JClassType classType = (JClassType) it.next();
      if (instantiatedTypes.contains(classType)) {
        Set/* <JMethod> */set = (Set) overrideMap.get(classType);
        results.addAll(set);
      }
    }
    return (JMethod[]) results.toArray(new JMethod[results.size()]);
  }

  public Set/* <JReferenceType> */getInstantiatedTypes() {
    return instantiatedTypes;
  }

  public boolean hasClinit(JReferenceType type) {
    return hasClinitSet.contains(type);
  }

  public boolean isInstantiatedType(JReferenceType type) {
    if (type instanceof JNullType) {
      return true;
    }

    if (type instanceof JArrayType) {
      JArrayType arrayType = (JArrayType) type;
      if (arrayType.getLeafType() instanceof JNullType) {
        return true;
      }
    }
    return instantiatedTypes.contains(type);
  }

  public void recomputeClinits() {
    hasClinitSet.clear();
    for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
      JReferenceType type = (JReferenceType) program.getDeclaredTypes().get(i);
      computeHasClinit(type);
    }
  }

  public void setInstantiatedTypes(Set/* <JReferenceType> */instantiatedTypes) {
    this.instantiatedTypes.clear();
    this.instantiatedTypes.addAll(instantiatedTypes);
  }

  private/* <K, V> */void add(Map/* <K, Set<V>> */map, Object key,
      Object value) {
    getOrCreate(map, key).add(value);
  }

  /**
   * Compute all of the things I might conceivably implement, either through
   * super types or sub types.
   */
  private void computeCouldImplement(JClassType type) {
    Set/* <JInterfaceType> */couldImplementSet = getOrCreate(
        couldImplementMap, type);
    // all of my direct implements are trivially true
    couldImplementSet.addAll(getOrCreate(implementsMap, type));
    List/* <JClassType> */subclasses = new ArrayList/* <JClassType> */();
    subclasses.addAll(getOrCreate(subClassMap, type));
    for (Iterator itSub = subclasses.iterator(); itSub.hasNext();) {
      JClassType subclass = (JClassType) itSub.next();
      for (Iterator itIntf = subclass.implments.iterator(); itIntf.hasNext();) {
        JInterfaceType intf = (JInterfaceType) itIntf.next();
        couldImplementSet.add(intf);
        for (Iterator itIsUp = getOrCreate(superInterfaceMap, intf).iterator(); itIsUp.hasNext();) {
          JInterfaceType isup = (JInterfaceType) itIsUp.next();
          couldImplementSet.add(isup);
        }
      }
    }
    for (Iterator itCouldImpl = couldImplementSet.iterator(); itCouldImpl.hasNext();) {
      JInterfaceType couldImpl = (JInterfaceType) itCouldImpl.next();
      add(couldBeImplementedMap, couldImpl, type);
    }
  }

  private void computeHasClinit(JReferenceType type) {
    JMethod method = (JMethod) type.methods.get(0);
    if (!method.body.statements.isEmpty()) {
      hasClinitSet.add(type);
    }
  }

  /**
   * Compute all of the things I implement directly, through super types.
   */
  private void computeImplements(JClassType type) {
    Set/* <JInterfaceType> */implementsSet = getOrCreate(implementsMap, type);
    List/* <JClassType> */list = new ArrayList/* <JClassType> */();
    list.add(type);
    list.addAll(getOrCreate(superClassMap, type));
    for (Iterator itSuper = list.iterator(); itSuper.hasNext();) {
      JClassType superclass = (JClassType) itSuper.next();
      for (Iterator itIntf = superclass.implments.iterator(); itIntf.hasNext();) {
        JInterfaceType intf = (JInterfaceType) itIntf.next();
        implementsSet.add(intf);
        for (Iterator itIsUp = getOrCreate(superInterfaceMap, intf).iterator(); itIsUp.hasNext();) {
          JInterfaceType isup = (JInterfaceType) itIsUp.next();
          implementsSet.add(isup);
        }
      }
    }
    for (Iterator itImpl = implementsSet.iterator(); itImpl.hasNext();) {
      JInterfaceType impl = (JInterfaceType) itImpl.next();
      add(isImplementedMap, impl, type);
    }
  }

  /**
   * WEIRD: Suppose class Foo declares void f(){} and unrelated interface I also
   * declares void f(). Then suppose Bar extends Foo implements I and doesn't
   * override f(). We need to record a "virtual" upref from Foo.f() to I.f() so
   * that if I.f() is rescued AND Bar is instantiable, Foo.f() does not get
   * pruned.
   */
  private void computeVirtualUpRefs(JClassType type) {
    if (type.extnds == null || type.extnds == javaLangObject) {
      return;
    }

    /*
     * For each interface I directly implement, check all methods and make sure
     * I define implementations for them. If I don't, then check all my super
     * classes to find virtual overrides.
     */
    for (Iterator itIntf = type.implments.iterator(); itIntf.hasNext();) {
      JInterfaceType intf = (JInterfaceType) itIntf.next();
      computeVirtualUpRefs(type, intf);
      Set/* <JInterfaceType> */superIntfs = getOrCreate(superInterfaceMap,
          intf);
      for (Iterator itSuper = superIntfs.iterator(); itSuper.hasNext();) {
        JInterfaceType superIntf = (JInterfaceType) itSuper.next();
        computeVirtualUpRefs(type, superIntf);
      }
    }
  }

  /**
   * For each interface I directly implement, check all methods and make sure I
   * define implementations for them. If I don't, then check all my super
   * classes to find virtual overrides.
   */
  private void computeVirtualUpRefs(JClassType type, JInterfaceType intf) {
    outer : for (Iterator itIntf = intf.methods.iterator(); itIntf.hasNext();) {
      JMethod intfMethod = (JMethod) itIntf.next();
      for (Iterator itType = type.methods.iterator(); itType.hasNext();) {
        JMethod classMethod = (JMethod) itType.next();
        if (JProgram.methodsDoMatch(intfMethod, classMethod)) {
          // this class directly implements the interface method
          continue outer;
        }
      }

      // this class does not directly implement the interface method
      // if any super classes do, create a virtual up ref
      for (JClassType superType = type.extnds; superType != javaLangObject; superType = superType.extnds) {
        for (Iterator itSuper = superType.methods.iterator(); itSuper.hasNext();) {
          JMethod superMethod = (JMethod) itSuper.next();
          if (JProgram.methodsDoMatch(intfMethod, superMethod)) {
            // this super class directly implements the interface method
            // create a virtual up ref

            // System.out.println("Virtual upref from " + superType.getName()
            // + "." + superMethod.getName() + " to " + intf.getName() + "."
            // + intfMethod.getName() + " via " + type.getName());

            Map/* <JClassType, Set<JMethod>> */classToMethodMap = getOrCreateMap(
                virtualUpRefMap, superMethod);
            Set/* <JMethod> */methodSet = getOrCreate(classToMethodMap, type);
            methodSet.add(intfMethod);

            // do not search additional super types
            continue outer;
          }
        }
      }
    }
  }

  private/* <K, V> */Set/* <V> */getOrCreate(Map/* <K, Set<V>> */map,
      Object key) {
    Set/* <V> */set = (Set) map.get(key);
    if (set == null) {
      set = new HashSet/* <V> */();
      map.put(key, set);
    }
    return set;
  }

  private/* <K, K2, V> */Map/* <K2, V> */getOrCreateMap(
      Map/* <K, Map<K2, V>> */map, Object key) {
    Map/* <K2, V> */map2 = (Map) map.get(key);
    if (map2 == null) {
      map2 = new HashMap/* <K2, V> */();
      map.put(key, map2);
    }
    return map2;
  }

  /**
   * Record the all of my super classes (and myself as a subclass of them).
   */
  private void recordSuperSubInfo(JClassType type) {
    Set/* <JClassType> */superSet = getOrCreate(superClassMap, type);
    for (JClassType t = type.extnds; t != null; t = t.extnds) {
      superSet.add(t);
      add(subClassMap, t, type);
    }
  }

  /**
   * Record the all of my super interfaces (and myself as a sub interface of
   * them).
   */
  private void recordSuperSubInfo(JInterfaceType type) {
    Set/* <JInterfaceType> */superSet = getOrCreate(superInterfaceMap, type);
    recordSuperSubInfo(type, superSet, type);
  }

  /**
   * Recursively record all of my super interfaces.
   */
  private void recordSuperSubInfo(JInterfaceType base,
      Set/* <JInterfaceType> */superSet, JInterfaceType cur) {
    for (Iterator it = cur.implments.iterator(); it.hasNext();) {
      JInterfaceType intf = (JInterfaceType) it.next();
      superSet.add(intf);
      add(subInterfaceMap, intf, base);
      recordSuperSubInfo(base, superSet, intf);
    }
  }

}
