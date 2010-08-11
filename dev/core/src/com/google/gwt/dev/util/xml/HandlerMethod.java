/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util.xml;

import com.google.gwt.core.ext.UnableToCompleteException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata about a handler method in a class derived from {@link Schema}. 
 */
public final class HandlerMethod {

  private static final HandlerParam[] EMPTY_HANDLERPARAMS = new HandlerParam[0];

  // A schema level that ignores everything.
  private static final Schema sArbitraryChildHandler = new Schema() {

    @Override
    public void onBadAttributeValue(int lineNumber, String elemName,
        String attrName, String attrValue, Class<?> paramType) {
      // Ignore
    }

    @Override
    public void onHandlerException(int lineNumber, String elemLocalName,
        Method method, Throwable e) {
      // Ignore
    }

    @Override
    public void onMissingAttribute(int lineNumber, String elemName,
        String argName) {
      // Ignore
    }

    @Override
    public void onUnexpectedAttribute(int lineNumber, String elemName,
        String attrName, String attrValue) {
      // Ignore
    }

    @Override
    public void onUnexpectedChild(int lineNumber, String elemName) {
      // Ignore
    }

    @Override
    public void onUnexpectedElement(int lineNumber, String elemName) {
      // Ignore
    }
  };

  private static final int TYPE_NONE = 0;
  private static final int TYPE_BEGIN = 1;
  private static final int TYPE_END = 2;
  private static final int TYPE_TEXT = 3;

  static {
    ReflectiveParser.registerSchemaLevel(sArbitraryChildHandler.getClass());
  }

