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
#include <condition_variable>
#include <functional>
#include <map>
#include <memory>
#include <mutex>
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
#include <r/session/RSession.hpp>

#include "../SessionRpc.hpp"

#ifndef _WIN32
#include <R_ext/libextern.h>

extern "C" {
LibExtern Rboolean R_interrupts_suspended;
}
#endif

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

TEST(SessionRTest, LookupsTreatNamesRCantInstallAsUnbound) {
   // R raises an error making a symbol from an empty name or one over 10000
   // bytes, and cuts a name off at a nul; a lookup by such a name, as the
   // linter makes for a long string it reads as a call, must find nothing
   // rather than longjmp, or look up the name before the nul
   Error error = r::exec::executeString(R"EOF(
      assign(".rs.test.lookup", function() NULL, envir = globalenv())
   )EOF");
   ASSERT_FALSE(error);

   BOOST_SCOPE_EXIT(void)
   {
      r::exec::executeString(R"EOF(
         rm(".rs.test.lookup", envir = globalenv())
      )EOF");
   }
   BOOST_SCOPE_EXIT_END

   std::string bound = ".rs.test.lookup";
   std::vector<std::string> names = {
      std::string(),
      bound + std::string("\0suffix", 7),
      std::string(10001, 'a'),
   };

   for (const std::string& name : names)
   {
      EXPECT_FALSE(r::sexp::isSymbolName(name)) << name.size();

      SEXP varSEXP = R_GlobalEnv;
      SEXP varInNamespaceSEXP = R_GlobalEnv;
      SEXP functionSEXP = R_GlobalEnv;
      SEXP identitySEXP = R_GlobalEnv;
      r::sexp::BindingType bindingType = r::sexp::BindingType::Normal;
      bool active = true;
      Error lookupError = r::exec::executeSafely([&]()
      {
         varSEXP = r::sexp::findVar(name, R_GlobalEnv);
         varInNamespaceSEXP = r::sexp::findVar(name, "base");
         functionSEXP = r::sexp::findFunction(name);
         identitySEXP = r::sexp::getBindingIdentity(name, R_GlobalEnv, r::sexp::BindingType::Normal);
         bindingType = r::sexp::getBindingType(name, R_GlobalEnv);
         active = r::sexp::isActiveBinding(name, R_GlobalEnv);
      });
      EXPECT_FALSE(lookupError) << name.size() << ": " << lookupError.asString();
      EXPECT_EQ(nullptr, varSEXP) << name.size();
      EXPECT_EQ(nullptr, varInNamespaceSEXP) << name.size();
      EXPECT_EQ(nullptr, functionSEXP) << name.size();
      EXPECT_EQ(R_NilValue, identitySEXP) << name.size();
      EXPECT_TRUE(bindingType == r::sexp::BindingType::Unbound) << name.size();
      EXPECT_FALSE(active) << name.size();
   }

   // the name itself is still found
   EXPECT_TRUE(r::sexp::isSymbolName(bound));
   EXPECT_TRUE(r::sexp::isSymbolName(std::string(10000, 'a')));
   EXPECT_NE(nullptr, r::sexp::findVar(bound, R_GlobalEnv));
   EXPECT_NE(nullptr, r::sexp::findFunction(bound));
}

