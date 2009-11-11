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

import com.google.gwt.dev.util.InstalledHelpInfo;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Check a compilation unit for violations of
 * {@link com.google.gwt.core.client.JavaScriptObject JavaScriptObject} (JSO)
 * restrictions. The restrictions are summarized in
 * <code>jsoRestrictions.html</code>.
 * 
 * 
 * Any violations found are attached as errors on the
 * CompilationUnitDeclaration.
 * 
 * @see <a
 *      href="http://code.google.com/p/google-web-toolkit/wiki/OverlayTypes">Overlay
 *      types design doc</a>
 * @see jsoRestrictions.html
 */
public class JSORestrictionsChecker {

  /**
   * The order in which the checker will process types is undefined, so this
   * type accumulates the information necessary for sanity-checking the JSO
   * types.
   */
  public static class CheckerState {

    /**
     * This maps JSO implementation types to their implemented SingleJsoImpl
     * interfaces.
     */
    private final Map<TypeDeclaration, Set<String>> jsoImplsToInterfaces = new HashMap<TypeDeclaration, Set<String>>();

    /**
     * Used for error reporting.
     */
    private final Map<TypeDeclaration, CompilationUnitDeclaration> nodesToCuds = new IdentityHashMap<TypeDeclaration, CompilationUnitDeclaration>();

    /**
     * This method should be called after all CUDs are passed into check().
     */
    public void finalCheck() {
      /*
       * Ensure that every interfaces has exactly zero or one JSO subtype that
       * implements it.
       */
      Map<String, TypeDeclaration> singleImplementations = new HashMap<String, TypeDeclaration>();
      for (Map.Entry<TypeDeclaration, Set<String>> entry : jsoImplsToInterfaces.entrySet()) {
        TypeDeclaration node = entry.getKey();
        for (String intfName : entry.getValue()) {

          if (!singleImplementations.containsKey(intfName)) {
            singleImplementations.put(intfName, node);
          } else {
            /*
             * Emit an error if the previously-defined type is neither a
             * supertype nor subtype of the current type
             */
            TypeDeclaration previous = singleImplementations.get(intfName);
            if (!(hasSupertypeNamed(node, previous.binding.compoundName) || hasSupertypeNamed(
                previous, node.binding.compoundName))) {
              String nodeName = CharOperation.toString(node.binding.compoundName);
              String previousName = CharOperation.toString(previous.binding.compoundName);

              // Provide consistent reporting, regardless of visitation order
              if (nodeName.compareTo(previousName) < 0) {
                String msg = errAlreadyImplemented(intfName, nodeName,
                    previousName);
                errorOn(node, nodesToCuds.get(node), msg);
                errorOn(previous, nodesToCuds.get(previous), msg);
              } else {
                String msg = errAlreadyImplemented(intfName, previousName,
                    nodeName);
                errorOn(previous, nodesToCuds.get(previous), msg);
                errorOn(node, nodesToCuds.get(node), msg);
              }
            }
          }
        }
      }
    }

    public void retainAll(Collection<CompilationUnit> units) {
      // Fast-path for removing everything
      if (units.isEmpty()) {
        jsoImplsToInterfaces.clear();
        nodesToCuds.clear();
        return;
      }

      // Build up a list of the types that should be retained
      Set<String> retainedTypeNames = new HashSet<String>();

      for (CompilationUnit u : units) {
        for (CompiledClass c : u.getCompiledClasses()) {
          // Can't rely on getJdtCud() because those are pruned
          retainedTypeNames.add(c.getSourceName());
        }
      }

      // Loop over all TypeDeclarations that we have
      for (Iterator<TypeDeclaration> it = nodesToCuds.keySet().iterator(); it.hasNext();) {
        TypeDeclaration decl = it.next();

        // Remove the TypeDeclaration if it's not in the list of retained types
        if (!retainedTypeNames.contains(CharOperation.toString(decl.binding.compoundName))) {
          it.remove();

          jsoImplsToInterfaces.remove(decl);
        }
      }
    }

    private void add(Map<TypeDeclaration, Set<String>> map,
        TypeDeclaration key, String value) {
      Set<String> set = map.get(key);
      if (set == null) {
        map.put(key, set = new HashSet<String>());
      }
      set.add(value);
    }

    private void addJsoInterface(TypeDeclaration jsoType,
        CompilationUnitDeclaration cud, String interfaceName) {
      nodesToCuds.put(jsoType, cud);
      add(jsoImplsToInterfaces, jsoType, interfaceName);
    }

    private boolean hasSupertypeNamed(TypeDeclaration type, char[][] qType) {
      ReferenceBinding b = type.binding;
      while (b != null) {
        if (CharOperation.equals(b.compoundName, qType)) {
          return true;
        }
        b = b.superclass();
      }
      return false;
    }
  }

