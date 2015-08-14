/*
 * Copyright 2015 Google Inc.
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
 * Contains logics for calculating hash codes in JavaScript.
 */
public class HashCodes {

  private static int sNextHashId = 0;
  private static final String HASH_CODE_PROPERTY = "$H";

  public static int hashCodeForString(String s) {
    return StringHashCache.getHashCode(s);
  }

  public static int getIdentityHashCode(Object o) {
    if (o == null) {
      return 0;
    }
    return o instanceof String
        ?  hashCodeForString(JsUtils.unsafeCastToString(o)) : getObjectIdentityHashCode(o);
  }

  public static native int getObjectIdentityHashCode(Object o) /*-{
    return o.$H || (o.$H = @HashCodes::getNextHashId()());
  }-*/;

  /**
   * Called from JSNI. Do not change this implementation without updating:
   * <ul>
   * <li>{@link com.google.gwt.user.client.rpc.impl.SerializerBase}</li>
   * </ul>
   */
  private static int getNextHashId() {
    return ++sNextHashId;
  }
}
