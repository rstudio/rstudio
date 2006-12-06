// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;

public class ObjectGraphTest extends GWTTestCase {
  private static final int TEST_DELAY = 5000;
  
  public String getModuleName() {
    return "com.google.gwt.user.RPCSuite";
  }

  public void testAcyclicGraph() {
    delayTestFinish(TEST_DELAY);

    ObjectGraphTestServiceAsync service = getServiceAsync();
    service.echo_AcyclicGraph(TestSetFactory.createAcyclicGraph(),
      new AsyncCallback() {
        public void onFailure(Throwable caught) {
          fail(caught.toString());
        }

        public void onSuccess(Object result) {
          assertNotNull(result);
          assertTrue(TestSetValidator.isValidAcyclicGraph((SerializableDoublyLinkedNode) result));
          finishTest();
        }
      });
  }

  public void testComplexCyclicGraph() {
    delayTestFinish(TEST_DELAY);

    ObjectGraphTestServiceAsync service = getServiceAsync();
    service.echo_ComplexCyclicGraph(TestSetFactory.createComplexCyclicGraph(),
      new AsyncCallback() {
        public void onFailure(Throwable caught) {
          fail(caught.toString());
        }

        public void onSuccess(Object result) {
          assertNotNull(result);
          assertTrue(TestSetValidator.isValidComplexCyclicGraph((SerializableDoublyLinkedNode) result));
          finishTest();
        }
      });
  }

  public void testComplexCyclicGraph2() {
    delayTestFinish(TEST_DELAY);

    ObjectGraphTestServiceAsync service = getServiceAsync();
    final SerializableDoublyLinkedNode node = TestSetFactory.createComplexCyclicGraph(); 
    service.echo_ComplexCyclicGraph(node, node,
      new AsyncCallback() {
        public void onFailure(Throwable caught) {
          fail(caught.toString());
        }

        public void onSuccess(Object result) {
          assertNotNull(result);
          assertTrue(TestSetValidator.isValidComplexCyclicGraph((SerializableDoublyLinkedNode) result));
          finishTest();
        }
      });
  }

  public void testTrivialCyclicGraph() {
    delayTestFinish(TEST_DELAY);

    ObjectGraphTestServiceAsync service = getServiceAsync();
    service.echo_TrivialCyclicGraph(TestSetFactory.createTrivialCyclicGraph(),
      new AsyncCallback() {
        public void onFailure(Throwable caught) {
          fail(caught.toString());
        }

        public void onSuccess(Object result) {
          assertNotNull(result);
          assertTrue(TestSetValidator.isValidTrivialCyclicGraph((SerializableDoublyLinkedNode) result));
          finishTest();
        }
      });
  }

  private ObjectGraphTestServiceAsync getServiceAsync() {
    if (objectGraphTestService == null) {
      objectGraphTestService = (ObjectGraphTestServiceAsync) GWT.create(ObjectGraphTestService.class);
      ((ServiceDefTarget) objectGraphTestService).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "objectgraphs");
    }
    return objectGraphTestService;
  }

  private ObjectGraphTestServiceAsync objectGraphTestService;
}
