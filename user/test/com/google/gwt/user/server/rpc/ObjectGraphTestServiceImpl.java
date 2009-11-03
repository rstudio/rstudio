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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.ObjectGraphTestService;
import com.google.gwt.user.client.rpc.TestSetValidator;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableGraphWithCFS;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializablePrivateNoArg;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableWithTwoArrays;

/**
 * TODO: document me.
 */
public class ObjectGraphTestServiceImpl extends HybridServiceServlet implements
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

  public SerializableGraphWithCFS echo_ComplexCyclicGraphWithCFS(
      SerializableGraphWithCFS root) {
    if (!TestSetValidator.isValidComplexCyclicGraphWithCFS(root)) {
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

  public SerializablePrivateNoArg echo_PrivateNoArg(
      SerializablePrivateNoArg node) {
    if (!TestSetValidator.isValid(node)) {
      throw new RuntimeException();
    }

    return node;
  }

  public SerializableWithTwoArrays echo_SerializableWithTwoArrays(
      SerializableWithTwoArrays node) {
    if (!TestSetValidator.isValid(node)) {
      throw new RuntimeException();
    }

    return node;
  }

  public SerializableDoublyLinkedNode echo_ComplexCyclicGraph(
      SerializableDoublyLinkedNode node1, SerializableDoublyLinkedNode node2) {
    if (node1 != node2) {
      throw new RuntimeException();
    }

    return node1;
  }
}
