// Copyright 2005 Google Inc.
// All Rights Reserved.

#ifndef FUNCTION_OBJECT_H
#define FUNCTION_OBJECT_H

#include "gwt-webkit.h"
#include <kjs/object.h>

class FunctionObject : public KJS::JSObject {
protected:
    FunctionObject(const KJS::UString& name);

public:
    const KJS::ClassInfo *classInfo() const { return &info; }

	// shared implementations of JSObject methods
    virtual bool getOwnPropertySlot(KJS::ExecState*, const KJS::Identifier&, KJS::PropertySlot&);
    virtual bool canPut(KJS::ExecState*, const KJS::Identifier&) const;
    virtual void put(KJS::ExecState*, const KJS::Identifier&, KJS::JSValue*, int);
    virtual bool deleteProperty(KJS::ExecState*, const KJS::Identifier&);
    virtual KJS::JSValue *defaultValue(KJS::ExecState*, KJS::JSType) const;
    virtual bool implementsCall() const;
	
	// subclasses must implement
    virtual KJS::JSValue *callAsFunction(KJS::ExecState*, KJS::JSObject*, const KJS::List&) = 0;
    
    static const KJS::ClassInfo info;

private:
	static KJS::JSValue* getter(KJS::ExecState*, KJS::JSObject*, const KJS::Identifier&, const KJS::PropertySlot&);

private:
	KJS::UString name;
};

class ToStringFunction : public FunctionObject {
public:
	ToStringFunction();
    virtual KJS::JSValue *callAsFunction(KJS::ExecState*, KJS::JSObject*, const KJS::List&);
};

#endif
