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
import com.google.gwt.dev.jdt.SafeASTVisitor;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.collect.Lists;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.jdt.internal.compiler.impl.StringConstant;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.ElementValuePair;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks the validity of ArtificialRescue annotations.
 *
 * <ul>
 * <li>(1) The ArtificialRescue annotation is only used in generated code.</li>
 * <li>(2) The className value names a type known to GWT.</li>
 * <li>(3) The methods and fields of the type are known to GWT.</li>
 * </ul>
 */
public abstract class ArtificialRescueChecker {

  /**
   * Represents a single
   * {@link com.google.gwt.core.client.impl.ArtificialRescue.Rescue}.
   * <p>
   * Only public so it can be used by
   * {@link com.google.gwt.dev.jjs.impl.GenerateJavaAST}.
   */
  public static class RescueData {
    private static final RescueData[] EMPTY_RESCUEDATA = new RescueData[0];

    public static RescueData[] createFromAnnotations(Annotation[] annotations) {
      RescueData[] result = EMPTY_RESCUEDATA;
      for (Annotation a : annotations) {
        ReferenceBinding binding = (ReferenceBinding) a.resolvedType;
        String name = CharOperation.toString(binding.compoundName);
        if (!name.equals(ArtificialRescue.class.getName())) {
          continue;
        }
        return createFromArtificialRescue(a);
      }
      return result;
    }

    public static RescueData[] createFromArtificialRescue(Annotation artificialRescue) {
      Object[] values = null;
      RescueData[] result = EMPTY_RESCUEDATA;
      for (ElementValuePair pair : artificialRescue.computeElementValuePairs()) {
        if ("value".equals(String.valueOf(pair.getName()))) {
          Object value = pair.getValue();
          if (value instanceof AnnotationBinding) {
            values = new Object[]{value};
          } else {
            values = (Object[]) value;
          }
          break;
        }
      }
      assert values != null;
      if (values.length > 0) {
        result = new RescueData[values.length];
        for (int i = 0; i < result.length; ++i) {
          result[i] = createFromRescue((AnnotationBinding) values[i]);
        }
      }
      return result;
    }

    private static RescueData createFromRescue(AnnotationBinding rescue) {
      String className = null;
      boolean instantiable = false;
      String[] methods = Empty.STRINGS;
      String[] fields = Empty.STRINGS;
      for (ElementValuePair pair : rescue.getElementValuePairs()) {
        String name = String.valueOf(pair.getName());
        if ("className".equals(name)) {
          className = ((StringConstant) pair.getValue()).stringValue();
        } else if ("instantiable".equals(name)) {
          BooleanConstant value = (BooleanConstant) pair.getValue();
          instantiable = value.booleanValue();
        } else if ("methods".equals(name)) {
          methods = getValueAsStringArray(pair.getValue());
        } else if ("fields".equals(name)) {
          fields = getValueAsStringArray(pair.getValue());
        } else {
          assert false : "Unknown ArtificialRescue field";
        }
      }
      assert className != null;
      return new RescueData(className, instantiable, fields, methods);
    }

    private static String[] getValueAsStringArray(Object value) {
      if (value instanceof StringConstant) {
        return new String[]{((StringConstant) value).stringValue()};
      }
      Object[] values = (Object[]) value;
      String[] toReturn = new String[values.length];
      for (int i = 0; i < toReturn.length; ++i) {
        toReturn[i] = ((StringConstant) values[i]).stringValue();
      }
      return toReturn;
    }

    private final String className;
    private final String[] fields;
    private final boolean instantiable;
    private final String[] methods;

    RescueData(String className, boolean instantiable, String[] fields, String[] methods) {
      this.className = className;
      this.instantiable = instantiable;
      this.methods = methods;
      this.fields = fields;
    }

    public String getClassName() {
      return className;
    }

    public String[] getFields() {
      return fields;
    }

    public String[] getMethods() {
      return methods;
    }

    public boolean isInstantiable() {
      return instantiable;
    }
  }

  /**
   * Checks that references are legal and resolve to a known element. Collects
   * the rescued elements per type for later user.
   */
  private static class Checker extends ArtificialRescueChecker {
    private final Map<TypeDeclaration, Binding[]> artificialRescues;
    private transient List<Binding> currentBindings = new ArrayList<Binding>();

    public Checker(CompilationUnitDeclaration cud, Map<TypeDeclaration, Binding[]> artificialRescues) {
      super(cud);
      this.artificialRescues = artificialRescues;
    }

