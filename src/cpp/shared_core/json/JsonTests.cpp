/*
 * JsonTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <tests/TestThat.hpp>

#include <iostream>
#include <set>

#include <boost/optional/optional_io.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

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
      array.push_back(1);
      array.push_back(2);
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

      array.push_back(obj1);
      array.push_back(obj2);

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

      std::transform(
         arr2.begin(),
         arr2.end(),
         std::back_inserter(arr),
         [=](json::Value val)
         { return json::Value(val.getInt() * 2); });

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
      for (const json::Value& arrVal : valArray)
         sum += arrVal.getInt();

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

   SECTION("Multiple assign")
   {
      json::Object object = createObject();
      json::Value val = object;
      json::Value val2 = val;

      json::Object root;
      root["a"] = val;
      root["b"] = val2;
   }

   SECTION("Can std erase an array meeting certain criteria")
   {
      json::Array arr;
      for (int i = 0; i < 10; ++i)
      {
         arr.push_back(json::Value(i));
      }

      arr.erase(
         std::remove_if(
            arr.begin(),
            arr.end(),
            [=](const json::Value& val)
            { return val.getInt() % 2 == 0; }),
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

      arr.erase(
         std::remove_if(
            arr.begin(),
            arr.end(),
            [=](const json::Value& val)
            { return val.getInt() > 32; }),
         arr.end());

      REQUIRE(arr.getSize() == 10);
   }

   SECTION("Can erase an empty array")
   {
      json::Array arr;

      arr.erase(
         std::remove_if(
            arr.begin(),
            arr.end(),
            [=](const json::Value& val)
            { return val.getInt() % 2 == 0; }),
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

   SECTION("Schema default parse")
   {
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

   SECTION("Object merge")
   {
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
         else
            if (i == 1)
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

   SECTION("Can set pointer value")
   {
      json::Object obj;
      obj["a"] = 1;
      obj["b"] = 2;

      json::Array cArray;
      cArray.push_back(json::Value("1"));
      cArray.push_back(json::Value("2"));
      cArray.push_back(json::Value("3"));

      obj["c"] = cArray;

      REQUIRE_FALSE(obj.setValueAtPointerPath("/d", json::Value(4)));

      json::Object eObj;
      eObj["param1"] = "param1";
      eObj["param2"] = "param2";

      json::Object param3Obj;
      param3Obj["nested"] = "nestedValue";
      eObj["param3"] = param3Obj;

      REQUIRE_FALSE(obj.setValueAtPointerPath("/e", eObj));
      REQUIRE_FALSE(obj.setValueAtPointerPath("/c/3", json::Value("4")));
      REQUIRE_FALSE(obj.setValueAtPointerPath("/e/param3/nested2", json::Value("nestedValue2")));

      REQUIRE(obj["c"].getArray().getSize() == 4);
      REQUIRE(obj["c"].getArray()[3].getString() == "4");
      REQUIRE(obj["d"].getInt() == 4);
      REQUIRE(obj["e"].getObject()["param3"].getObject()["nested"].getString() == "nestedValue");
      REQUIRE(obj["e"].getObject()["param3"].getObject()["nested2"].getString() == "nestedValue2");
   }

   SECTION("Invalid pointer path returns error on set")
   {
      json::Object obj;
      REQUIRE(obj.setValueAtPointerPath("path must begin with a /", json::Value(1)));
   }

   SECTION("Object deep comparison")
   {
      json::Object obj1;
      obj1.insert("member1", json::Value(1));
      obj1.insert("member2", json::Value(2));

      json::Object obj2;
      obj2.insert("member1", json::Value(1));
      obj2.insert("member2", json::Value(2));

      CHECK(obj1 == obj2);
   }

   SECTION("Object deep comparsion - insertion order doesn't matter")
   {
      json::Object obj1;
      obj1.insert("member1", json::Value(1));
      obj1.insert("member2", json::Value(2));

      json::Object obj2;
      obj2.insert("member2", json::Value(2));
      obj2.insert("member1", json::Value(1));

      CHECK(obj1 == obj2);
   }

   SECTION("Object deep comparison - not equal")
   {
      json::Object obj1;
      obj1.insert("member1", json::Value(1));
      obj1.insert("member2", json::Value(2));

      json::Object obj2;
      obj2.insert("member1", json::Value(1));
      obj2.insert("member2", json::Value(3));

      CHECK(obj1 != obj2);
   }

   SECTION("Array deep comparison")
   {
      json::Array arr1;
      arr1.push_back(json::Value("value1"));
      arr1.push_back(json::Value("value2"));
      arr1.push_back(json::Value(3.5));

      json::Array arr2;
      arr2.push_back(json::Value("value1"));
      arr2.push_back(json::Value("value2"));
      arr2.push_back(json::Value(3.5));

      CHECK(arr1 == arr2);
   }

   SECTION("Array deep comparison - different order")
   {
      json::Array arr1;
      arr1.push_back(json::Value("value1"));
      arr1.push_back(json::Value("value2"));
      arr1.push_back(json::Value(3.5));

      json::Array arr2;
      arr2.push_back(json::Value(3.5));
      arr2.push_back(json::Value("value1"));
      arr2.push_back(json::Value("value2"));

      CHECK(arr1 != arr2);
   }

   SECTION("Array deep comparison - different length")
   {
      json::Array arr1;
      arr1.push_back(json::Value("value1"));
      arr1.push_back(json::Value("value2"));
      arr1.push_back(json::Value(3.5));

      json::Array arr2;
      arr2.push_back(json::Value("value1"));
      arr2.push_back(json::Value("value2"));

      CHECK(arr1 != arr2);
   }

   SECTION("Array deep comparison - not equal")
   {
      json::Array arr1;
      arr1.push_back(json::Value("value1"));
      arr1.push_back(json::Value("value2"));
      arr1.push_back(json::Value(3.5));

      json::Array arr2;
      arr2.push_back(json::Value("value3"));
      arr2.push_back(json::Value("value2"));
      arr1.push_back(json::Value(5));

      CHECK(arr1 != arr2);
   }

   SECTION("Complex object deep comparison")
   {
      json::Array simpleArr;
      simpleArr.push_back(json::Value(1));
      simpleArr.push_back(json::Value(2));
      simpleArr.push_back(json::Value(3));
      simpleArr.push_back(json::Value(4));

      json::Object nestedObj;
      nestedObj.insert("simpleArr", simpleArr.clone());
      nestedObj.insert("boolVal", json::Value(true));
      nestedObj.insert("doubleVal", json::Value(2.13));

      json::Array complexArr;
      complexArr.push_back(nestedObj.clone());
      complexArr.push_back(json::Value(false));
      complexArr.push_back(json::Value(-5));

      json::Object obj1;
      obj1.insert("simpleArr", simpleArr.clone());
      obj1.insert("nestedObj", nestedObj.clone());
      obj1.insert("complexArr", complexArr.clone());
      obj1.insert("strValue", json::Value("hello"));
      obj1.insert("strValue2", json::Value("goodbye"));

      json::Object obj2;
      obj2.insert("simpleArr", simpleArr.clone());
      obj2.insert("nestedObj", nestedObj.clone());
      obj2.insert("complexArr", complexArr.clone());
      obj2.insert("strValue", json::Value("hello"));
      obj2.insert("strValue2", json::Value("goodbye"));

      CHECK(obj1 == obj2);
   }

   SECTION("Parse json object")
   {
      json::Array arr;
      arr.push_back(json::Value("a"));
      arr.push_back(json::Value("b"));
      arr.push_back(json::Value("c"));

      json::Object expected;
      expected.insert("1", json::Value(1));
      expected.insert("2", json::Value("hello"));
      expected.insert("3", expected.clone());
      expected.insert("4", json::Value(false));
      expected.insert("5", arr);

      json::Object actual;
      Error error = actual.parse(R"(
      {
          "1": 1,
          "2": "hello",
          "4": false,
          "3": {"1" : 1, "2": "hello"},
          "5": ["a", "b", "c"]
      })");

      REQUIRE_FALSE(error);
      CHECK(actual == expected);
   }

   SECTION("Parse json array into object")
   {
      json::Object result;
      Error error = result.parse(R"([ "a", "b", "c" ])");

      REQUIRE(error);
   }

   SECTION("Parse json array")
   {
      json::Array expected;
      expected.push_back(json::Value("a"));
      expected.push_back(json::Value("b"));
      expected.push_back(json::Value("c"));

      json::Array actual;
      Error error = actual.parse(R"([ "a", "b", "c" ])");

      REQUIRE_FALSE(error);
      CHECK(actual == expected);
   }

   SECTION("Parse object into json array")
   {
      json::Array result;
      Error error = result.parse(R"({ "first": 1, "second": true })");

      REQUIRE(error);
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
                                     "a", a,
                                     "b", b,
                                     "c", c,
                                     "d", d);

      REQUIRE_FALSE(error);
      REQUIRE(a == 1);
      REQUIRE_FALSE(b);
      REQUIRE(c == "Hello there");
      REQUIRE(d["a"].getString() == "Inner obj");

      error = json::readObject(obj,
                               "a", c,
                               "b", b,
                               "c", c);
      REQUIRE(error);

      error = json::readObject(obj,
                               "a", a,
                               "b", a,
                               "c", c);
      REQUIRE(error);

      error = json::readObject(obj,
                               "a", a,
                               "b", b,
                               "c", a);
      REQUIRE(error);
   }

   SECTION("readObject tests (lists and optionals)")
   {
      json::Array intArr;
      intArr.push_back(4);
      intArr.push_back(3);
      intArr.push_back(2);
      intArr.push_back(1);
      intArr.push_back(4);

      json::Array strArr;
      strArr.push_back("a string");
      strArr.push_back("A CAPITAL STRING");
      strArr.push_back("a duplicate string");
      strArr.push_back("a duplicate string");
      strArr.push_back("a duplicate string");
      strArr.push_back("A CAPITAL STRING");

      json::Object obj;
      obj["intArr"] = intArr;
      obj["strArr"] = strArr;

      std::vector<int> intList, badStrList;
      std::vector<std::string> strList, badIntList;
      std::set<int> intSet, badStrSet;
      std::set<std::string> strSet, badIntSet;

      boost::optional<std::vector<int> > optIntList, badOptStrList;
      boost::optional<std::vector<std::string> > optStrList, badOptIntList;
      boost::optional<std::set<int> > optIntSet, badOptStrSet;
      boost::optional<std::set<std::string> > optStrSet, badOptIntSet;

      // No errors.
      REQUIRE_FALSE(json::readObject(obj,
         "intArr", intList,
         "intArr", intSet,
         "intArr", optIntList,
         "intArr", optIntSet,
         "strArr", strList,
         "strArr", strSet,
         "strArr", optStrList,
         "strArr", optStrSet,
         "notFound", badOptIntList,
         "notFound", badOptStrSet));

      // Check good values
      REQUIRE(intList.size() == 5);
      CHECK(intList[0] == 4);
      CHECK(intList[1] == 3);
      CHECK(intList[2] == 2);
      CHECK(intList[3] == 1);
      CHECK(intList[4] == 4);
      REQUIRE(intSet.size() == 4);
      CHECK(intSet.find(1) != intSet.end());
      CHECK(intSet.find(2) != intSet.end());
      CHECK(intSet.find(3) != intSet.end());
      CHECK(intSet.find(4) != intSet.end());
      REQUIRE(!(optIntList == boost::none));
      CHECK(std::equal(intList.begin(), intList.end(), optIntList.get().begin()));
      REQUIRE(!(optIntSet == boost::none));
      CHECK(std::equal(intSet.begin(), intSet.end(), optIntSet.get().begin()));

      REQUIRE(strList.size() == 6);
      CHECK(strList[0] == "a string");
      CHECK(strList[1] == "A CAPITAL STRING");
      CHECK(strList[2] == "a duplicate string");
      CHECK(strList[3] == "a duplicate string");
      CHECK(strList[4] == "a duplicate string");
      CHECK(strList[5] == "A CAPITAL STRING");
      REQUIRE(strSet.size() == 3);
      CHECK(strSet.find("a string") != strSet.end());
      CHECK(strSet.find("A CAPITAL STRING") != strSet.end());
      CHECK(strSet.find("a duplicate string") != strSet.end());
      REQUIRE(!!optStrList);
      CHECK(std::equal(strList.begin(), strList.end(), optStrList.get().begin()));
      REQUIRE(!!optStrSet);
      CHECK(std::equal(strSet.begin(), strSet.end(), optStrSet.get().begin()));

      // Bad values, one at a time.
      CHECK((json::readObject(obj, "notFound", badStrList) && badStrList.empty()));
      CHECK((json::readObject(obj, "notFound", badStrSet) && badStrSet.empty()));
      CHECK((json::readObject(obj, "strArr", badStrList) && badStrList.empty()));
      CHECK((json::readObject(obj, "strArr", badOptStrList) && !!(badOptStrList == boost::none)));
      CHECK((json::readObject(obj, "strArr", badStrSet) && badStrSet.empty()));
      CHECK((json::readObject(obj, "strArr", badOptStrSet) && !!(badOptStrSet == boost::none)));
      CHECK((json::readObject(obj, "notFound", badIntList) && badIntList.empty()));
      CHECK((json::readObject(obj, "notFound", badIntSet) && badIntSet.empty()));
      CHECK((json::readObject(obj, "intArr", badIntList) && badIntList.empty()));
      CHECK((json::readObject(obj, "intArr", badOptIntList) && !!(badOptIntList == boost::none)));
      CHECK((json::readObject(obj, "intArr", badIntSet) && badIntSet.empty()));
      CHECK((json::readObject(obj, "intArr", badOptIntSet) && !!(badOptIntSet == boost::none)));
   }
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
