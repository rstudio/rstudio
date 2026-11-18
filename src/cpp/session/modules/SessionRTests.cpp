/*
 * SessionRTests.cpp
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

#include <boost/scope_exit.hpp>

#include <clocale>
#include <functional>
#include <map>
#include <memory>
#include <set>
#include <string>
#include <thread>
#include <utility>
#include <vector>

#include <yaml-cpp/yaml.h>

#include <core/json/JsonRpc.hpp>
#include <core/system/Environment.hpp>

#include <r/RExec.hpp>
#include <r/RJsonRpc.hpp>
#include <r/RSexp.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RInterface.hpp>
#include <r/RUtil.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace tests {

TEST(SessionRTest, RFunctionErrorsDontSetResultToNull) {
   SEXP result = R_NilValue;
   r::sexp::Protect protect;
   
   Error error = r::exec::RFunction("NoSuchFunction")
         .call(&result, &protect);
   
   EXPECT_NE(Success(), error);
   EXPECT_NE(nullptr, result);
   EXPECT_EQ(R_NilValue, result);
}

// matcher for the guard failure produced by off-main-thread RFunction use;
// checking the message distinguishes it from unrelated evaluation errors,
// which share the CodeExecutionError code
static void expectNonMainThreadError(const Error& error)
{
   EXPECT_TRUE(error == r::errc::CodeExecutionError);
   EXPECT_TRUE(error.getProperty("errormsg").find("non-main thread") != std::string::npos);
}

TEST(SessionRTest, RFunctionRefusesNonMainThread) {
   // the R runtime is single-threaded; RFunction must refuse to touch it
   // from other threads rather than race code running on the main thread.
   // note that the sub-cases use calls that would succeed if the guards
   // were absent (identity("x"), getwd(), list()), so a missing guard
   // yields Success() rather than an unrelated evaluation error

   // a full construct-and-call chain off the main thread errors cleanly
   Error error;
   std::thread([&error]()
   {
      error = r::exec::RFunction("identity")
            .addParam("x")
            .call();
   }).join();
   expectNonMainThreadError(error);

   // a function constructed off the main thread is poisoned, so it stays
   // unusable even when called from the main thread; getwd() succeeds with
   // no arguments, so only the poisoning can make this fail
   std::unique_ptr<r::exec::RFunction> pFunction;
   std::thread([&pFunction]()
   {
      pFunction.reset(new r::exec::RFunction("getwd"));
   }).join();
   error = pFunction->call();
   expectNonMainThreadError(error);

   // the same holds for a function wrapping an already-resolved SEXP
   SEXP functionSEXP = R_NilValue;
   r::sexp::Protect protect;
   error = r::exec::evaluateString("base::getwd", &functionSEXP, &protect);
   ASSERT_FALSE(error);

   std::unique_ptr<r::exec::RFunction> pSexpFunction;
   std::thread([&pSexpFunction, functionSEXP]()
   {
      pSexpFunction.reset(new r::exec::RFunction(functionSEXP));
   }).join();
   error = pSexpFunction->call();
   expectNonMainThreadError(error);

   // parameters added from a background thread are refused and poison the
   // function: a later main-thread call must fail rather than execute with
   // missing parameters (list() would otherwise succeed with no arguments)
   r::exec::RFunction listFunction("list");
   std::thread([&listFunction]()
   {
      listFunction.addParam("x");
   }).join();
   error = listFunction.call();
   expectNonMainThreadError(error);

   // the same call made entirely on the main thread succeeds
   std::string result;
   error = r::exec::RFunction("identity")
         .addParam("x")
         .call(&result);
   EXPECT_FALSE(error);
   EXPECT_EQ("x", result);
}

TEST(SessionRTest, SysGetenvSeesCoreSetenv) {
   // on Windows, the write-through only reaches R when R resolves getenv()
   // to the same CRT module we do. each module snapshots the Win32
   // environment block once, lazily, and never re-reads it, so a write into
   // another module's table stays invisible to R; only the r::util::setenv
   // bridge helps there
#if defined(_WIN32) && defined(_DEBUG)
   // debug builds link ucrtbased.dll, whereas R.dll always links the release
   // ucrtbase.dll
   GTEST_SKIP() << "debug builds link ucrtbased.dll; R links ucrtbase.dll";
#elif defined(_WIN32)
   // msvcrt builds of R (R < 4.2) keep their own copy of the environment
   bool sharedRuntime = false;
   Error versionError = r::exec::evaluateString("getRversion() >= '4.2.0'", &sharedRuntime);
   ASSERT_FALSE(versionError);
   if (!sharedRuntime)
      GTEST_SKIP() << "R < 4.2 links against msvcrt, which has its own environment";
#endif

   // Sys.getenv reads the environment via R's C runtime; on Windows,
   // core::system::setenv historically wrote only the Win32 environment
   // block, so variables set after R started were visible to child
   // processes but not to R code. setenv now writes through the C runtime
   // as well, so this must hold even with R running
   core::system::setenv("RSTUDIO_ENV_BRIDGE_TEST", "42");

   std::string value;
   Error error = r::exec::RFunction("Sys.getenv")
         .addParam("RSTUDIO_ENV_BRIDGE_TEST")
         .call(&value);
   EXPECT_FALSE(error);
   EXPECT_EQ("42", value);

   // deletion must be visible to R as well
   core::system::unsetenv("RSTUDIO_ENV_BRIDGE_TEST");

   error = r::exec::RFunction("Sys.getenv")
         .addParam("RSTUDIO_ENV_BRIDGE_TEST")
         .call(&value);
   EXPECT_FALSE(error);
   EXPECT_EQ("", value);
}

TEST(SessionRTest, RActiveBindingDetection) {
   // Create an active binding in R
   Error error = r::exec::executeString(R"EOF({
      make_counter <- function() {
        count <- 0L
        function() {
          count <<- count + 1L
          count
        }
      }
      makeActiveBinding("counter", make_counter(), env = globalenv())
   })EOF");
   
   EXPECT_FALSE(error) << "Failed to create active binding";
   
   // Check that it behaves as expected
   int v1 = 0;
   error = r::exec::evaluateString("counter", &v1);
   EXPECT_FALSE(error);
   EXPECT_EQ(1, v1);
   
   int v2 = 0;
   error = r::exec::evaluateString("counter", &v2);
   EXPECT_FALSE(error);
   EXPECT_EQ(2, v2);
   
   // Check that regular bindings are not considered active
   EXPECT_FALSE(r::sexp::isActiveBinding("data.frame", R_GlobalEnv));
   
   // Check that we can detect it
   EXPECT_TRUE(r::sexp::isActiveBinding("counter", R_GlobalEnv));
  
   // Check that undefined variables aren't an error
   EXPECT_FALSE(r::sexp::isActiveBinding("noBindingWithThisNameExists", R_GlobalEnv));
   
   // Remove it when we're done
   error = r::exec::executeString(R"EOF(
      rm(counter, make_counter)
   )EOF");
   EXPECT_FALSE(error);
}

// constructed during static initialization, before R has initialized
// R_NilValue, like the session's own file-scope instances
static r::sexp::PreservedSEXP s_staticPreservedSEXP;

TEST(SessionRTest, PreservedSEXPUnsetStaticReadsAsNil) {
   // a never-set instance must hand back R's NULL, not a null pointer
   ASSERT_NE(nullptr, R_NilValue);
   EXPECT_TRUE(s_staticPreservedSEXP.isNil());
   EXPECT_EQ(R_NilValue, s_staticPreservedSEXP.get());

   // the first set() has nothing to release
   r::sexp::Protect protect;
   SEXP valueSEXP = r::sexp::create("value", &protect);
   s_staticPreservedSEXP.set(valueSEXP);
   EXPECT_FALSE(s_staticPreservedSEXP.isNil());
   EXPECT_EQ(valueSEXP, s_staticPreservedSEXP.get());

   s_staticPreservedSEXP.releaseNow();
   EXPECT_TRUE(s_staticPreservedSEXP.isNil());
   EXPECT_EQ(R_NilValue, s_staticPreservedSEXP.get());
}

TEST(SessionRTest, PreservedSEXPTreatsRNullAsEmpty) {
   r::sexp::PreservedSEXP preserved(R_NilValue);
   EXPECT_TRUE(preserved.isNil());
   EXPECT_EQ(R_NilValue, preserved.get());

   r::sexp::Protect protect;
   SEXP valueSEXP = r::sexp::create("value", &protect);
   preserved.set(valueSEXP);
   EXPECT_FALSE(preserved.isNil());

   preserved.set(R_NilValue);
   EXPECT_TRUE(preserved.isNil());
   EXPECT_EQ(R_NilValue, preserved.get());
}

TEST(SessionRTest, PreservedSEXPMovedFromIsEmpty) {
   r::sexp::Protect protect;
   SEXP valueSEXP = r::sexp::create("value", &protect);

   r::sexp::PreservedSEXP source(valueSEXP);
   r::sexp::PreservedSEXP constructed(std::move(source));
   EXPECT_EQ(valueSEXP, constructed.get());
   EXPECT_TRUE(source.isNil());
   EXPECT_EQ(R_NilValue, source.get());

   r::sexp::PreservedSEXP assigned;
   assigned = std::move(constructed);
   EXPECT_EQ(valueSEXP, assigned.get());
   EXPECT_TRUE(constructed.isNil());
   EXPECT_EQ(R_NilValue, constructed.get());
}

TEST(SessionRTest, StringsReachRMarkedUtf8) {
   // C++ strings are UTF-8; marked native instead, non-ASCII text is misread
   // wherever the native encoding is not UTF-8 (e.g. Windows with R < 4.2), as
   // when an uploaded zip was expanded into a differently named folder (#19627)
   std::string value = "/tmp/test-\xc3\xa9";

   std::string encoding;
   Error error = r::exec::RFunction("base:::Encoding", value).call(&encoding);
   ASSERT_FALSE(error);
   EXPECT_EQ("UTF-8", encoding);

   encoding.clear();
   error = r::exec::RFunction("base:::Encoding")
         .addParam("x", value)
         .call(&encoding);
   ASSERT_FALSE(error);
   EXPECT_EQ("UTF-8", encoding);

   FilePath filePath(value);

   encoding.clear();
   error = r::exec::RFunction("base:::Encoding", filePath).call(&encoding);
   ASSERT_FALSE(error);
   EXPECT_EQ("UTF-8", encoding);

   std::string path;
   error = r::exec::RFunction("base:::identity", filePath).call(&path);
   ASSERT_FALSE(error);
   EXPECT_EQ(filePath.getAbsolutePath(), path);

   std::vector<std::string> encodings;
   std::vector<std::string> values = { value, value };
   error = r::exec::RFunction("base:::Encoding", values).call(&encodings);
   ASSERT_FALSE(error);
   EXPECT_EQ(std::vector<std::string>({ "UTF-8", "UTF-8" }), encodings);
}

// matcher for the error RFunction::call() returns for a parameter with an
// embedded nul; the message distinguishes it from R's own "embedded nul in
// string" error, which a check that came too late would let through
static void expectEmbeddedNulError(const Error& error, const std::string& label)
{
   EXPECT_TRUE(error == r::errc::CodeExecutionError) << label << ": " << error.asString();
   EXPECT_NE(std::string::npos, error.getProperty("errormsg").find("was passed a string with an embedded nul")) << label;
}

// likewise for a parameter name longer than R's limit on symbol names, which
// Rf_install() raises an R error on
static void expectLongNameError(const Error& error, const std::string& label)
{
   EXPECT_TRUE(error == r::errc::CodeExecutionError) << label << ": " << error.asString();
   EXPECT_NE(std::string::npos, error.getProperty("errormsg").find("was passed a parameter name longer than")) << label;
}

TEST(SessionRTest, JsonRpcRefusesStringsWithEmbeddedNul) {
   // R raises an error when asked for a string with an embedded nul. Raised
   // while an R handler's arguments are built, before the call runs protected,
   // it would longjmp past the handler and leave the request unanswered. the
   // handler counts its calls, so the test can tell a refused request from one
   // the handler ran with whatever arguments were accepted
   Error error = r::exec::executeString(R"EOF(
      assign(".rs.test_embedded_nul_calls", 0L, envir = globalenv())
      .rs.addJsonRpcHandler("test_embedded_nul", function(...) {
         calls <- get(".rs.test_embedded_nul_calls", envir = globalenv())
         assign(".rs.test_embedded_nul_calls", calls + 1L, envir = globalenv())
         .rs.scalar(length(list(...)))
      })
   )EOF");
   ASSERT_FALSE(error);

   BOOST_SCOPE_EXIT(void)
   {
      r::exec::executeString(R"EOF(
         rm(".rs.rpc.test_embedded_nul", envir = as.environment("tools:rstudio"))
         rm(".rs.test_embedded_nul_calls", envir = globalenv())
      )EOF");
   }
   BOOST_SCOPE_EXIT_END

   json::JsonRpcMethods methods;
   error = r::json::getRpcMethods(&methods);
   ASSERT_FALSE(error);

   auto it = methods.find("test_embedded_nul");
   ASSERT_NE(methods.end(), it);
   json::JsonRpcFunction handler = it->second;

   // requests are parsed from text, as the session receives them; a json::Value
   // assigned from a std::string stops at the first nul. names are refused too,
   // as R would otherwise truncate them at the nul
   std::vector<std::string> inputs = {
      R"({"method": "test_embedded_nul", "params": ["a\u0000b"]})",
      R"({"method": "test_embedded_nul", "kwparams": {"y": "ok", "x": "a\u0000b"}})",
      R"({"method": "test_embedded_nul", "params": [["a\u0000b"]]})",
      R"({"method": "test_embedded_nul", "params": ["ok", {"y": "ok", "x": "a\u0000b"}]})",
      R"({"method": "test_embedded_nul", "params": [{"a\u0000b": "x"}]})",
      R"({"method": "test_embedded_nul", "kwparams": {"a\u0000b": "x"}})",
   };

   for (const std::string& input : inputs)
   {
      json::JsonRpcRequest request;
      error = json::parseJsonRpcRequest(input, &request);
      ASSERT_FALSE(error) << input;

      // run protected, so that an R error escaping the handler fails this
      // test instead of ending the test run
      Error handlerError;
      json::JsonRpcResponse response;
      error = r::exec::executeSafely([&]() { handlerError = handler(request, &response); });
      EXPECT_FALSE(error) << input << ": " << error.asString();
      expectEmbeddedNulError(handlerError, input);
   }

   // a kwparams name is also refused when it's longer than R allows a symbol
   // name, which R would otherwise raise an error on in the same place
   {
      std::string input =
         R"({"method": "test_embedded_nul", "kwparams": {")" +
         std::string(10001, 'a') +
         R"(": "x"}})";

      json::JsonRpcRequest request;
      error = json::parseJsonRpcRequest(input, &request);
      ASSERT_FALSE(error);

      Error handlerError;
      json::JsonRpcResponse response;
      error = r::exec::executeSafely([&]() { handlerError = handler(request, &response); });
      EXPECT_FALSE(error) << error.asString();
      expectLongNameError(handlerError, "long kwparams name");
   }

   // none of the refused requests ran the handler
   int calls = -1;
   error = r::exec::evaluateString(".rs.test_embedded_nul_calls", &calls);
   ASSERT_FALSE(error);
   EXPECT_EQ(0, calls);

   // a request without a nul still reaches the handler
   json::JsonRpcRequest request;
   error = json::parseJsonRpcRequest(
      R"({"method": "test_embedded_nul", "params": ["ab"], "kwparams": {"x": "a"}})",
      &request);
   ASSERT_FALSE(error);

   Error handlerError;
   json::JsonRpcResponse response;
   error = r::exec::executeSafely([&]() { handlerError = handler(request, &response); });
   ASSERT_FALSE(error) << error.asString();
   ASSERT_FALSE(handlerError) << handlerError.asString();
   EXPECT_EQ(2, response.result().getInt());

   error = r::exec::evaluateString(".rs.test_embedded_nul_calls", &calls);
   ASSERT_FALSE(error);
   EXPECT_EQ(1, calls);
}

TEST(SessionRTest, RFunctionRefusesStringsWithEmbeddedNul) {
   // R raises an error on a string with an embedded nul. raised while a
   // parameter is built, outside the protected call, it would longjmp past the
   // caller, as from a C++ RPC handler passing on a client's string (#19697).
   // names are refused too, as Rf_install() would cut them off at the nul
   std::string nul("a\0b", 3);

   json::Value jsonValue;
   ASSERT_FALSE(jsonValue.parse(R"({"y": "ok", "x": ["ok", "a\u0000b"]})"));

   json::Value jsonName;
   ASSERT_FALSE(jsonName.parse(R"({"y": "ok", "a\u0000b": "x"})"));

   // the fixtures are built here, not in the protected callbacks below, so
   // that an R error escaping a callback can't skip their destructors
   r::sexp::Protect protect;
   r::sexp::ListBuilder builder(&protect);
   builder.add("y", 1);
   builder.add(nul, 2);

   std::vector<std::string> vector = { "ok", nul };
   std::set<std::string> set = { nul };
   std::vector<std::pair<std::string, std::string>> pairs = { { "y", "ok" }, { "x", nul } };
   std::vector<std::pair<std::string, std::string>> pairsName = { { nul, "ok" } };
   std::map<std::string, std::string> map = { { nul, "x" } };
   std::map<std::string, std::string> mapValue = { { "x", nul } };
   std::map<std::string, std::vector<std::string>> mapOfVectors = { { "x", { "ok", nul } } };
   std::map<std::string, std::vector<std::string>> mapOfVectorsName = { { nul, { "ok" } } };
   std::map<std::string, SEXP> mapOfSEXPs = { { nul, R_NilValue } };
   YAML::Node yamlMap = YAML::Load("x: \"a\\0b\"");
   YAML::Node yamlMapKey = YAML::Load("\"a\\0b\": x");
   YAML::Node yamlSequence = YAML::Load("[\"ok\", \"a\\0b\"]");

   typedef std::function<void(r::exec::RFunction*)> AddParams;
   std::vector<std::pair<std::string, AddParams>> cases = {
      { "string", [&](r::exec::RFunction* pFunction) { pFunction->addParam(nul); } },
      { "named string", [&](r::exec::RFunction* pFunction) { pFunction->addParam("x", nul); } },
      { "name", [&](r::exec::RFunction* pFunction) { pFunction->addParam(nul, "x"); } },
      { "SEXP name", [&](r::exec::RFunction* pFunction) { pFunction->addParam(nul, R_NilValue); } },
      { "quoted name", [&](r::exec::RFunction* pFunction) { pFunction->addQuotedParam(nul, R_NilValue); } },
      { "after a valid param", [&](r::exec::RFunction* pFunction) { pFunction->addParam("ok").addParam(nul); } },
      { "vector", [&](r::exec::RFunction* pFunction) { pFunction->addParam(vector); } },
      { "set", [&](r::exec::RFunction* pFunction) { pFunction->addParam(set); } },
      { "pairs", [&](r::exec::RFunction* pFunction) { pFunction->addParam(pairs); } },
      { "pairs name", [&](r::exec::RFunction* pFunction) { pFunction->addParam(pairsName); } },
      { "map name", [&](r::exec::RFunction* pFunction) { pFunction->addParam(map); } },
      { "map value", [&](r::exec::RFunction* pFunction) { pFunction->addParam(mapValue); } },
      { "map of vectors", [&](r::exec::RFunction* pFunction) { pFunction->addParam(mapOfVectors); } },
      { "map of vectors name", [&](r::exec::RFunction* pFunction) { pFunction->addParam(mapOfVectorsName); } },
      { "map of SEXPs", [&](r::exec::RFunction* pFunction) { pFunction->addParam(mapOfSEXPs); } },
      { "json value", [&](r::exec::RFunction* pFunction) { pFunction->addParam(jsonValue); } },
      { "json name", [&](r::exec::RFunction* pFunction) { pFunction->addParam(jsonName); } },
      { "list builder name", [&](r::exec::RFunction* pFunction) { pFunction->addParam(builder); } },
      { "yaml map", [&](r::exec::RFunction* pFunction) { pFunction->addParam(yamlMap); } },
      { "yaml map key", [&](r::exec::RFunction* pFunction) { pFunction->addParam(yamlMapKey); } },
      { "yaml sequence", [&](r::exec::RFunction* pFunction) { pFunction->addParam(yamlSequence); } },
   };

   for (const auto& testCase : cases)
   {
      // run protected, so that an R error escaping the call fails this test
      // instead of ending the test run. the result starts as something other
      // than R's NULL, as a refused call must still set it: callers may pass
      // it on before checking the error
      SEXP resultSEXP = R_GlobalEnv;
      Error callError;
      Error error = r::exec::executeSafely([&]()
      {
         r::sexp::Protect callProtect;
         r::exec::RFunction function("base:::list");
         testCase.second(&function);
         callError = function.call(&resultSEXP, &callProtect);
      });
      EXPECT_FALSE(error) << testCase.first << ": " << error.asString();
      expectEmbeddedNulError(callError, testCase.first);
      EXPECT_EQ(R_NilValue, resultSEXP) << testCase.first;
   }

   // R also raises an error building a call with a parameter name longer than
   // its limit on symbol names (10000 bytes)
   std::string longName(10001, 'a');
   std::vector<std::pair<std::string, AddParams>> longNameCases = {
      { "long name", [&](r::exec::RFunction* pFunction) { pFunction->addParam(longName, "x"); } },
      { "long SEXP name", [&](r::exec::RFunction* pFunction) { pFunction->addParam(longName, R_NilValue); } },
      { "long quoted name", [&](r::exec::RFunction* pFunction) { pFunction->addQuotedParam(longName, R_NilValue); } },
   };

   for (const auto& testCase : longNameCases)
   {
      Error callError;
      Error error = r::exec::executeSafely([&]()
      {
         r::exec::RFunction function("base:::list");
         testCase.second(&function);
         callError = function.call();
      });
      EXPECT_FALSE(error) << testCase.first << ": " << error.asString();
      expectLongNameError(callError, testCase.first);
   }

   // a name at the limit is still passed on, in full
   {
      std::string limitName(10000, 'a');
      std::vector<std::string> names;
      std::string value;
      Error callError;
      Error error = r::exec::executeSafely([&]()
      {
         r::sexp::Protect callProtect;
         SEXP resultSEXP = R_NilValue;
         callError = r::exec::RFunction("base:::list")
               .addParam(limitName, "x")
               .call(&resultSEXP, &callProtect);
         if (!callError && r::sexp::length(resultSEXP) == 1)
         {
            r::sexp::getNames(resultSEXP, &names);
            r::sexp::extract(VECTOR_ELT(resultSEXP, 0), &value);
         }
      });
      EXPECT_FALSE(error) << error.asString();
      ASSERT_FALSE(callError) << callError.asString();
      EXPECT_EQ(std::vector<std::string>({ limitName }), names);
      EXPECT_EQ("x", value);
   }

   // the same kinds of parameters without a nul still reach R
   json::Value jsonOk;
   ASSERT_FALSE(jsonOk.parse(R"({"y": "ok", "x": ["ok", "ab"]})"));

   r::sexp::ListBuilder builderOk(&protect);
   builderOk.add("y", 1);
   builderOk.add("ab", 2);

   std::vector<std::string> vectorOk = { "ok", "ab" };
   YAML::Node yamlOk = YAML::Load("[\"ok\", \"ab\"]");

   // the result is protected within the callback, which R would unwind on an
   // error; a Protect from outside would be left unprotecting the wrong objects
   int length = 0;
   Error callError;
   Error error = r::exec::executeSafely([&]()
   {
      r::sexp::Protect callProtect;
      SEXP resultSEXP = R_NilValue;
      callError = r::exec::RFunction("base:::list")
            .addParam("ok")
            .addParam("x", vectorOk)
            .addParam(jsonOk)
            .addParam(builderOk)
            .addParam(yamlOk)
            .call(&resultSEXP, &callProtect);
      length = r::sexp::length(resultSEXP);
   });
   ASSERT_FALSE(error) << error.asString();
   ASSERT_FALSE(callError) << callError.asString();
   EXPECT_EQ(5, length);
}

#ifndef _WIN32

TEST(SessionRTest, StringsFromRAreReadAsUtf8) {
   // the C locale can't represent the string, so reading it as native rather
   // than as UTF-8 would give "<U+00E9>" in place of the original bytes
   std::string ctype;
   Error error = r::exec::RFunction("base:::Sys.getlocale", "LC_CTYPE").call(&ctype);
   ASSERT_FALSE(error);

   BOOST_SCOPE_EXIT(&ctype)
   {
      r::exec::RFunction("base:::Sys.setlocale", "LC_CTYPE", ctype).call();
   }
   BOOST_SCOPE_EXIT_END

   error = r::exec::RFunction("base:::Sys.setlocale", "LC_CTYPE", "C").call();
   ASSERT_FALSE(error);

   std::string value = "/tmp/test-\xc3\xa9";
   r::sexp::Protect protect;
   SEXP valueSEXP = r::sexp::create(value, &protect);
   EXPECT_EQ(value, r::sexp::asString(valueSEXP));

   std::string result;
   error = r::exec::RFunction("base:::identity", valueSEXP).call(&result);
   ASSERT_FALSE(error);
   EXPECT_EQ(value, result);

   std::vector<std::string> results;
   error = r::exec::RFunction("base:::identity", valueSEXP).call(&results);
   ASSERT_FALSE(error);
   EXPECT_EQ(std::vector<std::string>({ value }), results);
}

TEST(SessionRTest, NamesReadFromRFindTheirBindings) {
   // names are read from R as UTF-8, but symbol names are native, so a lookup
   // by name must translate the name back. In Latin-1, as in a Windows code
   // page with R < 4.2, installing the UTF-8 bytes as-is names a different
   // symbol, and the Environment pane showed the variable with no value
   std::string ctype;
   Error error = r::exec::RFunction("base:::Sys.getlocale", "LC_CTYPE").call(&ctype);
   ASSERT_FALSE(error);

   BOOST_SCOPE_EXIT(&ctype)
   {
      r::exec::RFunction("base:::Sys.setlocale", "LC_CTYPE", ctype).call();
   }
   BOOST_SCOPE_EXIT_END

   std::string latin1;
   for (const char* locale : { "en_US.ISO8859-1", "en_US.ISO-8859-1" })
   {
      error = r::exec::RFunction("base:::Sys.setlocale", "LC_CTYPE", locale).call(&latin1);
      if (!error && !latin1.empty())
         break;
   }

   if (latin1.empty())
      GTEST_SKIP() << "no Latin-1 locale is installed";

   // binds "caf\xe9" (Latin-1) as R's parser would for that name
   r::sexp::Protect protect;
   SEXP envSEXP = R_NilValue;
   error = r::exec::evaluateString(R"(
      local({
         name <- rawToChar(as.raw(c(0x63, 0x61, 0x66, 0xe9)))
         inner <- new.env(parent = emptyenv())
         makeActiveBinding(name, function() 42L, inner)
         env <- new.env(parent = emptyenv())
         assign(name, 42L, envir = env)
         assign("inner", inner, envir = env)
         env
      })
   )", &envSEXP, &protect);
   ASSERT_FALSE(error) << error.asString();

   std::string name = "caf\xc3\xa9";

   std::vector<std::string> names;
   r::sexp::listEnvironment(envSEXP, false, false, &names);
   EXPECT_EQ(std::vector<std::string>({ name, "inner" }), names);

   std::vector<std::string> objects;
   error = r::sexp::objects(envSEXP, false, &objects);
   ASSERT_FALSE(error);
   EXPECT_EQ(names, objects);

   EXPECT_EQ(r::sexp::BindingType::Normal, r::sexp::getBindingType(name, envSEXP));

   SEXP valueSEXP = r::sexp::findVar(name, envSEXP);
   ASSERT_NE(nullptr, valueSEXP);
   EXPECT_EQ(42, r::sexp::asInteger(valueSEXP));
   EXPECT_EQ(valueSEXP, r::sexp::getBindingIdentity(name, envSEXP, r::sexp::BindingType::Normal));

   SEXP innerSEXP = r::sexp::findVar("inner", envSEXP);
   ASSERT_NE(nullptr, innerSEXP);
   EXPECT_TRUE(r::sexp::isActiveBinding(name, innerSEXP));
   EXPECT_TRUE(r::sexp::hasActiveBinding("inner", envSEXP));
}

#endif

#ifdef _WIN32

TEST(SessionRTest, SynchronizeLocaleRepairsAClobberedLocale) {
   // Before R 4.2, R and rsession use different C runtimes. There is no
   // shared locale for code outside R to clobber, so the repair path does not
   // apply.
   bool sharedRuntime = false;
   Error versionError = r::exec::evaluateString(
            "getRversion() >= '4.2.0'", &sharedRuntime);
   ASSERT_FALSE(versionError);
   if (!sharedRuntime)
      GTEST_SKIP() << "R < 4.2 links against a separate C runtime";

   // rsession and UCRT builds of R share one C runtime, so code that switches
   // to the "C" locale and then fails to restore it leaves the session there
   // while R goes on believing it is in its original locale. Everything that
   // trusts the C runtime locale then corrupts non-ASCII text.
   // https://github.com/rstudio/rstudio/issues/18139
   const wchar_t* pLocale = ::_wsetlocale(LC_CTYPE, nullptr);
   ASSERT_NE(nullptr, pLocale);

   std::wstring locale(pLocale);
   if (locale == L"C")
      GTEST_SKIP() << "session is already running in the C locale";

   BOOST_SCOPE_EXIT(&locale)
   {
      ::_wsetlocale(LC_CTYPE, locale.c_str());
   }
   BOOST_SCOPE_EXIT_END

   // let synchronizeLocale() record the session locale, then move the C
   // runtime behind R's back the way a failed save/restore would
   r::util::synchronizeLocale();
   ASSERT_NE(nullptr, ::_wsetlocale(LC_CTYPE, L"C"));

   r::util::synchronizeLocale();

   const wchar_t* pRepaired = ::_wsetlocale(LC_CTYPE, nullptr);
   ASSERT_NE(nullptr, pRepaired);
   EXPECT_STREQ(locale.c_str(), pRepaired);
}

#endif

} // namespace tests
} // namespace session
} // namespace rstudio
