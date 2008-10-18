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

import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AbstractClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AnonymousClassInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.Circle;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.JavaSerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.MySerializableInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;

/**
 * Service interface used by the
 * {@link com.google.gwt.user.client.rpc.InheritanceTest InheritanceTest} unit
 * test.
 */
public interface InheritanceTestService extends RemoteService {
  AnonymousClassInterface echo(AnonymousClassInterface serializable);

  Circle echo(Circle circle);

  JavaSerializableClass echo(JavaSerializableClass javaSerializableClass);

  SerializableClass echo(SerializableClass serializableClass);

  SerializableClassWithTransientField echo(
      SerializableClassWithTransientField serializableClass);

  /**
   * Used to test <a
   * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=1163">Issue
   * 1163</a>.
   * 
   * @return
   */
  AbstractClass getAbstractClass();

  /**
   * Used to test <a
   * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=1163">Issue
   * 1163</a>.
   * 
   * @return
   */
  MySerializableInterface getSerializableInterface1();

  /**
   * Used to test <a
   * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=1163">Issue
   * 1163</a>.
   * 
   * @return
   */
  MySerializableInterface getSerializableInterface2();

  SerializableClass getUnserializableClass();
}
