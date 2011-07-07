#ifndef _H_NPVariantWrapper
#define _H_NPVariantWrapper

#include <string>
#include <ostream>

#ifdef sun
// Sun's cstring doesn't define strlen/etc
#include <string.h>
#endif
#include <cstring>
#include <stdio.h>

#include "Debug.h"
#include "Platform.h"

#include "mozincludes.h"
#include "NPVariantUtil.h"

#include "Value.h"
#include "LocalObjectTable.h"
#include "Plugin.h"
#include "JavaObject.h"

/**
 * Contains NPVariantProxy, NPVariantWrapper, and NPVariantArray
 */

/**
 * Wraps an NPVariant and provides various conversion functions.  The variant
 * provided retained at create time.
 */
class NPVariantProxy {
  friend class NPVariantArray;
private:
  ScriptableInstance& plugin;
  NPVariant& variant;
public:
  NPVariantProxy(ScriptableInstance& plugin, NPVariant& variant)
      : plugin(plugin), variant(variant)
  {
    VOID_TO_NPVARIANT(variant);
  }
  
  NPVariantProxy(ScriptableInstance& plugin, NPVariant& variant, bool noinit)
      : plugin(plugin), variant(variant)
  {
  }

  ~NPVariantProxy() {
  }
  
  operator NPVariant() const {
    return variant;
  }
  
  const NPVariant* operator->() const {
	return &variant;
  }

  const NPVariant* address() const {
    return &variant;
  }

  int isBoolean() const {
    return NPVariantUtil::isBoolean(variant);
  }

  bool getAsBoolean() const {
    return NPVariantUtil::getAsBoolean(variant);
  }

  int isInt() const {
    return NPVariantUtil::isInt(variant);
  }
  
  int getAsInt() const {
    return NPVariantUtil::getAsInt(variant);
  }

  int isNull() const {
    return NPVariantUtil::isNull(variant);
  }
  
  int isObject() const {
    return NPVariantUtil::isObject(variant);
  }
  
  NPObject* getAsObject() const {
    return NPVariantUtil::getAsObject(variant);
  }

  int isString() const {
    return NPVariantUtil::isString(variant);
  }
  
  const NPString* getAsNPString() const {
    return NPVariantUtil::getAsNPString(variant);
  }

  Value getAsValue(ScriptableInstance& scriptInstance, bool unwrapJava = true) const {
    return getAsValue(variant, scriptInstance, unwrapJava);
  }

  static Value getAsValue(const NPVariant& variant, ScriptableInstance& scriptInstance,
      bool unwrapJava = true) {
    Value val;
    if (NPVARIANT_IS_VOID(variant)) {
      val.setUndefined();
    } else if (NPVARIANT_IS_NULL(variant)) {
      val.setNull();
    } else if (NPVARIANT_IS_BOOLEAN(variant)) {
      val.setBoolean(NPVARIANT_TO_BOOLEAN(variant));
    } else if (NPVARIANT_IS_INT32(variant)) {
      val.setInt(NPVARIANT_TO_INT32(variant));      
    } else if (NPVARIANT_IS_DOUBLE(variant)) {
      val.setDouble(NPVARIANT_TO_DOUBLE(variant));
    } else if (NPVARIANT_IS_STRING(variant)) {
      NPString str = NPVARIANT_TO_STRING(variant);
      val.setString(GetNPStringUTF8Characters(str), GetNPStringUTF8Length(str));
    } else if (NPVARIANT_IS_OBJECT(variant)) {
      NPObject* obj = NPVARIANT_TO_OBJECT(variant);
      if (unwrapJava && JavaObject::isInstance(obj)) {
        JavaObject* jObj = static_cast<JavaObject*>(obj);
        val.setJavaObject(jObj->getObjectId());
      } else {
        NPVariant result;
        VOID_TO_NPVARIANT(result);
        if (scriptInstance.tryGetStringPrimitive(obj, result)) {
          NPString str = NPVARIANT_TO_STRING(result);
          val.setString(GetNPStringUTF8Characters(str), GetNPStringUTF8Length(str));
          release(result);
        } else {
          val.setJsObjectId(scriptInstance.getLocalObjectRef(obj));
        }
      }
    } else {
      Debug::log(Debug::Error) << "Unsupported NPVariant type " << variant.type << Debug::flush;
    }
    return val;
  }
  
