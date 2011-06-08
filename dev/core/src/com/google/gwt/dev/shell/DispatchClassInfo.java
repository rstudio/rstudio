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

import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.StringInterner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

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

  private void addMember(
      LinkedHashMap<String, LinkedHashMap<String, Member>> members,
      Member member, String sig) {
    String fullSig = getJsniSignature(member, false);
    LinkedHashMap<String, Member> membersWithSig = members.get(sig);
    if (membersWithSig == null) {
      membersWithSig = new LinkedHashMap<String, Member>();
      members.put(sig, membersWithSig);
    }
    membersWithSig.put(fullSig, member);
  }

  private void addMemberIfUnique(String name, List<Member> membersForName) {
    if (membersForName.size() == 1) {
      memberById.add(membersForName.get(0));
      memberIdByName.put(
          StringInterner.get().intern(name), memberById.size() - 1);
    }
  }

  private List<Member> filterOutSyntheticMembers(Collection<Member> members) {
    List<Member> nonSynth = new ArrayList<Member>();
    for (Member member : members) {
      if (!member.isSynthetic()) {
        nonSynth.add(member);
      }
    }
    return nonSynth;
  }

  private LinkedHashMap<String, LinkedHashMap<String, Member>> findMostDerivedMembers(
      Class<?> targetClass, boolean addConstructors) {
    LinkedHashMap<String, LinkedHashMap<String, Member>> members = new LinkedHashMap<String, LinkedHashMap<String, Member>>();
    findMostDerivedMembers(members, targetClass, addConstructors);
    return members;
  }

  /**
   * For each available JSNI reference, find the most derived field or method
   * that matches it. For wildcard references, there will be more than one of
   * them, one for each signature matched.
   */
  private void findMostDerivedMembers(
      LinkedHashMap<String, LinkedHashMap<String, Member>> members,
      Class<?> targetClass, boolean addConstructors) {
    /*
     * Analyze superclasses and interfaces first. More derived members will thus
     * be seen later.
     */
    Class<?> superclass = targetClass.getSuperclass();
    if (superclass != null) {
      findMostDerivedMembers(members, superclass, false);
    }
    for (Class<?> intf : targetClass.getInterfaces()) {
      findMostDerivedMembers(members, intf, false);
    }

    if (addConstructors) {
      for (Constructor<?> ctor : targetClass.getDeclaredConstructors()) {
        ctor.setAccessible(true);
        addMember(members, ctor, getJsniSignature(ctor, false));
        addMember(members, ctor, getJsniSignature(ctor, true));
      }
    }

    // Get the methods on this class/interface.
    for (Method method : targetClass.getDeclaredMethods()) {
      method.setAccessible(true);
      addMember(members, method, getJsniSignature(method, false));
      addMember(members, method, getJsniSignature(method, true));
    }

    // Get the fields on this class/interface.
    Field[] fields = targetClass.getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
      addMember(members, field, field.getName());
    }

    // Add a synthetic field to access class literals from JSNI
    addMember(members, new SyntheticClassMember(targetClass), "class");
  }

  private String getJsniSignature(Member member, boolean wildcardParamList) {
    String name;
    Class<?>[] paramTypes;

    if (member instanceof Field) {
      return member.getName();
    } else if (member instanceof SyntheticClassMember) {
      return member.getName();
    } else if (member instanceof Method) {
      name = member.getName();
      paramTypes = ((Method) member).getParameterTypes();
    } else if (member instanceof Constructor<?>) {
      name = "new";
      paramTypes = ((Constructor<?>) member).getParameterTypes();
    } else {
      throw new RuntimeException("Unexpected member type "
          + member.getClass().getName());
    }

    StringBuffer sb = new StringBuffer();
    sb.append(name);
    sb.append("(");
    if (wildcardParamList) {
      sb.append(JsniRef.WILDCARD_PARAM_LIST);
    } else {
      for (int i = 0; i < paramTypes.length; ++i) {
        Class<?> type = paramTypes[i];
        String typeSig = getTypeSig(type);
        sb.append(typeSig);
      }
    }
    sb.append(")");

    String mangledName = StringInterner.get().intern(sb.toString());
    
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

      LinkedHashMap<String, LinkedHashMap<String, Member>> members = findMostDerivedMembers(
          cls, true);
      for (Entry<String, LinkedHashMap<String, Member>> entry : members.entrySet()) {
        String name = entry.getKey();

        List<Member> membersForName = new ArrayList<Member>(
            entry.getValue().values());
        addMemberIfUnique(name, membersForName); // backward compatibility
        addMemberIfUnique(name, filterOutSyntheticMembers(membersForName));
      }
    }
  }
}
