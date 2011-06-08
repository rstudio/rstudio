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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This class is used to compute type parameter exposure using a flow algorithm.
 */
class TypeParameterExposureComputer {
  /**
   * Helper class for type parameter flow information.
   */
  class TypeParameterFlowInfo {
    /**
     * The class that declares this type parameter.
     */
    private final JGenericType baseType;

    /**
     * The keys are the set of type parameters that, if exposed, cause this type
     * parameter to be exposed. The value for each key is the dimensionality
     * that the exposure will cause. If the key is exposed as an array, then the
     * dimensionality should be added to the dimensionality that the key is
     * already exposed as.
     */
    private final Map<TypeParameterFlowInfo, Integer> causesExposure =
        new LinkedHashMap<TypeParameterFlowInfo, Integer>();

    private int exposure = EXPOSURE_NONE;

    private final Map<TypeParameterFlowInfo, Boolean> isTransitivelyAffectedByCache =
        new HashMap<TypeParameterFlowInfo, Boolean>();

    /**
     * Type parameters that need to be notified when my exposure changes.
     */
    private final Set<TypeParameterFlowInfo> listeners = new LinkedHashSet<TypeParameterFlowInfo>();

    private boolean mightNotBeExposed = true;

    /**
     * Ordinal of this type parameter.
     */
    private final int ordinal;

    private boolean visited;

    TypeParameterFlowInfo(JGenericType baseType, int ordinal) {
      this.baseType = baseType;
      this.ordinal = ordinal;
    }

    public boolean checkDirectExposure() {
      boolean didChange = false;
      JClassType type = baseType;
      while (type != null) {
        // any problems should already have been captured by our caller, so we
        // make a throw-away ProblemReport here.
        if (SerializableTypeOracleBuilder.shouldConsiderFieldsForSerialization(type, typeFilter,
            new ProblemReport())) {
          JField[] fields = type.getFields();
          for (JField field : fields) {
            if (!SerializableTypeOracleBuilder.shouldConsiderForSerialization(TreeLogger.NULL,
                true, field)) {
              continue;
            }

            if (field.getType().getLeafType() == getTypeParameter()) {
              /*
               * If the type parameter is referenced explicitly or as the leaf
               * type of an array, then it will be considered directly exposed.
               */
              markExposedAsArray(0);
              mightNotBeExposed = false;
              didChange = true;

              JArrayType fieldTypeAsArray = field.getType().isArray();
              if (fieldTypeAsArray != null) {
                markExposedAsArray(fieldTypeAsArray.getRank());
              }
            }
          }
        }

        /*
         * Counting on substitution to propagate the type parameter.
         */
        type = type.getSuperclass();
      }

      return didChange;
    }

    public Map<TypeParameterFlowInfo, Integer> getCausesExposure() {
      return causesExposure;
    }

    public int getExposure() {
      return exposure;
    }

    public Set<TypeParameterFlowInfo> getListeners() {
      return listeners;
    }

    /**
     * Return whether it is possible for the parameter not to be exposed. For
     * example, if a class has one subclass that uses the parameter and another
     * that does not, then the parameter is exposed (exposure >=
     * <code>EXPOSURE_DIRECT</code>) but this method will return
     * <code>false</code>.
     */
    public boolean getMightNotBeExposed() {
      return mightNotBeExposed;
    }

    /**
     * Determine whether there is an infinite array exposure if this type
     * parameter is used in an array type which is then passed as an actual type
     * argument for the formal type parameter <code>other</code>.
     */
    public boolean infiniteArrayExpansionPathBetween(TypeParameterFlowInfo other) {
      Integer dimensionDelta = getCausesExposure().get(other);
      if (dimensionDelta == null) {
        return false;
      }
      return dimensionDelta > 0 && other.isTransitivelyAffectedBy(this);
    }

    @Override
    public String toString() {
      return getTypeParameter().getName() + " in " + baseType.getName();
    }

    public boolean updateFlowInfo() {
      boolean didChange = false;
      if (!wasVisited()) {
        didChange |= initializeExposure();
        markVisited();
      }

      for (Entry<TypeParameterFlowInfo, Integer> entry : getCausesExposure().entrySet()) {
        TypeParameterFlowInfo info2 = entry.getKey();
        int dimensionDelta = entry.getValue();
        if (info2.getExposure() >= 0) {
          if (!infiniteArrayExpansionPathBetween(info2)) {
            didChange |= markExposedAsArray(dimensionDelta + info2.getExposure());
          }
        }
      }

      return didChange;
    }

