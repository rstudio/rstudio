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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedFieldClass;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedNestedLists;
import com.google.gwt.user.client.rpc.TypeCheckedObjectsTestSetFactory.TypeCheckedSuperClass;

import junit.framework.AssertionFailedError;

/**
 * Test for RPC serialization of type checked objects.
 * 
 * Type checked objects are those that are verified as being of the correct type
 * before they are deserialized, thus catching certain attacks that occur
 * through deserialization.
 * 
 * Test Cases: - Type checked generic class with a server-side custom serializer
 * that is NOT derived from ServerCustomFieldSerializer but which does define
 * instantiateChecked and deserializeChecked, to verify that such methods are
 * found and called. - Generic class that has no custom field serializer but
 * which does include fields that do have type checked serializers, to verify
 * that such serializers are still used. - Generic class that has no custom
 * field serializer but which does extend a class with type checked serializers,
 * to verify that such serializers are still used.
 */
public class TypeCheckedObjectsTest extends RpcTestBase {

  private TypeCheckedObjectsTestServiceAsync typeCheckedObjectsTestService;

  public void testInvalidCheckedFieldSerializer() {
    TypeCheckedObjectsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TypeCheckedObjectsTestSetFactory.createInvalidCheckedFieldClass(),
        new AsyncCallback<TypeCheckedFieldClass<Integer, String>>() {
          @Override
          public void onFailure(Throwable caught) {
            // Expected in this case
            assertTrue(caught instanceof SerializationException);
            finishTest();
          }

          @Override
          public void onSuccess(TypeCheckedFieldClass<Integer, String> result) {
            fail("testInvalidCheckedFieldSerializer is expected to throw an assertion");
          }
        });
  }

  public void testInvalidCheckedSerializer() {
    TypeCheckedObjectsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TypeCheckedObjectsTestSetFactory.createInvalidCheckedGenericClass(),
        new AsyncCallback<TypeCheckedGenericClass<Integer, String>>() {
          @Override
          public void onFailure(Throwable caught) {
            // Expected in this case
            assertTrue(caught instanceof SerializationException);
            finishTest();
          }

          @Override
          public void onSuccess(TypeCheckedGenericClass<Integer, String> result) {
            fail("testInvalidCheckedSerializer is expected to throw an assertion");
          }
        });
  }

  public void testInvalidCheckedSuperSerializer() {
    TypeCheckedObjectsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TypeCheckedObjectsTestSetFactory.createInvalidCheckedSuperClass(),
        new AsyncCallback<TypeCheckedSuperClass<Integer, String>>() {
          @Override
          public void onFailure(Throwable caught) {
            // Expected in this case
            assertTrue(caught instanceof SerializationException);
            finishTest();
          }

          @Override
          public void onSuccess(TypeCheckedSuperClass<Integer, String> result) {
            fail("testInvalidCheckedSerializer is expected to throw an assertion");
          }
        });
  }

  public void testInvalidUncheckedSerializer() {
    TypeCheckedObjectsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TypeCheckedObjectsTestSetFactory.createInvalidUncheckedGenericClass(),
        new AsyncCallback<TypeUncheckedGenericClass<Integer, String>>() {
          @Override
          public void onFailure(Throwable caught) {
            // Expected in this case
            assertTrue(caught instanceof SerializationException);
            finishTest();
          }

          @Override
          public void onSuccess(TypeUncheckedGenericClass<Integer, String> result) {
            fail("testInvalidUncheckedSerializer is expected to throw an assertion");
          }
        });
  }

  public void testTypeCheckedFieldSerializer() {
    TypeCheckedObjectsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TypeCheckedObjectsTestSetFactory.createTypeCheckedFieldClass(),
        new AsyncCallback<TypeCheckedFieldClass<Integer, String>>() {
          @Override
          public void onFailure(Throwable caught) {
            AssertionFailedError er =
                new AssertionFailedError("Could not serialize/deserialize TypeCheckedFieldClass");
            er.initCause(caught);
            throw er;
          }

          @Override
          public void onSuccess(TypeCheckedFieldClass<Integer, String> result) {
            assertNotNull(result);
            assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            finishTest();
          }
        });
  }

  public void testTypeCheckedNestedLists() {
    TypeCheckedObjectsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TypeCheckedObjectsTestSetFactory.createTypeCheckedNestedLists(),
        new AsyncCallback<TypeCheckedNestedLists>() {
          @Override
          public void onFailure(Throwable caught) {
            AssertionFailedError er =
                new AssertionFailedError("Could not serialize/deserialize TypeCheckedNestedLists");
            er.initCause(caught);
            throw er;
          }

          @Override
          public void onSuccess(TypeCheckedNestedLists result) {
            assertNotNull(result);
            assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            finishTest();
          }
        });
  }

  public void testTypeCheckedSerializer() {
    TypeCheckedObjectsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TypeCheckedObjectsTestSetFactory.createTypeCheckedGenericClass(),
        new AsyncCallback<TypeCheckedGenericClass<Integer, String>>() {
          @Override
          public void onFailure(Throwable caught) {
            AssertionFailedError er =
                new AssertionFailedError("Could not serialize/deserialize TypeCheckedGenericClass");
            er.initCause(caught);
            throw er;
          }

          @Override
          public void onSuccess(TypeCheckedGenericClass<Integer, String> result) {
            assertNotNull(result);
            assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            finishTest();
          }
        });
  }

  public void testTypeCheckedSuperSerializer() {
    TypeCheckedObjectsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TypeCheckedObjectsTestSetFactory.createTypeCheckedSuperClass(),
        new AsyncCallback<TypeCheckedSuperClass<Integer, String>>() {
          @Override
          public void onFailure(Throwable caught) {
            AssertionFailedError er =
                new AssertionFailedError("Could not serialize/deserialize TypeCheckedGenericClass");
            er.initCause(caught);
            throw er;
          }

          @Override
          public void onSuccess(TypeCheckedSuperClass<Integer, String> result) {
            assertNotNull(result);
            assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            finishTest();
          }
        });
  }

  public void testTypeUncheckedSerializer() {
    TypeCheckedObjectsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(TypeCheckedObjectsTestSetFactory.createTypeUncheckedGenericClass(),
        new AsyncCallback<TypeUncheckedGenericClass<Integer, String>>() {
          @Override
          public void onFailure(Throwable caught) {
            AssertionFailedError er =
                new AssertionFailedError("Could not serialize/deserialize TypeUncheckedGenericClass");
            er.initCause(caught);
            throw er;
          }

          @Override
          public void onSuccess(TypeUncheckedGenericClass<Integer, String> result) {
            assertNotNull(result);
            assertTrue(TypeCheckedObjectsTestSetValidator.isValid(result));
            finishTest();
          }
        });
  }

  private TypeCheckedObjectsTestServiceAsync getServiceAsync() {
    if (typeCheckedObjectsTestService == null) {
      typeCheckedObjectsTestService =
          (TypeCheckedObjectsTestServiceAsync) GWT.create(TypeCheckedObjectsTestService.class);
      ((ServiceDefTarget) typeCheckedObjectsTestService).setServiceEntryPoint(GWT
          .getModuleBaseURL()
          + "typecheckedobjects");
    }
    return typeCheckedObjectsTestService;
  }
}
