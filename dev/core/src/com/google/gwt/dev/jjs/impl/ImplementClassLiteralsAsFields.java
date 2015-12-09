/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.dev.jjs.Correlation.Literal;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JEnumType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JField.Disposition;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.dev.jjs.ast.js.JsniClassLiteral;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Create fields to represent the mechanical implementation of class literals.
 * Must be done after all class literals are created, but before optimizations
 * begin. {@link ControlFlowAnalyzer} depends on this.
 * <p>
 * Class literals are implemented as static field references. The static fields
 * are all put into the special com.google.gwt.lang.ClassLiteralHolder class.
 * Ordinarily, accessing one of these fields would trigger a clinit to run, but
 * we've special-cased class literal fields to evaluate as top-level code before
 * the application starts running to avoid the clinit.
 *
 * Class literal factory methods are responsible for installing references
 * to themselves on the Object.clazz field of their JS runtime prototype
 * since getClass() is no longer an overridden method.  Prototypes can be
 * looked up via 'typeId' from the global prototypesByTypeId object, and so each
 * class literal factory method is passed the typeId of its type.
 * <p>
 */
public class ImplementClassLiteralsAsFields {
  private static Map<Class<? extends JType>, ClassLiteralFactoryMethod>
      literalFactoryMethodByTypeClass  = new ImmutableMap.Builder()
          .put(JEnumType.class, ClassLiteralFactoryMethod.CREATE_FOR_ENUM)
          .put(JClassType.class, ClassLiteralFactoryMethod.CREATE_FOR_CLASS)
          .put(JInterfaceType.class, ClassLiteralFactoryMethod.CREATE_FOR_INTERFACE)
          .put(JPrimitiveType.class, ClassLiteralFactoryMethod.CREATE_FOR_PRIMITIVE)
          .build();

  /**
   * Class used to construct invocations to class literal factory methods.
   */
  public enum ClassLiteralFactoryMethod {
    CREATE_FOR_ENUM() {
      @Override
      JMethodCall createCall(SourceInfo info, JProgram program, JType type,
          JLiteral superclassLiteral) {
        JEnumType enumType = type.isEnumOrSubclass();
        assert enumType != null;

        // createForEnum(packageName, typeName, runtimeTypeReference, Enum.class,  type.values(),
        // type.valueOf(java/lang/String));
        JMethodCall call = createBaseCall(info, program, type, "Class.createForEnum");

        call.addArg(new JRuntimeTypeReference(info, program.getTypeJavaLangObject(),
            (JReferenceType) type));
        call.addArg(superclassLiteral);

        call.addArg(getStandardMethodAsArg(info, program, type, "values()"));
        call.addArg(getStandardMethodAsArg(info, program, type, "valueOf(Ljava/lang/String;)"));
        return call;
      }

      private JExpression getStandardMethodAsArg(SourceInfo info, JProgram program, JType type,
          String methodSignature) {
        JEnumType enumType = type.isEnumOrSubclass();

        if (enumType != type) {
          // The type is an anonymous subclass that represents one of the enum values
          return JNullLiteral.INSTANCE;
        }

        // This type is the base enum type not one of the anonymous classes that represent
        // enum values.
        for (JMethod method : enumType.getMethods()) {
          if (method.isStatic() && method.getSignature().startsWith(methodSignature)) {
            return new JsniMethodRef(info, method.getJsniSignature(true, false),
                method, program.getJavaScriptObject());
          }
        }

        // The method was pruned.
        return JNullLiteral.INSTANCE;
      }
    },
    CREATE_FOR_CLASS() {
      @Override
      JMethodCall createCall(SourceInfo info, JProgram program, JType type,
          JLiteral superclassLiteral) {

        // Class.createForClass(packageName, typeName, runtimeTypeReference, superclassliteral)
        JMethodCall call =
            createBaseCall(info, program, type, RuntimeConstants.CLASS_CREATE_FOR_CLASS);

        call.addArg(new JRuntimeTypeReference(info, program.getTypeJavaLangObject(),
            (JReferenceType) type));
        call.addArg(superclassLiteral);
        return call;
      }
    },
    CREATE_FOR_PRIMITIVE() {
      @Override
      JMethodCall createCall(SourceInfo info, JProgram program, JType type,
          JLiteral superclassLiteral) {

        // Class.createForPrimitive(typeName, typeSignature)
        JMethodCall call = new JMethodCall(info, null, program.getIndexedMethod(
                RuntimeConstants.CLASS_CREATE_FOR_PRIMITIVE));
        call.addArg(program.getStringLiteral(info, type.getShortName()));
        call.addArg(program.getStringLiteral(info, type.getJavahSignatureName()));
        return call;
      }
    },
    CREATE_FOR_INTERFACE() {
      @Override
      JMethodCall createCall(SourceInfo info, JProgram program, JType type,
          JLiteral superclassLiteral) {

        // Class.createForInterface(packageName, typeName)
        return createBaseCall(info, program, type, RuntimeConstants.CLASS_CREATE_FOR_INTERFACE);
      }
    };

