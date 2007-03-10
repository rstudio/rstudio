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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.InheritanceTestSetFactory;
import com.google.gwt.user.client.rpc.InheritanceTestSetValidator;
import com.google.gwt.user.client.rpc.InheritanceTestService;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AnonymousClassInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableSubclass;

/**
 * TODO: document me.
 */
public class InheritanceTestServiceImpl extends RemoteServiceServlet implements
    InheritanceTestService {

  public AnonymousClassInterface echo(AnonymousClassInterface serializable) {
    return serializable;
  }

  public IsSerializable echo(IsSerializable serializable) {
    return serializable;
  }

  public SerializableClass echo(SerializableClass serializableClass) {
    if (!InheritanceTestSetValidator.isValid(serializableClass)) {
      throw new RuntimeException();
    }

    return serializableClass;
  }

  public SerializableClassWithTransientField echo(
      SerializableClassWithTransientField serializableClass) {
    if (!InheritanceTestSetValidator.isValid(serializableClass)) {
      throw new RuntimeException();
    }

    // this should not be sent back across the wire, the client will verify
    // that this is true
    serializableClass.setObj("hello");

    return serializableClass;
  }

  public SerializableSubclass echo(SerializableSubclass serializableSubclass) {
    if (!InheritanceTestSetValidator.isValid(serializableSubclass)) {
      throw new RuntimeException();
    }

    return serializableSubclass;
  }

  public SerializableClass getUnserializableClass() {
    return new InheritanceTestSetFactory.SerializableClassWithUnserializableClassField();
  }
}
