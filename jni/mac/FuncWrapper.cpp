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
#include "FuncWrapper.h"
#include <kjs/array_object.h>

using namespace KJS;

// FuncWrapper
FuncWrapper::FuncWrapper(const UString& name, jobject funcObj): FunctionObject(name)
, funcObj(funcObj) {
}

FuncWrapper::~FuncWrapper() {
	gEnv->DeleteGlobalRef(funcObj);
}

JSValue *FuncWrapper::callAsFunction(ExecState* execState, JSObject* thisObj, const List& args) {
    TRACE("ENTER FuncWrapper::callAsFunction");

	int argc = args.size();
    jintArray jsargs = gEnv->NewIntArray(argc);
    if (!jsargs || gEnv->ExceptionCheck())
        return TRACE("FAIL FuncWrapper::callAsFunction: NewIntArray"), jsUndefined();

	for (int i = 0; i < argc; ++i) {
		JSValue* arg = args[i];
		gEnv->SetIntArrayRegion(jsargs, i, 1, (jint*)&arg);
		if (gEnv->ExceptionCheck())
			return TRACE("FAIL FuncWrapper::callAsFunction: SetIntArrayRegion"), jsUndefined();
	}

    jint result = gEnv->CallIntMethod(funcObj, gInvokeMeth, execState, thisObj, jsargs);
    if (gEnv->ExceptionCheck())
        return TRACE("FAIL FuncWrapper::callAsFunction: java exception is active"), jsUndefined();

    if (execState->hadException())
        return TRACE("FAIL FuncWrapper::callAsFunction: js exception is active"), jsUndefined();

    TRACE("SUCCESS FuncWrapper::callAsFunction");
    return (JSValue*)result;
}
