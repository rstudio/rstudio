/*
 * SessionEnvironment.cpp
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

#include "SessionEnvironment.hpp"
#include "EnvironmentMonitor.hpp"

#define INTERNAL_R_FUNCTIONS

#include <fmt/format.h>

#include <algorithm>

#include <boost/container/flat_map.hpp>

#include <core/Exec.hpp>
#include <core/RecursionGuard.hpp>
#include <core/system/LibraryLoader.hpp>

#include <r/RExec.hpp>
#include <r/RHelpers.hpp>
#include <r/RInterface.hpp>
#include <r/RJson.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>
#include <r/RUtil.hpp>
#include <r/RVersion.hpp>
#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "EnvironmentUtils.hpp"

#if defined(_WIN32)
# define kLibraryName "R.dll"
#elif defined(__APPLE__)
# define kLibraryName "libR.dylib"
#else
# define kLibraryName "libR.so"
#endif

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules { 
namespace environment {

namespace {

// which language is currently active on the front-end? this state is
// synchronized between client and server so that we can tell when to
// active environment monitors for different languages
std::string s_environmentLanguage = kEnvironmentLanguageR;

// allocate on the heap so we control timing of destruction (if we leave it
// to the destructor we might release the underlying environment SEXP after
// R has already shut down)
EnvironmentMonitor* s_pEnvironmentMonitor = nullptr;

// which Python module is currently being monitored, if any?
std::string s_monitoredPythonModule;

// is the browser currently active? we store this state
// so that we can query this from R, without 'hiding' the
// browser state by pushing new contexts / frames on the stack
bool s_browserActive = false;

// the environment being browsed (set by onConsolePrompt when entering debug).
// this is needed because during promise forcing, the browser's environment
// may differ from any sys.frame() visible from R.
r::sexp::PreservedSEXP s_browserEnv;

// are we currently monitoring the environment? this is almost always true, but can be 
// disabled by the user to help mitigate pathological cases in which environment monitoring
// has undesirable side effects.
bool s_monitoring = true;

// whether or not the global environment can safely be serialized
bool s_isGlobalEnvironmentSerializable = true;

// whether or not particular objects can be safely serialized
using SerializationCache = boost::container::flat_map<SEXP, bool>;
SerializationCache s_serializationCache;

// by default, use regular 'INTEGER' accessor; 'INTEGER_OR_NULL' will be loaded
// if provided by this version of R
int* (*INTEGER_OR_NULL)(SEXP) = INTEGER;

// avoid potential contention between console handlers
std::recursive_mutex s_consoleMutex;

// Keeps track of the data related to the most recent debugging event
class LineDebugState
{
   public:
      LineDebugState()
      {
         reset();
      }
      void reset()
      {
         lastDebugText = "";
         lastDebugLine = 0;
      }
      std::string lastDebugText;
      int lastDebugLine;
};

// The environment monitor and friends do work in response to events in R.
// In rare cases, this work can trigger the same events in R that are
// being responded to, leading to unwanted recursion. This simple guard
// increments the given counter on construction (and decrements on destruction)
// so vulnerable event handlers below can prevent reentrancy.
class EventRecursionGuard
{
public:
   EventRecursionGuard(int& counter): counter_(counter) { counter_++; }
   ~EventRecursionGuard() { counter_--; }
private:
   int& counter_;
};

bool isCompactRowNames(SEXP rowNamesInfoSEXP)
{
   return
         TYPEOF(rowNamesInfoSEXP) == INTSXP &&
         r::sexp::length(rowNamesInfoSEXP) == 2 &&
         INTEGER(rowNamesInfoSEXP)[0] == NA_INTEGER;
}

bool isSerializableImpl(SEXP valueSEXP)
{
   // Check for SEXP types that we know can be safely serialized.
   switch (TYPEOF(valueSEXP))
   {
   
   // Assume that promises can be safely serialized.
   // (We just want to avoid evaluating them here.)
   case PROMSXP:
      return true;
      
   // Assume that functions can be serialized.
   // Technically, a function's closure _could_ contain arbitrary objects,
   // which might not be serializable, but that should be exceedingly rare.
   case CLOSXP:
      return true;
      
   }
   
   // Check for SEXP types that we know cannot be safely serialized.
   switch (TYPEOF(valueSEXP))
   {
   
   // External pointers and weak references cannot be serialized.
   case EXTPTRSXP:
   case WEAKREFSXP:
      return false;
      
   }
   
   // Assume base environments can be serialized.
   if (valueSEXP == R_BaseEnv || valueSEXP == R_BaseNamespace)
      return true;
   
   if (TYPEOF(valueSEXP) == ENVSXP)
   {
      // Assume package environments + namespaces can be serialized.
      if (R_IsNamespaceEnv(valueSEXP) || R_IsPackageEnv(valueSEXP))
         return true;
   }
   
   // Check for 'known-safe' object classes.
   auto safeClasses = { "data.frame", "grf", "igraph" };
   for (auto&& safeClass : safeClasses)
      if (Rf_inherits(valueSEXP, safeClass))
         return true;
   
   // Check for 'known-unsafe' object classes.
   auto unsafeClasses = { "ArrowObject", "DBIConnection", "python.builtin.object" };
   for (auto&& unsafeClass : unsafeClasses)
      if (Rf_inherits(valueSEXP, unsafeClass))
         return false;
   
   // Assume other objects can be serialized.
   return true;
}

bool isSerializable(SEXP valueSEXP)
{
   // An object is serializable if it is not, or does not contain, any non-serializable objects.
   return !r::recursiveFind(valueSEXP, [&](SEXP elSEXP)
   {
      return !isSerializableImpl(elSEXP);
   });
}

bool isGlobalEnvironmentSerializable()
{
   // Start building a new cache of serialized object state.
   SerializationCache newCache;
   
   // Flag tracking whether we found an object which cannot be serialized.
   bool allValuesSerializable = true;
   
   // Iterate over values in the global environment, and compute whether they can be serialized.
   SEXP hashTableSEXP = r::sexp::sxpinfo::getHashtab(R_GlobalEnv);
   R_xlen_t n = Rf_xlength(hashTableSEXP);
   for (R_xlen_t i = 0; i < n; i++)
   {
      SEXP frameSEXP = VECTOR_ELT(hashTableSEXP, i);
      for (; frameSEXP != R_NilValue; frameSEXP = CDR(frameSEXP))
      {
         // Compute whether the value can be serialized.
         // If we already have a cached value from a prior computation, use it.
         //
         // TODO: If an R environment is updated in-place with a value that
         // makes it no longer serializable, we would fail to detect this.
         // Is this okay? Or should we avoid caching results for environments?
         if (!r::internal::isImmediateBinding(frameSEXP))
         {
            SEXP valueSEXP = CAR(frameSEXP);
            bool canBeSerialized = s_serializationCache.contains(valueSEXP)
                  ? s_serializationCache.at(valueSEXP)
                  : isSerializable(valueSEXP);
            newCache[valueSEXP] = canBeSerialized;
            allValuesSerializable = allValuesSerializable && canBeSerialized;
         }
      }
   }
   
   // Set the new cache.
   s_serializationCache.clear();
   s_serializationCache = newCache;
   
   // Return true only if all values can be serialized.
   return allValuesSerializable;
}

bool isValidSrcref(SEXP srcref)
{
   return srcref && TYPEOF(srcref) != NILSXP && TYPEOF(srcref) != SYMSXP;
}

bool handleRBrowseEnv(const core::FilePath& filePath)
{
   if (filePath.getFilename() == "wsbrowser.html")
   {
      module_context::showContent("R objects", filePath);
      return true;
   }
   else
   {
      return false;
   }
}

// Does an object contain an ALTREP anywhere? ALTREP (alternative representation) objects often
// require special treatment.
SEXP rs_hasAltrep(SEXP obj)
{
   r::sexp::Protect protect;
   return r::sexp::create(hasAltrep(obj), &protect);
}

// Is an object an R ALTREP object? 
SEXP rs_isAltrep(SEXP obj)
{
   r::sexp::Protect protect;
   return r::sexp::create(isAltrep(obj), &protect);
}

// Check the package providing an ALTREP class definition.
//
// For an ALTREP object, the class information is stored as a TAG on the
// associated object. The attributes of that class contain metadata about
// the ALTREP class, where the second entry is the name of the package
// providing the class definition, as a symbol.
//
// https://github.com/wch/r-source/blob/e26e3f02a5e4255c4aad0842a46e141c03eed379/src/main/altrep.c#L38-L42
//
// TODO: There is no public R API for querying ALTREP class metadata.
// We use sxpinfo::getAttrib() to access the raw attribute pairlist
// directly. Replace this once a public API becomes available.
std::string altrepClassPackage(SEXP objectSEXP)
{
   SEXP altrepClassSEXP = TAG(objectSEXP);
   if (altrepClassSEXP == R_NilValue)
      return {};

   SEXP altrepAttribSEXP = r::sexp::sxpinfo::getAttrib(altrepClassSEXP);
   if (TYPEOF(altrepAttribSEXP) != LISTSXP || r::sexp::length(altrepAttribSEXP) < 2)
      return {};

   SEXP packageSEXP = CADR(altrepAttribSEXP);
   if (TYPEOF(packageSEXP) != SYMSXP)
      return {};

   return CHAR(PRINTNAME(packageSEXP));
}

SEXP rs_dim(SEXP objectSEXP)
{
   // For 'data.frame' objects, check the 'row.names' attribute
   if (Rf_inherits(objectSEXP, "data.frame"))
   {
      // default values for rows, columns
      int numRows = -1;
      int numCols = r::sexp::length(objectSEXP);

      SEXP rowNamesInfoSEXP = R_NilValue;
      r::sexp::Protect protect;

      Error error = r::exec::RFunction("base:::.row_names_info")
            .addParam(objectSEXP)
            .addParam(0)
            .call(&rowNamesInfoSEXP, &protect);
      if (error)
      {
         LOG_ERROR(error);
         return R_NilValue;
      }

      // Avoid materializing certain ALTREP representations.
      //
      // https://github.com/rstudio/rstudio/issues/13907
      // https://github.com/rstudio/rstudio/pull/13544
      bool canComputeRows = true;
      if (isAltrep(rowNamesInfoSEXP))
      {
         if (altrepClassPackage(rowNamesInfoSEXP) == "duckdb")
            canComputeRows = false;
      }
      
      // Detect compact row names.
      if (canComputeRows)
      {
         if (isCompactRowNames(rowNamesInfoSEXP))
         {
            numRows = abs(INTEGER(rowNamesInfoSEXP)[1]);
         }
         else
         {
            numRows = r::sexp::length(rowNamesInfoSEXP);
         }
      }
      
      SEXP resultSEXP = Rf_allocVector(INTSXP, 2);
      INTEGER(resultSEXP)[0] = numRows;
      INTEGER(resultSEXP)[1] = numCols;
      return resultSEXP;
   }
   
   // Otherwise, just call 'dim()' directly
   r::sexp::Protect protect;
   SEXP dimSEXP = R_NilValue;
   Error error = r::exec::RFunction("base:::dim")
         .addParam(objectSEXP)
         .call(&dimSEXP, &protect);
   if (error)
      LOG_ERROR(error);
   
   return dimSEXP;
}

SEXP rs_newTestExternalPointer(SEXP nullSEXP)
{
   bool nullPtr = r::sexp::asLogical(nullSEXP);
   return r::sexp::makeExternalPtr(nullPtr ? nullptr : R_EmptyEnv, R_NilValue, R_NilValue);
}

SEXP rs_isSerializable(SEXP valueSEXP)
{
   bool result = isSerializable(valueSEXP);
   return Rf_ScalarLogical(result);
}

// Result of call frame introspection from R
struct CallFrameResult
{
   // JSON array of frame descriptors for the client
   json::Array frames;

   // Context info at the requested depth (for further introspection)
   SEXP contextCallfun;      // callfun() at target depth
   SEXP contextCloenv;       // cloenv() at target depth
   std::string functionName; // function name at target depth
   SEXP originalCallfun;     // unwrapped (un-traced) callfun
   bool hasSourceRefs;       // whether the function has source refs
   SEXP callFunSourceRefs;   // srcref attribute on the original function

   // Source context info at target depth (for simulated srcrefs)
   SEXP srcContextCallfun;
   SEXP srcContextCall;

   CallFrameResult()
      : contextCallfun(R_NilValue),
        contextCloenv(R_NilValue),
        originalCallfun(R_NilValue),
        hasSourceRefs(false),
        callFunSourceRefs(R_NilValue),
        srcContextCallfun(R_NilValue),
        srcContextCall(R_NilValue)
   {
   }
};

// Call .rs.callFrames() in R and unpack the results.
CallFrameResult callFramesFromR(int depth,
                                LineDebugState* pLineDebugState,
                                r::sexp::Protect* pProtect)
{
   CallFrameResult result;

   // Build lineDebugState argument for R
   SEXP lineDebugStateSEXP = R_NilValue;
   if (pLineDebugState != nullptr)
   {
      r::sexp::ListBuilder builder(pProtect);
      builder.add("lastDebugText", pLineDebugState->lastDebugText);
      builder.add("lastDebugLine", pLineDebugState->lastDebugLine);
      lineDebugStateSEXP = r::sexp::create(builder, pProtect);
   }

   // Get the current srcref for the innermost context (set by R's evaluator
   // during debug stepping). This is not accessible from R's sys.*() functions,
   // so we pass it in.
   //
   // In R >= 4.5, skip=NA_INTEGER checks R_Srcref first, giving the correct
   // srcref for the current debug position. In R < 4.5, NA_INTEGER is not
   // handled specially, so we use skip=0 (checks R_Srcref, then walks
   // context stack).
   int skip = r::version() >= core::Version("4.5.0") ? NA_INTEGER : 0;
   SEXP currentSrcref = R_GetCurrentSrcref(skip);

   // Call .rs.callFrames(targetDepth, lineDebugState, currentSrcref)
   SEXP resultSEXP = R_NilValue;
   Error error = r::exec::RFunction(".rs.callFrames")
         .addParam(depth)
         .addParam(lineDebugStateSEXP)
         .addParam(currentSrcref)
         .call(&resultSEXP, pProtect);

   if (error)
   {
      LOG_ERROR(error);
      return result;
   }

   // Extract "frames" list and convert to JSON
   SEXP framesSEXP;
   error = r::sexp::getNamedListSEXP(resultSEXP, "frames", &framesSEXP);
   if (!error)
   {
      json::Value framesJson;
      error = r::json::jsonValueFromObject(framesSEXP, &framesJson);
      if (!error && framesJson.isArray())
         result.frames = framesJson.getArray();
      else if (error)
         LOG_ERROR(error);
   }

   // Extract context info at target depth
   SEXP contextSEXP;
   error = r::sexp::getNamedListSEXP(resultSEXP, "context", &contextSEXP);
   if (!error && contextSEXP != R_NilValue)
   {
      error = r::sexp::getNamedListSEXP(contextSEXP, "callfun", &result.contextCallfun);
      if (error) LOG_ERROR(error);
      error = r::sexp::getNamedListSEXP(contextSEXP, "cloenv", &result.contextCloenv);
      if (error) LOG_ERROR(error);
      error = r::sexp::getNamedListSEXP(contextSEXP, "originalCallfun", &result.originalCallfun);
      if (error) LOG_ERROR(error);
      error = r::sexp::getNamedListSEXP(contextSEXP, "callFunSourceRefs", &result.callFunSourceRefs);
      if (error) LOG_ERROR(error);

      error = r::sexp::getNamedListElement(contextSEXP, "functionName", &result.functionName);
      if (error) LOG_ERROR(error);
      error = r::sexp::getNamedListElement(contextSEXP, "hasSourceRefs", &result.hasSourceRefs);
      if (error) LOG_ERROR(error);
   }

   // Extract source context info
   SEXP srcContextSEXP;
   error = r::sexp::getNamedListSEXP(resultSEXP, "src_context", &srcContextSEXP);
   if (!error && srcContextSEXP != R_NilValue)
   {
      error = r::sexp::getNamedListSEXP(srcContextSEXP, "callfun", &result.srcContextCallfun);
      if (error) LOG_ERROR(error);
      error = r::sexp::getNamedListSEXP(srcContextSEXP, "call", &result.srcContextCall);
      if (error) LOG_ERROR(error);
   }

   // Propagate lastDebugLine update back to C++ (for simulated srcref state)
   if (pLineDebugState != nullptr)
   {
      int updatedLine = -1;
      error = r::sexp::getNamedListElement(resultSEXP, "lastDebugLine", &updatedLine);
      if (!error && updatedLine >= 0)
      {
         pLineDebugState->lastDebugLine = updatedLine;
      }
   }

   return result;
}

json::Array environmentListAsJson()
{
    using namespace rstudio::r::sexp;
    std::vector<std::string> names;
    json::Array listJson;

    if (s_pEnvironmentMonitor->hasEnvironment())
    {
       SEXP env = s_pEnvironmentMonitor->getMonitoredEnvironment();
       listEnvironment(env,
                       false,
                       prefs::userPrefs().showLastDotValue(),
                       &names);

       // get object details and transform to json
       std::transform(names.begin(),
                      names.end(),
                      std::back_inserter(listJson),
                      boost::bind(varToJson, _1, env));
    }

    return listJson;
}

Error listEnvironment(boost::shared_ptr<int> pContextDepth,
                      const json::JsonRpcRequest&,
                      json::JsonRpcResponse* pResponse)
{
   // return list
   pResponse->setResult(environmentListAsJson());
   return Success();
}

// Sets an environment by name. Used when the environment can be reliably
// identified by its name (e.g. package environments).
Error setEnvironmentName(int contextDepth,
                         SEXP contextEnv,
                         std::string environmentName)
{
   SEXP environment = R_GlobalEnv;
   if (environmentName == "R_GlobalEnv")
   {
      environment = R_GlobalEnv;
   }
   else if (environmentName == "base")
   {
      environment = R_BaseEnv;
   }
   else
   {
      r::sexp::Protect protect;
      // We need to traverse the search path manually looking for an environment
      // whose name matches the one the caller requested, because R's
      // as.environment() function only searches the global search path, and
      // we may wish to set an environment whose name only exists in a private
      // environment chain.
      //
      // This would be better wrapped in an R function, but this code may
      // run during session init when tools:rstudio isn't yet attached to the
      // search path.
      SEXP env = contextDepth > 0 && contextEnv != R_NilValue ?
                        contextEnv :
                        R_GlobalEnv;
      std::string candidateEnv;
      Error error;
      while (env != R_EmptyEnv)
      {
         error = r::exec::RFunction("environmentName", env).call(&candidateEnv);
         if (error)
            break;
         if (candidateEnv == environmentName)
         {
            environment = env;
            break;
         }
         // Proceed to the parent of the environment
         env = r::sexp::getParentEnv(env);
      }
      if (error || env == R_EmptyEnv)
      {
         s_pEnvironmentMonitor->setMonitoredEnvironment(R_GlobalEnv, true);
         return error;
      }
   }

   s_pEnvironmentMonitor->setMonitoredEnvironment(environment, true);
   return Success();
}

Error setEnvironment(boost::shared_ptr<int> pContextDepth,
                     boost::shared_ptr<SEXP> pCurrentEnv,
                     const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string environmentName;
   Error error = json::readParam(request.params, 0, &environmentName);
   if (error)
      return error;

   s_monitoredPythonModule = std::string();
   
   if (s_environmentLanguage == kEnvironmentLanguageR)
   {
      error = setEnvironmentName(*pContextDepth,
                                 *pCurrentEnv,
                                 environmentName);
   }
   else if (s_environmentLanguage == kEnvironmentLanguagePython)
   {
      if (environmentName != s_monitoredPythonModule)
      {
         s_monitoredPythonModule = environmentName;
         
         ClientEvent event(client_events::kEnvironmentRefresh);
         module_context::enqueClientEvent(event);
      }
   }
   else
   {
      LOG_WARNING_MESSAGE("Unexpected language '" + s_environmentLanguage + "'");
   }
   
   if (error)
      return error;

   persistentState().setActiveEnvironmentName(environmentName);
   return Success();
}

// Sets an environment by its frame number. Used for unnamed, transient
// function environments.
Error setEnvironmentFrame(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   int frameNumber = 0;
   Error error = json::readParam(request.params, 0, &frameNumber);
   if (error)
      return error;

   SEXP environment;
   r::sexp::Protect protect;
   error = r::exec::RFunction("sys.frame", frameNumber)
            .call(&environment, &protect);
   if (error)
      return error;

   s_pEnvironmentMonitor->setMonitoredEnvironment(environment, true);
   return Success();
}

// given a call frame result, indicate whether the copy of the source code
// for the function is different than the source code on disk.
bool functionIsOutOfSync(const CallFrameResult& cfResult,
                         std::string *pFunctionCode)
{
   Error error;
   r::sexp::Protect protect;
   SEXP sexpCode = R_NilValue;

   // start by extracting the source code from the call site
   error = r::exec::RFunction(".rs.deparseFunction")
         .addParam(cfResult.originalCallfun)
         .addParam(true)
         .addParam(true)
         .call(&sexpCode, &protect);

   if (error)
   {
      LOG_ERROR(error);
      return true;
   }

   error = r::sexp::extract(sexpCode, pFunctionCode, true);
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }

   // make sure the function has source references
   if (!cfResult.hasSourceRefs)
   {
      return true;
   }

   return functionDiffersFromSource(cfResult.callFunSourceRefs, *pFunctionCode);
}

// Returns a JSON array containing the names and associated call frame numbers
// of the current environment stack.
json::Value environmentNames(SEXP env)
{
   SEXP environments;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction(".rs.environmentList", env)
                                    .call(&environments, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return json::Array();
   }
   else
   {
      json::Value namesJson;
      error = r::json::jsonValueFromObject(environments, &namesJson);
      if (error)
      {
         LOG_ERROR(error);
         return json::Array();
      }
      return namesJson;
   }
}

json::Object pythonEnvironmentStateData(const std::string& environment)
{
   SEXP state = R_NilValue;
   r::sexp::Protect protect;
   Error error =
         r::exec::RFunction(".rs.reticulate.environmentState")
         .addParam(environment)
         .call(&state, &protect);
   
   if (error)
   {
      LOG_ERROR(error);
      return json::Object();
   }
   
   json::Object jsonState;
   error = r::json::jsonValueFromObject(state, &jsonState);
   if (error)
   {
      LOG_ERROR(error);
      return json::Object();
   }
   
   jsonState["environment_monitoring"] = s_monitoring;
   return jsonState;
}

// create a JSON object that contains information about the current environment;
// used both to initialize the environment state on first load and to send
// information about the new environment on a context change
json::Object commonEnvironmentStateData(
      bool isDebugStepping,
      int depth,
      bool includeContents,
      LineDebugState* pLineDebugState)
{
   bool hasCodeInFrame = false;
   json::Object varJson;
   bool useProvidedSource = false;
   std::string functionCode;
   bool inFunctionEnvironment = false;

   r::sexp::Protect protect;
   CallFrameResult cfResult = callFramesFromR(depth, pLineDebugState, &protect);

   // emit the current list of values in the environment, but only if not monitoring (as the intent
   // of the monitoring switch is to avoid implicit environment listing)
   varJson["environment_monitoring"] = s_monitoring;
   varJson["environment_list"] = includeContents ? environmentListAsJson() : json::Array();

   varJson["context_depth"] = depth;
   varJson["call_frames"] = cfResult.frames;
   varJson["function_name"] = "";

   // if we're in a debug context, add information about the function currently
   // being debugged
   if (depth > 0)
   {
      if (cfResult.contextCallfun != R_NilValue)
      {
         std::string functionName = cfResult.functionName;

         // If the environment currently monitored is the function's
         // environment, return that environment, unless the environment is the
         // global environment (which happens for source-equivalent functions).
         SEXP env = s_pEnvironmentMonitor->getMonitoredEnvironment();
         if (env != R_GlobalEnv && env == cfResult.contextCloenv)
         {
            varJson["environment_name"] = functionName + "()";

            std::string envLocation;
            Error error = r::exec::RFunction(".rs.environmentName")
                  .addParam(r::sexp::getParentEnv(cfResult.contextCloenv))
                  .call(&envLocation);

            if (error)
               LOG_ERROR(error);

            varJson["function_environment_name"] = envLocation;
            varJson["environment_is_local"] = true;
            inFunctionEnvironment = true;
         }

         // Check whether we already have code associated with this frame
         // from the call frames we previously queried.
         json::Value currentFrameJson = cfResult.frames.getValueAt(depth - 1);
         if (currentFrameJson.isObject())
         {
            std::string filename, lines;
            core::json::readObject(currentFrameJson.getObject(),
                     "file_name", filename,
                     "lines", lines);

            // TODO: Need to check if srcref code is in sync with file?
            if (!lines.empty())
            {
               hasCodeInFrame = true;
               useProvidedSource = filename.empty() || !module_context::resolveAliasedPath(filename).exists();
               functionCode = lines;
            }
         }

         if (!hasCodeInFrame)
         {
            // The eval and evalq functions receive special treatment since they
            // evaluate code from elsewhere (they don't have meaningful bodies we
            // can test here)
            if (functionName != "eval" && functionName != "evalq")
            {
               // see if the function to be debugged is out of sync with its saved
               // sources (if available).
               useProvidedSource =
                     functionIsOutOfSync(cfResult, &functionCode) &&
                     functionCode != "NULL";
            }
         }

         varJson["function_name"] = functionName;
      }
   }

   if (!inFunctionEnvironment)
   {
      // emit the name of the environment we're currently working with
      std::string environmentName;
      bool local = false;
      if (s_pEnvironmentMonitor->hasEnvironment())
      {
         Error error = r::exec::RFunction(".rs.environmentName",
                                    s_pEnvironmentMonitor->getMonitoredEnvironment())
                                    .call(&environmentName);
         if (error)
            LOG_ERROR(error);

         error = r::exec::RFunction(".rs.environmentIsLocal",
                                    s_pEnvironmentMonitor->getMonitoredEnvironment())
                                    .call(&local);
         if (error)
            LOG_ERROR(error);
      }
      varJson["environment_name"] = environmentName;
      varJson["environment_is_local"] = local;
   }
 
   // If we have source references while we're stepping through, then
   // we can accurately provide the current context even for the top-most frame.
   if (isDebugStepping && hasCodeInFrame)
      varJson["context_depth"] = 1;

   // always emit the code for the function, even if we don't think that the
   // client's going to need it. we only checked the saved copy of the function
   // above; the client may be aware of local/unsaved changes to the function,
   // in which case it will need to fall back on a server-provided copy.
   varJson["use_provided_source"] = useProvidedSource;
   varJson["function_code"] = functionCode;

   return varJson;
}

void enqueContextDepthChangedEvent(bool isDebugStepping,
                                   int depth,
                                   LineDebugState* pLineDebugState)
{
   // emit an event to the client indicating the new call frame and the
   // current state of the environment
   ClientEvent event(
            client_events::kContextDepthChanged,
            commonEnvironmentStateData(isDebugStepping, depth, s_monitoring,
                                       pLineDebugState));
   module_context::enqueClientEvent(event);
}

void enqueBrowserLineChangedEvent(const SEXP srcref)
{
   json::Object varJson;
   sourceRefToJson(srcref, &varJson);
   ClientEvent event(client_events::kBrowserLineChanged, varJson);
   module_context::enqueClientEvent(event);
}

Error setContextDepth(boost::shared_ptr<int> pContextDepth,
                      boost::shared_ptr<LineDebugState> pLineDebugState,
                      const json::JsonRpcRequest& request,
                      json::JsonRpcResponse*)
{
   // get the requested depth
   int requestedDepth;
   Error error = json::readParam(request.params, 0, &requestedDepth);
   if (error)
      return error;

   // set state for the new depth
   *pContextDepth = requestedDepth;
   SEXP env = R_GlobalEnv;
   r::session::getFunctionContext(requestedDepth, s_browserActive, nullptr, &env);
   s_pEnvironmentMonitor->setMonitoredEnvironment(env);

   // populate the new state on the client
   enqueContextDepthChangedEvent(false, *pContextDepth, pLineDebugState.get());

   return Success();
}

Error getEnvironmentState(boost::shared_ptr<int> pContextDepth,
                          boost::shared_ptr<LineDebugState> pLineDebugState,
                          const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   std::string language = kEnvironmentLanguageR;
   std::string environment = "R_GlobalEnv";
   Error error = json::readParams(request.params, &language, &environment);
   if (error)
      LOG_ERROR(error);
   
   json::Object jsonState;
   
   if (language == kEnvironmentLanguageR)
   {
      jsonState = commonEnvironmentStateData(
               false,
               *pContextDepth,
               true,
               pLineDebugState.get());
   }
   else if (language == kEnvironmentLanguagePython)
   {
      jsonState = pythonEnvironmentStateData(environment);
   }
   else
   {
      LOG_WARNING_MESSAGE("Unexpected language '" + language + "'");
   }
   
   pResponse->setResult(jsonState);
   return Success();
}

void onDetectChanges(module_context::ChangeSource /* source */)
{
   // Prevent recursive calls to this function
   DROP_RECURSIVE_CALLS;

   // Ignore if not monitoring
   if (!s_monitoring)
      return;

   // This operation may use the R runtime so don't run it if this RPC was run in the offline thread
   if (!core::thread::isMainThread())
      return;

   // Check for Python changes
   if (s_environmentLanguage == kEnvironmentLanguagePython &&
       !s_monitoredPythonModule.empty())
   {
      Error error =
            r::exec::RFunction(".rs.reticulate.detectChanges")
            .addParam(s_monitoredPythonModule)
            .call();
      
      if (error)
         LOG_ERROR(error);
   }
   
   // Check active environment for changes
   s_pEnvironmentMonitor->checkForChanges();
}

