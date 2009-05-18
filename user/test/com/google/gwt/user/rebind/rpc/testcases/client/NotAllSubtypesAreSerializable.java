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

/**
 * Used to test that the
 * {@link com.google.gwt.user.rebind.rpc.SerializableTypeOracleBuilder SerializableTypeOracleBuilder}
 * will not fail if all of the subtypes of a type used in a service
 * method signature are not serializable.
 */
public interface NotAllSubtypesAreSerializable {
  
  /**
   * Not serializable.
   */
  class A {
  }
  
  /**
   * Auto serializable.
   */
  class B extends A implements IsSerializable {
  }
  
  /**
   * Not serializable due to Object field.
   */
  class C extends B {
    Object field;
  }
 
  /**
   * Reintroduces auto serialization.
   */
  class D extends C implements IsSerializable {
  }
  
  /**
   * Not serializable.
   */
  class E extends A {
  }
  
  A getA();
}  
