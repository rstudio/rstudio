/*
 * JsonRpcTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <gtest/gtest.h>

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


TEST(JsonRpcTest, Initialize)
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

TEST(JsonRpcTest, NullTest)
{
   std::string json = "{\"a\": 1, \"b\": null}";

   json::Value value;
   ASSERT_FALSE(value.parse(json));

   ASSERT_EQ(json::Type::OBJECT, value.getType());
   json::Object obj = value.getObject();

   ASSERT_EQ(json::Type::INTEGER, obj["a"].getType());
   ASSERT_EQ(json::Type::NULL_TYPE, obj["b"].getType());

   std::string bVal;
   ASSERT_FALSE(json::getOptionalParam(obj, "b", std::string("DEFAULT"), &bVal));
   ASSERT_EQ(std::string("DEFAULT"), bVal);

   ASSERT_EQ(std::string("<Null>"), json::typeAsString(obj["b"]));
}

TEST(JsonRpcTest, ReadParamsTests)
{
   json::Array array;
   array.push_back(1);
   array.push_back(false);
   array.push_back("Hello there");

   int a;
   bool b;
   std::string c;
   Error error = json::readParams(array, &a, &b, &c);
   ASSERT_FALSE(error);
   ASSERT_EQ(1, a);
   ASSERT_FALSE(b);
   ASSERT_EQ(std::string("Hello there"), c);

   error = json::readParams(array, &c, &b, &c);
   ASSERT_TRUE(error);

   error = json::readParams(array, &a, &a, &c);
   ASSERT_TRUE(error);

   error = json::readParams(array, &a, &b, &a);
   ASSERT_TRUE(error);

   a = 5;
   b = true;
   error = json::readParams(array, &a, &b);
   ASSERT_FALSE(error);
   ASSERT_EQ(1, a);
   ASSERT_FALSE(b);
}

TEST(JsonRpcTest, ReadObjectParamTests)
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
   ASSERT_FALSE(error);
   ASSERT_EQ(1, a);
   ASSERT_TRUE(b);
   ASSERT_EQ(std::string("Hello there"), c);

   error = json::readObjectParam(array, 0,
                                "a", &b,
                                "b", &b,
                                "c", &c);
   ASSERT_TRUE(error);

   error = json::readObjectParam(array, 1,
                                "a", &a,
                                "b", &b,
                                "c", &c);
   ASSERT_TRUE(error);

   error = json::readObjectParam(array, 3,
                                "a", &a,
                                "b", &b,
                                "c", &c);
   ASSERT_FALSE(error);
}

TEST(JsonRpcTest, CanSerializeDeserializeComplexJsonObjectWithHelpers)
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
   ASSERT_FALSE(value.parse(json));
   ASSERT_EQ(json::Type::OBJECT, value.getType());

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
   ASSERT_FALSE(error);
   ASSERT_TRUE(a);
   ASSERT_FALSE(b);
   ASSERT_EQ(1000, c);
   ASSERT_EQ(18446744073709550615U, d);
   ASSERT_DOUBLE_EQ(246.9, e);
   ASSERT_EQ(std::string("Hello world"), f);

   ASSERT_EQ(100, g[0].getInt());
   ASSERT_EQ(200, g[1].getInt());
   ASSERT_EQ(300, g[2].getInt());

   int g1, g2, g3;
   error = json::readParams(g, &g1, &g2, &g3);
   ASSERT_FALSE(error);
   ASSERT_EQ(100, g1);
   ASSERT_EQ(200, g2);
   ASSERT_EQ(300, g3);

   json::Object h1, h2;
   error = json::readParams(h, &h1, &h2);
   ASSERT_FALSE(error);

   std::string a1;
   int a2;
   error = json::readObject(h1,
                           "a1", a1,
                           "a2", a2);
   ASSERT_FALSE(error);
   ASSERT_EQ(std::string("a1"), a1);
   ASSERT_EQ(1, a2);

   std::string b1;
   int b2;
   error = json::readObject(h2,
                           "b1", b1,
                           "b2", b2);
   ASSERT_FALSE(error);
   ASSERT_EQ(std::string("b1"), b1);
   ASSERT_EQ(2, b2);

   double nestedValue;
   json::Object innerObj;

   error = json::readObject(i,
                           "nestedValue", nestedValue,
                           "inner", innerObj);
   ASSERT_FALSE(error);
   ASSERT_DOUBLE_EQ(9876.324, nestedValue);

   std::string innerA;
   json::Array innerB;
   int innerC;

   error = json::readObject(innerObj,
                           "a", innerA,
                           "b", innerB,
                           "c", innerC);

   ASSERT_FALSE(error);
   ASSERT_EQ(std::string("Inner object a"), innerA);
   ASSERT_EQ(3u, innerB.getSize());
   ASSERT_EQ(1, innerB[0].getInt());
   ASSERT_EQ(5, innerB[1].getInt());
   ASSERT_EQ(6, innerB[2].getInt());
   ASSERT_EQ(3, innerC);
}

TEST(JsonRpcTest, CanSetRpcResponseValueFromComplexObject)
{
   json::Object object = createObject();
   json::JsonRpcResponse jsonRpcResponse;
   jsonRpcResponse.setResult(object);
}

TEST(JsonRpcTest, CanConvertToValueProperly)
{
   json::Object root;
   json::Value val = createValue();
   root["a"] = val;

   json::JsonRpcResponse jsonRpcResponse;
   jsonRpcResponse.setResult(root);
}

} // namespace tests
} // namespace core
} // namespace rstudio
