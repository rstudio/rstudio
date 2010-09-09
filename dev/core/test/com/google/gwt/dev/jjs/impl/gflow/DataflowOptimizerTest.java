package com.google.gwt.dev.jjs.impl.gflow;

import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.OptimizerTestBase;

public class DataflowOptimizerTest extends OptimizerTestBase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    addSnippetClassDecl("static void foo(int i) { }");
    addSnippetClassDecl("static boolean bar() { return true; }");
    addSnippetClassDecl("static void baz(boolean b) {  }");
    addSnippetClassDecl("static int genInt() { return 1; }");
    addSnippetClassDecl("static int multiply(int i, int j) { return i * j; }");

    addSnippetClassDecl("static class CheckedException extends Exception {}");
    addSnippetClassDecl("static class UncheckedException1 extends RuntimeException {}");
    addSnippetClassDecl("static class UncheckedException2 extends RuntimeException {}");
    
    addSnippetClassDecl("static void throwUncheckedException1() " +
        "throws UncheckedException1 {}");
    addSnippetClassDecl("static void throwCheckedException() " +
        "throws CheckedException {}");
    addSnippetClassDecl("static void throwSeveralExceptions() " +
        "throws CheckedException, UncheckedException1 {}");
    
    addSnippetClassDecl("static class Foo { int i; int j; int k; }");
    addSnippetClassDecl("static Foo createFoo() {return null;}");
    addSnippetClassDecl("static Foo staticFooInstance;");
  }

  public void testLinearStatements1() throws Exception {
    optimize("int", "int i = 1; int j = i; return i;").into(
        "int i; int j; return 1;");
  }

  public void testLinearStatements2() throws Exception {
    optimize("int", "int i = 1; int j = i; return j;").into(
        "int i; int j; return 1;");
  }

  public void testLinearStatements3() throws Exception {
    optimize("void", "int i = 1; int j = 1; foo(j);").into(
        "int i; int j; foo(1);");
  }

  public void testDeadIfBranch1() throws Exception {
    optimize("void",
        "int i = 1; if (i == 1) { int j = 2; } else { int j = 3; }").into(
        "int i; { int j; } ");
  }

  public void testDeadIfBranch2() throws Exception {
    optimize("void",
        "int i = 1; if (i != 1) { int j = 2; } else { int j = 3; }").into(
        "int i; { int j; } ");
  }

  public void testDeadIfBranch3() throws Exception {
    optimize("int",
        "int i = 1; int j = 0; if (i != 1) { j = 2; } else { j = 3; } return j;").into(
        "int i; int j; return 3; ");
  }
  
  public void testDeadIfBranch4() throws Exception {
    addSnippetClassDecl("static Object f = null;");
    optimize("void", 
        "Object e = null;" +
        "if (e == null && f == null) {" +
        "  return;" +
        "}" +
        "boolean b = e == null;").into(
        "Object e; if (EntryPoint.f == null) { return; } boolean b;"
        );
  }

  public void testDeadWhile() throws Exception {
    optimize("void",
    "int j = 0; while (j > 0) { }").into(
    "int j;");
  }

  // Various complex stuff
  public void testComplexCode1() throws Exception {
    optimize("int",
        "int i = 1; int j = 0; while (j > 0) { if (i != 1) { i++; j++; } } return i;").into(
        "int i; int j; return 1;");
  }
  
  public void testComplexCode2() throws Exception {
    optimize("void",
        "boolean b = bar(); if (b) { baz(b); return; }").into(
        "boolean b = bar(); if (b) { baz(true); return; }");
  }
  
  public void testAssert() throws Exception {
    optimize("void",
        "boolean b = true;",
        "assert b;").into(
        "boolean b;");
  }

  public void testNoChange() throws Exception {
    optimize("void",
        "try {",
        "  foo(1);",
        "} catch (RuntimeException e) { }").noChange();
  }

  public void testAssignToField() throws Exception {
    optimize("void",
        "Foo foo = createFoo();",
        "foo.i = 1;"
        ).noChange();
  }
  
  public void testSwapValues() throws Exception {
    optimize("int",
        "int i = genInt(); int j = genInt(); int t;",
        "if (i > j) { t = i; i = j; j = t; }",
        "return multiply(i, j);"
        ).into(
            "int i = genInt(); int j = genInt(); int t;",
            "if (i > j) { t = i; i = j; j = t; }",
            "return multiply(i, j);"
         );
  }

  public void testSwapValuesMethodParameter() throws Exception {
    addSnippetClassDecl("int calculate(int i, int j) {" +
        "int t;" +
        "if (i > j) { t = i; i = j; j = t; }" +
        "return multiply(i, j);" + 
        "}");
    
    optimizeMethod("calculate", "int", "return 1;"
        ).intoString(
            "int t;",
            "if (i > j) {",
            "  t = i;",
            "  i = j;",
            "  j = t;",
            "}",
            "return EntryPoint.multiply(i, j);"
            );
  }

  public void testComplexCode3() throws Exception {
    addSnippetClassDecl("static final int SPLIT_LOOKING_FOR_COMMA = 0;");
    addSnippetClassDecl("static final int SPLIT_IN_STRING = 1;");
    addSnippetClassDecl("static final int SPLIT_IN_ESCAPE = 2;");
    addSnippetClassDecl("static String getCsvString() { return null; }");

    addSnippetClassDecl(
        "static class JsArrayString {" + 
        "  static JsArrayString createArray() { return null; }" +
        "  JsArrayString cast() { return this; }" +
        "  void push(String s) { }" +
        "}");

    addSnippetClassDecl(
        "static class StringBuilder {" + 
        "  void append(char c) { }" +
        "}");

    optimize("JsArrayString", 
       "int state;",
       "String csvString = getCsvString();",
       "JsArrayString results = JsArrayString.createArray().cast();",
       "int index = 0;",
       "StringBuilder field = new StringBuilder();",
       "state = SPLIT_LOOKING_FOR_COMMA;",
       "while (index < csvString.length()) {",
       "  char nextCharacter = csvString.charAt(index);",
       "  switch (state) {",
       "    case SPLIT_LOOKING_FOR_COMMA:",
       "      switch (nextCharacter) {",
       "        case ',':",
       "          results.push(field.toString());",
       "          field = new StringBuilder();",
       "          break;",
       "        case '\"':",
       "          state = SPLIT_IN_STRING;",
       "          break;",
       "        default:",
       "          field.append(nextCharacter);",
       "      }",
       "      break;",
       "    case SPLIT_IN_STRING:",
       "      switch (nextCharacter) {",
       "        case '\"':",
       "          state = SPLIT_LOOKING_FOR_COMMA;",
       "          break;",
       "        case '\\\\':",
       "          state = SPLIT_IN_ESCAPE;",
       "          field.append(nextCharacter);",
       "          break;",
       "        default:",
       "          field.append(nextCharacter);",
       "      }",
       "      break;",
       "    case SPLIT_IN_ESCAPE:",
       "      state = SPLIT_IN_STRING;",
       "      field.append(nextCharacter);",
       "      break;",
       "    default:",
       "      field.append(nextCharacter);",
       "  }",
       "  index++;",
       "}",
       "results.push(field.toString());",
       "return results;"
   ).into(
       "int state;",
       "String csvString = getCsvString();",
       "JsArrayString results = JsArrayString.createArray().cast();",
       "int index = 0;",
       "StringBuilder field = new StringBuilder();",
       "state = SPLIT_LOOKING_FOR_COMMA;",
       "while (index < csvString.length()) {",
       "  char nextCharacter = csvString.charAt(index);",
       "  switch (state) {",
       "    case SPLIT_LOOKING_FOR_COMMA:",
       "      switch (nextCharacter) {",
       "        case ',':",
       "          results.push(field.toString());",
       "          field = new StringBuilder();",
       "          break;",
       "        case '\"':",
       "          state = SPLIT_IN_STRING;",
       "          break;",
       "        default:",
       "          field.append(nextCharacter);",
       "      }",
       "      break;",
       "    case SPLIT_IN_STRING:",
       "      switch (nextCharacter) {",
       "        case '\"':",
       "          state = SPLIT_LOOKING_FOR_COMMA;",
       "          break;",
       "        case '\\\\':",
       "          state = SPLIT_IN_ESCAPE;",
       "          field.append('\\\\');",
       "          break;",
       "        default:",
       "          field.append(nextCharacter);",
       "      }",
       "      break;",
       "    case SPLIT_IN_ESCAPE:",
       "      state = SPLIT_IN_STRING;",
       "      field.append(nextCharacter);",
       "      break;",
       "    default:",
       "      field.append(nextCharacter);",
       "  }",
       "  ++index;",
       "}",
       "results.push(field.toString());",
       "return results;"
       );
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    return DataflowOptimizer.exec(program, method).didChange();
  }
}
