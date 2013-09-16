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
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.DefaultConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the JsStaticEval optimizer.
 */
public class JsNamerTest extends OptimizerTestBase {

  public void testBannedIdent() throws Exception {
    assertEquals("function foo_0(){return 42}\n",
        rename("function foo() { return 42; }"));
    assertEquals("function bar_0(){return 42}\n",
        rename("function bar() { return 42; }"));
    assertEquals("function baz_0(){return 42}\n",
        rename("function baz() { return 42; }"));
  }

  public void testBannedSuffix() throws Exception {
    assertEquals("function fooLogger_0(){return 42}\n",
        rename("function fooLogger() { return 42; }"));
    assertEquals("function foologger_0(){return 42}\n",
        rename("function foologger() { return 42; }"));
    assertEquals("function fooLOGGER_0(){return 42}\n",
        rename("function fooLOGGER() { return 42; }"));
  }
  public void testNoBlacklist() throws Exception {
    assertEquals("function fooLogger(){return 42}\n",
        rename("function fooLogger() { return 42; }", false));
  }

  private String rename(String js) throws Exception {
    return rename(js, true);
  }

  private String rename(String js, final boolean useFilter) throws Exception {
    JsProgram program = new JsProgram();
    List<JsStatement> expected = JsParser.parse(SourceOrigin.UNKNOWN,
        program.getScope(), new StringReader(js));

    program.getGlobalBlock().getStatements().addAll(expected);
    JsSymbolResolver.exec(program);
    JsPrettyNamer.exec(program, new PropertyOracle[]{
        new PropertyOracle() {
          @Override
          public ConfigurationProperty getConfigurationProperty(
              String propertyName) throws BadPropertyValueException {
            if (useFilter) {
              if (JsNamer.BLACKLIST.equals(propertyName)) {
                return new DefaultConfigurationProperty(JsNamer.BLACKLIST,
                    Arrays.asList("foo, bar", "baz"));
              } else if (JsNamer.BLACKLIST_SUFFIXES.equals(propertyName)) {
                return new DefaultConfigurationProperty(
                    JsNamer.BLACKLIST_SUFFIXES, Arrays.asList("logger"));
              }
            }
            throw new BadPropertyValueException("No property value for "
                + propertyName);
          }

          @Override
          public SelectionProperty getSelectionProperty(TreeLogger logger,
              String propertyName) throws BadPropertyValueException {
            return null;
          }
        }
    });
    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);

    generator.accept(program);
    return text.toString();
  }
}

