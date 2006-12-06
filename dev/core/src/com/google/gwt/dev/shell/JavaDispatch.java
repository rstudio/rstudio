// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface JavaDispatch {

  Field getField(int dispId);

  Object getFieldValue(int dispId);

  Method getMethod(int dispId);

  Object getTarget();

  boolean isField(int dispId);

  boolean isMethod(int dispId);

  void setFieldValue(int dispId, Object value);

}