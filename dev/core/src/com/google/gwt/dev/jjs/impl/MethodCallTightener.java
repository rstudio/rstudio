// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;

/**
 * Update polymorphic method calls to tighter bindings based on the type of the
 * qualifier. For a given polymorphic method call to a non-final target, see if
 * the static type of the qualifer would let us target an override instead.
 * 
 * This is possible because the qualifier might have been tightened by
 * {@link com.google.gwt.dev.jjs.impl.TypeTightener}.
 */
public class MethodCallTightener {

  private final JProgram program;

  /**
   * Updates polymorphic method calls to tighter bindings based on the type of 
   * the qualifier.
   */
  public class MethodCallTighteningVisitor extends JVisitor {
    private final ChangeList changeList = new ChangeList(
      "Update polymorphic method calls to tighter bindings based on the type of the qualifier.");

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public void endVisit(JMethodCall x, Mutator m) {
      JMethod method = x.getTarget();
      JExpression instance = x.getInstance();

      // The method call is already known statically
      if (!x.canBePolymorphic()) {
        return;
      }

      JType instanceType = instance.getType();
      JReferenceType enclosingType = method.getEnclosingType();

      if (instanceType == enclosingType
        || instanceType instanceof JInterfaceType) {
        // This method call is as tight as it can be for the type of the
        // qualifier
        return;
      }

      if (instanceType instanceof JArrayType) {
        // shouldn't get here; arrays don't have extra methods
        return;
      }

      if (instanceType instanceof JNullType) {
        // TypeTightener will handle this case
        return;
      }

      assert (instanceType instanceof JClassType);

      /*
       * Search myself and all my super types to find a tighter implementation
       * of the called method, if possible.
       */
      JMethod foundMethod = null;
      JClassType type;
      outer : for (type = (JClassType) instanceType; type != null
        && type != enclosingType; type = type.extnds) {
        for (int i = 0; i < type.methods.size(); ++i) {
          JMethod methodIt = (JMethod) type.methods.get(i);
          if (JProgram.methodsDoMatch(method, methodIt)) {
            foundMethod = methodIt;
            break outer;
          }
        }
      }

      if (foundMethod == null) {
        return;
      }

      ChangeList changes = new ChangeList("Replace call '" + x + "' to type '"
        + enclosingType + "' with a call to type '"
        + foundMethod.getEnclosingType() + "'");

      // Update the call site
      JMethodCall call = new JMethodCall(program, null, foundMethod);
      call.setCanBePolymorphic(true);
      changes.replaceExpression(m, call);

      // Copy the qualifier
      changes.replaceExpression(call.instance, x.instance);

      // Copy the args
      for (int i = 0; i < x.args.size(); ++i) {
        Mutator arg = x.args.getMutator(i);
        changes.addExpression(arg, call.args);
      }

      changeList.add(changes);

      return;
    }
  }

  private MethodCallTightener(JProgram program) {
    this.program = program;
  }

  private boolean execImpl() {
    MethodCallTighteningVisitor tightener = new MethodCallTighteningVisitor();
    program.traverse(tightener);
    ChangeList changes = tightener.getChangeList();
    if (changes.empty()) {
      return false;
    }
    
    changes.apply();
    return true;
  }

  public static boolean exec(JProgram program) {
    return new MethodCallTightener(program).execImpl();
  }

}
