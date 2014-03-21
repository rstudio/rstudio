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
import com.google.gwt.dev.jjs.impl.codesplitter.FragmentPartitioningResult;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.collect.BiMap;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.HashBiMap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    @Override
    public int compare(JArrayType o1, JArrayType o2) {
      int comp = o1.getDims() - o2.getDims();
      if (comp != 0) {
        return comp;
      }
      return o1.getName().compareTo(o2.getName());
    }
  }

  private static final class TreeStatistics extends JVisitor {
    private int nodeCount = 0;

    public int getNodeCount() {
      return nodeCount;
    }

    @Override
    public boolean visit(JNode x, Context ctx) {
      nodeCount++;
      return true;
    }
  }

  public static final Set<String> CODEGEN_TYPES_SET = Sets.newLinkedHashSet(Arrays.asList(
      "com.google.gwt.lang.Array", "com.google.gwt.lang.Cast",
      "com.google.gwt.lang.RuntimePropertyRegistry", "com.google.gwt.lang.Exceptions",
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
  public static final Set<String> IMMORTAL_CODEGEN_TYPES_SET = Sets.newLinkedHashSet(Arrays.asList(
      "com.google.gwt.lang.CollapsedPropertyHolder", "com.google.gwt.lang.JavaClassHierarchySetupUtil"));

  public static final String JAVASCRIPTOBJECT = "com.google.gwt.core.client.JavaScriptObject";

  static final Map<String, Set<String>> traceMethods = Maps.newHashMap();

  private static final Comparator<JArrayType> ARRAYTYPE_COMPARATOR = new ArrayTypeComparator();

  private static final int IS_ARRAY = 2;

  private static final int IS_CLASS = 3;

  private static final int IS_INTERFACE = 1;

  private static final int IS_NULL = 0;

  private static final Map<String, JPrimitiveType> primitiveTypes = Maps.newHashMap();

  @Deprecated
  private static final Map<String, JPrimitiveType> primitiveTypesDeprecated = Maps.newHashMap();

  static {
    if (System.getProperty("gwt.coverage") != null) {
      IMMORTAL_CODEGEN_TYPES_SET.add("com.google.gwt.lang.CoverageUtil");
    }
    CODEGEN_TYPES_SET.addAll(IMMORTAL_CODEGEN_TYPES_SET);

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
            set = Sets.newHashSet();
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

  public static String getFullName(JMethod method) {
    return method.getEnclosingType().getName() + "." + getJsniSig(method);
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
    if ((enclosingType != null) && (method == enclosingType.getClinitMethod())) {
      assert (method.getName().equals("$clinit"));
      return true;
    } else {
      return false;
    }
  }

  public static boolean isTracingEnabled() {
    return traceMethods.size() > 0;
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

  public final List<JClassType> codeGenTypes = Lists.newArrayList();

  public final List<JClassType> immortalCodeGenTypes = Lists.newArrayList();

  public final JTypeOracle typeOracle;

  /**
   * Special serialization treatment.
   */
  // TODO(stalcup): make this a set, or take special care to make updates unique when lazily loading
  // in types. At the moment duplicates are accumulating.
  private transient List<JDeclaredType> allTypes = Lists.newArrayList();

  private final Map<JType, JArrayType> arrayTypes = Maps.newHashMap();

  private Map<JReferenceType, JCastMap> castMaps;

  private BiMap<JType, JField> classLiteralFieldsByType;

  private final List<JMethod> entryMethods = Lists.newArrayList();

  private final Map<String, JField> indexedFields = Maps.newHashMap();

  private final Map<String, JMethod> indexedMethods = Maps.newHashMap();

  /**
   * An index of types, from type name to type instance.
   */
  private final Map<String, JDeclaredType> indexedTypes = Maps.newHashMap();

  /**
   * The set of names of types (beyond the basic INDEX_TYPES_SET) whose instance should be indexed
   * when seen.
   */
  private final Set<String> typeNamesToIndex = buildInitialTypeNamesToIndex();

  private final Map<JMethod, JMethod> instanceToStaticMap = Maps.newIdentityHashMap();

  private String propertyProviderRegistratorTypeSourceName;

  // wrap up .add here, and filter out forced source
  private Set<String> referenceOnlyTypeNames = Sets.newHashSet();

  /**
   * Filled in by ReplaceRunAsync, once the numbers are known.
   */
  private List<JRunAsync> runAsyncs = Lists.newArrayList();

  private LinkedHashSet<JRunAsync> initialAsyncSequence = Sets.newLinkedHashSet();

  private List<Integer> initialFragmentIdSequence = Lists.newArrayList();

  private String runtimeRebindRegistratorTypeName;

  private final Map<JMethod, JMethod> staticToInstanceMap = Maps.newIdentityHashMap();

  private JClassType typeClass;

  private JInterfaceType typeJavaIoSerializable;

  private JInterfaceType typeJavaLangCloneable;

  private JClassType typeJavaLangEnum;

  private JClassType typeJavaLangObject;

  private final Map<String, JDeclaredType> typeNameMap = Maps.newHashMap();

  private List<JReferenceType> typesByQueryId;

  private Map<JField, JType> typesByClassLiteralField;

  private JClassType typeSpecialClassLiteralHolder;

  private JClassType typeSpecialJavaScriptObject;

  private JClassType typeString;

  private FragmentPartitioningResult fragmentPartitioninResult;

  public JProgram() {
    super(SourceOrigin.UNKNOWN);
    typeOracle = new JTypeOracle(this, true);
  }

  public JProgram(boolean hasWholeWorldKnowledge) {
    super(SourceOrigin.UNKNOWN);
    typeOracle = new JTypeOracle(this, hasWholeWorldKnowledge);
  }

  public void addEntryMethod(JMethod entryPoint) {
    assert !entryMethods.contains(entryPoint);
    entryMethods.add(entryPoint);
  }

  /**
   * Adds the given type name to the set of type names (beyond the basic INDEX_TYPES_SET) whose
   * instance should be indexed when seen.
   */
  public void addIndexedTypeName(String typeName) {
    typeNamesToIndex.add(typeName);
  }

  public void addReferenceOnlyType(JDeclaredType type) {
    referenceOnlyTypeNames.add(type.getName());
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

    if (typeNamesToIndex.contains(name)) {
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
   * Return a minimal upper bound of a set of types. That is, a type
   * that is a supertype of all the input types and is as close as possible to the
   * input types.
   *
   * NOTE: Ideally we would like to return the least upper bound but it does not exit as
   * the Java type hierarchy is not really a lattice.
   *
   * Hence, this function depends on the collection order. E.g.
   *
   *                I    O
   *                |\ / \
   *                | A  B
   *                \   /
   *                 \ /
   *                  C
   *
   * where I is an interface an {O,A,B,C} are classes.
   *
   * generalizeTypes({A,C}) could either be I or O.
   *
   * In particular generalizeTypes({I,A,C}) = I and generalizeTypes({A,C,I}) = O.
   *
   */
  public JReferenceType generalizeTypes(Collection<? extends JReferenceType> types) {
    assert (types != null);
    assert (!types.isEmpty());
    Iterator<? extends JReferenceType> it = types.iterator();
    JReferenceType curType = it.next();
    while (it.hasNext()) {
      curType = generalizeTypes(curType, it.next());
      if (curType == typeJavaLangObject) {
        break;
      }
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
    List<JArrayType> result = Lists.newArrayList(arrayTypes.values());
    Collections.sort(result, ARRAYTYPE_COMPARATOR);
    return result;
  }

  public Map<JReferenceType, JCastMap> getCastMap() {
    return Collections.unmodifiableMap(castMaps);
  }

  public JCastMap getCastMap(JReferenceType referenceType) {
    // ensure jsonCastableTypeMaps has been initialized
    // it might not have been if the ImplementCastsAndTypeChecks has not been run
    if (castMaps == null) {
      initTypeInfo(null);
    }
    return castMaps.get(referenceType);
  }

  public JField getClassLiteralField(JType type) {
    return classLiteralFieldsByType.get(isJavaScriptObject(type) ? getJavaScriptObject() : type);
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

  // TODO(stalcup): this is a blatant bug. there's no unambiguous way to convert from binary name to
  // source name. JProgram needs to index types both ways.
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

  public LinkedHashSet<JRunAsync> getInitialAsyncSequence() {
    return initialAsyncSequence;
  }

  public List<Integer> getInitialFragmentIdSequence() {
    return initialFragmentIdSequence;
  }

  public JClassType getJavaScriptObject() {
    return typeSpecialJavaScriptObject;
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

  public JStringLiteral getStringLiteral(SourceInfo sourceInfo, String s) {
    sourceInfo.addCorrelation(sourceInfo.getCorrelator().by(Literal.STRING));
    return new JStringLiteral(sourceInfo, s, typeString);
  }

  public List<JDeclaredType> getModuleDeclaredTypes() {
    List<JDeclaredType> moduleDeclaredTypes = Lists.newArrayList();
    for (JDeclaredType type : allTypes) {
      if (isReferenceOnly(type)) {
        continue;
      }
      moduleDeclaredTypes.add(type);
    }
    return moduleDeclaredTypes;
  }

  public int getNodeCount() {
    Event countEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "phase", "countNodes");
    TreeStatistics treeStats = new TreeStatistics();
    treeStats.accept(this);
    int numNodes = treeStats.getNodeCount();
    countEvent.end();
    return numNodes;
  }

  public JField getNullField() {
    return JField.NULL_FIELD;
  }

  public JMethod getNullMethod() {
    return JMethod.NULL_METHOD;
  }

  public String getPropertyProviderRegistratorTypeSourceName() {
    return propertyProviderRegistratorTypeSourceName;
  }

  public List<JRunAsync> getRunAsyncs() {
    return runAsyncs;
  }

  public int getCommonAncestorFragmentId(int thisFragmentId, int thatFragmentId) {
    return fragmentPartitioninResult.getCommonAncestorFragmentId(thisFragmentId, thatFragmentId);
  }

  public String getRuntimeRebindRegistratorTypeSourceName() {
    return runtimeRebindRegistratorTypeName;
  }

  public JMethod getStaticImpl(JMethod method) {
    JMethod staticImpl = instanceToStaticMap.get(method);
    assert staticImpl == null || staticImpl.getEnclosingType().getMethods().contains(staticImpl);
    return staticImpl;
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

  public JType getTypeByClassLiteralField(JField field) {
    return typesByClassLiteralField.get(field);
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

  public Set<String> getTypeNamesToIndex() {
    return typeNamesToIndex;
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

  public void initTypeInfo(Map<JReferenceType, JCastMap> castMapForType) {
    castMaps = castMapForType;
    if (castMaps == null) {
      castMaps = Maps.newIdentityHashMap();
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

  public boolean isReferenceOnly(JDeclaredType type) {
    if (type != null) {
      return referenceOnlyTypeNames.contains(type.getName());
    }
    return false;
  }

  public boolean isStaticImpl(JMethod method) {
    return staticToInstanceMap.containsKey(method);
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
    this.classLiteralFieldsByType = HashBiMap.create(classLiteralFields);
    this.typesByClassLiteralField = classLiteralFieldsByType.inverse();
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

  public void setInitialFragmentIdSequence(List<Integer> initialFragmentIdSequence) {
    this.initialFragmentIdSequence = initialFragmentIdSequence;
  }

  public void setRunAsyncs(List<JRunAsync> runAsyncs) {
    this.runAsyncs = ImmutableList.copyOf(runAsyncs);
  }

  public void setInitialAsyncSequence(LinkedHashSet<JRunAsync> initialAsyncSequence) {
    assert this.initialAsyncSequence.isEmpty();
    initialFragmentIdSequence = Lists.newArrayList();
    // TODO(rluble): hack for now the initial fragments correspond to the initial runAsyncIds.
    initialFragmentIdSequence.addAll(
        Collections2.transform(initialAsyncSequence,
            new Function<JRunAsync, Integer>() {
              @Override
              public Integer apply(JRunAsync runAsync) {
                return runAsync.getRunAsyncId();
              }
            }));
    this.initialAsyncSequence = initialAsyncSequence;
  }

  public void setPropertyProviderRegistratorTypeSourceName(
      String propertyProviderRegistratorTypeSourceName) {
    this.propertyProviderRegistratorTypeSourceName = propertyProviderRegistratorTypeSourceName;
  }

  public void setRuntimeRebindRegistratorTypeName(String runtimeRebindRegistratorTypeName) {
    this.runtimeRebindRegistratorTypeName = runtimeRebindRegistratorTypeName;
  }

  /**
   * If {@code method} is a static impl method, returns the instance method
   * that {@code method} is the implementation of. Otherwise, returns{@code null}.
   */
  public JMethod instanceMethodForStaticImpl(JMethod method) {
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

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitModuleTypes(visitor);
    }
    visitor.endVisit(this, ctx);
  }

  /**
   * Builds the starter set of type names that should be indexed when seen during addType(). This
   * set is a thread safe instance variable and external logic is free to modify it as further
   * requirements are discovered.
   */
  private static Set<String> buildInitialTypeNamesToIndex() {
    Set<String> typeNamesToIndex = Sets.newHashSet();
    typeNamesToIndex.addAll(ImmutableList.of("java.io.Serializable", "java.lang.Object",
        "java.lang.String", "java.lang.Class", "java.lang.CharSequence", "java.lang.Cloneable",
        "java.lang.Comparable", "java.lang.Enum", "java.lang.Iterable", "java.util.Iterator",
        "java.lang.AssertionError", "java.lang.Boolean", "java.lang.Byte", "java.lang.Character",
        "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float",
        "java.lang.Double", "java.lang.Throwable", "com.google.gwt.core.client.GWT",
        JProgram.JAVASCRIPTOBJECT, "com.google.gwt.lang.RuntimeRebinder",
        "com.google.gwt.lang.ClassLiteralHolder", "com.google.gwt.core.client.RunAsyncCallback",
        "com.google.gwt.core.client.impl.AsyncFragmentLoader",
        "com.google.gwt.core.client.impl.Impl",
        "com.google.gwt.core.client.prefetch.RunAsyncCode"));
    typeNamesToIndex.addAll(CODEGEN_TYPES_SET);
    return typeNamesToIndex;
  }

  public void visitAllTypes(JVisitor visitor) {
    visitor.accept(allTypes);
  }

  public void visitModuleTypes(JVisitor visitor) {
    for (JDeclaredType type : allTypes) {
      if (isReferenceOnly(type)) {
        continue;
      }
      visitor.accept(type);
    }
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
