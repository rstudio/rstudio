/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.rebind.model.OwnerClass;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tests the HandlerEvaluator.
 *
 */
public class HandlerEvaluatorTest extends TestCase {

  HandlerEvaluator evaluator;

  // Defines the mock control.
  private IMocksControl mockControl;

  private OwnerClass ownerType;
  private MortalLogger logger;
  private TypeOracle oracle;
  private FieldManager fieldManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    logger = new MortalLogger(new PrintWriterTreeLogger());

    // Creates all needed mocks.
    mockControl = EasyMock.createControl();
    ownerType = mockControl.createMock(OwnerClass.class);
    oracle = mockControl.createMock(TypeOracle.class);
    fieldManager = mockControl.createMock(FieldManager.class);

    // TODO(hermes): sucks I know!!!! This class shouldn't be using EasyMock
    // but for now that's the easiest way of creating new instances of
    // TypeOracle, TreeLogger, etc. Again, I must check a better way of
    // injecting TypeOracle, TreeLogger and JClassType.

    JClassType handlerRegistrationJClass = mockControl.createMock(JClassType.class);
    EasyMock.expect(oracle.findType(HandlerRegistration.class.getName())).andReturn(
        handlerRegistrationJClass);

    JClassType eventHandlerJClass = mockControl.createMock(JClassType.class);
    EasyMock.expect(oracle.findType(EventHandler.class.getName())).andReturn(
        eventHandlerJClass);

    mockControl.replay();
    evaluator = new HandlerEvaluator(ownerType, logger, oracle, false);
    mockControl.verify();
    mockControl.reset();
  }

  public void testWriteAddHandler() throws Exception {
    StringWriter sw = new StringWriter();
    evaluator.writeAddHandler(new IndentedWriter(new PrintWriter(sw)),
        fieldManager, "handler1", "addClickHandler", "label1");

    assertEquals("label1.addClickHandler(handler1);", sw.toString().trim());
  }
}