TEST(SessionRTest, LookupsCatchErrorsFromBindings) {
   // reading an active binding calls its function, and R >= 4.6 forces a
   // promise read with R_getVarEx(); an error from either must be caught, as
   // when the data viewer re-reads an object it shows after it starts to
   // fail, rather than longjmp past the lookup
   // the failing binding counts its calls, and the promise records whether
   // it was forced, so the test can tell a lookup that read them from one
   // that didn't
   Error error = r::exec::executeString(R"EOF(
      # a function of the same name further out, which findFunction() must
      # not return once the binding that shadows it fails, as R wouldn't;
      # attached first, as checking for conflicts would read the binding
      attach(list(.rs.test.failingBinding = function() NULL,
                  .rs.test.missingBinding = function() NULL),
             name = "rstudio-test-shadowed", warn.conflicts = FALSE)

      # a missing argument, an error for R's lookup of a function too
      assign(".rs.test.missingBinding", quote(expr = ), envir = globalenv())

      # bindings that warn, or signal an interrupt, while being read
      makeActiveBinding(".rs.test.warningBinding", function() {
         warning("binding warned")
         7L
      }, globalenv())
      makeActiveBinding(".rs.test.interruptingBinding", function() {
         signalCondition(structure(list(message = "", call = NULL), class = c("interrupt", "condition")))
         7L
      }, globalenv())

      assign(".rs.test.bindingCalls", 0L, envir = globalenv())
      assign(".rs.test.promiseForced", FALSE, envir = globalenv())
      makeActiveBinding(".rs.test.failingBinding", function() {
         calls <- get(".rs.test.bindingCalls", envir = globalenv())
         assign(".rs.test.bindingCalls", calls + 1L, envir = globalenv())
         stop("binding failed")
      }, globalenv())
      delayedAssign(".rs.test.failingPromise", {
         assign(".rs.test.promiseForced", TRUE, envir = globalenv())
         stop("promise failed")
      }, assign.env = globalenv())
      makeActiveBinding(".rs.test.workingBinding", function() 42L, globalenv())

      # a promise for a function, which counts how often it's forced
      assign(".rs.test.functionPromiseForces", 0L, envir = globalenv())
      delayedAssign(".rs.test.functionPromise", {
         forces <- get(".rs.test.functionPromiseForces", envir = globalenv())
         assign(".rs.test.functionPromiseForces", forces + 1L, envir = globalenv())
         function() NULL
      }, assign.env = globalenv())
   )EOF");
   ASSERT_FALSE(error) << error.asString();

   bool wasAtTopLevelPrompt = r::session::atTopLevelPrompt();
   BOOST_SCOPE_EXIT(wasAtTopLevelPrompt)
   {
      r::session::setAtTopLevelPrompt(wasAtTopLevelPrompt);
      r::exec::executeString(R"EOF(
         detach("rstudio-test-shadowed")
         rm(list = c(".rs.test.failingBinding", ".rs.test.failingPromise", ".rs.test.workingBinding",
                     ".rs.test.bindingCalls", ".rs.test.promiseForced", ".rs.test.missingBinding",
                     ".rs.test.warningBinding", ".rs.test.interruptingBinding",
                     ".rs.test.functionPromise", ".rs.test.functionPromiseForces"),
            envir = globalenv())
      )EOF");
   }
   BOOST_SCOPE_EXIT_END

   bool forcesPromises = r::runtime::lookupsForcePromises();

   bool catchesOnlyErrors = false;
   error = r::exec::evaluateString("getRversion() >= '3.6.2'", &catchesOnlyErrors);
   ASSERT_FALSE(error);

   // each lookup reads the failing binding once, and finds nothing; not
   // even findFunction(), with a function of that name further out
   SEXP bindingSEXP = R_GlobalEnv;
   SEXP bindingInFrameSEXP = R_GlobalEnv;
   SEXP bindingFunctionSEXP = R_GlobalEnv;
   Error lookupError = r::exec::executeSafely([&]()
   {
      bindingSEXP = r::sexp::findVar(".rs.test.failingBinding", R_GlobalEnv);
      bindingInFrameSEXP = r::sexp::findVarInFrame(R_GlobalEnv, Rf_install(".rs.test.failingBinding"));
      bindingFunctionSEXP = r::sexp::findFunction(".rs.test.failingBinding");
   });
   ASSERT_FALSE(lookupError) << lookupError.asString();
   EXPECT_EQ(nullptr, bindingSEXP);
   EXPECT_EQ(nullptr, bindingInFrameSEXP);
   EXPECT_EQ(nullptr, bindingFunctionSEXP);

   int bindingCalls = 0;
   error = r::exec::evaluateString(".rs.test.bindingCalls", &bindingCalls);
   ASSERT_FALSE(error);
   EXPECT_EQ(3, bindingCalls);

   // R_getVarEx() (where R exports it) forces a promise a lookup reads, and
   // the error is caught; R's older lookup returns the promise itself
   SEXP promiseSEXP = R_GlobalEnv;
   lookupError = r::exec::executeSafely([&]()
   {
      promiseSEXP = r::sexp::findVar(".rs.test.failingPromise", R_GlobalEnv);
   });
   ASSERT_FALSE(lookupError) << lookupError.asString();

   bool promiseForced = false;
   error = r::exec::evaluateString(".rs.test.promiseForced", &promiseForced);
   ASSERT_FALSE(error);
   EXPECT_EQ(forcesPromises, promiseForced);
   if (forcesPromises)
      EXPECT_EQ(nullptr, promiseSEXP);
   else
      EXPECT_TRUE(promiseSEXP != nullptr && TYPEOF(promiseSEXP) == PROMSXP);

   // a binding that warns is still read, as only errors and interrupts are
   // caught; one that signals an interrupt reads as unbound, and the
   // interrupt is re-armed for R code that's running, but not at the
   // top-level prompt, where none is; and a missing argument ends
   // findFunction()'s search, as it ends R's
   SEXP warningSEXP = nullptr;
   SEXP interruptingSEXP = R_GlobalEnv;
   bool interruptRearmed = false;
   bool interruptRearmedAtPrompt = true;
   SEXP missingFunctionSEXP = R_GlobalEnv;
   lookupError = r::exec::executeSafely([&]()
   {
      warningSEXP = r::sexp::findVar(".rs.test.warningBinding", R_GlobalEnv);

      r::session::setAtTopLevelPrompt(false);
      interruptingSEXP = r::sexp::findVar(".rs.test.interruptingBinding", R_GlobalEnv);
      interruptRearmed = r::exec::interruptsPending();
      r::exec::setInterruptsPending(false);

      r::session::setAtTopLevelPrompt(true);
      r::sexp::findVar(".rs.test.interruptingBinding", R_GlobalEnv);
      interruptRearmedAtPrompt = r::exec::interruptsPending();
      r::exec::setInterruptsPending(false);

      missingFunctionSEXP = r::sexp::findFunction(".rs.test.missingBinding");
   });
   ASSERT_FALSE(lookupError) << lookupError.asString();
   EXPECT_TRUE(warningSEXP != nullptr && TYPEOF(warningSEXP) == INTSXP);
   EXPECT_EQ(nullptr, missingFunctionSEXP);

   // before R 3.6.2, R_ToplevelExec() is used, under which signalling an
   // interrupt condition is a no-op
   if (catchesOnlyErrors)
   {
      EXPECT_EQ(nullptr, interruptingSEXP);
      EXPECT_TRUE(interruptRearmed);
      EXPECT_FALSE(interruptRearmedAtPrompt);
   }

   // findFunction() forces the promise, catching the error
   SEXP promiseFunctionSEXP = R_GlobalEnv;
   int workingValue = 0;
   lookupError = r::exec::executeSafely([&]()
   {
      promiseFunctionSEXP = r::sexp::findFunction(".rs.test.failingPromise");

      // a binding that doesn't fail is still read
      SEXP workingSEXP = r::sexp::findVar(".rs.test.workingBinding", R_GlobalEnv);
      if (workingSEXP != nullptr)
         workingValue = r::sexp::asInteger(workingSEXP);
   });
   ASSERT_FALSE(lookupError) << lookupError.asString();
   EXPECT_EQ(nullptr, promiseFunctionSEXP);
   EXPECT_EQ(42, workingValue);

   // a promise for a function is forced once, and once forced, its value is
   // found again (R < 4.6 reads it from the promise the lookup returns)
   SEXP firstFunctionSEXP = R_NilValue;
   SEXP secondFunctionSEXP = R_NilValue;
   lookupError = r::exec::executeSafely([&]()
   {
      firstFunctionSEXP = r::sexp::findFunction(".rs.test.functionPromise");
      secondFunctionSEXP = r::sexp::findFunction(".rs.test.functionPromise");
   });
   ASSERT_FALSE(lookupError) << lookupError.asString();
   EXPECT_TRUE(firstFunctionSEXP != nullptr && TYPEOF(firstFunctionSEXP) == CLOSXP);
   EXPECT_EQ(firstFunctionSEXP, secondFunctionSEXP);

   int functionPromiseForces = 0;
   error = r::exec::evaluateString(".rs.test.functionPromiseForces", &functionPromiseForces);
   ASSERT_FALSE(error);
   EXPECT_EQ(1, functionPromiseForces);
}

