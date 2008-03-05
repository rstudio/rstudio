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
#include "FuncWrapper.h"
#include <kjs/array_object.h>

using namespace KJS;

/*
 * Constructor for FuncWrapper.
 * 
 * name JavaScript name of the function
 * funcObj a GlobalRef of the Java MethodDispatch object (to be freed in
 *   the destructor, so the caller no longer has ownership)
 */
FuncWrapper::FuncWrapper(const UString& name, jobject funcObj)
    : FunctionObject(name), funcObj(funcObj) { }

/*
 * Destructor for FuncWrapper.
 * 
 * Frees the GlobalRef for the Java MethodDispatch object.
 */
FuncWrapper::~FuncWrapper() {
  gEnv->DeleteGlobalRef(funcObj);
}

/*
 * Call a Java MethodDispatch interface from JavaScript.
 * All JSValue* values passed to Java must be GC-protected, since Java
 * will take ownership of them and eventually unprotect them.
 *
 * execState the KJS execution state to run in
 * thisObj the JavaScript object wrapper for the Java object this method
 *   is defined on
 * args the argument list
 *   
 * Returns the JSValue returned from the Java method.
 */
JSValue *FuncWrapper::callAsFunction(ExecState* execState, JSObject* thisObj,
    const List& args)
{
  TRACE("ENTER FuncWrapper::callAsFunction");

  // create the array of JSValue* (passed as integers to Java)
  int argc = args.size();
  jintArray jsargs = gEnv->NewIntArray(argc);
  if (!jsargs || gEnv->ExceptionCheck()) {
    TRACE("FAIL FuncWrapper::callAsFunction: NewIntArray");
    return jsUndefined();
  }

  // protect the JSValue* values and store them in the array
  for (int i = 0; i < argc; ++i) {
    JSValue* arg = args[i];
    gwtGCProtect(arg);
    gEnv->SetIntArrayRegion(jsargs, i, 1, reinterpret_cast<jint*>(&arg));
    if (gEnv->ExceptionCheck()) {
      TRACE("FAIL FuncWrapper::callAsFunction: SetIntArrayRegion");
      return jsUndefined();
    }
  }

  // protect the "this" object, as Java will eventually unprotect it
  gwtGCProtect(thisObj);
  jint result = gEnv->CallIntMethod(funcObj, gInvokeMeth, execState,
      thisObj, jsargs);
  if (gEnv->ExceptionCheck()) {
    TRACE("FAIL FuncWrapper::callAsFunction: java exception is active");
    return jsUndefined();
  }

  if (execState->hadException()) {
    TRACE("FAIL FuncWrapper::callAsFunction: js exception is active");
    return jsUndefined();
  }

  TRACE("SUCCESS FuncWrapper::callAsFunction");
  return reinterpret_cast<JSValue*>(result);
}
