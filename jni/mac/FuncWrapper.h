// Copyright 2005 Google Inc.
// All Rights Reserved.

#ifndef FUNC_WRAPPER_H
#define FUNC_WRAPPER_H

#include "FunctionObject.h"
#include <jni.h>

// This class actually wraps Java method objects
class FuncWrapper : public FunctionObject {
public:
	// funcObj MUST be a global ref
	FuncWrapper(const KJS::UString& name, jobject funcObj);
	virtual ~FuncWrapper();
	jobject getFuncObj();

public:
    virtual KJS::JSValue *callAsFunction(KJS::ExecState*, KJS::JSObject*, const KJS::List&);

private:
	jobject funcObj;
};

inline jobject FuncWrapper::getFuncObj() {
	return funcObj;
}

#endif