TEST(SessionRTest, ParseStringRefusesEmbeddedNul) {
   // R would raise an error making the code's string, before the parse runs
   // protected (#19697)
   std::string code = std::string("1\0", 2) + "+ 1";

   int value = 0;
   Error evaluateError;
   Error error = r::exec::executeSafely([&]()
   {
      evaluateError = r::exec::evaluateString(code, &value);
   });
   ASSERT_FALSE(error) << error.asString();
   EXPECT_TRUE(evaluateError == r::errc::ExpressionParsingError) << evaluateError.asString();
}

TEST(SessionRTest, CreateKeepsTheProtectionStackShortForLongJson) {
   // R's protection stack holds at most 500000 entries, and overflowing it
   // is an R error; protecting every element of a long array or object
   // while it was built overflowed it
   const int n = 600000;
   json::Array array;
   for (int i = 0; i < n; i++)
      array.push_back(json::Value("x"));

   // parsed, as adding members one at a time checks each for a duplicate
   std::string objectText = "{";
   for (int i = 0; i < n; i++)
   {
      if (i > 0)
         objectText += ",";
      objectText += "\"k" + std::to_string(i) + "\":" + std::to_string(i);
   }
   objectText += "}";

   json::Value object;
   ASSERT_FALSE(object.parse(objectText));

   int arrayLength = 0;
   int objectLength = 0;
   Error error = r::exec::executeSafely([&]()
   {
      r::sexp::Protect protect;
      arrayLength = r::sexp::length(r::sexp::create(array, &protect));
      objectLength = r::sexp::length(r::sexp::create(object, &protect));
   });
   ASSERT_FALSE(error) << error.asString();
   EXPECT_EQ(n, arrayLength);
   EXPECT_EQ(n, objectLength);
}