    void addListener(TypeParameterFlowInfo listener) {
      listeners.add(listener);
    }

    JTypeParameter getTypeParameter() {
      return baseType.getTypeParameters()[ordinal];
    }

    boolean initializeExposure() {
      computeIndirectExposureCauses();
      return checkDirectExposure();
    }

    boolean isTransitivelyAffectedBy(TypeParameterFlowInfo flowInfo) {
      Boolean result = isTransitivelyAffectedByCache.get(flowInfo);
      if (result != null) {
        return result;
      }

      HashSet<TypeParameterFlowInfo> affectedBy = new HashSet<TypeParameterFlowInfo>();
      Set<TypeParameterFlowInfo> affectedByWorklist = new LinkedHashSet<TypeParameterFlowInfo>();
      affectedByWorklist.add(this);

      result = false;
      while (!affectedByWorklist.isEmpty()) {
        TypeParameterFlowInfo currFlowInfo = affectedByWorklist.iterator().next();
        affectedByWorklist.remove(currFlowInfo);

        if (currFlowInfo == flowInfo) {
          result = true;
          break;
        }

        if (affectedBy.add(currFlowInfo)) {
          affectedByWorklist.addAll(currFlowInfo.getAffectedBy());
        }
      }

      isTransitivelyAffectedByCache.put(flowInfo, result);
      return result;
    }

    boolean markExposedAsArray(int dim) {
      if (exposure >= dim) {
        return false;
      }

      exposure = dim;
      return true;
    }

    void markVisited() {
      visited = true;
    }

    boolean wasVisited() {
      return visited;
    }

    private void computeIndirectExposureCauses() {
      // TODO(spoon): this only needs to consider immediate subtypes, not all
      // subtypes
      JClassType[] subtypes = baseType.getSubtypes();
      for (JClassType subtype : subtypes) {
        JGenericType isGeneric = subtype.isGenericType();
        if (isGeneric == null) {
          // Only generic types can cause a type parameter to be exposed
          continue;
        }

        // any problems should already have been captured by our caller, so we
        // make a throw-away ProblemReport here.
        if (!SerializableTypeOracleBuilder.shouldConsiderFieldsForSerialization(subtype,
            typeFilter, new ProblemReport())) {
          continue;
        }

        JParameterizedType asParameterizationOf = subtype.asParameterizationOf(baseType);
        Set<JTypeParameter> paramsUsed = new LinkedHashSet<JTypeParameter>();
        SerializableTypeOracleBuilder.recordTypeParametersIn(
            asParameterizationOf.getTypeArgs()[ordinal], paramsUsed);

        for (JTypeParameter paramUsed : paramsUsed) {
          recordCausesExposure(isGeneric, paramUsed.getOrdinal(), 0);
        }
      }

      JClassType type = baseType;
      while (type != null) {
        if (SerializableTypeOracleBuilder.shouldConsiderFieldsForSerialization(type, typeFilter,
            new ProblemReport())) {
          JField[] fields = type.getFields();
          for (JField field : fields) {
            if (!SerializableTypeOracleBuilder.shouldConsiderForSerialization(TreeLogger.NULL,
                true, field)) {
              continue;
            }

            JParameterizedType isParameterized = field.getType().getLeafType().isParameterized();
            if (isParameterized == null) {
              continue;
            }

            JClassType[] typeArgs = isParameterized.getTypeArgs();
            for (int i = 0; i < typeArgs.length; ++i) {
              if (referencesTypeParameter(typeArgs[i], getTypeParameter())) {
                JGenericType genericFieldType = isParameterized.getBaseType();
                recordCausesExposure(genericFieldType, i, 0);
                JArrayType typeArgIsArray = typeArgs[i].isArray();
                if (typeArgIsArray != null && typeArgIsArray.getLeafType() == getTypeParameter()) {
                  int dims = typeArgIsArray.getRank();
                  recordCausesExposure(genericFieldType, i, dims);
                }
              }
            }
          }
        }

        /*
         * Counting on substitution to propagate the type parameter.
         */
        type = type.getSuperclass();
      }
    }