namespace {

SEXP inferDebugSrcrefs(
      int depth,
      boost::shared_ptr<LineDebugState> pLineDebugState)
{
   // R_GetCurrentSrcref(skip) is a public R API that returns the srcref
   // for the expression currently being evaluated during debugging.
   //
   // In R >= 4.5, skip=NA_INTEGER checks R_Srcref (the evaluator's current
   // position) first, giving the correct srcref during debug stepping.
   // In R < 4.5, skip=NA_INTEGER is not handled specially and always
   // returns R_NilValue, so we use skip=0 (checks R_Srcref, then walks
   // context stack) which is correct on older R.
   r::sexp::Protect protect;
   int skip = r::version() >= core::Version("4.5.0") ? NA_INTEGER : 0;
   SEXP srcref = R_GetCurrentSrcref(skip);
   if (isValidSrcref(srcref))
      return srcref;

   // Fall back to building call frames and simulating source refs
   CallFrameResult cfResult = callFramesFromR(depth, pLineDebugState.get(), &protect);

   // Use the source context's callfun to simulate source refs
   if (cfResult.srcContextCallfun != R_NilValue)
   {
      SEXP info = r::sexp::create("_rs_sourceinfo", &protect);
      r::sexp::setAttrib(info, "_rs_callfun", cfResult.srcContextCallfun);

      if (pLineDebugState)
      {
         SEXP lastDebugSEXP = r::sexp::create(pLineDebugState->lastDebugText, &protect);
         r::sexp::setAttrib(info, "_rs_calltext", lastDebugSEXP);

         SEXP lastLineSEXP = r::sexp::create(pLineDebugState->lastDebugLine, &protect);
         r::sexp::setAttrib(info, "_rs_lastline", lastLineSEXP);
      }

      srcref = R_NilValue;
      Error error = r::exec::RFunction(".rs.simulateSourceRefs", info)
            .call(&srcref, &protect);
      if (error)
         LOG_ERROR(error);
   }

   if (pLineDebugState && isValidSrcref(srcref))
   {
      int lastDebugLine = INTEGER(srcref)[0] - 1;
      pLineDebugState->lastDebugLine = lastDebugLine;
   }

   return srcref;
}

} // end anonymous namespace