TEST(SessionRTest, CreateTurnsACyclicYamlAliasIntoNull) {
   // an alias can refer to a node that contains it; following it recursed
   // until the stack overflowed, crashing the session (as for a _quarto.yml
   // read at startup)
   YAML::Node node = YAML::Load("a: &x [1, *x]");

   int outerLength = 0;
   int innerLength = 0;
   bool cycleIsNull = false;
   bool nul = true;
   Error error = r::exec::executeSafely([&]()
   {
      r::sexp::Protect protect;
      SEXP listSEXP = r::sexp::create(node, &protect);
      outerLength = r::sexp::length(listSEXP);
      if (outerLength == 1)
      {
         SEXP innerSEXP = VECTOR_ELT(listSEXP, 0);
         innerLength = r::sexp::length(innerSEXP);
         cycleIsNull = innerLength == 2 && VECTOR_ELT(innerSEXP, 1) == R_NilValue;
      }
      nul = r::sexp::containsNul(node);
   });
   ASSERT_FALSE(error) << error.asString();
   EXPECT_EQ(1, outerLength);
   EXPECT_EQ(2, innerLength);
   EXPECT_TRUE(cycleIsNull);
   EXPECT_FALSE(nul);

   // a null key is read as "null", so a nul after one is still found; a
   // composite key, which create() can't read, ends the check
   EXPECT_TRUE(r::sexp::containsNul(YAML::Load("~: x\nmode: \"a\\0b\"")));
   EXPECT_FALSE(r::sexp::containsNul(YAML::Load("[a, b]: x\nmode: \"a\\0b\"")));

   // an alias to a node that doesn't contain it is still followed
   YAML::Node shared = YAML::Load("a: &x [1, 2]\nb: *x");
   int sharedLength = 0;
   error = r::exec::executeSafely([&]()
   {
      r::sexp::Protect protect;
      SEXP listSEXP = r::sexp::create(shared, &protect);
      if (r::sexp::length(listSEXP) == 2)
         sharedLength = r::sexp::length(VECTOR_ELT(listSEXP, 1));
   });
   ASSERT_FALSE(error) << error.asString();
   EXPECT_EQ(2, sharedLength);
}

