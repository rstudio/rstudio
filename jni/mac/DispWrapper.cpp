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
#include "DispWrapper.h"
#include "FunctionObject.h"

using namespace KJS;

const ClassInfo DispWrapper::info = {"DispWrapper", 0, 0, 0};

JSValue *DispWrapper::getter(ExecState* exec, JSObject* thisObj, const Identifier& propertyName, const PropertySlot& slot) {
	TRACE("ENTER DispWrapper::getter");
	if (propertyName.ustring() == "toString") {
		return new ToStringFunction();
	}
	if (thisObj->classInfo() == &DispWrapper::info) {
		DispWrapper* dispWrap = static_cast<DispWrapper*>(thisObj);
		jobject dispObj = dispWrap->dispObj;
		jstring jpropName = gEnv->NewString((const jchar*)propertyName.data(), propertyName.size());
		if (!jpropName || gEnv->ExceptionCheck()) {
			gEnv->ExceptionClear();
			return jsUndefined();
		}
		jint result = gEnv->CallIntMethod(dispObj, gGetFieldMeth, jpropName);
		if (!result || gEnv->ExceptionCheck()) {
			gEnv->ExceptionClear();
			return jsUndefined();
		}
		TRACE("SUCCESS DispWrapper::getter");
		return (JSValue*)result;
	}
	return jsUndefined();
}

DispWrapper::DispWrapper(jobject dispObj): dispObj(dispObj) {
}

DispWrapper::~DispWrapper() {
	gEnv->DeleteGlobalRef(dispObj);
}

bool DispWrapper::getOwnPropertySlot(ExecState *exec, const Identifier& propertyName, PropertySlot& slot) {
	slot.setCustom(this, getter);
	return true;
}

bool DispWrapper::canPut(ExecState *exec, const Identifier &propertyName) const {
	return true;
}

void DispWrapper::put(ExecState *exec, const Identifier &propertyName, JSValue *value, int attr) {
	TRACE("ENTER DispWrapper::put");
	jstring jpropName = gEnv->NewString((const jchar*)propertyName.data(), propertyName.size());
	if (!jpropName || gEnv->ExceptionCheck()) {
		gEnv->ExceptionClear();
		return;
	}

	gEnv->CallVoidMethod(dispObj, gSetFieldMeth, jpropName, (jint)value);
	if (gEnv->ExceptionCheck()) {
		gEnv->ExceptionClear();
		return;
	}
	TRACE("SUCCESS DispWrapper::put");
}

bool DispWrapper::deleteProperty(ExecState *exec, const Identifier &propertyName) {
	return false;
}

JSValue *DispWrapper::defaultValue(ExecState *exec, JSType hint) const {
	jstring result = (jstring)gEnv->CallObjectMethod(dispObj, gToStringMeth);
	if (gEnv->ExceptionCheck()) {
		return jsUndefined();
	} else if (!result) {
		return jsNull();
	} else {
		JStringWrap jresult(gEnv, result);
		return jsString(UString((const UChar*)jresult.jstr(), jresult.length()));
	}
}

bool DispWrapper::implementsCall() const {
	return false;
}

JSValue *DispWrapper::callAsFunction(ExecState *exec, JSObject *thisObj, const List &args) {
	return jsUndefined();
}
