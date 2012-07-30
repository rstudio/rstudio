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

import com.google.gwt.dev.jjs.Correlation.Literal;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.js.JsCastMap;
import com.google.gwt.dev.jjs.impl.CodeSplitter;
import com.google.gwt.dev.jjs.impl.CodeSplitter2.FragmentPartitioningResult;
import com.google.gwt.dev.util.collect.Lists;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Root for the AST representing an entire Java program.
 */
public class JProgram extends JNode {

  private static final class ArrayTypeComparator implements Comparator<JArrayType>, Serializable {
    public int compare(JArrayType o1, JArrayType o2) {
      int comp = o1.getDims() - o2.getDims();
      if (comp != 0) {
        return comp;
      }
      return o1.getName().compareTo(o2.getName());
    }
  }

  public static final Set<String> CODEGEN_TYPES_SET = new LinkedHashSet<String>(Arrays.asList(
      "com.google.gwt.lang.Array", "com.google.gwt.lang.Cast",
      "com.google.gwt.lang.CollapsedPropertyHolder", "com.google.gwt.lang.Exceptions",
      "com.google.gwt.lang.LongLib", "com.google.gwt.lang.Stats", "com.google.gwt.lang.Util"));

  /*
   * Types which are not referenced by any Java code, but are required to exist
   * after Java optimizations have run in order to be used by backend
   * code-generation. These classes and their members, are considered live
   * by ControlFlowAnalysis, at all times. Immortal types always live in the
   * initial fragment and their definitions are hoisted to appear before all
   * other types. Only static methods and fields are allowed, and no clinits
   * are run. Field initializers must be primitives, literals, or one of
   * JSO.createObject() or JSO.createArray().
   *
   * Classes are inserted into the JsAST in the order they appear in the Set.
   */
  public static final Set<String> IMMORTAL_CODEGEN_TYPES_SET = new LinkedHashSet<String>(Arrays.asList(
      "com.google.gwt.lang.SeedUtil"));

  public static final Set<String> INDEX_TYPES_SET = new LinkedHashSet<String>(Arrays.asList(
      "java.io.Serializable", "java.lang.Object", "java.lang.String", "java.lang.Class",
      "java.lang.CharSequence", "java.lang.Cloneable", "java.lang.Comparable", "java.lang.Enum",
      "java.lang.Iterable", "java.util.Iterator", "java.lang.AssertionError", "java.lang.Boolean",
      "java.lang.Byte", "java.lang.Character", "java.lang.Short", "java.lang.Integer",
      "java.lang.Long", "java.lang.Float", "java.lang.Double", "java.lang.Throwable",
      "com.google.gwt.core.client.GWT", JProgram.JAVASCRIPTOBJECT,
      "com.google.gwt.lang.ClassLiteralHolder", "com.google.gwt.core.client.RunAsyncCallback",
      "com.google.gwt.core.client.impl.AsyncFragmentLoader",
      "com.google.gwt.core.client.impl.Impl", "com.google.gwt.lang.EntryMethodHolder",
      "com.google.gwt.core.client.prefetch.RunAsyncCode"));

  public static final String JAVASCRIPTOBJECT = "com.google.gwt.core.client.JavaScriptObject";

  static final Map<String, Set<String>> traceMethods = new HashMap<String, Set<String>>();

  private static final Comparator<JArrayType> ARRAYTYPE_COMPARATOR = new ArrayTypeComparator();

  private static final int IS_ARRAY = 2;

  private static final int IS_CLASS = 3;

  private static final int IS_INTERFACE = 1;

  private static final int IS_NULL = 0;

  private static final Map<String, JPrimitiveType> primitiveTypes =
      new HashMap<String, JPrimitiveType>();

  @Deprecated
  private static final Map<String, JPrimitiveType> primitiveTypesDeprecated =
      new HashMap<String, JPrimitiveType>();

