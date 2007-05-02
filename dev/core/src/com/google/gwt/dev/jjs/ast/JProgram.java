/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jdt.FindDeferredBindingSitesVisitor;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Root for the AST representing an entire Java program.
 */
public class JProgram extends JNode {

  private static final int IS_ARRAY = 2;
  private static final int IS_CLASS = 3;
  private static final int IS_INTERFACE = 1;
  private static final int IS_NULL = 0;

  public static boolean methodsDoMatch(JMethod method1, JMethod method2) {
    // static methods cannot match each other
    if (method1.isStatic() || method2.isStatic()) {
      return false;
    }

    // names must be identical
    if (!method1.getName().equals(method2.getName())) {
      return false;
    }

    // original parameter types must be identical
    List/* <JType> */params1 = method1.getOriginalParamTypes();
    List/* <JType> */params2 = method2.getOriginalParamTypes();
    int params1size = params1.size();
    if (params1size != params2.size()) {
      return false;
    }

    for (int i = 0; i < params1size; ++i) {
      if (params1.get(i) != params2.get(i)) {
        return false;
      }
    }
    return true;
  }

  private static String dotify(char[][] name) {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < name.length; ++i) {
      if (i > 0) {
        result.append('.');
      }

      result.append(name[i]);
    }
    return result.toString();
  }

  public final List/* <JMethod> */entryMethods = new ArrayList/* <JMethod> */();

  public final Map/* <String, HasEnclosingType> */jsniMap = new HashMap/*
                                                                         * <String,
                                                                         * HasEnclosingType>
                                                                         */();

  public final List/* <JClassType> */specialTypes = new ArrayList/* <JClassType> */();

  public final JTypeOracle typeOracle = new JTypeOracle(this);

  private final List/* <JArrayType> */allArrayTypes = new ArrayList/* <JArrayType> */();

  private final List/* <JReferenceType> */allTypes = new ArrayList/* <JReferenceType> */();

  /**
   * Each entry is a HashMap(JType => JArrayType), arranged such that the number
   * of dimensions is that index (plus one) at which the JArrayTypes having that
   * number of dimensions resides.
   */
  private final ArrayList/* <HashMap<JType, JArrayType>> */dimensions = new ArrayList/*
                                                                                       * <HashMap<JType,
                                                                                       * JArrayType>>
                                                                                       */();

  private final Map/* <JMethod, JMethod> */instanceToStaticMap = new IdentityHashMap/*
                                                                                     * <JMethod,
                                                                                     * JMethod>
                                                                                     */();

  private List/* <JsonObject> */jsonTypeTable;

  private final JAbsentArrayDimension literalAbsentArrayDim = new JAbsentArrayDimension(
      this);

  private final JBooleanLiteral literalFalse = new JBooleanLiteral(this, false);

  private final JIntLiteral literalIntNegOne = new JIntLiteral(this, -1);

  private final JIntLiteral literalIntOne = new JIntLiteral(this, 1);

  private final JIntLiteral literalIntZero = new JIntLiteral(this, 0);

  private final JNullLiteral literalNull = new JNullLiteral(this);

  private final JBooleanLiteral literalTrue = new JBooleanLiteral(this, true);

  private final TreeLogger logger;

  private JField nullField;

  private JMethod nullMethod;

  private Map/* <JReferenceType, Integer> */queryIds;

  private JMethod rebindCreateMethod;

  private final RebindOracle rebindOracle;

  private final Map/* <String, JField> */specialFields = new HashMap/*
                                                                     * <String,
                                                                     * JField>
                                                                     */();

  private final Map/* <String, JMethod> */specialMethods = new HashMap/*
                                                                       * <String,
                                                                       * JMethod>
                                                                       */();

  private final Map/* <JMethod, JMethod> */staticToInstanceMap = new IdentityHashMap/*
                                                                                     * <JMethod,
                                                                                     * JMethod>
                                                                                     */();

  private final JPrimitiveType typeBoolean = new JPrimitiveType(this,
      "boolean", "Z", literalFalse);

  private final JPrimitiveType typeByte = new JPrimitiveType(this, "byte", "B",
      literalIntZero);

  private final JPrimitiveType typeChar = new JPrimitiveType(this, "char", "C",
      getLiteralChar((char) 0));

  private JClassType typeClass;

  private final JPrimitiveType typeDouble = new JPrimitiveType(this, "double",
      "D", getLiteralDouble(0));

  private final JPrimitiveType typeFloat = new JPrimitiveType(this, "float",
      "F", getLiteralFloat(0));

  private Map/* <JClassType, Integer> */typeIdMap = new HashMap/*
                                                                 * <JClassType,
                                                                 * Integer>
                                                                 */();

  private final JPrimitiveType typeInt = new JPrimitiveType(this, "int", "I",
      literalIntZero);

  private JClassType typeJavaLangObject;

  private final JPrimitiveType typeLong = new JPrimitiveType(this, "long", "J",
      getLiteralLong(0));

  private final Map/* <String, JReferenceType> */typeNameMap = new HashMap/*
                                                                           * <String,
                                                                           * JReferenceType>
                                                                           */();

  private final JNullType typeNull = new JNullType(this);

  private final JPrimitiveType typeShort = new JPrimitiveType(this, "short",
      "S", literalIntZero);

  private JClassType typeSpecialArray;

  private JClassType typeSpecialCast;

  private JClassType typeSpecialExceptions;

  private JClassType typeSpecialJavaScriptObject;

  private JClassType typeString;

  private final JPrimitiveType typeVoid = new JPrimitiveType(this, "void", "V",
      null);

  public JProgram(TreeLogger logger, RebindOracle rebindOracle) {
    super(null, null);
    this.logger = logger;
    this.rebindOracle = rebindOracle;
  }

  public void addEntryMethod(JMethod entryPoint) {
    if (!entryMethods.contains(entryPoint)) {
      entryMethods.add(entryPoint);
    }
  }

  /**
   * Helper to create an assignment, used to initalize fields, etc.
   */
  public JExpressionStatement createAssignmentStmt(SourceInfo info,
      JExpression lhs, JExpression rhs) {
    JBinaryOperation assign = new JBinaryOperation(this, info, lhs.getType(),
        JBinaryOperator.ASG, lhs, rhs);
    return assign.makeStatement();
  }

  public JClassType createClass(SourceInfo info, char[][] name,
      boolean isAbstract, boolean isFinal) {
    String sname = dotify(name);
    JClassType x = new JClassType(this, info, sname, isAbstract, isFinal);

    allTypes.add(x);
    putIntoTypeMap(sname, x);

    if (sname.equals("java.lang.Object")) {
      typeJavaLangObject = x;
      specialTypes.add(x);
    } else if (sname.equals("java.lang.String")) {
      typeString = x;
    } else if (sname.equals("java.lang.Class")) {
      typeClass = x;
    } else if (sname.equals("com.google.gwt.core.client.JavaScriptObject")) {
      typeSpecialJavaScriptObject = x;
    } else if (sname.equals("com.google.gwt.lang.Array")) {
      typeSpecialArray = x;
      specialTypes.add(x);
    } else if (sname.equals("com.google.gwt.lang.Cast")) {
      typeSpecialCast = x;
      specialTypes.add(x);
    } else if (sname.equals("com.google.gwt.lang.Exceptions")) {
      typeSpecialExceptions = x;
      specialTypes.add(x);
    }

    return x;
  }

  public JField createField(SourceInfo info, char[] name,
      JReferenceType enclosingType, JType type, boolean isStatic,
      boolean isFinal, boolean hasInitializer) {
    assert (name != null);
    assert (enclosingType != null);
    assert (type != null);

    /*
     * MAGIC: special fields are filled in during code gen, don't bother
     * synthesizing dummy initializations.
     */
    boolean isSpecialField = specialTypes.contains(enclosingType);

    if (isSpecialField) {
      hasInitializer = true;
    }

    String sname = String.valueOf(name);
    JField x = new JField(this, info, sname, enclosingType, type, isStatic,
        isFinal, hasInitializer);

    if (isSpecialField) {
      specialFields.put(enclosingType.getShortName() + '.' + sname, x);
    }

    enclosingType.fields.add(x);

    return x;
  }

  public JInterfaceType createInterface(SourceInfo info, char[][] name) {
    String sname = dotify(name);
    JInterfaceType x = new JInterfaceType(this, info, sname);

    allTypes.add(x);
    putIntoTypeMap(sname, x);

    return x;
  }

  public JLocal createLocal(SourceInfo info, char[] name, JType type,
      boolean isFinal, JMethod enclosingMethod) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethod != null);

    JLocal x = new JLocal(this, info, String.valueOf(name), type, isFinal,
        enclosingMethod);

    enclosingMethod.locals.add(x);

    return x;
  }

  public JMethod createMethod(SourceInfo info, char[] name,
      JReferenceType enclosingType, JType returnType, boolean isAbstract,
      boolean isStatic, boolean isFinal, boolean isPrivate, boolean isNative) {
    assert (name != null);
    assert (returnType != null);
    assert (!isAbstract || !isNative);

    JMethod x;
    String sname = String.valueOf(name);
    if (isNative) {
      x = new JsniMethod(this, info, sname, enclosingType, returnType,
          isStatic, isFinal, isPrivate);
    } else {
      x = new JMethod(this, info, sname, enclosingType, returnType, isAbstract,
          isStatic, isFinal, isPrivate);
    }

    if (sname.equals(FindDeferredBindingSitesVisitor.REBIND_MAGIC_METHOD)
        && enclosingType.getName().equals(
            FindDeferredBindingSitesVisitor.REBIND_MAGIC_CLASS)) {
      rebindCreateMethod = x;
    } else if (!isPrivate && specialTypes.contains(enclosingType)) {
      specialMethods.put(enclosingType.getShortName() + '.' + sname, x);
    }

    if (enclosingType != null) {
      enclosingType.methods.add(x);
    }

    return x;
  }

  public JParameter createParameter(SourceInfo info, char[] name, JType type,
      boolean isFinal, JMethod enclosingMethod) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethod != null);

    JParameter x = new JParameter(this, info, String.valueOf(name), type,
        isFinal, enclosingMethod);

    enclosingMethod.params.add(x);

    return x;
  }

  public JReferenceType generalizeTypes(Collection/* <JReferenceType> */types) {
    assert (types != null);
    assert (!types.isEmpty());
    Iterator/* <JReferenceType> */it = types.iterator();
    JReferenceType curType = (JReferenceType) it.next();
    while (it.hasNext()) {
      curType = generalizeTypes(curType, (JReferenceType) it.next());
    }
    return curType;
  }

  public List/* <JArrayType> */getAllArrayTypes() {
    return allArrayTypes;
  }

  public List/* <JReferenceType> */getDeclaredTypes() {
    return allTypes;
  }

  public JThisRef getExprThisRef(SourceInfo info, JClassType enclosingType) {
    return new JThisRef(this, info, enclosingType);
  }

  public JReferenceType getFromTypeMap(String qualifiedBinaryOrSourceName) {
    String srcTypeName = qualifiedBinaryOrSourceName.replace('$', '.');
    return (JReferenceType) typeNameMap.get(srcTypeName);
  }

  public List/* <JsonObject> */getJsonTypeTable() {
    return jsonTypeTable;
  }

  public JAbsentArrayDimension getLiteralAbsentArrayDimension() {
    return literalAbsentArrayDim;
  }

  public JBooleanLiteral getLiteralBoolean(boolean z) {
    return z ? literalTrue : literalFalse;
  }

  public JCharLiteral getLiteralChar(char c) {
    // could be interned
    return new JCharLiteral(this, c);
  }

  public JClassLiteral getLiteralClass(JType type) {
    // could be interned
    return new JClassLiteral(this, type);
  }

  public JClassSeed getLiteralClassSeed(JClassType type) {
    // could be interned
    return new JClassSeed(this, type);
  }

  public JDoubleLiteral getLiteralDouble(double d) {
    // could be interned
    return new JDoubleLiteral(this, d);
  }

  public JFloatLiteral getLiteralFloat(float f) {
    // could be interned
    return new JFloatLiteral(this, f);
  }

  public JIntLiteral getLiteralInt(int i) {
    switch (i) {
      case -1:
        return literalIntNegOne;
      case 0:
        return literalIntZero;
      case 1:
        return literalIntOne;
      default:
        // could be interned
        return new JIntLiteral(this, i);
    }
  }

  public JLongLiteral getLiteralLong(long l) {
    return new JLongLiteral(this, l);
  }

  public JNullLiteral getLiteralNull() {
    return literalNull;
  }

  public JStringLiteral getLiteralString(char[] s) {
    // should conslidate so we can build a string table in output code later?
    return new JStringLiteral(this, String.valueOf(s));
  }

  public JStringLiteral getLiteralString(String s) {
    // should conslidate so we can build a string table in output code later?
    return new JStringLiteral(this, s);
  }

  public JField getNullField() {
    if (nullField == null) {
      nullField = new JField(this, null, "nullField", null, typeNull, false,
          true, true);
    }
    return nullField;
  }

  public JMethod getNullMethod() {
    if (nullMethod == null) {
      nullMethod = new JsniMethod(this, null, "nullMethod", null, typeNull,
          false, true, true);
    }
    return nullMethod;
  }

  public int getQueryId(JReferenceType elementType) {
    Integer integer = (Integer) queryIds.get(elementType);
    if (integer == null) {
      return 0;
    }

    return integer.intValue();
  }

  public JMethod getRebindCreateMethod() {
    return rebindCreateMethod;
  }

  public JClassType getSpecialArray() {
    return typeSpecialArray;
  }

  public JClassType getSpecialCast() {
    return typeSpecialCast;
  }

  public JClassType getSpecialExceptions() {
    return typeSpecialExceptions;
  }

  public JField getSpecialField(String string) {
    return (JField) specialFields.get(string);
  }

  public JClassType getSpecialJavaScriptObject() {
    return typeSpecialJavaScriptObject;
  }

  public JMethod getSpecialMethod(String string) {
    return (JMethod) specialMethods.get(string);
  }

  public JMethod getStaticImpl(JMethod method) {
    return (JMethod) instanceToStaticMap.get(method);
  }

  public JArrayType getTypeArray(JType leafType, int dimensions) {
    HashMap/* <JType, JArrayType> */typeToArrayType;

    // Create typeToArrayType maps for index slots that don't exist yet.
    //
    for (int i = this.dimensions.size(); i < dimensions; ++i) {
      typeToArrayType = new HashMap();
      this.dimensions.add(typeToArrayType);
    }

    // Get the map for array having this number of dimensions (biased by one
    // since we don't store non-arrays in there -- thus index 0 => 1 dim).
    //
    typeToArrayType = (HashMap) this.dimensions.get(dimensions - 1);

    JArrayType arrayType = (JArrayType) typeToArrayType.get(leafType);
    if (arrayType == null) {
      arrayType = new JArrayType(this, leafType, dimensions);
      arrayType.extnds = typeJavaLangObject;
      allArrayTypes.add(arrayType);

      /*
       * TODO(later): should we setup the various array types as an inheritance
       * heirarchy? Currently we're just doing all the heavy lifting in
       * JTypeOracle. If we tried to setup inheritance, we'd have to recompute
       * JTypeOracle if anything changed, so maybe this is better.
       */
      typeToArrayType.put(leafType, arrayType);
    }

    return arrayType;
  }

  public int getTypeId(JClassType classType) {
    Integer integer = (Integer) typeIdMap.get(classType);
    if (integer == null) {
      return 0;
    }

    return integer.intValue();
  }

  public JClassType getTypeJavaLangClass() {
    return typeClass;
  }

  public JClassType getTypeJavaLangObject() {
    return typeJavaLangObject;
  }

  public JClassType getTypeJavaLangString() {
    return typeString;
  }

  public JNullType getTypeNull() {
    return typeNull;
  }

  public JPrimitiveType getTypePrimitiveBoolean() {
    return typeBoolean;
  }

  public JPrimitiveType getTypePrimitiveByte() {
    return typeByte;
  }

  public JPrimitiveType getTypePrimitiveChar() {
    return typeChar;
  }

  public JPrimitiveType getTypePrimitiveDouble() {
    return typeDouble;
  }

  public JPrimitiveType getTypePrimitiveFloat() {
    return typeFloat;
  }

  public JPrimitiveType getTypePrimitiveInt() {
    return typeInt;
  }

  public JPrimitiveType getTypePrimitiveLong() {
    return typeLong;
  }

  public JPrimitiveType getTypePrimitiveShort() {
    return typeShort;
  }

  public JType getTypeVoid() {
    return typeVoid;
  }

  public void initTypeInfo(List/* <JClassType> */classes,
      List/* <JsonObject> */jsonObjects) {
    for (int i = 0, c = classes.size(); i < c; ++i) {
      typeIdMap.put(classes.get(i), new Integer(i));
    }
    this.jsonTypeTable = jsonObjects;
  }

  public boolean isJavaScriptObject(JType type) {
    if (type instanceof JClassType) {
      return typeOracle.canTriviallyCast((JClassType) type,
          typeSpecialJavaScriptObject);
    }
    return false;
  }

  public boolean isStaticImpl(JMethod method) {
    return staticToInstanceMap.containsKey(method);
  }

  public void putIntoTypeMap(String qualifiedBinaryName, JReferenceType type) {
    // Make it into a source type name.
    //
    String srcTypeName = qualifiedBinaryName.replace('$', '.');
    typeNameMap.put(srcTypeName, type);
  }

  public void putStaticImpl(JMethod method, JMethod staticImpl) {
    instanceToStaticMap.put(method, staticImpl);
    staticToInstanceMap.put(staticImpl, method);
  }

  public JClassType rebind(JType type) {
    JType result = type;
    // Rebinds are always on a source type name.
    String reqType = type.getName().replace('$', '.');
    String reboundClassName;
    try {
      reboundClassName = rebindOracle.rebind(logger, reqType);
    } catch (UnableToCompleteException e) {
      // The fact that we already compute every rebind permutation before
      // compiling should prevent this case from ever happening in real life.
      //
      throw new IllegalStateException("Unexpected failure to rebind '"
          + reqType + "'");
    }
    if (reboundClassName != null) {
      result = getFromTypeMap(reboundClassName);
    }
    assert (result != null);
    assert (result instanceof JClassType);
    return (JClassType) result;
  }

  public void recordQueryIds(Map/* <JReferenceType, Integer> */queryIds) {
    this.queryIds = queryIds;
  }

  /**
   * If <code>method</code> is a static impl method, returns the instance
   * method that <code>method</code> is the implementation of. Otherwise,
   * returns <code>null</code>.
   */
  public JMethod staticImplFor(JMethod method) {
    return (JMethod) staticToInstanceMap.get(method);
  }

  public JReferenceType strongerType(JReferenceType type1, JReferenceType type2) {
    if (type1 == type2) {
      return type1;
    }

    if (typeOracle.canTriviallyCast(type1, type2)) {
      return type1;
    }

    if (typeOracle.canTriviallyCast(type2, type1)) {
      return type2;
    }

    // canot determine a strong type, just return the first one (this makes two
    // "unrelated" interfaces work correctly in TypeTightener
    return type1;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(entryMethods);
      visitor.accept(allTypes);
    }
    visitor.endVisit(this, ctx);
  }

  JReferenceType generalizeTypes(JReferenceType type1, JReferenceType type2) {
    if (type1 == type2) {
      return type1;
    }

    int classify1 = classifyType(type1);
    int classify2 = classifyType(type2);

    if (classify1 == IS_NULL) {
      return type2;
    }

    if (classify2 == IS_NULL) {
      return type1;
    }

    if (classify1 == classify2) {

      // same basic kind of type
      if (classify1 == IS_INTERFACE) {

        if (typeOracle.canTriviallyCast(type1, type2)) {
          return type2;
        }

        if (typeOracle.canTriviallyCast(type2, type1)) {
          return type1;
        }

        // unrelated
        return typeJavaLangObject;

      } else if (classify1 == IS_ARRAY) {

        JArrayType aType1 = (JArrayType) type1;
        JArrayType aType2 = (JArrayType) type2;
        int dims1 = aType1.getDims();
        int dims2 = aType2.getDims();

        int minDims = Math.min(dims1, dims2);
        /*
         * At a bare minimum, any two arrays generalize to an Object array with
         * one less dim than the lesser of the two; that is, int[][][][] and
         * String[][][] generalize to Object[][]. If minDims is 1, then they
         * just generalize to Object.
         */
        JReferenceType minimalGeneralType;
        if (minDims > 1) {
          minimalGeneralType = getTypeArray(typeJavaLangObject, minDims - 1);
        } else {
          minimalGeneralType = typeJavaLangObject;
        }

        if (dims1 == dims2) {

          // Try to generalize by leaf types
          JType leafType1 = aType1.getLeafType();
          JType leafType2 = aType2.getLeafType();

          if (!(leafType1 instanceof JReferenceType)
              || !(leafType2 instanceof JReferenceType)) {
            return minimalGeneralType;
          }

          /*
           * Both are reference types; the result is the generalization of the
           * leaf types combined with the number of dims; that is, Foo[] and
           * Bar[] generalize to X[] where X is the generalization of Foo and
           * Bar.
           */
          JReferenceType leafRefType1 = (JReferenceType) leafType1;
          JReferenceType leafRefType2 = (JReferenceType) leafType2;
          JReferenceType leafGeneralization = generalizeTypes(leafRefType1,
              leafRefType2);
          return getTypeArray(leafGeneralization, dims1);

        } else {

          // Conflicting number of dims

          // int[][] and Object[] generalize to Object[]
          JArrayType lesser = dims1 < dims2 ? aType1 : aType2;
          if (lesser.getLeafType() == typeJavaLangObject) {
            return lesser;
          }

          // Totally unrelated
          return minimalGeneralType;
        }

      } else {

        assert (classify1 == IS_CLASS);

        /*
         * see how far each type is from object; walk the one who's farther up
         * until they're even; then walk them up together until they meet (worst
         * case at Object)
         */
        int distance1 = countSuperTypes(type1);
        int distance2 = countSuperTypes(type2);
        for (; distance1 > distance2; --distance1) {
          type1 = type1.extnds;
        }

        for (; distance1 < distance2; --distance2) {
          type2 = type2.extnds;
        }

        while (type1 != type2) {
          type1 = type1.extnds;
          type2 = type2.extnds;
        }

        return type1;
      }
    } else {

      // different kinds of types
      int lesser = Math.min(classify1, classify2);
      int greater = Math.max(classify1, classify2);

      JReferenceType tLesser = classify1 > classify2 ? type1 : type2;
      JReferenceType tGreater = classify1 < classify2 ? type1 : type2;

      if (lesser == IS_INTERFACE && greater == IS_CLASS) {

        // just see if the class implements the interface
        if (typeOracle.canTriviallyCast(tGreater, tLesser)) {
          return tLesser;
        }

        // unrelated
        return typeJavaLangObject;

      } else {

        // unrelated: the best commonality between an interface and array, or
        // between an array and a class is Object
        return typeJavaLangObject;
      }
    }
  }

  private int classifyType(JReferenceType type) {
    if (type instanceof JNullType) {
      return IS_NULL;
    } else if (type instanceof JInterfaceType) {
      return IS_INTERFACE;
    } else if (type instanceof JArrayType) {
      return IS_ARRAY;
    } else if (type instanceof JClassType) {
      return IS_CLASS;
    }
    throw new InternalCompilerException("Unknown reference type");
  }

  private int countSuperTypes(JReferenceType type) {
    if (type instanceof JArrayType) {
      JType leafType = ((JArrayType) type).getLeafType();
      if (leafType instanceof JReferenceType) {
        // however many steps from Foo[] -> Object[] + 1 for Object[]->Object
        return countSuperTypes((JReferenceType) leafType) + 1;
      } else {
        // primitive array types can only cast up to object
        return 1;
      }
    }
    int count = 0;
    while ((type = type.extnds) != null) {
      ++count;
    }
    return count;
  }

}
