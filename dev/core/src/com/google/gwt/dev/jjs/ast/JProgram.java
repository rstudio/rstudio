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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JField.Disposition;
import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsonObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Root for the AST representing an entire Java program.
 */
public class JProgram extends JNode {

  private static final class ArrayTypeComparator implements
      Comparator<JArrayType>, Serializable {
    public int compare(JArrayType o1, JArrayType o2) {
      int comp = o1.getDims() - o2.getDims();
      if (comp != 0) {
        return comp;
      }
      return o1.getName().compareTo(o2.getName());
    }
  }

  public static final Set<String> CODEGEN_TYPES_SET = new LinkedHashSet<String>(
      Arrays.asList(new String[] {
          "com.google.gwt.lang.Array", "com.google.gwt.lang.Cast",
          "com.google.gwt.lang.Exceptions", "com.google.gwt.lang.LongLib",
          "com.google.gwt.lang.Stats",}));

  public static final Set<String> INDEX_TYPES_SET = new LinkedHashSet<String>(
      Arrays.asList(new String[] {
          "java.lang.Object", "java.lang.String", "java.lang.Class",
          "java.lang.CharSequence", "java.lang.Comparable", "java.lang.Enum",
          "java.lang.Iterable", "java.util.Iterator",
          "com.google.gwt.core.client.GWT",
          "com.google.gwt.core.client.JavaScriptObject",
          "com.google.gwt.lang.ClassLiteralHolder",}));

  static final Map<String, Set<String>> traceMethods = new HashMap<String, Set<String>>();

  private static final Comparator<JArrayType> ARRAYTYPE_COMPARATOR = new ArrayTypeComparator();

  private static final int IS_ARRAY = 2;

  private static final int IS_CLASS = 3;

  private static final int IS_INTERFACE = 1;

  private static final int IS_NULL = 0;

  static {
    INDEX_TYPES_SET.addAll(CODEGEN_TYPES_SET);

    /*
     * The format to trace methods is a colon-separated list of
     * "className.methodName", such as "Hello.onModuleLoad:Foo.bar". You can
     * fully-qualify a class to disambiguate classes, and you can also append
     * the JSNI signature of the method to disambiguate overloads, ala
     * "Foo.bar(IZ)".
     */
    String toTrace = System.getProperty("gwt.jjs.traceMethods");
    if (toTrace != null) {
      String[] split = toTrace.split(":");
      for (String str : split) {
        int pos = str.lastIndexOf('.');
        if (pos > 0) {
          String className = str.substring(0, pos);
          String methodName = str.substring(pos + 1);
          Set<String> set = traceMethods.get(className);
          if (set == null) {
            set = new HashSet<String>();
            traceMethods.put(className, set);
          }
          set.add(methodName);
        }
      }
    }
  }

  public static String getJsniSig(JMethod method) {
    StringBuffer sb = new StringBuffer();
    sb.append(method.getName());
    sb.append("(");
    for (int i = 0; i < method.getOriginalParamTypes().size(); ++i) {
      JType type = method.getOriginalParamTypes().get(i);
      sb.append(type.getJsniSignatureName());
    }
    sb.append(")");
    return sb.toString();
  }

  public static boolean isClinit(JMethod method) {
    JReferenceType enclosingType = method.getEnclosingType();
    if ((enclosingType != null) && (method == enclosingType.methods.get(0))) {
      assert (method.getName().equals("$clinit"));
      return true;
    } else {
      return false;
    }
  }

