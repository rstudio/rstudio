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
package com.google.gwt.dev.util;

import java.util.regex.Pattern;

/**
 * Utility methods for dealing with the various types of Java names.
 */
public class Name {

  /**
   * Represents a Java class name in binary form, for example:
   * {@code org.example.Foo$Bar}.
   * 
   * See {@link "http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html#59892"}
   */
  public static class BinaryName {

    public static String getClassName(String binaryName) {
      assert isBinaryName(binaryName);
      int lastDot = binaryName.lastIndexOf('.');
      if (lastDot < 0) {
        return binaryName;
      }
      return binaryName.substring(lastDot + 1);
    }

    public static String getShortClassName(String binaryName) {
      assert isBinaryName(binaryName);
      String className = getClassName(binaryName);
      int lastDollar = className.lastIndexOf('$', className.length() - 2);
      if (lastDollar < 0) {
        return className;
      }
      return className.substring(lastDollar + 1);
    }

    public static String toInternalName(String binaryName) {
      assert isBinaryName(binaryName);
      return binaryName.replace('.', '/');
    }

    public static String toSourceName(String binaryName) {
      assert isBinaryName(binaryName);
      // don't change a trailing $ to a .
      return NON_TRAILING_DOLLAR.matcher(binaryName).replaceAll(".$1");
    }
    
    private BinaryName() {
    }
  }

  /**
     * Represents a Java class name in internal form, for example:
     * {@code org/example/Foo$Bar}.
     * 
     * See {@link "http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#14757"}
     */
    public static class InternalName {
  
      public static String getClassName(String name) {
        assert isInternalName(name);
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash < 0) {
          return name;
        }
        return name.substring(lastSlash + 1);
      }
  
      public static String toBinaryName(String internalName) {
        assert isInternalName(internalName);
        return internalName.replace('/', '.');
      }
      
      public static String toSourceName(String internalName) {
        assert isInternalName(internalName);
        // don't change a trailing $ or slash to a .
        return NON_TRAILING_DOLLAR_SLASH.matcher(internalName).replaceAll(".$1");
      }
  
      private InternalName() {
      }
    }

  /**
   * Represents a Java class name in either source or binary form, for example:
   * {@code org.example.Foo.Bar or org.example.Foo$Bar}.
   * 
   * See {@link "http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html#59892"}
   */
  public static class SourceOrBinaryName {

    public static String toSourceName(String dottedName) {
      // don't change a trailing $ to a .
      return NON_TRAILING_DOLLAR.matcher(dottedName).replaceAll(".$1");
    }
  }

  // Non-trailing $
  private static final Pattern NON_TRAILING_DOLLAR =
    Pattern.compile("[$](\\p{javaJavaIdentifierStart})");

  // Non-trailing $ or /
  private static final Pattern NON_TRAILING_DOLLAR_SLASH =
    Pattern.compile("[$/](\\p{javaJavaIdentifierStart})");

  /**
   * Get the binary name for a Java class.
   * 
   * @param clazz class literal
   * @return binary name for the class
   */
  public static String getBinaryNameForClass(Class<?> clazz) {
    return clazz.getName();
  }

  /**
   * Get the internal name for a Java class.
   * 
   * @param clazz class literal
   * @return internal name for the class
   */
  public static String getInternalNameForClass(Class<?> clazz) {
    return BinaryName.toInternalName(getBinaryNameForClass(clazz));
  }

  /**
   * Get the source name for a Java class.
   * 
   * @param clazz class literal
   * @return source name for the class
   */
  public static String getSourceNameForClass(Class<?> clazz) {
    return clazz.getCanonicalName();
  }

  /**
   * @return true if name could be a valid binary name.
   * 
   * Note that many invalid names might pass this test -- in particular, source
   * names cannot be verified to know they are not valid binary names without
   * being able to tell the package name part of the name.
   * 
   * @param name class name to test
   */
  public static boolean isBinaryName(String name) {
    return name == null || !name.contains("/");
  }

  /**
   * @return true if name could be a valid internal name.
   * 
   * Note that many invalid names might pass this test.
   * 
   * @param name class name to test
   */
  public static boolean isInternalName(String name) {
    return name == null || !name.contains(".");
  }

  /**
   * @return true if name could be a valid source name.
   * 
   * Note that many invalid names might pass this test.
   * 
   * @param name class name to test
   */
  public static boolean isSourceName(String name) {
    if (name == null) {
      return true;
    }
    int dollar = name.indexOf('$');
    return !name.contains("/") && (dollar < 0 || dollar == name.length() - 1);
  }
  
  private Name() {
  }
}
