#ifndef _H_mozincludes
#define _H_mozincludes

// Defines private prototypes for copy constructor and assigment operator. Do
// not implement these methods.
#define DISALLOW_EVIL_CONSTRUCTORS(CLASS) \
 private:                                 \
  CLASS(const CLASS&);                    \
  CLASS& operator=(const CLASS&)

#include "xpcom-config.h"
#include "mozilla-config.h"

// See https://developer.mozilla.org/en/SpiderMonkey/1.8.8
#if GECKO_VERSION >= 13000

// See https://bugzilla.mozilla.org/show_bug.cgi?id=417710
//     https://bugzilla.mozilla.org/show_bug.cgi?id=723517 
#define JS_GET_CLASS(cx, obj) JS_GetClass(obj)
#define MOZ_JS_SetPrivate(cx, obj, data) JS_SetPrivate(obj, data)
#define MOZ_JS_SetReservedSlot(cx, obj, index, v) JS_SetReservedSlot(obj, index, v)
#define uintN unsigned int
#define intN int
#define jsdouble double
#else
#define MOZ_JS_SetPrivate(cx, obj, data) JS_SetPrivate(cx, obj, data)
#define MOZ_JS_SetReservedSlot(cx, obj, index, v) JS_SetReservedSlot(cx, obj, index, v)
#endif

#endif
