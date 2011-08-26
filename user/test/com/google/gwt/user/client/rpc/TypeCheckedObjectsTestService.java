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
 * Service interface used by the
 * {@link com.google.gwt.user.client.rpc.TypeCheckedObjectsTest
 * TypeCheckedObjectsTest} unit test.
 */
public interface TypeCheckedObjectsTestService extends RemoteService {
  TypeCheckedFieldClass<Integer, String> echo(TypeCheckedFieldClass<Integer, String> arg1);

  TypeCheckedGenericClass<Integer, String> echo(TypeCheckedGenericClass<Integer, String> arg1);

  TypeCheckedNestedLists echo(TypeCheckedNestedLists arg1);

  TypeCheckedSuperClass<Integer, String> echo(TypeCheckedSuperClass<Integer, String> arg1);

  TypeUncheckedGenericClass<Integer, String> echo(TypeUncheckedGenericClass<Integer, String> arg1);
}
