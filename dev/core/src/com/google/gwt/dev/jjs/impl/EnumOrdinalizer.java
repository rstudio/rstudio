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
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JEnumField;
import com.google.gwt.dev.jjs.ast.JEnumType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

/**
 * This optimizer replaces enum constants with their ordinal value (a simple
 * int) when possible.  We call this process "ordinalization".
 * 
 * Generally, this can be done for enums that are only ever referred to by 
 * reference, or by their ordinal value.  For the specific set of conditions 
 * under which ordinalization can proceed, see the notes for the nested class 
 * {@link EnumOrdinalizer.CannotBeOrdinalAnalyzer} below.
 *
 * This optimizer modifies enum classes to change their field constants to ints,
 * and to remove initialization of those constants in the clinit method.  An
 * ordinalized enum class will not be removed from the AST by this optimizer,
 * but as long as all references to it are replaced (which is one of the 
 * requirements for ordinalization), then the enum class itself will be pruned 
 * by subsequent optimizer passes.
 *
 * Regardless of whether an ordinalized enum class ends up being completely
 * pruned away, the AST is expected to be in a coherent and usable state after 
 * any pass of this optimizer.  Thus, this optimizer should be considered to be 
 * stateless.
 * 
 * The process is broken up into 3 separate passes over the AST, each
 * implemented as a separate visitor class.  The first visitor, 
 * {@link EnumOrdinalizer.CannotBeOrdinalAnalyzer} compiles information
 * about each enum class in the AST, and looks for reasons not to ordinalize
 * each enum.  Thus, it prepares a "black-list" of enum classes that cannot be
 * ordinalized (and it follows that all enums that don't get entered in the 
 * black-list, will be allowed to be ordinalized.  The set of conditions which
 * cause an enum to be black-listed are outlined below.
 * 
 * If there are enum classes that didn't get black-listed remaining, the
 * subsequent passes of the optimizer will be invoked.  The first,
 * {@link EnumOrdinalizer.ReplaceEnumTypesWithInteger}, replaces the type info
 * for each field or expression involved with a target enum constant with an
 * integer.  The final visitor, descriptively named 
 * {@link EnumOrdinalizer.ReplaceOrdinalFieldAndMethodRefsWithOrdinal}, will do
 * the job of replacing the value for references to the Enum ordinal field or
 * method of an enum constant that has been ordinalized.
 */
