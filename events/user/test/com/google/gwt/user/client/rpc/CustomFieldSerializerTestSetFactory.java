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

import java.util.Date;

/**
 * Generated test data for the
 * {@link com.google.gwt.user.client.rpc.CustomFieldSerializerTest CustomFieldSerializerTest}
 * unit test.
 */
public class CustomFieldSerializerTestSetFactory {

  /**
   * Used to test an automatically serializable subclass of a manually
   * serializable subtype.
   */
  public static class SerializableSubclass extends ManuallySerializedClass
      implements IsSerializable {
    private int d = 4;

    public int getD() {
      return d;
    }

    public void setD(int d) {
      this.d = d;
    }
  }

  /**
   * Used to test a subclass of a manually serializable type that is not
   * automatically or manually serializable.
   */
  public static class UnserializableSubclass extends ManuallySerializedClass {
  }

  public static ManuallySerializedImmutableClass[] createSerializableImmutablesArray() {
    ManuallySerializedImmutableClass immutable = new ManuallySerializedImmutableClass(
        new Date(12345L), new Date(54321L));
    return new ManuallySerializedImmutableClass[] {immutable, immutable};
  }

  public static SerializableSubclass createSerializableSubclass() {
    return new SerializableSubclass();
  }

  public static ManuallySerializedClass createUnserializableClass() {
    return new ManuallySerializedClass();
  }

  public static UnserializableSubclass createUnserializableSubclass() {
    return new UnserializableSubclass();
  }
}