  /**
   * Attempts to create a handler method from any method. You can pass in any
   * method at all, but an exception will be thrown if the method is clearly a
   * handler but the containing class does not have the proper parameter
   * metafields.
   */
  @SuppressWarnings("unchecked")
  public static HandlerMethod tryCreate(Method method) {
    String methodName = method.getName();
    String normalizedTagName = null;
    try {
      int type = TYPE_NONE;

      if (methodName.startsWith("__")) {
        if (methodName.endsWith("_begin")) {
          type = TYPE_BEGIN;
          normalizedTagName = methodName.substring(0, methodName.length()
              - "_begin".length());
        } else if (methodName.endsWith("_end")) {
          type = TYPE_END;
          normalizedTagName = methodName.substring(0, methodName.length()
              - "_end".length());
        } else if (methodName.equals("__text")) {
          type = TYPE_TEXT;
        }
      }

      if (type == TYPE_NONE) {
        // This was not a handler method.
        // Exit early.
        //
        return null;
      }

      assert (type == TYPE_BEGIN || type == TYPE_END || type == TYPE_TEXT);

      // Can the corresponding element have arbitrary children?
      //
      Class<?> returnType = method.getReturnType();
      boolean arbitraryChildren = false;
      if (type == TYPE_BEGIN) {
        if (Schema.class.isAssignableFrom(returnType)) {
          arbitraryChildren = false;

          // Also, we need to register this schema type.
          //
          ReflectiveParser.registerSchemaLevel((Class<? extends Schema>) returnType);

        } else if (returnType.equals(Void.TYPE)) {
          arbitraryChildren = true;
        } else {
          throw new IllegalArgumentException(
              "The return type of begin handlers must be 'void' or assignable to 'SchemaLevel'");
        }
      } else if (!Void.TYPE.equals(returnType)) {
        throw new IllegalArgumentException(
            "Only 'void' may be specified as a return type for 'end' and 'text' handlers");
      }

      // Create handler args.
      //
      if (type == TYPE_TEXT) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1 || !String.class.equals(paramTypes[0])) {
          throw new IllegalArgumentException(
              "__text handlers must have exactly one String parameter");
        }

        // We pretend it doesn't have any param since they're always
        // pre-determined.
        //
        return new HandlerMethod(method, type, false, EMPTY_HANDLERPARAMS);
      } else {
        Class<?>[] paramTypes = method.getParameterTypes();
        List<HandlerParam> handlerParams = new ArrayList<HandlerParam>();
        for (int i = 0, n = paramTypes.length; i < n; ++i) {
          HandlerParam handlerParam = HandlerParam.create(method,
              normalizedTagName, i);
          if (handlerParam != null) {
            handlerParams.add(handlerParam);
          } else {
            throw new IllegalArgumentException("In method '" + method.getName()
                + "', parameter " + (i + 1) + " is an unsupported type");
          }
        }

        HandlerParam[] hpa = handlerParams.toArray(EMPTY_HANDLERPARAMS);
        return new HandlerMethod(method, type, arbitraryChildren, hpa);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to use method '" + methodName
          + "' as a handler", e);
    }
  }

  private final boolean arbitraryChildren;

  private final HandlerParam[] handlerParams;

  private final Method method;

  private final int methodType;

  private HandlerMethod(Method method, int type, boolean arbitraryChildren,
      HandlerParam[] hpa) {
    this.method = method;
    this.methodType = type;
    this.arbitraryChildren = arbitraryChildren;
    this.handlerParams = hpa.clone();

    this.method.setAccessible(true);
  }

  public HandlerArgs createArgs(Schema schema, int lineNumber, String elemName) {
    return new HandlerArgs(schema, lineNumber, elemName, handlerParams);
  }

  public String getNormalizedName() {
    String name = method.getName();
    if (isStartMethod()) {
      return name.substring(2, name.length() - "_begin".length());
    } else if (isEndMethod()) {
      return name.substring(2, name.length() - "_end".length());
    } else {
      throw new IllegalStateException("Unexpected method name");
    }
  }

  public HandlerParam getParam(int i) {
    return handlerParams[i];
  }

  public int getParamCount() {
    return handlerParams.length;
  }

  public Schema invokeBegin(int lineNumber, String elemLocalName,
      Schema target, HandlerArgs args, Object[] outInvokeArgs)
      throws UnableToCompleteException {
    assert (outInvokeArgs.length == args.getArgCount());

    for (int i = 0, n = args.getArgCount(); i < n; ++i) {
      Object invokeArg = args.convertToArg(i);
      outInvokeArgs[i] = invokeArg;
    }

    Schema nextSchemaLevel = null;

    Throwable caught = null;
    try {
      target.setLineNumber(lineNumber);
      nextSchemaLevel = (Schema) method.invoke(target, outInvokeArgs);
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    }

    if (caught != null) {
      target.onHandlerException(lineNumber, elemLocalName, method, caught);
    }

    // Prepare a resulting schema level that allows the reflective parser
    // to simply perform its normal logic, even while there are some
    // special cases.
    //
    // Four cases:
    // (1) childSchemaLevel is non-null, in which case it becomes the new
    // schema used for child elements
    // (2) the handler method has return type "SchemaLevel" but the result
    // was null, meaning that it cannot have child elements;
    // we return null to indicate this
    // (3) the handler method has return type "void", meaning that child
    // elements are simply ignored; we push null to detect this
    // (4) the method failed or could not be called, which is treated the same
    // as case (3)
    //
    if (nextSchemaLevel != null) {
      return nextSchemaLevel;
    } else if (arbitraryChildren) {
      return sArbitraryChildHandler;
    } else {
      return null;
    }
  }

  public void invokeEnd(int lineNumber, String elem, Schema target,
      Object[] args) throws UnableToCompleteException {
    Throwable caught = null;
    try {
      target.setLineNumber(lineNumber);
      method.invoke(target, args);
      return;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    }
    target.onHandlerException(lineNumber, elem, method, caught);
  }

  public void invokeText(int lineNumber, String text, Schema target)
      throws UnableToCompleteException {
    Throwable caught = null;
    try {
      target.setLineNumber(lineNumber);
      method.invoke(target, new Object[] {text});
      return;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    }
    target.onHandlerException(lineNumber, "#text", method, caught);
  }

  public boolean isEndMethod() {
    return methodType == TYPE_END;
  }

  public boolean isStartMethod() {
    return methodType == TYPE_BEGIN;
  }
}