  static {
    if (System.getProperty("gwt.coverage") != null) {
      IMMORTAL_CODEGEN_TYPES_SET.add("com.google.gwt.lang.CoverageUtil");
    }
    CODEGEN_TYPES_SET.addAll(IMMORTAL_CODEGEN_TYPES_SET);
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

    primitiveTypes.put(JPrimitiveType.BOOLEAN.getName(), JPrimitiveType.BOOLEAN);
    primitiveTypes.put(JPrimitiveType.BYTE.getName(), JPrimitiveType.BYTE);
    primitiveTypes.put(JPrimitiveType.CHAR.getName(), JPrimitiveType.CHAR);
    primitiveTypes.put(JPrimitiveType.DOUBLE.getName(), JPrimitiveType.DOUBLE);
    primitiveTypes.put(JPrimitiveType.FLOAT.getName(), JPrimitiveType.FLOAT);
    primitiveTypes.put(JPrimitiveType.INT.getName(), JPrimitiveType.INT);
    primitiveTypes.put(JPrimitiveType.LONG.getName(), JPrimitiveType.LONG);
    primitiveTypes.put(JPrimitiveType.SHORT.getName(), JPrimitiveType.SHORT);
    primitiveTypes.put(JPrimitiveType.VOID.getName(), JPrimitiveType.VOID);

    primitiveTypesDeprecated.put(JPrimitiveType.BOOLEAN.getJsniSignatureName(),
        JPrimitiveType.BOOLEAN);
    primitiveTypesDeprecated.put(JPrimitiveType.BYTE.getJsniSignatureName(), JPrimitiveType.BYTE);
    primitiveTypesDeprecated.put(JPrimitiveType.CHAR.getJsniSignatureName(), JPrimitiveType.CHAR);
    primitiveTypesDeprecated.put(JPrimitiveType.DOUBLE.getJsniSignatureName(),
        JPrimitiveType.DOUBLE);
    primitiveTypesDeprecated.put(JPrimitiveType.FLOAT.getJsniSignatureName(), JPrimitiveType.FLOAT);
    primitiveTypesDeprecated.put(JPrimitiveType.INT.getJsniSignatureName(), JPrimitiveType.INT);
    primitiveTypesDeprecated.put(JPrimitiveType.LONG.getJsniSignatureName(), JPrimitiveType.LONG);
    primitiveTypesDeprecated.put(JPrimitiveType.SHORT.getJsniSignatureName(), JPrimitiveType.SHORT);
    primitiveTypesDeprecated.put(JPrimitiveType.VOID.getJsniSignatureName(), JPrimitiveType.VOID);
  }

  /**
   * Helper to create an assignment, used to initalize fields, etc.
   */
  public static JExpressionStatement createAssignmentStmt(SourceInfo info, JExpression lhs,
      JExpression rhs) {
    JBinaryOperation assign =
        new JBinaryOperation(info, lhs.getType(), JBinaryOperator.ASG, lhs, rhs);
    return assign.makeStatement();
  }

