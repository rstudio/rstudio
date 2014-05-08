/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.cfg.DepsInfoProvider;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Java AST visitor that verifies that "Type A"->"Type B" references are enabled by matching "Type
 * A Module"->"Type B Module" inherits.
 */
public class TypeRefDepsChecker extends JVisitor {

  /**
   * A Tuple of 'from' and 'to' type source names.
   */
  private static class TypeRef {
    private String fromTypeSourceName;
    private String toTypeSourceName;

    public TypeRef(String fromTypeSourceName, String toTypeSourceName) {
      this.fromTypeSourceName = fromTypeSourceName;
      this.toTypeSourceName = toTypeSourceName;
    }

    @Override
    public boolean equals(Object object) {
      if (object instanceof TypeRef) {
        TypeRef that = (TypeRef) object;
        return Objects.equal(this.fromTypeSourceName, that.fromTypeSourceName)
            && Objects.equal(this.toTypeSourceName, that.toTypeSourceName);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(fromTypeSourceName, toTypeSourceName);
    }
  }

  public static void exec(TreeLogger logger, JProgram program, DepsInfoProvider depsInfoProvider,
      boolean warnMissingDeps, File missingDepsFile) {
    if (!warnMissingDeps && missingDepsFile == null) {
      return;
    }

    new TypeRefDepsChecker(logger, program, depsInfoProvider, warnMissingDeps, missingDepsFile)
        .execImpl();
  }

  private static JDeclaredType getOuterMostType(JDeclaredType type) {
    while (type.getEnclosingType() != null) {
      type = type.getEnclosingType();
    }
    return type;
  }

  private final DepsInfoProvider depsInfoProvider;
  private String fromTypeSourceName;
  private final TreeLogger logger;
  private final File missingDepsFile;
  private final Function<String, String> moduleNameToModuleFile = new Function<String, String>() {
    @Override
    public String apply(String moduleName) {
      return depsInfoProvider.getGwtXmlFilePath(moduleName);
    }
  };
  private final JProgram program;
  private final Set<TypeRef> recordedTypeRefs = new HashSet<TypeRef>();
  private final boolean warnMissingDeps;

  public TypeRefDepsChecker(TreeLogger logger, JProgram program, DepsInfoProvider depsInfoProvider,
      boolean warnMissingDeps, File missingDepsFile) {
    this.logger = logger;
    this.program = program;
    this.depsInfoProvider = depsInfoProvider;
    this.warnMissingDeps = warnMissingDeps;
    this.missingDepsFile = missingDepsFile;
  }

