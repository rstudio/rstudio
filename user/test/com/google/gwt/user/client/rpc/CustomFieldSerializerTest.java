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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

import junit.framework.AssertionFailedError;

/**
 * Tests the following scenarios.
 * <ul>
 * <li>Manually serializable types use their custom field serializer</li>
 * <li>Subtypes of manually serializable types that are not auto-serializable
 * fail to be serialized</li>
 * <li>Automatically serializable subtypes of manually serialized types can be
 * serialized</li>
 * </ul>
 */
public class CustomFieldSerializerTest extends RpcTestBase {

  private CustomFieldSerializerTestServiceAsync customFieldSerializerTestService;

  /**
   * Test that custom field serializers do not make their subclasses
   * serializable.
   */
  public void testCustomFieldSerializabilityInheritance() {
    CustomFieldSerializerTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(
        CustomFieldSerializerTestSetFactory.createUnserializableSubclass(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            assertTrue("Should be a SerializationException",
                caught instanceof SerializationException);
            finishTest();
          }

          public void onSuccess(Object result) {
            fail("Class UnserializableSubclass should not be serializable");
          }
        });
  }

  /**
   * Tests that the custom field serializers are actually called when the
   * custom field serializer does not derive from
   * {@link CustomFieldSerializer}
   */
  public void testCustomFieldSerialization() {
    CustomFieldSerializerTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(
        CustomFieldSerializerTestSetFactory.createUnserializableClass(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            AssertionFailedError er = new AssertionFailedError(
                "Class UnserializableClass should be serializable because it has a custom field serializer");
            er.initCause(caught);
            throw er;
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(CustomFieldSerializerTestSetValidator.isValid((ManuallySerializedClass) result));
            finishTest();
          }
        });
  }

  /**
   * Test that custom serializers that call readObject() inside instantiate (as
   * is required for most immutable classes) work.
   * 
   * This also checks that custom <code>instantiate</code> works when the
   * custom serializer does not implement {@link CustomFieldSerializer}.
   */
  public void testSerializableImmutables() {
    CustomFieldSerializerTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(
        CustomFieldSerializerTestSetFactory.createSerializableImmutablesArray(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            AssertionFailedError er = new AssertionFailedError(
                "Could not serialize/deserialize immutable classes");
            er.initCause(caught);
            throw er;
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(CustomFieldSerializerTestSetValidator.isValid((ManuallySerializedImmutableClass[]) result));
            finishTest();
          }
        });
  }

  /**
   * Test that serializable subclasses of classes that have custom field
   * serializers serialize and deserialize correctly.
   */
  public void testSerializableSubclasses() {
    CustomFieldSerializerTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(
        CustomFieldSerializerTestSetFactory.createSerializableSubclass(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            AssertionFailedError er = new AssertionFailedError(
                "Class SerializableSubclass should be serializable automatically");
            er.initCause(caught);
            throw er;
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(CustomFieldSerializerTestSetValidator.isValid((SerializableSubclass) result));
            finishTest();
          }
        });
  }

  private CustomFieldSerializerTestServiceAsync getServiceAsync() {
    if (customFieldSerializerTestService == null) {
      customFieldSerializerTestService = (CustomFieldSerializerTestServiceAsync) GWT.create(CustomFieldSerializerTestService.class);
      ((ServiceDefTarget) customFieldSerializerTestService).setServiceEntryPoint(GWT.getModuleBaseURL()
          + "customfieldserializers");
    }
    return customFieldSerializerTestService;
  }
}
