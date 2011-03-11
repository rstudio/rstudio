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
package com.google.gwt.i18n.rebind.keygen;

/**
 * Interface for key generation algorithms used by <code>LocalizableResource</code>
 * implementations.
 * 
 * Implementations of this interface are executed at compile time, and
 * therefore must not contain any JSNI code.
 *
 * @deprecated Use {@link com.google.gwt.i18n.server.KeyGenerator} instead.
 */
@Deprecated
public interface KeyGenerator {

  /**
   * Generates a key for a given method with its default text and meaning.
   * 
   * @param className fully qualified name of the Messages/Constants subinterface
   * @param methodName name of the method in the subinterface
   * @param text default text to use if no translation is available, which may be null
   *     if no default text is supplied
   * @param meaning extra text explaining the meaning of the text, or null if not needed
   * @return the lookup key as a string or null if the key cannot be computed (for
   *     example, if text is null but this generator requires it)
   */
  String generateKey(String className, String methodName, String text,
      String meaning);
}