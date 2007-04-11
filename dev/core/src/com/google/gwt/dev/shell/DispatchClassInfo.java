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
package com.google.gwt.dev.shell;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Helper class for dispatching methods to Java objects. It takes methods on
 * various Java classes and assigns DISPID's to them.
 */
public class DispatchClassInfo {

  private Class cls;

  private final int clsId;

  private ArrayList memberById;

  private HashMap memberIdByName;

  public DispatchClassInfo(Class cls, int classId) {
    this.cls = cls;
    clsId = classId;
  }

  public int getClassId() {
    return clsId;
  }

  public Member getMember(int id) {
    lazyInitTargetMembers();
    id &= 0xffff;
    return (Member) memberById.get(id);
  }

  public int getMemberId(String mangledMemberName) {
    lazyInitTargetMembers();

    Integer id = (Integer) memberIdByName.get(mangledMemberName);
    if (id == null) {
      return -1;
    }

    return id.intValue();
  }

  private void addMember(Member member, String sig) {
    memberById.add(member);
    int index = memberById.size() - 1;
    memberIdByName.put(sig, new Integer(index));
  }

  /**
   * @see com.google.gwt.server.magic.js.MethodInfo for the corresponding
   *      algorithm written in terms of the QDox model.
   */
  private String getJsniSignature(Method method) {
    String name = method.getName();

    StringBuffer sb = new StringBuffer();
    sb.append(name);
    sb.append("(");
    Class[] paramTypes = method.getParameterTypes();
    for (int i = 0; i < paramTypes.length; ++i) {
      Class type = paramTypes[i];
      String typeSig = getTypeSig(type);
      sb.append(typeSig);
    }
    sb.append(")");

    String mangledName = sb.toString();
    return mangledName;
  }

  private String getTypeSig(Class type) {
    if (type.isArray()) {
      return "[" + getTypeSig(type.getComponentType());
    }

    if (type.isPrimitive()) {
      if (type.equals(int.class)) {
        return "I";
      } else if (type.equals(boolean.class)) {
        return "Z";
      } else if (type.equals(char.class)) {
        return "C";
      } else if (type.equals(long.class)) {
        return "J";
      } else if (type.equals(short.class)) {
        return "S";
      } else if (type.equals(float.class)) {
        return "F";
      } else if (type.equals(double.class)) {
        return "D";
      } else if (type.equals(byte.class)) {
        return "B";
      } else {
        throw new RuntimeException("Unexpected primitive type: "
            + type.getName());
      }
    } else {
      StringBuffer sb = new StringBuffer();
      sb.append("L");
      sb.append(type.getName().replace('.', '/'));
      sb.append(";");
      return sb.toString();
    }
  }

  private void lazyInitTargetMembers() {
    if (memberById == null) {
      memberById = new ArrayList();
      memberById.add(null); // 0 is reserved; it's magic on Win32
      memberIdByName = new HashMap();
      lazyInitTargetMembersUsingReflectionHelper(cls);
    }
  }

  private void lazyInitTargetMembersUsingReflectionHelper(Class targetClass) {
    // Start by analyzing the superclass recursively.
    Class superclass = targetClass.getSuperclass();
    if (superclass != null) {
      lazyInitTargetMembersUsingReflectionHelper(superclass);
    }

    /*
     * TODO(mmendez): How should we handle the case where a user writes JSNI
     * code to interact with an instance that is typed as a particular
     * interface? Should a user write JSNI code as follows:
     * 
     * x.@com.google.gwt.HasFocus::equals(Ljava/lang/Object;)(y)
     * 
     * or
     * 
     * x.@java.lang.Object::equals(Ljava/lang/Object;)(y)
     * 
     */

    // Get the methods on this class/interface.
    //
    Method[] methods = targetClass.getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      methods[i].setAccessible(true);
      String sig = getJsniSignature(methods[i]);
      addMember(methods[i], sig);
    }

    // Get the fields on this class/interface.
    Field[] fields = targetClass.getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      fields[i].setAccessible(true);
      addMember(fields[i], fields[i].getName());
    }
  }
}
