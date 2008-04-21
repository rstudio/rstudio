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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Encapsulates an API class.
 */
public class ApiClass {
  /**
   * Enum for indexing the common storage used for methods and constructors
   * 
   */
  public static enum MethodType {
    CONSTRUCTOR(0), METHOD(1);

    private final int id;

    MethodType(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }
  }

  public static String computeFieldApiSignature(JField field) {
    return field.getEnclosingType().getQualifiedSourceName() + "::"
        + field.getName();
  }

  /**
   * Assumption: Clients may sub-class an API class, but they will not add their
   * class to the package.
   * 
   * Notes: -- A class with only private constructors can be an Api class.
   */
  public static boolean isApiClass(JClassType classType) {
    // check for outer classes
    if (isPublicOuterClass(classType)) {
      return true;
    }
    // if classType is not a member type, return false
    if (!classType.isMemberType()) {
      return false;
    }
    JClassType enclosingType = classType.getEnclosingType();
    if (classType.isPublic()) {
      return isApiClass(enclosingType) || isAnySubtypeAnApiClass(enclosingType);
    }
    if (classType.isProtected()) {
      return isSubclassableApiClass(enclosingType)
          || isAnySubtypeASubclassableApiClass(enclosingType);
    }
    return false;
  }

  public static boolean isInstantiableApiClass(JClassType classType) {
    return !classType.isAbstract()
        && hasPublicOrProtectedConstructor(classType);
  }

  public static boolean isNotsubclassableApiClass(JClassType classType) {
    return isApiClass(classType) && !isSubclassable(classType);
  }

  /**
   * @return returns true if classType is public AND an outer class
   */
  public static boolean isPublicOuterClass(JClassType classType) {
    return classType.isPublic() && !classType.isMemberType();
  }

  public static boolean isSubclassable(JClassType classType) {
    return !classType.isFinal() && hasPublicOrProtectedConstructor(classType);
  }

  public static boolean isSubclassableApiClass(JClassType classType) {
    return isApiClass(classType) && isSubclassable(classType);
  }