    abstract JMethodCall createCall(SourceInfo info, JProgram program, JType type,
        JLiteral superclassLiteral);

    private static JMethodCall createBaseCall(SourceInfo info, JProgram program, JType type,
        String indexedMethodName) {

      String[] compoundName = maybeMangleJSOTypeName(type);
      JMethodCall call = new JMethodCall(info, null, program.getIndexedMethod(indexedMethodName),
          program.getStringLiteral(info, type.getPackageName()),
          getCompoundNameLiteral(program, info, compoundName));

      return call;
    }

    private static String[] maybeMangleJSOTypeName(JType type) {
      assert !(type instanceof JArrayType);
      String[] compoundName = type.getCompoundName();

      // Mangle the class name to match hosted mode.
      if (type.isJsoType()) {
        compoundName[compoundName.length - 1] = compoundName[compoundName.length - 1] + '$';
      }
      return compoundName;
    }

    private static JExpression getCompoundNameLiteral(final JProgram program, final SourceInfo info,
        String[] compoundName) {
      return program.getStringLiteral(info, Joiner.on('/').join(compoundName));
    }
  }

  private class NormalizeVisitor extends JModVisitor {
    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      JType type = x.getRefType();
      if (type instanceof JArrayType && !type.isJsNative()) {
        // Replace array class literals by an expression to obtain the class literal from the
        // leaf type of the array.
        JArrayType arrayType = (JArrayType) type;
        JClassLiteral leafTypeClassLiteral =
            new JClassLiteral(x.getSourceInfo(), arrayType.getLeafType());
        resolveClassLiteral(leafTypeClassLiteral);

        JExpression arrayClassLiteralExpression = program.createArrayClassLiteralExpression(
            x.getSourceInfo(), leafTypeClassLiteral, arrayType.getDims());
        ctx.replaceMe(arrayClassLiteralExpression);
      } else {
        // Just resolve the class literal.
        resolveClassLiteral(x);
      }
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (x.isJsMethodVarargs()) {
        // ImplementJsVarargs might insert an array creation for the varargs parameter which is not
        // seen by this pass.
        JParameter varargsParameter = Iterables.getLast(x.getParams());
        assert varargsParameter.isVarargs();
        resolveClassLiteralField(((JArrayType) varargsParameter.getType()).getLeafType());
      }
    }

    @Override
    public void endVisit(JsniClassLiteral x, Context ctx) {
      // JsniClassLiterals will be traversed explicitly in JsniMethodBody. For each JsniClassLiteral
      // in JsniMethodBody.classRefs there is at least a corresponding JsNameRef (with a jsni
      // identifier) that needs to be altered accordingly.
    }

