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
#ifndef GWT_WEBKIT_H
#define GWT_WEBKIT_H

#include <jni.h>
#include "JStringWrap.h"

extern JNIEnv* gEnv;
extern jclass gClass;
extern jclass gDispObjCls;
extern jclass gDispMethCls;
extern jmethodID gSetFieldMeth;
extern jmethodID gGetFieldMeth;
extern jmethodID gInvokeMeth;
extern jmethodID gToStringMeth;

//#define FILETRACE
//#define JAVATRACE
#if defined(FILETRACE) && defined(JAVATRACE)
#define TRACE(s) filetrace(s),javatrace(s)
#elif defined(FILETRACE)
#define TRACE(s) filetrace(s)
#elif defined(JAVATRACE)
#define TRACE(s) javatrace(s)
#else
#define TRACE(s) ((void)0)
#endif

#ifdef FILETRACE
extern FILE* gout;
void filetrace(const char* s);
#endif // FILETRACE

#ifdef JAVATRACE
extern jmethodID gTraceMethod;
void javatrace(const char* s);
#endif // JAVATRACE

#endif