  /**
   * The incoming variant is not altered, and is not even required to have
   * its contents retained.  Any object will get an extra refcount on it
   * when copied to this variant, and strings will be copied.
   */
  NPVariantProxy& operator=(const NPVariant& newval) {
    assignFrom(variant, newval);
    return *this;
  }

  /**
   * The incoming variant is not altered, and is not even required to have
   * its contents retained.  Any object will get an extra refcount on it
   * when copied to this variant, and strings will be copied.
   */
  static void assignFrom(NPVariant& variant, const NPVariant& newval) {
    release(variant);
    variant = newval;
    if (NPVARIANT_IS_STRING(newval)) {
      int n = variant.value.stringValue.UTF8Length;
      char* strBytes = reinterpret_cast<char*>(NPN_MemAlloc(n));
      memcpy(strBytes, variant.value.stringValue.UTF8Characters, n);
      variant.value.stringValue.UTF8Characters = strBytes;
    } else {
      retain(variant);
    }
  }

  NPVariantProxy& operator=(NPObject* obj) {
    assignFrom(variant, obj);
    return *this;
  }
  
  static void assignFrom(NPVariant& variant, NPObject* obj) {
    release(variant);
    OBJECT_TO_NPVARIANT(obj, variant);
    retain(variant);
  }

  // Convenience method for C++ code
  NPVariantProxy& operator=(int intVal) {
    assignFrom(variant, intVal);
    return *this;
  }

  // Convenience method for C++ code
  static void assignFrom(NPVariant& variant, int intVal) {
    NPVariant newvar;
    INT32_TO_NPVARIANT(intVal, newvar);
    assignFrom(variant, newvar);
  }

  // Convenience method for C++ code
  NPVariantProxy& operator=(const std::string& strval) {
    assignFrom(variant, strval);
    return *this;
  }

  // Convenience method for C++ code
  static void assignFrom(NPVariant& variant, const std::string& strval) {
    NPVariant newvar;
    STDSTRING_TO_NPVARIANT(strval, newvar);
    assignFrom(variant, newvar);
  }
  
  // Convenience method for C++ code
  NPVariantProxy& operator=(const char* strval) {
    assignFrom(variant, strval);
    return *this;
  }

  // Convenience method for C++ code
  static void assignFrom(NPVariant& variant, const char* strval) {
    NPVariant newvar;
    STRINGZ_TO_NPVARIANT(strval, newvar);
    assignFrom(variant, newvar);
  }

  NPVariantProxy& operator=(const Value& newval) {
    assignFrom(plugin, variant, newval);
    return *this;
  }

  static void assignFrom(ScriptableInstance& plugin, NPVariant& variant, const Value& newval) {
    NPVariant newvar;
    VOID_TO_NPVARIANT(newvar);
    if (newval.isBoolean()) {
      BOOLEAN_TO_NPVARIANT(newval.getBoolean(), newvar);
    } else if (newval.isByte()) {
      INT32_TO_NPVARIANT(newval.getByte(), newvar);
    } else if (newval.isChar()) {
      INT32_TO_NPVARIANT(newval.getChar(), newvar);
    } else if (newval.isShort()) {
      INT32_TO_NPVARIANT(newval.getShort(), newvar);
    } else if (newval.isInt()) {
      int value = newval.getInt();
      // Firefox NPAPI bug: 32-bit ints get mapped to int jsvals, regardless of range.
      // However, int jsvals are 31 bits, so we need to use a double if the value is
      // not representable in a 31 bit signed 2's-complement value.
      if (value >= 0x40000000 || value < -0x40000000) {
        DOUBLE_TO_NPVARIANT(static_cast<double>(value), newvar);
      } else {
        INT32_TO_NPVARIANT(value, newvar);
      }
    } else if (newval.isFloat()) {
      DOUBLE_TO_NPVARIANT(newval.getFloat(), newvar);
    } else if (newval.isDouble()) {
      DOUBLE_TO_NPVARIANT(newval.getDouble(), newvar);
    } else if (newval.isNull()) {
      NULL_TO_NPVARIANT(newvar);
    } else if (newval.isUndefined()) {
      VOID_TO_NPVARIANT(newvar);
    } else if (newval.isString()) {
      assignFrom(variant, newval.getString());
      return;
    } else if (newval.isJavaObject()) {
      if (1) {
        JavaObject* jObj = plugin.createJavaWrapper(newval.getJavaObjectId());
        NPObject* obj = jObj;
        OBJECT_TO_NPVARIANT(obj, newvar);
      } else {
        VOID_TO_NPVARIANT(newvar);
      }
    } else if (newval.isJsObject()) {
      OBJECT_TO_NPVARIANT(plugin.getLocalObject(newval.getJsObjectId()),
    	  newvar);
    } else {
      Debug::log(Debug::Error) << "Unsupported NPVariant type " << newval.getType() << Debug::flush;
    }
    assignFrom(variant, newvar);
  }

