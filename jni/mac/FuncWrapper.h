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
#ifndef FUNC_WRAPPER_H
#define FUNC_WRAPPER_H

#include "FunctionObject.h"
#include <jni.h>

/*
 * Wraps Java methods (MethodDispatch)
 */
class FuncWrapper : public FunctionObject {
public:
  // funcObj MUST be a global ref
  FuncWrapper(const KJS::UString& name, jobject funcObj);
  virtual ~FuncWrapper();
  jobject getFuncObj();

public:
  virtual KJS::JSValue *callAsFunction(KJS::ExecState*, KJS::JSObject*,
      const KJS::List&);

private:
  jobject funcObj;
};

inline jobject FuncWrapper::getFuncObj() {
  return funcObj;
}

#endif
