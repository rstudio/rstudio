/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.server;

import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.Method;
import com.google.gwt.requestfactory.server.RequestFactoryInterfaceValidatorTest.VisibleErrorContext;
import com.google.gwt.requestfactory.shared.BoxesAndPrimitivesTest;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.Service;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A JRE version of {@link BoxesAndPrimitivesTest} with additional validation
 * tests.
 */
public class BoxesAndPrimitivesJreTest extends BoxesAndPrimitivesTest {

  @Service(ServiceImpl.class)
  interface ContextMismatchedParameterA extends RequestContext {
    Request<Void> checkBoxed(int value);
  }

  @Service(ServiceImpl.class)
  interface ContextMismatchedParameterB extends RequestContext {
    Request<Void> checkPrimitive(Integer value);
  }

  @ProxyFor(Entity.class)
  interface ProxyMismatchedGetterA extends EntityProxy {
    int getBoxed();
  }

  @ProxyFor(Entity.class)
  interface ProxyMismatchedGetterB extends EntityProxy {
    Integer getPrimitive();
  }

  private VisibleErrorContext errors;
  private RequestFactoryInterfaceValidator v;

  @Override
  public String getModuleName() {
    return null;
  }

  public void test() {
    RequestFactoryInterfaceValidator v = new RequestFactoryInterfaceValidator(
        Logger.getAnonymousLogger(),
        new RequestFactoryInterfaceValidator.ClassLoaderLoader(
            getClass().getClassLoader()));
    v.validateRequestFactory(Factory.class.getName());
    assertFalse(v.isPoisoned());
  }

  /**
   * Tests that mismatched primitive verses boxed getters are correctly
   * reported.
   */
  public void testMismatchedGetters() {
    v.validateEntityProxy(ProxyMismatchedGetterA.class.getName());
    v.validateEntityProxy(ProxyMismatchedGetterB.class.getName());
    assertTrue(v.isPoisoned());

    String getBoxedMessage = RequestFactoryInterfaceValidator.messageCouldNotFindMethod(
        Type.getType(Entity.class),
        Arrays.asList(new Method("getBoxed", "()Ljava/lang/Integer;")));
    String getPrimitiveMessage = RequestFactoryInterfaceValidator.messageCouldNotFindMethod(
        Type.getType(Entity.class),
        Arrays.asList(new Method("getPrimitive", "()I")));
    assertEquals(Arrays.asList(getBoxedMessage, getPrimitiveMessage),
        errors.logs);
  }

  /**
   * Tests that mismatched parameter types are correctly reported.
   */
  public void testMismatchedParameters() {
    v.validateRequestContext(ContextMismatchedParameterA.class.getName());
    v.validateRequestContext(ContextMismatchedParameterB.class.getName());

    String checkBoxedMessage = RequestFactoryInterfaceValidator.messageCouldNotFindMethod(
        Type.getType(ServiceImpl.class),
        Arrays.asList(new Method("checkBoxed", "(Ljava/lang/Integer;)V")));
    String checkPrimitiveMessage = RequestFactoryInterfaceValidator.messageCouldNotFindMethod(
        Type.getType(ServiceImpl.class),
        Arrays.asList(new Method("checkPrimitive", "(I)V")));
    assertEquals(Arrays.asList(checkBoxedMessage, checkPrimitiveMessage),
        errors.logs);
  }

  @Override
  protected Factory createFactory() {
    return RequestFactoryJreTest.createInProcess(Factory.class);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    errors = new VisibleErrorContext(Logger.getAnonymousLogger());
    v = new RequestFactoryInterfaceValidator(errors,
        new RequestFactoryInterfaceValidator.ClassLoaderLoader(
            getClass().getClassLoader()));
  }
}