  private class JSORestrictionsVisitor extends ASTVisitor implements
      ClassFileConstants {

    private final Stack<Boolean> isJsoStack = new Stack<Boolean>();

    @Override
    public void endVisit(AllocationExpression exp, BlockScope scope) {
      // Anywhere an allocation occurs is wrong.
      if (exp.type != null && isJsoSubclass(exp.type.resolveType(scope))) {
        errorOn(exp, ERR_NEW_JSO);
      }
    }

    @Override
    public void endVisit(ConstructorDeclaration meth, ClassScope scope) {
      if (!isJso()) {
        return;
      }
      if ((meth.arguments != null) && (meth.arguments.length > 0)) {
        errorOn(meth, ERR_CONSTRUCTOR_WITH_PARAMETERS);
      }
      if ((meth.modifiers & AccProtected) == 0) {
        errorOn(meth, ERR_NONPROTECTED_CONSTRUCTOR);
      }
      if (meth.statements != null && meth.statements.length > 0) {
        errorOn(meth, ERR_NONEMPTY_CONSTRUCTOR);
      }
    }

    @Override
    public void endVisit(FieldDeclaration field, MethodScope scope) {
      if (!isJso()) {
        return;
      }
      if (!field.isStatic()) {
        errorOn(field, ERR_INSTANCE_FIELD);
      }
    }

    @Override
    public void endVisit(MethodDeclaration meth, ClassScope scope) {
      if (!isJso()) {
        return;
      }
      if ((meth.modifiers & (AccFinal | AccPrivate | AccStatic)) == 0) {
        // The method's modifiers allow it to be overridden. Make
        // one final check to see if the surrounding class is final.
        if ((meth.scope == null) || !meth.scope.enclosingSourceType().isFinal()) {
          errorOn(meth, ERR_INSTANCE_METHOD_NONFINAL);
        }
      }

      // Should not have to check isStatic() here, but isOverriding() appears
      // to be set for static methods.
      if (!meth.isStatic()
          && (meth.binding != null && meth.binding.isOverriding())) {
        errorOn(meth, ERR_OVERRIDDEN_METHOD);
      }
    }

    @Override
    public void endVisit(TypeDeclaration type, BlockScope scope) {
      popIsJso();
    }

    @Override
    public void endVisit(TypeDeclaration type, ClassScope scope) {
      popIsJso();
    }

    @Override
    public void endVisit(TypeDeclaration type, CompilationUnitScope scope) {
      popIsJso();
    }

    @Override
    public boolean visit(TypeDeclaration type, BlockScope scope) {
      pushIsJso(checkType(type));
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration type, ClassScope scope) {
      pushIsJso(checkType(type));
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration type, CompilationUnitScope scope) {
      pushIsJso(checkType(type));
      return true;
    }

    private boolean checkType(TypeDeclaration type) {
      if (!isJsoSubclass(type.binding)) {
        return false;
      }

      if (type.enclosingType != null && !type.binding.isStatic()) {
        errorOn(type, ERR_IS_NONSTATIC_NESTED);
      }

      ReferenceBinding[] interfaces = type.binding.superInterfaces();
      if (interfaces != null) {
        for (ReferenceBinding interf : interfaces) {
          if (interf.methods() == null) {
            continue;
          }

          if (interf.methods().length > 0) {
            String intfName = CharOperation.toString(interf.compoundName);
            state.addJsoInterface(type, cud, intfName);
          }
        }
      }

      return true;
    }

    private boolean isJso() {
      return isJsoStack.peek();
    }

    private void popIsJso() {
      isJsoStack.pop();
    }

    private void pushIsJso(boolean isJso) {
      isJsoStack.push(isJso);
    }
  }

  static final String ERR_CONSTRUCTOR_WITH_PARAMETERS = "Constructors must not have parameters in subclasses of JavaScriptObject";
  static final String ERR_INSTANCE_FIELD = "Instance fields cannot be used in subclasses of JavaScriptObject";
  static final String ERR_INSTANCE_METHOD_NONFINAL = "Instance methods must be 'final' in non-final subclasses of JavaScriptObject";
  static final String ERR_IS_NONSTATIC_NESTED = "Nested classes must be 'static' if they extend JavaScriptObject";
  static final String ERR_NEW_JSO = "'new' cannot be used to create instances of JavaScriptObject subclasses; instances must originate in JavaScript";
  static final String ERR_NONEMPTY_CONSTRUCTOR = "Constructors must be totally empty in subclasses of JavaScriptObject";
  static final String ERR_NONPROTECTED_CONSTRUCTOR = "Constructors must be 'protected' in subclasses of JavaScriptObject";
  static final String ERR_OVERRIDDEN_METHOD = "Methods cannot be overridden in JavaScriptObject subclasses";
  static final String JSO_CLASS = "com/google/gwt/core/client/JavaScriptObject";

  /**
   * Checks an entire
   * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration}.
   * 
   */
  public static void check(CheckerState state, CompilationUnitDeclaration cud) {
    JSORestrictionsChecker checker = new JSORestrictionsChecker(state, cud);
    checker.check();
  }

  static String errAlreadyImplemented(String intfName, String impl1,
      String impl2) {
    return "Only one JavaScriptObject type may implement the methods of an "
        + "interface that declared methods. The interface (" + intfName
        + ") is implemented by both (" + impl1 + ") and (" + impl2 + ")";
  }

  private static void errorOn(ASTNode node, CompilationUnitDeclaration cud,
      String error) {
    GWTProblem.recordError(node, cud, error, new InstalledHelpInfo(
        "jsoRestrictions.html"));
  }

  private final CompilationUnitDeclaration cud;

  private final CheckerState state;

  private JSORestrictionsChecker(CheckerState state,
      CompilationUnitDeclaration cud) {
    this.cud = cud;
    this.state = state;
  }

  private void check() {
    cud.traverse(new JSORestrictionsVisitor(), cud.scope);
  }

  private void errorOn(ASTNode node, String error) {
    errorOn(node, cud, error);
  }

  private boolean isJsoSubclass(TypeBinding typeBinding) {
    if (!(typeBinding instanceof ReferenceBinding)) {
      return false;
    }
    ReferenceBinding binding = (ReferenceBinding) typeBinding;
    while (binding.superclass() != null) {
      if (JSO_CLASS.equals(String.valueOf(binding.superclass().constantPoolName()))) {
        return true;
      }
      binding = binding.superclass();
    }
    return false;
  }
}
