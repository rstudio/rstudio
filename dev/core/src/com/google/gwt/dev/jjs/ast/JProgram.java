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

import com.google.gwt.dev.jjs.CorrelationFactory;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.Correlation.Literal;
import com.google.gwt.dev.jjs.ast.JField.Disposition;
import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.impl.CodeSplitter;
import com.google.gwt.dev.jjs.impl.ReplaceRunAsyncs.RunAsyncReplacement;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Maps;

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
          "java.io.Serializable", "java.lang.Object", "java.lang.String",
          "java.lang.Class", "java.lang.CharSequence", "java.lang.Cloneable",
          "java.lang.Comparable", "java.lang.Enum", "java.lang.Iterable",
          "java.util.Iterator", "com.google.gwt.core.client.GWT",
          "com.google.gwt.core.client.JavaScriptObject",
          "com.google.gwt.lang.ClassLiteralHolder",
          "com.google.gwt.core.client.RunAsyncCallback",
          "com.google.gwt.core.client.impl.AsyncFragmentLoader",
          "com.google.gwt.core.client.impl.Impl",
          "com.google.gwt.lang.EntryMethodHolder",
          "com.google.gwt.core.client.prefetch.RunAsyncCode",}));

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
    sb.append(method.getOriginalReturnType().getJsniSignatureName());
    return sb.toString();
  }

  public static boolean isClinit(JMethod method) {
    JDeclaredType enclosingType = method.getEnclosingType();
    if ((enclosingType != null)
        && (method == enclosingType.getMethods().get(0))) {
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
   * @param numSps The number of split points in the program
   * @param firstFragment The first fragment to consider
   * @param restFragments The rest of the fragments to consider
   */
  public static int lastFragmentLoadingBefore(List<Integer> initialSeq,
      int numSps, int firstFragment, int... restFragments) {
    int latest = firstFragment;
    for (int frag : restFragments) {
      latest = pairwiseLastFragmentLoadingBefore(initialSeq, numSps, latest,
          frag);
    }
    return latest;
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

  /**
   * The main logic behind {@link #lastFragmentLoadingBefore(int, int...)} and
   * {@link #lastFragmentLoadingBefore(List, int, int, int...)}.
   */
  private static int pairwiseLastFragmentLoadingBefore(
      List<Integer> initialSeq, int numSps, int frag1, int frag2) {
    if (frag1 == frag2) {
      return frag1;
    }

    if (frag1 == 0) {
      return 0;
    }

    if (frag2 == 0) {
      return 0;
    }

    // See if either is in the initial sequence
    int initPos1 = initialSeq.indexOf(frag1);
    int initPos2 = initialSeq.indexOf(frag2);

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

    // They are both leftovers or exclusive. Leftovers goes first in all cases.
    return CodeSplitter.getLeftoversFragmentNumber(numSps);
  }

  public final List<JClassType> codeGenTypes = new ArrayList<JClassType>();

  /**
   * There is a list containing the main entry methods as well as the entry
   * methods for each split point. The main entry methods are at entry 0 of this
   * list. Split points are numbered sequentially from 1, and the entry methods
   * for split point <em>i</em> are at entry <em>i</em> of this list.
   */
  public final List<List<JMethod>> entryMethods = new ArrayList<List<JMethod>>();

  public final Map<String, HasEnclosingType> jsniMap = new HashMap<String, HasEnclosingType>();

  public final JTypeOracle typeOracle = new JTypeOracle(this);

  /**
   * Sorted to avoid nondeterministic iteration.
   */
  private final Set<JArrayType> allArrayTypes = new TreeSet<JArrayType>(
      ARRAYTYPE_COMPARATOR);

  /**
   * Special serialization treatment.
   */
  private transient List<JDeclaredType> allTypes = new ArrayList<JDeclaredType>();

  private final Map<JType, JClassLiteral> classLiterals = new IdentityHashMap<JType, JClassLiteral>();

  /**
   * A factory to create correlations.
   */
  private final CorrelationFactory correlator;

  /**
   * Each entry is a HashMap(JType => JArrayType), arranged such that the number
   * of dimensions is that index (plus one) at which the JArrayTypes having that
   * number of dimensions resides.
   */
  private final ArrayList<HashMap<JType, JArrayType>> dimensions = new ArrayList<HashMap<JType, JArrayType>>();

  private final Map<String, JField> indexedFields = new HashMap<String, JField>();

  private final Map<String, JMethod> indexedMethods = new HashMap<String, JMethod>();

  private final Map<String, JDeclaredType> indexedTypes = new HashMap<String, JDeclaredType>();

  private final Map<JMethod, JMethod> instanceToStaticMap = new IdentityHashMap<JMethod, JMethod>();

  /**
   * The root intrinsic source info.
   */
  private final SourceInfo intrinsic;

  private List<JsonObject> jsonTypeTable;

  private Map<JReferenceType, JNonNullType> nonNullTypes = new IdentityHashMap<JReferenceType, JNonNullType>();

  private JField nullField;

  private JMethod nullMethod;

  /**
   * Turned on once optimizations begin.
   */
  private boolean optimizationsStarted = false;

  private Map<JReferenceType, Integer> queryIds;

  /**
   * Filled in by ReplaceRunAsync, once the numbers are known.
   */
  private Map<Integer, RunAsyncReplacement> runAsyncReplacements = Maps.create();

  private List<Integer> splitPointInitialSequence = Lists.create();

  private final Map<JMethod, JMethod> staticToInstanceMap = new IdentityHashMap<JMethod, JMethod>();

  private final Map<String, JStringLiteral> stringLiteralMap = new HashMap<String, JStringLiteral>();

  private final SourceInfo stringPoolSourceInfo;

  private JClassType typeClass;

  private Map<JReferenceType, Integer> typeIdMap = new HashMap<JReferenceType, Integer>();

  private JInterfaceType typeJavaIoSerializable;

  private JInterfaceType typeJavaLangCloneable;

  private JClassType typeJavaLangEnum;

  private JClassType typeJavaLangObject;

  private final Map<String, JDeclaredType> typeNameMap = new HashMap<String, JDeclaredType>();

  private JNonNullType typeNonNullString;

  private JClassType typeSpecialClassLiteralHolder;

  private JClassType typeSpecialJavaScriptObject;

  private JClassType typeString;

  public JProgram() {
    this(new CorrelationFactory.DummyCorrelationFactory());
  }

  /**
   * Constructor.
   * 
   * @param correlator Controls whether or not SourceInfo nodes created via the
   *          JProgram will record descendant information. Enabling this feature
   *          will collect extra data during the compilation cycle, but at a
   *          cost of memory and object allocations.
   */
  public JProgram(CorrelationFactory correlator) {
    super(correlator.makeSourceInfo(SourceOrigin.create(0,
        JProgram.class.getName())));

    this.correlator = correlator;
    intrinsic = createSourceInfo(0, getClass().getName());

    stringPoolSourceInfo = createLiteralSourceInfo("String pool",
        Literal.STRING);
  }

  public void addEntryMethod(JMethod entryPoint) {
    addEntryMethod(entryPoint, 0);
  }

  public void addEntryMethod(JMethod entryPoint, int fragmentNumber) {
    assert entryPoint.isStatic();
    while (fragmentNumber >= entryMethods.size()) {
      entryMethods.add(new ArrayList<JMethod>());
    }
    List<JMethod> methods = entryMethods.get(fragmentNumber);
    if (!methods.contains(entryPoint)) {
      methods.add(entryPoint);
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
    JBinaryOperation assign = new JBinaryOperation(info, lhs.getType(),
        JBinaryOperator.ASG, lhs, rhs);
    return assign.makeStatement();
  }

  public JClassType createClass(SourceInfo info, char[][] name,
      boolean isAbstract, boolean isFinal) {
    String sname = dotify(name);
    JClassType x = new JClassType(info, sname, isAbstract, isFinal);

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
        typeNonNullString = getNonNullType(x);
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
    JEnumType x = new JEnumType(info, sname);
    x.setSuperClass(getTypeJavaLangEnum());

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
    JEnumField x = new JEnumField(info, sname, ordinal, enclosingType, type);
    enclosingType.addField(x);
    return x;
  }

  public JField createField(SourceInfo info, char[] name,
      JDeclaredType enclosingType, JType type, boolean isStatic,
      Disposition disposition) {
    assert (name != null);
    assert (enclosingType != null);
    assert (type != null);

    String sname = String.valueOf(name);
    JField x = new JField(info, sname, enclosingType, type, isStatic,
        disposition);

    if (indexedTypes.containsValue(enclosingType)) {
      indexedFields.put(enclosingType.getShortName() + '.' + sname, x);
    }

    enclosingType.addField(x);
    return x;
  }

  public JInterfaceType createInterface(SourceInfo info, char[][] name) {
    String sname = dotify(name);
    JInterfaceType x = new JInterfaceType(info, sname);

    allTypes.add(x);
    putIntoTypeMap(sname, x);

    if (INDEX_TYPES_SET.contains(sname)) {
      indexedTypes.put(x.getShortName(), x);
      if (sname.equals("java.lang.Cloneable")) {
        typeJavaLangCloneable = x;
      } else if (sname.equals("java.io.Serializable")) {
        typeJavaIoSerializable = x;
      }
    }

    return x;
  }

  public JLocal createLocal(SourceInfo info, char[] name, JType type,
      boolean isFinal, JMethodBody enclosingMethodBody) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethodBody != null);

    JLocal x = new JLocal(info, String.valueOf(name), type, isFinal,
        enclosingMethodBody);

    enclosingMethodBody.addLocal(x);
    return x;
  }

  public JMethod createMethod(SourceInfo info, char[] name,
      JDeclaredType enclosingType, JType returnType, boolean isAbstract,
      boolean isStatic, boolean isFinal, boolean isPrivate, boolean isNative) {
    String sname = String.valueOf(name);
    assert (sname != null);
    assert (enclosingType != null);
    assert (returnType != null);
    assert (!isAbstract || !isNative);
    JMethod x = new JMethod(info, sname, enclosingType, returnType, isAbstract,
        isStatic, isFinal, isPrivate);
    if (isNative) {
      x.setBody(new JsniMethodBody(this, info));
    } else if (!isAbstract) {
      x.setBody(new JMethodBody(info));
    }

    if (!isPrivate && indexedTypes.containsValue(enclosingType)) {
      indexedMethods.put(enclosingType.getShortName() + '.' + sname, x);
    }

    enclosingType.addMethod(x);
    return x;
  }

  public JParameter createParameter(SourceInfo info, char[] name, JType type,
      boolean isFinal, boolean isThis, JMethod enclosingMethod) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethod != null);

    JParameter x = new JParameter(info, String.valueOf(name), type, isFinal,
        isThis, enclosingMethod);

    enclosingMethod.addParam(x);
    return x;
  }

  /**
   * Create a SourceInfo object when the source is derived from a physical
   * location.
   */
  public SourceInfo createSourceInfo(int startPos, int endPos, int startLine,
      String fileName) {
    return correlator.makeSourceInfo(SourceOrigin.create(startPos, endPos,
        startLine, fileName));
  }

  /**
   * Create a SourceInfo object when the source is derived from a physical
   * location.
   */
  public SourceInfo createSourceInfo(int startLine, String fileName) {
    return correlator.makeSourceInfo(SourceOrigin.create(startLine, fileName));
  }

  /**
   * Create a SourceInfo object when the source is created by the compiler
   * itself.
   */
  public SourceInfo createSourceInfoSynthetic(Class<?> caller,
      String description) {
    return createSourceInfo(0, caller.getName()).makeChild(caller, description);
  }

  /**
   * Return the least upper bound of a set of types. That is, the smallest type
   * that is a supertype of all the input types.
   */
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
   * Return the least upper bound of two types. That is, the smallest type that
   * is a supertype of both types.
   */
  public JReferenceType generalizeTypes(JReferenceType type1,
      JReferenceType type2) {
    if (type1 == type2) {
      return type1;
    }

    if (type1 instanceof JNonNullType && type2 instanceof JNonNullType) {
      // Neither can be null.
      type1 = type1.getUnderlyingType();
      type2 = type2.getUnderlyingType();
      return getNonNullType(generalizeTypes(type1, type2));
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
          type1 = type1.getSuperClass();
        }

        for (; distance1 < distance2; --distance2) {
          type2 = type2.getSuperClass();
        }

        while (type1 != type2) {
          type1 = type1.getSuperClass();
          type2 = type2.getSuperClass();
        }

        return type1;
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
   * Returns a sorted set of array types, so the returned set can be iterated
   * over without introducing nondeterminism.
   */
  public Set<JArrayType> getAllArrayTypes() {
    return allArrayTypes;
  }

  public List<JMethod> getAllEntryMethods() {
    List<JMethod> allEntryMethods = new ArrayList<JMethod>();
    for (List<JMethod> entries : entryMethods) {
      allEntryMethods.addAll(entries);
    }
    return allEntryMethods;
  }

  public CorrelationFactory getCorrelator() {
    return correlator;
  }

  public List<JDeclaredType> getDeclaredTypes() {
    return allTypes;
  }

  public int getEntryCount(int fragment) {
    return entryMethods.get(fragment).size();
  }

  public JThisRef getExprThisRef(SourceInfo info, JClassType enclosingType) {
    return new JThisRef(info, getNonNullType(enclosingType));
  }

  public int getFragmentCount() {
    return entryMethods.size();
  }

  public JDeclaredType getFromTypeMap(String qualifiedBinaryOrSourceName) {
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

  public Collection<JMethod> getIndexedMethods() {
    return Collections.unmodifiableCollection(indexedMethods.values());
  }

  public JDeclaredType getIndexedType(String string) {
    JDeclaredType type = indexedTypes.get(string);
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

  public JExpression getLiteralAbsentArrayDimension() {
    return JAbsentArrayDimension.INSTANCE;
  }

  public JBooleanLiteral getLiteralBoolean(boolean value) {
    return JBooleanLiteral.get(value);
  }

  public JCharLiteral getLiteralChar(char value) {
    return JCharLiteral.get(value);
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
      JField field = new JField(info, type.getJavahSignatureName()
          + "_classLit", typeSpecialClassLiteralHolder, getTypeJavaLangClass(),
          true, Disposition.FINAL);
      typeSpecialClassLiteralHolder.addField(field);

      // Initialize the field.
      JFieldRef fieldRef = new JFieldRef(info, null, field,
          typeSpecialClassLiteralHolder);
      JDeclarationStatement decl = new JDeclarationStatement(info, fieldRef,
          alloc);
      JMethodBody clinitBody = (JMethodBody) typeSpecialClassLiteralHolder.getMethods().get(
          0).getBody();
      clinitBody.getBlock().addStmt(decl);

      SourceInfo literalInfo = createSourceInfoSynthetic(JProgram.class,
          "class literal for " + type.getName());
      literalInfo.addCorrelation(correlator.by(Literal.CLASS));
      classLiteral = new JClassLiteral(literalInfo, type, field);
      classLiterals.put(type, classLiteral);
    } else {
      // Make sure the field hasn't been pruned.
      JField field = classLiteral.getField();
      if (optimizationsStarted
          && !field.getEnclosingType().getFields().contains(field)) {
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
    return new JClassSeed(createSourceInfoSynthetic(JProgram.class,
        "class seed"), type, getTypeJavaLangObject());
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
    JStringLiteral toReturn = stringLiteralMap.get(s);
    if (toReturn == null) {
      toReturn = new JStringLiteral(stringPoolSourceInfo.makeChild(
          JProgram.class, "String literal: " + s), s, typeNonNullString);
      stringLiteralMap.put(s, toReturn);
    }
    toReturn.getSourceInfo().merge(sourceInfo);
    return toReturn;
  }

  public JNonNullType getNonNullType(JReferenceType type) {
    if (type instanceof JNonNullType) {
      return (JNonNullType) type;
    }
    JNonNullType nonNullType = nonNullTypes.get(type);
    if (nonNullType == null) {
      nonNullType = new JNonNullType(type);
      nonNullTypes.put(type, nonNullType);
    }
    return nonNullType;
  }

  public JField getNullField() {
    if (nullField == null) {
      nullField = new JField(createSourceInfoSynthetic(JProgram.class,
          "Null field"), "nullField", null, JNullType.INSTANCE, false,
          Disposition.FINAL);
    }
    return nullField;
  }

  public JMethod getNullMethod() {
    if (nullMethod == null) {
      nullMethod = new JMethod(createSourceInfoSynthetic(JProgram.class,
          "Null method"), "nullMethod", null, JNullType.INSTANCE, false, false,
          true, true);
    }
    return nullMethod;
  }

  public int getQueryId(JReferenceType elementType) {
    assert (elementType == getRunTimeType(elementType));
    Integer integer = queryIds.get(elementType);
    if (integer == null) {
      return 0;
    }

    return integer.intValue();
  }

  public Map<Integer, RunAsyncReplacement> getRunAsyncReplacements() {
    return runAsyncReplacements;
  }

  /**
   * A run-time type is a type at the granularity that GWT tests at run time.
   * These include declared types, arrays of declared types, arrays of
   * primitives, and null. This is also the granularity for the notion of
   * instantiability recorded in {@link JTypeOracle}. This method returns the
   * narrowest supertype of <code>type</code> that is a run-time type.
   */
  public JReferenceType getRunTimeType(JReferenceType type) {
    type = type.getUnderlyingType();
    if (type instanceof JArrayType) {
      JArrayType typeArray = (JArrayType) type;
      if (typeArray.getLeafType() instanceof JNonNullType) {
        JNonNullType leafType = (JNonNullType) typeArray.getLeafType();
        type = getTypeArray(leafType.getUnderlyingType(), typeArray.getDims());
      }
    }
    return type;
  }

  public List<Integer> getSplitPointInitialSequence() {
    return splitPointInitialSequence;
  }

  public JMethod getStaticImpl(JMethod method) {
    return instanceToStaticMap.get(method);
  }

  public JArrayType getTypeArray(JType leafType, int dimensions) {
    assert (!(leafType instanceof JArrayType));
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
      JType elementType;
      if (dimensions == 1) {
        elementType = leafType;
      } else {
        elementType = getTypeArray(leafType, dimensions - 1);
      }
      arrayType = new JArrayType(elementType, leafType, dimensions);
      arrayType.setSuperClass(typeJavaLangObject);
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

  /**
   * Returns the JType corresponding to a JSNI type reference.
   */
  public JType getTypeFromJsniRef(String className) {
    int dim = 0;
    while (className.endsWith("[]")) {
      dim++;
      className = className.substring(0, className.length() - 2);
    }

    JType type;
    if ("Z".equals(className)) {
      type = getTypePrimitiveBoolean();
    } else if ("B".equals(className)) {
      type = getTypePrimitiveByte();
    } else if ("C".equals(className)) {
      type = getTypePrimitiveChar();
    } else if ("D".equals(className)) {
      type = getTypePrimitiveDouble();
    } else if ("F".equals(className)) {
      type = getTypePrimitiveFloat();
    } else if ("I".equals(className)) {
      type = getTypePrimitiveInt();
    } else if ("J".equals(className)) {
      type = getTypePrimitiveLong();
    } else if ("S".equals(className)) {
      type = getTypePrimitiveShort();
    } else if ("V".equals(className)) {
      type = getTypeVoid();
    } else {
      type = getFromTypeMap(className);
    }

    if (type == null || dim == 0) {
      return type;
    } else {
      return getTypeArray(type, dim);
    }
  }

  public int getTypeId(JReferenceType referenceType) {
    assert (referenceType == getRunTimeType(referenceType));
    Integer integer = typeIdMap.get(referenceType);
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

  public JPrimitiveType getTypeVoid() {
    return JPrimitiveType.VOID;
  }

  public void initTypeInfo(List<JReferenceType> types,
      List<JsonObject> jsonObjects) {
    for (int i = 0, c = types.size(); i < c; ++i) {
      typeIdMap.put(types.get(i), Integer.valueOf(i));
    }
    this.jsonTypeTable = jsonObjects;
  }

  public boolean isJavaLangString(JType type) {
    return type == typeString || type == typeNonNullString;
  }

  public boolean isJavaScriptObject(JType type) {
    if (type instanceof JReferenceType && typeSpecialJavaScriptObject != null) {
      return typeOracle.canTriviallyCast((JReferenceType) type,
          typeSpecialJavaScriptObject);
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
    return lastFragmentLoadingBefore(splitPointInitialSequence,
        entryMethods.size() - 1, firstFragment, restFragments);
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

  public void recordQueryIds(Map<JReferenceType, Integer> queryIds) {
    this.queryIds = queryIds;
  }

  public void setRunAsyncReplacements(Map<Integer, RunAsyncReplacement> map) {
    assert runAsyncReplacements.isEmpty();
    runAsyncReplacements = map;
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

    if (type1 instanceof JNonNullType != type2 instanceof JNonNullType) {
      // If either is non-nullable, the result should be non-nullable.
      return strongerType(getNonNullType(type1), getNonNullType(type2));
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
    while ((type = type.getSuperClass()) != null) {
      ++count;
    }
    return count;
  }

  private SourceInfo createLiteralSourceInfo(String description) {
    return intrinsic.makeChild(getClass(), description);
  }

  private SourceInfo createLiteralSourceInfo(String description, Literal literal) {
    SourceInfo child = createLiteralSourceInfo(description);
    child.addCorrelation(correlator.by(literal));
    return child;
  }

  /**
   * See notes in {@link #writeObject(ObjectOutputStream)}.
   * 
   * @see #writeObject(ObjectOutputStream)
   */
  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream stream) throws IOException,
      ClassNotFoundException {
    allTypes = (List<JDeclaredType>) stream.readObject();
    for (JDeclaredType type : allTypes) {
      type.readMembers(stream);
    }
    for (JDeclaredType type : allTypes) {
      type.readMethodBodies(stream);
    }
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
    stream.writeObject(allTypes);
    for (JDeclaredType type : allTypes) {
      type.writeMembers(stream);
    }
    for (JDeclaredType type : allTypes) {
      type.writeMethodBodies(stream);
    }
    stream.defaultWriteObject();
  }
}
