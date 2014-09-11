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
import com.google.gwt.uibinder.rebind.model.OwnerClass;
import com.google.web.bindery.event.shared.HandlerRegistration;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tests the HandlerEvaluator.
 *
 */
public class HandlerEvaluatorTest extends TestCase {

  HandlerEvaluator evaluator;

  private OwnerClass ownerType;
  private MortalLogger logger;
  private TypeOracle oracle;
  private FieldManager fieldManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    logger = new MortalLogger(new PrintWriterTreeLogger());

    // Creates all needed mocks.
    ownerType = mock(OwnerClass.class);
    oracle = mock(TypeOracle.class);
    fieldManager = mock(FieldManager.class);
  }

  public void testWriteAddHandler() throws Exception {
    JClassType handlerRegistrationJClass = mock(JClassType.class);
    when(oracle.findType(HandlerRegistration.class.getName())).thenReturn(
        handlerRegistrationJClass);

    JClassType eventHandlerJClass = mock(JClassType.class);
    when(oracle.findType(EventHandler.class.getName())).thenReturn(
        eventHandlerJClass);

    evaluator = new HandlerEvaluator(ownerType, logger, oracle, false);

    StringWriter sw = new StringWriter();
    evaluator.writeAddHandler(new IndentedWriter(new PrintWriter(sw)),
        fieldManager, "handler1", "addClickHandler", "label1");

    assertEquals("label1.addClickHandler(handler1);", sw.toString().trim());
  }
}
