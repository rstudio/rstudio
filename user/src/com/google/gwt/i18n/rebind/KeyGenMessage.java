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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.i18n.client.Constants.DefaultStringMapValue;
import com.google.gwt.i18n.client.LocalizableResource.Description;
import com.google.gwt.i18n.client.LocalizableResource.Meaning;
import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.google.gwt.i18n.server.Message;
import com.google.gwt.i18n.server.MessageFormatUtils.MessageStyle;
import com.google.gwt.i18n.server.MessageInterface;
import com.google.gwt.i18n.server.MessageProcessingException;
import com.google.gwt.i18n.server.MessageTranslation;
import com.google.gwt.i18n.server.MessageUtils;
import com.google.gwt.i18n.server.MessageVisitor;
import com.google.gwt.i18n.server.Parameter;
import com.google.gwt.i18n.server.Type;
import com.google.gwt.i18n.shared.GwtLocale;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Implementation of {@link Message} only suitable for use by key generators.
 * See the restrictions in
 * {@link com.google.gwt.i18n.server.KeyGenerator#generateKey(Message)}.
 */
class KeyGenMessage implements Message {

  private JMethod method;

  /**
   * @param method
   */
  public KeyGenMessage(JMethod method) {
    this.method = method;
  }

  public void accept(MessageVisitor v) throws MessageProcessingException {
    throw new MessageProcessingException("unsupported");
  }

  public void accept(MessageVisitor v, GwtLocale locale)
      throws MessageProcessingException {
    throw new MessageProcessingException("unsupported");
  }

  public int compareTo(Message o) {
    return 0;
  }

  public Iterable<AlternateFormMapping> getAllMessageForms() {
    return null;
  }

  public <A extends Annotation> A getAnnotation(Class<A> annotClass) {
    A annot = method.getAnnotation(annotClass);
    if (annot != null) {
      return annot;
    }
    return method.getEnclosingType().findAnnotationInTypeHierarchy(annotClass);
  }

  public String getDefaultMessage() {
    if (isAnnotationPresent(DefaultMessage.class)) {
      DefaultMessage annot = getAnnotation(DefaultMessage.class);
      return annot.value();
    } else if (isAnnotationPresent(DefaultStringMapValue.class)) {
      DefaultStringMapValue annot = getAnnotation(DefaultStringMapValue.class);
      String[] keyValues = annot.value();
      StringBuilder buf = new StringBuilder();
      boolean needComma = false;
      for (int i = 0; i < keyValues.length; i += 2) {
        if (needComma) {
          buf.append(',');
        } else {
          needComma = true;
        }
        buf.append(MessageUtils.quoteComma(keyValues[i]));
      }
      return buf.toString();
    } else {
      return MessageUtils.getConstantsDefaultValue(this);
    }
  }

  public String getDescription() {
    Description annot = getAnnotation(Description.class);
    return annot != null ? annot.value() : null;
  }

  public String getKey() {
    return null;
  }

  public GwtLocale getMatchedLocale() {
    return null;
  }

  public String getMeaning() {
    Meaning meaningAnnot = getAnnotation(Meaning.class);
    return meaningAnnot != null ? meaningAnnot.value() : null;
  }

  public MessageInterface getMessageInterface() {
    return new KeyGenMessageInterface(method.getEnclosingType());
  }

  public MessageStyle getMessageStyle() {
    return isAnnotationPresent(DefaultMessage.class) ?
      MessageStyle.MESSAGE_FORMAT : MessageStyle.PLAIN;
  }

  public String getMethodName() {
    return method.getName();
  }

  public List<Parameter> getParameters() {
    return null;
  }

  public Type getReturnType() {
    return null;
  }

  public int[] getSelectorParameterIndices() {
    return null;
  }

  public MessageTranslation getTranslation(GwtLocale locale) {
    return null;
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotClass) {
    return getAnnotation(annotClass) != null;
  }

  public boolean isVarArgs() {
    return false;
  }
}