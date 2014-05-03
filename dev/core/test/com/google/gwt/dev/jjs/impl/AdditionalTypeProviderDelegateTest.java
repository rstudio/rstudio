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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.GeneratedUnit;
import com.google.gwt.dev.javac.JdtCompiler.AdditionalTypeProviderDelegate;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests that a AdditionalTypeProviderDelegate correctly gets control when an unknown
 * class is found, and that source for an unknown class gets correctly parsed.
 */
public class AdditionalTypeProviderDelegateTest extends OptimizerTestBase {

  /**
   * Compilation unit for a class generated at runtime when an unknown
   * reference appears.
   */
  private class JavaWrapperCompilationUnit implements GeneratedUnit {
    private final long createTime = System.currentTimeMillis();

    @Override
    public String optionalFileLocation() {
      return null; // not used
    }

    @Override
    public String getStrongHash() {
      return "InsertedClass";
    }

    @Override
    public long creationTime() {
      return createTime;
    }

    @Override
    public String getSource() {
      String classSource =
          "package myPackage;\n" +
          "public class InsertedClass {\n" +
          "  public static int getSmallNumber() {\n" +
          "    return 5;\n" +
          "  }\n" +
          "}";
      return classSource;
    }

    @Override
    public String getSourceMapPath() {
      // The named file location requires a non-Java extension,
      // or else the file won't get compiled correctly.
      return "myPackage/InsertedClass.notjava";
    }

    @Override
    public String getTypeName() {
      return "myPackage.InsertedClass";
    }
    @Override
    public long getSourceToken() {
      return -1;
    }
  }

  public boolean insertInsertedClass = false;

  @Override
  public void setUp() {
     // Create a source class that passes fine (just to test infrastructure.)
     sourceOracle.addOrReplace(new MockJavaResource("test.A") {
       @Override
       public CharSequence getContent() {
         StringBuffer code = new StringBuffer();
         code.append("package test;\n");
         code.append("class A {\n");
         code.append("  void myFunc() {\n");
         code.append("  }\n");
         code.append("}\n");
         return code;
       }
     });

     // Create a source file containing a reference to a class in another
     // package that we don't yet know about.  That code will be inserted
     // by the AdditionalTypeProviderDelegate.
     sourceOracle.addOrReplace(new MockJavaResource("test.B") {
       @Override
       public CharSequence getContent() {
         StringBuffer code = new StringBuffer();
         code.append("package test;\n");
         code.append("import myPackage.InsertedClass;");
         code.append("class B {\n");
         code.append("  int func() {\n");
         // Reference an unknown class that will be substituted on the fly.
         code.append("    return myPackage.InsertedClass.getSmallNumber();\n");
         code.append("  }\n");
         code.append("}\n");
         return code;
       }
     });

     // Create a source file containing a reference to a class in another
     // package, but that lacks an import directive.  Are we creating the
     // class anyway?
     sourceOracle.addOrReplace(new MockJavaResource("test.B1") {
       @Override
       public CharSequence getContent() {
         StringBuffer code = new StringBuffer();
         code.append("package test;\n");
         code.append("class B1 {\n");
         code.append("  int func() {\n");
         // Reference an unknown class that will be substituted on the fly.
         code.append("    return myPackage.InsertedClass.getSmallNumber();\n");
         code.append("  }\n");
         code.append("}\n");
         return code;
       }
     });
  }

  public void testInsertedClass() throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "new test.B().func();");

    // Make sure the compiled classes appeared.
    JDeclaredType bType = findDeclaredType(program, "test.B");
    assertNotNull("Unknown type B", bType);
    JDeclaredType insertedClassType = findDeclaredType(program, "myPackage.InsertedClass");
    assertNotNull("Unknown type InsertedClass", insertedClassType);
  }

  public void testInsertedClass2() throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "new test.B1().func();");

    // Make sure the compiled classes appeared.
    JDeclaredType bType = findDeclaredType(program, "test.B1");
    assertNotNull("Unknown type B1", bType);
    JDeclaredType insertedClassType = findDeclaredType(program, "myPackage.InsertedClass");
    assertNotNull("Unknown type InsertedClass", insertedClassType);
  }

  // Make sure regular code not using the AdditionalTypeProviderDelegate still works.
  public void testSimpleParse() throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "new test.A();");
    JDeclaredType goodClassType = findDeclaredType(program, "test.A");
    assertNotNull("Unknown class A", goodClassType);
  }

  public void testUnknownClass() {
    // Create a source file containing a reference to an unknown class.
    sourceOracle.addOrReplace(new MockJavaResource("test.C") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("import myPackage.UnknownClass;");
        code.append("class C {\n");
        code.append("  int func() {\n");
        // Reference an unknown class.
        code.append("    return myPackage.UnknownClass.getSmallNumber();\n");
        code.append("  }\n");
        code.append("}\n");
        return code;
      }
    });
    try {
      compileSnippet("void", "new test.C();");
      fail("Shouldn't have compiled");
    } catch (UnableToCompleteException expected) {
    }
  }

  public void testUnknownClassNoImport() {
    // Create a source file with a reference to an unknown class and no
    // import statement.
    sourceOracle.addOrReplace(new MockJavaResource("test.D") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("class D {\n");
        code.append("  int func() {\n");
        // Reference an unknown class.
        code.append("    return myPackage.UnknownClass.getSmallNumber();\n");
        code.append("  }\n");
        code.append("}\n");
        return code;
      }
    });
    try {
      compileSnippet("void", "new test.D();");
      fail("Shouldn't have compiled");
    } catch (UnableToCompleteException expected) {
    }
  }

  @Override
  protected AdditionalTypeProviderDelegate getAdditionalTypeProviderDelegate() {
    // We'll provide a simple compiler delegate that will provide source
    // for a class called myPackage.InsertedClass.
    return new AdditionalTypeProviderDelegate() {
      @Override
      public boolean doFindAdditionalPackage(String slashedPackageName) {
        if (slashedPackageName.compareTo("myPackage") == 0) {
            return true;
        }
        return false;
      }

      @Override
      public GeneratedUnit doFindAdditionalType(String binaryName) {
        if (binaryName.compareTo("myPackage/InsertedClass") == 0) {
          return new JavaWrapperCompilationUnit();
      }
      return null;
      }
    };
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    return false;
  }
}
