/*
 * JsonTests.cpp
 *
 * Copyright (C) 2018-19 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <iostream>
#include <tests/TestThat.hpp>

#include <core/json/JsonRpc.hpp>

namespace rstudio {
namespace core {
namespace tests {

namespace {

json::Object createObject()
{
   json::Object object;
   object["a"] = true;
   object["b"] = false;
   object["c"] = 1000;
   object["d"] = (uint64_t)18446744073709550615U;
   object["e"] = 246.9;
   object["f"] = std::string("Hello world");

   json::Array simpleArray;
   simpleArray.push_back(json::Value(100));
   simpleArray.push_back(json::Value(200));
   simpleArray.push_back(json::Value(300));
   object["g"] = simpleArray;

   json::Array objectArray;

   json::Object obj1;
   obj1["a1"] = "a1";
   obj1["a2"] = 1;

   json::Object obj2;
   obj2["b1"] = "b1";
   obj2["b2"] = 2;

   objectArray.push_back(json::Value(obj1));
   objectArray.push_back(json::Value(obj2));

   object["h"] = objectArray;

   json::Object obj3;
   obj3["nestedValue"] = 9876.324;
   json::Object obj4;
   obj4["a"] = "Inner object a";
   json::Array innerArray;
   innerArray.push_back(json::Value(1));
   innerArray.push_back(json::Value(5));
   innerArray.push_back(json::Value(6));
   obj4["b"] = innerArray;
   obj4["c"] = json::Value();
   obj3["inner"] = obj4;
   object["i"] = obj3;

   return object;
}

json::Object returnObject()
{
   std::string jsonStr = "{\"a\": 5}";
   json::Value val;
   REQUIRE(!val.parse(jsonStr));

   return val.getValue<json::Object>();
}

json::Value createValue()
{
   json::Object obj = createObject();
   return std::move(obj);
}

json::Value getValue()
{
   std::string jsonStr = "{\"a\": 5}";
   json::Value val;
   REQUIRE(!val.parse(jsonStr));

   json::Object obj = val.getObject();

   // must return a copy instead of actual reference
   return obj["a"].clone();
}

json::Object s_object;

json::Value getGlobalValue(std::string scope,
                           std::string name)
{
   json::Object::Iterator i = s_object.find(scope);
   if (i == s_object.end())
   {
      return json::Value();
   }
   else
   {
      if (!json::isType<core::json::Object>((*i).getValue()))
         return json::Value();
      json::Object scopeObject = (*i).getValue().getObject();
      return scopeObject[name];
   }
}

void insertGlobalValue(const std::string& scope,
                       const std::string& entryName,
                       const json::Value& entryValue)
{
   json::Object::Iterator pos = s_object.find(scope);
   if (pos == s_object.end())
   {
      json::Object newScopeObject;
      s_object.insert(scope, newScopeObject);
   }

   const json::Value& scopeValue = s_object[scope];
   json::Object scopeObject = scopeValue.getObject();

   // insert the value into the scope
   scopeObject.insert(entryName, entryValue);
}

} // anonymous namespace

TEST_CASE("Json")
{
   SECTION("Initialize")
   {
      json::Object objectA;
      objectA["1"] = 1;
      objectA["2"] = 2;

      json::Object objectC;
      objectC["1"] = "a";
      objectC["2"] = "b";

      s_object["a"] = objectA;
      s_object["b"] = 5;
      s_object["c"] = objectC;
   }

   SECTION("Null test")
   {
      std::string json = "{\"a\": 1, \"b\": null}";

      json::Value value;
      REQUIRE(!value.parse(json));

      REQUIRE(value.getType() == json::Type::OBJECT);
      json::Object obj = value.getObject();

      REQUIRE(obj["a"].getType() == json::Type::INTEGER);
      REQUIRE(obj["b"].getType() == json::Type::NULL_TYPE);

      std::string bVal;
      REQUIRE_FALSE(json::getOptionalParam(obj, "b", std::string("DEFAULT"), &bVal));
      REQUIRE(bVal == "DEFAULT");

      REQUIRE(json::typeAsString(obj["b"]) == "<Null>");
   }

   SECTION("Can construct simple json object")
   {
      json::Object obj;

      obj["a"] = "Hello";
      REQUIRE(obj["a"].getString() == "Hello");

      obj["b"] = "world";
      REQUIRE(obj["b"].getString() == "world");

      obj["c"] = 25;
      REQUIRE(obj["c"].getInt() == 25);

      json::Array array;
      array.push_back(json::Value(1));
      array.push_back(json::Value(2));
      array.push_back(json::Value(3));

      obj["d"] = array;

      int expectedNum = 1;
      for (const json::Value& val : obj["d"].getArray())
      {
         int num = val.getInt();
         REQUIRE(num == expectedNum);
         expectedNum++;
      }

      REQUIRE(obj["d"].getArray()[0].getInt() == 1);
      REQUIRE(obj["d"].getArray()[1].getInt() == 2);
      REQUIRE(obj["d"].getArray()[2].getInt() == 3);

      json::Object innerObj;
      innerObj["a"] = "Inner hello";
      obj["e"] = innerObj;

      REQUIRE(obj["e"].getObject()["a"].getString() == "Inner hello");

      std::string serialized = obj.write();
      std::string expected = "{\"a\":\"Hello\",\"b\":\"world\",\"c\":25,\"d\":[1,2,3],\"e\":{\"a\":\"Inner hello\"}}";
      REQUIRE(serialized == expected);
   }

   SECTION("Can deserialize simple json object")
   {
      std::string json = "{\"a\":\"Hello\",\"b\":\"world\",\"c\":25,\"c2\":25.5,\"d\":[1,2,3],\"e\":{\"a\":\"Inner hello\"}}";

      json::Value value;
      REQUIRE(!value.parse(json));

      REQUIRE(value.getType() == json::Type::OBJECT);
      json::Object obj = value.getObject();

      REQUIRE(obj["a"].getType() == json::Type::STRING);
      REQUIRE(obj["a"].getString() == "Hello");

      REQUIRE(obj["b"].getType() == json::Type::STRING);
      REQUIRE(obj["b"].getString() == "world");

      REQUIRE(obj["c"].getType() == json::Type::INTEGER);
      REQUIRE(obj["c"].getInt() == 25);

      REQUIRE(obj["c2"].getType() == json::Type::REAL);
      REQUIRE(obj["c2"].getDouble() == Approx(25.5));

      REQUIRE(obj["d"].getType() == json::Type::ARRAY);
      json::Array array = obj["d"].getArray();
      REQUIRE(array[0].getInt() == 1);
      REQUIRE(array[1].getInt() == 2);
      REQUIRE(array[2].getInt() == 3);

      REQUIRE(obj["e"].getType() == json::Type::OBJECT);
      json::Object innerObj = obj["e"].getObject();
      REQUIRE(innerObj["a"].getType() == json::Type::STRING);
      REQUIRE(innerObj["a"].getString() == "Inner hello");
   }

   SECTION("Can nest objects within arrays")
   {
      json::Array array;

      json::Object obj1;
      obj1["1"] = "obj1";
      obj1["2"] = 1;

      json::Object obj2;
      obj2["1"] = "obj2";
      obj2["2"] = 2;

      array.push_back(json::Value(obj1));
      array.push_back(json::Value(obj2));

      REQUIRE(array[0].getObject()["1"].getString() == "obj1");
      REQUIRE(array[0].getObject()["2"].getInt() == 1);
      REQUIRE(array[1].getObject()["1"].getString() == "obj2");
      REQUIRE(array[1].getObject()["2"].getInt() == 2);
   }

   SECTION("Can iterate arrays")
   {
      json::Array arr;
      arr.push_back(json::Value(1));
      arr.push_back(json::Value(2));
      arr.push_back(json::Value(3));

      json::Array arr2;
      arr2.push_back(json::Value(4));
      arr2.push_back(json::Value(5));
      arr2.push_back(json::Value(6));

      std::transform(arr2.begin(),
                     arr2.end(),
                     std::back_inserter(arr),
                     [=](json::Value val) { return json::Value(val.getInt() * 2); });

      json::Array::Iterator iter = arr.begin();
      REQUIRE((*iter++).getInt() == 1);
      REQUIRE((*iter++).getInt() == 2);
      REQUIRE((*iter++).getInt() == 3);
      REQUIRE((*iter++).getInt() == 8);
      REQUIRE((*iter++).getInt() == 10);
      REQUIRE((*iter++).getInt() == 12);
      REQUIRE(iter == arr.end());

      json::Array::ReverseIterator riter = arr.rbegin();
      REQUIRE((*riter++).getInt() == 12);
      REQUIRE((*riter++).getInt() == 10);
      REQUIRE((*riter++).getInt() == 8);
      REQUIRE((*riter++).getInt() == 3);
      REQUIRE((*riter++).getInt() == 2);
      REQUIRE((*riter++).getInt() == 1);
      REQUIRE(riter == arr.rend());

      std::string jsonStr = "[1, 2, 3, 4, 5]";
      json::Value val;
      val.parse(jsonStr);
      const json::Array& valArray = val.getArray();

      int sum = 0;
      for (const json::Value& val : valArray)
         sum += val.getInt();

      REQUIRE(sum == 15);
   }

   SECTION("Ref/copy semantics")
   {
      std::string json = R"({"a":"Hello","b":"world","c":25,"c2":25.5,"d":[1,2,3],"e":{"a":"Inner hello"}})";

      json::Value value;
      REQUIRE(!value.parse(json));

      json::Object obj1 = value.getObject();
      json::Object obj2 = value.getValue<json::Object>();

      obj1["a"] = "Modified Hello";
      obj2["b"] = "modified world";

      // should be a reference here
      json::Array arr1 = value.getObject()["d"].getArray();

      // should be a copy here
      json::Array arr2 = value.getObject()["d"].getValue<json::Array>();

      // should be a reference
      json::Array arr3 = value.getObject()["d"].getArray();

      // another copy
      json::Array arr4 = arr3;

      arr1[1] = 4;
      arr2[2] = 6;
      arr3[2] = 5;
      arr4[2] = 6;

      REQUIRE(value.getObject()["a"].getString() == "Modified Hello");
      REQUIRE(obj2["a"].getString() == "Hello");
      REQUIRE(value.getObject()["b"].getString() == "world");
      REQUIRE(obj2["b"].getString() == "modified world");

      REQUIRE(value.getObject()["d"].getArray()[1].getInt() == 4);
      REQUIRE(value.getObject()["d"].getArray()[2].getInt() == 5);
      REQUIRE(arr2[1].getInt() == 2);
      REQUIRE(arr2[2].getInt() == 6);

      json::Object obj = returnObject();
      REQUIRE(obj["a"].getInt() == 5);
      obj["a"] = 15;
      REQUIRE(obj["a"].getInt() == 15);
   }

   SECTION("readObject tests")
   {
      json::Object obj;
      json::Object obj2;
      obj["a"] = 1;
      obj["b"] = false;
      obj["c"] = "Hello there";
      obj2["a"] = "Inner obj";
      obj["d"] = obj2;

      int a;
      bool b;
      std::string c;
      json::Object d;
      Error error = json::readObject(obj,
                                     "a", &a,
                                     "b", &b,
                                     "c", &c,
                                     "d", &d);

      REQUIRE_FALSE(error);
      REQUIRE(a == 1);
      REQUIRE_FALSE(b);
      REQUIRE(c == "Hello there");
      REQUIRE(d["a"].getString() == "Inner obj");

      error = json::readObject(obj,
                               "a", &c,
                               "b", &b,
                               "c", &c);
      REQUIRE(error);

      error = json::readObject(obj,
                               "a", &a,
                               "b", &a,
                               "c", &c);
      REQUIRE(error);

      error = json::readObject(obj,
                               "a", &a,
                               "b", &b,
                               "c", &a);
      REQUIRE(error);
   }

   SECTION("readParams tests")
   {
      json::Array array;
      array.push_back(json::Value(1));
      array.push_back(json::Value(false));
      array.push_back(json::Value("Hello there"));

      int a;
      bool b;
      std::string c;
      Error error = json::readParams(array, &a, &b, &c);
      REQUIRE_FALSE(error);
      REQUIRE(a == 1);
      REQUIRE_FALSE(b);
      REQUIRE(c == "Hello there");

      error = json::readParams(array, &c, &b, &c);
      REQUIRE(error);

      error = json::readParams(array, &a, &a, &c);
      REQUIRE(error);

      error = json::readParams(array, &a, &b, &a);
      REQUIRE(error);

      a = 5;
      b = true;
      error = json::readParams(array, &a, &b);
      REQUIRE_FALSE(error);
      REQUIRE(a == 1);
      REQUIRE_FALSE(b);
   }

   SECTION("readObjectParam tests")
   {
      json::Array array;
      json::Object obj;
      obj["a"] = 1;
      obj["b"] = true;
      obj["c"] = "Hello there";

      array.push_back(json::Value(obj));
      array.push_back(json::Value(1));
      array.push_back(json::Value(false));
      array.push_back(json::Value(obj));

      int a;
      bool b;
      std::string c;
      Error error = json::readObjectParam(array, 0,
                                          "a", &a,
                                          "b", &b,
                                          "c", &c);
      REQUIRE_FALSE(error);
      REQUIRE(a == 1);
      REQUIRE(b);
      REQUIRE(c == "Hello there");

      error = json::readObjectParam(array, 0,
                                    "a", &b,
                                    "b", &b,
                                    "c", &c);
      REQUIRE(error);

      error = json::readObjectParam(array, 1,
                                    "a", &a,
                                    "b", &b,
                                    "c", &c);
      REQUIRE(error);

      error = json::readObjectParam(array, 3,
                                    "a", &a,
                                    "b", &b,
                                    "c", &c);
      REQUIRE_FALSE(error);
   }

   SECTION("Can serialize / deserialize complex json object with helpers")
   {
      json::Object object;
      object["a"] = true;
      object["b"] = false;
      object["c"] = 1000;
      object["d"] = (uint64_t)18446744073709550615U;
      object["e"] = 246.9;
      object["f"] = std::string("Hello world");

      json::Array simpleArray;
      simpleArray.push_back(json::Value(100));
      simpleArray.push_back(json::Value(200));
      simpleArray.push_back(json::Value(300));
      object["g"] = simpleArray;

      json::Array objectArray;

      json::Object obj1;
      obj1["a1"] = "a1";
      obj1["a2"] = 1;

      json::Object obj2;
      obj2["b1"] = "b1";
      obj2["b2"] = 2;

      objectArray.push_back(json::Value(obj1));
      objectArray.push_back(json::Value(obj2));

      object["h"] = objectArray;

      json::Object obj3;
      obj3["nestedValue"] = 9876.324;
      json::Object obj4;
      obj4["a"] = "Inner object a";
      json::Array innerArray;
      innerArray.push_back(json::Value(1));
      innerArray.push_back(json::Value(5));
      innerArray.push_back(json::Value(6));
      obj4["b"] = innerArray;
      obj4["c"] = 3;
      obj3["inner"] = obj4;
      object["i"] = obj3;

      std::string json = object.write();

      json::Value value;
      REQUIRE(!value.parse(json));
      REQUIRE(value.getType() == json::Type::OBJECT);

      json::Object deserializedObject = value.getObject();

      bool a, b;
      int c;
      uint64_t d;
      double e;
      std::string f;
      json::Array g, h;
      json::Object i;

      Error error = json::readObject(deserializedObject,
                                     "a", &a,
                                     "b", &b,
                                     "c", &c,
                                     "d", &d,
                                     "e", &e,
                                     "f", &f,
                                     "g", &g,
                                     "h", &h,
                                     "i", &i);
      REQUIRE_FALSE(error);
      REQUIRE(a);
      REQUIRE_FALSE(b);
      REQUIRE(c == 1000);
      REQUIRE(d == 18446744073709550615U);
      REQUIRE(e == Approx(246.9));
      REQUIRE(f == "Hello world");

      REQUIRE(g[0].getInt() == 100);
      REQUIRE(g[1].getInt() == 200);
      REQUIRE(g[2].getInt() == 300);

      int g1, g2, g3;
      error = json::readParams(g, &g1, &g2, &g3);
      REQUIRE_FALSE(error);
      REQUIRE(g1 == 100);
      REQUIRE(g2 == 200);
      REQUIRE(g3 == 300);

      json::Object h1, h2;
      error = json::readParams(h, &h1, &h2);
      REQUIRE_FALSE(error);

      std::string a1;
      int a2;
      error = json::readObject(h1,
                               "a1", &a1,
                               "a2", &a2);
      REQUIRE_FALSE(error);
      REQUIRE(a1 == "a1");
      REQUIRE(a2 == 1);

      std::string b1;
      int b2;
      error = json::readObject(h2,
                               "b1", &b1,
                               "b2", &b2);
      REQUIRE_FALSE(error);
      REQUIRE(b1 == "b1");
      REQUIRE(b2 == 2);

      double nestedValue;
      json::Object innerObj;

      error = json::readObject(i,
                               "nestedValue", &nestedValue,
                               "inner", &innerObj);
      REQUIRE_FALSE(error);
      REQUIRE(nestedValue == Approx(9876.324));

      std::string innerA;
      json::Array innerB;
      int innerC;

      error = json::readObject(innerObj,
                               "a", &innerA,
                               "b", &innerB,
                               "c", &innerC);

      REQUIRE_FALSE(error);
      REQUIRE(innerA == "Inner object a");
      REQUIRE(innerB.getSize() == 3);
      REQUIRE(innerB[0].getInt() == 1);
      REQUIRE(innerB[1].getInt() == 5);
      REQUIRE(innerB[2].getInt() == 6);
      REQUIRE(innerC == 3);
   }

   SECTION("Can modify object members via iterator")
   {
      json::Object obj = createObject();
      auto iter = obj.find("c");
      REQUIRE((*iter).getValue().getInt() == 1000);

      (*iter).getValue() = 25;
      REQUIRE((*iter).getValue().getInt() == 25);
      REQUIRE(obj["c"].getInt() == 25);
   }

   SECTION("Can add new member")
   {
      std::string jsonStr = "{\"a\": {}}";
      json::Value val;

      REQUIRE(!val.parse(jsonStr));
      REQUIRE(val.getType() == json::Type::OBJECT);

      json::Object object = val.getObject();
      object.insert("a", json::Value(1));

      REQUIRE(object["a"].getInt() == 1);
   }

   SECTION("Complex member fetch and set test")
   {
      json::Value value = getGlobalValue("a", "1");
      REQUIRE(value.getInt() == 1);

      insertGlobalValue("a", "testVal", json::Value(55));
      REQUIRE(getGlobalValue("a", "testVal").getInt() == 55);
   }

   SECTION("Can set rpc response value from complex object")
   {
      json::Object object = createObject();
      json::JsonRpcResponse jsonRpcResponse;
      jsonRpcResponse.setResult(object);
   }

   SECTION("Multiple assign")
   {
      json::Object object = createObject();
      json::Value val = object;
      json::Value val2 = val;

      json::Object root;
      root["a"] = val;
      root["b"] = val2;
   }

   SECTION("Can convert to value properly")
   {
      json::Object root;
      json::Value val = createValue();
      root["a"] = val;

      json::JsonRpcResponse jsonRpcResponse;
      jsonRpcResponse.setResult(root);
   }

   SECTION("Can std erase an array meeting certain criteria")
   {
      json::Array arr;
      for (int i = 0; i < 10; ++i)
      {
         arr.push_back(json::Value(i));
      }

      arr.erase(std::remove_if(arr.begin(),
                               arr.end(),
                               [=](const json::Value& val) { return val.getInt() % 2 == 0; }),
                arr.end());


      REQUIRE(arr.getSize() == 5);
      REQUIRE(arr[0].getInt() == 1);
      REQUIRE(arr[1].getInt() == 3);
      REQUIRE(arr[2].getInt() == 5);
      REQUIRE(arr[3].getInt() == 7);
      REQUIRE(arr[4].getInt() == 9);
   }

   SECTION("Can std erase an array meeting no criteria")
   {
      json::Array arr;
      for (int i = 0; i < 10; ++i)
      {
         arr.push_back(json::Value(i));
      }

      arr.erase(std::remove_if(arr.begin(),
                               arr.end(),
                               [=](const json::Value& val) { return val.getInt() > 32; }),
                arr.end());


      REQUIRE(arr.getSize() == 10);
   }

   SECTION("Can erase an empty array")
   {
      json::Array arr;

      arr.erase(std::remove_if(arr.begin(),
                               arr.end(),
                               [=](const json::Value& val) { return val.getInt() % 2 == 0; }),
                arr.end());


      REQUIRE(arr.getSize() == 0);
   }

   SECTION("Test self assignment")
   {
      json::Value val = createValue();
      val = val;

      REQUIRE(val.getObject()["a"].getBool());
   }

   SECTION("Unicode string test")
   {
      std::string jsonStr = "{\"a\": \"的中文翻譯 | 英漢字典\"}";
      json::Value val;
      REQUIRE(!val.parse(jsonStr));

      REQUIRE(val.getObject()["a"].getString() == "的中文翻譯 | 英漢字典");
   }

   SECTION("Can get value from function")
   {
      json::Value val = getValue();
      REQUIRE(val.getInt() == 5);
   }

   SECTION("Parse errors")
   {
      std::string invalid = R"({ key: value )";
      json::Value val;
      Error err = val.parse(invalid);
      REQUIRE(err);
   }

   SECTION("Schema default parse") {
      std::string schema = R"(
      {
         "$id": "https://rstudio.com/rstudio.preferences.json",
         "$schema": "http://json-schema.org/draft-07/schema#",
         "title": "Defaults Test Example Schema",
         "type": "object",
         "properties": {
             "first": {
                 "type": "int",
                 "default": 5,
                 "description": "A number. How about 5?"
             },
             "second": {
                 "type": "object",
                 "description": "An object which contains defaults.",
                 "properties": {
                     "foo": {
                        "type": "int",
                        "default": 10,
                        "description": "Another number. How about 10?"
                     }
                 }
             }
           }
        })";

      json::Object defaults;
      Error err = json::Object::getSchemaDefaults(schema, defaults);
      INFO(err.asString());
      REQUIRE(!err);
      
      REQUIRE(defaults["first"].getInt() == 5);
      json::Object second = defaults["second"].getObject();
      REQUIRE(second["foo"].getInt() == 10);
   }

   SECTION("Object merge") {
      json::Object base;
      json::Object overlay;

      // Property 1: has an overlay
      base["p1"] = "base";
      overlay["p1"] = "overlay";

      // Property 2: no overlay
      base["p2"] = "base";

      // Property 3: an object with non-overlapping properties
      json::Object p3base, p3overlay;
      p3base["p3-a"] = "base";
      p3overlay["p3-b"] = "overlay";
      base["p3"] = p3base;
      overlay["p3"] = p3overlay;
      
      // Regular properties should pick up values from the overlay (ensure they are copied, not
      // moved)
      auto result = json::Object::mergeObjects(base, overlay);
      REQUIRE(result["p1"].getString() == "overlay");
      REQUIRE(overlay["p1"].getString() == "overlay");

      // Properties with no overlay should pick up values from the base (ensure they are copied, not
      // moved)
      REQUIRE(result["p2"].getString() == "base");
      REQUIRE(base["p2"].getString() == "base");

      // Sub-objects with interleaved properties should inherit the union of properties
      auto p3result = result["p3"].getObject();
      REQUIRE(p3result["p3-a"].getString() == "base");
      REQUIRE(p3result["p3-b"].getString() == "overlay");
   }

   SECTION("Schema validation")
   {
      std::string schema = R"(
      {
         "$id": "https://rstudio.com/rstudio.preferences.json",
         "$schema": "http://json-schema.org/draft-07/schema#",
         "title": "Unit Test Example Schema",
         "type": "object",
         "properties": {
             "first": {
                 "type": "boolean",
                 "default": false,
                 "description": "The first example property"
             },
             "second": {
                 "type": "string",
                 "enum": ["a", "b", "c"],
                 "default": "b",
                 "description": "The second example property"
             }
           }
        })";
         
      // do valid documents pass validation?
      std::string valid = R"(
         { "first": true, "second": "a" }
      )";

      json::Value val;
      Error err = val.parseAndValidate(valid, schema);
      REQUIRE(!err);
      REQUIRE(val.getObject()["first"].getBool());

      // do invalid documents fail?
      std::string invalid = R"(
         { "first": "a", "second": "d", "third": 3 }
      )";
      err = val.parseAndValidate(invalid, schema);
      REQUIRE(err);

      // finally, test the defaults:
      std::string partial = R"(
         { "first": true }
      )";
      // ... parse according to the schema
      err = val.parseAndValidate(partial, schema);
      REQUIRE(!err);

      // ... extract defaults from the schema (RapidJSON doesn't do defaults)
      json::Object defaults;
      err = json::Object::getSchemaDefaults(schema, defaults);
      REQUIRE(!err);

      // ... overlay the document on the defaults
      json::Object result = json::Object::mergeObjects(defaults.getObject(), val.getObject());

      // ... see if we got what we expected.
      REQUIRE(result["first"].getBool() == true);   // non-default value
      REQUIRE(result["second"].getString() == "b");   // default value

      // now let's try coercing the invalid document
      json::Value corrected;
      err = corrected.parse(invalid);
      REQUIRE(!err);
      std::vector<std::string> violations;
      err = corrected.coerce(schema, violations);
      REQUIRE(!err);

      // make sure that we got a violation
      REQUIRE(violations.size() > 0);

      // make sure the coerced document is valid according to the schema
      err = val.validate(schema);
      REQUIRE(!err);

      // make sure that the two invalid nodes were removed, leaving the valid one
      json::Object obj = corrected.getObject();
      REQUIRE(obj.find("first") == obj.end());
      REQUIRE(obj.find("second") == obj.end());
      REQUIRE(obj.find("third") != obj.end());
   }

   SECTION("Can iterate object")
   {
      json::Object obj;
      obj["first"] = 1;
      obj["second"] = 2;
      obj["third"] = 3;

      int i = 0;
      for (auto itr = obj.begin(); itr != obj.end(); ++itr, ++i)
      {
         if (i == 0)
         {
            REQUIRE((*itr).getName() == "first");
            REQUIRE((*itr).getValue().getInt() == 1);
         }
         else if (i == 1)
         {
            REQUIRE((*itr).getName() == "second");
            REQUIRE((*itr).getValue().getInt() == 2);
         }
         else
         {
            REQUIRE((*itr).getName() == "third");
            REQUIRE((*itr).getValue().getInt() == 3);
         }
      }

      // Check that we iterated the correct number of times.
      REQUIRE(i == 3);
   }

   SECTION("Can compare object iterators")
   {
      json::Object obj1, obj2;
      obj1["first"] = 1;
      obj1["second"] = 2;
      obj1["third"] = 3;

      obj2["first"] = 1;
      obj2["second"] = 2;
      obj2["third"] = 3;

      // Comparing iterators pointing to the same object.
      auto itr1 = obj1.begin(), itr2 = obj1.begin();
      REQUIRE(itr1 == itr2);           // Both at the start.
      REQUIRE(++itr1 == ++itr2);       // Both at the second element.
      REQUIRE(itr1 != obj1.begin());   // An itr at the second element is not the same as an itr at the start.
      REQUIRE(++itr1 != itr2);         // itr1 at the 3rd element, itr2 at the 2nd element.
      REQUIRE(itr1 != obj1.end());     // An itr isn't at the end until it's after the last element.
      REQUIRE(++itr1 == obj1.end());   // Both at the end.

      // Comparing iterators pointing to different objects. They should never be equal.
      itr1 = obj1.begin(), itr2 = obj2.begin();
      REQUIRE(itr1 != itr2);
      REQUIRE(++itr1 != ++itr2);
      REQUIRE(obj1.end() != obj2.end());
   }

   SECTION("Can compare array iterators")
   {
      json::Array arr1, arr2;
      arr1.push_back(json::Value("first"));
      arr1.push_back(json::Value("second"));
      arr1.push_back(json::Value("third"));

      arr2.push_back(json::Value("first"));
      arr2.push_back(json::Value("second"));
      arr2.push_back(json::Value("third"));

      // Comparing iterators pointing to the same object.
      auto itr1 = arr1.begin(), itr2 = arr1.begin();
      REQUIRE(itr1 == itr2);           // Both at the start.
      REQUIRE(++itr1 == ++itr2);       // Both at the second element.
      REQUIRE(itr1 != arr1.begin());   // An itr at the second element is not the same as an itr at the start.
      REQUIRE(++itr1 != itr2);         // itr1 at the 3rd element, itr2 at the 2nd element.
      REQUIRE(itr1 != arr1.end());     // An itr isn't at the end until it's after the last element.
      REQUIRE(++itr1 == arr1.end());   // Both at the end.

      // Comparing iterators pointing to different objects. They should never be equal.
      itr1 = arr1.begin(), itr2 = arr2.begin();
      REQUIRE(itr1 != itr2);
      REQUIRE(++itr1 != ++itr2);
      REQUIRE(arr1.end() != arr2.end());
   }
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
