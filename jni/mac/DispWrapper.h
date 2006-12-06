// Copyright 2005 Google Inc.
// All Rights Reserved.

#ifndef DISP_WRAPPER_H
#define DISP_WRAPPER_H

#include "gwt-webkit.h"
#include <kjs/object.h>

// This class actually wraps Java objects
class DispWrapper : public KJS::JSObject {
public:
	// dispObj MUST be a global ref
    DispWrapper(jobject dispObj);
	virtual ~DispWrapper();
	jobject getDispObj();

public:
	// implementations of JSObject methods
    const KJS::ClassInfo *classInfo() const { return &info; }

    virtual bool getOwnPropertySlot(KJS::ExecState*, const KJS::Identifier&, KJS::PropertySlot&);
    virtual bool canPut(KJS::ExecState*, const KJS::Identifier&) const;
    virtual void put(KJS::ExecState*, const KJS::Identifier&, KJS::JSValue*, int);
    virtual bool deleteProperty(KJS::ExecState*, const KJS::Identifier&);
    virtual KJS::JSValue *defaultValue(KJS::ExecState*, KJS::JSType) const;
    virtual bool implementsCall() const;
    virtual KJS::JSValue *callAsFunction(KJS::ExecState*, KJS::JSObject*, const KJS::List&);
    
    static const KJS::ClassInfo info;

private:
	static KJS::JSValue* getter(KJS::ExecState*, KJS::JSObject*, const KJS::Identifier&, const KJS::PropertySlot&);

private:
	jobject dispObj;
};

inline jobject DispWrapper::getDispObj() {
	return dispObj;
}

#endif
