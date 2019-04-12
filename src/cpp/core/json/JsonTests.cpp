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
   simpleArray.push_back(100);
   simpleArray.push_back(200);
   simpleArray.push_back(300);
   object["g"] = simpleArray;

   json::Array objectArray;

   json::Object obj1;
   obj1["a1"] = "a1";
   obj1["a2"] = 1;

   json::Object obj2;
   obj2["b1"] = "b1";
   obj2["b2"] = 2;

   objectArray.push_back(obj1);
   objectArray.push_back(obj2);

   object["h"] = objectArray;

   json::Object obj3;
   obj3["nestedValue"] = 9876.324;
   json::Object obj4;
   obj4["a"] = "Inner object a";
   json::Array innerArray;
   innerArray.push_back(1);
   innerArray.push_back(5);
   innerArray.push_back(6);
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
   REQUIRE(json::parse(jsonStr, &val));

   return val.get_value<json::Object>();
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
   REQUIRE(json::parse(jsonStr, &val));

   json::Object obj = val.get_obj();

   // must return a copy instead of actual reference
   return obj["a"].clone();
}

json::Object s_object;

json::Value getGlobalValue(std::string scope,
                           std::string name)
{
   json::Object::iterator i = s_object.find(scope);
   if (i == s_object.end())
   {
      return json::Value();
   }
   else
   {
      if (!json::isType<core::json::Object>((*i).value()))
         return json::Value();
      json::Object scopeObject = (*i).value().get_obj();
      return scopeObject[name].clone();
   }
}

