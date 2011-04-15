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
package com.google.gwt.tools.apichecker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates an API class.
 */
final class ApiClass implements Comparable<ApiClass>, ApiElement {
  /**
   * Enum for indexing the common storage used for methods and constructors
   * 
   */
  public static enum MethodType {
    CONSTRUCTOR, METHOD;
  }

  private HashMap<String, ApiField> apiFields = null;

  /**
   * TODO (amitmanjhi): Toby felt that combining structures for storing
   * MethodType and Constructors was unnecessary. In particular, the hashMap of
   * name#args -> object is meaningless for constructor, since name is empty for
   * constructors. Make it separate.
   * 
   * In addition, the current method fails when constructors or methods accept
   * variable arguments. In future, just index everything by name [and not add
   * the number of arguments that they accept].
   */

  /**
   * 2 entries in the list: one for CONSTRUCTOR, and the other for METHOD. Each
   * entry is a mapping from MethodName#args to a set of ApiAbstractMethod
   */
  private EnumMap<MethodType, Map<String, Set<ApiAbstractMethod>>> apiMembersByName = null;

  private final ApiPackage apiPackage;
  private final JClassType classType;

  private final boolean isInstantiableApiClass;
  private final boolean isNotsubclassableApiClass;
  private final boolean isSubclassableApiClass;
  private final TreeLogger logger;

  ApiClass(JClassType classType, ApiPackage apiPackage) {
    this.classType = classType;
    this.apiPackage = apiPackage;
    logger = apiPackage.getApiContainer().getLogger();
    ApiContainer apiContainer = apiPackage.getApiContainer();
    isSubclassableApiClass = apiContainer.isSubclassableApiClass(classType);
    isNotsubclassableApiClass = apiContainer.isNotsubclassableApiClass(classType);
    isInstantiableApiClass = apiContainer.isInstantiableApiClass(classType);
  }

