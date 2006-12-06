// Copyright 2005 Google Inc.
// All Rights Reserved.

#ifndef JSTRINGWRAP_H
#define JSTRINGWRAP_H

#include <jni.h>

struct JStringWrap
{
    JStringWrap(JNIEnv* env, jstring str): env(env), s(str), p(0), jp(0) { }
    ~JStringWrap() { if (p) env->ReleaseStringUTFChars(s, p); if (jp) env->ReleaseStringChars(s, jp); }
    const char* str() { if (!p) p = env->GetStringUTFChars(s, 0); return p; }
    const jchar* jstr() { if (!jp) jp = env->GetStringChars(s, 0); return jp; }
	jsize length() { return env->GetStringLength(s); }
private:
    JNIEnv* env;
    jstring s;
    const char* p;
    const jchar* jp;
};

#endif
