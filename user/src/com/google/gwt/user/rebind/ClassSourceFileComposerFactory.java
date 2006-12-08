/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.user.rebind;

import com.google.gwt.core.ext.GeneratorContext;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Factory clas to create <code>ClassSourceFileComposer</code> instances.
 * 
 */
public class ClassSourceFileComposerFactory {
  /**
   * Represents a java source file category. Right now support interface and
   * class, later should support abstract class, static class, etc.
   */
  public static class JavaSourceCategory extends Enum {
    /**
     * This type is a class.
     */
    public static final JavaSourceCategory CLASS;

    /**
     * This type is a interface.
     */
    public static final JavaSourceCategory INTERFACE;
    static Map pool = new HashMap();

    static {
      CLASS = new JavaSourceCategory("class");
      INTERFACE = new JavaSourceCategory("interface");
    }

    public static JavaSourceCategory require(String key) {
      return (JavaSourceCategory) Enum.require(key, pool);
    }

    protected JavaSourceCategory(String key) {
      super(key, pool);
    }
  }

  private JavaSourceCategory classCategory = JavaSourceCategory.CLASS;

  private String classComment;

  private String className;

  private Set imports = new HashSet();

  private Set interfaceNames = new HashSet();

  private String packageName;

  private String superClassName;

  public ClassSourceFileComposerFactory(String packageName, String className) {
    this.packageName = packageName;
    this.className = className;
  }

  public void addImplementedInterface(String intfName) {
    interfaceNames.add(intfName);
  }

  public void addImport(String typeName) {
    imports.add(typeName);
  }

  /**
   * Creates an implementation of {@link SourceWriter} that can be used to write
   * the innards of a class. Note that the subsequent changes to this factory do
   * not affect the returned instance.
   * 
   * @throws RuntimeException If the settings on this factory are inconsistent
   *           or invalid
   */
  public SourceWriter createSourceWriter(GeneratorContext ctx,
      PrintWriter printWriter) {
    return new ClassSourceFileComposer(ctx, printWriter, getCreatedPackage(),
        getCreatedClassShortName(), getSuperclassName(), getInterfaceNames(),
        getImports(), classCategory, classComment);
  }

  /**
   * Creates an implementation of {@link SourceWriter} that can be used to write
   * the innards of a class. Note that the subsequent changes to this factory do
   * not affect the returned instance.
   * 
   * @param printWriter underlying writer
   * @return the source writer
   * @throws RuntimeException If the settings on this factory are inconsistent
   *           or invalid
   */
  public SourceWriter createSourceWriter(PrintWriter printWriter) {
    return new ClassSourceFileComposer(null, printWriter, getCreatedPackage(),
        getCreatedClassShortName(), getSuperclassName(), getInterfaceNames(),
        getImports(), classCategory, classComment);
  }

  public String getCreatedClassName() {
    return getCreatedPackage() + "." + getCreatedClassShortName();
  }

  public String getCreatedClassShortName() {
    return className;
  }

  public String getCreatedPackage() {
    return packageName;
  }

  public String[] getInterfaceNames() {
    return (String[]) interfaceNames.toArray(new String[interfaceNames.size()]);
  }

  public String getSuperclassName() {
    return superClassName;
  }

  /**
   * This class is an interface.
   */
  public void makeInterface() {
    classCategory = JavaSourceCategory.INTERFACE;
  }

  /**
   * Sets the java doc comment for <code>this</code>.
   * 
   * @param comment java doc comment.
   */
  public void setJavaDocCommentForClass(String comment) {
    classComment = comment;
  }

  public void setSuperclass(String superclassName) {
    superClassName = superclassName;
  }

  private String[] getImports() {
    return (String[]) imports.toArray(new String[imports.size()]);
  }
}
