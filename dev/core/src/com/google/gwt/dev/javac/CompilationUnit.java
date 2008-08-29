/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.jdt.TypeRefVisitor;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the state of a single active compilation unit in a particular
 * module. State is accumulated throughout the life cycle of the containing
 * module and may be invalidated at certain times and recomputed.
 */
public abstract class CompilationUnit {

  /**
   * Tracks the state of a compilation unit through the compile and recompile
   * process.
   */
  enum State {
    /**
     * All internal state is cleared; the unit's source has not yet been
     * compiled by JDT.
     */
    FRESH,
    /**
     * In this intermediate state, the unit's source has been compiled by JDT.
     * The unit will contain a set of CompiledClasses.
     */
    COMPILED,
    /**
     * In this final state, the unit was compiled, but contained one or more
     * errors. Those errors are cached inside the unit, but all other internal
     * state is cleared.
     */
    ERROR,
    /**
     * In this final state, the unit has been compiled and is error free.
     * Additionally, all other units this unit depends on (transitively) are
     * also error free. The unit contains a set of checked CompiledClasses. The
     * unit and each contained CompiledClass releases all references to the JDT
     * AST. Each class contains a reference to a valid JRealClassType, which has
     * been added to the module's TypeOracle, as well as byte code, JSNI
     * methods, and all other final state.
     */
    CHECKED
  }

  private class FindTypesInCud extends ASTVisitor {
    Map<SourceTypeBinding, CompiledClass> map = new IdentityHashMap<SourceTypeBinding, CompiledClass>();

    public Set<CompiledClass> getClasses() {
      return new HashSet<CompiledClass>(map.values());
    }

    @Override
    public boolean visit(TypeDeclaration typeDecl, BlockScope scope) {
      CompiledClass enclosingClass = map.get(typeDecl.binding.enclosingType());
      assert (enclosingClass != null);
      /*
       * Weird case: if JDT determines that this local class is totally
       * uninstantiable, it won't bother allocating a local name.
       */
      if (typeDecl.binding.constantPoolName() != null) {
        CompiledClass newClass = new CompiledClass(CompilationUnit.this,
            typeDecl, enclosingClass);
        map.put(typeDecl.binding, newClass);
      }
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration typeDecl, ClassScope scope) {
      CompiledClass enclosingClass = map.get(typeDecl.binding.enclosingType());
      assert (enclosingClass != null);
      CompiledClass newClass = new CompiledClass(CompilationUnit.this,
          typeDecl, enclosingClass);
      map.put(typeDecl.binding, newClass);
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration typeDecl, CompilationUnitScope scope) {
      assert (typeDecl.binding.enclosingType() == null);
      CompiledClass newClass = new CompiledClass(CompilationUnit.this,
          typeDecl, null);
      map.put(typeDecl.binding, newClass);
      return true;
    }
  }

  private static Set<String> computeFileNameRefs(CompilationUnitDeclaration cud) {
    final Set<String> result = new HashSet<String>();
    cud.traverse(new TypeRefVisitor() {
      @Override
      protected void onTypeRef(SourceTypeBinding referencedType,
          CompilationUnitDeclaration unitOfReferrer) {
        // Map the referenced type to the target compilation unit file.
        result.add(String.valueOf(referencedType.getFileName()));
      }
    }, cud.scope);
    return result;
  }

  private CompilationUnitDeclaration cud;
  private CategorizedProblem[] errors;
  private Set<CompiledClass> exposedCompiledClasses;
  private Set<String> fileNameRefs;
  private State state = State.FRESH;

  /**
   * Overridden to finalize; always returns object identity.
   */
  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * Returns the user-relevant location of the source file. No programmatic
   * assumptions should be made about the return value.
   */
  public abstract String getDisplayLocation();

  /**
   * Returns the source code for this unit.
   */
  public abstract String getSource();

  /**
   * Returns the fully-qualified name of the top level public type.
   */
  public abstract String getTypeName();

  /**
   * Overridden to finalize; always returns identity hash code.
   */
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * Returns <code>true</code> if this unit is compiled and valid.
   */
  public boolean isCompiled() {
    return state == State.COMPILED || state == State.CHECKED;
  }

  public boolean isError() {
    return state == State.ERROR;
  }

  /**
   * Returns <code>true</code> if this unit was generated by a
   * {@link com.google.gwt.core.ext.Generator}.
   */
  public abstract boolean isGenerated();

  /**
   * Overridden to finalize; always returns {@link #getDisplayLocation()}.
   */
  public final String toString() {
    return getDisplayLocation();
  }

  /**
   * Called when this unit no longer needs to keep an internal cache of its
   * source.
   */
  protected void dumpSource() {
  }

  /**
   * If compiled, returns all contained classes; otherwise returns
   * <code>null</code>.
   */
  Set<CompiledClass> getCompiledClasses() {
    if (!isCompiled()) {
      return null;
    }
    if (exposedCompiledClasses == null) {
      FindTypesInCud typeFinder = new FindTypesInCud();
      cud.traverse(typeFinder, cud.scope);
      Set<CompiledClass> compiledClasses = typeFinder.getClasses();
      exposedCompiledClasses = Collections.unmodifiableSet(compiledClasses);
    }
    return exposedCompiledClasses;
  }

  CategorizedProblem[] getErrors() {
    return errors;
  }

  Set<String> getFileNameRefs() {
    if (fileNameRefs == null) {
      fileNameRefs = computeFileNameRefs(cud);
    }
    return fileNameRefs;
  }

  /**
   * If compiled, returns the JDT compilation unit declaration; otherwise
   * <code>null</code>.
   */
  CompilationUnitDeclaration getJdtCud() {
    return cud;
  }

  State getState() {
    return state;
  }

  /**
   * Sets the compiled JDT AST for this unit.
   */
  void setJdtCud(CompilationUnitDeclaration cud) {
    assert (state == State.FRESH || state == State.ERROR);
    this.cud = cud;
    state = State.COMPILED;
  }

  /**
   * Changes the compilation unit's internal state.
   */
  void setState(State newState) {
    assert (newState != State.COMPILED);
    if (state == newState) {
      return;
    }
    state = newState;

    dumpSource();
    switch (newState) {
      case CHECKED:
        // Must cache before we destroy the cud.
        assert (cud != null);
        getFileNameRefs();
        for (CompiledClass compiledClass : getCompiledClasses()) {
          compiledClass.checked();
        }
        cud = null;
        break;

      case ERROR:
        this.errors = cud.compilationResult().getErrors();
        invalidate();
        break;
      case FRESH:
        this.errors = null;
        invalidate();
        break;
    }
  }

  /**
   * Removes all accumulated state associated with compilation.
   */
  private void invalidate() {
    cud = null;
    fileNameRefs = null;
    if (exposedCompiledClasses != null) {
      for (CompiledClass compiledClass : exposedCompiledClasses) {
        compiledClass.invalidate();
      }
      exposedCompiledClasses = null;
    }
  }
}
