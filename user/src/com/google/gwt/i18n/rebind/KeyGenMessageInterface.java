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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.i18n.server.MessageInterface;
import com.google.gwt.i18n.server.MessageInterfaceVisitor;
import com.google.gwt.i18n.server.MessageProcessingException;
import com.google.gwt.i18n.shared.GwtLocale;

import java.lang.annotation.Annotation;

/**
 * Simple {@link MessageInterface} implementation only suitable for use with
 * {@link com.google.gwt.i18n.server.KeyGenerator KeyGenerator}.
 */
class KeyGenMessageInterface implements MessageInterface {

  private JClassType type;

  /**
   * @param type
   */
  public KeyGenMessageInterface(JClassType type) {
    this.type = type;
  }

  public void accept(MessageInterfaceVisitor cv)
      throws MessageProcessingException {
    throw new MessageProcessingException("unsupported");
  }

  public void accept(MessageInterfaceVisitor cv, GwtLocale locale)
      throws MessageProcessingException {
    throw new MessageProcessingException("unsupported");
  }

  public <A extends Annotation> A getAnnotation(Class<A> annotClass) {
    return type.findAnnotationInTypeHierarchy(annotClass);
  }

  public String getClassName() {
    return type.getName();
  }

  public String getPackageName() {
    return type.getPackage().getName();
  }

  public String getQualifiedName() {
    return type.getQualifiedSourceName();
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotClass) {
    return type.findAnnotationInTypeHierarchy(annotClass) != null;
  }
}