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

import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.lang.annotation.Annotation;

/**
 * Base implementation of {@link MessageInterface}.
 */
public abstract class AbstractMessageInterface implements MessageInterface {

  protected final GwtLocaleFactory factory;

  public AbstractMessageInterface(GwtLocaleFactory factory) {
    this.factory = factory;
  }

  public void accept(MessageInterfaceVisitor cv)
      throws MessageProcessingException {
    accept(cv, factory.getDefault());
  }

  public void accept(MessageInterfaceVisitor cv, GwtLocale locale)
      throws MessageProcessingException {
    String defaultLocale = DefaultLocale.DEFAULT_LOCALE;
    DefaultLocale defLocaleAnnot = getAnnotation(DefaultLocale.class);
    if (defLocaleAnnot != null) {
      defaultLocale = defLocaleAnnot.value();
    }
    GwtLocale sourceLocale;
    try {
      sourceLocale = factory.fromString(defaultLocale);
    } catch (IllegalArgumentException e) {
      // ignore specified default
      sourceLocale = factory.fromString(DefaultLocale.DEFAULT_LOCALE);
    }
    cv.visitMessageInterface(this, sourceLocale);
    for (Message msg : getMessages()) {
      MessageTranslation trans = msg.getTranslation(locale);
      MessageVisitor mv = cv.visitMessage(msg, trans != null ? trans : msg);
      if (mv != null) {
        msg.accept(mv);
      }
    }
    cv.endMessageInterface(this);
  }

  public abstract <A extends Annotation> A getAnnotation(Class<A> annotClass);

  public abstract String getClassName();

  /**
   * Gets the list of messages defined in this interface, including inherited.
   * These must be returned in order of {@link Message#getKey()}.
   *
   * @return an iteration of {@link Message} instances
   * @throws MessageProcessingException
   */
  public abstract Iterable<Message> getMessages()
      throws MessageProcessingException;

  public abstract String getPackageName();

  public abstract String getQualifiedName();

  public abstract boolean isAnnotationPresent(
      Class<? extends Annotation> annotClass);
 
  @Override
  public String toString() {
    return getClassName();
  }
}
