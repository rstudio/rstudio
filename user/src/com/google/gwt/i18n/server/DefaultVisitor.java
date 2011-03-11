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
import com.google.gwt.i18n.shared.GwtLocale;

/**
 * A default visitor implementation which does nothing and returns itself
 * for sub-visitors.  Most implementations should subclass this class and
 * override individual methods as needed for future compatibility.
 */
public class DefaultVisitor implements MessageInterfaceVisitor, MessageVisitor {

  public void endMessage(Message msg, MessageTranslation trans)
      throws MessageProcessingException {
    // do nothing by default
  }

  public void endMessageInterface(MessageInterface msgIntf) 
      throws MessageProcessingException {
    // do nothing by default
  }

  public MessageVisitor visitMessage(Message msg, MessageTranslation trans)
      throws MessageProcessingException {
    return this;
  }

  public void visitMessageInterface(MessageInterface msgIntf,
      GwtLocale sourceLocale) throws MessageProcessingException {
    // do nothing by default
  }

  public void visitTranslation(String[] formNames, boolean isDefault,
      MessageStyle style, String msg) throws MessageProcessingException {
    // do nothing by default
  }
}