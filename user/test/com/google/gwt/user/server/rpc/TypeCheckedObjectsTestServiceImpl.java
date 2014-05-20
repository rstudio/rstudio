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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.TypeCheckedGenericClass;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedFieldClass;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedNestedLists;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedSuperClass;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetValidator;
import com.google.gwt.user.client.rpc.TypeUncheckedGenericClass;

/**
 * Servlet used by the
 * {@link com.google.gwt.user.client.rpc.TypeCheckedObjectsTest} unit tests.
 */
public class TypeCheckedObjectsTestServiceImpl extends RemoteServiceServlet implements
    TypeCheckedObjectsTestService {

  /*
   * @see
   * com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google
   * .
   * gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedFieldClass)
   */
  @Override
  public TypeCheckedFieldClass<Integer, String> echo(TypeCheckedFieldClass<Integer, String> arg1) {
    if (!TypeCheckedObjectsTestSetValidator.isValid(arg1)) {
      throw new RuntimeException();
    }

    return arg1;
  }

  /*
   * @see
   * com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google
   * .
   * gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedNestedLists)
   */
  @Override
  public TypeCheckedNestedLists echo(TypeCheckedNestedLists arg1) {
    if (!TypeCheckedObjectsTestSetValidator.isValid(arg1)) {
      throw new RuntimeException();
    }

    return arg1;
  }

  /*
   * @see
   * com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google
   * .gwt.user.client.rpc.TypeCheckedGenericClass)
   */
  @Override
  public TypeCheckedGenericClass<Integer, String> echo(
      TypeCheckedGenericClass<Integer, String> arg1) {
    if (!TypeCheckedObjectsTestSetValidator.isValid(arg1)) {
      throw new RuntimeException();
    }

    return arg1;
  }

  /*
   * @see
   * com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google
   * .
   * gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedSuperClass)
   */
  @Override
  public TypeCheckedSuperClass<Integer, String> echo(TypeCheckedSuperClass<Integer, String> arg1) {
    if (!TypeCheckedObjectsTestSetValidator.isValid(arg1)) {
      throw new RuntimeException();
    }

    return arg1;
  }

  /*
   * @see
   * com.google.gwt.user.client.rpc.TypeCheckedObjectsTestService#echo(com.google
   * .gwt.user.client.rpc.TypeUncheckedGenericClass)
   */
  @Override
  public TypeUncheckedGenericClass<Integer, String> echo(
      TypeUncheckedGenericClass<Integer, String> arg1) {
    if (!TypeCheckedObjectsTestSetValidator.isValid(arg1)) {
      throw new RuntimeException();
    }

    return arg1;
  }
}
