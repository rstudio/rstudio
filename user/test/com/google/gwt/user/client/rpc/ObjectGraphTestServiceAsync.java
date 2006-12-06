// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;

public interface ObjectGraphTestServiceAsync {
  void echo_AcyclicGraph(SerializableDoublyLinkedNode node,
      AsyncCallback callback);

  void echo_ComplexCyclicGraph(SerializableDoublyLinkedNode node,
      AsyncCallback callback);
  
  void echo_ComplexCyclicGraph(
      SerializableDoublyLinkedNode node1, SerializableDoublyLinkedNode node2, AsyncCallback callback);

  void echo_TrivialCyclicGraph(SerializableDoublyLinkedNode node,
      AsyncCallback callback);
}