TEST(SessionRTest, InvokeRpcHandlerAnswersARequestAnRErrorInterrupted) {
   // an R error raised by code a handler runs unprotected would longjmp past
   // the request, leaving it unanswered; the dispatcher's backstop answers it,
   // and the error then carries on unwinding (here, to executeSafely())
   json::JsonRpcRequest request;
   request.method = "test_r_error";

   // a continuation following the dispatcher's delivery protocol, as the
   // dispatcher's own continuations do: it claims the response, can be made
   // to fail before sending it, and marks it sent
   boost::shared_ptr<session::rpc::RpcDelivery> pDelivery;
   int responses = 0;
   int failuresBeforeSending = 0;
   Error responseError;
   json::JsonRpcFunctionContinuation continuation =
      [&](const Error& error, json::JsonRpcResponse* pResponse)
      {
         if (!session::rpc::claimRpcDelivery(pDelivery.get()))
            return;

         if (failuresBeforeSending > 0)
         {
            failuresBeforeSending--;
            Rf_error("failed before sending");
         }

         responses++;
         responseError = error;
         session::rpc::markRpcDeliverySent(pDelivery.get());

         if (pResponse != nullptr && pResponse->hasAfterResponse())
            session::rpc::runAfterResponse(pDelivery.get(), pResponse);
      };

   json::JsonRpcFunctionContinuation retained;
   auto reset = [&]()
   {
      pDelivery = boost::make_shared<session::rpc::RpcDelivery>();
      responses = 0;
      failuresBeforeSending = 0;
      responseError = Success();
      retained = json::JsonRpcFunctionContinuation();
   };

   // returns whether an R error unwound past invokeRpcHandler()
   auto invoke = [&](const json::JsonRpcAsyncFunction& handler)
   {
      Error error = r::exec::executeSafely([&]()
      {
         session::rpc::invokeRpcHandler(handler, request, continuation, pDelivery);
      });
      return !!error;
   };

   // a handler that raises an R error before responding is answered with it
   json::JsonRpcAsyncFunction failing =
      [](const json::JsonRpcRequest&, const json::JsonRpcFunctionContinuation&)
      {
         Rf_error("handler failed");
      };

   reset();
   EXPECT_TRUE(invoke(failing));
   EXPECT_EQ(1, responses);
   EXPECT_TRUE(responseError == r::errc::CodeExecutionError) << responseError.asString();
   EXPECT_NE(std::string::npos, responseError.getProperty("errormsg").find("handler failed"));

#ifndef _WIN32
   // an interrupt writes no error message, so the reply doesn't quote the one
   // R reported last; Rf_onintr() is R's own path for a pending interrupt
   // (on Windows, R_ProcessEvents() runs it, and would run session events)
   json::JsonRpcAsyncFunction interrupted =
      [](const json::JsonRpcRequest&, const json::JsonRpcFunctionContinuation&)
      {
         r::exec::setInterruptsPending(true);
         Rf_onintr();
      };

   // executeString() catches the error, leaving its message as R's last
   reset();
   ASSERT_TRUE(r::exec::executeString("stop('a stale error')"));

   // the tests run inside R's initialization, which suspends interrupts
   Rboolean interruptsWereSuspended = R_interrupts_suspended;
   R_interrupts_suspended = static_cast<Rboolean>(false);
   bool interruptUnwound = invoke(interrupted);
   R_interrupts_suspended = interruptsWereSuspended;
   r::exec::setInterruptsPending(false);

   EXPECT_TRUE(interruptUnwound);
   EXPECT_EQ(1, responses);
   EXPECT_TRUE(responseError == r::errc::CodeExecutionError) << responseError.asString();
   std::string interruptedMessage = responseError.getProperty("errormsg");
   EXPECT_EQ(std::string::npos, interruptedMessage.find("a stale error")) << interruptedMessage;
   EXPECT_NE(std::string::npos, interruptedMessage.find("interrupted")) << interruptedMessage;
#endif

   // one that responded before the error isn't answered again
   json::JsonRpcAsyncFunction respondsThenFails =
      [](const json::JsonRpcRequest&, const json::JsonRpcFunctionContinuation& respond)
      {
         json::JsonRpcResponse response;
         respond(Success(), &response);
         Rf_error("handler failed after responding");
      };

   reset();
   EXPECT_TRUE(invoke(respondsThenFails));
   EXPECT_EQ(1, responses);
   EXPECT_FALSE(responseError) << responseError.asString();

   // one whose response failed before it was sent (as when change detection
   // fails ahead of sending) is answered with the error
   json::JsonRpcAsyncFunction responds =
      [](const json::JsonRpcRequest&, const json::JsonRpcFunctionContinuation& respond)
      {
         json::JsonRpcResponse response;
         respond(Success(), &response);
      };

   reset();
   failuresBeforeSending = 1;
   EXPECT_TRUE(invoke(responds));
   EXPECT_EQ(1, responses);
   EXPECT_NE(std::string::npos, responseError.getProperty("errormsg").find("failed before sending"));

   // a response another thread has claimed, but not yet sent, is left to it:
   // the backstop doesn't answer, and that thread's response is the one sent
   reset();
   std::mutex mutex;
   std::condition_variable changed;
   bool workerClaimed = false;
   bool workerMaySend = false;
   std::thread worker([&]()
   {
      bool owns = session::rpc::claimRpcDelivery(pDelivery.get());

      std::unique_lock<std::mutex> lock(mutex);
      workerClaimed = true;
      changed.notify_all();
      changed.wait(lock, [&]() { return workerMaySend; });

      if (owns)
      {
         responses++;
         session::rpc::markRpcDeliverySent(pDelivery.get());
      }
   });

   {
      std::unique_lock<std::mutex> lock(mutex);
      changed.wait(lock, [&]() { return workerClaimed; });
   }

   EXPECT_TRUE(invoke(failing));
   EXPECT_EQ(0, responses);

   {
      std::lock_guard<std::mutex> lock(mutex);
      workerMaySend = true;
   }
   changed.notify_all();
   worker.join();
   EXPECT_EQ(1, responses);
   EXPECT_FALSE(responseError) << responseError.asString();
   EXPECT_TRUE(pDelivery->sent);

   // after-response work runs once the handler has returned, outside the
   // backstop's context: R discards calling handlers registered in a context
   // when it ends, and that work registers some (a notebook's first chunk)
   bool handlerReturned = false;
   bool afterResponseRan = false;
   bool afterResponseRanAfterHandler = false;
   json::JsonRpcAsyncFunction respondsWithAfterResponse =
      [&](const json::JsonRpcRequest&, const json::JsonRpcFunctionContinuation& respond)
      {
         json::JsonRpcResponse response;
         response.setSuppressDetectChanges(true);
         response.setAfterResponse([&]()
         {
            afterResponseRan = true;
            afterResponseRanAfterHandler = handlerReturned;
         });
         respond(Success(), &response);
         handlerReturned = true;
      };

   reset();
   EXPECT_FALSE(invoke(respondsWithAfterResponse));
   EXPECT_EQ(1, responses);
   EXPECT_TRUE(afterResponseRan);
   EXPECT_TRUE(afterResponseRanAfterHandler);

   // an async handler that responds after returning is answered once, by it
   json::JsonRpcAsyncFunction retains =
      [&](const json::JsonRpcRequest&, const json::JsonRpcFunctionContinuation& respond)
      {
         retained = respond;
      };

   reset();
   EXPECT_FALSE(invoke(retains));
   EXPECT_EQ(0, responses);

   json::JsonRpcResponse lateResponse;
   retained(Success(), &lateResponse);
   EXPECT_EQ(1, responses);
   EXPECT_FALSE(responseError) << responseError.asString();

   // and one that keeps its continuation, then fails, isn't answered twice
   json::JsonRpcAsyncFunction retainsThenFails =
      [&](const json::JsonRpcRequest&, const json::JsonRpcFunctionContinuation& respond)
      {
         retained = respond;
         Rf_error("handler failed after keeping its continuation");
      };

   reset();
   EXPECT_TRUE(invoke(retainsThenFails));
   EXPECT_EQ(1, responses);

   retained(Success(), &lateResponse);
   EXPECT_EQ(1, responses);
   EXPECT_TRUE(responseError == r::errc::CodeExecutionError) << responseError.asString();

   // off the main thread (offline requests), the handler runs directly
   reset();
   std::thread([&]()
   {
      session::rpc::invokeRpcHandler(responds, request, continuation, pDelivery);
   }).join();
   EXPECT_EQ(1, responses);

   // a C++ exception still reaches the caller, and leaves R usable
   json::JsonRpcAsyncFunction throws =
      [](const json::JsonRpcRequest&, const json::JsonRpcFunctionContinuation&)
      {
         throw std::runtime_error("handler threw");
      };

   reset();
   EXPECT_THROW(session::rpc::invokeRpcHandler(throws, request, continuation, pDelivery), std::runtime_error);

   int value = 0;
   Error error = r::exec::evaluateString("1L + 1L", &value);
   EXPECT_FALSE(error) << error.asString();
   EXPECT_EQ(2, value);
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