  std::string toString() const {
    return toString(variant);
  }

  static std::string toString(const NPVariant& variant) {
    std::string retval;
    // TODO(jat): remove sprintfs
    char buf[40];
    NPObject* npObj;
    switch (variant.type) {
      case NPVariantType_Void:
        retval = "undef";
        break;
      case NPVariantType_Null:
        retval = "null";
        break;
      case NPVariantType_Bool:
        retval = "bool(";
        retval += (NPVARIANT_TO_BOOLEAN(variant) ? "true" : "false");
        retval += ')';
        break;
      case NPVariantType_Int32:
        retval = "int(";
        snprintf(buf, sizeof(buf), "%d)", NPVARIANT_TO_INT32(variant));
        retval += buf;
        break;
      case NPVariantType_Double:
        retval = "double(";
        snprintf(buf, sizeof(buf), "%g)", NPVARIANT_TO_DOUBLE(variant));
        retval += buf;
        break;
      case NPVariantType_String:
        {
          retval = "string(";
          NPString str = NPVARIANT_TO_STRING(variant);
          retval += std::string(str.UTF8Characters, str.UTF8Length);
          retval += ')';
        }
        break;
      case NPVariantType_Object:
        npObj = NPVARIANT_TO_OBJECT(variant);
        if (JavaObject::isInstance(npObj)) {
          JavaObject* javaObj = static_cast<JavaObject*>(npObj);
          snprintf(buf, sizeof(buf), "javaObj(id=%d, ", javaObj->getObjectId());
        } else {
          snprintf(buf, sizeof(buf), "jsObj(class=%p, ", npObj->_class);
        }
        retval = buf;
        snprintf(buf, sizeof(buf), "%p)", npObj);
        retval += buf;
        break;
      default:
        snprintf(buf, sizeof(buf), "Unknown type %d", variant.type);
        retval = buf;
        break;
    }
    return retval;
  }

public:
  void release() {
    release(variant);
  }

  static void release(NPVariant& variant) {
    NPN_ReleaseVariantValue(&variant);
  }

  void retain() {
    retain(variant);
  }

  static void retain(NPVariant& variant) {
    if (NPVARIANT_IS_OBJECT(variant)) {
      NPN_RetainObject(NPVARIANT_TO_OBJECT(variant));
    }
  }
};

inline Debug::DebugStream& operator<<(Debug::DebugStream& dbg, const NPVariant& var) {
  return dbg << NPVariantProxy::toString(var);
}

inline Debug::DebugStream& operator<<(Debug::DebugStream& dbg, const NPVariantProxy& var) {
  return dbg << var.toString();
}

/**
 * Variation of NPVariantProxy that provides its own variant and always frees it
 * when the wrapper goes away.
 */
class NPVariantWrapper  {
private:
  ScriptableInstance& plugin;
  NPVariant variant;
public:
  NPVariantWrapper(ScriptableInstance& plugin) : plugin(plugin) {
    VOID_TO_NPVARIANT(variant);
  }

  ~NPVariantWrapper() {
    release();
  }

  operator NPVariant() const {
    return variant;
  }

  const NPVariant* operator->() const {
  return &variant;
  }

  const NPVariant* address() const {
    return &variant;
  }

