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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableGraphWithCFS;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializablePrivateNoArg;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableWithTwoArrays;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStream;

/**
 * TODO: document me.
 */
public class ObjectGraphTest extends RpcTestBase {

  public void testAcyclicGraph() {
    ObjectGraphTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_AcyclicGraph(TestSetFactory.createAcyclicGraph(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidAcyclicGraph((SerializableDoublyLinkedNode) result));
            finishTest();
          }
        });
  }

  public void testComplexCyclicGraph() {
    ObjectGraphTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_ComplexCyclicGraph(TestSetFactory.createComplexCyclicGraph(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidComplexCyclicGraph((SerializableDoublyLinkedNode) result));
            finishTest();
          }
        });
  }

  public void testComplexCyclicGraphWithCFS() {
    ObjectGraphTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_ComplexCyclicGraphWithCFS(
        TestSetFactory.createComplexCyclicGraphWithCFS(),
        new AsyncCallback<SerializableGraphWithCFS>() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(SerializableGraphWithCFS result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidComplexCyclicGraphWithCFS(result));
            finishTest();
          }
        });
  }

  public void testComplexCyclicGraph2() {
    ObjectGraphTestServiceAsync service = getServiceAsync();
    final SerializableDoublyLinkedNode node = TestSetFactory.createComplexCyclicGraph();
    delayTestFinishForRpc();
    service.echo_ComplexCyclicGraph(node, node, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValidComplexCyclicGraph((SerializableDoublyLinkedNode) result));
        finishTest();
      }
    });
  }

  public void testDoublyReferencedArray() {
    ObjectGraphTestServiceAsync service = getServiceAsync();
    final SerializableWithTwoArrays node = TestSetFactory.createDoublyReferencedArray();
    delayTestFinishForRpc();
    service.echo_SerializableWithTwoArrays(node, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValid((SerializableWithTwoArrays) result));
        finishTest();
      }
    });
  }

  public void testElision() throws SerializationException {
    ObjectGraphTestServiceAsync async = getServiceAsync();

    SerializationStreamWriter writer = ((SerializationStreamFactory) async).createStreamWriter();
    AbstractSerializationStream stream = (AbstractSerializationStream) writer;
    assertEquals("Missing flag", expectedObfuscationState(),
        stream.hasFlags(AbstractSerializationStream.FLAG_ELIDE_TYPE_NAMES));

    SerializableDoublyLinkedNode node = new SerializableDoublyLinkedNode();
    writer.writeObject(node);
    String s = writer.toString();

    // Don't use class.getName() due to conflict with removal of type names
    assertEquals("Checking for SerializableDoublyLinkedNode",
        expectedObfuscationState(), !s.contains("SerializableDoublyLinkedNode"));
  }

  public void testPrivateNoArg() {
    ObjectGraphTestServiceAsync service = getServiceAsync();
    final SerializablePrivateNoArg node = TestSetFactory.createPrivateNoArg();
    delayTestFinishForRpc();
    service.echo_PrivateNoArg(node, new AsyncCallback() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Object result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValid((SerializablePrivateNoArg) result));
        finishTest();
      }
    });
  }

  public void testTrivialCyclicGraph() {
    ObjectGraphTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_TrivialCyclicGraph(TestSetFactory.createTrivialCyclicGraph(),
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(Object result) {
            assertNotNull(result);
            assertTrue(TestSetValidator.isValidTrivialCyclicGraph((SerializableDoublyLinkedNode) result));
            finishTest();
          }
        });
  }

  protected boolean expectedObfuscationState() {
    return false;
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
