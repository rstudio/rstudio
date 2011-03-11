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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.server.AbstractMessageInterface;
import com.google.gwt.i18n.server.Message;
import com.google.gwt.i18n.server.MessageProcessingException;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of the {@link com.google.gwt.i18n.server.MessageInterface} API
 * on top of TypeOracle.
 */
public class TypeOracleMessageInterface extends AbstractMessageInterface {

  private final TypeOracle oracle;
  private final JClassType type;
  private final ResourceList resources;

  public TypeOracleMessageInterface(GwtLocaleFactory factory,
      JClassType type, ResourceList resources) {
    super(factory);
    this.type = type;
    this.oracle = type.getOracle();
    this.resources = resources;
  }

  @Override
  public <A extends Annotation> A getAnnotation(Class<A> annotClass) {
    return type.findAnnotationInTypeHierarchy(annotClass);
  }

  @Override
  public String getClassName() {
    return type.getName();
  }

  @Override
  public Iterable<Message> getMessages() throws MessageProcessingException {
    JMethod[] methods = type.getMethods();
    List<Message> messages = new ArrayList<Message>();
    for (JMethod method : methods) {
      messages.add(new TypeOracleMessage(oracle, factory, this, method,
          resources));
    }
    Collections.sort(messages);
    return Collections.unmodifiableList(messages);
  }

  @Override
  public String getPackageName() {
    return type.getPackage().getName();
  }

  @Override
  public String getQualifiedName() {
    return type.getQualifiedSourceName();
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotClass) {
    return type.findAnnotationInTypeHierarchy(annotClass) != null;
  }
}
