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
#include "gwt-ll.h"
#include "JStringWrap.h"

// http://unixjunkie.blogspot.com/2006/07/access-argc-and-argv-from-anywhere.html
extern "C" int *_NSGetArgc(void);
extern "C" char ***_NSGetArgv(void);

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _getArgc
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1getArgc
  (JNIEnv* env , jclass) {
  return *_NSGetArgc();
}

/*
 * Class:     com_google_gwt_dev_shell_mac_LowLevelSaf
 * Method:    _getArgv
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_google_gwt_dev_shell_mac_LowLevelSaf__1getArgv
    (JNIEnv* env, jclass, jint i)
{
  int argc = *_NSGetArgc();
  if (i < 0 || i >= argc) {
    return 0;
  }
  char **argv = *_NSGetArgv();
  return env->NewStringUTF(argv[i]);
}