    @Override
    public void endVisit(final JsniMethodBody jsniMethodBody, Context ctx) {
      if (jsniMethodBody.getClassRefs().size() == 0) {
        return;
      }

      final Multimap<String, JsniClassLiteral> jsniClassLiteralsByJsniReference =
          ArrayListMultimap.create();
      final JMethod getClassLiteralForArrayMethod =
          program.getIndexedMethod(RuntimeConstants.ARRAY_GET_CLASS_LITERAL_FOR_ARRAY);
      final String getClassLiteralForArrayMethodIdent =
          "@" + getClassLiteralForArrayMethod.getJsniSignature(true, false);

      boolean areThereArrayClassLiterals = false;
      for (JsniClassLiteral jsniClassLiteral : jsniMethodBody.getClassRefs()) {

        // Check for the presence of array class literals.
        if (jsniClassLiteral.getRefType() instanceof JArrayType) {
          areThereArrayClassLiterals = true;
        } else {
          // Resolve non array class literals.
          resolveClassLiteral(jsniClassLiteral);
        }

        // Map JSNI reference string to the actual JsniClassLiterals.
        jsniClassLiteralsByJsniReference.put(jsniClassLiteral.getIdent(), jsniClassLiteral);
      }

      if (!areThereArrayClassLiterals) {
        // No array class literal no need to explore the body.
        return;
      }

      final Set<JsniClassLiteral> newClassRefs = Sets.newLinkedHashSet();

      // Replace all arrays JSNI class literals with the its construction via the leaf type class
      // literal.
      JsModVisitor replaceJsniClassLiteralVisitor = new JsModVisitor() {

        @Override
        public void endVisit(JsNameRef x, JsContext ctx) {
          if (!x.isJsniReference()) {
            return;
          }

          if (jsniClassLiteralsByJsniReference.get(x.getIdent()).isEmpty()) {
            // The JsNameRef is not a class literal.
            return;
          }

          JsniClassLiteral jsniClassLiteral = jsniClassLiteralsByJsniReference.get(
              x.getIdent()).iterator().next();
          jsniClassLiteralsByJsniReference.remove(x.getIdent(), jsniClassLiteral);

          if (jsniClassLiteral.getRefType() instanceof JArrayType) {
            // Replace the array class literal by an expression that retrieves it from
            // that of the leaf type.
            JArrayType arrayType = (JArrayType) jsniClassLiteral.getRefType();
            JType leafType = arrayType.getLeafType();

            jsniClassLiteral = new JsniClassLiteral(jsniClassLiteral.getSourceInfo(), leafType);

            // Array.getClassLiteralForArray(leafType.class, dimensions)
            SourceInfo info = x.getSourceInfo();
            JsNameRef getArrayClassLiteralMethodNameRef =
                new JsNameRef(info, getClassLiteralForArrayMethodIdent);
            JsInvocation invocation = new JsInvocation(info, getArrayClassLiteralMethodNameRef,
                new JsNameRef(info, jsniClassLiteral.getIdent()),
                new JsNumberLiteral(info, arrayType.getDims()));
            // Finally resolve the class literal.
            resolveClassLiteral(jsniClassLiteral);
            ctx.replaceMe(invocation);
          }
          newClassRefs.add(jsniClassLiteral);
        }
      };
      replaceJsniClassLiteralVisitor.accept(jsniMethodBody.getFunc());
      if (!replaceJsniClassLiteralVisitor.didChange()) {
        // Nothing was changed, no need to replace JsniMethodBody.
        return;
      }

      JsniMethodBody newBody = new JsniMethodBody(jsniMethodBody.getSourceInfo(), jsniMethodBody.getFunc(),
          Lists.newArrayList(newClassRefs), jsniMethodBody.getJsniFieldRefs(),
          jsniMethodBody.getJsniMethodRefs(), jsniMethodBody.getUsedStrings());

      // Add getClassLiteralForArray as a JsniMethodRef.
      newBody.addJsniRef(
          new JsniMethodRef(jsniMethodBody.getSourceInfo(), getClassLiteralForArrayMethodIdent,
              getClassLiteralForArrayMethod, program.getJavaScriptObject()));

      ctx.replaceMe(newBody);
    }
  }

  public static void exec(JProgram program, boolean shouldOptimize) {
    Event normalizerEvent = SpeedTracerLogger.start(CompilerEventType.NORMALIZER);
    new ImplementClassLiteralsAsFields(program, shouldOptimize).execImpl();
    normalizerEvent.end();
  }

  private final Map<JType, JField> classLiteralFields = Maps.newIdentityHashMap();
  private final JMethodBody classLiteralHolderClinitBody;
  private final JProgram program;
  private final JClassType typeClassLiteralHolder;
  private final boolean shouldOptimize;

  private ImplementClassLiteralsAsFields(JProgram program, boolean shouldOptimize) {
    this.program = program;
    this.typeClassLiteralHolder = program.getTypeClassLiteralHolder();
    this.classLiteralHolderClinitBody = (JMethodBody) typeClassLiteralHolder.getClinitMethod().getBody();
    this.shouldOptimize = shouldOptimize;
    assert program.getDeclaredTypes().contains(typeClassLiteralHolder);
  }

  private JLiteral getSuperclassClassLiteral(SourceInfo info, JType type) {
    if (!(type instanceof JClassType) ||  ((JClassType) type).getSuperClass() == null) {
      return JNullLiteral.INSTANCE;
    }
    return createDependentClassLiteral(info, ((JClassType) type).getSuperClass());
  }

  private JClassLiteral createDependentClassLiteral(SourceInfo info, JType type) {
    JClassLiteral classLiteral = new JClassLiteral(info.makeChild(), type);
    classLiteral.setField(resolveClassLiteralField(classLiteral.getRefType()));
    return classLiteral;
  }

  private void execImpl() {
    if (!shouldOptimize) {
      // Create all class literals regardless of whether they are referenced or not.
      for (JPrimitiveType type : JPrimitiveType.types) {
        resolveClassLiteralField(type);
      }
      for (JType type : program.getDeclaredTypes()) {
        resolveClassLiteralField(type);
      }
    }
    NormalizeVisitor visitor = new NormalizeVisitor();
    visitor.accept(program);
    program.recordClassLiteralFields(classLiteralFields);
  }

  /**
   * Create an expression that will evaluate, at run time, to the class literal.
   * Causes recursive literal create (super type, array element type). Examples:
   *
   * Class:
   *
   * <pre>
   * Class.createForClass("java.lang.", "Object", /JRuntimeTypeReference/"java.lang.Object", null)
   * Class.createForClass("java.lang.", "Exception", /JRuntimeTypeReference/"java.lang.Exception",
   *   Throwable.class)
   * </pre>
   *
   * Interface:
   *
   * <pre>
   * Class.createForInterface(&quot;java.lang.&quot;, &quot;Comparable&quot;)
   * </pre>
   *
   * Arrays are lazily created.
   *
   * Primitive:
   *
   * <pre>
   * Class.createForPrimitive(&quot;&quot;, &quot;int&quot;, &quot;I&quot;)
   * </pre>
   *
   * Enum:
   *
   * <pre>
   * Class.createForEnum("com.example.", "MyEnum", /JRuntimeTypeReference/"com.example.MyEnum",
   *     Enum.class, public static MyEnum[] values(), public static MyEnum valueOf(String name))
   * </pre>
   *
   * Enum subclass:
   *
   * <pre>
   * Class.createForEnum("com.example.", "MyEnum$1", /JRuntimeTypeReference/"com.example.MyEnum$1",
   *   MyEnum.class, null, null))
   * </pre>
   */
  private JMethodCall createLiteralCall(SourceInfo info, JProgram program, JType type) {
    type = type.getUnderlyingType();

    Class<? extends JType>  typeClass = type.getClass();
    if (type.isEnumOrSubclass() != null) {
      typeClass = JEnumType.class;
    }

    return literalFactoryMethodByTypeClass.get(typeClass).createCall(info, program, type,
        getSuperclassClassLiteral(info, type));
  }

  private void resolveClassLiteral(JClassLiteral x) {
    JField field = resolveClassLiteralField(x.getRefType());
    x.setField(field);
  }

  /**
   * Resolve a class literal field. Takes the form:
   *
   * <pre>
   * class ClassLiteralHolder {
   *   Class Ljava_lang_Object_2_classLit =
   *       Class.createForClass("java.lang.", "Object", /JNameOf/"java.lang.Object", null)
   * }
   * </pre>
   */
  private JField resolveClassLiteralField(JType type) {
    type = type.isJsNative() ? program.getJavaScriptObject() : program.normalizeJsoType(type);
    JField field = classLiteralFields.get(type);
    if (field == null) {
      // Create the allocation expression FIRST since this may be recursive on
      // super type (this forces the super type classLit to be created first).
      SourceInfo info = type.getSourceInfo().makeChild();
      assert !(type instanceof JArrayType);

      JMethodCall classLiteralCreationExpression = createLiteralCall(info, program, type);

      // Create a field in the class literal holder to hold the object.
      field =
          new JField(info, getClassLiteralFieldName(type), typeClassLiteralHolder, program
              .getTypeJavaLangClass(), true, Disposition.FINAL);
      typeClassLiteralHolder.addField(field);
      info.addCorrelation(info.getCorrelator().by(Literal.CLASS));

      // Initialize the field.
      JFieldRef fieldRef = new JFieldRef(info, null, field, typeClassLiteralHolder);
      JDeclarationStatement decl =
          new JDeclarationStatement(info, fieldRef, classLiteralCreationExpression);
      classLiteralHolderClinitBody.getBlock().addStmt(decl);
      classLiteralFields.put(type, field);
    }
    return field;
  }

  private static String getClassLiteralFieldName(JType type) {
    return JjsUtils.classLiteralFieldNameFromJavahTypeSignatureName(type.getJavahSignatureName());
  }
}
