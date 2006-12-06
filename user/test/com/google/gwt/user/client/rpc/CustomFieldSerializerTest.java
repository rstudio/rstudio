// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

public class CustomFieldSerializerTest extends TypeSerializerWorkAround {
  private static final int TEST_DELAY = 5000;
  
  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  /**
   * Test that custom field serializers do not make their subclasses
   * serializable
   */
  public void testCustomFieldSerializabilityInheritance() {
    delayTestFinish(TEST_DELAY);

    try {
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
    } catch (RuntimeException ex) {
      workAroundTypeSerializerBug(ex);
    }
  }

  /**
   * Tests that the custom field serializers are actually called
   */
  public void testCustomFieldSerialization() {
    delayTestFinish(TEST_DELAY);

    try {
      CustomFieldSerializerTestServiceAsync service = getServiceAsync();
      service.echo(
        CustomFieldSerializerTestSetFactory.createUnserializableClass(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            fail("Class UnserializableClass should be serializable because it has a custom field serializer");
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(CustomFieldSerializerTestSetValidator.isValid((UnserializableClass) result));
            finishTest();
          }
        });
    } catch (RuntimeException ex) {
      workAroundTypeSerializerBug(ex);
    }
  }

  /*
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

  private CustomFieldSerializerTestServiceAsync customFieldSerializerTestService;
}