void insertGlobalValue(const std::string& scope,
                       const json::Member& entry)
{
   json::Object::iterator pos = s_object.find(scope);
   if (pos == s_object.end())
   {
      json::Object newScopeObject;
      s_object.insert(json::Member(scope, newScopeObject));
   }

   const json::Value& scopeValue = s_object[scope];
   json::Object scopeObject = scopeValue.get_obj();

   // insert the value into the scope
   scopeObject.insert(entry);
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
      REQUIRE(json::parse(json, &value));

      REQUIRE(value.type() == json::ObjectType);
      json::Object obj = value.get_obj();

      REQUIRE(obj["a"].type() == json::IntegerType);
      REQUIRE(obj["b"].type() == json::NullType);

      std::string bVal;
      REQUIRE_FALSE(json::getOptionalParam(obj, "b", std::string("DEFAULT"), &bVal));
      REQUIRE(bVal == "DEFAULT");

      REQUIRE(json::typeAsString(obj["b"]) == "<null>");
   }

   SECTION("Can construct simple json object")
   {
      json::Object obj;

      obj["a"] = "Hello";
      REQUIRE(obj["a"].get_str() == "Hello");

      obj["b"] = "world";
      REQUIRE(obj["b"].get_str() == "world");

      obj["c"] = 25;
      REQUIRE(obj["c"].get_int() == 25);

      json::Array array;
      array.push_back(1);
      array.push_back(2);
      array.push_back(3);

      obj["d"] = array;

      int expectedNum = 1;
      for (const json::Value& val : obj["d"].get_array())
      {
         int num = val.get_int();
         REQUIRE(num == expectedNum);
         expectedNum++;
      }

      REQUIRE(obj["d"].get_array()[0].get_int() == 1);
      REQUIRE(obj["d"].get_array()[1].get_int() == 2);
      REQUIRE(obj["d"].get_array()[2].get_int() == 3);

      json::Object innerObj;
      innerObj["a"] = "Inner hello";
      obj["e"] = innerObj;

      REQUIRE(obj["e"].get_obj()["a"].get_str() == "Inner hello");

      std::string serialized = json::write(obj);
      std::string expected = "{\"a\":\"Hello\",\"b\":\"world\",\"c\":25,\"d\":[1,2,3],\"e\":{\"a\":\"Inner hello\"}}";
      REQUIRE(serialized == expected);
   }

   SECTION("Can deserialize simple json object")
   {
      std::string json = "{\"a\":\"Hello\",\"b\":\"world\",\"c\":25,\"c2\":25.5,\"d\":[1,2,3],\"e\":{\"a\":\"Inner hello\"}}";

      json::Value value;
      REQUIRE(json::parse(json, &value));

      REQUIRE(value.type() == json::ObjectType);
      json::Object obj = value.get_obj();

      REQUIRE(obj["a"].type() == json::StringType);
      REQUIRE(obj["a"].get_str() == "Hello");

      REQUIRE(obj["b"].type() == json::StringType);
      REQUIRE(obj["b"].get_str() == "world");

      REQUIRE(obj["c"].type() == json::IntegerType);
      REQUIRE(obj["c"].get_int() == 25);

      REQUIRE(obj["c2"].type() == json::RealType);
      REQUIRE(obj["c2"].get_real() == Approx(25.5));

      REQUIRE(obj["d"].type() == json::ArrayType);
      json::Array array = obj["d"].get_array();
      REQUIRE(array[0].get_int() == 1);
      REQUIRE(array[1].get_int() == 2);
      REQUIRE(array[2].get_int() == 3);

      REQUIRE(obj["e"].type() == json::ObjectType);
      json::Object innerObj = obj["e"].get_obj();
      REQUIRE(innerObj["a"].type() == json::StringType);
      REQUIRE(innerObj["a"].get_str() == "Inner hello");
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

      array.push_back(obj1);
      array.push_back(obj2);

      REQUIRE(array[0].get_obj()["1"].get_str() == "obj1");
      REQUIRE(array[0].get_obj()["2"].get_int() == 1);
      REQUIRE(array[1].get_obj()["1"].get_str() == "obj2");
      REQUIRE(array[1].get_obj()["2"].get_int() == 2);
   }

   SECTION("Can iterate arrays")
   {
      json::Array arr;
      arr.push_back(1);
      arr.push_back(2);
      arr.push_back(3);

      json::Array arr2;
      arr2.push_back(4);
      arr2.push_back(5);
      arr2.push_back(6);

      std::transform(arr2.begin(),
                     arr2.end(),
                     std::back_inserter(arr),
                     [=](json::Value val) { return json::Value(val.get_int() * 2); });

      json::Array::iterator iter = arr.begin();
      REQUIRE(*iter++ == 1);
      REQUIRE(*iter++ == 2);
      REQUIRE(*iter++ == 3);
      REQUIRE(*iter++ == 8);
      REQUIRE(*iter++ == 10);
      REQUIRE(*iter++ == 12);
      REQUIRE(iter == arr.end());

      json::Array::reverse_iterator riter = arr.rbegin();
      REQUIRE(*riter++ == 12);
      REQUIRE(*riter++ == 10);
      REQUIRE(*riter++ == 8);
      REQUIRE(*riter++ == 3);
      REQUIRE(*riter++ == 2);
      REQUIRE(*riter++ == 1);
      REQUIRE(riter == arr.rend());

      std::string jsonStr = "[1, 2, 3, 4, 5]";
      json::Value val;
      json::parse(jsonStr, &val);
      const json::Array& valArray = val.get_array();

      int sum = 0;
      for (const json::Value& val : valArray)
         sum += val.get_int();

      REQUIRE(sum == 15);
   }

   SECTION("Ref/copy semantics")
   {
      std::string json = R"({"a":"Hello","b":"world","c":25,"c2":25.5,"d":[1,2,3],"e":{"a":"Inner hello"}})";

      json::Value value;
      REQUIRE(json::parse(json, &value));

      json::Object obj1 = value.get_obj();
      json::Object obj2 = value.get_value<json::Object>();

      obj1["a"] = "Modified Hello";
      obj2["b"] = "modified world";

      // should be a reference here
      json::Array arr1 = value.get_obj()["d"].get_array();

      // should be a copy here
      json::Array arr2 = value.get_obj()["d"].get_value<json::Array>();

      // should be a reference
      json::Array arr3 = value.get_obj()["d"].get_array();

      // another copy
      json::Array arr4 = arr3;

      arr1[1] = 4;
      arr2[2] = 6;
      arr3[2] = 5;
      arr4[2] = 6;

      REQUIRE(value.get_obj()["a"].get_str() == "Modified Hello");
      REQUIRE(obj2["a"].get_str() == "Hello");
      REQUIRE(value.get_obj()["b"].get_str() == "world");
      REQUIRE(obj2["b"].get_str() == "modified world");

      REQUIRE(value.get_obj()["d"].get_array()[1] == 4);
      REQUIRE(value.get_obj()["d"].get_array()[2] == 5);
      REQUIRE(arr2[1] == 2);
      REQUIRE(arr2[2] == 6);

      json::Object obj = returnObject();
      REQUIRE(obj["a"].get_int() == 5);
      obj["a"] = 15;
      REQUIRE(obj["a"].get_int() == 15);
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
      REQUIRE(d["a"].get_str() == "Inner obj");

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
      array.push_back(1);
      array.push_back(false);
      array.push_back("Hello there");

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

      array.push_back(obj);
      array.push_back(1);
      array.push_back(false);
      array.push_back(obj);

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
      simpleArray.push_back(100);
      simpleArray.push_back(200);
      simpleArray.push_back(300);
      object["g"] = simpleArray;

      json::Array objectArray;

      json::Object obj1;
      obj1["a1"] = "a1";
      obj1["a2"] = 1;

      json::Object obj2;
      obj2["b1"] = "b1";
      obj2["b2"] = 2;

      objectArray.push_back(obj1);
      objectArray.push_back(obj2);

      object["h"] = objectArray;

      json::Object obj3;
      obj3["nestedValue"] = 9876.324;
      json::Object obj4;
      obj4["a"] = "Inner object a";
      json::Array innerArray;
      innerArray.push_back(1);
      innerArray.push_back(5);
      innerArray.push_back(6);
      obj4["b"] = innerArray;
      obj4["c"] = 3;
      obj3["inner"] = obj4;
      object["i"] = obj3;

      std::string json = json::write(object);

      json::Value value;
      REQUIRE(json::parse(json, &value));
      REQUIRE(value.type() == json::ObjectType);

      json::Object deserializedObject = value.get_obj();

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

      REQUIRE(g[0].get_int() == 100);
      REQUIRE(g[1].get_int() == 200);
      REQUIRE(g[2].get_int() == 300);

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
      REQUIRE(innerB.size() == 3);
      REQUIRE(innerB[0].get_int() == 1);
      REQUIRE(innerB[1].get_int() == 5);
      REQUIRE(innerB[2].get_int() == 6);
      REQUIRE(innerC == 3);
   }

   SECTION("Can modify object members via interator")
   {
      json::Object obj = createObject();
      auto iter = obj.find("c");
      REQUIRE((*iter).value().get_int() == 1000);

      (*iter).value() = 25;
      REQUIRE((*iter).value().get_int() == 25);
      REQUIRE(obj["c"].get_int() == 25);
   }

   SECTION("Can add new member")
   {
      std::string jsonStr = "{\"a\": {}}";
      json::Value val;

      REQUIRE(json::parse(jsonStr, &val));
      REQUIRE(val.type() == json::ObjectType);

      json::Object object = val.get_obj();
      object.insert(json::Member("a", 1));

      REQUIRE(object["a"].get_int() == 1);
   }

   SECTION("Complex member fetch and set test")
   {
      json::Value value = getGlobalValue("a", "1");
      REQUIRE(value.get_int() == 1);

      insertGlobalValue("a", json::Member("testVal", 55));
      REQUIRE(getGlobalValue("a", "testVal").get_int() == 55);
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
         arr.push_back(i);
      }

      arr.erase(std::remove_if(arr.begin(),
                               arr.end(),
                               [=](const json::Value& val) { return val.get_int() % 2 == 0; }),
                arr.end());


      REQUIRE(arr.size() == 5);
      REQUIRE(arr[0].get_int() == 1);
      REQUIRE(arr[1].get_int() == 3);
      REQUIRE(arr[2].get_int() == 5);
      REQUIRE(arr[3].get_int() == 7);
      REQUIRE(arr[4].get_int() == 9);
   }

   SECTION("Can std erase an array meeting no criteria")
   {
      json::Array arr;
      for (int i = 0; i < 10; ++i)
      {
         arr.push_back(i);
      }

      arr.erase(std::remove_if(arr.begin(),
                               arr.end(),
                               [=](const json::Value& val) { return val.get_int() > 32; }),
                arr.end());


      REQUIRE(arr.size() == 10);
   }

   SECTION("Can erase an empty array")
   {
      json::Array arr;

      arr.erase(std::remove_if(arr.begin(),
                               arr.end(),
                               [=](const json::Value& val) { return val.get_int() % 2 == 0; }),
                arr.end());


      REQUIRE(arr.size() == 0);
   }

   SECTION("Test self assignment")
   {
      json::Value val = createValue();
      val = val;

      REQUIRE(val.get_obj()["a"].get_bool());
   }

   SECTION("Unicode string test")
   {
      std::string jsonStr = "{\"a\": \"的中文翻譯 | 英漢字典\"}";
      json::Value val;
      REQUIRE(json::parse(jsonStr, &val));

      REQUIRE(val.get_obj()["a"].get_str() == "的中文翻譯 | 英漢字典");
   }

   SECTION("Can get value from function")
   {
      json::Value val = getValue();
      REQUIRE(val.get_int() == 5);
   }

   SECTION("Parse errors")
   {
      std::string invalid = R"({ key: value )";
      json::Value val;
      Error err = json::parse(invalid, ERROR_LOCATION, &val);
      REQUIRE(err != Success());
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
      Error err = json::getSchemaDefaults(schema, &defaults);
      INFO(err.description());
      REQUIRE(err == Success());
      
      REQUIRE(defaults["first"] == 5);
      json::Object second = defaults["second"].get_obj();
      REQUIRE(second["foo"] == 10);
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
      auto result = json::merge(base, overlay);
      REQUIRE(result["p1"] == "overlay");
      REQUIRE(overlay["p1"] == "overlay");

      // Properties with no overlay should pick up values from the base (ensure they are copied, not
      // moved)
      REQUIRE(result["p2"] == "base");
      REQUIRE(base["p2"] == "base");

      // Sub-objects with interleaved properties should inherit the union of properties
      auto p3result = result["p3"].get_obj();
      REQUIRE(p3result["p3-a"] == "base");
      REQUIRE(p3result["p3-b"] == "overlay");
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
      Error err = json::parseAndValidate(valid, schema, ERROR_LOCATION, &val);
      REQUIRE(err == Success());
      REQUIRE(val.get_obj()["first"].get_bool());

      // do invalid documents fail?
      std::string invalid = R"(
         { "first": 1, "second": "d" }
      )";
      err = json::parseAndValidate(invalid, schema, ERROR_LOCATION, &val);
      REQUIRE(err != Success());

      // finally, test the defaults:
      std::string partial = R"(
         { "first": true }
      )";
      // ... parse according to the schema
      err = json::parseAndValidate(partial, schema, ERROR_LOCATION, &val);
      REQUIRE(err == Success());

      // ... extract defaults from the schema (RapidJSON doesn't do defaults)
      json::Value defaults;
      err = json::getSchemaDefaults(schema, &defaults);
      REQUIRE(err == Success());

      // ... overlay the document on the defaults
      json::Object result = json::merge(defaults.get_obj(), val.get_obj());

      // ... see if we got what we expected.
      REQUIRE(result["first"] == true);   // non-default value
      REQUIRE(result["second"] == "b");   // default value
   }
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
