package com.google.gwt.dev.jjs.impl.gflow;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.OptimizerTestBase;
import com.google.gwt.dev.jjs.impl.gflow.DataflowOptimizer;
import com.google.gwt.dev.util.Strings;

public class DataflowOptimizerTest extends OptimizerTestBase {
  private final class Result {
    private final String optimized;
    private final String returnType;
    private final String userCode;
    private final boolean madeChages;

    public Result(String returnType, String userCode, String optimized, 
        boolean madeChages) {
      this.returnType = returnType;
      this.userCode = userCode;
      this.optimized = optimized;
      this.madeChages = madeChages;
    }

    public void into(String...expected) throws UnableToCompleteException {
      JProgram program = compileSnippet(returnType, Strings.join(expected, "\n"));
      assertEquals(userCode, getMainMethodSource(program), optimized);
    }

    public void intoString(String...expected) {
      String expectedSnippet = Strings.join(expected, "\n");
      assertEquals(userCode, expectedSnippet, optimized);
    }

    public void noChange() {
      assertFalse(madeChages);
    }
  }

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
    
    optimizeMethod("int", "calculate", "return 1;"
        ).intoString(
            "{",
            "  int t;",
            "  if (i > j) {",
            "    t = i;",
            "    i = j;",
            "    j = t;",
            "  }",
            "  return EntryPoint.multiply(i, j);",
            "}"         
            );
  }

/*  public void testInlineField1() throws Exception {
    optimize("int",
        "int i = staticFooInstance.i;",
        "return i;"
        ).into("int i; return EntryPoint.staticFooInstance.i;");
  }

  public void testInlineField2() throws Exception {
    optimize("int",
        "int i = staticFooInstance.i;",
        "createFoo();",
        "return i;"
        ).noChange();
  }

  public void testInlineField3() throws Exception {
    optimize("int",
        "int i = staticFooInstance.i;",
        "i = 2;",
        "return i;"
        ).into("int i; return 2;");
  }

  public void testLoop1() throws Exception {
    optimize("int", 
        "int i = 0; int j = 0;",
        "while (bar()) {",
        "  j = i + 2;",
        "  i = j + 1;",
        "}",
        "return i;").into(
            "int i = 0;",
            "int j;",
            "while (EntryPoint.bar()) {",
            "  i = i + 2 + 1;",
            "}",
            "return i;");
  }

  public void testLoop2() throws Exception {
    optimize("int", 
        "int i = 0; int j = 0;",
        "while (bar()) {",
        "  j = i + 2;",
        "  i = j + 1;",
        "}",
        "return j;").into(
            "int i = 0;",
            "int j = 0;",
            "while (EntryPoint.bar()) {",
            "  j = i + 2;",
            "  i = i + 2 + 1;",
            "}",
            "return j;");
  }
*/
  private Result optimize(final String returnType, final String...codeSnippet)
      throws UnableToCompleteException {
    return optimizeMethod(returnType, MAIN_METHOD_NAME, codeSnippet);
  }

  private Result optimizeMethod(final String returnType, 
      final String methodName, final String... codeSnippet)
      throws UnableToCompleteException {
    String snippet = Strings.join(codeSnippet, "\n");
    JProgram program = compileSnippet(returnType, snippet);
    JMethod method = findMethod(program, methodName);
    boolean madeChanges = DataflowOptimizer.exec(program, method);
    return new Result(returnType, snippet, method.getBody().toSource(),
        madeChanges);
  }

}
