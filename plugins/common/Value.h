#ifndef _H_Value
#define _H_Value
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

#ifndef _WINDOWS
// TODO(jat): remove; for abort() which should probably go away
#include <stdlib.h>
#endif

#include <string>

#include "Debug.h"

#include "BrowserChannel.h"

namespace gwt {
class Value {
public:
  enum ValueType {
    NULL_TYPE = VALUE_TYPE_NULL,
    BOOLEAN = VALUE_TYPE_BOOLEAN,
    BYTE = VALUE_TYPE_BYTE,
    CHAR = VALUE_TYPE_CHAR,
    SHORT = VALUE_TYPE_SHORT,
    INT = VALUE_TYPE_INT,
    LONG = VALUE_TYPE_LONG,
    FLOAT = VALUE_TYPE_FLOAT,
    DOUBLE = VALUE_TYPE_DOUBLE,
    STRING = VALUE_TYPE_STRING,
    JAVA_OBJECT = VALUE_TYPE_JAVA_OBJECT,
    JS_OBJECT = VALUE_TYPE_JS_OBJECT,
    UNDEFINED = VALUE_TYPE_UNDEFINED
  };

private:
  ValueType type;
  union {
    bool  boolValue;
    unsigned char byteValue;
    unsigned short charValue;
    double doubleValue;
    float floatValue;
    int32_t intValue;
    int64_t longValue;
    short shortValue;
    std::string* stringValue;
  } value;

public:
  Value() {
    type = UNDEFINED;
  }

  Value(const Value& other) {
    copyValue(other);
  }

  Value& operator=(const Value& other) {
    clearOldValue();
    copyValue(other);
    return *this;
  }

  ~Value() {
    clearOldValue();
  }

  bool getBoolean() const {
    assertType(BOOLEAN);
    return value.boolValue;
  }

  unsigned char getByte() const {
    assertType(BYTE);
    return value.byteValue;
  }

  unsigned short getChar() const {
    assertType(CHAR);
    return value.charValue;
  }

  double getDouble() const {
    assertType(DOUBLE);
    return value.doubleValue;
  }

  float getFloat() const {
    assertType(FLOAT);
    return value.floatValue;
  }

  int getInt() const {
    assertType(INT);
    return value.intValue;
  }

  int getJavaObjectId() const {
    assertType(JAVA_OBJECT);
    return value.intValue;
  }

  int getJsObjectId() const {
    assertType(JS_OBJECT);
    return value.intValue;
  }

  int64_t getLong() const {
    assertType(LONG);
    return value.longValue;
  }

  short getShort() const {
    assertType(SHORT);
    return value.shortValue;
  }

  const std::string getString() const {
    assertType(STRING);
    return std::string(*value.stringValue);
  }

  ValueType getType() const {
    return type;
  }

  bool isBoolean() const {
    return type == BOOLEAN;
  }

  bool isByte() const {
    return type == BYTE;
  }

  bool isChar() const {
    return type == CHAR;
  }

  bool isDouble() const {
    return type == DOUBLE;
  }

  bool isFloat() const {
    return type == FLOAT;
  }

  bool isInt() const {
    return type == INT;
  }

  bool isJavaObject() const {
    return type == JAVA_OBJECT;
  }

  bool isJsObject() const {
    return type == JS_OBJECT;
  }

  bool isLong() const {
    return type == LONG;
  }

  bool isNull() const {
    return type == NULL_TYPE;
  }

  bool isNumber() const {
    switch (type) {
      case BYTE:
      case CHAR:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;
      default:
        return false;
    }
  }

  bool isPrimitive() const {
    switch (type) {
      case BOOLEAN:
      case BYTE:
      case CHAR:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;
      default:
        return false;
    }
  }

  bool isShort() const {
    return type == SHORT;
  }

  bool isString() const {
    return type == STRING;
  }

  bool isUndefined() const {
    return type == UNDEFINED;
  }

  void setBoolean(bool val) {
    clearOldValue();
    type = BOOLEAN;
    value.boolValue = val;
  }

  void setByte(unsigned char val) {
    clearOldValue();
    type = BYTE;
    value.byteValue = val;
  }

