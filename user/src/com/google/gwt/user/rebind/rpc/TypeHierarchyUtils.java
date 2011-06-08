/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.typeinfo.JClassType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Collection of utility methods for dealing with type hierarchies.
 */
class TypeHierarchyUtils {

  /**
   * Returns <code>true</code> if the type directly implements the specified
   * interface. The test is done on erased types; any paramaterizations supplied
   * in the arguments are ignored.
   * 
   * @param type type to check
   * @param intf interface to look for
   * @return <code>true</code> if the type directly implements the specified
   *         interface
   */
  public static boolean directlyImplementsInterface(JClassType type, JClassType intf) {
    type = type.getErasedType();
    intf = intf.getErasedType();
    return directlyImplementsInterfaceRecursive(new HashSet<JClassType>(), type, intf);
  }

  /**
   * Returns all types on the path from the root type to the serializable
   * leaves.
   * 
   * @param root the root type
   * @param leaves the set of serializable leaf types
   * @return all types on the path from the root type to the serializable leaves
   */
  public static List<JClassType> getAllTypesBetweenRootTypeAndLeaves(JClassType root,
      Collection<JClassType> leaves) {
    Map<JClassType, List<JClassType>> adjList = getInvertedTypeHierarchy(root.getErasedType());
    Set<JClassType> types = new HashSet<JClassType>();

    for (JClassType type : leaves) {
      depthFirstSearch(types, adjList, type.getErasedType());
    }

    return Arrays.asList(types.toArray(new JClassType[0]));
  }

  /**
   * Returns the immediate subtypes of the erased class argument.
   */
  public static List<JClassType> getImmediateSubtypes(JClassType clazz) {
    List<JClassType> immediateSubtypes = new ArrayList<JClassType>();
    clazz = clazz.getErasedType();
    for (JClassType subclass : clazz.getSubtypes()) {
      JClassType superclass = subclass.getSuperclass();
      if (superclass != null) {
        superclass = superclass.getErasedType();
      }

      if (superclass == clazz || clazz.isInterface() != null
          && directlyImplementsInterface(subclass, clazz)) {
        immediateSubtypes.add(subclass);
      }
    }

    return immediateSubtypes;
  }

  private static void addEdge(Map<JClassType, List<JClassType>> adjList, JClassType subclass,
      JClassType clazz) {
    List<JClassType> edges = adjList.get(subclass);
    if (edges == null) {
      edges = new ArrayList<JClassType>();
      adjList.put(subclass, edges);
    }

    edges.add(clazz);
  }

  private static void depthFirstSearch(Set<JClassType> seen,
      Map<JClassType, List<JClassType>> adjList, JClassType type) {
    if (seen.contains(type)) {
      return;
    }
    seen.add(type);

    List<JClassType> children = adjList.get(type);
    if (children != null) {
      for (JClassType child : children) {
        if (!seen.contains(child)) {
          depthFirstSearch(seen, adjList, child);
        }
      }
    }
  }

  private static boolean directlyImplementsInterfaceRecursive(Set<JClassType> seen,
      JClassType clazz, JClassType intf) {
    assert (clazz.getErasedType() == clazz);
    assert (intf.getErasedType() == intf);

    if (clazz == intf) {
      return true;
    }

    JClassType[] intfImpls = clazz.getImplementedInterfaces();

    for (JClassType intfImpl : intfImpls) {
      intfImpl = intfImpl.getErasedType();
      if (!seen.contains(intfImpl)) {
        seen.add(intfImpl);

        if (directlyImplementsInterfaceRecursive(seen, intfImpl, intf)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Given a root type return an adjacency list that is the inverted type
   * hierarchy.
   */
  private static Map<JClassType, List<JClassType>> getInvertedTypeHierarchy(JClassType root) {
    Map<JClassType, List<JClassType>> adjList = new HashMap<JClassType, List<JClassType>>();
    Set<JClassType> seen = new HashSet<JClassType>();
    Stack<JClassType> queue = new Stack<JClassType>();
    queue.push(root);
    while (!queue.isEmpty()) {
      JClassType clazz = queue.pop();
      if (seen.contains(clazz)) {
        continue;
      }
      seen.add(clazz);

      List<JClassType> immediateSubtypes = getImmediateSubtypes(clazz);
      for (JClassType immediateSubtype : immediateSubtypes) {
        // Add an edge from the immediate subtype to the supertype
        addEdge(adjList, immediateSubtype, clazz);
        queue.push(immediateSubtype);
      }
    }

    return adjList;
  }

}
