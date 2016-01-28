/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.dev.jjs.ast;

/**
 * Provides access to known types, methods and fields.
 */
public class RuntimeConstants {
  public static final String ASYNC_FRAGMENT_LOADER_BROWSER_LOADER =
      "AsyncFragmentLoader.BROWSER_LOADER";
  public static final String ASYNC_FRAGMENT_LOADER_ON_LOAD = "AsyncFragmentLoader.onLoad";
  public static final String ASYNC_FRAGMENT_LOADER_RUN_ASYNC = "AsyncFragmentLoader.runAsync";

  public static final String ARRAY_ENSURE_NOT_NULL = "Array.ensureNotNull";
  public static final String ARRAY_GET_CLASS_LITERAL_FOR_ARRAY = "Array.getClassLiteralForArray";
  public static final String ARRAY_INITIALIZE_UNIDIMENSIONAL_ARRAY = "Array.initUnidimensionalArray";
  public static final String ARRAY_INITIALIZE_MULTIDIMENSIONAL_ARRAY = "Array.initMultidimensionalArray";
  public static final String ARRAY_NEW_ARRAY = "Array.newArray";
  public static final String ARRAY_SET_CHECK = "Array.setCheck";
  public static final String ARRAY_STAMP_JAVA_TYPE_INFO = "Array.stampJavaTypeInfo";
  public static final String ARRAY_IS_JAVA_ARRAY = "Array.isJavaArray";

  public static final String CAST_CHAR_TO_STRING = "Cast.charToString";
  public static final String CAST_HAS_JAVA_OBJECT_VIRTUAL_DISPATCH =
      "Cast.hasJavaObjectVirtualDispatch";
  public static final String CAST_THROW_CLASS_CAST_EXCEPTION_UNLESS_NULL
      = "Cast.throwClassCastExceptionUnlessNull";

  public static final String CLASS_CREATE_FOR_CLASS = "Class.createForClass";
  public static final String CLASS_CREATE_FOR_PRIMITIVE = "Class.createForPrimitive";
  public static final String CLASS_CREATE_FOR_INTERFACE = "Class.createForInterface";

  public static final String COLLAPSED_PROPERTY_HOLDER_GET_PERMUTATION_ID
      = "CollapsedPropertyHolder.getPermutationId";

  public static final String COVERAGE_UTIL_ON_BEFORE_UNLOAD = "CoverageUtil.onBeforeUnload";
  public static final String COVERAGE_UTIL_COVER = "CoverageUtil.cover";
  public static final String COVERAGE_UTIL_COVERAGE = "CoverageUtil.coverage";

  public static final String ENUM_CREATE_VALUE_OF_MAP = "Enum.createValueOfMap";
  public static final String ENUM_ENUM = "Enum.Enum";
  public static final String ENUM_NAME = "Enum.name";
  public static final String ENUM_ORDINAL = "Enum.ordinal";
  public static final String ENUM_TO_STRING = "Enum.toString";

  public static final String EXCEPTIONS_CHECK_NOT_NULL = "Exceptions.checkNotNull";
  public static final String EXCEPTIONS_MAKE_ASSERTION_ERROR_ = "Exceptions.makeAssertionError";
  public static final String EXCEPTIONS_TO_JS = "Exceptions.toJs";
  public static final String EXCEPTIONS_TO_JAVA = "Exceptions.toJava";

  public static final String GWT_IS_SCRIPT = "GWT.isScript";

  public static final String LONG_LIB_FROM_DOUBLE = "LongLib.fromDouble";
  public static final String LONG_LIB_FROM_INT = "LongLib.fromInt";
  public static final String LONG_LIB_TO_DOUBLE = "LongLib.toDouble";
  public static final String LONG_LIB_TO_INT = "LongLib.toInt";
  public static final String LONG_LIB_TO_STRING = "LongLib.toString";

  public static final String MODULE_UTILS_GWT_ON_LOAD = "ModuleUtils.gwtOnLoad";

  public static final String OBJECT_CASTABLE_TYPE_MAP = "Object.castableTypeMap";
  public static final String OBJECT_CLAZZ = "Object.___clazz";
  public static final String OBJECT_GET_CLASS = "Object.getClass";
  public static final String OBJECT_TO_STRING = "Object.toString";
  public static final String OBJECT_TYPEMARKER = "Object.typeMarker";

  public static final String RUN_ASYNC_CALLBACK_ON_SUCCESS = "RunAsyncCallback.onSuccess";
  public static final String RUN_ASYNC_CODE_FOR_SPLIT_POINT_NUMBER
      = "RunAsyncCode.forSplitPointNumber";

  public static final String RUNTIME = "Runtime";
  public static final String RUNTIME_BOOTSTRAP = "Runtime.bootstrap";
  public static final String RUNTIME_COPY_OBJECT_PROPERTIES = "Runtime.copyObjectProperties";
  public static final String RUNTIME_DEFINE_CLASS = "Runtime.defineClass";
  public static final String RUNTIME_DEFINE_PROPERTIES = "Runtime.defineProperties";
  public static final String RUNTIME_EMPTY_METHOD = "Runtime.emptyMethod";
  public static final String RUNTIME_GET_CLASS_PROTOTYPE = "Runtime.getClassPrototype";
  public static final String RUNTIME_MAKE_LAMBDA_FUNCTION = "Runtime.makeLambdaFunction";
  public static final String RUNTIME_PROVIDE = "Runtime.provide";
  public static final String RUNTIME_TYPE_MARKER_FN = "Runtime.typeMarkerFn";
  public static final String RUNTIME_UNIQUE_ID = "Runtime.uniqueId";

  public static final String UTIL_MAKE_ENUM_NAME = "Util.makeEnumName";
}
