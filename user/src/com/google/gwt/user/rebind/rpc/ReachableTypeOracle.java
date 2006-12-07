/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * Answers questions about types reachable from from types and methods.
 */
public interface ReachableTypeOracle {
  /**
   * Determine the set of types that are reachable from the method signatures in
   * this interface and any interfaces that it implements. This includes
   * parameter types, return types, and checked exception types.
   * 
   * <li>Interface
   * <ul>
   * <li>Types reachable from implemented interfaces
   * <li>Types reachable from non-static member methods
   * </ul>
   * 
   * @param intf
   * @return reachable types
   */
  JType[] getTypesReachableFromInterface(JClassType intf);

  /**
   * Determine the set of type that are reachable from this method signature.
   * This includes the parameter types, return types, and checked exception
   * types.
   * 
   * @param method
   * @return reachable types
   */
  JType[] getTypesReachableFromMethod(JMethod method);

  /**
   * Determine the set of types that are reachable from a given type. The rules
   * used to examine the type are as follows:
   * 
   * <ul>
   * <li>Primitive
   * <ul>
   * <li>Only the primitive is reachable. TODO(mmendez): should the boxed
   * version be included?
   * </ul>
   * <li>Interface
   * <ul>
   * <li>Types reachable from the interface's subtypes
   * </ul>
   * <li>Method
   * <ul>
   * <li>Method signatures are ignored by this method.
   * </ul>
   * <li>Class
   * <ul>
   * <li>Types reachable from the field types
   * <li>Types reachable from the class's subtypes
   * </ul>
   * <li>Array
   * <ul>
   * <li>Types reachable from the component type
   * </ul>
   * <li>Parameterized Class
   * <ul>
   * <li>Types reachable from the raw type
   * <li>Types reachable from the type arguments
   * </ul>
   * </ul>
   * 
   * @param type
   * @return reachable types
   */
  JType[] getTypesReachableFromType(JType cls);
}
