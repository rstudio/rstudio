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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

/**
 * Implements <code>ICompilationUnit</code> in terms of a
 * {@link CompilationUnitProvider}.
 */
public class ICompilationUnitAdapter implements ICompilationUnit {

  private final CompilationUnitProvider cup;

  public ICompilationUnitAdapter(CompilationUnitProvider cup) {
    assert (cup != null);
    this.cup = cup;
  }

  public CompilationUnitProvider getCompilationUnitProvider() {
    return cup;
  }

  public char[] getContents() {
    try {
      return cup.getSource();
    } catch (UnableToCompleteException e) {
      return null;
    }
  }

  public char[] getFileName() {
    return cup.getLocation().toCharArray();
  }

  /**
   * This method is supposed to return the simple class name for this
   * compilation unit. Examples of simple class names would be "String", or
   * "ArrayList". JDT allows this method to return null in the cases where this
   * compilation unit is not a package-info class.
   */
  public char[] getMainTypeName() {
    String typeName = cup.getMainTypeName();
    if (typeName != null) {
      return typeName.toCharArray();
    }
    return null;
  }

  public char[][] getPackageName() {
    final char[] pkg = cup.getPackageName().toCharArray();
    final char[][] pkgParts = CharOperation.splitOn('.', pkg);
    return pkgParts;
  }
}
