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

  public char[] getMainTypeName() {
    String mainTypeName = cup.getLocation();
    int nameStart = mainTypeName.lastIndexOf(File.separatorChar);
    if (nameStart != -1) {
      mainTypeName = mainTypeName.substring(nameStart + 1);
    }

    /*
     * This is required to resolve the package-info class.
     */
    int ext = mainTypeName.lastIndexOf(".java");
    if (ext != -1) {
      return mainTypeName.substring(0, ext).toCharArray();
    }

    // seems to work just returning null
    return null;
  }

  public char[][] getPackageName() {
    final char[] pkg = cup.getPackageName().toCharArray();
    final char[][] pkgParts = CharOperation.splitOn('.', pkg);
    return pkgParts;
  }
}