public class EnumOrdinalizer {
  public static String NAME = EnumOrdinalizer.class.getSimpleName();
  public static Tracker tracker = null;
  
  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(
        CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new EnumOrdinalizer(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }
  
  public static Tracker getTracker() {
    return tracker;
  }
  
  public static void startTracker() {
    tracker = new Tracker();
  }
  
  public static void stopTracker() {
    tracker = null;
  }
  
  private final JProgram program;
  private final JType classLiteralHolderType;
  private final JType nullType;
  private final JType javaScriptObjectType;
  private final JType enumType;
  private final JField enumOrdinalField;
  private final JMethod classCreateForEnumMethod;
  private final JMethod enumCreateValueOfMapMethod;
  private final JMethod enumSuperConstructor;
  private final JMethod enumOrdinalMethod;
  private final Set<JEnumType> ordinalizationBlackList = new HashSet<JEnumType>();
  private final Map<String, JEnumType> enumsVisited = new HashMap<String, JEnumType>();

  public EnumOrdinalizer(JProgram program) {
    this.program = program;
    this.classLiteralHolderType = program.getIndexedType("ClassLiteralHolder");
    this.nullType = program.getTypeNull();
    this.javaScriptObjectType = program.getJavaScriptObject();
    this.enumType = program.getIndexedType("Enum");
    this.enumOrdinalField = program.getIndexedField("Enum.ordinal");
    this.classCreateForEnumMethod = program.getIndexedMethod("Class.createForEnum");
    this.enumCreateValueOfMapMethod = program.getIndexedMethod("Enum.createValueOfMap");
    this.enumOrdinalMethod = program.getIndexedMethod("Enum.ordinal");
    this.enumSuperConstructor = program.getIndexedMethod("Enum.Enum");
  }
  
  /**
   * A visitor which keeps track of the enums which cannot be ordinalized.  It
   * does this by keeping track of a "black-list" for ordinals which violate the
   * conditions for ordinalization, below.
   * 
   * An enum cannot be ordinalized, if it:
   *    is implicitly upcast.
   *    is implicitly cast to from a nullType.
   *    is implicitly cast to or from a javaScriptObject type.
   *    is explicitly cast to another type (or vice-versa).
   *    it's class literal is used explicitly.
   *    it has an artificial rescue recorded for it.
   *    has any field referenced, except for:
   *      one of it's enum constants
   *      Enum.ordinal
   *    has any method called, except for:
   *      ordinal()
   *      Enum.ordinal()
   *      Enum() super constructor
   *      Enum.createValueOfMap()
   *    
   * This visitor extends the ImplicitUpcastAnalyzer, which encapsulates all 
   * the conditions where implicit upcasting can occur in an AST.  The rest of
   * the logic for checking ordinalizability is encapsulated in this sub-class.
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
   * within a JSNI method body.  We don't get a visit to JClassLiteral in that
   * case, so we need to inspect visits to JsniFieldRef for the possibility it
   * might be a reference to a class literal.
   * 
   * We also skip any checking in a method call to Enum.createValueOfMap(), 
   * since this is generated for any enum class initially within the extra 
   * enumClass$Map class, and this call contains an implicit upcast in the 
   * method call args (as well as a reference to the static enumClass$VALUES 
   * field), which we want to ignore.  The enumClass$Map class will not get 
   * pruned as long as the enumClass is not ordinalized, and so we need to 
   * ignore it's presence in the consideration for whether an enum class is 
   * ordinalizable.
   */
  private class CannotBeOrdinalAnalyzer extends ImplicitUpcastAnalyzer {
    
    private final Set<String> jsniClassLiteralsVisited = new HashSet<String>();
    private final Stack<JCastOperation> castOpsToIgnore =
        new Stack<JCastOperation>();
  
    public CannotBeOrdinalAnalyzer(JProgram program) {
      super(program);
    }
    
    /*
     * After program is visited, post-process remaining tasks from accumulated data.
     */
    public void afterVisitor() {
      // black-list any Jsni enum ClassLiteralsVisited
      for (String classLiteralName : jsniClassLiteralsVisited) {
        JEnumType enumFromLiteral = enumsVisited.get(classLiteralName);
        if (enumFromLiteral != null) {
          addToBlackList(enumFromLiteral);
        }
      }
    }
    
    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      // see if we've previously marked this castOp to be exempted
      if (!castOpsToIgnore.empty() &&
          castOpsToIgnore.peek() == x) {
        castOpsToIgnore.pop();
        return;
      }
      
      // check for explicit cast (check both directions)
      blackListIfEnumCast(x.getExpr().getType(), x.getCastType());
      blackListIfEnumCast(x.getCastType(), x.getExpr().getType());
    }
    
    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      /*
       * Check for references to an enum's class literal.  We need to 
       * black-list classes in this case, since there could be a call
       * to Enum.valueOf(someEnum.class,"name"), etc.
       * 
       * Note: we won't get here for class literals that occur in the
       * ClassLiteralHolder class, or within the getClass method of an
       * enum class (see comments above).
       */
      JEnumType type = getEnumType(x.getRefType());
      if (type != null) {
        blackListIfEnum(type);
      }
    }
    
