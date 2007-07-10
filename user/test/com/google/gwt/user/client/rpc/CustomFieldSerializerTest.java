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
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

/**
 * Tests the following scenarios:
 * <ul>
 * <li>Manually serializable types use their custom field serializer</li>
 * <li>Subtypes of manually serializable types that are not auto-serializable
 * fail to be serialized</li>
 * <li>Automatically serializable subtypes of manually serialized types can be
 * serialized</li>
 * </ul>
 */
public class CustomFieldSerializerTest extends GWTTestCase {
  private static final int TEST_DELAY = 5000;

  private CustomFieldSerializerTestServiceAsync customFieldSerializerTestService;

  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  /**
   * Test that custom field serializers do not make their subclasses
   * serializable
   */
  public void testCustomFieldSerializabilityInheritance() {
    delayTestFinish(TEST_DELAY);

    CustomFieldSerializerTestServiceAsync service = getServiceAsync();
    service.echo(
        CustomFieldSerializerTestSetFactory.createUnserializableSubclass(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            finishTest();
          }

          public void onSuccess(Object result) {
            fail("Class UnserializableSubclass should not be serializable");
          }
        });
  }

  /**
   * Tests that the custom field serializers are actually called
   */
  public void testCustomFieldSerialization() {
    delayTestFinish(TEST_DELAY);

    CustomFieldSerializerTestServiceAsync service = getServiceAsync();
    service.echo(
        CustomFieldSerializerTestSetFactory.createUnserializableClass(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            fail("Class UnserializableClass should be serializable because it has a custom field serializer");
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(CustomFieldSerializerTestSetValidator.isValid((ManuallySerializedClass) result));
            finishTest();
          }
        });
  }

  /**
   * Test that serializable subclasses of classes that have custom field
   * serializers serialize and deserialize correctly
   */
  public void testSerializableSubclasses() {
    delayTestFinish(TEST_DELAY);

    CustomFieldSerializerTestServiceAsync service = getServiceAsync();
    service.echo(
        CustomFieldSerializerTestSetFactory.createSerializableSubclass(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            fail("Class UnserializableClass should be serializable because it has a custom field serializer");
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
