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
package com.google.gwt.i18n.server.impl;

import com.google.gwt.i18n.client.LocalizableResource;
import com.google.gwt.i18n.server.AbstractMessageInterface;
import com.google.gwt.i18n.server.Message;
import com.google.gwt.i18n.server.MessageProcessingException;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflection-based implementation of
 * {@link com.google.gwt.i18n.server.MessageInterface MessageInterface}.
 */
public class ReflectionMessageInterface extends AbstractMessageInterface {

  private final Class<? extends LocalizableResource> msgIntf;
  
  private Map<Method, String[]> paramNames = null;

  public ReflectionMessageInterface(GwtLocaleFactory factory,
      Class<? extends LocalizableResource> msgIntf) {
    super(factory);
    this.msgIntf = msgIntf;
  }

  @Override
  public <A extends Annotation> A getAnnotation(Class<A> annotClass) {
    return ReflectionUtils.getAnnotation(msgIntf, annotClass, true);
  }

  @Override
  public String getClassName() {
    StringBuilder buf = new StringBuilder();
    Class<?> encl = msgIntf.getEnclosingClass();
    while (encl != null) {
      buf.insert(0, encl.getSimpleName() + ".");
      encl = encl.getEnclosingClass();
    }
    return buf.toString() + msgIntf.getSimpleName();
  }

  @Override
  public Iterable<Message> getMessages() throws MessageProcessingException {
    final Method[] methods = msgIntf.getMethods();
    List<Message> messages = new ArrayList<Message>();
    for (Method method : methods) {
      messages.add(new ReflectionMessage(factory,
          ReflectionMessageInterface.this, method));
    }
    Collections.sort(messages);
    return Collections.unmodifiableList(messages);
  }

  @Override
  public String getPackageName() {
    return msgIntf.getPackage().getName();
  }

  @Override
  public String getQualifiedName() {
    String name = getPackageName();
    if (name != null) {
      name += ".";
    }
    return name + getClassName();
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotClass) {
    return ReflectionUtils.getAnnotation(msgIntf, annotClass, true) != null;
  }

  String[] getParameterNames(Method method) {
    if (paramNames == null) {
      fetchParameterNames();
    }
    return paramNames.get(method);
  }

  private void fetchParameterNames() {
    paramNames = new HashMap<Method, String[]>();
    // String path = getPackageName().replace('.', '/') + '/';
    // String className = getClassName();
    // int dot = className.indexOf('.');
    // if (dot >= 0) {
    //   className = className.substring(0, dot);
    // }
    // path += className + ".java";
    // InputStream stream = msgIntf.getClassLoader().getResourceAsStream(path);
    // if (stream != null) {
    //   return;
    // }
    // String source = Util.readStreamAsString(stream);
    // CompilationUnitDeclaration cud = JavaSourceParser.parseJava(source);
    // String binaryName = getPackageName() + '.'
        // + getClassName().replace('.', '$');
    // TypeDeclaration typeDecl = JavaSourceParser.findType(cud, binaryName);
    // for (AbstractMethodDeclaration methodDecl : typeDecl.methods) {
      // if (methodDecl.isClinit() || methodDecl.isConstructor()) {
        // continue;
      // }
      // String methodName = new String(methodDecl.selector);
      // int numArgs = methodDecl.arguments.length;
      // Class<?>[] paramTypes = new Class<?>[numArgs];
      // for (int i = 0; i < numArgs; ++i) {
      // }
      // try {
        // Method method = msgIntf.getMethod(methodName, paramTypes);
        // String[] names = new String[numArgs];
        // for (int i = 0; i < numArgs; ++i) {
          // names[i] = new String(methodDecl.arguments[i].name);
        // }
        // paramNames.put(method, names);
      // } catch (SecurityException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
      // } catch (NoSuchMethodException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
      // }
    // }
  }
}
