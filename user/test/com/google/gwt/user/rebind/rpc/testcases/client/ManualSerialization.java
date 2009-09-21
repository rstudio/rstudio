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
package com.google.gwt.user.rebind.rpc.testcases.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Used to test that the
 * {@link com.google.gwt.user.rebind.rpc.SerializableTypeOracleBuilder SerializableTypeOracleBuilder}
 * will not fail if a manually serialized type has a field that is not 
 * serializables.
 */
@SuppressWarnings("rpc-validation")
public interface ManualSerialization extends RemoteService {
  /**
   * Manually serialized.  Field b is not serializable.
   */
  class A {
    B b;
  }
  
  /**
   * Custom field serializer for {@link A}.
   */
  class A_CustomFieldSerializer {
    public static void serialize(SerializationStreamWriter ssw, A instance) {
    }
    
    public static void deserialize(SerializationStreamReader ssr, A instance) {
    }
  }
  
  /**
   * Not automatically serializable.
   */
  class B implements IsSerializable {
    Object obj;
  }
  
  A getA();
}