void onConsolePrompt(boost::shared_ptr<int> pContextDepth,
                     boost::shared_ptr<LineDebugState> pLineDebugState,
                     boost::shared_ptr<bool> pCapturingDebugOutput,
                     boost::shared_ptr<SEXP> pCurrentEnv)
{
   // Prevent recursive calls to this function
   DROP_RECURSIVE_CALLS;
   std::lock_guard<std::recursive_mutex> guard(s_consoleMutex);
   
   int depth = 0;
   SEXP environmentTop = R_GlobalEnv;

   // End debug output capture every time a console prompt occurs
   *pCapturingDebugOutput = false;
   
   // Update session suspendable state
   s_isGlobalEnvironmentSerializable = isGlobalEnvironmentSerializable();

   // If we were debugging but there's no longer a browser on the context stack,
   // switch back to the top level; otherwise, examine the stack and find the
   // first function there running user code.
   s_browserActive = r::session::isBrowseActive();
   if (*pContextDepth > 0 && !s_browserActive)
   {
      s_browserEnv.set(R_NilValue);
   }
   else
   {
      // Find the function context associated with the browser
      r::session::getFunctionContext(0, s_browserActive, &depth, &environmentTop);
      s_browserEnv.set(environmentTop);
   }
   
   if (environmentTop != s_pEnvironmentMonitor->getMonitoredEnvironment() ||
       depth != *pContextDepth ||
       environmentTop != *pCurrentEnv)
   {
      // if we appear to be switching into debug mode, make sure there's a
      // browser call somewhere on the stack. if there isn't, then we're
      // probably just waiting for user input inside a function (e.g. scan());
      // assume the user isn't interested in seeing the function's internals.
      if (*pContextDepth == 0 && !s_browserActive)
      {
         return;
      }

      // if we're leaving debug mode, clear out the debug state to prepare
      // for the next debug session
      if (*pContextDepth > 0 && depth == 0)
      {
         pLineDebugState->reset();
      }

      // start monitoring the environment at the new depth
      s_pEnvironmentMonitor->setMonitoredEnvironment(environmentTop);
      *pContextDepth = depth;
      R_ReleaseObject(*pCurrentEnv);
      *pCurrentEnv = environmentTop;
      R_PreserveObject(*pCurrentEnv);
      enqueContextDepthChangedEvent(true, depth, pLineDebugState.get());
   }

   // if we're debugging and stayed in the same frame, update the line number
   else if (depth > 0 && !r::session::inDebugHiddenContext())
   {
      SEXP srcref = inferDebugSrcrefs(depth, pLineDebugState);
      enqueBrowserLineChangedEvent(srcref);
   }
   
}

