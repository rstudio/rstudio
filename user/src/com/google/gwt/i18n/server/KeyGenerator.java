/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.i18n.server;

/**
 * Interface for key generation algorithms used by <code>LocalizableResource</code>
 * implementations.
 * 
 * Implementations of this interface are executed at compile time, and
 * therefore must not contain any JSNI code.
 */
public interface KeyGenerator {

  /**
   * Generates a key for a given method with its default text and meaning.
   * 
   * <p>
   * NOTE: currently the MessageInterface and Message implementations passed to
   * the key generator are incomplete as they are wrappers around the existing
   * functionality. Only {@link Message#getMessageInterface()},
   * {@link Message#getMethodName()}, {@link Message#getDefaultMessage()},
   * {@link Message#getDescription()}, {@link Message#getMeaning()}, and
   * {@link Message#getMessageStyle()} (and only
   * {@link com.google.gwt.i18n.server.MessageInterface#getClassName()},
   * {@link com.google.gwt.i18n.server.MessageInterface#getPackageName()}, and
   * {@link com.google.gwt.i18n.server.MessageInterface#getQualifiedName()} on
   * the {@link com.google.gwt.i18n.server.MessageInterface MessageInterface}
   * instance returned from {@link Message#getMessageInterface()}) may be relied
   * upon until the i18n generator is updated.
   * 
   * @param msg the message to generate a key for
   * @return the lookup key as a string or null if the key cannot be computed
   *         (for example, if text is null but this generator requires it)
   */
  String generateKey(Message msg);
}