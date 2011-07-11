#ifndef _H_NPVariantUtil
#define _H_NPVariantUtil

#include "Debug.h"
#include "mozincludes.h"

/**
 * Wraps an NPVariant and provides various helper functions
 */
class NPVariantUtil {
public:
  static int isBoolean(const NPVariant& variant) {
    return NPVARIANT_IS_BOOLEAN(variant);
  }

  static bool getAsBoolean(const NPVariant& variant) {
    return NPVARIANT_TO_BOOLEAN(variant);
  }

  // Return true if the variant is holding a regular integer or an integral double.
  static int isInt(const NPVariant& variant) {
    if (NPVARIANT_IS_INT32(variant)) {
      return 1;
    } else if (NPVARIANT_IS_DOUBLE(variant)) {
      // As of http://trac.webkit.org/changeset/72974 we get doubles for all
      // numerical variants out of V8.
      double d = NPVARIANT_TO_DOUBLE(variant);
      int i = static_cast<int>(d);
      // Verify that d is an integral value in range.
      return (d == static_cast<double>(i));
    } else {
      return 0;
    }
  }

  static int getAsInt(const NPVariant& variant) {
    if (isInt(variant)) {
      if (NPVARIANT_IS_INT32(variant)) {
        return NPVARIANT_TO_INT32(variant);
      } else if (NPVARIANT_IS_DOUBLE(variant)) {
        return static_cast<int>(NPVARIANT_TO_DOUBLE(variant));
      }
    }

    Debug::log(Debug::Error) << "getAsInt: variant " <<
      NPVariantUtil::toString(variant) << "not int" << Debug::flush;
    return 0;
  }

  static int isNull(const NPVariant& variant) {
    return NPVARIANT_IS_NULL(variant);
  }

  static int isObject(const NPVariant& variant) {
    return NPVARIANT_IS_OBJECT(variant);
  }

  static NPObject* getAsObject(const NPVariant& variant) {
    if (NPVARIANT_IS_OBJECT(variant)) {
      return NPVARIANT_TO_OBJECT(variant);
    }
    Debug::log(Debug::Error) << "getAsObject: variant not object" << Debug::flush;
    return 0;
  }

  static int isString(const NPVariant& variant) {
    return NPVARIANT_IS_STRING(variant);
  }

  static const NPString* getAsNPString(const NPVariant& variant) {
    if (NPVARIANT_IS_STRING(variant)) {
      return &NPVARIANT_TO_STRING(variant);
    }
    Debug::log(Debug::Error) << "getAsNPString: variant not string" << Debug::flush;
    return 0;
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
        snprintf(buf, sizeof(buf), "obj(class=%p, ", npObj->_class);
        retval = buf;
        snprintf(buf, sizeof(buf), "count=%d, ", npObj->referenceCount);
        retval += buf;
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

};

#endif
