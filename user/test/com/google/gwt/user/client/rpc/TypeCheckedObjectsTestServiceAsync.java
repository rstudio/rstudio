/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedFieldClass;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedNestedLists;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedSuperClass;

/**
 * Async service for testing type checking of RPC arguments.
 * 
 * Note that the first argument to each echo method is raw, so as to allow
 * incorrect types to be sent and tested.
 * 
 */
public interface TypeCheckedObjectsTestServiceAsync {
  /**
   * @see com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedFieldClass)
   */
  @SuppressWarnings("rawtypes")
  void echo(TypeCheckedFieldClass arg1,
      AsyncCallback<TypeCheckedFieldClass<Integer, String>> callback);

  /**
   * @see com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedNestedLists)
   */
  void echo(TypeCheckedNestedLists arg1, AsyncCallback<TypeCheckedNestedLists> callback);

  /**
   * @see com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google.gwt.user.client.rpc.TypeCheckedGenericClass)
   */
  @SuppressWarnings("rawtypes")
  void echo(TypeCheckedGenericClass arg1,
      AsyncCallback<TypeCheckedGenericClass<Integer, String>> callback);

  /**
   * @see com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedSuperClass)
   */
  @SuppressWarnings("rawtypes")
  void echo(TypeCheckedSuperClass arg1,
      AsyncCallback<TypeCheckedSuperClass<Integer, String>> callback);

  /**
   * @see com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google.gwt.user.client.rpc.TypeUncheckedGenericClass)
   */
  @SuppressWarnings("rawtypes")
  void echo(TypeUncheckedGenericClass arg1,
      AsyncCallback<TypeUncheckedGenericClass<Integer, String>> callback);
}
