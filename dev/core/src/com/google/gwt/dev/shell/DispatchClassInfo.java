// Copyright 2006 Google Inc. All Rights Reserved.
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

  public DispatchClassInfo(Class cls, int classId) {
    fClass = cls;
    fClassId = classId;
  }

  public int getClassId() {
    return fClassId;
  }

  public Member getMember(int id) {
    lazyInitTargetMembers();
    id &= 0xffff;
    return (Member) fMemberById.get(id);
  }

  public int getMemberId(String mangledMemberName) {
    lazyInitTargetMembers();

    Integer id = (Integer) fMemberIdByName.get(mangledMemberName);
    if (id == null) return -1;

    return id.intValue();
  }

  public Class getWrappedClass() {
    return fClass;
  }

  private void addMember(Member member, String sig) {
    fMemberById.add(member);
    int index = fMemberById.size() - 1;
    fMemberIdByName.put(sig, new Integer(index));
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
      if (type.equals(int.class))
        return "I";
      else if (type.equals(boolean.class))
        return "Z";
      else if (type.equals(char.class))
        return "C";
      else if (type.equals(long.class))
        return "J";
      else if (type.equals(short.class))
        return "S";
      else if (type.equals(float.class))
        return "F";
      else if (type.equals(double.class))
        return "D";
      else if (type.equals(byte.class))
        return "B";
      else
        throw new RuntimeException("Unexpected primitive type: "
            + type.getName());
    } else {
      StringBuffer sb = new StringBuffer();
      sb.append("L");
      sb.append(type.getName().replace('.', '/'));
      sb.append(";");
      return sb.toString();
    }
  }

  private void lazyInitTargetMembers() {
    if (fMemberById == null) {
      fMemberById = new ArrayList();
      try {
        // MAGIC: 0 is the default property
        fMemberById.add(fClass.getMethod("toString", null));
      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        /*
         * Interfaces do not automatically inherit toString() implicitly.  If they
         * have not defined a toString() method then we will pick the one from
         * java.lang.Object::toString() method and use that at slot zero.
         * 
         * TODO(mmendez): How should we handle the case where a user writes JSNI
         * code to interact with an instance that is typed as a particular 
         * interface?  Should a user write JSNI code as follows:
         *  
         *   x.@com.google.gwt.HasFocus::equals(Ljava/lang/Object;)(y) 
         *   
         * or
         * 
         *   x.@java.lang.Object::equals(Ljava/lang/Object;)(y)
         */
        if (fClass.isInterface()) {
          try {
            fMemberById.add(Object.class.getMethod("toString", null));
          } catch (Exception e1) {
            e1.printStackTrace();
          }
        } else {
          e.printStackTrace();  
        }
      }
      
      fMemberIdByName = new HashMap();
      lazyInitTargetMembersUsingReflectionHelper(fClass);
    }
  }

  private void lazyInitTargetMembersUsingReflectionHelper(Class targetClass) {
    // Start by analyzing the superclass recursively.
    Class superclass = targetClass.getSuperclass();
    if (superclass != null)
      lazyInitTargetMembersUsingReflectionHelper(superclass);

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

  private Class fClass;

  private final int fClassId;

  private ArrayList fMemberById;

  private HashMap fMemberIdByName;
}
