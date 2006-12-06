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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides a mutable compilation service host on top of a
 * {@link com.google.gwt.dev.typeinfo.TypeOracle} as well as providing
 * subclasses an opportunity to substitute their own source to implement
 * special-handling, such as rewriting JSNI source.
 */
public class StandardSourceOracle implements SourceOracle {

  /**
   * @param typeOracle answers questions about compilation unit locations
   * @param genDir for compilation units whose location does not correspond to a
   *          URL that can be opened, their source will be written to this
   *          directory to support debugging, or <code>null</code> if the
   *          source need not be written to disk
   */
  public StandardSourceOracle(TypeOracle typeOracle) {
    this.typeOracle = typeOracle;
  }

  /**
   * Attempts to find the compilation unit for the requested type. Often
   * legitimately returns <code>null</code> because the compilation service
   * does tests to help determine whether a particular symbol refers to a
   * package or a type.
   */
  public final CompilationUnitProvider findCompilationUnit(TreeLogger logger,
      String typeName) throws UnableToCompleteException {

    // Check the cache first.
    //
    CompilationUnitProvider cup =
        (CompilationUnitProvider) cupsByTypeName.get(typeName);

    if (cup != null) {
      // Found in cache.
      //
      return cup;
    }

    // See if the type oracle can find it.
    //
    JClassType type = typeOracle.findType(typeName);
    if (type != null) {
      // All type oracle types are supposed to know their compilation unit.
      //
      cup = type.getCompilationUnit();
      assert (cup != null);
    }

    // Give the subclass a chance to replace the source. This used for JSNI in
    // hosted mode at present but could be used for any source rewriting trick.
    //
    if (cup != null) {
      try {
        CompilationUnitProvider specialCup =
            doFilterCompilationUnit(logger, typeName, cup);

        if (specialCup != null) {
          // Use the cup that the subclass returned instead. Note that even
          // though this file may not exist on disk, it is special so we don't
          // want users to have to debug into it unless they specifically ask
          // to.
          //
          cup = specialCup;
        }
      } catch (UnableToCompleteException e) {
        String location = cup.getLocation();
        char[] source = cup.getSource();
        Util.maybeDumpSource(logger, location, source, typeName);
        throw e;
      }
    }

    if (cup == null) {
      // Did not find a cup for the type.
      // This happens commonly and is not a cause for alarm.
      //
      return null;
    }

    // Remember its package and cache it.
    //
    cupsByTypeName.put(typeName, cup);
    return cup;
  }

  public TypeOracle getTypeOracle() {
    return typeOracle;
  }

   void invalidateCups(Set typeNames) {
   cupsByTypeName.keySet().removeAll(typeNames);
  }

  /**
   * Determines whether or not a particular name is a package name.
   */
  public final boolean isPackage(String possiblePackageName) {
    if (knownPackages.contains(possiblePackageName)) {
      // The quick route -- we've already answered yes to this question.
      // OPTIMIZE: cache NOs as well
      return true;
    }

    if (typeOracle.findPackage(possiblePackageName) != null) {
      // Was a package know on the source path.
      //
      rememberPackage(possiblePackageName);
      return true;
    } else {
      // Not a package.
      //
      return false;
    }
  }

  /**
   * Subclasses can override this method if they use a special mechanism to find
   * the compilation unit for a type. For example, rewriting source code (as
   * with JSNI) or preempting the source for a given type (as with
   * <code>GWT.create</code>).
   * <p>
   * Note that subclasses should <em>not</em> call
   * <code>super.{@link #findCompilationUnit(TreeLogger, String)}</code> in
   * their implementation.
   * 
   * @return <code>null</code> if you want the superclass to use its normal
   *         mechanism for finding types
   */
  protected CompilationUnitProvider doFilterCompilationUnit(TreeLogger logger,
      String typeName, CompilationUnitProvider existing)
      throws UnableToCompleteException {
    return null;
  }

  /**
   * Subclasses can override this method if they use additional mechanisms to
   * find types magically.
   * <p>
   * Note that subclasses should <em>not</em> call
   * <code>super.{@link #findAdditionalTypesUsingMagic(TreeLogger, CompilationUnitDeclaration)}</code>
   * in their implementation.
   * 
   * @return <code>null</code> to indicate that no additional types should be
   *         added
   */
  protected String[] doFindAdditionalTypesUsingMagic(TreeLogger logger,
      CompilationUnitDeclaration unit) throws UnableToCompleteException {
    return null;
  }

  /**
   * Remember that this package was added. Used for generated packages.
   */
  private void rememberPackage(String packageName) {
    int i = packageName.lastIndexOf('.');
    if (i != -1) {
      // Ensure the parent package is also created.
      //
      rememberPackage(packageName.substring(0, i));
    }
    knownPackages.add(packageName);
  }

  private final Map cupsByTypeName = new HashMap();
  private final Set knownPackages = new HashSet();
  private final TypeOracle typeOracle;
}
