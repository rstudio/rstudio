/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.jdt.test;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.dev.jdt.ByteCodeCompiler;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.SourceOracle;
import com.google.gwt.dev.jdt.URLCompilationUnitProvider;
import com.google.gwt.dev.util.FileOracle;
import com.google.gwt.dev.util.FileOracleFactory;
import com.google.gwt.dev.util.FileOracleFactory.FileFilter;
import com.google.gwt.dev.util.log.Loggers;

import junit.framework.TestCase;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ByteCodeCompilerTest extends TestCase {

  private static class TestByteCodeCompilerHost implements SourceOracle,
      RebindOracle {

    private abstract class TestCup implements CompilationUnitProvider {

      public TestCup(String packageName, String onlyTypeName) {
        this(packageName, new String[] {onlyTypeName});
      }

      public TestCup(String packageName, String[] typeNames) {
        this.packageName = packageName;
        registerPackage(packageName);
        for (int i = 0; i < typeNames.length; i++) {
          if (packageName.length() > 0) {
            registerType(packageName + "." + typeNames[i], this);
          } else {
            // In the default package.
            //
            registerType(typeNames[i], this);
          }
        }
        firstTypeName = typeNames[0];
      }

      public long getLastModified() throws UnableToCompleteException {
        return 0;
      }

      public String getLocation() {
        return "transient source for " + packageName + "." + firstTypeName;
      }

      public String getMainTypeName() {
        return null;
      }

      public String getPackageName() {
        return packageName;
      }

      public abstract char[] getSource();

      public boolean isTransient() {
        return true;
      }

      private final String firstTypeName;
      private final String packageName;
    }

    public TestByteCodeCompilerHost() {
      registerPackage(""); // the default package
    }

    public final CompilationUnitProvider findCompilationUnit(TreeLogger logger,
        String typeName) {
      return cupsByTypeName.get(typeName);
    }

    public final boolean isPackage(String possiblePackageName) {
      return pkgNames.contains(possiblePackageName);
    }

    // Override for specific test cases.
    //
    public String rebind(TreeLogger logger, String typeName)
        throws UnableToCompleteException {
      return typeName;
    }

    public final void registerPackage(String packageName) {
      String[] packageParts = packageName.split("\\.");
      String toRegister = null;
      for (int i = 0; i < packageParts.length; i++) {
        String part = packageParts[i];
        if (toRegister != null) {
          toRegister += "." + part;
        } else {
          toRegister = part;
        }
        pkgNames.add(toRegister);
      }
    }

    public final void registerType(String typeName, TestCup cup) {
      cupsByTypeName.put(typeName, cup);
    }

    {
      pkgNames = new HashSet<String>();
      cupsByTypeName = new HashMap<String, CompilationUnitProvider>();
    }

    final CompilationUnitProvider CU_AB = new TestCup("test", new String[] {
        "A", "A.B"}) {
      public char[] getSource() {
        StringBuffer sb = new StringBuffer();
        sb.append("package test;\n");
        sb.append("public class A {\n");
        sb.append("  public static class B extends A { }\n");
        sb.append("}\n");
        return sb.toString().toCharArray();
      }
    };

    final CompilationUnitProvider CU_C = new TestCup("test", new String[] {
        "C", "C.Message"}) {
      public char[] getSource() {
        StringBuffer sb = new StringBuffer();
        sb.append("package test;\n");
        sb.append("import com.google.gwt.core.client.GWT;\n");
        sb.append("public class C {\n");
        sb.append("  public static String getMessage() {\n");
        sb.append("    return ((Message)GWT.create(Message.class)).f();\n");
        sb.append("  }\n");
        sb.append("  public static class Message {\n");
        sb.append("    public String f() {\n");
        sb.append("      return \"C.Message\";\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString().toCharArray();
      }
    };

    final CompilationUnitProvider CU_CLASS = new TestCup("java.lang", "Class") {

      public char[] getSource() {
        StringBuffer sb = new StringBuffer();
        sb.append("package java.lang;\n");
        sb.append("public class Class { }\n");
        return sb.toString().toCharArray();
      }
    };

    /**
     * This one is different because D is not public and it lives in the default
     * package.
     */
    final CompilationUnitProvider CU_DE = new TestCup("", new String[] {
        "D", "D.E"}) {
      public char[] getSource() {
        StringBuffer sb = new StringBuffer();
        sb.append("class D extends test.C.Message {\n");
        sb.append("  public static class E extends D {\n");
        sb.append("    public String getMessage() {\n");
        sb.append("      return \"D.E.Message\";\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString().toCharArray();
      }
    };

    final CompilationUnitProvider CU_GWT = new TestCup(
        "com.google.gwt.core.client", "GWT") {

      public char[] getSource() {
        StringBuffer sb = new StringBuffer();
        sb.append("package com.google.gwt.core.client;\n");
        sb.append("public final class GWT {\n");
        sb.append("  public static Object create(Class classLiteral) { return null; }\n");
        sb.append("}\n");
        return sb.toString().toCharArray();
      }
    };

    final CompilationUnitProvider CU_MAIN = new TestCup("test", "Main") {

      public char[] getSource() {
        StringBuffer sb = new StringBuffer();
        sb.append("package test;\n");
        sb.append("import com.google.gwt.core.client.GWT;\n");
        sb.append("public class Main {\n");
        sb.append("  public static void main(String[] args) {\n");
        sb.append("    A a = (A)GWT.create(A.class);\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString().toCharArray();
      }
    };

    final TestCup CU_OBJECT = new TestCup("java.lang", "Object") {
      public char[] getSource() {
        StringBuffer sb = new StringBuffer();
        sb.append("package java.lang;\n");
        sb.append("public class Object { }\n");
        return sb.toString().toCharArray();
      }
    };

    final CompilationUnitProvider CU_STRING = new TestCup("java.lang", "String") {

      public char[] getSource() {
        StringBuffer sb = new StringBuffer();
        sb.append("package java.lang;\n");
        sb.append("public class String { }\n");
        return sb.toString().toCharArray();
      }
    };

    private final Map<String, CompilationUnitProvider> cupsByTypeName;
    private final Set<String> pkgNames;
  }

  private static void scanAndCompile(TreeLogger logger)
      throws UnableToCompleteException {
    FileOracleFactory fof = new FileOracleFactory();
    fof.addPackage("", new FileFilter() {
      public boolean accept(String string) {
        return string.endsWith(".java");
      }
    });
    final FileOracle fo = fof.create(logger);

    final SourceOracle host = new SourceOracle() {

      public CompilationUnitProvider findCompilationUnit(TreeLogger logger,
          String typeName) {
        // ONLY LOOK FOR TOP-LEVEL TYPES.
        //
        CompilationUnitProvider cup = cups.get(typeName);
        if (cup == null) {
          String path = typeName.replace('.', '/') + ".java";
          URL url = fo.find(path);
          if (url != null) {
            String pkgName = "";
            int len = findLengthOfPackagePart(typeName);
            if (len > 0) {
              pkgName = typeName.substring(0, len);
            }
            return new URLCompilationUnitProvider(url, pkgName);
          } else {
            return null;
          }
        }
        return cup;
      }

      public boolean isPackage(String possiblePackageName) {
        String path = possiblePackageName.replace('.', '/') + "/";
        URL url = fo.find(path);
        if (url != null) {
          return true;
        } else {
          return false;
        }
      }

      private int findLengthOfPackagePart(String typeName) {
        int maxDotIndex = 0;
        int i = typeName.indexOf('.');
        while (i != -1) {
          if (!isPackage(typeName.substring(0, i))) {
            break;
          } else {
            maxDotIndex = i;
            i = typeName.indexOf('.', i + 1);
          }
        }
        return maxDotIndex;
      }

      private Map<String, CompilationUnitProvider> cups = new HashMap<String, CompilationUnitProvider>();
    };

    ByteCodeCompiler cs = new ByteCodeCompiler(host);
    String[] allJava = fo.getAllFiles();

    for (int i = 0; i < 3; ++i) {
      long before = System.currentTimeMillis();

      for (int j = 0; j < allJava.length; j++) {
        String typeName = allJava[j].substring(0, allJava[j].length() - 5).replace(
            '/', '.');
        cs.getClassBytes(logger, typeName);
      }

      long after = System.currentTimeMillis();
      System.out.println("Iter " + i + " took " + (after - before) + " ms");
    }
  }

  // This one is standalone.
  //
  public void testJavaLangObject() throws Exception {
    ByteCodeCompiler cs = new ByteCodeCompiler(testHost);
    assertNotNull(cs.getClassBytes(logger, "java.lang.Object"));
  }

  // This one requires java.lang.Object.
  //
  public void testJavaLangString() throws Exception {
    ByteCodeCompiler cs = new ByteCodeCompiler(testHost);
    assertNotNull(cs.getClassBytes(logger, "java.lang.Object"));
    assertNotNull(cs.getClassBytes(logger, "java.lang.String"));
  }

  // Try deferred binding that takes three compile iterations.
  // - In Main, we rebind from A to C
  // - In C, we rebind from C to D.E
  //
  public void testRebindCreateTransitive() throws Exception {
    ByteCodeCompiler cs = new ByteCodeCompiler(new TestByteCodeCompilerHost() {
      public String rebind(TreeLogger logger, String typeName)
          throws com.google.gwt.core.ext.UnableToCompleteException {
        if ("test.C.Message".equals(typeName)) {
          return "D.E";
        } else {
          return typeName;
        }
      }
    });

    assertNotNull(cs.getClassBytes(logger, "test.C"));
    assertNotNull(cs.getClassBytes(logger, "test.C$Message"));
    assertNotNull(cs.getClassBytes(logger, "D"));
    assertNotNull(cs.getClassBytes(logger, "D$E"));

    assertNotNull(cs.getClassBytes(logger, "java.lang.Object"));
    assertNotNull(cs.getClassBytes(logger, "java.lang.String"));
    assertNotNull(cs.getClassBytes(logger, "java.lang.Class"));
    assertNotNull(cs.getClassBytes(logger, "com.google.gwt.core.client.GWT"));
  }

  // Try deferred binding that works.
  //
  public void testRebindCreateWithSuccess() throws Exception {
    ByteCodeCompiler cs = new ByteCodeCompiler(new TestByteCodeCompilerHost() {
      public String rebind(TreeLogger logger, String typeName)
          throws com.google.gwt.core.ext.UnableToCompleteException {
        if ("test.A".equals(typeName)) {
          return "test.A.B";
        } else {
          return typeName;
        }
      }
    });

    assertNotNull(cs.getClassBytes(logger, "java.lang.Object"));
    assertNotNull(cs.getClassBytes(logger, "java.lang.String"));
    assertNotNull(cs.getClassBytes(logger, "java.lang.Class"));
    assertNotNull(cs.getClassBytes(logger, "com.google.gwt.core.client.GWT"));

    assertNotNull(cs.getClassBytes(logger, "test.Main"));
    assertNotNull(cs.getClassBytes(logger, "test.A"));
    assertNotNull(cs.getClassBytes(logger, "test.A$B"));

    // Check again for the same class to make sure it's cached.
    // (Although you have to run this test with "-Dgwt.useGuiLogger" defined
    // to see what it does.)
    //
    assertNotNull(cs.getClassBytes(logger, "java.lang.Object"));
  }

  protected void setUp() throws Exception {
    logger = Loggers.createOptionalGuiTreeLogger();
    testHost = new TestByteCodeCompilerHost();
  }

  private TreeLogger logger = TreeLogger.NULL;
  private TestByteCodeCompilerHost testHost = null;
}