    @Override
    protected void processRescue(RescueData rescue) {
      String className = rescue.getClassName();
      // Strip off any array-like extensions and just find base type
      int arrayDims = 0;
      while (className.endsWith("[]")) {
        className = className.substring(0, className.length() - 2);
        ++arrayDims;
      }

      // Goal (2) The className value names a type known to GWT.
      char[][] compoundName = CharOperation.splitOn('.', className.toCharArray());
      TypeBinding typeBinding = cud.scope.getType(compoundName, compoundName.length);
      if (typeBinding == null) {
        error(notFound(className));
        return;
      }
      if (typeBinding instanceof ProblemReferenceBinding) {
        ProblemReferenceBinding problem = (ProblemReferenceBinding) typeBinding;
        if (problem.problemId() == ProblemReasons.NotVisible) {
          // Ignore
        } else if (problem.problemId() == ProblemReasons.NotFound) {
          error(notFound(className));
        } else {
          error(unknownProblem(className, problem));
        }
        return;
      }
      if (arrayDims > 0) {
        typeBinding = cud.scope.createArrayType(typeBinding, arrayDims);
      }
      if (rescue.isInstantiable()) {
        currentBindings.add(typeBinding);
      }
      if (typeBinding instanceof BaseTypeBinding || arrayDims > 0) {
        // No methods or fields on primitive types or array types (3)
        if (rescue.getMethods().length > 0) {
          error(noMethodsAllowed());
        }

        if (rescue.getFields().length > 0) {
          error(noFieldsAllowed());
        }
        return;
      }

      // Goal (3) The methods and fields of the type are known to GWT.
      ReferenceBinding ref = (ReferenceBinding) typeBinding;
      for (String field : rescue.getFields()) {
        FieldBinding fieldBinding = ref.getField(field.toCharArray(), false);
        if (fieldBinding == null) {
          error(unknownField(field));
        } else {
          currentBindings.add(fieldBinding);
        }
      }
      for (String method : rescue.getMethods()) {
        if (method.contains("@")) {
          error(nameAndTypesOnly());
          continue;
        }
        // Method signatures use the same format as JSNI method refs.
        JsniRef jsni = JsniRef.parse("@foo::" + method);
        if (jsni == null) {
          error(badMethodSignature(method));
          continue;
        }

        MethodBinding[] methodBindings;
        if (jsni.memberName().equals(String.valueOf(ref.compoundName[ref.compoundName.length - 1]))) {
          // Constructor
          methodBindings = ref.getMethods("<init>".toCharArray());
        } else {
          methodBindings = ref.getMethods(jsni.memberName().toCharArray());
        }
        boolean found = false;
        for (MethodBinding methodBinding : methodBindings) {
          if (jsni.matchesAnyOverload() || jsni.paramTypesString().equals(sig(methodBinding))) {
            currentBindings.add(methodBinding);
            found = true;
          }
        }
        if (!found) {
          error(noMethod(className, jsni.memberSignature()));
          continue;
        }
      }
    }

    @Override
    protected void processType(TypeDeclaration x) {
      super.processType(x);
      if (currentBindings.size() > 0) {
        Binding[] result = currentBindings.toArray(new Binding[currentBindings.size()]);
        artificialRescues.put(x, result);
        currentBindings = new ArrayList<Binding>();
      }
    }

    private String sig(MethodBinding methodBinding) {
      StringBuilder sb = new StringBuilder();
      for (TypeBinding paramType : methodBinding.parameters) {
        sb.append(paramType.signature());
      }
      return sb.toString();
    }
  }

  /**
   * Collects only the names of the rescued types; does not report errors.
   */
  private static class Collector extends ArtificialRescueChecker {
    private final List<String> referencedTypes;

    public Collector(CompilationUnitDeclaration cud, List<String> referencedTypes) {
      super(cud);
      this.referencedTypes = referencedTypes;
    }

    @Override
    protected void processRescue(RescueData rescue) {
      String className = rescue.getClassName();
      while (className.endsWith("[]")) {
        className = className.substring(0, className.length() - 2);
      }
      referencedTypes.add(className);
    }
  }

  /**
   * Records an error if artificial rescues are used at all.
   */
  private static class Disallowed extends ArtificialRescueChecker {
    public Disallowed(CompilationUnitDeclaration cud) {
      super(cud);
    }

    @Override
    protected void processRescue(RescueData rescue) {
      // Goal (1) ArtificialRescue annotation is only used in generated code.
      error(onlyGeneratedCode());
    }
  }

  private class Visitor extends SafeASTVisitor {
    @Override
    public void endVisit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
      processType(memberTypeDeclaration);
    }

    @Override
    public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
      processType(typeDeclaration);
    }

    @Override
    public void endVisitValid(TypeDeclaration localTypeDeclaration, BlockScope scope) {
      processType(localTypeDeclaration);
    }
  }

  /**
   * Check the {@link ArtificialRescue} annotations in a CompilationUnit. Errors
   * are reported through {@link GWTProblem}.
   */
  public static void check(CompilationUnitDeclaration cud, boolean allowArtificialRescue,
      Map<TypeDeclaration, Binding[]> artificialRescues) {
    if (allowArtificialRescue) {
      new Checker(cud, artificialRescues).exec();
    } else {
      new Disallowed(cud).exec();
    }
  }

  /**
   * Report all types named in {@link ArtificialRescue} annotations in a CUD. No
   * error checking is done.
   */
  public static List<String> collectReferencedTypes(CompilationUnitDeclaration cud) {
    ArrayList<String> result = new ArrayList<String>();
    new Collector(cud, result).exec();
    return Lists.normalize(result);
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

  static String noMethod(String className, String methodSig) {
    return "No method " + methodSig + " in type " + className;
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
    return "Unknown problem: " + ProblemReferenceBinding.problemReasonString(problem.problemId())
        + " " + className;
  }

  protected final CompilationUnitDeclaration cud;

  private Annotation errorNode;

  private ArtificialRescueChecker(CompilationUnitDeclaration cud) {
    this.cud = cud;
  }

  protected final void error(String msg) {
    GWTProblem.recordError(errorNode, cud, msg, null);
  }

  protected final void exec() {
    cud.traverse(new Visitor(), cud.scope);
  }

  protected abstract void processRescue(RescueData rescue);

  /**
   * Examine a TypeDeclaration for ArtificialRescue annotations.
   */
  protected void processType(TypeDeclaration x) {
    if (x.annotations == null) {
      return;
    }
    for (Annotation a : x.annotations) {
      ReferenceBinding binding = (ReferenceBinding) a.resolvedType;
      String name = CharOperation.toString(binding.compoundName);
      if (!name.equals(ArtificialRescue.class.getName())) {
        continue;
      }
      errorNode = a;
      RescueData[] rescues = RescueData.createFromArtificialRescue(a);
      for (RescueData rescue : rescues) {
        processRescue(rescue);
      }
      break;
    }
  }
}