  public static boolean isTracingEnabled() {
    return traceMethods.size() > 0;
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

  public final List<JClassType> codeGenTypes = new ArrayList<JClassType>();

  public final List<JMethod> entryMethods = new ArrayList<JMethod>();

  public final Map<String, HasEnclosingType> jsniMap = new HashMap<String, HasEnclosingType>();

  public final JTypeOracle typeOracle = new JTypeOracle(this);

  /**
   * Sorted to avoid nondeterministic iteration.
   */
  private final Set<JArrayType> allArrayTypes = new TreeSet<JArrayType>(
      ARRAYTYPE_COMPARATOR);

  private final List<JReferenceType> allTypes = new ArrayList<JReferenceType>();

  private final Map<JType, JClassLiteral> classLiterals = new IdentityHashMap<JType, JClassLiteral>();

  /**
   * Each entry is a HashMap(JType => JArrayType), arranged such that the number
   * of dimensions is that index (plus one) at which the JArrayTypes having that
   * number of dimensions resides.
   */
  private final ArrayList<HashMap<JType, JArrayType>> dimensions = new ArrayList<HashMap<JType, JArrayType>>();

  private boolean enableSourceInfoDescendants;

  private final Map<String, JField> indexedFields = new HashMap<String, JField>();

  private final Map<String, JMethod> indexedMethods = new HashMap<String, JMethod>();

  private final Map<String, JReferenceType> indexedTypes = new HashMap<String, JReferenceType>();

  private final Map<JMethod, JMethod> instanceToStaticMap = new IdentityHashMap<JMethod, JMethod>();

  private List<JsonObject> jsonTypeTable;

  private final JAbsentArrayDimension literalAbsentArrayDim = new JAbsentArrayDimension(
      this, createSourceInfoSynthetic("Absent array dimension"));

  private final JBooleanLiteral literalFalse = new JBooleanLiteral(this,
      createSourceInfoSynthetic("false literal"), false);

  private final JIntLiteral literalIntNegOne = new JIntLiteral(this,
      createSourceInfoSynthetic("-1 literal"), -1);

  private final JIntLiteral literalIntOne = new JIntLiteral(this,
      createSourceInfoSynthetic("1 literal"), 1);

  private final JIntLiteral literalIntZero = new JIntLiteral(this,
      createSourceInfoSynthetic("0 literal"), 0);

  private final JNullLiteral literalNull = new JNullLiteral(this,
      createSourceInfoSynthetic("null literal"));

  private final JBooleanLiteral literalTrue = new JBooleanLiteral(this,
      createSourceInfoSynthetic("true literal"), true);

  private JField nullField;

  private JMethod nullMethod;

  /**
   * Turned on once optimizations begin.
   */
  private boolean optimizationsStarted = false;

  private Map<JReferenceType, Integer> queryIds;

  private final Map<JMethod, JMethod> staticToInstanceMap = new IdentityHashMap<JMethod, JMethod>();

  private final JPrimitiveType typeBoolean = new JPrimitiveType(this,
      "boolean", "Z", "java.lang.Boolean", literalFalse);

  private final JPrimitiveType typeByte = new JPrimitiveType(this, "byte", "B",
      "java.lang.Byte", literalIntZero);

  private final JPrimitiveType typeChar = new JPrimitiveType(this, "char", "C",
      "java.lang.Character", getLiteralChar((char) 0));

  private JClassType typeClass;

  private final JPrimitiveType typeDouble = new JPrimitiveType(this, "double",
      "D", "java.lang.Double", getLiteralDouble(0));

  private final JPrimitiveType typeFloat = new JPrimitiveType(this, "float",
      "F", "java.lang.Float", getLiteralFloat(0));

  private Map<JClassType, Integer> typeIdMap = new HashMap<JClassType, Integer>();

  private final JPrimitiveType typeInt = new JPrimitiveType(this, "int", "I",
      "java.lang.Integer", literalIntZero);

  private JClassType typeJavaLangEnum;

  private JClassType typeJavaLangObject;

  private final JPrimitiveType typeLong = new JPrimitiveType(this, "long", "J",
      "java.lang.Long", getLiteralLong(0));

  private final Map<String, JReferenceType> typeNameMap = new HashMap<String, JReferenceType>();

  private final JNullType typeNull = new JNullType(this,
      createSourceInfoSynthetic("null type"));

  private final JPrimitiveType typeShort = new JPrimitiveType(this, "short",
      "S", "java.lang.Short", literalIntZero);

  private JClassType typeSpecialClassLiteralHolder;

  private JClassType typeSpecialJavaScriptObject;

  private JClassType typeString;

  private final JPrimitiveType typeVoid = new JPrimitiveType(this, "void", "V",
      "java.lang.Void", null);

  public JProgram() {
    super(null, SourceInfoJava.INTRINSIC.makeChild("Top-level program"));
  }

  public void addEntryMethod(JMethod entryPoint) {
    if (!entryMethods.contains(entryPoint)) {
      entryMethods.add(entryPoint);
    }
  }

  /**
   * Record the start of optimizations, which disables certain problematic
   * constructions. In particular, new class literals cannot be created once
   * optimization starts.
   */
  public void beginOptimizations() {
    optimizationsStarted = true;
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

    if (CODEGEN_TYPES_SET.contains(sname)) {
      codeGenTypes.add(x);
    }
    if (INDEX_TYPES_SET.contains(sname)) {
      indexedTypes.put(x.getShortName(), x);
      if (sname.equals("java.lang.Object")) {
        typeJavaLangObject = x;
      } else if (sname.equals("java.lang.String")) {
        typeString = x;
      } else if (sname.equals("java.lang.Enum")) {
        typeJavaLangEnum = x;
      } else if (sname.equals("java.lang.Class")) {
        typeClass = x;
      } else if (sname.equals("com.google.gwt.core.client.JavaScriptObject")) {
        typeSpecialJavaScriptObject = x;
      } else if (sname.equals("com.google.gwt.lang.ClassLiteralHolder")) {
        typeSpecialClassLiteralHolder = x;
      }
    }

    return x;
  }

  public JEnumType createEnum(SourceInfo info, char[][] name) {
    String sname = dotify(name);
    JEnumType x = new JEnumType(this, info, sname);

    allTypes.add(x);
    putIntoTypeMap(sname, x);

    return x;
  }

  public JField createEnumField(SourceInfo info, char[] name,
      JEnumType enclosingType, JClassType type, int ordinal) {
    assert (name != null);
    assert (type != null);
    assert (ordinal >= 0);

    String sname = String.valueOf(name);
    JEnumField x = new JEnumField(this, info, sname, ordinal, enclosingType,
        type);
    List<JEnumField> enumList = enclosingType.enumList;
    while (ordinal >= enumList.size()) {
      enumList.add(null);
    }

    enumList.set(ordinal, x);

    enclosingType.fields.add(x);
    return x;
  }

  public JField createField(SourceInfo info, char[] name,
      JReferenceType enclosingType, JType type, boolean isStatic,
      Disposition disposition) {
    assert (name != null);
    assert (enclosingType != null);
    assert (type != null);

    String sname = String.valueOf(name);
    JField x = new JField(this, info, sname, enclosingType, type, isStatic,
        disposition);

    if (indexedTypes.containsValue(enclosingType)) {
      indexedFields.put(enclosingType.getShortName() + '.' + sname, x);
    }

    enclosingType.fields.add(x);
    return x;
  }

  public JInterfaceType createInterface(SourceInfo info, char[][] name) {
    String sname = dotify(name);
    JInterfaceType x = new JInterfaceType(this, info, sname);

    allTypes.add(x);
    putIntoTypeMap(sname, x);

    if (INDEX_TYPES_SET.contains(sname)) {
      indexedTypes.put(x.getShortName(), x);
    }

    return x;
  }

  public JLocal createLocal(SourceInfo info, char[] name, JType type,
      boolean isFinal, JMethodBody enclosingMethodBody) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethodBody != null);

