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
package com.google.gwt.codegen.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A builder for {@link JavaSourceWriter} instances.
 * <p>
 * Experimental API - subject to change.
 */
public class JavaSourceWriterBuilder {
 
  private final String className;
  private final String packageName;
  private final AbortablePrintWriter printWriter;

  private final List<String> annotations = new ArrayList<String>();

  private boolean isClass = true;

  private String classComment;

  private final Set<String> imports = new TreeSet<String>();

  private final Set<String> interfaceNames = new LinkedHashSet<String>();

  private String superClassName;

  /**
   * @param printWriter
   * @param packageName
   * @param className
   */
  public JavaSourceWriterBuilder(AbortablePrintWriter printWriter, String packageName,
      String className) {
    this.printWriter = printWriter;
    this.packageName = packageName;
    this.className = className;
  }

  /**
   * Add an class/interface annotation.
   * 
   * @param declaration
   */
  public void addAnnotationDeclaration(String declaration) {
    annotations.add(declaration);
  }

  /**
   * Add an implemented/extended interface.
   * 
   * @param intfName
   */
  public void addImplementedInterface(String intfName) {
    interfaceNames.add(intfName);
  }

  /**
   * Add an import entry.
   * 
   * @param typeName fully-qualified source name
   */
  public void addImport(String typeName) {
    imports.add(typeName);
  }

  /**
   * Creates an implementation of {@link JavaSourceWriter} that can be used to write
   * the innards of a class. Note that the subsequent changes to this factory do
   * not affect the returned instance.
   * 
   * @return a {@link JavaSourceWriter} instance
   * @throws RuntimeException If the settings on this factory are inconsistent
   *           or invalid
   */
  public SourceWriter createSourceWriter() {
    return new JavaSourceWriter(printWriter, packageName, imports, isClass, classComment,
        annotations, className, superClassName, interfaceNames);
  }

  /**
   * Get the annotations.
   * 
   * @return list of annotations
   */
  public Iterable<String> getAnnotationDeclarations() {
    return annotations;
  }

  /**
   * Get the simple name of the class being created.
   * 
   * @return class name
   */
  public String getClassName() {
    return className;
  }

  /**
   * Get the fully-qualified source name of the class being created.
   * 
   * @return fqcn
   */
  public String getFullyQualifiedClassName() {
    return getPackageName() + "." + getClassName();
  }

  /**
   * Get the implemented/extended interfaces for the class being created.
   * 
   * @return list of interface names
   */
  public Iterable<String> getInterfaceNames() {
    return interfaceNames;
  }

  /**
   * Get the package of the class being created.
   * 
   * @return package name
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Get the superclass for the class being created.
   * 
   * @return superclass name
   */
  public String getSuperclassName() {
    return superClassName;
  }

  /**
   * We are creating an interface instead of a class.
   */
  public void makeInterface() {
    isClass = false;
  }

  /**
   * Sets the java doc comment for <code>this</code>.
   * 
   * @param comment java doc comment.
   */
  public void setJavaDocCommentForClass(String comment) {
    classComment = comment;
  }

  /**
   * Set the superclass of the class being created.
   * 
   * @param superclassName
   */
  public void setSuperclass(String superclassName) {
    superClassName = superclassName;
  }
}
