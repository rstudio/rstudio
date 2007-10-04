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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

import java.io.File;

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
   * This method is supposed to return the simple class name for
   * this compilation unit. Examples of simple class names would
   * be "String", or "ArrayList". JDT allows this method to return
   * null in the cases where this compilation unit is not a package-info
   * class.
   */
  public char[] getMainTypeName() {

    // Note that cup.getLocation() can either return a path, a URL,
    // or "transient source for ..."
    String mainTypeName = cup.getLocation();

    // Let's check to see if we are dealing with a path to a java file,
    // or something else

    int ext = mainTypeName.lastIndexOf(".java");
    if (ext == -1) {
      // not a path to a java file, return null
      return null;
    }

    mainTypeName = mainTypeName.substring(0, ext);

    int nameStart = mainTypeName.lastIndexOf(File.separatorChar);

    // We're not dealing with a path, so check for the URL separator
    if (nameStart == -1) {
      nameStart = mainTypeName.lastIndexOf('/');
    }

    // If we do not find a separator, then this is a simple name. The
    // substring call will act as a no-op.
    mainTypeName = mainTypeName.substring(nameStart + 1);

    return mainTypeName.toCharArray();
  }

  public char[][] getPackageName() {
    final char[] pkg = cup.getPackageName().toCharArray();
    final char[][] pkgParts = CharOperation.splitOn('.', pkg);
    return pkgParts;
  }
}
