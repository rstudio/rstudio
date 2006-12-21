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

// A very slim and portable set of JNI methods for GWT.

#include <jni.h>
#include <windows.h>
#include "wininet.h"

// A home-brewed vector class to eliminate dependence on STL.
// Reuse at your own peril, it hasn't been tested to do anything
// more than we're actually using it for here.
struct char_vector {
    char_vector(long initSize=0): sz(0), cap(0), buf(0) {
        resize(initSize);
    }
    
    ~char_vector() {
        clear();
    }

    char& operator[](long i) {
        return buf[i];
    }

    void append(const char* start, const char* end) {
        long len = end - start;
        long cursize = sz;
        resize(sz + len);
        memcpy(&buf[cursize], start, len);
    }

    void clear() {
        if (buf)
            free(buf);
        buf = 0;
        sz = cap = 0;
    }

    void reserve(long newcap) {
        if (newcap <= cap)
            return;
        char* newbuf = (char*)malloc(newcap);
        if (!newbuf)
            return;
        if (buf) {
            memcpy(newbuf, buf, sz);
            free(buf);
        }
        buf = newbuf;
        cap = newcap;
    }

    void resize(long newsize) {
        reserve(newsize);
        sz = newsize;
    }

    long size() {
        return sz;
    }

private:
    long sz;
    long cap;
    char* buf;
};

// Does an HTTP GET on the specified URL using the WinInet subsystem, which respects proxy settings.
// Returns a status code such that 
//      0  Success
//    < 0  Failure at some step (see the code below)
//    > 0  Windows system error code
//
int fetch(const char* userAgent, const char* url, char_vector& response) {
    int status = -1;
    HINTERNET h1 = InternetOpenA(userAgent, INTERNET_OPEN_TYPE_PRECONFIG, NULL, NULL, 0);
    if (h1 != NULL) {
        status = -2;
        DWORD flags = INTERNET_FLAG_PRAGMA_NOCACHE | INTERNET_FLAG_RELOAD | INTERNET_FLAG_NO_COOKIES | INTERNET_FLAG_NO_UI;
        HINTERNET h2 = InternetOpenUrlA(h1, url, "", -1L, flags, 0);
        if (h2 != NULL) {
            status = -3;
            const int MAX_BUFFER = 1024 * 1024; // don't grow past this ever
            bool notDone = true;
            response.clear();
            response.reserve(4096);
            char_vector chunk(4096); // initial buffer size
            while (notDone && chunk.size() < MAX_BUFFER) {
                DWORD toRead = (DWORD)chunk.size();
                DWORD didRead;
                while (true) {
                    if (InternetReadFile(h2, (LPVOID)&chunk[0], toRead, &didRead)) {
                        if (didRead != 0) {
                            // Copy to response.
                            //
                            response.append(&chunk[0], &chunk[0] + didRead);

                            // Keep going.
                            //
                        } else {
                            // EOF.
                            //
                            status = 0;
                            notDone = false;
                            break;
                        }
                    } else {
                        // Oops -- why?
                        //
                        DWORD err = GetLastError();
                        if (err == ERROR_INSUFFICIENT_BUFFER) {
                            // Grow the buffer and retry.
                            //
                            chunk.resize(toRead * 2);
                            break;
                        } else {
                            // Give up.
                            //
                            status = err;
                            notDone = false;
                            break;
                        }
                    }
                }
            }
            InternetCloseHandle(h2);
        }
        InternetCloseHandle(h1);
    }
    return status;
}

extern "C" {

class UTFChars {
public:
    UTFChars(JNIEnv* env, jstring str): m_env(env), m_str(str) {
        m_ptr = env->GetStringUTFChars(str, NULL);
    }

    ~UTFChars() {
        if (m_ptr) {
            m_env->ReleaseStringUTFChars(m_str, m_ptr);
        }
    }

    operator const char*() const {
        return m_ptr;
    }

    JNIEnv* m_env;
    jstring m_str;
    const char* m_ptr;
};

/*
 * Class:     com_google_gwt_dev_shell_ie_LowLevelIE6
 * Method:    _httpGet
 * Signature: (Ljava/lang/String;Ljava/lang/String;[[B)I
 */
JNIEXPORT jint JNICALL Java_com_google_gwt_dev_shell_ie_LowLevelIE6__1httpGet(JNIEnv* env, jclass, jstring userAgent, jstring url, jobjectArray out) {
    UTFChars userAgentChars(env, userAgent);
    if (!userAgentChars || env->ExceptionCheck()) {
		return -100;
	}

    UTFChars urlChars(env, url);
    if (!urlChars || env->ExceptionCheck()) {
		return -101;
	}

    // Do the fetch.
    //
    char_vector response;
    int status = fetch(userAgentChars, urlChars, response);

    if (status != 0) {
        // Failure.
        //
        return status;
    }

    // Copy the response.
    //
    jbyteArray byteArray = env->NewByteArray(response.size());
    if (!byteArray || env->ExceptionCheck()) {
		return -102;
	}

    jbyte* bytes = env->GetByteArrayElements(byteArray, NULL);
    if (!bytes || env->ExceptionCheck()) {
		return -103;
    }

    memcpy(bytes, &response[0], response.size());

    env->ReleaseByteArrayElements(byteArray, bytes, 0);

    // May throw immediately after return if out array is bad.
    //
    env->SetObjectArrayElement(out, 0, byteArray);  

	return 0;
}

} // extern "C"
