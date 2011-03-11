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

import com.google.gwt.i18n.shared.GwtLocale;

/**
 * Visitor that processes messages in a single class.
 * 
 * <p>The general calling sequence of methods on this visitor and sub-visitors
 * should be, for example, for the following interface:
 * <pre>
 * interface Foo extends Messages {
 *   &#64;DefaultMessage("0")
 *   &#64;AlternateMessage({
 *     "one|MALE", "2",
 *     "one|FEMALE", "1",
 *     "one|other", "3",
 *     "other|FEMALE", "4",
 *     "other|MALE", "5",
 *   })
 *   String foo(@PluralCount int count, String where, Gender gender);
 * }
 * </pre>
 * 
 * the following sequence of calls would be generated:
 * 
 * <pre>
 * MessageInterfaceVisitor miv; 
 * miv.visitMessageInterface(msgInf, locale);
 * MessageVisitor mv = miv.visitMessage(msg, msgTrans);
 * mv.visitTranslation(["one", "FEMALE"], false, msgStyle, "1");
 * mv.visitTranslation(["one", "MALE"], false, msgStyle, "2");}
 * mv.visitTranslation(["one", "other"], false, msgStyle, "3");
 * mv.visitTranslation(["other", "FEMALE"], false, msgStyle, "4");
 * mv.visitTranslation(["other", "MALE"], false, msgStyle, "5");
 * mv.visitTranslation(["other", "other"], true, msgStyle, "0");
 * mv.endMessage(msg);
 * miv.endMessageInterface(msgIntf);
 * </pre>
 * 
 * Visitors that are interested in knowing when particular selectors and their
 * form values begin/end should see {@link FormVisitorDriver}.
 */
public interface MessageInterfaceVisitor {

  /**
   * Called after processing a message interface is complete.
   * @param msgIntf
   * 
   * @throws MessageProcessingException
   */
  void endMessageInterface(MessageInterface msgIntf)
      throws MessageProcessingException;

  /**
   * Visit a single message in the current {@link MessageInterface}.
   * 
   * @return a {@link MessageVisitor} instance which will be used to visit the
   *     supplied message, or null if no visit is required
   */
  MessageVisitor visitMessage(Message msg, MessageTranslation trans)
      throws MessageProcessingException;

  /**
   * Called at the start of processing a new message interface.
   *
   * @param msgIntf
   * @param sourceLocale the locale of source messages in this class
   * 
   * @throws MessageProcessingException
   */
  void visitMessageInterface(MessageInterface msgIntf, GwtLocale sourceLocale)
      throws MessageProcessingException;
}