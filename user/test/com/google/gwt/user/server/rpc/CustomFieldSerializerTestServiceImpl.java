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

import com.google.gwt.user.client.rpc.CustomFieldSerializerTestService;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetValidator;
import com.google.gwt.user.client.rpc.UnserializableClass;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;

/**
 * TODO: document me.
 */
public class CustomFieldSerializerTestServiceImpl extends RemoteServiceServlet
    implements CustomFieldSerializerTestService {

  public SerializableSubclass echo(SerializableSubclass serializableClass) {
    if (!CustomFieldSerializerTestSetValidator.isValid(serializableClass)) {
      throw new RuntimeException();
    }

    return serializableClass;
  }

  public UnserializableClass echo(UnserializableClass unserializableClass) {
    if (!CustomFieldSerializerTestSetValidator.isValid(unserializableClass)) {
      throw new RuntimeException();
    }

    return unserializableClass;
  }
}
