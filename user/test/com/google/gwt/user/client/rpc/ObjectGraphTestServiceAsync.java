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

import com.google.gwt.user.client.rpc.TestSetFactory.SerializableDoublyLinkedNode;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableGraphWithCFS;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializablePrivateNoArg;
import com.google.gwt.user.client.rpc.TestSetFactory.SerializableWithTwoArrays;

/**
 * TODO: document me.
 */
public interface ObjectGraphTestServiceAsync {
  void echo_AcyclicGraph(SerializableDoublyLinkedNode node,
      AsyncCallback callback);

  void echo_ComplexCyclicGraph(SerializableDoublyLinkedNode node,
      AsyncCallback callback);

  void echo_ComplexCyclicGraph(SerializableDoublyLinkedNode node1,
      SerializableDoublyLinkedNode node2, AsyncCallback callback);

  void echo_PrivateNoArg(SerializablePrivateNoArg node, AsyncCallback callback);

  void echo_SerializableWithTwoArrays(SerializableWithTwoArrays node,
      AsyncCallback callback);

  void echo_TrivialCyclicGraph(SerializableDoublyLinkedNode node,
      AsyncCallback callback);

  void echo_ComplexCyclicGraphWithCFS(
      SerializableGraphWithCFS createComplexCyclicGraphWithArrays,
      AsyncCallback<SerializableGraphWithCFS> asyncCallback);
}
