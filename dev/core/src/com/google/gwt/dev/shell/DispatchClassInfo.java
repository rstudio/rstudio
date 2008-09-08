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
package com.google.gwt.dev.shell;

import java.lang.reflect.Constructor;
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

  private Class<?> cls;

  private final int clsId;

  private ArrayList<Member> memberById;

  private HashMap<String, Integer> memberIdByName;

  public DispatchClassInfo(Class<?> cls, int classId) {
    this.cls = cls;
    clsId = classId;
  }

  public int getClassId() {
    return clsId;
  }

  public Member getMember(int id) {
    lazyInitTargetMembers();
    id &= 0xffff;
    return memberById.get(id);
  }

  public int getMemberId(String mangledMemberName) {
    lazyInitTargetMembers();

    Integer id = memberIdByName.get(mangledMemberName);
    if (id == null) {
      return -1;
    }

    return id.intValue();
  }

  private void addMember(Member member, String sig) {
    if (member.isSynthetic()) {
      return;
    }
    int index = memberById.size();
    memberById.add(member);
    memberIdByName.put(sig, index);
  }

  private String getJsniSignature(Member member) {
    String name;
    Class<?>[] paramTypes;

    if (member instanceof Method) {
      name = member.getName();
      paramTypes = ((Method) member).getParameterTypes();
    } else if (member instanceof Constructor) {
      name = "new";
      paramTypes = ((Constructor<?>) member).getParameterTypes();
    } else {
      throw new RuntimeException("Unexpected member type "
          + member.getClass().getName());
    }

    StringBuffer sb = new StringBuffer();
    sb.append(name);
    sb.append("(");
    for (int i = 0; i < paramTypes.length; ++i) {
      Class<?> type = paramTypes[i];
      String typeSig = getTypeSig(type);
      sb.append(typeSig);
    }
    sb.append(")");

    String mangledName = sb.toString();
    return mangledName;
  }

  /*
   * TODO(jat): generics?
   */
  private String getTypeSig(Class<?> type) {
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
      memberById = new ArrayList<Member>();
      memberById.add(null); // 0 is reserved; it's magic on Win32
      memberIdByName = new HashMap<String, Integer>();
      lazyInitTargetMembersUsingReflectionHelper(cls, true);
    }
  }

  private void lazyInitTargetMembersUsingReflectionHelper(Class<?> targetClass,
      boolean addConstructors) {
    // Start by analyzing the superclass recursively; the concrete class will
    // clobber on overrides.
    Class<?> superclass = targetClass.getSuperclass();
    if (superclass != null) {
      lazyInitTargetMembersUsingReflectionHelper(superclass, false);
    }
    for (Class<?> intf : targetClass.getInterfaces()) {
      lazyInitTargetMembersUsingReflectionHelper(intf, false);
    }

    if (addConstructors) {
      for (Constructor<?> ctor : targetClass.getDeclaredConstructors()) {
        ctor.setAccessible(true);
        String sig = getJsniSignature(ctor);
        addMember(ctor, sig);
      }
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
    for (Method method : targetClass.getDeclaredMethods()) {
      method.setAccessible(true);
      String sig = getJsniSignature(method);
      addMember(method, sig);
    }

    // Get the fields on this class/interface.
    Field[] fields = targetClass.getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
      addMember(field, field.getName());
    }
  }
}
