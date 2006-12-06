// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.ObjectGraphTestService;
import com.google.gwt.user.client.rpc.TestSetValidator;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;

public class ObjectGraphTestServiceImpl extends RemoteServiceServlet implements
    ObjectGraphTestService {

  public SerializableDoublyLinkedNode echo_AcyclicGraph(
      SerializableDoublyLinkedNode root) {
    if (!TestSetValidator.isValidAcyclicGraph(root)) {
      throw new RuntimeException();
    }

    return root;
  }

  public SerializableDoublyLinkedNode echo_ComplexCyclicGraph(
      SerializableDoublyLinkedNode root) {
    if (!TestSetValidator.isValidComplexCyclicGraph(root)) {
      throw new RuntimeException();
    }

    return root;
  }

  public SerializableDoublyLinkedNode echo_TrivialCyclicGraph(
      SerializableDoublyLinkedNode root) {
    if (!TestSetValidator.isValidTrivialCyclicGraph(root)) {
      throw new RuntimeException();
    }

    return root;
  }

  public SerializableDoublyLinkedNode echo_ComplexCyclicGraph(SerializableDoublyLinkedNode node1, SerializableDoublyLinkedNode node2) {
    if (node1 != node2) {
      throw new RuntimeException();
    }
    
    return node1;
  }
}