    private Collection<? extends TypeParameterFlowInfo> getAffectedBy() {
      return causesExposure.keySet();
    }

    /**
     * The same as
     * {@link TypeParameterExposureComputer#getFlowInfo(JGenericType, int)},
     * except that it additionally adds <code>this</code> as a listener to the
     * returned flow info.
     */
    private TypeParameterFlowInfo getFlowInfo(JGenericType type, int index) {
      TypeParameterFlowInfo flowInfo = TypeParameterExposureComputer.this.getFlowInfo(type, index);
      flowInfo.addListener(this);
      return flowInfo;
    }

    private void recordCausesExposure(JGenericType type, int index, int level) {
      assert (index < type.getTypeParameters().length);
      TypeParameterFlowInfo flowInfo = getFlowInfo(type, index);
      Integer oldLevel = causesExposure.get(flowInfo);
      if (oldLevel == null || oldLevel < level) {
        causesExposure.put(flowInfo, level);
      }
    }

    private boolean referencesTypeParameter(JClassType classType, JTypeParameter typeParameter) {
      Set<JTypeParameter> typeParameters = new LinkedHashSet<JTypeParameter>();
      SerializableTypeOracleBuilder.recordTypeParametersIn(classType, typeParameters);
      return typeParameters.contains(typeParameter);
    }
  }

  /**
   * Type parameter is exposed.
   */
  static final int EXPOSURE_DIRECT = 0;

  /**
   * Type parameter is exposed as a bounded array. The value is the max bound of
   * the exposure.
   */
  static final int EXPOSURE_MIN_BOUNDED_ARRAY = EXPOSURE_DIRECT + 1;

  /**
   * Type parameter is not exposed.
   */
  static final int EXPOSURE_NONE = -1;

  private TypeFilter typeFilter;

  private final Map<JTypeParameter, TypeParameterFlowInfo> typeParameterToFlowInfo =
      new IdentityHashMap<JTypeParameter, TypeParameterFlowInfo>();

  private final Set<TypeParameterFlowInfo> worklist = new LinkedHashSet<TypeParameterFlowInfo>();

  TypeParameterExposureComputer(TypeFilter typeFilter) {
    this.typeFilter = typeFilter;
  }

  /**
   * Computes flow information for the specified type parameter. If it has
   * already been computed just return the value of the previous computation.
   * 
   * @param type the generic type whose type parameter flow we are interested in
   * @param index the index of the type parameter whose flow we want to compute
   */
  public TypeParameterFlowInfo computeTypeParameterExposure(JGenericType type, int index) {
    // check if it has already been computed
    JTypeParameter[] typeParameters = type.getTypeParameters();
    assert (index < typeParameters.length);
    JTypeParameter typeParameter = typeParameters[index];
    TypeParameterFlowInfo queryFlow = typeParameterToFlowInfo.get(typeParameter);
    if (queryFlow != null) {
      return queryFlow;
    }

    // not already computed; compute it
    queryFlow = getFlowInfo(type, index); // adds it to the work list as a
    // side effect

    while (!worklist.isEmpty()) {
      TypeParameterFlowInfo info = worklist.iterator().next();
      worklist.remove(info);

      boolean didChange = info.updateFlowInfo();

      if (didChange) {
        for (TypeParameterFlowInfo listener : info.getListeners()) {
          worklist.add(listener);
        }
      }
    }

    return queryFlow;
  }

  public void setTypeFilter(TypeFilter typeFilter) {
    this.typeFilter = typeFilter;
  }

  /**
   * Return the parameter flow info for a type parameter specified by class and
   * index. If the flow info did not previously exist, create it and add it to
   * the work list.
   */
  private TypeParameterFlowInfo getFlowInfo(JGenericType type, int index) {
    JTypeParameter typeParameter = type.getTypeParameters()[index];
    TypeParameterFlowInfo info = typeParameterToFlowInfo.get(typeParameter);
    if (info == null) {
      info = new TypeParameterFlowInfo(type, index);
      typeParameterToFlowInfo.put(typeParameter, info);
      worklist.add(info);
    }
    return info;
  }
}