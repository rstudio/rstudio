// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jdt.FindDeferredBindingSitesVisitor;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;
import com.google.gwt.dev.jjs.impl.InternalCompilerException;

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

  public JProgram(TreeLogger logger, RebindOracle rebindOracle) {
    super(null);
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
  public JExpressionStatement createAssignmentStmt(JExpression lhs,
      JExpression rhs) {
    JBinaryOperation assign = new JBinaryOperation(this, lhs.getType(),
      JBinaryOperator.ASG, lhs, rhs);
    return new JExpressionStatement(this, assign);
  }

  public JClassType createClass(char[][] name, boolean isAbstract,
      boolean isFinal) {
    String sname = dotify(name);
    JClassType x = new JClassType(this, sname, isAbstract, isFinal);

    allTypes.add(x);
    putIntoTypeMap(sname, x);

    if (sname.equals("java.lang.Object")) {
      fTypeJavaLangObject = x;
      specialTypes.add(x);
    } else if (sname.equals("java.lang.String")) {
      fTypeString = x;
    } else if (sname.equals("java.lang.Class")) {
      fTypeClass = x;
    } else if (sname.equals("com.google.gwt.core.client.JavaScriptObject")) {
      fTypeSpecialJavaScriptObject = x;
    } else if (sname.equals("com.google.gwt.lang.Array")) {
      fTypeSpecialArray = x;
      specialTypes.add(x);
    } else if (sname.equals("com.google.gwt.lang.Cast")) {
      fTypeSpecialCast = x;
      specialTypes.add(x);
    } else if (sname.equals("com.google.gwt.lang.Exceptions")) {
      fTypeSpecialExceptions = x;
      specialTypes.add(x);
    }

    return x;
  }

  public JField createField(char[] name, JReferenceType enclosingType,
      JType type, boolean isStatic, boolean isFinal, boolean hasInitializer) {
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
    JField x = new JField(this, sname, enclosingType, type, isStatic, isFinal,
      hasInitializer);

    if (isSpecialField) {
      specialFields.put(enclosingType.getShortName() + '.' + sname, x);
    }
    
    enclosingType.fields.add(x);

    return x;
  }

  public JInterfaceType createInterface(char[][] name) {
    String sname = dotify(name);
    JInterfaceType x = new JInterfaceType(this, sname);

    allTypes.add(x);
    putIntoTypeMap(sname, x);

    return x;
  }

  public JLocal createLocal(char[] name, JType type, boolean isFinal,
      JMethod enclosingMethod) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethod != null);

    JLocal x = new JLocal(this, String.valueOf(name), type, isFinal,
      enclosingMethod);

    enclosingMethod.locals.add(x);

    return x;
  }

  public JMethod createMethod(char[] name, JReferenceType enclosingType,
      JType returnType, boolean isAbstract, boolean isStatic, boolean isFinal,
      boolean isPrivate, boolean isNative) {
    assert (name != null);
    assert (returnType != null);
    assert (!isAbstract || !isNative);

    JMethod x;
    String sname = String.valueOf(name);
    if (isNative) {
      x = new JsniMethod(this, sname, enclosingType, returnType, isStatic,
        isFinal, isPrivate);
    } else {
      x = new JMethod(this, sname, enclosingType, returnType, isAbstract,
        isStatic, isFinal, isPrivate);
    }

    if (sname.equals(FindDeferredBindingSitesVisitor.REBIND_MAGIC_METHOD)
      && enclosingType.getName().equals(
        FindDeferredBindingSitesVisitor.REBIND_MAGIC_CLASS)) {
      fRebindCreateMethod = x;
    } else if (!isPrivate && specialTypes.contains(enclosingType)) {
      specialMethods.put(enclosingType.getShortName() + '.' + sname, x);
    }

    if (enclosingType != null) {
      enclosingType.methods.add(x);
    }
    
    return x;
  }

  public JParameter createParameter(char[] name, JType type, boolean isFinal,
      JMethod enclosingMethod) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethod != null);

    JParameter x = new JParameter(this, String.valueOf(name), type, isFinal,
      enclosingMethod);

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

  public JThisRef getExpressionThisRef(JClassType enclosingType) {
    return new JThisRef(this, enclosingType);
  }

  public JReferenceType getFromTypeMap(String qualifiedBinaryOrSourceName) {
    String srcTypeName = qualifiedBinaryOrSourceName.replace('$', '.');
    return (JReferenceType) fTypeNameMap.get(srcTypeName);
  }

  public List/* <JsonObject> */getJsonTypeTable() {
    return jsonTypeTable;
  }

  public JAbsentArrayDimension getLiteralAbsentArrayDimension() {
    return fLiteralAbsentArrayDim;
  }

  public JBooleanLiteral getLiteralBoolean(boolean z) {
    return z ? fLiteralTrue : fLiteralFalse;
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
        return fLiteralIntNegOne;
      case 0:
        return fLiteralIntZero;
      case 1:
        return fLiteralIntOne;
      default:
        // could be interned
        return new JIntLiteral(this, i);
    }
  }

  public JLongLiteral getLiteralLong(long l) {
    return new JLongLiteral(this, l);
  }

  public JNullLiteral getLiteralNull() {
    return fLiteralNull;
  }

  public JStringLiteral getLiteralString(char[] s) {
    // should conslidate so we can build a string table in output code later?
    return new JStringLiteral(this, String.valueOf(s));
  }

  public JField getNullField() {
    if (fNullField == null) {
      fNullField = new JField(this, "nullField", null, fTypeNull, false, true, true);
    }
    return fNullField;
  }

  public JMethod getNullMethod() {
    if (fNullMethod == null) {
      fNullMethod = new JsniMethod(this, "nullMethod", null, fTypeNull, false,
        true, true);
    }
    return fNullMethod;
  }

  public int getQueryId(JReferenceType elementType) {
    Integer integer = (Integer) queryIds.get(elementType);
    if (integer == null) {
      return 0;
    }
    
    return integer.intValue();
  }

  public JMethod getRebindCreateMethod() {
    return fRebindCreateMethod;
  }

  public JClassType getSpecialArray() {
    return fTypeSpecialArray;
  }

  public JClassType getSpecialCast() {
    return fTypeSpecialCast;
  }

  public JClassType getSpecialExceptions() {
    return fTypeSpecialExceptions;
  }

  public JField getSpecialField(String string) {
    return (JField) specialFields.get(string);
  }

  public JClassType getSpecialJavaScriptObject() {
    return fTypeSpecialJavaScriptObject;
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
    for (int i = fDimensions.size(); i < dimensions; ++i) {
      typeToArrayType = new HashMap();
      fDimensions.add(typeToArrayType);
    }

    // Get the map for array having this number of dimensions (biased by one
    // since we don't store non-arrays in there -- thus index 0 => 1 dim).
    //
    typeToArrayType = (HashMap) fDimensions.get(dimensions - 1);

    JArrayType arrayType = (JArrayType) typeToArrayType.get(leafType);
    if (arrayType == null) {
      arrayType = new JArrayType(this, leafType, dimensions);
      arrayType.extnds = fTypeJavaLangObject;
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
    Integer integer = (Integer) fTypeIdMap.get(classType);
    if (integer == null) {
      return 0;
    }
    
    return integer.intValue();
  }

  public JClassType getTypeJavaLangClass() {
    return fTypeClass;
  }

  public JClassType getTypeJavaLangObject() {
    return fTypeJavaLangObject;
  }

  public JClassType getTypeJavaLangString() {
    return fTypeString;
  }

  public JNullType getTypeNull() {
    return fTypeNull;
  }

  public JPrimitiveType getTypePrimitiveBoolean() {
    return fTypeBoolean;
  }

  public JPrimitiveType getTypePrimitiveByte() {
    return fTypeByte;
  }

  public JPrimitiveType getTypePrimitiveChar() {
    return fTypeChar;
  }

  public JPrimitiveType getTypePrimitiveDouble() {
    return fTypeDouble;
  }

  public JPrimitiveType getTypePrimitiveFloat() {
    return fTypeFloat;
  }

  public JPrimitiveType getTypePrimitiveInt() {
    return fTypeInt;
  }

  public JPrimitiveType getTypePrimitiveLong() {
    return fTypeLong;
  }

  public JPrimitiveType getTypePrimitiveShort() {
    return fTypeShort;
  }

  public JType getTypeVoid() {
    return fTypeVoid;
  }

  public void initTypeInfo(List/* <JClassType> */classes,
      List/* <JsonObject> */jsonObjects) {
    for (int i = 0, c = classes.size(); i < c; ++i) {
      fTypeIdMap.put(classes.get(i), new Integer(i));
    }
    this.jsonTypeTable = jsonObjects;
  }

  public boolean isJavaScriptObject(JType type) {
    if (type instanceof JClassType) {
      return typeOracle.canTriviallyCast((JClassType) type,
        fTypeSpecialJavaScriptObject);
    }
    return false;
  }

  public boolean isStaticImpl(JMethod method) {
    return instanceToStaticMap.containsValue(method);
  }

  public void putIntoTypeMap(String qualifiedBinaryName, JReferenceType type) {
    // Make it into a source type name.
    //
    String srcTypeName = qualifiedBinaryName.replace('$', '.');
    fTypeNameMap.put(srcTypeName, type);
  }

  public void putStaticImpl(JMethod method, JMethod staticImpl) {
    instanceToStaticMap.put(method, staticImpl);
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

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      for (int i = 0; i < entryMethods.size(); ++i) {
        JMethod method = (JMethod) entryMethods.get(i);
        method.traverse(visitor);
      }
      for (int i = 0; i < allTypes.size(); ++i) {
        JReferenceType type = (JReferenceType) allTypes.get(i);
        type.traverse(visitor);
      }
    }
    visitor.endVisit(this);
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
        return fTypeJavaLangObject;

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
          minimalGeneralType = getTypeArray(fTypeJavaLangObject, minDims - 1);
        } else {
          minimalGeneralType = fTypeJavaLangObject;
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
          if (lesser.getLeafType() == fTypeJavaLangObject) {
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
        return fTypeJavaLangObject;

      } else {

        // unrelated: the best commonality between an interface and array, or
        // between an array and a class is Object
        return fTypeJavaLangObject;
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
  private final ArrayList/* <HashMap<JType, JArrayType>> */fDimensions = new ArrayList/*
                                                                                       * <HashMap<JType,
                                                                                       * JArrayType>>
                                                                                       */();

  private final JAbsentArrayDimension fLiteralAbsentArrayDim = new JAbsentArrayDimension(
    this);

  private final JBooleanLiteral fLiteralFalse = new JBooleanLiteral(this, false);

  private final JIntLiteral fLiteralIntNegOne = new JIntLiteral(this, -1);

  private final JIntLiteral fLiteralIntOne = new JIntLiteral(this, 1);

  private final JIntLiteral fLiteralIntZero = new JIntLiteral(this, 0);

  private final JNullLiteral fLiteralNull = new JNullLiteral(this);

  private final JBooleanLiteral fLiteralTrue = new JBooleanLiteral(this, true);

  private JField fNullField;

  private JMethod fNullMethod;

  private JMethod fRebindCreateMethod;

  private final JPrimitiveType fTypeBoolean = new JPrimitiveType(this,
    "boolean", "Z", fLiteralFalse);

  private final JPrimitiveType fTypeByte = new JPrimitiveType(this, "byte",
    "B", fLiteralIntZero);

  private final JPrimitiveType fTypeChar = new JPrimitiveType(this, "char",
    "C", getLiteralChar((char) 0));

  private JClassType fTypeClass;
  private final JPrimitiveType fTypeDouble = new JPrimitiveType(this, "double",
    "D", getLiteralDouble(0));
  private final JPrimitiveType fTypeFloat = new JPrimitiveType(this, "float",
    "F", getLiteralFloat(0));

  private Map/* <JClassType, Integer> */fTypeIdMap = new HashMap/*
                                                                 * <JClassType,
                                                                 * Integer>
                                                                 */();

  private final JPrimitiveType fTypeInt = new JPrimitiveType(this, "int", "I",
    fLiteralIntZero);

  private JClassType fTypeJavaLangObject;

  private final JPrimitiveType fTypeLong = new JPrimitiveType(this, "long",
    "J", getLiteralLong(0));

  private final Map/* <String, JReferenceType> */fTypeNameMap = new HashMap/*
                                                                             * <String,
                                                                             * JReferenceType>
                                                                             */();

  private final JNullType fTypeNull = new JNullType(this);

  private final JPrimitiveType fTypeShort = new JPrimitiveType(this, "short",
    "S", fLiteralIntZero);

  private JClassType fTypeSpecialArray;

  private JClassType fTypeSpecialCast;

  private JClassType fTypeSpecialExceptions;

  private JClassType fTypeSpecialJavaScriptObject;

  private JClassType fTypeString;

  private final JPrimitiveType fTypeVoid = new JPrimitiveType(this, "void",
    "V", null);

  private final Map/* <JMethod, JMethod> */instanceToStaticMap = new IdentityHashMap/*
                                                                                     * <JMethod,
                                                                                     * JMethod>
                                                                                     */();

  private List/* <JsonObject> */jsonTypeTable;

  private final TreeLogger logger;

  private Map/* <JReferenceType, Integer> */queryIds;

  private final RebindOracle rebindOracle;

  private final Map/* <String, JField> */specialFields = new HashMap/*
                                                                     * <String,
                                                                     * JField>
                                                                     */();

  private final Map/* <String, JMethod> */specialMethods = new HashMap/*
                                                                       * <String,
                                                                       * JMethod>
                                                                       */();

}
