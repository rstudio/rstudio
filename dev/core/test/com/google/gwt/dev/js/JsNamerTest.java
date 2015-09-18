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

import com.google.gwt.dev.cfg.ConfigurationProperties;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.JsIncrementalNamer.JsIncrementalNamerState;
import com.google.gwt.dev.js.JsNamer.IllegalNameException;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests the JsStaticEval optimizer.
 */
public class JsNamerTest extends TestCase {

  private BlacklistProps props;
  private JsIncrementalNamerState jsIncrementalNamerState;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    props = new BlacklistProps();
    props.blacklist = Arrays.asList("foo, bar", "baz");
    props.blacklistSuffixes = Arrays.asList("logger");
    jsIncrementalNamerState = new JsIncrementalNamerState();
  }

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
    props.blacklist = null;
    props.blacklistSuffixes = null;
    assertEquals("function fooLogger(){return 42}\n",
        rename("function fooLogger() { return 42; }"));
  }

  public void testAvoidDuplicatesSameScope() throws Exception {
    JsProgram program = parseJs(
        "function f1(){ return 1 }\n" +
        "function f2(){ return 2 }\n");

    program.getScope().findExistingName("f1").setShortIdent("thing");
    program.getScope().findExistingName("f2").setShortIdent("thing");

    assertEquals(
        "function thing(){return 1}\n" +
        "function thing_0(){return 2}\n",
        rename(program));
  }

  public void testAvoidDuplicatesChildScope() throws Exception {
    JsProgram program = parseJs(
        "function f1(){\n" +
        "  function f2(){ return 2 }\n" +
        "  return 1\n" +
        "}\n");

    program.getScope().findExistingName("f1").setShortIdent("thing");
    program.getScope().getChildren().get(0).findExistingName("f2").setShortIdent("thing");

    assertEquals("function thing_0(){function thing(){return 2}\nreturn 1}\n",
        rename(program));
  }

  public void testPackageInfo() throws Exception {
    // Synthesize a function definition with an illegal name, "package-info" like can result from
    // JDT compilation of package-info.java files.
    JsProgram jsProgram = new JsProgram();
    JsScope scope = jsProgram.getScope();

    // Function declaration statement.
    JsName name = scope.declareName("package-info", "package-info");
    List<JsStatement> statements = jsProgram.getFragment(0).getGlobalBlock().getStatements();
    final SourceOrigin sourceInfo = SourceOrigin.UNKNOWN;
    JsFunction function = new JsFunction(sourceInfo, scope, name);
    function.setBody(new JsBlock(sourceInfo));
    statements.add(new JsExprStmt(sourceInfo, function));

    // Function invocation statement.
    statements.add(new JsInvocation(sourceInfo, new JsNameRef(sourceInfo, name)).makeStmt());

    // Verify that the illegal "-" character is translated.
    assertEquals("function package_info(){}\npackage_info();",
        rename(jsProgram, JsOutputOption.PRETTY, false));
    assertEquals("function package_info(){}\npackage_info();",
        rename(jsProgram, JsOutputOption.DETAILED, false));
  }

  public void testRejectsReservedSuffix() throws Exception {
    // Regular renaming runs fine.
    assertEquals("function foo_0_g$(){return 42}\n",
        rename(parseJs("function foo() { return 42; }"), JsOutputOption.PRETTY, true));

    // Renaming with the reserved suffix is rejected.
    try {
      String functionName = "foo" + JsIncrementalNamer.RESERVED_IDENT_SUFFIX;
      JsProgram jsProgram = parseJs(
          "function " + functionName + "() { return 42; }");
      jsProgram.getScope().findExistingName(functionName).setUnobfuscatable();
      rename(jsProgram, JsOutputOption.OBFUSCATED, true);
      fail("Naming an unobfuscatable identifier containing the reserved suffix should have "
          + "thrown an exception in JsIncrementalNamer.");
    } catch (IllegalNameException e) {
      // Expected path.
    }
  }

  private JsProgram parseJs(String js) throws IOException, JsParserException {
    JsProgram program = new JsProgram();
    List<JsStatement> expected = JsParser.parse(SourceOrigin.UNKNOWN,
        program.getScope(), new StringReader(js));
    program.getGlobalBlock().getStatements().addAll(expected);
    return program;
  }

  private String rename(JsProgram program) throws IllegalNameException {
    return rename(program, JsOutputOption.PRETTY, false);
  }

  private String rename(JsProgram program, JsOutputOption outputOption, boolean persistent)
      throws IllegalNameException {
    JsSymbolResolver.exec(program);
    ConfigurationProperties config = props.makeConfig();
    switch (outputOption) {
      case PRETTY:
        if (persistent) {
          JsIncrementalNamer.exec(program, config, jsIncrementalNamerState, null, false);
        } else {
          JsPrettyNamer.exec(program, config);
        }
        break;
      case OBFUSCATED:
        if (persistent) {
          JsIncrementalNamer.exec(program, config, jsIncrementalNamerState, null, true);
        } else {
          JsObfuscateNamer.exec(program, config);
        }
        break;
      case DETAILED:
        JsVerboseNamer.exec(program, config);
        break;
    }
    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);
    generator.accept(program);
    return text.toString();
  }

  private String rename(String js) throws Exception {
    return rename(parseJs(js));
  }

  private static class BlacklistProps {
    List<String> blacklist;
    List<String> blacklistSuffixes;

    private ConfigurationProperties makeConfig() {
      Map<String, List<String>> props = Maps.newHashMap();
      if (blacklist != null) {
        props.put(ReservedNames.BLACKLIST, blacklist);
      }
      if (blacklistSuffixes != null) {
        props.put(ReservedNames.BLACKLIST_SUFFIXES, blacklistSuffixes);
      }
      return new ConfigurationProperties(props);
    }
  }
}
