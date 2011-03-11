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

import com.google.gwt.i18n.server.MessageFormatUtils.MessageStyle;

/**
 * A visitor which is used to process individual messages.
 */
public interface MessageVisitor {

  /**
   * Called after processing a message is complete.
   * 
   * @param msg {@link Message} instance
   * @param trans message translation, which may be the same as {@code msg}
   * @throws MessageProcessingException
   */
  void endMessage(Message msg, MessageTranslation trans)
      throws MessageProcessingException;

  /**
   * Called to process one variant of a message.
   * 
   * <p>Note that the default message will always be passed in a call to this
   * method, even if there are no selectors.
   * @param formNames array of form names, one for each selector
   * @param isDefault true if this is the default message (ie, all forms are
   *     "other")
   * @param style
   * @param msg
   * 
   * @throws MessageProcessingException
   */
  void visitTranslation(String[] formNames, boolean isDefault,
      MessageStyle style, String msg) throws MessageProcessingException;
}