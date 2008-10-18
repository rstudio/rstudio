// Copyright 2005-2006 Google Inc.
// All Rights Reserved.

// A slim and portable set of JNI methods for GWT hosted mode

#include <jni.h>
#include <stdlib.h>

extern "C" {

/*
 * Class:     com_google_gwt_dev_shell_LowLevel
 * Method:    newGlobalRefInt
 * Signature: (Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_com_google_gwt_dev_shell_LowLevel__1newGlobalRefInt(JNIEnv* env, jclass, jobject o) {
	return reinterpret_cast<int>(env->NewGlobalRef(o));
}
/*
 * Class:     com_google_gwt_dev_shell_LowLevel
 * Method:    objFromGlobalRefInt
 * Signature: (I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_com_google_gwt_dev_shell_LowLevel__1objFromGlobalRefInt(JNIEnv* env, jclass, jint globalRef) {
	return reinterpret_cast<jobject>(globalRef);
}

/*
 * Class:     com_google_gwt_dev_shell_LowLevel
 * Method:    deleteGlobalRefInt
 * Signature: (I)I
 */
JNIEXPORT void JNICALL Java_com_google_gwt_dev_shell_LowLevel__1deleteGlobalRefInt(JNIEnv* env, jclass, jint globalRef) {
	env->DeleteGlobalRef(reinterpret_cast<jobject>(globalRef));
}

/*
 * Class:     com_google_gwt_dev_shell_LowLevel
 * Method:    getEnv
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_google_gwt_dev_shell_LowLevel__1getEnv(JNIEnv* env, jclass, jstring key) {
    if (!key) {
        // null -> null
        return 0;
    }

	const char* keyPtr = env->GetStringUTFChars(key, 0);
	if (!keyPtr) {
		// There will be a pending OutOfMemoryException on this thread, so leave immediately and let it be thrown.
		return 0;
	}

    const char* valuePtr = getenv(keyPtr);
  	env->ReleaseStringUTFChars(key, keyPtr);

    if (!valuePtr) {
        // no match found
        return 0;
    }

    // return the result
	return env->NewStringUTF(valuePtr);
}

} // extern "C"