void onBeforeExecute()
{
   // The client tracks busy state based on whether a console prompt has
   // been issued (because R doesn't reliably deliver non-busy state) --
   // i.e. when a console prompt occurs the client leaves busy state.
   // During debugging the busy state is therefore exited as soon as a
   // Browse> prompt is hit. This is often not a problem as the debug
   // stop command will interrupt R if necessary. However, in the case
   // where the Next or Continue command results in R running without
   // hitting another breakpoint we've essentially lost the busy state.
   //
   // This handler (which executes right before console input is returned
   // to R) checks whether we are in the Browser and if so re-raises the
   // busy event to indicate that R is now back in a busy state. The busy
   // state will be immediately cleared if another Browse> prompt is hit
   // however if R continues running then the client will properly restore
   // the state of the interruptR command

   s_browserActive = r::session::isBrowseActive();
   if (s_browserActive)
   {
      ClientEvent event(client_events::kBusy, true);
      module_context::enqueClientEvent(event);
   }
}

Error getEnvironmentNames(boost::shared_ptr<int> pContextDepth,
                          boost::shared_ptr<SEXP> pCurrentEnv,
                          const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   std::string language;
   Error error = json::readParams(request.params, &language);
   if (error)
   {
      LOG_ERROR(error);
      return Success();
   }
   
   if (language == kEnvironmentLanguagePython)
   {
      Error error;
      
      SEXP environments = R_NilValue;
      r::sexp::Protect protect;
      error = r::exec::RFunction(".rs.reticulate.listLoadedModules")
            .call(&environments, &protect);
      
      if (error)
      {
         LOG_ERROR(error);
         return Success();
      }
      
      json::Value environmentsJson;
      error = r::json::jsonValueFromObject(environments, &environmentsJson);
      if (error)
      {
         LOG_ERROR(error);
         return Success();
      }
      
      pResponse->setResult(environmentsJson);
      return Success();
   }
   else if (language == kEnvironmentLanguageR)
   {
      // If looking at a non-toplevel context, start from there; otherwise, start
      // from the global environment.
      SEXP env = *pContextDepth > 0 ?
               *pCurrentEnv :
               R_GlobalEnv;
      pResponse->setResult(environmentNames(env));
   }
   else
   {
      LOG_WARNING_MESSAGE("Unexpected language '" + language + "'");
   }
   
   return Success();
}

