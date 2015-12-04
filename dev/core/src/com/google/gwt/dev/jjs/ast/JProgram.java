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

import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.common.InliningMode;
import com.google.gwt.dev.jjs.Correlation.Literal;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.impl.GwtAstBuilder;
import com.google.gwt.dev.jjs.impl.JjsUtils;
import com.google.gwt.dev.jjs.impl.TypeCategory;
import com.google.gwt.dev.jjs.impl.codesplitter.FragmentPartitioningResult;
import com.google.gwt.dev.js.CoverageInstrumentor;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.CaseFormat;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.BiMap;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.HashBiMap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Root for the AST representing an entire Java program.
 */
public class JProgram extends JNode implements ArrayTypeCreator {

  /**
   * Encapsulates all information necessary to deal with native represented types in an
   * generic fashion used throughout GWT. This can be extended later to deal with say, unboxed
   * Integer if desired.
   */
  public enum DispatchType {
    // These this list can be extended by creating the appropriate fields/methods on Cast,
    // as well as extending the TypeCategory enum and updating EqualityNormalizer.
    // The order in which these native types appear is the inverse as the way they are
    // checked by devirtualized method.
    BOOLEAN(true),
    DOUBLE(true),
    STRING(true),

    // non-native represented type values.
    HAS_JAVA_VIRTUAL_DISPATCH(false), JAVA_ARRAY(false), JSO(false);

    private final String castMapField;
    private final TypeCategory typeCategory;
    private final String className;

    DispatchType(boolean nativeType) {
      if (nativeType) {
        // These field are initialized to methods that are by-convention
        // The conventions are:
        // Cast.[boxedTypeName]CastMap for cast map fields
        // TypedCategory.TYPE_JAVA_LANG_[BoxedTypeName]
        String methodName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
        this.castMapField = "Cast." + methodName + "CastMap";
        this.typeCategory = TypeCategory.valueOf("TYPE_JAVA_LANG_" + name());
        String simpleClassName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
        this.className = "java.lang." + simpleClassName;
      } else {
        this.castMapField = null;
        this.typeCategory = null;
        this.className = null;
      }
    }

    public String getCastMapField() {
      return castMapField;
    }

    public TypeCategory getTypeCategory() {
      return typeCategory;
    }