    @Override
    public void endVisit(JClassType x, Context ctx) {
      // black-list any artificially rescued classes recorded for this class
      List<JNode> rescues = x.getArtificialRescues();
      if (rescues != null && rescues.size() > 0) {
        for (JNode rescueNode : rescues) {
          if (rescueNode instanceof JType) {
            blackListIfEnum((JType) rescueNode);
          }
        }
      }
      
      // keep track of all enum classes visited
      JEnumType maybeEnum = x.isEnumOrSubclass();
      if (maybeEnum != null) {
        enumsVisited.put(program.getClassLiteralName(maybeEnum), maybeEnum);
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
        // check static field references
        
        /*
         * Need to exempt static fieldRefs to the special $VALUES array that
         * gets generated for all enum classes, if the reference occurs
         * within the enum class itself (such as happens in the clinit() or 
         * values() method for all enums).
         */ 
        if (x.getField().getName().equals("$VALUES") &&
            this.currentMethod.getEnclosingType() == 
                x.getField().getEnclosingType()) {
          if (getEnumType(x.getField().getEnclosingType()) != null) {
            return;
          }
        }
        
        /*
         * Need to exempt static fieldRefs for enum constants themselves.
         * Detect these as final fields, that have the same enum type as
         * their enclosing type.
         */
        if (x.getField().isFinal() && 
              (x.getField().getEnclosingType() == 
                getEnumType(x.getField().getType()))) {
          return;
        }
        
        /*
         * Check any other refs to static fields of an enum class.  This
         * includes references to $VALUES that might occur outside of the enum
         * class itself.  This can occur when a call to the values() method gets
         * inlined, etc.  Also check here for any user defined static fields.
         */
        blackListIfEnum(x.getField().getEnclosingType());
      }
    }
    
    @Override
    public void endVisit(JsniFieldRef x, Context ctx) {
      /*
       * Can't do the same thing as for JFieldRef, 
       * all JsniFieldRefs are cast to JavaScriptObjects.
       * Need to check both the field type and the type of the instance
       * or enclosing class referencing the field.
       */ 
      
      // check the field type
      blackListIfEnum(x.getField().getType());
      
      // check the referrer
      if (x.getInstance() != null) {
        blackListIfEnumExpression(x.getInstance());
      } else {
        blackListIfEnum(x.getField().getEnclosingType());
      }
      
      /*
       * need to also check JsniFieldRef's for a possible reference to a 
       * class literal, since we don't get a visit to JClassLiteral when
       * it occurs within Jsni (shouldn't it?).
       */
      if (x.getField().getEnclosingType() == classLiteralHolderType) {
        // see if it has an initializer with a method call to "createForEnum"
        JExpression initializer = x.getField().getInitializer();
        if (initializer instanceof JMethodCall) {
          if (((JMethodCall) initializer).getTarget() == classCreateForEnumMethod) {
            jsniClassLiteralsVisited.add(x.getField().getName());
          }
        }
      }
    }
      
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // exempt calls to certain methods on the Enum super class
      if (x.getTarget() == enumCreateValueOfMapMethod ||
          x.getTarget() == enumSuperConstructor ||
          x.getTarget() == enumOrdinalMethod) {
        return;
      }
      
      // any other method on an enum class should cause it to be black-listed
      if (x.getInstance() != null) { 
        blackListIfEnumExpression(x.getInstance());
      } else if (x.getTarget().isStatic()) {
        /*
         * need to exempt static methodCalls for an enum class if it occurs
         * within the enum class itself (such as in $clinit() or values())
         */ 
        if (this.currentMethod.getEnclosingType() != 
                      x.getTarget().getEnclosingType()) {
          blackListIfEnum(x.getTarget().getEnclosingType());
        }
      }
      