  public int compareTo(ApiClass other) {
    return getName().compareTo(other.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ApiClass)) {
      return false;
    }
    return this.getName().equals(((ApiClass) o).getName());
  }

  public String getRelativeSignature() {
    return classType.getQualifiedSourceName();
  }

  @Override
  public int hashCode() {
    return this.getName().hashCode();
  }

  @Override
  public String toString() {
    return classType.toString();
  }

  String getApiAsString() {
    StringBuffer sb = new StringBuffer();
    sb.append("\t" + getName() + "\n");
    if (apiFields != null) {
      ArrayList<ApiField> apiFieldsList = new ArrayList<ApiField>(apiFields.values());
      Collections.sort(apiFieldsList);
      for (ApiField apiField : apiFieldsList) {
        sb.append("\t\t" + apiField.getRelativeSignature() + "\n");
      }
    }
    if (apiMembersByName != null && apiMembersByName.get(MethodType.METHOD) != null) {
      for (MethodType method : MethodType.values()) {
        HashSet<ApiAbstractMethod> apiMethodsSet = new HashSet<ApiAbstractMethod>();
        for (Set<ApiAbstractMethod> methodsSets : apiMembersByName.get(method).values()) {
          apiMethodsSet.addAll(methodsSets);
        }
        ArrayList<ApiAbstractMethod> apiMethodsList =
            new ArrayList<ApiAbstractMethod>(apiMethodsSet);
        Collections.sort(apiMethodsList);
        for (ApiAbstractMethod apiMethod : apiMethodsList) {
          sb.append("\t\t" + apiMethod.getRelativeSignature() + "\n");
        }
      }
    }
    return sb.toString();
  }

  ApiField getApiFieldByName(String name) {
    return apiFields.get(name);
  }

  Set<String> getApiFieldNames() {
    if (apiFields == null) {
      initializeApiFields();
    }
    return new HashSet<String>(apiFields.keySet());
  }

  Set<ApiField> getApiFieldsBySet(Set<String> names) {
    Set<ApiField> ret = new HashSet<ApiField>();
    for (String name : names) {
      ret.add(apiFields.get(name));
    }
    return ret;
  }

  Set<String> getApiMemberNames(MethodType type) {
    if (apiMembersByName == null) {
      initializeApiConstructorsAndMethods();
    }
    return new HashSet<String>(apiMembersByName.get(type).keySet());
  }

  Set<ApiAbstractMethod> getApiMembersBySet(Set<String> methodNames, MethodType type) {
    Map<String, Set<ApiAbstractMethod>> current = apiMembersByName.get(type);
    Set<ApiAbstractMethod> tempMethods = new HashSet<ApiAbstractMethod>();
    for (String methodName : methodNames) {
      tempMethods.addAll(current.get(methodName));
    }
    return tempMethods;
  }

  Set<ApiAbstractMethod> getApiMethodsByName(String name, MethodType type) {
    return apiMembersByName.get(type).get(name);
  }

  JClassType getClassObject() {
    return classType;
  }

  String getFullName() {
    return classType.getQualifiedSourceName();
  }

  /**
   * compute the modifier changes. check for: (i) added 'final' or 'abstract' or
   * 'static' (ii) removed 'static' or 'non-abstract class made into interface'
   * (if a non-abstract class is made into interface, the client class/interface
   * inheriting from it would need to change)
   */
  List<ApiChange.Status> getModifierChanges(ApiClass newClass) {
    JClassType newClassType = newClass.getClassObject();
    List<ApiChange.Status> statuses = new ArrayList<ApiChange.Status>(5);

    // check for addition of 'final', 'abstract', 'static'
    if (!classType.isFinal() && newClassType.isFinal()) {
      statuses.add(ApiChange.Status.FINAL_ADDED);
    }
    if (!classType.isAbstract() && newClassType.isAbstract()) {
      statuses.add(ApiChange.Status.ABSTRACT_ADDED);
    }
    if (!classType.isStatic() && newClassType.isStatic()) {
      statuses.add(ApiChange.Status.STATIC_ADDED);
    }

    // removed 'static'
    if (classType.isStatic() && !newClassType.isStatic()) {
      statuses.add(ApiChange.Status.STATIC_REMOVED);
    }

    if (!classType.isAbstract() && (newClassType.isInterface() != null)) {
      statuses.add(ApiChange.Status.NONABSTRACT_CLASS_MADE_INTERFACE);
    }
    if (apiPackage.getApiContainer().isSubclassableApiClass(classType)) {
      if ((classType.isClass() != null) && (newClassType.isInterface() != null)) {
        statuses.add(ApiChange.Status.SUBCLASSABLE_API_CLASS_MADE_INTERFACE);
      }
      if ((classType.isInterface() != null) && (newClassType.isClass() != null)) {
        statuses.add(ApiChange.Status.SUBCLASSABLE_API_INTERFACE_MADE_CLASS);
      }
    }
    return statuses;
  }

  String getName() {
    return classType.getName();
  }

  ApiPackage getPackage() {
    return apiPackage;
  }

  void initializeApiFields() {
    apiFields = new HashMap<String, ApiField>();
    List<String> notAddedFields = new ArrayList<String>();
    JField fields[] = getAccessibleFields();
    for (JField field : fields) {
      if (isApiMember(field)) {
        apiFields.put(field.getName(), new ApiField(field, this));
      } else {
        notAddedFields.add(field.toString());
      }
    }
    if (notAddedFields.size() > 0) {
      logger.log(TreeLogger.SPAM, "class " + getName() + " " + ", not adding "
          + notAddedFields.size() + " nonApi fields: " + notAddedFields, null);
    }
  }

  boolean isSubclassableApiClass() {
    return isSubclassableApiClass;
  }

  private JField[] getAccessibleFields() {
    Map<String, JField> fieldsBySignature = new HashMap<String, JField>();
    JClassType tempClassType = classType;
    do {
      JField declaredFields[] = tempClassType.getFields();
      for (JField field : declaredFields) {
        if (field.isPrivate()) {
          continue;
        }
        String signature = field.toString();
        JField existing = fieldsBySignature.put(signature, field);
        if (existing != null) {
          // TODO(amitmanjhi): Consider whether this is sufficient
          fieldsBySignature.put(signature, existing);
        }
      }
      tempClassType = tempClassType.getSuperclass();
    } while (tempClassType != null);
    return fieldsBySignature.values().toArray(new JField[0]);
  }

  // TODO(amitmanjhi): to optimize, cache results
  private JMethod[] getAccessibleMethods() {
    boolean isInterface = false;
    if (classType.isInterface() != null) {
      isInterface = true;
    }
    Map<String, JMethod> methodsBySignature = new HashMap<String, JMethod>();
    LinkedList<JClassType> classesToBeProcessed = new LinkedList<JClassType>();
    classesToBeProcessed.add(classType);
    JClassType tempClassType = null;
    while (classesToBeProcessed.peek() != null) {
      tempClassType = classesToBeProcessed.remove();
      JMethod declaredMethods[] = tempClassType.getMethods();
      for (JMethod method : declaredMethods) {
        if (method.isPrivate()) {
          continue;
        }
        String signature = ApiAbstractMethod.computeInternalSignature(method);
        JMethod existing = methodsBySignature.put(signature, method);
        if (existing != null) {
          // decide which implementation to keep
          if (existing.getEnclosingType().isAssignableTo(method.getEnclosingType())) {
            methodsBySignature.put(signature, existing);
          }
        }
      }
      if (isInterface) {
        classesToBeProcessed.addAll(Arrays.asList(tempClassType.getImplementedInterfaces()));
      } else {
        classesToBeProcessed.add(tempClassType.getSuperclass());
      }
    }
    return methodsBySignature.values().toArray(new JMethod[0]);
  }

  private JAbstractMethod[] getAccessibleMethods(MethodType member) {
    switch (member) {
      case CONSTRUCTOR:
        return classType.getConstructors();
      case METHOD:
        return getAccessibleMethods();
    }
    throw new AssertionError("Unknown value : " + member);
  }

  private void initializeApiConstructorsAndMethods() {
    apiMembersByName =
        new EnumMap<MethodType, Map<String, Set<ApiAbstractMethod>>>(MethodType.class);
    for (MethodType method : MethodType.values()) {
      apiMembersByName.put(method, new HashMap<String, Set<ApiAbstractMethod>>());
      Map<String, Set<ApiAbstractMethod>> pointer = apiMembersByName.get(method);
      List<String> notAddedMembers = new ArrayList<String>();
      JAbstractMethod jams[] = getAccessibleMethods(method);
      for (JAbstractMethod jam : jams) {
        if (isApiMember(jam)) {
          String tempName = jam.getName() + jam.getParameters().length;
          Set<ApiAbstractMethod> existingMembers = pointer.get(tempName);
          if (existingMembers == null) {
            existingMembers = new HashSet<ApiAbstractMethod>();
          }
          switch (method) {
            case CONSTRUCTOR:
              existingMembers.add(new ApiConstructor(jam, this));
              break;
            case METHOD:
              existingMembers.add(new ApiMethod(jam, this));
              break;
            default:
              throw new AssertionError("Unknown memberType : " + method);
          }
          pointer.put(tempName, existingMembers);
        } else {
          notAddedMembers.add(jam.toString());
        }
      }
      if (notAddedMembers.size() > 0) {
        logger.log(TreeLogger.SPAM, "class " + getName() + ", removing " + notAddedMembers.size()
            + " nonApi members: " + notAddedMembers, null);
      }
    }
  }

  /**
   * Note: Instance members of a class that is not instantiable are not api
   * members.
   */
  private boolean isApiMember(final Object member) {
    boolean isPublic = false;
    boolean isPublicOrProtected = false;
    boolean isStatic = false;

    if (member instanceof JField) {
      JField field = (JField) member;
      isPublic = field.isPublic();
      isPublicOrProtected = isPublic || field.isProtected();
      isStatic = field.isStatic();
    }
    if (member instanceof JAbstractMethod) {
      JAbstractMethod method = (JAbstractMethod) member;
      isPublic = method.isPublic();
      isPublicOrProtected = isPublic || method.isProtected();
      if (method instanceof JMethod) {
        JMethod temp = (JMethod) method;
        isStatic = temp.isStatic();
      } else {
        isStatic = false; // constructors can't be static
      }
    }
    if (ApiCompatibilityChecker.REMOVE_NON_SUBCLASSABLE_ABSTRACT_CLASS_FROM_API) {
      if (!isInstantiableApiClass && !isStatic && !isSubclassableApiClass) {
        return false;
      }
    }
    return (isSubclassableApiClass && isPublicOrProtected)
        || (isNotsubclassableApiClass && isPublic);
  }

}