  /**
   * Get the address for use as a return value.  Since the value can be trashed,
   * we need to release any data we currently hold.
   */
  NPVariant* addressForReturn() {
    NPVariantProxy::release(variant);
    VOID_TO_NPVARIANT(variant);
    NPVariantProxy::retain(variant); // does nothing, present for consistency
    return &variant;
  }
  
  bool isBoolean() const {
    return NPVariantUtil::isBoolean(variant);
  }

  int isInt() const {
    return NPVariantUtil::isInt(variant);
  }
  
  int isObject() const {
    return NPVariantUtil::isObject(variant);
  }
  
  int isString() const {
    return NPVariantUtil::isString(variant);
  }
  
  bool getAsBoolean() const {
    return NPVariantUtil::getAsBoolean(variant);
  }

  int getAsInt() const {
    return NPVariantUtil::getAsInt(variant);
  }

  NPObject* getAsObject() const {
    return NPVariantUtil::getAsObject(variant);
  }

  const NPString* getAsNPString() const {
    return NPVariantUtil::getAsNPString(variant);
  }

  Value getAsValue(ScriptableInstance& scriptInstance, bool unwrapJava = true) const {
    return NPVariantProxy::getAsValue(variant, scriptInstance, unwrapJava);
  }

  /**
   * The incoming variant is not altered, and is not even required to have
   * its contents retained.  Any object will get an extra refcount on it
   * when copied to this variant, and strings will be copied.
   */
  NPVariantWrapper& operator=(const NPVariant& newval) {
    NPVariantProxy::assignFrom(variant, newval);
    return *this;
  }

  NPVariantWrapper& operator=(const Value& newval) {
    NPVariantProxy::assignFrom(plugin, variant, newval);
    return *this;
  }
  
  NPVariantWrapper& operator=(NPObject* obj) {
    NPVariantProxy::assignFrom(variant, obj);
    return *this;
  }
  
  // Convenience method for C++ code
  NPVariantWrapper& operator=(const std::string& strval) {
    NPVariantProxy::assignFrom(variant, strval);
    return *this;
  }

  // Convenience method for C++ code
  NPVariantWrapper& operator=(const char* strval) {
    NPVariantProxy::assignFrom(variant, strval);
    return *this;
  }

  // Convenience method for C++ code
  NPVariantWrapper& operator=(int intval) {
    NPVariantProxy::assignFrom(variant, intval);
    return *this;
  }

  void release() {
    NPVariantProxy::release(variant);
  }

  void retain() {
    NPVariantProxy::retain(variant);
  }

  std::string toString() const {
    return NPVariantProxy::toString(variant);
  }
};

inline Debug::DebugStream& operator<<(Debug::DebugStream& dbg, const NPVariantWrapper& var) {
  dbg << var.toString();
  return dbg;
}

/**
 * Maintains an array of NPVariants and cleans them up when it is destroyed.
 */
class NPVariantArray {
private:
	ScriptableInstance& plugin;
  int size;
  NPVariant* args;

public:
  NPVariantArray(ScriptableInstance& plugin, int size) : plugin(plugin), size(size) {
    args = new NPVariant[size];
    for (int i = 0; i < size; ++i) {
      VOID_TO_NPVARIANT(args[i]);
    }
  }

  ~NPVariantArray() {
    for (int i = 0; i < size; ++i) {
      NPN_ReleaseVariantValue(&args[i]);
    }
    delete [] args;
  }

  const NPVariant* getArray() const {
    return args;
  }

  int getSize() const {
    return size;
  }

  const NPVariant& operator[](int idx) const {
    if (idx >= size) {
      printf("NPVariantArray[idx=%d] const: size=%d\n", idx, size);
    }
    return args[idx];
  }

  NPVariantProxy operator[](int idx) {
    if (idx >= size) {
      printf("NPVariantArray[idx=%d]: size=%d\n", idx, size);
    }
    return NPVariantProxy(plugin, args[idx], true);
  }
};

inline Debug::DebugStream& operator<<(Debug::DebugStream& dbg, const NPVariantArray& var) {
  dbg << "[";
  for (int i = 0; i < var.getSize(); ++i) {
    dbg << " " << var[i]; 
  }
  dbg << " ]";
  return dbg;
}

#endif
