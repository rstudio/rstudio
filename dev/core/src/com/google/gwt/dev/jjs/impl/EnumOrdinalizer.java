/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JEnumField;
import com.google.gwt.dev.jjs.ast.JEnumType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JNonNullType;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This optimizer replaces enum constants with their ordinal value (a simple
 * int) when possible. We call this process "ordinalization".
 * 
 * Generally, this can be done for enums that are only ever referred to by
 * reference, or by their ordinal value. For the specific set of conditions
 * under which ordinalization can proceed, see the notes for the nested class
 * {@link EnumOrdinalizer.CannotBeOrdinalAnalyzer} below.
 * 
 * This optimizer modifies enum classes to change their field constants to ints,
 * and to remove initialization of those constants in the clinit method. An
 * ordinalized enum class will not be removed from the AST by this optimizer,
 * but as long as all references to it are replaced, then the enum class itself
 * will be pruned by subsequent optimizer passes. Some enum classes may not be
 * completely removed however. Ordinalization can proceed in cases where there
 * are added static fields or methods in the enum class. In such cases a reduced
 * version of the original enum class can remain in the AST, containing only
 * static fields and methods which aren't part of the enum infrastructure (in
 * which case it will no longer behave as an enum class at all).
 * 
 * Regardless of whether an ordinalized enum class ends up being completely
 * pruned away, the AST is expected to be in a coherent and usable state after
 * any pass of this optimizer. Thus, this optimizer should be considered to be
 * stateless.
 * 
 * The process is broken up into 3 separate passes over the AST, each
 * implemented as a separate visitor class. The first visitor,
 * {@link EnumOrdinalizer.CannotBeOrdinalAnalyzer} compiles information about
 * each enum class in the AST, and looks for reasons not to ordinalize each
 * enum. Thus, it prepares a "black-list" of enum classes that cannot be
 * ordinalized (and it follows that all enums that don't get entered in the
 * black-list, will be allowed to be ordinalized. The set of conditions which
 * cause an enum to be black-listed are outlined below.
 * 
 * If there are enum classes that didn't get black-listed remaining, the
 * subsequent passes of the optimizer will be invoked. The first,
 * {@link EnumOrdinalizer.ReplaceEnumTypesWithInteger}, replaces the type info
 * for each field or expression involved with a target enum constant with an
 * integer. The final visitor, descriptively named
 * {@link EnumOrdinalizer.ReplaceOrdinalFieldAndMethodRefsWithOrdinal}, will do
 * the job of replacing the value for references to the Enum ordinal field or
 * method of an enum constant that has been ordinalized.
 */
public class EnumOrdinalizer {
  /**
   * A simple Tracker class for compiling lists of enum classes processed by
   * this optimizer. If enabled, the results can be logged as debug output, and
   * the results can be tested after running with a given input.
   */
  public static class Tracker {
    private final Set<String> allEnumsOrdinalized;
    private final Set<String> allEnumsVisited;
    private final Map<String, List<SourceInfo>> enumInfoMap;
    private final List<Set<String>> enumsOrdinalizedPerPass;
    private final List<Set<String>> enumsVisitedPerPass;
    private int runCount = -1;

    // use TreeSets, for nice sorted iteration for output
    public Tracker() {
      allEnumsVisited = new TreeSet<String>();
      allEnumsOrdinalized = new TreeSet<String>();
      enumsVisitedPerPass = new ArrayList<Set<String>>();
      enumsOrdinalizedPerPass = new ArrayList<Set<String>>();
      enumInfoMap = new HashMap<String, List<SourceInfo>>();

      // add entry for initial pass
      enumsVisitedPerPass.add(new TreeSet<String>());
      enumsOrdinalizedPerPass.add(new TreeSet<String>());
    }

    public void addEnumNotOrdinalizedInfo(String enumName, SourceInfo info) {
      List<SourceInfo> infos = enumInfoMap.get(enumName);
      if (infos == null) {
        infos = new ArrayList<SourceInfo>();
        enumInfoMap.put(enumName, infos);
      }
      if (!infos.contains(info)) {
        infos.add(info);
      }
    }

    public void addOrdinalized(String ordinalized) {
      enumsOrdinalizedPerPass.get(runCount).add(ordinalized);
      allEnumsOrdinalized.add(ordinalized);
    }

    public void addVisited(String visited) {
      enumsVisitedPerPass.get(runCount).add(visited);
      allEnumsVisited.add(visited);
    }

    public String getInfoString(SourceInfo info) {
      if (info != null) {
        return info.getFileName() + ": Line " + info.getStartLine();
      }
      return null;
    }

    public int getNumOrdinalized() {
      return allEnumsOrdinalized.size();
    }

    public int getNumVisited() {
      return allEnumsVisited.size();
    }

    public void incrementRunCount() {
      runCount++;
      enumsVisitedPerPass.add(new TreeSet<String>());
      enumsOrdinalizedPerPass.add(new TreeSet<String>());
    }

    public boolean isOrdinalized(String className) {
      return allEnumsOrdinalized.contains(className);
    }

    public boolean isVisited(String className) {
      return allEnumsVisited.contains(className);
    }

    public void logEnumsNotOrdinalized(TreeLogger logger, TreeLogger.Type logType) {
      if (logger != null) {
        boolean initialMessageLogged = false;
        for (String enumVisited : allEnumsVisited) {
          if (!isOrdinalized(enumVisited)) {
            if (!initialMessageLogged) {
              logger = logger.branch(logType, "Enums Not Ordinalized:");
              initialMessageLogged = true;
            }
            TreeLogger subLogger = logger.branch(logType, enumVisited);
            List<SourceInfo> infos = enumInfoMap.get(enumVisited);
            if (infos == null) {
              continue;
            }

            Collections.sort(infos, new Comparator<SourceInfo>() {
              public int compare(SourceInfo s1, SourceInfo s2) {
                int fileNameComp = s1.getFileName().compareTo(s2.getFileName());
                if (fileNameComp != 0) {
                  return fileNameComp;
                }
                if (s1.getStartLine() < s2.getStartLine()) {
                  return -1;
                } else if (s1.getStartLine() > s2.getStartLine()) {
                  return 1;
                }
                return 0;
              }
            });

            for (SourceInfo info : infos) {
              subLogger.branch(logType, getInfoString(info));
            }
          }
        }
      }
    }

    public void logEnumsOrdinalized(TreeLogger logger, TreeLogger.Type logType) {
      if (logger != null && allEnumsOrdinalized.size() > 0) {
        logger = logger.branch(logType, "Enums Ordinalized:");
        for (String enumOrdinalized : allEnumsOrdinalized) {
          logger.branch(logType, enumOrdinalized);
        }
      }
    }

    public void logEnumsOrdinalizedPerPass(TreeLogger logger, TreeLogger.Type logType) {
      if (logger != null) {
        if (allEnumsOrdinalized.size() == 0) {
          return;
        }
        int pass = 0;
        for (Set<String> enumsOrdinalized : enumsOrdinalizedPerPass) {
          pass++;
          if (enumsOrdinalized.size() > 0) {
            TreeLogger subLogger =
                logger.branch(logType, "Pass " + pass + ": " + enumsOrdinalized.size()
                    + " ordinalized");
            for (String enumOrdinalized : enumsOrdinalized) {
              subLogger.branch(logType, enumOrdinalized);
            }
          }
        }
      }
    }

    public void logEnumsVisitedPerPass(TreeLogger logger, TreeLogger.Type logType) {
      if (logger != null) {
        if (allEnumsVisited.size() == 0) {
          return;
        }
        int pass = 0;
        for (Set<String> enumsVisited : enumsVisitedPerPass) {
          pass++;
          if (enumsVisited.size() > 0) {
            TreeLogger subLogger =
                logger.branch(logType, "Pass " + pass + ": " + enumsVisited.size() + " visited");
            for (String enumVisited : enumsVisited) {
              subLogger.branch(logType, enumVisited);
            }
          }
        }
      }
    }

    public void logResults(TreeLogger logger, TreeLogger.Type logType) {
      logger = logResultsSummary(logger, logType);
      logEnumsOrdinalized(logger, logType);
      logEnumsNotOrdinalized(logger, logType);
    }

    public void logResultsDetailed(TreeLogger logger, TreeLogger.Type logType) {
      logger = logResultsSummary(logger, logType);
      logEnumsOrdinalizedPerPass(logger, logType);
      // logEnumsVisitedPerPass(logger, logType);
      logEnumsNotOrdinalized(logger, logType);
    }

    public TreeLogger logResultsSummary(TreeLogger logger, TreeLogger.Type logType) {
      if (logger != null) {
        logger = logger.branch(logType, "EnumOrdinalizer Results:");
        logger.branch(logType, (runCount + 1) + " ordinalization passes completed");
        logger.branch(logType, allEnumsOrdinalized.size() + " of " + allEnumsVisited.size()
            + " ordinalized");
        return logger;
      }
      return null;
    }

    public void maybeDumpAST(JProgram program, int stage) {
      AstDumper.maybeDumpAST(program, NAME + "_" + (runCount + 1) + "_" + stage);
    }
  }

  /**
   * A visitor which keeps track of the enums which cannot be ordinalized. It
   * does this by keeping track of a "black-list" for ordinals which violate the
   * conditions for ordinalization, below.
   * 
   * An enum cannot be ordinalized, if it:
   * <ul>
   * <li>is implicitly upcast.</li>
   * <li>is implicitly cast to from a nullType.</li>
   * <li>is implicitly cast to or from a javaScriptObject type.</li>
   * <li>is explicitly cast to another type (or vice-versa).</li>
   * <li>is tested in an instanceof expression.</li>
   * <li>it's class literal is used explicitly.</li>
   * <li>it has an artificial rescue recorded for it.</li>
   * <li>has any field referenced, except for:</li>
   * <ul>
   * <li>static fields, other than the synthetic $VALUES field.</li>
   * <li>Enum.ordinal.</li>
   * </ul>
   * <li>has any method called, except for:</li>
   * <ul>
   * <li>ordinal().</li>
   * <li>Enum.ordinal().</li>
   * <li>Enum() super constructor.</li>
   * <li>Enum.createValueOfMap().</li>
   * <li>static methods, other than values() or valueOf().</li>
   * </ul>
   * </ul>
   * 
   * This visitor extends the ImplicitUpcastAnalyzer, which encapsulates all the
   * conditions where implicit upcasting can occur in an AST. The rest of the
   * logic for checking ordinalizability is encapsulated in this sub-class.
   * 
   * It also keeps track of all enums encountered, so we can know if we need to
   * continue with the other visitors of the optimizer after this visitor runs.
   * 
   * We make special allowances not to check any code statements that appear
   * within the ClassLiteralHolder class, which can contain a reference to all
   * enum class literals in the program, even after ordinalization occurs.
   * 
   * Also, we ignore visiting the getClass() method for any enum subclass, since
   * it will also cause a visit to the enum's class literal, and we don't
   * necessarily want to prevent ordinalization in that case.
   * 
   * Special checking is needed to detect a class literal reference that occurs
   * within a JSNI method body. We don't get a visit to JClassLiteral in that
   * case, so we need to inspect visits to JsniFieldRef for the possibility it
   * might be a reference to a class literal.
   * 
   * We also skip any checking in a method call to Enum.createValueOfMap(),
   * since this is generated for any enum class initially within the extra
   * enumClass$Map class, and this call contains an implicit upcast in the
   * method call args (as well as a reference to the static enumClass$VALUES
   * field), which we want to ignore. The enumClass$Map class will not get
   * pruned as long as the enumClass is not ordinalized, and so we need to
   * ignore it's presence in the consideration for whether an enum class is
   * ordinalizable.
   */
  private class CannotBeOrdinalAnalyzer extends ImplicitUpcastAnalyzer {

    private final Map<String, SourceInfo> jsniClassLiteralsInfo = new HashMap<String, SourceInfo>();

    public CannotBeOrdinalAnalyzer(JProgram program) {
      super(program);
    }

    /*
     * After program is visited, post-process remaining tasks from accumulated
     * data.
     */
    public void afterVisitor() {
      // black-list any Jsni enum ClassLiteralsVisited
      for (String classLiteralName : jsniClassLiteralsInfo.keySet()) {
        JEnumType enumFromLiteral = enumsVisited.get(classLiteralName);
        if (enumFromLiteral != null) {
          addToBlackList(enumFromLiteral, jsniClassLiteralsInfo.get(classLiteralName));
        }
      }
    }

    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      // check for explicit cast (check both directions)
      blackListIfEnumCast(x.getExpr().getType(), x.getCastType(), x.getSourceInfo());
      blackListIfEnumCast(x.getCastType(), x.getExpr().getType(), x.getSourceInfo());
    }

    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      /*
       * Check for references to an enum's class literal. We need to black-list
       * classes in this case, since there could be a call to
       * Enum.valueOf(someEnum.class,"name"), etc.
       * 
       * Note: we won't get here for class literals that occur in the
       * ClassLiteralHolder class, or within the getClass method of an enum
       * class (see comments above).
       */
      JEnumType type = getEnumType(x.getRefType());
      if (type != null) {
        blackListIfEnum(type, x.getSourceInfo());
      }
    }

    @Override
    public void endVisit(JClassType x, Context ctx) {
      // black-list any artificially rescued classes recorded for this class
      List<JNode> rescues = x.getArtificialRescues();
      if (rescues != null && rescues.size() > 0) {
        for (JNode rescueNode : rescues) {
          if (rescueNode instanceof JType) {
            blackListIfEnum((JType) rescueNode, x.getSourceInfo());
          }
        }
      }

      // keep track of all enum classes visited
      JEnumType maybeEnum = x.isEnumOrSubclass();
      if (maybeEnum != null) {
        enumsVisited.put(program.getClassLiteralName(maybeEnum), maybeEnum);

        // don't need to re-ordinalize a previously ordinalized enum
        if (maybeEnum.isOrdinalized()) {
          addToBlackList(maybeEnum, x.getSourceInfo());
        }
      }
    }

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      // don't need to check Enum.ordinal
      if (x.getField() == enumOrdinalField) {
        return;
      }

      if (x.getInstance() != null) {
        // check any instance field reference other than ordinal
        blackListIfEnumExpression(x.getInstance());
      } else if (x.getField().isStatic()) {
        /*
         * Black list if the $VALUES static field is referenced, unless it's
         * within the auto-generated clinit or values method, within the enum
         * class itself.
         * 
         * TODO (jbrosenberg): Investigate further whether referencing the
         * $VALUES array (as well as the values() method) should not block
         * ordinalization. Instead, convert $VALUES to an array of int.
         */
        if (x.getField().getName().equals("$VALUES")
            && ((this.currentMethod.getEnclosingType() != x.getField().getEnclosingType()) ||
                (!this.currentMethod.getName().equals("values") &&
                 !this.currentMethod.getName().equals("$clinit")))) {
          blackListIfEnum(x.getField().getEnclosingType(), x.getSourceInfo());
        }
      }
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      // If any instanceof tests haven't been optimized out, black list.
      blackListIfEnum(x.getExpr().getType(), x.getSourceInfo());
      // TODO (jbrosenberg): Investigate further whether ordinalization can be
      // allowed in this case.
      blackListIfEnum(x.getTestType(), x.getSourceInfo());
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // exempt calls to certain methods on the Enum super class
      if (x.getTarget() == enumCreateValueOfMapMethod || x.getTarget() == enumSuperConstructor
          || x.getTarget() == enumOrdinalMethod) {
        return;
      }

      // any other method on an enum class should cause it to be black-listed
      if (x.getInstance() != null) {
        blackListIfEnumExpression(x.getInstance());
      } else if (x.getTarget().isStatic()) {
        // black-list static method calls on an enum class only for valueOf()
        // and values()
        String methodName = x.getTarget().getName();
        if (methodName.equals("valueOf") || methodName.equals("values")) {
          blackListIfEnum(x.getTarget().getEnclosingType(), x.getSourceInfo());
        }
      }

      // defer to ImplicitUpcastAnalyzer to check method call args & params
      super.endVisit(x, ctx);
    }

    @Override
    public void endVisit(JsniFieldRef x, Context ctx) {
      /*
       * Can't do the same thing as for JFieldRef, all JsniFieldRefs are cast to
       * JavaScriptObjects. Need to check both the field type and the type of
       * the instance or enclosing class referencing the field.
       */

      // check the field type
      blackListIfEnum(x.getField().getType(), x.getSourceInfo());

      // check the referrer
      if (x.getInstance() != null) {
        blackListIfEnumExpression(x.getInstance());
      } else {
        blackListIfEnum(x.getField().getEnclosingType(), x.getSourceInfo());
      }

      /*
       * need to also check JsniFieldRef's for a possible reference to a class
       * literal, since we don't get a visit to JClassLiteral when it occurs
       * within Jsni (shouldn't it?).
       */
      if (x.getField().getEnclosingType() == classLiteralHolderType) {
        // see if it has an initializer with a method call to "createForEnum"
        JExpression initializer = x.getField().getInitializer();
        if (initializer instanceof JMethodCall) {
          if (((JMethodCall) initializer).getTarget() == classCreateForEnumMethod) {
            jsniClassLiteralsInfo.put(x.getField().getName(), x.getSourceInfo());
          }
        }
      }
    }

    @Override
    public void endVisit(JsniMethodRef x, Context ctx) {
      // no enum methods are exempted if occur within a JsniMethodRef
      if (x.getInstance() != null) {
        blackListIfEnumExpression(x.getInstance());
      } else if (x.getTarget().isStatic()) {
        /*
         * need to exempt static methodCalls for an enum class if it occurs
         * within the enum class itself (such as in $clinit() or values())
         */
        if (this.currentMethod.getEnclosingType() != x.getTarget().getEnclosingType()) {
          blackListIfEnum(x.getTarget().getEnclosingType(), x.getSourceInfo());
        }
      }

      // defer to ImplicitUpcastAnalyzer to check method call args & params
      super.endVisit(x, ctx);
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      /*
       * Don't want to visit the large ClassLiteralHolder class, it doesn't
       * contain references to actual usage of enum class literals. It's also a
       * time savings to not traverse this class.
       */
      if (x == classLiteralHolderType) {
        return false;
      }
      return true;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      /*
       * Don't want to visit the generated getClass() method on an enum, since
       * it contains a reference to an enum's class literal that we don't want
       * to consider. Make sure this is not a user overloaded version (i.e.
       * check that it has no params).
       */
      if (getEnumType(x.getEnclosingType()) != null && x.getName().equals("getClass")
          && (x.getOriginalParamTypes() == null || x.getOriginalParamTypes().size() == 0)) {
        return false;
      }

      // defer to parent method on ImplicitCastAnalyzer
      return super.visit(x, ctx);
    }

    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      /*
       * skip all calls to Enum.createValueOfMap, since they'd get falsely
       * flagged for referencing $VALUES and for implicitly upcasting an array
       * of an enum class, in the arg passing. This method is only used by the
       * enumClass$Map class that gets generated for every enum class. Once
       * ordinalization proceeds, this $Map class should be pruned.
       */
      if (x.getTarget() == enumCreateValueOfMapMethod) {
        return false;
      }
      // ok to visit
      return true;
    }

    /*
     * Override for the method called from ImplicitUpcastAnalyzer, which will be
     * called for any implicit upcast.
     */
    @Override
    protected void processImplicitUpcast(JType fromType, JType destType, SourceInfo info) {
      if (fromType == nullType) {
        // handle case where a nullType is cast to an enum
        blackListIfEnum(destType, info);
      } else if (fromType == javaScriptObjectType) {
        // handle case where a javaScriptObject is cast to an enum
        blackListIfEnum(destType, info);
      } else {
        blackListIfEnumCast(fromType, destType, info);
      }
    }

    private void addToBlackList(JEnumType enumType, SourceInfo info) {
      ordinalizationBlackList.add(enumType);

      if (tracker != null) {
        tracker.addEnumNotOrdinalizedInfo(enumType.getName(), info);
      }
    }

    private void blackListIfEnum(JType maybeEnum, SourceInfo info) {
      JEnumType actualEnum = getEnumType(maybeEnum);
      if (actualEnum != null) {
        addToBlackList(actualEnum, info);
      }
    }

    private void blackListIfEnumCast(JType maybeEnum, JType destType, SourceInfo info) {
      JEnumType actualEnum = getEnumType(maybeEnum);
      JEnumType actualDestType = getEnumType(destType);
      if (actualEnum != null) {
        if (actualDestType != actualEnum) {
          addToBlackList(actualEnum, info);
        }
        return;
      }

      // check JArrayTypes of enums
      actualEnum = getEnumTypeFromArrayLeafType(maybeEnum);
      actualDestType = getEnumTypeFromArrayLeafType(destType);
      if (actualEnum != null) {
        if (actualDestType != actualEnum) {
          addToBlackList(actualEnum, info);
        }
      }
    }

    private void blackListIfEnumExpression(JExpression instance) {
      if (instance != null) {
        blackListIfEnum(instance.getType(), instance.getSourceInfo());
      }
    }
  }
  /**
   * A visitor which replaces enum types with an integer.
   * 
   * It sub-classes TypeRemapper, which encapsulates all the locations for a
   * settable type. The overridden remap() method will be called in each
   * instance, and it will test whether the type is a candidate for replacement,
   * and if so, return the new type to set (JPrimitiveType.INT).
   * 
   * Any reference to an enum field constant in an expression is replaced with
   * integer.
   * 
   * This will also explicitly replace an enum's field constants with its
   * ordinal int values, and remove initialization of enum constants and the
   * $VALUES array in the clinit method for the enum.
   */
  private class ReplaceEnumTypesWithInteger extends TypeRemapper {

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // don't waste time visiting the large ClassLiteralHolder class
      if (x == classLiteralHolderType) {
        return false;
      }

      if (canBeOrdinal(x)) {
        /*
         * Cleanup clinit method for ordinalizable enums. Note, method 0 is
         * always the clinit.
         */
        updateClinit(x.getMethods().get(0));

        /*
         * Remove any static impl mappings for any methods in an ordinal enum
         * class. An ordinalized enum will no longer have an instance passed as
         * the first argument for a static impl (it will just be an int). This
         * is needed to preserve proper assumptions about static impls by other
         * optimizers (e.g. we might need to insert a clinit, when it wouldn't
         * be needed if a method call still had a static impl target).
         */
        for (JMethod method : x.getMethods()) {
          program.removeStaticImplMapping(method);
        }
      }
      return true;
    }

    /**
     * Replace an enum field constant, with it's integer valued ordinal.
     */
    @Override
    public boolean visit(JField x, Context ctx) {
      if (x instanceof JEnumField && canBeOrdinal(x.getEnclosingType())) {
        int ordinal = ((JEnumField) x).ordinal();
        x.setInitializer(new JDeclarationStatement(x.getSourceInfo(), new JFieldRef(x
            .getSourceInfo(), null, x, x.getEnclosingType()), program.getLiteralInt(ordinal)));
      }
      return true;
    }

    /**
     * Replace an enum field ref with it's integer valued ordinal.
     */
    @Override
    public boolean visit(JFieldRef x, Context ctx) {
      JField field = x.getField();
      if (field instanceof JEnumField && canBeOrdinal(field.getEnclosingType())) {
        int ordinal = ((JEnumField) field).ordinal();
        ctx.replaceMe(program.getLiteralInt(ordinal));
      }
      return true;
    }

    /**
     * Remap enum types with JPrimitiveType.INT. Also handle case for arrays,
     * replace enum leaftype with JPrimitive.INT. This is an override
     * implementation called from TypeRemapper.
     */
    @Override
    protected JType remap(JType type) {
      JType remappedType = getOrdinalizedType(type);
      if (remappedType != null) {
        return remappedType;
      } else {
        return type;
      }
    }

    private boolean canBeOrdinal(JType type) {
      JType uType = getPossiblyUnderlyingType(type);
      return uType instanceof JEnumType && !ordinalizationBlackList.contains(uType);
    }

    private JType getOrdinalizedType(JType type) {
      if (canBeOrdinal(type)) {
        return JPrimitiveType.INT;
      }

      boolean nonNull = type instanceof JNonNullType;
      JType uType = nonNull ? ((JNonNullType) type).getUnderlyingType() : type;
      if (uType instanceof JArrayType) {
        JArrayType aType = (JArrayType) uType;
        JType leafType = aType.getLeafType();
        if (canBeOrdinal(leafType)) {
          JArrayType newAType = program.getTypeArray(JPrimitiveType.INT, aType.getDims());
          return nonNull ? newAType.getNonNull() : newAType;
        }
      }

      return null;
    }

    /**
     * Remove initialization of enum constants, and the $VALUES array, in the
     * clinit for an ordinalizable enum.
     */
    private void updateClinit(JMethod method) {
      assert JProgram.isClinit(method);
      JDeclaredType enclosingType = method.getEnclosingType();
      JBlock block = ((JMethodBody) method.getBody()).getBlock();
      int removeIndex = 0;
      // Make a copy to avoid concurrent modification.
      for (JStatement stmt : new ArrayList<JStatement>(block.getStatements())) {
        if (stmt instanceof JDeclarationStatement) {
          JVariableRef ref = ((JDeclarationStatement) stmt).getVariableRef();
          if (ref instanceof JFieldRef) {
            JFieldRef enumRef = (JFieldRef) ref;
            // See if LHS is a field ref to the class being initialized.
            JField field = enumRef.getField();
            if (field.isStatic() && field.getEnclosingType() == enclosingType) {
              if (field instanceof JEnumField || field.getName().equals("$VALUES")) {
                block.removeStmt(removeIndex--);
                field.setInitializer(null);
              }
            }
          }
        }
        ++removeIndex;
      }
    }
  }

  /**
   * Any references to the {@link Enum#ordinal()} method and
   * {@link Enum#ordinal} field for ordinalized types are replaced with the
   * qualifying instance.
   * 
   * Note, this visitor must run after the ReplaceEnumTypesWithInteger visitor,
   * since it depends on detecting the locations where the enumOrdinalField or
   * enumOrdinalMethod have had their types changed to integer.
   */
  private class ReplaceOrdinalFieldAndMethodRefsWithOrdinal extends JModVisitor {
    /**
     * Replace any references to Enum.ordinal field with the qualifying
     * instance, if that instance has been ordinalized (e.g. replace
     * <code>4.ordinal</code> with <code>4</code>).
     */
    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      super.endVisit(x, ctx);
      if (x.getField() == enumOrdinalField) {
        if (x.getInstance() != null) {
          JType type = x.getInstance().getType();
          if (type == JPrimitiveType.INT) {
            ctx.replaceMe(x.getInstance());
          }
        }
      }
    }

    /**
     * Replace any references to Enum.ordinal() with the qualifying instance, if
     * that instance has been ordinalized (e.g. replace <code>4.ordinal()</code>
     * with <code>4</code>).
     */
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      super.endVisit(x, ctx);
      if (x.getTarget() == enumOrdinalMethod) {
        if (x.getInstance() != null) {
          JType type = x.getInstance().getType();
          if (type == JPrimitiveType.INT) {
            ctx.replaceMe(x.getInstance());
          }
        }
      }
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // don't waste time visiting the large ClassLiteralHolder class
      if (x == classLiteralHolderType) {
        return false;
      }
      return true;
    }
  }

  private static final String NAME = EnumOrdinalizer.class.getSimpleName();

  private static Tracker tracker = null;

  private static boolean trackerEnabled =
      (System.getProperty("gwt.enableEnumOrdinalizerTracking") != null);

  public static void enableTracker() {
    trackerEnabled = true;
  }

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);

    startTracker();
    OptimizerStats stats = new EnumOrdinalizer(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  public static Tracker getTracker() {
    return tracker;
  }

  public static void resetTracker() {
    if (tracker != null) {
      tracker = null;
      startTracker();
    }
  }

  private static void startTracker() {
    if (trackerEnabled && tracker == null) {
      tracker = new Tracker();
    }
  }

  private final JMethod classCreateForEnumMethod;
  private final JType classLiteralHolderType;
  private final JMethod enumCreateValueOfMapMethod;
  private final JField enumOrdinalField;
  private final JMethod enumOrdinalMethod;
  private final JMethod enumSuperConstructor;
  private final Map<String, JEnumType> enumsVisited = new HashMap<String, JEnumType>();
  private final JType javaScriptObjectType;
  private final JType nullType;
  private final Set<JEnumType> ordinalizationBlackList = new HashSet<JEnumType>();
  private final JProgram program;

  public EnumOrdinalizer(JProgram program) {
    this.program = program;
    this.classLiteralHolderType = program.getIndexedType("ClassLiteralHolder");
    this.nullType = program.getTypeNull();
    this.javaScriptObjectType = program.getJavaScriptObject();
    this.enumOrdinalField = program.getIndexedField("Enum.ordinal");
    this.classCreateForEnumMethod = program.getIndexedMethod("Class.createForEnum");
    this.enumCreateValueOfMapMethod = program.getIndexedMethod("Enum.createValueOfMap");
    this.enumOrdinalMethod = program.getIndexedMethod("Enum.ordinal");
    this.enumSuperConstructor = program.getIndexedMethod("Enum.Enum");
  }

  private OptimizerStats execImpl() {
    OptimizerStats stats = new OptimizerStats(NAME);

    if (tracker != null) {
      tracker.incrementRunCount();
      tracker.maybeDumpAST(program, 0);
    }

    // Create black list of enum refs which can't be converted to an ordinal ref
    CannotBeOrdinalAnalyzer ordinalAnalyzer = new CannotBeOrdinalAnalyzer(program);
    ordinalAnalyzer.accept(program);
    ordinalAnalyzer.afterVisitor();

    // Bail if we don't need to do any ordinalization
    if (enumsVisited.size() == ordinalizationBlackList.size()) {
      // Update tracker stats
      if (tracker != null) {
        for (JEnumType type : enumsVisited.values()) {
          tracker.addVisited(type.getName());
        }
      }
      return stats;
    }

    // Replace enum type refs
    ReplaceEnumTypesWithInteger replaceEnums = new ReplaceEnumTypesWithInteger();
    replaceEnums.accept(program);
    stats.recordModified(replaceEnums.getNumMods());

    if (tracker != null) {
      tracker.maybeDumpAST(program, 1);
    }

    // Replace enum field and method refs
    ReplaceOrdinalFieldAndMethodRefsWithOrdinal replaceOrdinalRefs =
        new ReplaceOrdinalFieldAndMethodRefsWithOrdinal();
    replaceOrdinalRefs.accept(program);
    stats.recordModified(replaceOrdinalRefs.getNumMods());

    if (tracker != null) {
      tracker.maybeDumpAST(program, 2);
    }

    // Update enums ordinalized, and tracker stats
    for (JEnumType type : enumsVisited.values()) {
      if (tracker != null) {
        tracker.addVisited(type.getName());
      }
      if (!ordinalizationBlackList.contains(type)) {
        if (tracker != null) {
          tracker.addOrdinalized(type.getName());
        }
        type.setOrdinalized();
      }
    }
    return stats;
  }

  private JEnumType getEnumType(JType type) {
    type = getPossiblyUnderlyingType(type);
    if (type instanceof JClassType) {
      return ((JClassType) type).isEnumOrSubclass();
    }
    return null;
  }

  private JEnumType getEnumTypeFromArrayLeafType(JType type) {
    type = getPossiblyUnderlyingType(type);
    if (type instanceof JArrayType) {
      type = ((JArrayType) type).getLeafType();
      return getEnumType(type);
    }
    return null;
  }

  private JType getPossiblyUnderlyingType(JType type) {
    if (type instanceof JReferenceType) {
      return ((JReferenceType) type).getUnderlyingType();
    }
    return type;
  }
}