void initEnvironmentMonitoring()
{
   // Restore monitoring state
   s_monitoring = persistentState().environmentMonitoring();

   // Check to see whether we're actively debugging. If we are, the debug
   // environment trumps whatever the user wants to browse in at the top level.
   if (!r::session::isBrowseActive())
   {
      // Not actively debugging; see if we have a stored environment name to
      // begin monitoring.
      std::string envName = persistentState().activeEnvironmentName();
      if (!envName.empty())
      {
         // It's possible for this to fail if the environment we were
         // monitoring doesn't exist any more. If this is the case, reset
         // the monitor to the global environment.
         Error error = setEnvironmentName(0, R_GlobalEnv, envName);
         if (error)
         {
            persistentState().setActiveEnvironmentName("R_GlobalEnv");
         }
      }
   }
}

Error setEnvironmentMonitoring(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   bool monitoring = false;
   Error error = json::readParam(request.params, 0, &monitoring);
   if (error)
      return error;

   // save the user's requested monitoring state
   s_monitoring = monitoring;
   persistentState().setEnvironmentMonitoring(s_monitoring);

   return Success();
}

// Remove the given objects from the currently monitored environment.
Error removeObjects(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   json::Array objectNames;
   Error error = json::readParam(request.params, 0, &objectNames);
   if (error)
      return error;

   error = r::exec::RFunction(".rs.removeObjects")
         .addParam(objectNames)
         .addParam(s_pEnvironmentMonitor->getMonitoredEnvironment())
         .call();
   
   if (error)
      return error;

   return Success();
}

