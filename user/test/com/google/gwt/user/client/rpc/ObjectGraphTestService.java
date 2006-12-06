// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;

public interface ObjectGraphTestService extends RemoteService {

  SerializableDoublyLinkedNode echo_AcyclicGraph(
      SerializableDoublyLinkedNode node);

  SerializableDoublyLinkedNode echo_ComplexCyclicGraph(
      SerializableDoublyLinkedNode node);

  SerializableDoublyLinkedNode echo_ComplexCyclicGraph(
      SerializableDoublyLinkedNode node1, SerializableDoublyLinkedNode node2);
  
  SerializableDoublyLinkedNode echo_TrivialCyclicGraph(
      SerializableDoublyLinkedNode node);
}
