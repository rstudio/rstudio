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

import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AnonymousClassInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.Circle;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.JavaSerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;

/**
 * Async service interface used by the
 * {@link com.google.gwt.user.client.rpc.InheritanceTest InheritanceTest} unit
 * test.
 */
public interface InheritanceTestServiceAsync {
  void echo(AnonymousClassInterface serializable, AsyncCallback callback);

  void echo(Circle circle, AsyncCallback callback);
  
  void echo(JavaSerializableClass javaSerializableClass, AsyncCallback callback);
  
  void echo(SerializableClass serializableClass, AsyncCallback callback);

  void echo(SerializableClassWithTransientField serializableClass,
      AsyncCallback callback);

  void getAbstractClass(AsyncCallback callback);
  
  void getSerializableInterface1(AsyncCallback callback);
  
  void getSerializableInterface2(AsyncCallback callback);
  
  void getUnserializableClass(AsyncCallback callback);
}