// Remove all the objects from the currently monitored environment.
Error removeAllObjects(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse*)
{
   bool includeHidden = false;
   Error error = json::readParam(request.params, 0, &includeHidden);
   if (error)
      return error;

   error = r::exec::RFunction(".rs.removeAllObjects")
         .addParam(includeHidden)
         .addParam(s_pEnvironmentMonitor->getMonitoredEnvironment())
         .call();
   
   if (error)
      return error;

   return Success();
}

// Return the contents of the given object. Called on-demand by the client when
// the object is large enough that we don't want to get its contents
// immediately (i.e. as part of environmentListAsJson)
Error getObjectContents(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)

{
   std::string objectName;
   Error error = json::readParam(request.params, 0, &objectName);
   if (error)
      return error;
   
   SEXP objContents;
   r::sexp::Protect protect;
   error = r::exec::RFunction(".rs.getObjectContents")
         .addParam(objectName)
         .addParam(s_pEnvironmentMonitor->getMonitoredEnvironment())
         .call(&objContents, &protect);
   
   if (error)
      return error;

   json::Value contents;
   error = r::json::jsonValueFromObject(objContents, &contents);
   if (error)
      return error;

   json::Object result;
   result["contents"] = contents;
   pResponse->setResult(result);
   return Success();
}

// Called by the client to force a re-query of the currently monitored
// context depth and environment.
Error requeryContext(boost::shared_ptr<int> pContextDepth,
                     boost::shared_ptr<LineDebugState> pLineDebugState,
                     boost::shared_ptr<SEXP> pCurrentEnv,
                     const json::JsonRpcRequest&,
                     json::JsonRpcResponse*)
{
   onConsolePrompt(
            pContextDepth,
            pLineDebugState,
            boost::make_shared<bool>(false),
            pCurrentEnv);
   
   return Success();
}

// Used by the client to inform the server what language is currently
// being displayed in the Environment pane.
Error environmentSetLanguage(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse*)
{
   std::string language = kEnvironmentLanguageR;
   Error error = json::readParams(request.params, &language);
   if (error)
      LOG_ERROR(error);
   
   s_environmentLanguage = language;
  
   // reset Python module to 'main' after changing language
   if (language == kEnvironmentLanguagePython)
   {
      s_monitoredPythonModule = "__main__";
   }
   
   return Success();
}

// Stores the last "debug: " R output. Used to reconstruct source references
// when unavailable (see simulatedSourceRefsOfContext).
void onConsoleOutput(boost::shared_ptr<LineDebugState> pLineDebugState,
                     boost::shared_ptr<bool> pCapturingDebugOutput,
                     module_context::ConsoleOutputType type,
                     const std::string& output)
{
   std::lock_guard<std::recursive_mutex> guard(s_consoleMutex);

   if (*pCapturingDebugOutput)
   {
      // stop capturing output if non-normal output occurs
      if (type != module_context::ConsoleOutputNormal)
      {
         *pCapturingDebugOutput = false;
         return;
      }
      
      // When printing things which are not symbols / calls in
      // the debugger, R will prepend a '[1] ' as these objects
      // will be printed in the "regular" way. Strip that off
      // if it is present.
      if (output.find("[1] ") == 0)
      {
         pLineDebugState->lastDebugText.append(output.substr(4));
      }
      else
      {
         pLineDebugState->lastDebugText.append(output);
      }
   }
   
   else if (type == module_context::ConsoleOutputNormal)
   {
      static boost::regex reDebugAtPosition("debug at ([^#]*+)#([^:]++): ");
      boost::smatch match;
      
      // start capturing debug output when R outputs "debug: "
      if (output == "debug: ")
      {
         *pCapturingDebugOutput = true;
         pLineDebugState->lastDebugText = "";
      }
      
      // emitted when browsing with srcref
      else if (boost::algorithm::starts_with(output, "debug at ") &&
               boost::regex_match(output, match, reDebugAtPosition))
      {
         std::string lineText = match[2];
         auto lineNumber = safe_convert::stringTo<int>(lineText);
         if (lineNumber)
         {
            *pCapturingDebugOutput = true;
            pLineDebugState->lastDebugText = "";
            pLineDebugState->lastDebugLine = *lineNumber;
         }
      }
      
      // emitted by R when a 'browser()' statement is encountered
      else if (boost::algorithm::starts_with(output, "Called from: "))
      {
         pLineDebugState->lastDebugLine = 0;
         pLineDebugState->lastDebugText = "browser()";
      }
   }
}


SEXP rs_jumpToFunction(SEXP file, SEXP line, SEXP col, SEXP moveCursor)
{
   json::Object funcLoc;
   FilePath path(r::sexp::safeAsString(file));
   funcLoc["file_name"] = module_context::createAliasedPath(path);
   funcLoc["line_number"] = r::sexp::asInteger(line);
   funcLoc["column_number"] = r::sexp::asInteger(col);
   funcLoc["move_cursor"] = r::sexp::asLogical(moveCursor);
   ClientEvent jumpEvent(client_events::kJumpToFunction, funcLoc);
   module_context::enqueClientEvent(jumpEvent);
   return R_NilValue;
}

} // anonymous namespace