  public static JLocal createLocal(SourceInfo info, String name, JType type, boolean isFinal,
      JMethodBody enclosingMethodBody) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethodBody != null);
    JLocal x = new JLocal(info, name, type, isFinal, enclosingMethodBody);
    enclosingMethodBody.addLocal(x);
    return x;
  }

  public static JParameter createParameter(SourceInfo info, String name, JType type,
      boolean isFinal, boolean isThis, JMethod enclosingMethod) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethod != null);

    JParameter x = new JParameter(info, name, type, isFinal, isThis, enclosingMethod);

    enclosingMethod.addParam(x);
    return x;
  }

  public static List<JDeclaredType> deserializeTypes(ObjectInputStream stream) throws IOException,
      ClassNotFoundException {
    @SuppressWarnings("unchecked")
    List<JDeclaredType> types = (List<JDeclaredType>) stream.readObject();
    for (JDeclaredType type : types) {
      type.readMembers(stream);
    }
    for (JDeclaredType type : types) {
      type.readMethodBodies(stream);
    }
    return types;
  }

  public static String getJsniSig(JMethod method) {
    return getJsniSig(method, true);
  }

  public static String getJsniSig(JMethod method, boolean addReturnType) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getName());
    sb.append("(");
    for (int i = 0; i < method.getOriginalParamTypes().size(); ++i) {
      JType type = method.getOriginalParamTypes().get(i);
      sb.append(type.getJsniSignatureName());
    }
    sb.append(")");
    if (addReturnType) {
      sb.append(method.getOriginalReturnType().getJsniSignatureName());
    }
    return sb.toString();
  }

  public static boolean isClinit(JMethod method) {
    JDeclaredType enclosingType = method.getEnclosingType();
    if ((enclosingType != null) && (method == enclosingType.getMethods().get(0))) {
      assert (method.getName().equals("$clinit"));
      return true;
    } else {
      return false;
    }
  }

  public static boolean isTracingEnabled() {
    return traceMethods.size() > 0;
  }

  /**
   * The same as {@link #lastFragmentLoadingBefore(int, int...)}, except that
   * all of the parameters must be passed explicitly. The instance method should
   * be preferred whenever a JProgram instance is available.
   * 
   * @param initialSeq The initial split point sequence of the program
   * @param result The fragment partitioning result, null if it hasn't be partitioned.
   * @param numSps The number of split points in the program
   * @param firstFragment The first fragment to consider
   * @param restFragments The rest of the fragments to consider
   */
  public static int lastFragmentLoadingBefore(List<Integer> initialSeq,
      FragmentPartitioningResult result, int numSps, int firstFragment, int... restFragments) {
    int latest = firstFragment;
    for (int frag : restFragments) {
      latest = pairwiseLastFragmentLoadingBefore(initialSeq, result, numSps, latest, frag);
    }
    return latest;
  }
  
  public static int lastFragmentLoadingBefore(List<Integer> initialSeq,
      int numSps, int firstFragment, int... restFragments) {
    int latest = firstFragment;
    for (int frag : restFragments) {
      latest = pairwiseLastFragmentLoadingBefore(initialSeq, null, numSps, latest, frag);
    }
    return latest;
  }


  public static void serializeTypes(List<JDeclaredType> types, ObjectOutputStream stream)
      throws IOException {
    stream.writeObject(types);
    for (JDeclaredType type : types) {
      type.writeMembers(stream);
    }
    for (JDeclaredType type : types) {
      type.writeMethodBodies(stream);
    }
  }

  /**
   * The main logic behind {@link #lastFragmentLoadingBefore(int, int...)} and
   * {@link #lastFragmentLoadingBefore(List, int, int, int...)}.
   */
  private static int pairwiseLastFragmentLoadingBefore(List<Integer> initialSeq,
      FragmentPartitioningResult result, int numSps, int frag1, int frag2) {

    if (frag1 == frag2) {
      return frag1;
    }

    if (frag1 == 0) {
      return 0;
    }

    if (frag2 == 0) {
      return 0;
    }

    // TODO(acleung): While the logic for this is correct, the terminology used
    // in the code is not correct when fragment partitioning is on.
    // Once the new code splitter is the default. This function needs to be
    // rewritten.
    int sp1 = frag1;
    int sp2 = frag2;

    // If there were some fragment merging.
    if (result != null) {
      sp1 = result.getSplitPointFromFragment(sp1);
      sp2 = result.getSplitPointFromFragment(sp2);
    }
    
    int initPos1 = initialSeq.indexOf(sp1);
    int initPos2 = initialSeq.indexOf(sp2);
    
    // If both are in the initial sequence, then pick the earlier
    if (initPos1 >= 0 && initPos2 >= 0) {
      if (initPos1 < initPos2) {
        return frag1;
      }
      return frag2;
    }

    // If exactly one is in the initial sequence, then it's the earlier one
    if (initPos1 >= 0) {
      return frag1;
    }
    if (initPos2 >= 0) {
      return frag2;
    }

    assert (initPos1 < 0 && initPos2 < 0);
    assert (frag1 != frag2);

    if (result != null) {
      return result.getLeftoverFragmentIndex();
    } else {
      return CodeSplitter.getLeftoversFragmentNumber(numSps);
    }
  }

  public final List<JClassType> codeGenTypes = new ArrayList<JClassType>();
  public final List<JClassType> immortalCodeGenTypes = new ArrayList<JClassType>();

  public final JTypeOracle typeOracle = new JTypeOracle(this);

  /**
   * Special serialization treatment.
   */
  private transient List<JDeclaredType> allTypes = new ArrayList<JDeclaredType>();

  private final HashMap<JType, JArrayType> arrayTypes = new HashMap<JType, JArrayType>();

  private IdentityHashMap<JReferenceType, JsCastMap> castMaps;

  private Map<JType, JField> classLiteralFields;

  private final List<JMethod> entryMethods = new ArrayList<JMethod>();

  private final Map<String, JField> indexedFields = new HashMap<String, JField>();

  private final Map<String, JMethod> indexedMethods = new HashMap<String, JMethod>();

  private final Map<String, JDeclaredType> indexedTypes = new HashMap<String, JDeclaredType>();

  private final Map<JMethod, JMethod> instanceToStaticMap = new IdentityHashMap<JMethod, JMethod>();

  private Map<JReferenceType, Integer> queryIdsByType;

  /**
   * Filled in by ReplaceRunAsync, once the numbers are known.
   */
  private List<JRunAsync> runAsyncs = Lists.create();

  private List<Integer> splitPointInitialSequence = Lists.create();

  private final Map<JMethod, JMethod> staticToInstanceMap = new IdentityHashMap<JMethod, JMethod>();

  private JClassType typeClass;

  private JInterfaceType typeJavaIoSerializable;

  private JInterfaceType typeJavaLangCloneable;

  private JClassType typeJavaLangEnum;

  private JClassType typeJavaLangObject;

  private final Map<String, JDeclaredType> typeNameMap = new HashMap<String, JDeclaredType>();

  private List<JReferenceType> typesByQueryId;

  private JClassType typeSpecialClassLiteralHolder;

  private JClassType typeSpecialJavaScriptObject;

  private JClassType typeString;
  
  private FragmentPartitioningResult fragmentPartitioninResult;

  /**
   * Constructor.
   * 
   * @param correlator Controls whether or not SourceInfo nodes created via the
   *          JProgram will record descendant information. Enabling this feature
   *          will collect extra data during the compilation cycle, but at a
   *          cost of memory and object allocations.
   */
  public JProgram() {
    super(SourceOrigin.UNKNOWN);
  }

  public void addEntryMethod(JMethod entryPoint) {
    assert !entryMethods.contains(entryPoint);
    entryMethods.add(entryPoint);
  }

  public void addType(JDeclaredType type) {
    allTypes.add(type);
    String name = type.getName();
    putIntoTypeMap(name, type);

    if (CODEGEN_TYPES_SET.contains(name)) {
      codeGenTypes.add((JClassType) type);
    }

    if (IMMORTAL_CODEGEN_TYPES_SET.contains(name)) {
      immortalCodeGenTypes.add((JClassType) type);
    }
    
    if (INDEX_TYPES_SET.contains(name)) {
      indexedTypes.put(type.getShortName(), type);
      for (JMethod method : type.getMethods()) {
        if (!method.isPrivate()) {
          indexedMethods.put(type.getShortName() + '.' + method.getName(), method);
        }
      }
      for (JField field : type.getFields()) {
        indexedFields.put(type.getShortName() + '.' + field.getName(), field);
      }
      if (name.equals("java.lang.Object")) {
        typeJavaLangObject = (JClassType) type;
      } else if (name.equals("java.lang.String")) {
        typeString = (JClassType) type;
      } else if (name.equals("java.lang.Enum")) {
        typeJavaLangEnum = (JClassType) type;
      } else if (name.equals("java.lang.Class")) {
        typeClass = (JClassType) type;
      } else if (name.equals(JAVASCRIPTOBJECT)) {
        typeSpecialJavaScriptObject = (JClassType) type;
      } else if (name.equals("com.google.gwt.lang.ClassLiteralHolder")) {
        typeSpecialClassLiteralHolder = (JClassType) type;
      } else if (name.equals("java.lang.Cloneable")) {
        typeJavaLangCloneable = (JInterfaceType) type;
      } else if (name.equals("java.io.Serializable")) {
        typeJavaIoSerializable = (JInterfaceType) type;
      }
    }
  }

  /**
   * Return the least upper bound of a set of types. That is, the smallest type
   * that is a supertype of all the input types.
   */
  public JReferenceType generalizeTypes(Collection<? extends JReferenceType> types) {
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
   * Return the least upper bound of two types. That is, the smallest type that
   * is a supertype of both types.
   */
  public JReferenceType generalizeTypes(JReferenceType type1, JReferenceType type2) {
    if (type1 == type2) {
      return type1;
    }

    if (type1 instanceof JNonNullType && type2 instanceof JNonNullType) {
      // Neither can be null.
      type1 = type1.getUnderlyingType();
      type2 = type2.getUnderlyingType();
      return generalizeTypes(type1, type2).getNonNull();
    } else if (type1 instanceof JNonNullType) {
      // type2 can be null, so the result can be null
      type1 = type1.getUnderlyingType();
    } else if (type2 instanceof JNonNullType) {
      // type1 can be null, so the result can be null
      type2 = type2.getUnderlyingType();
    }
    assert !(type1 instanceof JNonNullType);
    assert !(type2 instanceof JNonNullType);

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

          if (!(leafType1 instanceof JReferenceType) || !(leafType2 instanceof JReferenceType)) {
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
          JReferenceType leafGeneralization = generalizeTypes(leafRefType1, leafRefType2);
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
        JClassType class1 = (JClassType) type1;
        JClassType class2 = (JClassType) type2;

        /*
         * see how far each type is from object; walk the one who's farther up
         * until they're even; then walk them up together until they meet (worst
         * case at Object)
         */
        int distance1 = countSuperTypes(class1);
        int distance2 = countSuperTypes(class2);
        for (; distance1 > distance2; --distance1) {
          class1 = class1.getSuperClass();
        }

        for (; distance1 < distance2; --distance2) {
          class2 = class2.getSuperClass();
        }

        while (class1 != class2) {
          class1 = class1.getSuperClass();
          class2 = class2.getSuperClass();
        }

        return class1;
      }
    } else {

      // different kinds of types
      int lesser = Math.min(classify1, classify2);
      int greater = Math.max(classify1, classify2);

      JReferenceType tLesser = classify1 < classify2 ? type1 : type2;
      JReferenceType tGreater = classify1 > classify2 ? type1 : type2;

      if (lesser == IS_INTERFACE && greater == IS_CLASS) {

        // just see if the class implements the interface
        if (typeOracle.canTriviallyCast(tGreater, tLesser)) {
          return tLesser;
        }

        // unrelated
        return typeJavaLangObject;

      } else if (greater == IS_ARRAY
          && ((tLesser == typeJavaLangCloneable) || (tLesser == typeJavaIoSerializable))) {
        return tLesser;
      } else {

        // unrelated: the best commonality between an interface and array, or
        // between an array and a class is Object
        return typeJavaLangObject;
      }
    }
  }

  /**
   * Returns a sorted list of array types, so the returned set can be iterated
   * over without introducing nondeterminism.
   */
  public List<JArrayType> getAllArrayTypes() {
    ArrayList<JArrayType> result = new ArrayList<JArrayType>(arrayTypes.values());
    Collections.sort(result, ARRAYTYPE_COMPARATOR);
    return result;
  }

  public JsCastMap getCastMap(JReferenceType referenceType) {
    // ensure jsonCastableTypeMaps has been initialized
    // it might not have been if the CastNormalizer has not been run
    if (castMaps == null) {
      initTypeInfo(null);
    }
    return castMaps.get(referenceType);
  }

  public JField getClassLiteralField(JType type) {
    return classLiteralFields.get(isJavaScriptObject(type) ? getJavaScriptObject() : type);
  }

  public String getClassLiteralName(JType type) {
    return type.getJavahSignatureName() + "_classLit";
  }

  public List<JDeclaredType> getDeclaredTypes() {
    return allTypes;
  }

  public List<JMethod> getEntryMethods() {
    return entryMethods;
  }

  public int getFragmentCount() {
    // Initial fragment is the +1.
    return runAsyncs.size() + 1;
  }
  
  public FragmentPartitioningResult getFragmentPartitioningResult() {
    return fragmentPartitioninResult;
  }

  public JDeclaredType getFromTypeMap(String qualifiedBinaryOrSourceName) {
    String srcTypeName = qualifiedBinaryOrSourceName.replace('$', '.');

    return typeNameMap.get(srcTypeName);
  }

  public JField getIndexedField(String string) {
    JField field = indexedFields.get(string);
    if (field == null) {
      throw new InternalCompilerException("Unable to locate index field: " + string);
    }
    return field;
  }

  public Collection<JField> getIndexedFields() {
    return Collections.unmodifiableCollection(indexedFields.values());
  }

  public JMethod getIndexedMethod(String string) {
    JMethod method = indexedMethods.get(string);
    if (method == null) {
      throw new InternalCompilerException("Unable to locate index method: " + string);
    }
    return method;
  }

  public Collection<JMethod> getIndexedMethods() {
    return Collections.unmodifiableCollection(indexedMethods.values());
  }

  public JDeclaredType getIndexedType(String string) {
    JDeclaredType type = indexedTypes.get(string);
    if (type == null) {
      throw new InternalCompilerException("Unable to locate index type: " + string);
    }
    return type;
  }

  public JClassType getJavaScriptObject() {
    return typeSpecialJavaScriptObject;
  }

  public JExpression getLiteralAbsentArrayDimension() {
    return JAbsentArrayDimension.INSTANCE;
  }

  public JBooleanLiteral getLiteralBoolean(boolean value) {
    return JBooleanLiteral.get(value);
  }

  public JCharLiteral getLiteralChar(char value) {
    return JCharLiteral.get(value);
  }

  public JDoubleLiteral getLiteralDouble(double d) {
    return JDoubleLiteral.get(d);
  }

  public JFloatLiteral getLiteralFloat(float f) {
    return JFloatLiteral.get(f);
  }

  public JIntLiteral getLiteralInt(int value) {
    return JIntLiteral.get(value);
  }

  public JLongLiteral getLiteralLong(long value) {
    return JLongLiteral.get(value);
  }

  public JNullLiteral getLiteralNull() {
    return JNullLiteral.INSTANCE;
  }

  public JStringLiteral getLiteralString(SourceInfo sourceInfo, char[] s) {
    return getLiteralString(sourceInfo, String.valueOf(s));
  }

  public JStringLiteral getLiteralString(SourceInfo sourceInfo, String s) {
    sourceInfo.addCorrelation(sourceInfo.getCorrelator().by(Literal.STRING));
    return new JStringLiteral(sourceInfo, s, typeString);
  }

  public JField getNullField() {
    return JField.NULL_FIELD;
  }

  public JMethod getNullMethod() {
    return JMethod.NULL_METHOD;
  }

  public int getQueryId(JReferenceType elementType) {
    assert (elementType == elementType.getUnderlyingType());
    Integer integer = queryIdsByType.get(elementType);
    if (integer == null) {
      return 0;
    }

    return integer.intValue();
  }

  public List<JRunAsync> getRunAsyncs() {
    return runAsyncs;
  }

  public List<Integer> getSplitPointInitialSequence() {
    return splitPointInitialSequence;
  }

  public JMethod getStaticImpl(JMethod method) {
    return instanceToStaticMap.get(method);
  }

  public JArrayType getTypeArray(JType elementType) {
    JArrayType arrayType = arrayTypes.get(elementType);
    if (arrayType == null) {
      arrayType = new JArrayType(elementType);
      arrayTypes.put(elementType, arrayType);
    }
    return arrayType;
  }

  public JArrayType getTypeArray(JType leafType, int dimensions) {
    assert dimensions > 0;
    assert (!(leafType instanceof JArrayType));
    JArrayType result = getTypeArray(leafType);
    while (dimensions > 1) {
      result = getTypeArray(result);
      --dimensions;
    }
    return result;
  }

  public JClassType getTypeClassLiteralHolder() {
    return typeSpecialClassLiteralHolder;
  }

  /**
   * Returns the JType corresponding to a JSNI type reference.
   */
  public JType getTypeFromJsniRef(String className) {
    int dim = 0;
    while (className.endsWith("[]")) {
      dim++;
      className = className.substring(0, className.length() - 2);
    }

    JType type = primitiveTypes.get(className);
    if (type == null) {
      type = getFromTypeMap(className);
    }
    // TODO(deprecation): remove support for this.
    if (type == null) {
      type = primitiveTypesDeprecated.get(className);
    }
    if (type == null || dim == 0) {
      return type;
    } else {
      return getTypeArray(type, dim);
    }
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
    return JNullType.INSTANCE;
  }

  public JPrimitiveType getTypePrimitiveBoolean() {
    return JPrimitiveType.BOOLEAN;
  }

  public JPrimitiveType getTypePrimitiveByte() {
    return JPrimitiveType.BYTE;
  }

  public JPrimitiveType getTypePrimitiveChar() {
    return JPrimitiveType.CHAR;
  }

  public JPrimitiveType getTypePrimitiveDouble() {
    return JPrimitiveType.DOUBLE;
  }

  public JPrimitiveType getTypePrimitiveFloat() {
    return JPrimitiveType.FLOAT;
  }

  public JPrimitiveType getTypePrimitiveInt() {
    return JPrimitiveType.INT;
  }

  public JPrimitiveType getTypePrimitiveLong() {
    return JPrimitiveType.LONG;
  }

  public JPrimitiveType getTypePrimitiveShort() {
    return JPrimitiveType.SHORT;
  }

  public List<JReferenceType> getTypesByQueryId() {
    return typesByQueryId;
  }

  public JPrimitiveType getTypeVoid() {
    return JPrimitiveType.VOID;
  }

  public void initTypeInfo(IdentityHashMap<JReferenceType, JsCastMap> instantiatedCastableTypesMap) {
    castMaps = instantiatedCastableTypesMap;
    if (castMaps == null) {
      castMaps = new IdentityHashMap<JReferenceType, JsCastMap>();
    }
  }

  public boolean isJavaLangString(JType type) {
    return type == typeString || type == typeString.getNonNull();
  }

  public boolean isJavaScriptObject(JType type) {
    if (type instanceof JReferenceType && typeSpecialJavaScriptObject != null) {
      return typeOracle.canTriviallyCast((JReferenceType) type, typeSpecialJavaScriptObject);
    }
    return false;
  }

  public boolean isStaticImpl(JMethod method) {
    return staticToInstanceMap.containsKey(method);
  }

  /**
   * Given a sequence of fragment numbers, return the latest fragment number
   * possible that does not load later than any of these. It might be one of the
   * supplied fragments, or it might be a common predecessor.
   */
  public int lastFragmentLoadingBefore(int firstFragment, int... restFragments) {
    return lastFragmentLoadingBefore(splitPointInitialSequence, fragmentPartitioninResult,
        runAsyncs.size(), firstFragment, restFragments);
  }

  public void putIntoTypeMap(String qualifiedBinaryName, JDeclaredType type) {
    // Make it into a source type name.
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

  public void recordClassLiteralFields(Map<JType, JField> classLiteralFields) {
    this.classLiteralFields = classLiteralFields;
  }

  public void recordQueryIds(Map<JReferenceType, Integer> queryIdsByType,
      List<JReferenceType> typesByQueryId) {
    this.queryIdsByType = queryIdsByType;
    this.typesByQueryId = typesByQueryId;
  }

  public void removeStaticImplMapping(JMethod staticImpl) {
    JMethod instanceMethod = staticToInstanceMap.remove(staticImpl);
    if (instanceMethod != null) {
      instanceToStaticMap.remove(instanceMethod);
    }
  }

  public void setFragmentPartitioningResult(FragmentPartitioningResult result) {
    fragmentPartitioninResult = result;
  }

  public void setRunAsyncs(List<JRunAsync> runAsyncs) {
    this.runAsyncs = Lists.normalizeUnmodifiable(runAsyncs);
  }

  public void setSplitPointInitialSequence(List<Integer> list) {
    assert splitPointInitialSequence.isEmpty();
    splitPointInitialSequence = new ArrayList<Integer>(list);
  }

  /**
   * If <code>method</code> is a static impl method, returns the instance method
   * that <code>method</code> is the implementation of. Otherwise, returns
   * <code>null</code>.
   */
  public JMethod staticImplFor(JMethod method) {
    return staticToInstanceMap.get(method);
  }

  /**
   * Return the greatest lower bound of two types. That is, return the largest
   * type that is a subtype of both inputs.
   */
  public JReferenceType strongerType(JReferenceType type1, JReferenceType type2) {
    if (type1 == type2) {
      return type1;
    }

    if (type1 instanceof JNullType || type2 instanceof JNullType) {
      return JNullType.INSTANCE;
    }

    if (type1 instanceof JNonNullType != type2 instanceof JNonNullType) {
      // If either is non-nullable, the result should be non-nullable.
      return strongerType(type1.getNonNull(), type2.getNonNull());
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
      visitor.accept(allTypes);
    }
    visitor.endVisit(this, ctx);
  }

  private int classifyType(JReferenceType type) {
    assert !(type instanceof JNonNullType);
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

  private int countSuperTypes(JClassType type) {
    int count = 0;
    while ((type = type.getSuperClass()) != null) {
      ++count;
    }
    return count;
  }

  /**
   * See notes in {@link #writeObject(ObjectOutputStream)}.
   * 
   * @see #writeObject(ObjectOutputStream)
   */
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    allTypes = deserializeTypes(stream);
    stream.defaultReadObject();
  }

  /**
   * Serializing the Java AST is a multi-step process to avoid blowing out the
   * stack.
   * 
   * <ol>
   * <li>Write all declared types in a lightweight manner to establish object
   * identity for types</li>
   * <li>Write all fields; write all methods in a lightweight manner to
   * establish object identity for methods</li>
   * <li>Write all method bodies</li>
   * <li>Write everything else, which will mostly refer to already-serialized
   * objects.</li>
   * <li>Write the bodies of the entry methods (unlike all other methods, these
   * are not contained by any type.</li>
   * </ol>
   * 
   * The goal of this process to to avoid "running away" with the stack. Without
   * special logic here, lots of things would reference types, method body code
   * would reference both types and other methods, and really, really long
   * recursion chains would result.
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    serializeTypes(allTypes, stream);
    stream.defaultWriteObject();
  }
}