    public String getClassName() {
      return className;
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
      "com.google.gwt.lang.Array", "com.google.gwt.lang.Cast", "com.google.gwt.lang.Exceptions",
      "com.google.gwt.lang.LongLib", "com.google.gwt.lang.Stats", "com.google.gwt.lang.Util",
      "java.lang.Object"));

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
      "com.google.gwt.lang.CollapsedPropertyHolder",
      "com.google.gwt.lang.Runtime",
      "com.google.gwt.lang.ModuleUtils"));

  public static final String JAVASCRIPTOBJECT = "com.google.gwt.core.client.JavaScriptObject";

  public static final String CLASS_LITERAL_HOLDER = "com.google.gwt.lang.ClassLiteralHolder";

  /**
   * Types whose entire implementation is synthesized at compile time.
   */
  public static final Set<String> SYNTHETIC_TYPE_NAMES = Sets.newHashSet(CLASS_LITERAL_HOLDER);

  private static final Comparator<JArrayType> ARRAYTYPE_COMPARATOR =
      new Comparator<JArrayType>() {
        @Override
        public int compare(JArrayType o1, JArrayType o2) {
          int comp = o1.getDims() - o2.getDims();
          if (comp != 0) {
            return comp;
          }
          return o1.getName().compareTo(o2.getName());
        }
      };

  private static final Map<String, JPrimitiveType> primitiveTypes = Maps.newHashMap();

  @Deprecated
  private static final Map<String, JPrimitiveType> primitiveTypesDeprecated = Maps.newHashMap();

  static {
    if (CoverageInstrumentor.isCoverageEnabled()) {
      IMMORTAL_CODEGEN_TYPES_SET.add("com.google.gwt.lang.CoverageUtil");
    }
    CODEGEN_TYPES_SET.addAll(IMMORTAL_CODEGEN_TYPES_SET);

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
   * Helper to create an assignment, used to initialize fields, etc.
   */
  public static JExpressionStatement createAssignmentStmt(SourceInfo info, JExpression lhs,
      JExpression rhs) {
    return createAssignment(info, lhs, rhs).makeStatement();
  }

  public static JBinaryOperation createAssignment(SourceInfo info, JExpression lhs,
      JExpression rhs) {
    return new JBinaryOperation(info, lhs.getType(), JBinaryOperator.ASG, lhs, rhs);
  }

  public static JLocal createLocal(SourceInfo info, String name, JType type, boolean isFinal,
      JMethodBody enclosingMethodBody) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethodBody != null);
    JLocal x = new JLocal(info, name, type, isFinal);
    enclosingMethodBody.addLocal(x);
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
    return method.getEnclosingType().getName() + "." + method.getJsniSignature(false, true);
  }

  public static boolean isClinit(JMethod method) {
    JDeclaredType enclosingType = method.getEnclosingType();

    boolean isClinit = enclosingType != JClassType.NULL_CLASS &&
        method == enclosingType.getClinitMethod();
    assert !isClinit || method.getName().equals(GwtAstBuilder.CLINIT_METHOD_NAME);
    return isClinit;
  }

  public static boolean isInit(JMethod method) {
    JDeclaredType enclosingType = method.getEnclosingType();

    if (method.isStatic()) {
      // Hack, check the name.
      return method.getName().equals(GwtAstBuilder.STATIC_INIT_METHOD_NAME);
    }

    boolean isInit = enclosingType != null && method == enclosingType.getInitMethod();
    assert !isInit || method.getName().equals(GwtAstBuilder.INIT_NAME_METHOD_NAME);
    return isInit;
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

  // wrap up .add here, and filter out forced source
  private Set<String> referenceOnlyTypeNames = Sets.newHashSet();

  /**
   * Filled in by ReplaceRunAsync, once the numbers are known.
   */
  private List<JRunAsync> runAsyncs = Lists.newArrayList();

  private LinkedHashSet<JRunAsync> initialAsyncSequence = Sets.newLinkedHashSet();

  private List<Integer> initialFragmentIdSequence = Lists.newArrayList();

  private final Map<JMethod, JMethod> staticToInstanceMap = Maps.newIdentityHashMap();

  private JClassType typeClass;

  private JClassType typeJavaLangObject;

  private final Map<String, JDeclaredType> typeNameMap = Maps.newHashMap();

  private Map<JField, JType> typesByClassLiteralField;

  private JClassType typeSpecialClassLiteralHolder;

  private JClassType typeSpecialJavaScriptObject;

  private JClassType typeString;

  private FragmentPartitioningResult fragmentPartitioningResult;

  private Map<JClassType, DispatchType> dispatchTypeByNativeType;

  /**
   * Add a pinned method.
   */
  public void addPinnedMethod(JMethod method) {
    method.setInliningMode(InliningMode.DO_NOT_INLINE);
    method.disallowDevirtualization();
  }

  public JProgram(MinimalRebuildCache minimalRebuildCache) {
    this(minimalRebuildCache, true);
  }

  public JProgram(MinimalRebuildCache minimalRebuildCache, boolean legacyJsInterop) {
    super(SourceOrigin.UNKNOWN);
    typeOracle = new JTypeOracle(this, minimalRebuildCache, legacyJsInterop);
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
      // Immortal types by definition won't run clinits.
      type.setClinitTarget(null);
      immortalCodeGenTypes.add((JClassType) type);
    }

    if (!typeNamesToIndex.contains(name)) {
      return;
    }

    indexedTypes.put(type.getShortName(), type);
    for (JMethod method : type.getMethods()) {
      if (!method.isPrivate()) {
        indexedMethods.put(JjsUtils.getIndexedName(method), method);
      }
    }
    for (JField field : type.getFields()) {
      indexedFields.put(JjsUtils.getIndexedName(field), field);
    }
    switch (name) {
      case "java.lang.Object":
        typeJavaLangObject = (JClassType) type;
        break;
      case "java.lang.String":
        typeString = (JClassType) type;
        break;
      case "java.lang.Class":
        typeClass = (JClassType) type;
        break;
      case JAVASCRIPTOBJECT:
        typeSpecialJavaScriptObject = (JClassType) type;
        break;
      case CLASS_LITERAL_HOLDER:
        typeSpecialClassLiteralHolder = (JClassType) type;
        break;
    }
  }

  public static boolean isRepresentedAsNative(final String className) {
    return Iterables.any(Arrays.asList(DispatchType.values()), new Predicate<DispatchType>() {
      @Override
      public boolean apply(DispatchType dispatchType) {
        return className.equals(dispatchType.getClassName());
      }
    });
  }

  public boolean isRepresentedAsNativeJsPrimitive(JType type) {
    return getRepresentedAsNativeTypes().contains(type);
  }

  public Set<JClassType> getRepresentedAsNativeTypes() {
    return getRepresentedAsNativeTypesDispatchMap().keySet();
  }

  public Map<JClassType, DispatchType> getRepresentedAsNativeTypesDispatchMap() {
     if (dispatchTypeByNativeType == null) {
       ImmutableMap.Builder<JClassType, DispatchType> builder =
           new ImmutableMap.Builder<JClassType, DispatchType>();
       for (DispatchType dispatchType : DispatchType.values()) {
         if (dispatchType.getClassName() == null) {
           continue;
         }
         JClassType classType = (JClassType) getFromTypeMap(dispatchType.getClassName());
         assert classType != null : "Class " + dispatchType.getClassName() + " has not been loaded";
         builder.put(classType, dispatchType);
       }
       dispatchTypeByNativeType = builder.build();
     }
    return dispatchTypeByNativeType;
  }

  public EnumSet<DispatchType> getDispatchType(JReferenceType type) {
    if (!typeOracle.isInstantiatedType(type)) {
      return EnumSet.noneOf(DispatchType.class);
    }

    // Object methods can be dispatched to all four possible classes.
    if (type == getTypeJavaLangObject()) {
      return EnumSet.allOf(DispatchType.class);
    }

    if (type.isArrayType()) {
      // A variable of type Object[] could contain an instance of native JsType[], the latter
      // is treated as a JSO for devirtualization purposes.
      return EnumSet.of(DispatchType.JSO, DispatchType.JAVA_ARRAY);
    }
    EnumSet<DispatchType> dispatchSet = EnumSet.noneOf(DispatchType.class);
    DispatchType dispatchType = getRepresentedAsNativeTypesDispatchMap().get(type);
    if (dispatchType != null) {
      dispatchSet = EnumSet.of(dispatchType);
    } else if (typeOracle.isDualJsoInterface(type) || type.isJsNative()) {
      // If it is an interface implemented both by JSOs and regular Java Objects; native JsTypes
      // are considered JSOs for object method devirtualization.
      dispatchSet = EnumSet.of(DispatchType.HAS_JAVA_VIRTUAL_DISPATCH, DispatchType.JSO);
    } else if (typeOracle.isSingleJsoImpl(type) || type.isJsoType()) {
      // If it is either an interface implemented by JSOs or JavaScriptObject or one of its
      // subclasses.
      dispatchSet = EnumSet.of(DispatchType.JSO);
    }

    for (JDeclaredType potentialNativeDispatchType : getRepresentedAsNativeTypes()) {
      if (potentialNativeDispatchType == type) {
        continue;
      }

      if (typeOracle.isInstantiatedType(potentialNativeDispatchType)
          && typeOracle.isSuperClassOrInterface(potentialNativeDispatchType, type)) {
        dispatchSet.add(getRepresentedAsNativeTypesDispatchMap().get(potentialNativeDispatchType));
        dispatchSet.add(DispatchType.HAS_JAVA_VIRTUAL_DISPATCH);
      }
    }
    return dispatchSet;
  }

  /**
   * Return the greatest lower bound of two types. That is, return the largest
   * type that is a subtype of both inputs. If none exists return {@code thisType}.
   */
  public JReferenceType strengthenType(JReferenceType thisType, JReferenceType thatType) {
    if (thisType == thatType) {
      return thisType;
    }

    if (thisType.isNullType() || thatType.isNullType()) {
      return JReferenceType.NULL_TYPE;
    }

    if (thisType.canBeNull()  != thatType.canBeNull()) {
      // If either is non-nullable, the result should be non-nullable.
      return strengthenType(thisType.strengthenToNonNull(), thatType.strengthenToNonNull());
    }

    if (typeOracle.castSucceedsTrivially(thisType, thatType)) {
      return thisType;
    }

    if (typeOracle.castSucceedsTrivially(thatType, thisType)) {
      return thatType;
    }

    // This types are incompatible; ideally this code should not be reached, but there are two
    // situations where this happens:
    //   1 - unrelated interfaces;
    //   2 - unsafe code.
    // The original type is preserved in this case.
    return thisType;
  }

  /**
   * Return a minimal upper bound of a set of types. That is, a type
   * that is a supertype of all the input types and is as close as possible to the
   * input types.
   * <p>
   * NOTE: Ideally we would like to return the least upper bound but it does not exit as
   * the Java type hierarchy is not really a lattice.
   * <p>
   * Hence, this function depends on the collection order. E.g.
   * <p>
   * {@code
   *                I    O
   *                |\ / \
   *                | A  B
   *                \   /
   *                 \ /
   *                  C
   * }
   * <p>
   * where I is an interface an {O,A,B,C} are classes.
   * <p>
   * generalizeTypes({A,C}) could either be I or O.
   * <p>
   * In particular generalizeTypes({I,A,C}) = I and generalizeTypes({A,C,I}) = O.
   *
   */
  public JReferenceType generalizeTypes(Iterable<JReferenceType> types) {
    Iterator<JReferenceType> it = types.iterator();
    if (!it.hasNext()) {
      return JReferenceType.NULL_TYPE;
    }
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
   * Return the least upper bound of two types. That is, the "smallest" type that
   * is a supertype of both types. In this lattice there the smallest element might no exist, there
   * might be multiple minimal elements neither of which is smaller than the others. E.g.
   * <p>
   * {@code
   *                 I      J
   *                | \    /|
   *                |  \  / |
   *                |   x   |
   *                |  / \  |
   *                | /   \ |
   *                 A     B
   * }
   * <p>
   * where I and J are interfaces, A and B are classes and both A and B implement I and J. In this
   * case both I and J are generalizing the types A and B.
   */
  private JReferenceType generalizeTypes(JReferenceType thisType, JReferenceType thatType) {

    if (!thisType.canBeNull() && !thatType.canBeNull()) {
      // Nullability is an orthogonal property, so remove non_nullability and perform the
      // generalization on the nullable types, and if both were NOT nullable then strengthen the
      // result to NOT nullable.
      //
      // not_nullable(A) v not_nullable(B) = not_nullable(A v B)
      JReferenceType nulllableGeneralizer =
          generalizeTypes(thisType.weakenToNullable(), thatType.weakenToNullable());
      return nulllableGeneralizer.strengthenToNonNull();
    }
    thisType = thisType.weakenToNullable();
    thatType = thatType.weakenToNullable();

    // From here on nullability does not need to be considered.

    // Generalization for exact types is as follows.
    // exact(A) v null = exact(A)
    // A v null = A
    if (thatType.isNullType()) {
      return thisType;
    }

    // null v exact(A) = exact(A)
    // null v A = A
    if (thisType.isNullType()) {
      return thatType;
    }

    // exact(A) v exact(A)  = exact(A)
    // A v A  = A
    if (thisType == thatType) {
      return thisType;
    }

    // exact(A) v exact(B) = A v B
    // A v exact(B) = A v B
    // exact(A) v B = A v B
    // A v B = A v B
    return generalizeUnderlyingTypes(thisType.getUnderlyingType(), thatType.getUnderlyingType());
  }

  private JReferenceType generalizeUnderlyingTypes(
      JReferenceType thisType, JReferenceType thatType) {

    // We should not have any analysis properties from this point forward.
    assert thisType == thisType.getUnderlyingType() && thatType == thatType.getUnderlyingType();

    if (thisType == thatType) {
      return thisType;
    }

    if (thisType instanceof JInterfaceType && thatType instanceof JInterfaceType) {
      return generalizeInterfaces((JInterfaceType) thisType, (JInterfaceType) thatType);
    }

    if (thisType instanceof JArrayType && thatType instanceof JArrayType) {
      return generalizeArrayTypes((JArrayType) thisType, (JArrayType) thatType);
    }

    if (thisType instanceof JClassType && thatType instanceof JClassType) {
      return generalizeClasses((JClassType) thisType, (JClassType) thatType);
    }

    JInterfaceType interfaceType = thisType instanceof JInterfaceType ? (JInterfaceType) thisType :
        (thatType instanceof JInterfaceType ? (JInterfaceType) thatType : null);

    JReferenceType nonInterfaceType = interfaceType == thisType ? thatType : thisType;

    // See if the class or the array is castable to the interface type.
    if (interfaceType != null &&
        typeOracle.castSucceedsTrivially(nonInterfaceType, interfaceType)) {
      return interfaceType;
    }

    // unrelated: the best commonality is Object
    return typeJavaLangObject;
  }

  private JReferenceType generalizeArrayTypes(JArrayType thisArrayType, JArrayType thatArrayType) {
    assert thisArrayType != thatArrayType;

    int thisDims = thisArrayType.getDims();
    int thatDims = thatArrayType.getDims();

    int minDims = Math.min(thisDims, thatDims);
      /*
       * At a bare minimum, any two arrays generalize to an Object array with
       * one less dim than the lesser of the two; that is, int[][][][] and
       * String[][][] generalize to Object[][]. If minDims is 1, then they
       * just generalize to Object.
       */
    JReferenceType minimalGeneralType = (minDims == 1) ? typeJavaLangObject :
        getOrCreateArrayType(typeJavaLangObject, minDims - 1);

    if (thisDims == thatDims) {

      // Try to generalize by leaf types
      JType thisLeafType = thisArrayType.getLeafType();
      JType thatLeafType = thatArrayType.getLeafType();

      if (!(thisLeafType instanceof JReferenceType) || !(thatLeafType instanceof JReferenceType)) {
        return minimalGeneralType;
      }

      /*
       * Both are reference types; the result is the generalization of the leaf types combined with
       * the number of dims; that is, Foo[] and Bar[] generalize to X[] where X is the
       * generalization of Foo and Bar.
       *
       * Never generalize arrays to arrays of {@link JAnalysisDecoratedType}. One of the reasons is
       * that array initialization is not accounted for in {@link TypeTightener}.
       */
      JReferenceType leafGeneralization = generalizeTypes(
          (JReferenceType) thisLeafType, (JReferenceType) thatLeafType).getUnderlyingType();
      return getOrCreateArrayType(leafGeneralization, thisDims);
    }

    // Different number of dims
    if (typeOracle.castSucceedsTrivially(thatArrayType, thisArrayType)) {
      return thisArrayType;
    }

    if (typeOracle.castSucceedsTrivially(thisArrayType, thatArrayType)) {
      return thatArrayType;
    }

    // Totally unrelated
    return minimalGeneralType;
  }

  private JReferenceType generalizeInterfaces(JInterfaceType thisInterface,
      JInterfaceType thatInterface) {
    if (typeOracle.castSucceedsTrivially(thisInterface, thatInterface)) {
      return thatInterface;
    }

    if (typeOracle.castSucceedsTrivially(thatInterface, thisInterface)) {
      return thisInterface;
    }

    // unrelated
    return typeJavaLangObject;
  }

  private JReferenceType generalizeClasses(JClassType thisClass, JClassType thatClass) {
  /*
   * see how far each type is from object; walk the one who's farther up
   * until they're even; then walk them up together until they meet (worst
   * case at Object)
   */
    int distance1 = countSuperTypes(thisClass);
    int distance2 = countSuperTypes(thatClass);
    for (; distance1 > distance2; --distance1) {
      thisClass = thisClass.getSuperClass();
    }

    for (; distance1 < distance2; --distance2) {
      thatClass = thatClass.getSuperClass();
    }

    while (thisClass != thatClass) {
      thisClass = thisClass.getSuperClass();
      thatClass = thatClass.getSuperClass();
    }

    return thisClass;
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

  /**
   * Returns an expression that evaluates to an array class literal at runtime.
   * <p>
   * Note: This version can only be called after {@link
   * com.google.gwt.dev.jjs.impl.ImplementClassLiteralsAsFields} has been run.
   */
  public JExpression createArrayClassLiteralExpression(SourceInfo sourceInfo,
      JClassLiteral leafTypeClassLiteral, int dimensions) {
    JField leafTypeClassLiteralField = leafTypeClassLiteral.getField();
    assert leafTypeClassLiteralField != null : "Array leaf type must have a class literal field; "
        + "either ImplementClassLiteralsAsField has not run yet or or there is an error computing"
        + "live class literals.";

    return new JMethodCall(sourceInfo, null, getIndexedMethod(
        RuntimeConstants.ARRAY_GET_CLASS_LITERAL_FOR_ARRAY),
        new JFieldRef(sourceInfo, null,  leafTypeClassLiteralField,
            leafTypeClassLiteralField.getEnclosingType()), getLiteralInt(dimensions));
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
    return classLiteralFieldsByType.get(
        type.isJsoType() ? getJavaScriptObject() : type);
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
    return fragmentPartitioningResult;
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

  public Set<JField> getIndexedFields() {
    return ImmutableSet.copyOf(indexedFields.values());
  }

  public JMethod getIndexedMethod(String string) {
    JMethod method = indexedMethods.get(string);
    if (method == null) {
      throw new InternalCompilerException("Unable to locate index method: " + string);
    }
    return method;
  }

  public Set<JMethod> getIndexedMethods() {
    return ImmutableSet.copyOf(indexedMethods.values());
  }

  public JMethod getIndexedMethodOrNull(String string) {
    return indexedMethods.get(string);
  }

  public JDeclaredType getIndexedType(String string) {
    JDeclaredType type = indexedTypes.get(string);
    if (type == null) {
      throw new InternalCompilerException("Unable to locate index type: " + string);
    }
    return type;
  }

  public Collection<JDeclaredType> getIndexedTypes() {
    return Collections.unmodifiableCollection(indexedTypes.values());
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

  public JLiteral getLiteral(Object value) {
    return getLiteral(SourceOrigin.UNKNOWN, value);
  }

  public JLiteral getLiteral(SourceInfo info,  Object value) {
    if (value == null) {
      return getLiteralNull();
    }
    if (value instanceof String) {
      return getStringLiteral(info, (String) value);
    }
    if (value instanceof Integer) {
      return getLiteralInt((Integer) value);
    }
    if (value instanceof Long) {
      return getLiteralLong((Long) value);
    }
    if (value instanceof Character) {
      return getLiteralChar((Character) value);
    }
    if (value instanceof Boolean) {
      return getLiteralBoolean((Boolean) value);
    }
    if (value instanceof Double) {
      return getLiteralDouble((Double) value);
    }
    if (value instanceof Float) {
      return getLiteralFloat((Float) value);
    }
    throw new IllegalArgumentException("Unknown literal type for " + value);
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

  public JFloatLiteral getLiteralFloat(double f) {
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
    return new JStringLiteral(sourceInfo, StringInterner.get().intern(s), typeString);
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

  public List<JRunAsync> getRunAsyncs() {
    return runAsyncs;
  }

  public int getCommonAncestorFragmentId(int thisFragmentId, int thatFragmentId) {
    return fragmentPartitioningResult.getCommonAncestorFragmentId(thisFragmentId, thatFragmentId);
  }

  public Collection<JType> getSubclasses(JType type) {
    return Collections2.transform(typeOracle.getSubClassNames(type.getName()),
        new Function<String, JType>() {
          @Override
          public JType apply(String typeName) {
            return getFromTypeMap(typeName);
          }
        }
    );
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

  // TODO(dankurka): Why does JProgram synthezise array types on the fly
  // Look into refactoring JProgram to get rid of this responsibility
  @Override
  public JArrayType getOrCreateArrayType(JType leafType, int dimensions) {
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
      return getOrCreateArrayType(type, dim);
    }
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

  public Set<String> getTypeNamesToIndex() {
    return typeNamesToIndex;
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

  public JPrimitiveType getTypeVoid() {
    return JPrimitiveType.VOID;
  }

  public void initTypeInfo(Map<JReferenceType, JCastMap> castMapForType) {
    castMaps = castMapForType;
    if (castMaps == null) {
      castMaps = Maps.newIdentityHashMap();
    }
  }

  public boolean isUntypedArrayType(JType type) {
    if (!type.isArrayType()) {
      return false;
    }

    JArrayType arrayType = (JArrayType) type;
    return arrayType.getLeafType().isJsNative();
  }

  public boolean isJavaLangString(JType type) {
    assert type != null;
    return type.getUnderlyingType() == typeString;
  }

  public boolean isJavaLangObject(JType type) {
    assert type != null;
    return type.getUnderlyingType() == typeJavaLangObject;
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

  /**
   * If the type is a JSO or an array of JSOs it returns cggcc.JavaScriptObject or an array of
   * cggcc.JavaScriptObject respectively; otherwise returns {@code type}.
   */
  public JType normalizeJsoType(JType type) {
    type = type.getUnderlyingType();

    if (type instanceof JArrayType) {
      return getOrCreateArrayType(normalizeJsoType(((JArrayType) type).getLeafType()),
          ((JArrayType) type).getDims());
    }

    if (type.isJsoType()) {
      return getJavaScriptObject();
    }
    return type;
  }

  public void putIntoTypeMap(String qualifiedBinaryName, JDeclaredType type) {
    // Make it into a source type name.
    String srcTypeName = qualifiedBinaryName.replace('$', '.');
    typeNameMap.put(srcTypeName, type);
  }

  public void putStaticImpl(JMethod method, JMethod staticImpl) {
    instanceToStaticMap.put(method, staticImpl);
    staticToInstanceMap.put(staticImpl, method);
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

  public void removeReferenceOnlyType(JDeclaredType type) {
    referenceOnlyTypeNames.remove(type.getName());
  }

  public void setFragmentPartitioningResult(FragmentPartitioningResult result) {
    fragmentPartitioningResult = result;
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

  /**
   * If {@code method} is a static impl method, returns the instance method
   * that {@code method} is the implementation of. Otherwise, returns{@code null}.
   */
  public JMethod instanceMethodForStaticImpl(JMethod method) {
    return staticToInstanceMap.get(method);
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
        JAVASCRIPTOBJECT, CLASS_LITERAL_HOLDER, "com.google.gwt.core.client.RunAsyncCallback",
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
