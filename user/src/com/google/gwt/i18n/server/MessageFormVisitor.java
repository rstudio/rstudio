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
 * A visitor which is used to keep track of nesting of individual selectors and
 * their forms within one message.
 * 
 * <p>See {@link FormVisitorDriver} for how this would be used.
 */
public interface MessageFormVisitor {

  /**
   * Called before any messages underneath this form are processed.
   *
   * @param level
   * @param formName
   * @throws MessageProcessingException
   */
  void beginForm(int level, String formName) throws MessageProcessingException;

  /**
   * Called before the first form for a given selector is processed.
   *
   * @param level indicates selector depth, and ranged from 0 to
   *     {@link Message#getSelectorParameterIndices()}.length - 1
   * @param selectorArg may be null in the case of a Map<String, String> value
   * @throws MessageProcessingException
   */
  void beginSelector(int level, Parameter selectorArg)
      throws MessageProcessingException;

  /**
   * Called after all messages underneath this form have been processed.
   *
   * @param level
   * @param formName
   * @throws MessageProcessingException
   */
  void endForm(int level, String formName) throws MessageProcessingException;

  /**
   * Called after the last form of a givne selector has been processed.
   *
   * @param level indicates selector depth, and ranged from 0 to
   *     Message.getSelectorParameterIndices().length - 1
   * @param selectorArg may be null in the case of a Map<String, String> value
   * @throws MessageProcessingException
   */
  void endSelector(int level, Parameter selectorArg)
      throws MessageProcessingException;
}
