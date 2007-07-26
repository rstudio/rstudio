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
 * will handle covariant arrays correctly. For example, the service interface
 * below should be able to handle the following covariant array types: AA[],
 * A[], BB[], B[], CC[], C[], DD[], D[]. Notice that E[] should not be included.
 */
public interface CovariantArrays {
  /**
   * Not serializable.
   */
  class A implements DD {
  }

  /**
   * Not serializable.
   */
  interface AA {
  }

  /**
   * Auto serializable.
   */
  class B extends A implements IsSerializable {
  }

  /**
   * Not serializable.
   */
  interface BB extends AA {
  }

  /**
   * Not auto serializable due to bad field
   */
  class C extends B {
    Object field;
  }

  /**
   * Not serializable.
   */
  interface CC extends AA {
  }

  /**
   * Auto serializable.
   */
  class D extends C implements IsSerializable {
  }

  /**
   * Not serializable.
   */
  interface DD extends BB, CC {
  }

  /**
   * Not auto serializable because super class is not.
   */
  class E extends C {
  }

  AA[] getAs();
}
