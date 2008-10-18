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

#ifndef JNI_LINUX_JSSTRINGWRAP_H_
#define JNI_LINUX_JSSTRINGWRAP_H_

/*
 * Wrapper arouond JavaScript Strings, keeps pointers to unpacked strings
 * and makes sure they are not cleaned up early. 
 */
class JsStringWrap {
  private:
    JSContext* context_;
    JSString* string_;
    const char* bytes_;
    const wchar_t* chars_;

  public:
    JsStringWrap(JSContext* context, JSString* str)
        : context_(context), string_(str), bytes_(0), chars_(0) {
      JS_AddRoot(context_, &string_);
      JS_AddRoot(context_, &bytes_);
      JS_AddRoot(context_, &chars_);
    }
    JsStringWrap(JSContext* context, jsval str)
        : context_(context), string_(JSVAL_TO_STRING(str)), bytes_(0), chars_(0) {
      JS_AddRoot(context_, &string_);
      JS_AddRoot(context_, &bytes_);
      JS_AddRoot(context_, &chars_);
    }
    ~JsStringWrap() {
      JS_RemoveRoot(context_, &string_);
      JS_RemoveRoot(context_, &bytes_);
      JS_RemoveRoot(context_, &chars_);
    }
    const char* bytes() {
      if (!bytes_) bytes_ = JS_GetStringBytes(string_);
      return bytes_;
    }
    const wchar_t* chars() {
      if (!chars_) {
        chars_ = reinterpret_cast<wchar_t*>(JS_GetStringChars(string_));
      }
      return chars_;
    }
    int length() {
      return JS_GetStringLength(string_);
    }
};

#endif // JNI_LINUX_JSSTRINGWRAP_H_
