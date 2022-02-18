/*
 * JsonRpcTests.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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
   object["d"] = (uint64_t) 18446744073709550615U;
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

json::Value createValue()
{
   json::Object obj = createObject();
   return std::move(obj);
}

json::Object s_object;

} // anonymous namespace


TEST_CASE("JsonRpc")
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
                                     "a", a,
                                     "b", b,
                                     "c", c,
                                     "d", d,
                                     "e", e,
                                     "f", f,
                                     "g", g,
                                     "h", h,
                                     "i", i);
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
                               "a1", a1,
                               "a2", a2);
      REQUIRE_FALSE(error);
      REQUIRE(a1 == "a1");
      REQUIRE(a2 == 1);

      std::string b1;
      int b2;
      error = json::readObject(h2,
                               "b1", b1,
                               "b2", b2);
      REQUIRE_FALSE(error);
      REQUIRE(b1 == "b1");
      REQUIRE(b2 == 2);

      double nestedValue;
      json::Object innerObj;

      error = json::readObject(i,
                               "nestedValue", nestedValue,
                               "inner", innerObj);
      REQUIRE_FALSE(error);
      REQUIRE(nestedValue == Approx(9876.324));

      std::string innerA;
      json::Array innerB;
      int innerC;

      error = json::readObject(innerObj,
                               "a", innerA,
                               "b", innerB,
                               "c", innerC);

      REQUIRE_FALSE(error);
      REQUIRE(innerA == "Inner object a");
      REQUIRE(innerB.getSize() == 3);
      REQUIRE(innerB[0].getInt() == 1);
      REQUIRE(innerB[1].getInt() == 5);
      REQUIRE(innerB[2].getInt() == 6);
      REQUIRE(innerC == 3);
   }

   SECTION("Can set rpc response value from complex object")
   {
      json::Object object = createObject();
      json::JsonRpcResponse jsonRpcResponse;
      jsonRpcResponse.setResult(object);
   }

   SECTION("Can convert to value properly")
   {
      json::Object root;
      json::Value val = createValue();
      root["a"] = val;

      json::JsonRpcResponse jsonRpcResponse;
      jsonRpcResponse.setResult(root);
   }
}

} // namespace tests
} // namespace core
} // namespace rstudio