json::Value environmentStateAsJson()
{
   if (s_environmentLanguage == kEnvironmentLanguagePython)
      return pythonEnvironmentStateData(s_monitoredPythonModule);
   
   int contextDepth = 0;
   bool browsing = r::session::isBrowseActive();
   r::session::getFunctionContext(0, browsing, &contextDepth, nullptr);

   // If there's no browser on the stack, stay at the top level even if
   // there are functions on the stack--this is not a user debug session.
   if (!browsing)
      contextDepth = 0;
   
   return commonEnvironmentStateData(
            false,
            contextDepth, 
            s_monitoring, // include contents if actively monitoring
            nullptr);
}

SEXP rs_isBrowserActive()
{
   r::sexp::Protect protect;
   return r::sexp::create(s_browserActive, &protect);
}

SEXP rs_getBrowserEnv()
{
   return s_browserEnv.get();
}

SEXP rs_setCapturedBrowserEnv(SEXP envSEXP)
{
   r::session::setBrowserEnv(envSEXP);
   return R_NilValue;
}

SEXP rs_dumpContexts()
{
   SEXP result = R_NilValue;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction("sys.status").call(&result, &protect);
   if (error)
      LOG_ERROR(error);
   return result;
}

bool isSuspendable()
{
   return s_isGlobalEnvironmentSerializable;
}