  void setChar(unsigned short val) {
    clearOldValue();
    type = CHAR;
    value.charValue = val;
  }

  void setDouble(double val) {
    clearOldValue();
    type = DOUBLE;
    value.doubleValue = val;
  }

  void setDouble(const double* val) {
    clearOldValue();
    type = DOUBLE;
    value.doubleValue = *val;
  }

  void setFloat(float val) {
    clearOldValue();
    type = FLOAT;
    value.floatValue = val;
  }

  void setInt(int val) {
    clearOldValue();
    type = INT;
    value.intValue = val;
  }

  void setJavaObject(int objectId) {
    clearOldValue();
    type = JAVA_OBJECT;
    value.intValue = objectId;
  }

  void setJsObjectId(int val) {
    clearOldValue();
    type = JS_OBJECT;
    value.intValue = val;
  }

  void setLong(int64_t val) {
    clearOldValue();
    type = LONG;
    value.longValue = val;
  }

  void setNull() {
    clearOldValue();
    type = NULL_TYPE;
  }

  void setShort(short val) {
    clearOldValue();
    type = SHORT;
    value.shortValue = val;
  }

  void setString(const char* chars, int len) {
    setString(std::string(chars, len));
  }

  void setString(const std::string& val) {
    clearOldValue();
    type = STRING;
    value.stringValue = new std::string(val);
  }

  void setUndefined() {
    clearOldValue();
    type = UNDEFINED;
  }

  std::string toString() const {
    char buf[64];
    switch (type) {
      case NULL_TYPE:
        return "null";
      case BOOLEAN:
        snprintf(buf, sizeof(buf), "boolean(%s)", getBoolean() ? "true"
            : "false");
        return std::string(buf);
      case BYTE:
        snprintf(buf, sizeof(buf), "byte(%d)", getByte());
        return std::string(buf);
      case CHAR:
        snprintf(buf, sizeof(buf), "char(%d)", getChar());
        return std::string(buf);
      case SHORT:
        snprintf(buf, sizeof(buf), "short(%d)", getShort());
        return std::string(buf);
      case INT:
        snprintf(buf, sizeof(buf), "int(%d)", getInt());
        return std::string(buf);
      case LONG:
        snprintf(buf, sizeof(buf), "long(%lld)",
            static_cast<long long>(getLong()));
        return std::string(buf);
      case FLOAT:
        snprintf(buf, sizeof(buf), "float(%g)", getFloat());
        return std::string(buf);
      case DOUBLE:
        snprintf(buf, sizeof(buf), "double(%g)", getDouble());
        return std::string(buf);
      case STRING:
        snprintf(buf, sizeof(buf), "string(%.20s)", getString().c_str());
        return std::string(buf);
      case JAVA_OBJECT:
        snprintf(buf, sizeof(buf), "JavaObj(%d)", getJavaObjectId());
        return std::string(buf);
      case JS_OBJECT:
        snprintf(buf, sizeof(buf), "JsObj(%d)", getJsObjectId());
        return std::string(buf);
      case UNDEFINED:
        return "undefined";
      default:
        return "Unknown type";
    }
  }

private:
  void assertType(ValueType reqType) const {
    if (type != reqType) {
      Debug::log(Debug::Error) << "Value::assertType - expecting type "
          << int(reqType) << ", was " << int(type) << Debug::flush;
      // TODO(jat): is this portable?  Should we do something else here?
      abort();
    }
  }

  void clearOldValue() {
    if (type == STRING) {
      delete value.stringValue;
      type = UNDEFINED;
    }
  }

  // Precondition: existing value, if any, has been cleared
  void copyValue(const Value& other) {
    type = other.type;
    value = other.value;
    // handle deep copies of value types that need it
    switch (type) {
      case STRING:
        value.stringValue = new std::string(*value.stringValue);
        break;
      default:
        // no other values need deep copies
        break;
    }
  }
};

inline Debug::DebugStream& operator<<(Debug::DebugStream& dbg, const Value& val) {
  if (dbg.isActive()) {
    dbg << val.toString();
  }
  return dbg;
}

} // namespace gwt

#endif