    JLocal x = new JLocal(this, info, String.valueOf(name), type, isFinal,
        enclosingMethodBody);

    enclosingMethodBody.locals.add(x);
    return x;
  }

  public JMethod createMethod(SourceInfo info, char[] name,
      JReferenceType enclosingType, JType returnType, boolean isAbstract,
      boolean isStatic, boolean isFinal, boolean isPrivate, boolean isNative) {
    assert (name != null);
    assert (returnType != null);
    assert (!isAbstract || !isNative);

    String sname = String.valueOf(name);
    JMethod x = new JMethod(this, info, sname, enclosingType, returnType,
        isAbstract, isStatic, isFinal, isPrivate);
    if (isNative) {
      x.setBody(new JsniMethodBody(this, info));
    } else if (!isAbstract) {
      x.setBody(new JMethodBody(this, info));
    }

    if (!isPrivate && indexedTypes.containsValue(enclosingType)) {
      indexedMethods.put(enclosingType.getShortName() + '.' + sname, x);
    }

    if (enclosingType != null) {
      enclosingType.methods.add(x);
    }

    return x;
  }

  public JParameter createParameter(SourceInfo info, char[] name, JType type,
      boolean isFinal, boolean isThis, JMethod enclosingMethod) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethod != null);

    JParameter x = new JParameter(this, info, String.valueOf(name), type,
        isFinal, isThis, enclosingMethod);

    enclosingMethod.params.add(x);
    return x;
  }

  /**
   * Create a SourceInfo object when the source is derived from a physical
   * location.
   */
  public SourceInfo createSourceInfo(int startPos, int endPos, int startLine,
      String fileName) {
    return new SourceInfoJava(startPos, endPos, startLine, fileName,
        enableSourceInfoDescendants);
  }

  /**
   * Create a SourceInfo object when the source is derived from a physical
   * location.
   */
  public SourceInfo createSourceInfo(int startLine, String fileName) {
    return new SourceInfoJava(-1, -1, startLine, fileName,
        enableSourceInfoDescendants);
  }

  /**
   * Create a SourceInfo object when the source is created by the compiler
   * itself.
   */
  public SourceInfo createSourceInfoSynthetic(String description) {
    String caller = enableSourceInfoDescendants ? SourceInfoJava.findCaller()
        : "Unknown caller";
    return createSourceInfo(0, caller).makeChild(description);
  }

  public JReferenceType generalizeTypes(
      Collection<? extends JReferenceType> types) {
    assert (types != null);
    assert (!types.isEmpty());
    Iterator<? extends JReferenceType> it = types.iterator();
    JReferenceType curType = it.next();
    while (it.hasNext()) {
      curType = generalizeTypes(curType, it.next());
    }
    return curType;
  }

  /**
   * Returns a sorted set of array types, so the returned set can be iterated
   * over without introducing nondeterminism.
   */
  public Set<JArrayType> getAllArrayTypes() {
    return allArrayTypes;
  }

  public List<JReferenceType> getDeclaredTypes() {
    return allTypes;
  }

  public JThisRef getExprThisRef(SourceInfo info, JClassType enclosingType) {
    return new JThisRef(this, info, enclosingType);
  }

  public JReferenceType getFromTypeMap(String qualifiedBinaryOrSourceName) {
    String srcTypeName = qualifiedBinaryOrSourceName.replace('$', '.');
    return typeNameMap.get(srcTypeName);
  }

  public JField getIndexedField(String string) {
    JField field = indexedFields.get(string);
    if (field == null) {
      throw new InternalCompilerException("Unable to locate index field: "
          + string);
    }
    return field;
  }

  public JMethod getIndexedMethod(String string) {
    JMethod method = indexedMethods.get(string);
    if (method == null) {
      throw new InternalCompilerException("Unable to locate index method: "
          + string);
    }
    return method;
  }

  public JReferenceType getIndexedType(String string) {
    JReferenceType type = indexedTypes.get(string);
    if (type == null) {
      throw new InternalCompilerException("Unable to locate index type: "
          + string);
    }
    return type;
  }

  public JClassType getJavaScriptObject() {
    return typeSpecialJavaScriptObject;
  }

  public List<JsonObject> getJsonTypeTable() {
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
    return new JCharLiteral(this, createSourceInfoSynthetic(c + " literal"), c);
  }

  /**
   * May not be called once optimizations begin; all possible class literals
   * must be created up front.
   */
  public JClassLiteral getLiteralClass(JType type) {
    JClassLiteral classLiteral = classLiterals.get(type);
    if (classLiteral == null) {
      if (optimizationsStarted) {
        throw new InternalCompilerException(
            "New class literals cannot be created once optimizations have started; type '"
                + type + "'");
      }

      SourceInfo info = typeSpecialClassLiteralHolder.getSourceInfo();

      // Create the allocation expression FIRST since this may be recursive on
      // super type (this forces the super type classLit to be created first).
      JExpression alloc = JClassLiteral.computeClassObjectAllocation(this,
          info, type);

      // Create a field in the class literal holder to hold the object.
      JField field = new JField(this, info, type.getJavahSignatureName()
          + "_classLit", typeSpecialClassLiteralHolder, getTypeJavaLangClass(),
          true, Disposition.FINAL);
      typeSpecialClassLiteralHolder.fields.add(field);

      // Initialize the field.
      JFieldRef fieldRef = new JFieldRef(this, info, null, field,
          typeSpecialClassLiteralHolder);
      JDeclarationStatement decl = new JDeclarationStatement(this, info,
          fieldRef, alloc);
      JMethodBody clinitBody = (JMethodBody) typeSpecialClassLiteralHolder.methods.get(
          0).getBody();
      clinitBody.getStatements().add(decl);

      classLiteral = new JClassLiteral(this,
          createSourceInfoSynthetic("class literal for " + type.getName()),
          type, field);
      classLiterals.put(type, classLiteral);
    } else {
      // Make sure the field hasn't been pruned.
      JField field = classLiteral.getField();
      if (optimizationsStarted
          && !field.getEnclosingType().fields.contains(field)) {
        throw new InternalCompilerException(
            "Getting a class literal whose field holder has already been pruned; type '"
                + type + " '");
      }
    }
    return classLiteral;
  }

  /**
   * TODO: unreferenced; remove this and JClassSeed?
   */
  public JClassSeed getLiteralClassSeed(JClassType type) {
    // could be interned
    return new JClassSeed(this, createSourceInfoSynthetic("class seed"), type);
  }

  public JDoubleLiteral getLiteralDouble(double d) {
    // could be interned
    return new JDoubleLiteral(this, createSourceInfoSynthetic(d + " literal"),
        d);
  }

  public JFloatLiteral getLiteralFloat(float f) {
    // could be interned
    return new JFloatLiteral(this, createSourceInfoSynthetic(f + " literal"), f);
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
        return new JIntLiteral(this, createSourceInfoSynthetic(i + " literal"),
            i);
    }
  }

  public JLongLiteral getLiteralLong(long l) {
    return new JLongLiteral(this, createSourceInfoSynthetic(l + " literal"), l);
  }

  public JNullLiteral getLiteralNull() {
    return literalNull;
  }

  public JStringLiteral getLiteralString(SourceInfo sourceInfo, char[] s) {
    // should consolidate so we can build a string table in output code later?
    return new JStringLiteral(this, sourceInfo, String.valueOf(s));
  }

  public JStringLiteral getLiteralString(SourceInfo sourceInfo, String s) {
    // should consolidate so we can build a string table in output code later?
    return new JStringLiteral(this, sourceInfo, s);
  }

  public JField getNullField() {
    if (nullField == null) {
      nullField = new JField(this, createSourceInfoSynthetic("Null field"),
          "nullField", null, typeNull, false, Disposition.FINAL);
    }
    return nullField;
  }

  public JMethod getNullMethod() {
    if (nullMethod == null) {
      nullMethod = new JMethod(this, createSourceInfoSynthetic("Null method"),
          "nullMethod", null, typeNull, false, false, true, true);
    }
    return nullMethod;
  }

  public int getQueryId(JReferenceType elementType) {
    Integer integer = queryIds.get(elementType);
    if (integer == null) {
      return 0;
    }

    return integer.intValue();
  }

  public JMethod getStaticImpl(JMethod method) {
    return instanceToStaticMap.get(method);
  }

  public JArrayType getTypeArray(JType leafType, int dimensions) {
    HashMap<JType, JArrayType> typeToArrayType;

    // Create typeToArrayType maps for index slots that don't exist yet.
    //
    for (int i = this.dimensions.size(); i < dimensions; ++i) {
      typeToArrayType = new HashMap<JType, JArrayType>();
      this.dimensions.add(typeToArrayType);
    }

    // Get the map for array having this number of dimensions (biased by one
    // since we don't store non-arrays in there -- thus index 0 => 1 dim).
    //
    typeToArrayType = this.dimensions.get(dimensions - 1);

    JArrayType arrayType = typeToArrayType.get(leafType);
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

  public JClassType getTypeClassLiteralHolder() {
    return typeSpecialClassLiteralHolder;
  }

  public int getTypeId(JClassType classType) {
    Integer integer = typeIdMap.get(classType);
    if (integer == null) {
      return 0;
    }

    return integer.intValue();
  }

  public JClassType getTypeJavaLangClass() {
    return typeClass;
  }

  public JClassType getTypeJavaLangEnum() {
    return typeJavaLangEnum;
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

  public void initTypeInfo(List<JClassType> classes,
      List<JsonObject> jsonObjects) {
    for (int i = 0, c = classes.size(); i < c; ++i) {
      typeIdMap.put(classes.get(i), new Integer(i));
    }
    this.jsonTypeTable = jsonObjects;
  }

  public boolean isJavaScriptObject(JType type) {
    if (type instanceof JClassType && typeSpecialJavaScriptObject != null) {
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
    if (method.isTrace()) {
      staticImpl.setTrace();
    }
  }

  public void recordQueryIds(Map<JReferenceType, Integer> queryIds) {
    this.queryIds = queryIds;
  }

  /**
   * Controls whether or not SourceInfo nodes created via the JProgram will
   * record descendant information. Enabling this feature will collect extra
   * data during the compilation cycle, but at a cost of memory and object
   * allocations.
   */
  public void setEnableSourceInfoDescendants(boolean enable) {
    enableSourceInfoDescendants = enable;
  }

  /**
   * If <code>method</code> is a static impl method, returns the instance method
   * that <code>method</code> is the implementation of. Otherwise, returns
   * <code>null</code>.
   */
  public JMethod staticImplFor(JMethod method) {
    return staticToInstanceMap.get(method);
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

    // cannot determine a strong type, just return the first one (this makes two
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
