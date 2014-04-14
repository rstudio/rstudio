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

/**
 * Class for wrapping Java things for JavaScript.
 */
public class JavaDispatchImpl implements JavaDispatch {

  private final CompilingClassLoader classLoader;

  private final Object target;

  /**
   * This constructor initializes a dispatcher for handling static members.
   *
   * @param ccl class loader to use for dispatching member access
   */
  public JavaDispatchImpl(CompilingClassLoader ccl) {
    classLoader = ccl;
    target = null;
  }

  /**
   * This constructor initializes a dispatcher around a particular instance.
   *
   * @param ccl class loader to use for dispatching member access
   * @param target the instance object to use for dispatching member accesses
   *
   * @throws NullPointerException if target is null
   */
  public JavaDispatchImpl(CompilingClassLoader ccl, Object target) {
    if (target == null) {
      throw new NullPointerException("target cannot be null");
    }

    classLoader = ccl;
    this.target = target;
  }

  /**
   * @param dispId the unique number of a field
   * @return the field
   */
  @Override
  public Field getField(int dispId) {
    if (dispId < 0) {
      throw new RuntimeException("Field does not exist.");
    }
    Member member = getMember(dispId);

    if (member instanceof SyntheticClassMember) {
      try {
        Field f = SyntheticClassMember.class.getDeclaredField("clazz");
        assert f != null;
        return f;
      } catch (SecurityException e) {
      } catch (NoSuchFieldException e) {
      }
      assert false : "Should never get here";
    }
    return (Field) member;
  }

  /**
   * @param dispId the unique number of a field
   * @return true the value of the field
   * @throws IllegalArgumentException
   */
  @Override
  public Object getFieldValue(int dispId) {
    if (dispId < 0) {
      throw new RuntimeException("Field does not exist.");
    }

    Member member = getMember(dispId);

    if (member instanceof SyntheticClassMember) {
      return member.getDeclaringClass();
    }

    Field field = (Field) member;
    try {
      return field.get(target);
    } catch (IllegalAccessException e) {
      // should never, ever happen
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * @param dispId the unique number of a method
   * @return the method
   */
  @Override
  public MethodAdaptor getMethod(int dispId) {
    if (dispId < 0) {
      throw new RuntimeException("Method does not exist.");
    }

    Member m = getMember(dispId);
    if (m instanceof Method) {
      return new MethodAdaptor((Method) m);
    } else if (m instanceof Constructor<?>) {
      return new MethodAdaptor((Constructor<?>) m);
    } else {
      throw new RuntimeException();
    }
  }

  @Override
  public Object getTarget() {
    return target;
  }

  /**
   * @param dispId the unique number of a method or field
   * @return true if the dispId represents a field
   */
  @Override
  public boolean isField(int dispId) {
    if (dispId < 0) {
      return false;
    }

    Member member = getMember(dispId);
    return member instanceof Field || member instanceof SyntheticClassMember;
  }

  /**
   * @param dispId the unique number of a method or field
   * @return true if the dispId represents a method
   */
  @Override
  public boolean isMethod(int dispId) {
    if (dispId < 0) {
      return false;
    }

    Member m = getMember(dispId);
    return (m instanceof Method) || (m instanceof Constructor<?>);
  }

  /**
   * @param dispId the unique number of a field
   * @param value the value to assign to the field
   * @throws IllegalArgumentException
   */
  @Override
  public void setFieldValue(int dispId, Object value) {
    Field field = (Field) getMember(dispId);
    try {
      field.set(target, value);
    } catch (IllegalAccessException e) {
      // should never, ever happen
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * @param dispId the unique number of a method or field
   * @return the member
   */
  protected Member getMember(int dispId) {
    DispatchClassInfo clsInfo = classLoader.getClassInfoByDispId(dispId);
    return clsInfo.getMember(dispId);
  }
}
