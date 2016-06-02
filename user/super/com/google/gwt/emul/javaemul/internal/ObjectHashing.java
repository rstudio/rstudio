/*
 * Copyright 2016 Google Inc.
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
package javaemul.internal;

/**
 * Utility class to generate hash code for Objects.
 */
class ObjectHashing {

  private static int nextHashId = 0;
  private static final String HASH_CODE_PROPERTY = "$H";

  public static native int getHashCode(Object o) /*-{
    return o.$H || (o.$H = @ObjectHashing::getNextHashId()());
  }-*/;

  /**
   * Called from JSNI. Do not change this implementation without updating:
   * <ul>
   * <li>{@link com.google.gwt.user.client.rpc.impl.SerializerBase}</li>
   * </ul>
   */
  private static int getNextHashId() {
    return ++nextHashId;
  }
}
