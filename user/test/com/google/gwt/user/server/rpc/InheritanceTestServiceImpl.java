/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.InheritanceTestServiceSubtype;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AbstractClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.AnonymousClassInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.Circle;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.JavaSerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.MySerializableInterface;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClass;
import com.google.gwt.user.client.rpc.InheritanceTestSetFactory.SerializableClassWithTransientField;
import com.google.gwt.user.client.rpc.InheritanceTestSetValidator;
import com.google.gwt.user.client.rpc.SerializationException;

import javax.servlet.ServletContext;

/**
 * Servlet used by the {@link com.google.gwt.user.client.rpc.InheritanceTest
 * InheritanceTest} unit test.
 */
public class InheritanceTestServiceImpl extends HybridServiceServlet implements
    InheritanceTestServiceSubtype {

  /**
   * Filters log messages to avoid logging spurious warnings during successful
   * tests.
   */
  private ServletContext wrappedServletContext;

  @Override
  public AnonymousClassInterface echo(AnonymousClassInterface serializable) {
    return serializable;
  }

  @Override
  public Circle echo(Circle circle) {
    return circle;
  }

  @Override
  public JavaSerializableClass echo(JavaSerializableClass javaSerializableClass) {
    return javaSerializableClass;
  }

  @Override
  public SerializableClass echo(SerializableClass serializableClass) {
    if (!InheritanceTestSetValidator.isValid(serializableClass)) {
      throw new RuntimeException();
    }

    return serializableClass;
  }

  @Override
  public SerializableClassWithTransientField echo(
      SerializableClassWithTransientField serializableClass) {
    if (!InheritanceTestSetValidator.isValid(serializableClass)) {
      throw new RuntimeException();
    }

    // this should not be sent back across the wire, the client will verify
    // that this is true
    serializableClass.setObj("hello");

    return serializableClass;
  }

  @Override
  public void foo() {
  }

  @Override
  public AbstractClass getAbstractClass() {
    // never actually called, used in testing the RPC generator
    return null;
  }

  @Override
  public MySerializableInterface getSerializableInterface1() {
    // never actually called, used in testing the RPC generator
    return null;
  }

  @Override
  public MySerializableInterface getSerializableInterface2() {
    // never actually called, used in testing the RPC generator
    return null;
  }

  /**
   * Overrides the default servlet context to filter expected log messages.
   */
  @Override
  public ServletContext getServletContext() {
    if (wrappedServletContext == null) {
      wrappedServletContext = new LogFilterServletContext(
          super.getServletContext()) {
        @Override
        protected boolean shouldLog(Throwable t, String msg) {
          if ((t instanceof IncompatibleRemoteServiceException || t instanceof SerializationException)
              && (t.getMessage().contains("com.google.gwt.user.client.rpc.InheritanceTest$1"))
              || t.getMessage().contains(
                  "com.google.gwt.user.client.rpc.InheritanceTestSetFactory$1")) {
            return false;
          }
          return true;
        }
      };
    }
    return wrappedServletContext;
  }

  @Override
  public SerializableClass getUnserializableClass() {
    return InheritanceTestSetFactory.createNonStaticInnerClass();
  }
}