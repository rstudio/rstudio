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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.shell.FailErrorLogger;
import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.server.MessageInterface;
import com.google.gwt.i18n.server.MessageInterfaceTestBase;
import com.google.gwt.i18n.server.testing.Child;

/**
 * Tests for {@link TypeOracleMessage}.
 */
public class TypeOracleMessageTest extends MessageInterfaceTestBase {

  private static MessageInterface getMessageInterfaceInstance() {
    JClassType classType;
    TreeLogger logger = new FailErrorLogger();
    try {
      ModuleDef module = ModuleDefLoader.loadFromClassPath(logger,
          Child.class.getPackage().getName() + ".Testing");
      CompilationState compilationState = module.getCompilationState(logger);
      TypeOracle typeOracle = compilationState.getTypeOracle();
      classType = typeOracle.findType(TEST_CLASS.getCanonicalName());
    } catch (UnableToCompleteException e) {
      throw new RuntimeException(e);
    }
    return new TypeOracleMessageInterface(new GwtLocaleFactoryImpl(),
        classType, null);
  }

  public TypeOracleMessageTest() {
    super(getMessageInterfaceInstance());
  }
}
