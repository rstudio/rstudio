// Copyright 2005 Google Inc.
// All Rights Reserved.

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
