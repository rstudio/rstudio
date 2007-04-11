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
#ifndef JSTRINGWRAP_H
#define JSTRINGWRAP_H

#include <jni.h>

/*
 * Wrap a Java String and automatically clean up temporary storage allocated
 * for accessing its contents.
 */
struct JStringWrap
{
  JStringWrap(JNIEnv* env, jstring str): env(env), s(str), p(0), jp(0) { }
  ~JStringWrap() {
  	if (p) env->ReleaseStringUTFChars(s, p);
  	if (jp) env->ReleaseStringChars(s, jp);
  }
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