  @Override
  public void endVisit(JCastOperation x, Context ctx) {
    // Gather (Foo) casts.
    maybeRecordTypeRef(x.getCastType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JClassLiteral x, Context ctx) {
    // Gather Foo.class literal references.
    maybeRecordTypeRef(x.getRefType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JFieldRef x, Context ctx) {
    // Gather Foo.someField static references.
    processJFieldRef(x);
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JInstanceOf x, Context ctx) {
    // Gather instanceof Foo references.
    maybeRecordTypeRef(x.getTestType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JMethod x, Context ctx) {
    // Gather return types of method definitions.
    maybeRecordTypeRef(x.getType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JMethodCall x, Context ctx) {
    // Gather Foo.doSomething() static method calls.
    processMethodCall(x);
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JsniFieldRef x, Context ctx) {
    // Gather Foo.someField static references.
    processJFieldRef(x);
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JsniMethodRef x, Context ctx) {
    // Gather Foo.doSomething() static method calls.
    processMethodCall(x);
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JValueLiteral x, Context ctx) {
    // Gather types whose constructor function is effectively called in value literal definitions.
    maybeRecordTypeRef(x.getType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JVariable x, Context ctx) {
    // Gather declared types of local variables, class fields and method parameters.
    maybeRecordTypeRef(x.getType());
    super.endVisit(x, ctx);
  }

  @Override
  public boolean visit(JDeclaredType x, Context ctx) {
    fromTypeSourceName = getOuterMostType(x).getName();

    // Gather superclass and implemented interface types.
    maybeRecordTypeRef(x.getSuperClass());
    maybeRecordTypeRefs(x.getImplements());
    return super.visit(x, ctx);
  }

  @VisibleForTesting
  boolean maybeRecordTypeRef(String fromTypeSourceName, String toTypeSourceName) {
    return recordedTypeRefs.add(new TypeRef(fromTypeSourceName, toTypeSourceName));
  }

  /**
   * Verifies that no Type->Type references exceed the bounds of Module->Module dependencies.
   * <p>
   * In actuality both the "from" and "to" Types can be provided by multiple modules. Each "from"
   * module is considered valid if its set of transitive deps modules contains at least one of the
   * multiple "to" modules.
   */
  @VisibleForTesting
  void verifyTypeRefsInModules() {
    Set<String> examinedModuleRefs = new HashSet<String>();

    PrintStream missingDepsStream = null;
    if (missingDepsFile != null) {
      try {
        missingDepsStream = new PrintStream(missingDepsFile);
      } catch (FileNotFoundException e) {
        logger.log(TreeLogger.WARN, "Failed to open missing deps file " + missingDepsFile);
      }
    }

    for (TypeRef recordedTypeRef : recordedTypeRefs) {
      // Figure out where each type came from.
      Collection<String> fromModuleNames =
          depsInfoProvider.getSourceModuleNames(recordedTypeRef.fromTypeSourceName);
      List<String> toModuleNames = Lists.newArrayList(
          depsInfoProvider.getSourceModuleNames(recordedTypeRef.toTypeSourceName));
      Collections.sort(toModuleNames);

      // Types created by Generators do not currently have a known source module.
      if (fromModuleNames.isEmpty() || toModuleNames.isEmpty()) {
        continue;
      }

      for (String fromModuleName : fromModuleNames) {
        // Only examine each unique module dep one time.
        String moduleRef = fromModuleName + ":" + toModuleNames;
        if (examinedModuleRefs.contains(moduleRef)) {
          continue;
        }
        examinedModuleRefs.add(moduleRef);
        // If both files are from the same module then the reference is obviously legal.
        if (toModuleNames.contains(fromModuleName)) {
          continue;
        }
        // If the toType is supplied by at least one module that is in the set of transitive dep
        // modules of the module that provided the fromType then the reference is legal.
        if (!Collections.disjoint(depsInfoProvider.getTransitiveDepModuleNames(fromModuleName),
            toModuleNames)) {
          continue;
        }

        if (warnMissingDeps) {
          logger.log(TreeLogger.WARN, String.format(
              "Type '%s' wants to reference type '%s' but can't because module '%s' "
              + "has no dependency (neither direct nor transitive) on '%s'.",
              recordedTypeRef.fromTypeSourceName, recordedTypeRef.toTypeSourceName, fromModuleName,
              Joiner.on("|").join(toModuleNames)));
        }
        if (missingDepsStream != null) {
          missingDepsStream.printf("%s\t%s\t%s\t%s\t%s\n", fromModuleName,
              depsInfoProvider.getGwtXmlFilePath(fromModuleName),
              Joiner.on("|").join(toModuleNames),
              Joiner.on("|").join(Collections2.transform(toModuleNames, moduleNameToModuleFile)),
                  "Type '" + recordedTypeRef.fromTypeSourceName + "' wants to reference type '"
                  + recordedTypeRef.toTypeSourceName + "'.");
        }
      }
    }

    if (missingDepsStream != null) {
      missingDepsStream.close();
    }
  }

  private void execImpl() {
    accept(program);
    verifyTypeRefsInModules();
  }

  private void maybeRecordTypeRef(JType toPossiblyNestedType) {
    if (toPossiblyNestedType instanceof JDeclaredType) {
      JDeclaredType toType = getOuterMostType((JDeclaredType) toPossiblyNestedType);
      maybeRecordTypeRef(fromTypeSourceName, toType.getName());
    }
  }

  private void maybeRecordTypeRefs(List<? extends JDeclaredType> toTypes) {
    for (JDeclaredType toType : toTypes) {
      maybeRecordTypeRef(toType);
    }
  }

  private void processJFieldRef(JFieldRef x) {
    if (x.getTarget() instanceof JField) {
      JField field = (JField) x.getTarget();
      if (field.isStatic()) {
        maybeRecordTypeRef(field.getEnclosingType());
      }
    }
  }

  private void processMethodCall(JMethodCall x) {
    if (x.getTarget().isStatic()) {
      maybeRecordTypeRef(x.getTarget().getEnclosingType());
    }
  }
}