  private static boolean hasPublicOrProtectedConstructor(JClassType classType) {
    JConstructor[] constructors = classType.getConstructors();
    for (JConstructor constructor : constructors) {
      if (constructor.isPublic() || constructor.isProtected()) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAnySubtypeAnApiClass(JClassType classType) {
    JClassType subTypes[] = classType.getSubtypes();
    for (JClassType tempType : subTypes) {
      if (isApiClass(tempType)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAnySubtypeASubclassableApiClass(JClassType classType) {
    JClassType subTypes[] = classType.getSubtypes();
    for (JClassType tempType : subTypes) {
      if (isSubclassableApiClass(tempType)) {
        return true;
      }
    }
    return false;
  }

  private HashMap<String, JField> apiFields = null;

  /**
   * 2 entries in the list: one for CONSTRUCTOR, and the other for METHOD. Each
   * entry is a mapping from MethodName#args to a set of ApiAbstractMethod
   */
  private ArrayList<HashMap<String, HashSet<ApiAbstractMethod>>> apiMembersByName = new ArrayList<HashMap<String, HashSet<ApiAbstractMethod>>>(
      MethodType.values().length);

  private ApiPackage apiPackage = null;

  private JClassType classType = null;

  private boolean isInstantiableApiClass = false;

  private boolean isNotsubclassableApiClass = false;
  private boolean isSubclassableApiClass = false;
  private TreeLogger logger = null;

  public ApiClass(JClassType classType, ApiPackage apiPackage) {
    this.classType = classType;
    this.apiPackage = apiPackage;
    logger = apiPackage.getApiContainer().getLogger();
    isSubclassableApiClass = isSubclassableApiClass(classType);
    isNotsubclassableApiClass = isNotsubclassableApiClass(classType);
    isInstantiableApiClass = isInstantiableApiClass(classType);
  }

  public <E> String computeRelativeSignature(E element) {
    String signature = element.toString();
    JClassType enclosingType = null;
    if (element instanceof JField) {
      JField field = (JField) element;
      signature = field.getName();
      enclosingType = field.getEnclosingType();
    }
    if (element instanceof JAbstractMethod) {
      JAbstractMethod jam = (JAbstractMethod) element;
      signature = ApiAbstractMethod.computeInternalSignature(jam);
      enclosingType = jam.getEnclosingType();
    }
    if (ApiCompatibilityChecker.DEBUG) {
      return classType.getQualifiedSourceName()
          + "::"
          + signature
          + " defined in "
          + (enclosingType == null ? "null enclosing type "
              : enclosingType.getQualifiedSourceName());
    }
    return classType.getQualifiedSourceName() + "::" + signature;
  }

  public JField getApiFieldByName(String name) {
    return apiFields.get(name);
  }

  public HashSet<String> getApiFieldNames() {
    if (apiFields == null) {
      initializeApiFields();
    }
    return new HashSet<String>(apiFields.keySet());
  }

  public HashSet<JField> getApiFieldsBySet(HashSet<String> names) {
    HashSet<JField> ret = new HashSet<JField>();
    String tempStrings[] = names.toArray(new String[0]);
    for (String temp : tempStrings) {
      ret.add(apiFields.get(temp));
    }
    return ret;
  }

  public HashSet<String> getApiMemberNames(MethodType type) {
    if (apiMembersByName.size() == 0) {
      initializeApiConstructorsAndFields();
    }
    return new HashSet<String>(apiMembersByName.get(type.getId()).keySet());
  }

  public HashSet<ApiAbstractMethod> getApiMembersBySet(
      HashSet<String> methodNames, MethodType type) {
    Iterator<String> iteratorString = methodNames.iterator();
    HashMap<String, HashSet<ApiAbstractMethod>> current = apiMembersByName.get(type.getId());
    HashSet<ApiAbstractMethod> tempMethods = new HashSet<ApiAbstractMethod>();
    while (iteratorString.hasNext()) {
      tempMethods.addAll(current.get(iteratorString.next()));
    }
    return tempMethods;
  }

  public HashSet<ApiAbstractMethod> getApiMethodsByName(String name,
      MethodType type) {
    return apiMembersByName.get(type.getId()).get(name);
  }

  public JClassType getClassObject() {
    return classType;
  }

  public String getFullName() {
    return classType.getQualifiedSourceName();
  }

  /**
   * compute the modifier changes. check for: (i) added 'final' or 'abstract' or
   * 'static' (ii) removed 'static' or 'non-abstract class made into interface'
   * (if a non-abstract class is made into interface, the client class/interface
   * inheriting from it would need to change)
   */
  public ArrayList<ApiChange.Status> getModifierChanges(ApiClass newClass) {
    JClassType newClassType = newClass.getClassObject();
    ArrayList<ApiChange.Status> statuses = new ArrayList<ApiChange.Status>(5);

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
    if (isSubclassableApiClass(classType)) {
      if ((classType.isClass() != null) && (newClassType.isInterface() != null)) {
        statuses.add(ApiChange.Status.SUBCLASSABLE_API_CLASS_MADE_INTERFACE);
      }
      if ((classType.isInterface() != null) && (newClassType.isClass() != null)) {
        statuses.add(ApiChange.Status.SUBCLASSABLE_API_INTERFACE_MADE_CLASS);
      }
    }
    return statuses;
  }

  public String getName() {
    return classType.getName();
  }

  public ApiPackage getPackage() {
    return apiPackage;
  }

  public void initializeApiFields() {
    apiFields = new HashMap<String, JField>();
    ArrayList<String> notAddedFields = new ArrayList<String>();
    JField fields[] = getAccessibleFields();
    for (JField field : fields) {
      if (isApiMember(field)) {
        apiFields.put(computeFieldApiSignature(field), field);
      } else {
        notAddedFields.add(field.toString());
      }
    }
    if (notAddedFields.size() > 0) {
      logger.log(TreeLogger.SPAM, "class " + getName() + " " + ", not adding "
          + notAddedFields.size() + " nonApi fields: " + notAddedFields, null);
    }
  }

  @Override
  public String toString() {
    return classType.toString();
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

  private JAbstractMethod[] getAccessibleMembers(MethodType member) {
    switch (member) {
      case CONSTRUCTOR:
        return classType.getConstructors();
      case METHOD:
        return getAccessibleMethods();
    }
    throw new AssertionError("Unknown value : " + member.getId());
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
          if (existing.getEnclosingType().isAssignableTo(
              method.getEnclosingType())) {
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

  private void initializeApiConstructorsAndFields() {
    for (MethodType member : MethodType.values()) {
      apiMembersByName.add(member.getId(),
          new HashMap<String, HashSet<ApiAbstractMethod>>());
      HashMap<String, HashSet<ApiAbstractMethod>> pointer = apiMembersByName.get(member.getId());
      ArrayList<String> notAddedMembers = new ArrayList<String>();
      JAbstractMethod jams[] = getAccessibleMembers(member);
      for (JAbstractMethod jam : jams) {
        if (isApiMember(jam)) {
          String tempName = jam.getName() + jam.getParameters().length;
          HashSet<ApiAbstractMethod> existingMembers = pointer.get(tempName);
          if (existingMembers == null) {
            existingMembers = new HashSet<ApiAbstractMethod>();
          }
          switch (member) {
            case CONSTRUCTOR:
              existingMembers.add(new ApiConstructor(jam, this));
              break;
            case METHOD:
              existingMembers.add(new ApiMethod(jam, this));
              break;
            default:
              throw new AssertionError("Unknown memberType : " + member);
          }
          pointer.put(tempName, existingMembers);
        } else {
          notAddedMembers.add(jam.toString());
        }
      }
      if (notAddedMembers.size() > 0) {
        logger.log(TreeLogger.SPAM, "class " + getName() + ", removing "
            + notAddedMembers.size() + " nonApi members: " + notAddedMembers,
            null);
      }
    }
  }

  /**
   * Note: Instance members of a class that is not instantiable are not api
   * members.
   */
  private <E> boolean isApiMember(final E member) {
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
    if (ApiCompatibilityChecker.REMOVE_ABSTRACT_CLASS_FROM_API) {
      if (!isInstantiableApiClass && !isStatic) {
        return false;
      }
    }
    return (isSubclassableApiClass && isPublicOrProtected)
        || (isNotsubclassableApiClass && isPublic);
  }
}