// Code within this namespace is borrowed from the lobstr package.
// See https://github.com/r-lib/lobstr for more details.
// Some patches are included to make this implementation conform
// with the base R implementation where appropriate.
namespace lobstr {

bool is_linked_list(SEXP x)
{
   switch (TYPEOF(x))
   {
   case DOTSXP:
   case LISTSXP:
   case LANGSXP:
      return true;
   default:
      return false;
   }
}

double v_size(double n, int element_size)
{
   if (n == 0)
      return 0;

   double vec_size = std::max(sizeof(SEXP), sizeof(double));
   double elements_per_byte = vec_size / element_size;
   double n_bytes = std::ceil(n / elements_per_byte);
   // Rcout << n << " elements, each of " << elements_per_byte << " = " <<
   //  n_bytes << "\n";

   double size = 0;
   // Big vectors always allocated in 8 byte chunks
   if (n_bytes > 16)
      size = n_bytes * 8;
   // For small vectors, round to sizes allocated in small vector pool
   else if (n_bytes > 8)
      size = 128;
   else if (n_bytes > 6)
      size = 64;
   else if (n_bytes > 4)
      size = 48;
   else if (n_bytes > 2)
      size = 32;
   else if (n_bytes > 1)
      size = 16;
   else if (n_bytes > 0)
      size = 8;

   // Size is pointer to struct + struct size
   return size;
}

bool is_namespace(SEXP envSEXP)
{
   if (envSEXP == R_BaseNamespace)
      return true;

   static SEXP nsSymSEXP = Rf_install(".__NAMESPACE__.");
   SEXP nsSEXP = r::sexp::findVarInFrame(envSEXP, nsSymSEXP);
   return nsSEXP != nullptr;
}

// R equivalent
// https://github.com/wch/r-source/blob/master/src/library/utils/src/size.c#L41

double obj_size_tree(SEXP x,
                     SEXP base_env,
                     int sizeof_node,
                     int sizeof_vector,
                     int depth);

double obj_size_attrib(SEXP x,
                       SEXP base_env,
                       int sizeof_node,
                       int sizeof_vector,
                       int depth)
{
   return obj_size_tree(r::sexp::sxpinfo::getAttrib(x), base_env, sizeof_node, sizeof_vector, depth);
}

double obj_size_tree(SEXP x,
                     SEXP base_env,
                     int sizeof_node,
                     int sizeof_vector,
                     int depth)
{
   // Rcout << "\n" << std::string(depth * 2, ' ');
   // Rprintf("type: %s", Rf_type2char(TYPEOF(x)));

   // Use sizeof(SEXPREC) and sizeof(VECTOR_SEXPREC) computed in R.
   // CHARSXP are treated as vectors for this purpose
   double size = 0;

   // Handle unexpected null pointers -- it seems like certain ALTREP objects
   // might encode them in certain cases?
   //
   // https://github.com/rstudio/rstudio/issues/16436
   if (x == nullptr)
      return 0;

   // Handle ALTREP objects
   if (r::sexp::isAltrep(x))
   {
      size += 3 * sizeof(SEXP);
      size += obj_size_tree(TAG(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      size += obj_size_tree(CAR(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      size += obj_size_tree(CDR(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      return size;
   }

   // Rprintf("type: %-10s size: %6.0f\n", Rf_type2char(TYPEOF(x)), size);

   switch (TYPEOF(x))
   {
   // Vectors
   // -------------------------------------------------------------------
   // See details in v_size()

   // Simple vectors
   case LGLSXP:
      size += sizeof_vector;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += v_size(XLENGTH(x), sizeof(int));
      break;

   case INTSXP:
      size += sizeof_vector;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += v_size(XLENGTH(x), sizeof(int));
      break;

   case REALSXP:
      size += sizeof_vector;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += v_size(XLENGTH(x), sizeof(double));
      break;

   case CPLXSXP:
      size += sizeof_vector;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += v_size(XLENGTH(x), sizeof(Rcomplex));
      break;

   case RAWSXP:
      size += sizeof_vector;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += v_size(XLENGTH(x), 1);
      break;

   // Strings
   case STRSXP:
   {
      // R ignores duplicates within the same string.
      std::set<SEXP> visited;

      size += sizeof_vector;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += v_size(XLENGTH(x), sizeof(SEXP));
      for (R_xlen_t i = 0; i < XLENGTH(x); i++)
      {
         SEXP eltSEXP = STRING_ELT(x, i);
         if (eltSEXP != NA_STRING && visited.count(eltSEXP) == 0)
         {
            size += obj_size_tree(eltSEXP, base_env, sizeof_node, sizeof_vector, depth + 1);
            visited.insert(eltSEXP);
         }
      }
      break;
   }

   case CHARSXP:
      size += sizeof_vector;
      size += v_size(LENGTH(x) + 1, 1);
      break;

   // Generic vectors
   case VECSXP:
   case EXPRSXP:
   case WEAKREFSXP:
      size += sizeof_vector;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += v_size(XLENGTH(x), sizeof(SEXP));
      for (R_xlen_t i = 0; i < XLENGTH(x); ++i)
      {
         size += obj_size_tree(VECTOR_ELT(x, i), base_env, sizeof_node, sizeof_vector, depth + 1);
      }
      break;

   // Nodes
   // ---------------------------------------------------------------------
   // https://github.com/wch/r-source/blob/master/src/include/Rinternals.h#L237-L249
   // All have enough space for three SEXP pointers

   // R treates 'NULL' as an object with 0 size
   case NILSXP:
      return 0;
   
   // Special objects
   case SPECIALSXP:
   case BUILTINSXP:
      size += sizeof_node;
      break;

   // Linked lists
   case LISTSXP:
   case LANGSXP:
   case DOTSXP:
      // NOTE: Certain R objects (seemingly, ALTREP objects?) may also place non-node
      // R objects within the CDR of a node here, so we need to validate we do indeed
      // still have a node while looping here.
      //
      // https://github.com/rstudio/rstudio/issues/16202
      for (; is_linked_list(x); x = CDR(x))
      {
         size += sizeof_node;
         size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
         size += obj_size_tree(TAG(x), base_env, sizeof_node, sizeof_vector, depth + 1);

         if (!r::internal::isImmediateBinding(x))
            size += obj_size_tree(CAR(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      }
      break;

   case BCODESXP:
      size += sizeof_node;
      size += sizeof_node;  // ?
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += obj_size_tree(TAG(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      size += obj_size_tree(CAR(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      size += obj_size_tree(CDR(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      break;

   // Environments
   case ENVSXP:
      size += sizeof_node;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      // size += obj_size_tree(r::sexp::sxpinfo::getFrame(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      // size += obj_size_tree(r::sexp::getParentEnv(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      // size += obj_size_tree(r::sexp::sxpinfo::getHashtab(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      break;

   // Functions
   case CLOSXP:
      size += sizeof_node;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += obj_size_tree(R_ClosureFormals(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      size += obj_size_tree(R_ClosureBody(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      // size += obj_size_tree(R_ClosureEnv(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      break;

   case PROMSXP:
      size += sizeof_node;
      // size += obj_size_tree(PRVALUE(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      // size += obj_size_tree(PRCODE(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      // size += obj_size_tree(PRENV(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      break;

   case EXTPTRSXP:
      size += sizeof_node;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += obj_size_tree(TAG(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      size += sizeof(void*); // the actual pointer; lives in the CAR of the node
      size += obj_size_tree(CDR(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      break;

   case S4SXP:
      size += sizeof_node;
      size += obj_size_attrib(x, base_env, sizeof_node, sizeof_vector, depth + 1);
      size += obj_size_tree(TAG(x), base_env, sizeof_node, sizeof_vector, depth + 1);
      break;

   case SYMSXP:
      size += sizeof_node;
      break;

   default:
      break;
   }

   return size;
}

} // end namespace lobstr

int computeSize(SEXP objectSEXP)
{
   double size = 0;
   Error error = r::exec::RFunction("utils:::object.size")
      .addParam(objectSEXP)
      .call(&size);

   if (error)
      LOG_ERROR(error);

   return static_cast<int>(size);
}

int computeNodeSize()
{
   int size = 0;
   Error error = r::exec::RFunction(".rs.computeNodeSize").call(&size);
   if (error)
      LOG_ERROR(error);

   return size;
}

int computeVectorSize()
{
   int size = 0;
   Error error = r::exec::RFunction(".rs.computeVectorSize").call(&size);
   if (error)
      LOG_ERROR(error);

   return size;
}

SEXP rs_objectSize(SEXP objectSEXP,
                   SEXP envirSEXP)
{
   static int nodeSize = computeNodeSize();
   static int vectorSize = computeVectorSize();
   double size = lobstr::obj_size_tree(objectSEXP, envirSEXP, nodeSize, vectorSize, 0);

   r::sexp::Protect protect;
   return r::sexp::create(size, &protect);
}

SEXP rs_functionBody(SEXP functionSEXP)
{
   return R_ClosureBody(functionSEXP);
}

Error initialize()
{
   // store on the heap so that the destructor is never called (so we
   // don't end up releasing the underlying environment SEXP after
   // R has already shut down / deinitialized)
   s_pEnvironmentMonitor = new EnvironmentMonitor();

   boost::shared_ptr<int> pContextDepth =
         boost::make_shared<int>(0);
   
   boost::shared_ptr<SEXP> pCurrentEnv =
         boost::make_shared<SEXP>(R_GlobalEnv);
   R_PreserveObject(*pCurrentEnv);
   
   // get reference to INTEGER_OR_NULL if provided by this version of R
   {
      using core::system::Library;
      Library rLibrary(kLibraryName);
      core::system::loadSymbol(rLibrary, "INTEGER_OR_NULL", (void**) &INTEGER_OR_NULL);
   }

   // functions that emit call frames also emit source references; these
   // values capture and supply the currently executing expression emitted by R
   // for the purpose of reconstructing references when none are present.
   boost::shared_ptr<LineDebugState> pLineDebugState =
         boost::make_shared<LineDebugState>();
   
   boost::shared_ptr<bool> pCapturingDebugOutput =
         boost::make_shared<bool>(false);

   RS_REGISTER_CALL_METHOD(rs_isBrowserActive);
   RS_REGISTER_CALL_METHOD(rs_getBrowserEnv);
   RS_REGISTER_CALL_METHOD(rs_setCapturedBrowserEnv);
   RS_REGISTER_CALL_METHOD(rs_jumpToFunction);
   RS_REGISTER_CALL_METHOD(rs_hasAltrep);
   RS_REGISTER_CALL_METHOD(rs_isAltrep);
   RS_REGISTER_CALL_METHOD(rs_dim);
   RS_REGISTER_CALL_METHOD(rs_dumpContexts);
   RS_REGISTER_CALL_METHOD(rs_newTestExternalPointer);
   RS_REGISTER_CALL_METHOD(rs_isSerializable);
   RS_REGISTER_CALL_METHOD(rs_objectSize);
   RS_REGISTER_CALL_METHOD(rs_functionBody);

   // subscribe to events
   using boost::bind;
   using namespace session::module_context;
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   events().onConsolePrompt.connect(bind(onConsolePrompt,
                                         pContextDepth,
                                         pLineDebugState,
                                         pCapturingDebugOutput,
                                         pCurrentEnv));
   events().onBeforeExecute.connect(onBeforeExecute);
   events().onConsoleOutput.connect(bind(onConsoleOutput,
                                         pLineDebugState,
                                         pCapturingDebugOutput, _1, _2));

   json::JsonRpcFunction listEnv =
         boost::bind(listEnvironment, pContextDepth, _1, _2);
   json::JsonRpcFunction setCtxDepth =
         boost::bind(setContextDepth, pContextDepth, pLineDebugState,
                     _1, _2);
   json::JsonRpcFunction getEnv =
         boost::bind(getEnvironmentState, pContextDepth, pLineDebugState,
                     _1, _2);
   json::JsonRpcFunction getEnvNames =
         boost::bind(getEnvironmentNames, pContextDepth, pCurrentEnv,
                     _1, _2);
   json::JsonRpcFunction setEnvName =
         boost::bind(setEnvironment, pContextDepth, pCurrentEnv,
                     _1, _2);
   json::JsonRpcFunction requeryCtx =
         boost::bind(requeryContext, pContextDepth, pLineDebugState,
                     pCurrentEnv, _1, _2);

   initEnvironmentMonitoring();

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRBrowseFileHandler, handleRBrowseEnv))
      (bind(registerRpcMethod, "list_environment", listEnv))
      (bind(registerRpcMethod, "set_context_depth", setCtxDepth))
      (bind(registerRpcMethod, "set_environment", setEnvName))
      (bind(registerRpcMethod, "set_environment_frame", setEnvironmentFrame))
      (bind(registerRpcMethod, "get_environment_names", getEnvNames))
      (bind(registerRpcMethod, "remove_objects", removeObjects))
      (bind(registerRpcMethod, "remove_all_objects", removeAllObjects))
      (bind(registerRpcMethod, "get_environment_state", getEnv))
      (bind(registerRpcMethod, "get_object_contents", getObjectContents))
      (bind(registerRpcMethod, "requery_context", requeryCtx))
      (bind(registerRpcMethod, "environment_set_language", environmentSetLanguage))
      (bind(registerRpcMethod, "set_environment_monitoring", setEnvironmentMonitoring))
      (bind(sourceModuleRFile, "SessionEnvironment.R"));

   return initBlock.execute();
}
   
} // namespace environment
} // namespace modules
} // namespace session
} // namespace rstudio

