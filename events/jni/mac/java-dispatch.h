/*
 * Copyright 2008 Google Inc.
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

#ifndef JAVA_DISPATCHER_H__
#define JAVA_DISPATCHER_H__
#include <iostream>
#include <JavaScriptCore/JavaScriptCore.h>
#include <jni.h>

namespace gwt {

/*
 * Initializes static members needed by DispatchObject, DispatchMethod,
 * and LowLevelSaf.  This should be called before before calling anything
 * else.
 */
bool Initialize(JNIEnv*, jclass, jclass, jclass);

/*
 * Returns a shared reference to the DispatchObject class
 */
JSClassRef GetDispatchObjectClass();

/*
 * Constructs a new DispatchObject.
 *
 * jContext - the JavaScript context
 * jObject - the java instance of DispatchObject to which
 *   this instance will dispatch calls
 */
JSObjectRef DispatchObjectCreate(JSContextRef jContext, jobject jObject);

/*
 * Returns a shared reference to the DispatchMethod class
 */
JSClassRef GetDispatchMethodClass();

/*
 * Constructs a new DispatchMethod object.
 *
 * jsContext - the JavaScript context
 * name - the name of the method (used in toString)
 * jObject - the java instance of DispatchMethod to which this object will
 *   delegate calls.
 */
JSObjectRef DispatchMethodCreate(JSContextRef jsContext, std::string& name,
                                 jobject jObject);


} // namespace gwt

#endif // JAVA_DISPATCHER_H__
