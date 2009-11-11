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
package com.google.gwt.dev.javac;

import com.google.gwt.core.client.impl.ArtificialRescue;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.collect.Lists;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import java.util.List;

/**
 * Checks the validity of ArtificialRescue annotations.
 * 
 * <ul>
 * <li>(1) The ArtificialRescue annotation is only used in generated code.</li>
 * <li>(2) The className value names a type known to GWT (ignoring access rules)
 * </li>
 * <li>(3) The methods and fields of the type are known to GWT</li>
 * </ul>
 */
public class ArtificialRescueChecker {
  private class Visitor extends ASTVisitor {

    {
      assert collectTypes || reportErrors : "No work to be done";
    }

    @Override
    public void endVisit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
      processType(localTypeDeclaration);
    }

    @Override
    public void endVisit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
      processType(memberTypeDeclaration);
    }

    @Override
    public void endVisit(TypeDeclaration typeDeclaration,
        CompilationUnitScope scope) {
      processType(typeDeclaration);
    }

    private void processArtificialRescue(Annotation rescue) {
      if (!allowArtificialRescue) {
        // Goal (1)
        GWTProblem.recordError(rescue, cud, onlyGeneratedCode(), null);
        return;
      }

      String className = null;
      String[] methods = Empty.STRINGS;
      String[] fields = Empty.STRINGS;
      for (MemberValuePair pair : rescue.memberValuePairs()) {
        String name = String.valueOf(pair.name);
        if ("className".equals(name)) {
          className = pair.value.constant.stringValue();
        } else if ("methods".equals(name)) {
          methods = stringArrayFromValue(pair.value);
        } else if ("fields".equals(name)) {
          fields = stringArrayFromValue(pair.value);
        }
      }

      assert className != null;

      if (collectTypes) {
        referencedTypes = Lists.add(referencedTypes, className);
      }

      boolean isArray = false;
      while (className.endsWith("[]")) {
        className = className.substring(0, className.length() - 2);
        if (collectTypes) {
          referencedTypes = Lists.add(referencedTypes, className);
        }
        isArray = true;
      }

      if (!reportErrors) {
        // Nothing else to do
        return;
      }

      // Goal (2)
      // Strip off any array-like extensions and just find base type

      // Fix JSNI primitive type names to something JDT will understand
      if (isArray && className.length() == 1) {
        switch (className.charAt(0)) {
          case 'B':
            className = "byte";
            break;
          case 'C':
            className = "char";
            break;
          case 'D':
            className = "double";
            break;
          case 'F':
            className = "float";
            break;
          case 'I':
            className = "int";
            break;
          case 'J':
            className = "long";
            break;
          case 'S':
            className = "short";
            break;
          case 'Z':
            className = "boolean";
            break;
        }
      }

      char[][] compoundName = CharOperation.splitOn('.',
          className.toCharArray());
      TypeBinding typeBinding = cud.scope.getType(compoundName,
          compoundName.length);
      if (typeBinding == null) {
        GWTProblem.recordError(rescue, cud, notFound(className), null);
      } else if (typeBinding instanceof ProblemReferenceBinding) {
        ProblemReferenceBinding problem = (ProblemReferenceBinding) typeBinding;
        if (problem.problemId() == ProblemReasons.NotVisible) {
          // Ignore
        } else if (problem.problemId() == ProblemReasons.NotFound) {
          GWTProblem.recordError(rescue, cud, notFound(className), null);
        } else {
          GWTProblem.recordError(rescue, cud,
              unknownProblem(className, problem), null);
        }
      } else if (typeBinding instanceof BaseTypeBinding) {
        // No methods or fields on primitive types (3)
        if (methods.length > 0) {
          GWTProblem.recordError(rescue, cud, noMethodsAllowed(), null);
        }

        if (fields.length > 0) {
          GWTProblem.recordError(rescue, cud, noFieldsAllowed(), null);
        }
      } else if (typeBinding instanceof ReferenceBinding) {
        ReferenceBinding ref = (ReferenceBinding) typeBinding;

        if (isArray) {
          // No methods or fields on array types (3)
          if (methods.length > 0) {
            GWTProblem.recordError(rescue, cud, noMethodsAllowed(), null);
          }

          if (fields.length > 0) {
            GWTProblem.recordError(rescue, cud, noFieldsAllowed(), null);
          }
        } else {
          // Check methods on reference types (3)
          for (String method : methods) {
            if (method.contains("@")) {
              GWTProblem.recordError(rescue, cud, nameAndTypesOnly(), null);
              continue;
            }
            JsniRef jsni = JsniRef.parse("@foo::" + method);
            if (jsni == null) {
              GWTProblem.recordError(rescue, cud, badMethodSignature(method),
                  null);
              continue;
            }

            if (jsni.memberName().equals(
                String.valueOf(ref.compoundName[ref.compoundName.length - 1]))) {
              // Constructor
            } else {
              MethodBinding[] methodBindings = ref.getMethods(jsni.memberName().toCharArray());
              if (methodBindings == null || methodBindings.length == 0) {
                GWTProblem.recordError(rescue, cud, noMethod(className,
                    jsni.memberName()), null);
                continue;
              }
            }
          }

          // Check fields on reference types (3)
          for (String field : fields) {
            if (ref.getField(field.toCharArray(), false) == null) {
              GWTProblem.recordError(rescue, cud, unknownField(field), null);
            }
          }
        }
      }
    }

    /**
     * Examine a TypeDeclaration for ArtificialRescue annotations. Delegates to
     * {@link #processArtificialRescue(Annotation)} to complete the processing.
     */
    private void processType(TypeDeclaration x) {
      if (x.annotations == null) {
        return;
      }

      for (Annotation a : x.annotations) {
        if (!ArtificialRescue.class.getName().equals(
            CharOperation.toString(((ReferenceBinding) a.resolvedType).compoundName))) {
          continue;
        }

        // Sometimes it's a SingleMemberAnnotation, other times it's not
        Expression value = null;
        if (a instanceof SingleMemberAnnotation) {
          value = ((SingleMemberAnnotation) a).memberValue;
        } else {
          for (MemberValuePair pair : a.memberValuePairs()) {
            if ("value".equals(String.valueOf(pair.name))) {
              value = pair.value;
              break;
            }
          }
        }

        assert value != null;
        if (value instanceof ArrayInitializer) {
          for (Expression e : ((ArrayInitializer) value).expressions) {
            processArtificialRescue((Annotation) e);
          }
        } else if (value instanceof Annotation) {
          processArtificialRescue((Annotation) value);
        } else {
          throw new InternalCompilerException(
              "Unable to process annotation with value of type "
                  + value.getClass().getName());
        }

        return;
      }
    }

    private String[] stringArrayFromValue(Expression value) {
      if (value instanceof StringLiteral) {
        return new String[] {value.constant.stringValue()};
      } else if (value instanceof ArrayInitializer) {
        ArrayInitializer init = (ArrayInitializer) value;
        String[] toReturn = new String[init.expressions == null ? 0
            : init.expressions.length];
        for (int i = 0; i < toReturn.length; i++) {
          toReturn[i] = init.expressions[i].constant.stringValue();
        }
        return toReturn;
      } else {
        throw new InternalCompilerException("Unhandled value type "
            + value.getClass().getName());
      }
    }
  }

  /**
   * Check the {@link ArtificialRescue} annotations in a CompilationUnit. Errors
   * are reported through {@link GWTProblem}.
   */
  public static void check(CompilationUnitDeclaration cud,
      boolean allowArtificialRescue) {
    new ArtificialRescueChecker(cud, allowArtificialRescue).check();
  }

  /**
   * Report all types named in {@link ArtificialRescue} annotations in a CUD. No
   * error checking is done.
   */
  public static List<String> collectReferencedTypes(
      CompilationUnitDeclaration cud) {
    return new ArtificialRescueChecker(cud).collect();
  }

  static String badMethodSignature(String method) {
    return "Bad method signature " + method;
  }

  static String nameAndTypesOnly() {
    return "Only method name and parameter types expected";
  }

  static String noFieldsAllowed() {
    return "Cannot refer to fields on array or primitive types";
  }

  static String noMethod(String className, String methodName) {
    return "No method named " + methodName + " in type " + className;
  }

  static String noMethodsAllowed() {
    return "Cannot refer to methods on array or primitive types";
  }

  static String notFound(String className) {
    return "Could not find type " + className;
  }

  static String onlyGeneratedCode() {
    return "The " + ArtificialRescue.class.getName()
        + " annotation may only be used in generated code and its use"
        + " by third parties is not supported.";
  }

  static String unknownField(String field) {
    return "Unknown field " + field;
  }

  static String unknownProblem(String className, ProblemReferenceBinding problem) {
    return "Unknown problem: "
        + ProblemReferenceBinding.problemReasonString(problem.problemId())
        + " " + className;
  }

  private final boolean allowArtificialRescue;

  private boolean collectTypes;

  private boolean reportErrors;

  private final CompilationUnitDeclaration cud;

  private List<String> referencedTypes;

  private ArtificialRescueChecker(CompilationUnitDeclaration cud) {
    allowArtificialRescue = true;
    this.cud = cud;
  }

  private ArtificialRescueChecker(CompilationUnitDeclaration cud,
      boolean allowArtificialRescue) {
    this.cud = cud;
    this.allowArtificialRescue = allowArtificialRescue;
  }

  private void check() {
    collectTypes = false;
    reportErrors = true;
    cud.traverse(new Visitor(), cud.scope);
  }

  private List<String> collect() {
    collectTypes = true;
    referencedTypes = Lists.create();
    reportErrors = false;
    cud.traverse(new Visitor(), cud.scope);
    return referencedTypes;
  }

}