      // defer to ImplicitUpcastAnalyzer to check method call args & params
      super.endVisit(x, ctx);
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
        if (this.currentMethod.getEnclosingType() != 
                      x.getTarget().getEnclosingType()) {
          blackListIfEnum(x.getTarget().getEnclosingType());
        }
      }
      
      // defer to ImplicitUpcastAnalyzer to check method call args & params
      super.endVisit(x, ctx);
    }
      
    @Override
    public boolean visit(JClassType x, Context ctx) {
      /*
       * Don't want to visit the large ClassLiteralHolder class, it doesn't
       * contain references to actual usage of enum class literals.  It's also
       * a time savings to not traverse this class.
       */
      if (x == classLiteralHolderType) {
        return false;
      }
      return true;
    }
    
    @Override
    public boolean visit(JFieldRef x, Context ctx) {
      /*
       * If we have a field ref of Enum.ordinal, then we want to allow a
       * cast operation from enum subclass to Enum on the instance.  Other
       * optimizers have a tendency to convert things like:
       * 
       *  'switch(enumObj)' to  
       *  'switch((Enum)enumObj).ordinal' 
       *  
       * We don't want to blacklist enumObj in that case, so we push this castOp
       * on a stack and check it in the subsequent call to endVisit for
       * JCastOperation.  We can't simply return false and prevent
       * the visit of the JCastOperation altogether, since we do need to visit
       * the JCastOperation's sub-expression.  Since the sub-expression could
       * potentially also contain similar cast operations, we use a stack to
       * keep track of 'castOpsToIgnore'.
       */
      if (x.getField() == enumOrdinalField) {
        if (x.getInstance() != null &&
            x.getInstance() instanceof JCastOperation) {
          JCastOperation castOp = (JCastOperation) x.getInstance();
          if (getPossiblyUnderlyingType(castOp.getCastType()) == enumType) {
            JEnumType fromType = getEnumType(castOp.getExpr().getType());
            if (fromType != null) {
              castOpsToIgnore.push(castOp);
            }
          }
        }
      }
      return true;
    }
    
    @Override
    public boolean visit(JMethod x, Context ctx) {
      /*
       * Don't want to visit the generated getClass() method on an enum, since
       * it contains a reference to an enum's class literal that we don't want
       * to consider.  Make sure this is not a user overloaded version 
       * (i.e. check that it has no params).
       */
      if (getEnumType(x.getEnclosingType()) != null && 
          x.getName().equals("getClass") && 
          (x.getOriginalParamTypes() == null || x.getOriginalParamTypes().size() == 0)) {
        return false;
      }
      
      // defer to parent method on ImplicitCastAnalyzer
      return super.visit(x, ctx);
    }
    
    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      /*
       * skip all calls to Enum.createValueOfMap, since they'd get falsely
       * flagged for referencing $VALUES and for implicitly upcasting an
       * array of an enum class, in the arg passing.  This method is only
       * used by the enumClass$Map class that gets generated for every enum
       * class.  Once ordinalization proceeds, this $Map class should be pruned.
       */
      if (x.getTarget() == enumCreateValueOfMapMethod) {
        return false;
      }
      
      /*
       * If we have a method call of Enum.ordinal(), then we want to allow a
       * cast operation from enum subclass to Enum on the instance.  Other
       * optimizers have a tendency to convert things like:
       * 
       *  'switch(enumObj.ordinal())' to  
       *  'switch((Enum)enumObj).ordinal' 
       *  
       * We don't want to blacklist enumObj in that case, so we push this castOp
       * on a stack and and check it in the subsequent call to endVisit for
       * JCastOperation (above).  We can't simply return false and prevent
       * the visit of the JCastOperation altogether, since we do need to visit
       * the castOperation's sub-expression.
       */
      if (x.getTarget() == enumOrdinalMethod) {
        if (x.getInstance() != null &&
            x.getInstance() instanceof JCastOperation) {
          JCastOperation castOp = (JCastOperation) x.getInstance();
          if (getPossiblyUnderlyingType(castOp.getCastType()) == enumType) {
            JEnumType fromType = getEnumType(castOp.getExpr().getType());
            if (fromType != null) {
              castOpsToIgnore.push(castOp);
            }
          }
        }
      }
     
      // ok to visit
      return true;
    }
    
    /*
     * Override for the method called from ImplicitUpcastAnalyzer, 
     * which will be called for any implicit upcast.
     */
    @Override
    protected void processImplicitUpcast(JType fromType, JType destType) {
      if (fromType == nullType) {
        // handle case where a nullType is cast to an enum
        blackListIfEnum(destType);
      } else if (fromType == javaScriptObjectType) {
        // handle case where a javaScriptObject is cast to an enum
        blackListIfEnum(destType);
      } else {
        blackListIfEnumCast(fromType, destType);
      }
    }
    
    private void addToBlackList(JEnumType enumType) {
      ordinalizationBlackList.add(enumType);
    }
    
    private void blackListIfEnum(JType maybeEnum) {
      JEnumType actualEnum = getEnumType(maybeEnum);
      if (actualEnum != null) {
        addToBlackList(actualEnum);
      }
    }
    
    private void blackListIfEnumCast(JType maybeEnum, JType destType) {
      JEnumType actualEnum = getEnumType(maybeEnum);
      JEnumType actualDestType = getEnumType(destType);
      if (actualEnum != null) {
        if (actualDestType != actualEnum) {
          addToBlackList(actualEnum);
        }
        return;
      }
      
      // check JArrayTypes of enums
      actualEnum = getEnumTypeFromArrayLeafType(maybeEnum);
      actualDestType = getEnumTypeFromArrayLeafType(destType);
      if (actualEnum != null) {
        if (actualDestType != actualEnum) {
          addToBlackList(actualEnum);
        }
      }
    }
    
    private void blackListIfEnumExpression(JExpression instance) {
      if (instance != null) {
        blackListIfEnum(instance.getType());
      }
    }
  }

  /**
   * A visitor which replaces enum types with an integer.
   * 
   * It sub-classes TypeRemapper, which encapsulates all the locations for a 
   * settable type.  The overridden remap() method will be called in each 
   * instance, and it will test whether the type is a candidate for replacement,
   * and if so, return the new type to set (JPrimitiveType.INT).
   * 
   * Any reference to an enum field constant in an expression is replaced 
   * with integer. 
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
      
      // cleanup clinit method for ordinalizable enums
      if (canBeOrdinal(x)) {
        // method 0 is always the clinit
        updateClinit(x.getMethods().get(0));
      }
      return true;
    } 

    @Override
    public boolean visit(JField x, Context ctx) {
      /*
       * Replace an enum field constant, with it's integer valued ordinal.
       */
      if (x instanceof JEnumField && canBeOrdinal(x.getEnclosingType())) {
        int ordinal = ((JEnumField) x).ordinal();
        x.setInitializer(new JDeclarationStatement(x.getSourceInfo(),
            new JFieldRef(x.getSourceInfo(), null, x,
                x.getEnclosingType()), program.getLiteralInt(ordinal)));
      }
      return true;
    }
    
    @Override
    public boolean visit(JFieldRef x, Context ctx) {
      /*
       * Replace an enum field ref with it's integer valued ordinal.
       */ 
      JField field = x.getField();
      if (field instanceof JEnumField && canBeOrdinal(field.getEnclosingType())) {
        int ordinal = ((JEnumField) field).ordinal();
        ctx.replaceMe(program.getLiteralInt(ordinal));
      }
      return true;
    }
    
    /*
     * Remap enum types with JPrimitiveType.INT.
     * Also handle case for arrays, replace enum leaftype with JPrimitive.INT.
     * This is an override implementation called from TypeRemapper.
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
      return uType instanceof JEnumType && 
          !ordinalizationBlackList.contains(uType);
    }
    
    private JType getOrdinalizedType(JType type) {
      if (canBeOrdinal(type)) {
        return JPrimitiveType.INT;
      }
      
      JType uType = getPossiblyUnderlyingType(type);
      if (uType instanceof JArrayType) {
        JArrayType aType = (JArrayType) uType;
        JType leafType = aType.getLeafType();
        if (canBeOrdinal(leafType)) {
          JArrayType newAType = program.getTypeArray(
                    JPrimitiveType.INT, aType.getDims());
          return newAType.getNonNull();
        }
      }
      
      return null;
    }

    /*
     * Remove initialization of enum constants, and the $VALUES array, in the
     * clinit for an ordinalizable enum.
     */
    private void updateClinit(JMethod method) {
      List<JStatement> stmts = ((JMethodBody) method.getBody()).getStatements();
      Iterator<JStatement> it = stmts.iterator();
      // look for statements of the form EnumValueField = ...
      while (it.hasNext()) {
        JStatement stmt = it.next();
        if (stmt instanceof JDeclarationStatement) {
          JVariableRef ref = ((JDeclarationStatement) stmt).getVariableRef();
          if (ref instanceof JFieldRef) {
            JFieldRef enumRef = (JFieldRef) ref;
            if (enumRef.getField().getEnclosingType() == method.getEnclosingType()) {
              // see if LHS is a field ref that corresponds to the class of this method
              if (enumRef.getField() instanceof JEnumField) {
                it.remove();
              } else if (enumRef.getField().getName().equals("$VALUES")) {
                it.remove();
              }
            }
          }
        } 
      }
    } 
  }

  /**
   * A visitor which will replace references to the ordinal field and 
   * ordinal method refs with the appropropriate ordinal integer value.
   * 
   * Note, this visitor must run after the ReplaceEnumTypesWithInteger visitor,
   * since it depends on detecting the locations where the enumOrdinalField or
   * enumOrdinalMethod have had their types changed to integer.
   */
  private class ReplaceOrdinalFieldAndMethodRefsWithOrdinal extends JModVisitor {
    @Override
    public boolean visit(JClassType x, Context ctx) {
      // don't waste time visiting the large ClassLiteralHolder class
      if (x == classLiteralHolderType) {
        return false;
      }
      return true;
    }
    
    @Override
    public boolean visit(JFieldRef x, Context ctx) {
      /*
       * Check field refs that refer to Enum.ordinal, but which have already
       * been ordinalized by the previous pass.  This is implemented with visit
       * instead of endVisit since, in the case of an underlying cast operation,
       * we don't want to traverse the instance expression before we replace it.
       */
      if (x.getField() == enumOrdinalField) {
        if (x.getInstance() != null) {
          JType type = x.getInstance().getType();
          if (type == JPrimitiveType.INT) {
            /*
             * See if this fieldRef was converted to JPrimitiveType.INT, but 
             * still points to the Enum.ordinal field.  If so, replace the field 
             * ref with the ordinalized int itself.
             */
            ctx.replaceMe(x.getInstance());
          } else if (x.getInstance() instanceof JCastOperation) {
            /*
             * See if this reference to Enum.ordinal is via a cast from an enum
             * sub-class, that we've already ordinalized to JPrimitiveType.INT.
             * If so, replace the whole cast operation.
             * (see JFieldRef visit method in CannotBeOrdinalAnalyzer above).
             */
            JCastOperation castOp = (JCastOperation) x.getInstance();
            if (getPossiblyUnderlyingType(castOp.getType()) == enumType) {
              if (castOp.getExpr().getType() == JPrimitiveType.INT) {
                ctx.replaceMe(castOp.getExpr());
              }
            }
          }
        }
      } 
      return true;
    }
    
    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      /*
       * See if this methodCall was converted to JPrimitiveType.INT, but still
       * points to the Enum.ordinal() method.  If so, replace the method call with
       * the ordinal expression itself.  Implement with visit (and not endVisit),
       * so we don't traverse the method call itself unnecessarily.
       */
      if (x.getTarget() == enumOrdinalMethod) {
        if (x.getInstance() != null) {
          JType type = x.getInstance().getType();
          if (type == JPrimitiveType.INT) {
            /*
             * See if this instance was converted to JPrimitiveType.INT, but still
             * points to the Enum.ordinal() method.  If so, replace the method call 
             * with the ordinalized int itself.
             */
            ctx.replaceMe(x.getInstance());
          } else if (x.getInstance() instanceof JCastOperation) {
            /*
             * See if this reference to Enum.ordinal() is via a cast from an enum
             * sub-class, that we've already ordinalized to JPrimitiveType.INT.
             * If so, replace the whole cast operation.
             * (see JMethodCall visit method in CannotBeOrdinalAnalyzer above).
             */
            JCastOperation castOp = (JCastOperation) x.getInstance();
            if (getPossiblyUnderlyingType(castOp.getType()) == enumType) {
              if (castOp.getExpr().getType() == JPrimitiveType.INT) {
                ctx.replaceMe(castOp.getExpr());
              }
            }
          }
        }
      }
      return true;
    }
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
    
    if (tracker != null) {
      for (JEnumType type : enumsVisited.values()) {
        tracker.addVisited(type.getName());
        if (!ordinalizationBlackList.contains(type)) {
          tracker.addOrdinalized(type.getName());
        }
      }
    }
    
    // Bail if we don't need to do any ordinalization
    if (enumsVisited.size() == ordinalizationBlackList.size()) {
      return stats;
    }
    
    // Replace enum type refs
    ReplaceEnumTypesWithInteger replaceEnums = 
        new ReplaceEnumTypesWithInteger();
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
  
  /**
   * A simple Tracker class for compiling lists of enum classes processed by
   * this optimizer.  If enabled, the results can be logged as debug output, and
   * the results can be tested after running with a given input.
   */
  public static class Tracker {
    private final Set<String> allEnumsVisited;
    private final Set<String> allEnumsOrdinalized;
    private final List<Set<String>> enumsVisitedPerPass;
    private final List<Set<String>> enumsOrdinalizedPerPass;
    private int runCount = -1;
    
    // use TreeSets, for nice sorted iteration for output
    public Tracker() {
      allEnumsVisited = new TreeSet<String>();
      allEnumsOrdinalized = new TreeSet<String>();
      enumsVisitedPerPass = new ArrayList<Set<String>>();
      enumsOrdinalizedPerPass = new ArrayList<Set<String>>();
      
      // add entry for initial pass
      enumsVisitedPerPass.add(new TreeSet<String>());
      enumsOrdinalizedPerPass.add(new TreeSet<String>());
    }
    
    public void addOrdinalized(String ordinalized) {
      enumsOrdinalizedPerPass.get(runCount).add(ordinalized);
      allEnumsOrdinalized.add(ordinalized);
    }
    
    public void addVisited(String visited) {
      enumsVisitedPerPass.get(runCount).add(visited);
      allEnumsVisited.add(visited);
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
        logger = logger.branch(logType, "Enums Not Ordinalized:");
        for (String enumVisited : allEnumsVisited) {
          if (!isOrdinalized(enumVisited)) {
            logger.branch(logType, enumVisited);
          }
        }
      }
    }
        
    public void logEnumsOrdinalized(TreeLogger logger, TreeLogger.Type logType) {
      if (logger != null) {
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
        logger = logger.branch(logType, "Enums Ordinalized per Optimization Pass:");
        int pass = 0;
        for (Set<String> enumsOrdinalized : enumsOrdinalizedPerPass) {
          pass++;
          if (enumsOrdinalized.size() > 0) {
            TreeLogger subLogger = logger.branch(logType, "Pass " + pass + ": " + 
                                      enumsOrdinalized.size() + " ordinalized");
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
        logger = logger.branch(logType, "Enums Visited per Optimization Pass:");
        int pass = 0;
        for (Set<String> enumsVisited : enumsVisitedPerPass) {
          pass++;
          if (enumsVisited.size() > 0) {
            TreeLogger subLogger = logger.branch(logType, "Pass " + pass + ": " + 
                                      enumsVisited.size() + " visited");
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
        logger.branch(logType, "After pass " + (runCount + 1));
        logger.branch(logType, allEnumsOrdinalized.size() + " of " + 
                                    allEnumsVisited.size() + " ordinalized");
        return logger;
      }
      return null;
    }
    
    public void maybeDumpAST(JProgram program, int stage) {
      AstDumper.maybeDumpAST(program, NAME + "_" + (runCount + 1) + "_" + stage);
    }
  } 
}
