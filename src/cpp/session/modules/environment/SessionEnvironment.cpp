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

#include <r/RCntxt.hpp>
#include <r/RCntxtUtils.hpp>
#include <r/RExec.hpp>
#include <r/RHelpers.hpp>
#include <r/RInterface.hpp>
#include <r/RJson.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>
#include <r/RSxpInfo.hpp>
#include <r/RUtil.hpp>
#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "EnvironmentUtils.hpp"

#include "../../SessionConsoleInput.hpp"

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
   SEXP hashTableSEXP = HASHTAB(R_GlobalEnv);
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

bool hasExternalPointer(SEXP obj, bool nullPtr, std::set<SEXP>& visited);

bool pairlistHasExternalPointer(SEXP list, bool nullPtr, std::set<SEXP>& visited)
{
   if (hasExternalPointer(CAR(list), nullPtr, visited))
      return true;

   if (hasExternalPointer(CDR(list), nullPtr, visited))
      return true;

   return false;
}

bool listHasExternalPointer(SEXP obj, bool nullPtr, std::set<SEXP>& visited)
{
   R_xlen_t n = XLENGTH(obj);
   for (R_xlen_t i = 0; i < n; i++)
   {
      if (hasExternalPointer(VECTOR_ELT(obj, i), nullPtr, visited))
         return true;
   }
   return false;
}

bool frameBindingIsActive(SEXP binding)
{
   static unsigned int ACTIVE_BINDING_MASK = (1<<15);
   return reinterpret_cast<r::sxpinfo*>(binding)->gp & ACTIVE_BINDING_MASK;
}

bool frameBindingHasExternalPointer(SEXP b, bool nullPtr, std::set<SEXP>& visited)
{
   if (frameBindingIsActive(b))
      return false;

   // ->extra is only used for immediate bindings: this needs special care
   // before we call CAR() because it might error with "bad binding access":
   // from Rinlinedfuns.h :
   //
   //     INLINE_FUN SEXP CAR(SEXP e)
   //     {
   //        if (BNDCELL_TAG(e))
   //        error("bad binding access");
   //        return CAR0(e);
   //     }
   unsigned int typetag = reinterpret_cast<r::sxpinfo*>(b)->extra;
   if (typetag)
   {
      // it should not be set on 32-bits: unset it
      if (sizeof(size_t) < sizeof(double))
      {
         reinterpret_cast<r::sxpinfo*>(b)->extra = 0;
      }
      else
      {
         switch(typetag) {
            case INTSXP:
            case REALSXP:
            case LGLSXP:
               // this is an immediate binding, R_expand_binding_value() would expand to a scalar
               return false;

            default:
               // otherwise (not sure this even hapens), ->extra should not be set: unset it
               reinterpret_cast<r::sxpinfo*>(b)->extra = 0;
         }
      }
   }

   // now safe to test the value in CAR()
   return hasExternalPointer(CAR(b), nullPtr, visited);
}

bool frameHasExternalPointer(SEXP frame, bool nullPtr, std::set<SEXP>& visited)
{
   while(frame != R_NilValue)
   {
      if (frameBindingHasExternalPointer(frame, nullPtr, visited))
         return true;

      frame = CDR(frame);
   }

   return false;
}

bool envHasExternalPointer(SEXP obj, bool nullPtr, std::set<SEXP>& visited)
{
   SEXP hash = HASHTAB(obj);
   if (hash == R_NilValue)
      return frameHasExternalPointer(FRAME(obj), nullPtr, visited);

   R_xlen_t n = XLENGTH(hash);
   for (R_xlen_t i = 0; i < n; i++)
   {
      if (frameHasExternalPointer(VECTOR_ELT(hash, i), nullPtr, visited))
         return true;
   }
   return false;
}

bool weakrefHasExternalPointer(SEXP obj, bool nullPtr, std::set<SEXP>& visited)
{
   SEXP key = r::sexp::getWeakRefKey(obj);
   if (key != R_NilValue)
   {
      if (hasExternalPointer(key, nullPtr, visited))
         return true;

      // only consider the value if the key is not NULL
      if (hasExternalPointer(r::sexp::getWeakRefValue(obj), nullPtr, visited))
         return true;
   }

   return false;
}

bool altrepHasExternalPointer(SEXP obj, bool nullPtr, std::set<SEXP>& visited)
{
   if (hasExternalPointer(CAR(obj), nullPtr, visited))
      return true;

   if (hasExternalPointer(CDR(obj), nullPtr, visited))
      return true;

   return false;
}

bool hasExternalPointer(SEXP obj, bool nullPtr, std::set<SEXP>& visited)
{
   if (obj == nullptr || obj == R_NilValue || visited.count(obj))
      return false;

   // mark SEXP as visited
   visited.insert(obj);

   // check if this is an external pointer
   if (r::sexp::isExternalPointer(obj))
   {
      // NOTE: this includes UserDefinedDatabase, aka
      //       external pointers to R_ObjectTable

      // when nullPtr is true, only return true for null pointer xp
      // otherwise only return true for non null pointer xp
      if (nullPtr == (r::sexp::getExternalPtrAddr(obj) == nullptr))
         return true;

      if (hasExternalPointer(r::sexp::getExternalPtrProtected(obj), nullPtr, visited))
         return true;

      if (hasExternalPointer(r::sexp::getExternalPtrTag(obj), nullPtr, visited))
         return true;
   }

   switch(TYPEOF(obj))
   {
      case SYMSXP:
         return false;

      case ENVSXP:
      {
         if (envHasExternalPointer(obj, nullPtr, visited))
            return true;
         break;
      }
      case VECSXP:
      case EXPRSXP:
      {
         if (listHasExternalPointer(obj, nullPtr, visited))
            return true;
         break;
      }

      case LISTSXP:
      case LANGSXP:
      {
         if (pairlistHasExternalPointer(obj, nullPtr, visited))
            return true;
         break;
      }

      case WEAKREFSXP:
      {
         if (weakrefHasExternalPointer(obj, nullPtr, visited))
            return true;

         break;
      }
      case PROMSXP:
      {
         SEXP value = PRVALUE(obj);
         if (value != R_UnboundValue)
         {
            if (hasExternalPointer(value, nullPtr, visited))
               return true;
         }
         else
         {
            if (hasExternalPointer(PRCODE(obj), nullPtr, visited))
               return true;

            if (hasExternalPointer(PRENV(obj), nullPtr, visited))
               return true;

            return false;
         }
         break;
      }
      case CLOSXP:
      {
         if (hasExternalPointer(FORMALS(obj), nullPtr, visited))
            return true;

         if (hasExternalPointer(BODY(obj), nullPtr, visited))
            return true;

         if (hasExternalPointer(CLOENV(obj), nullPtr, visited))
            return true;
      }
      default:
         break;
   }

   // altrep objects use ATTRIB() to hold class info, so no need
   // to check ATTRIB() on them, but altrepHasExternalPointer()
   // checks for their data1 and data2, aka CAR() and CDR()
   if (isAltrep(obj))
      return altrepHasExternalPointer(obj, nullPtr, visited);

   // check attributes, this includes slots for S4 objects
   if (hasExternalPointer(ATTRIB(obj), nullPtr, visited))
      return true;

   return false;
}

bool hasExternalPtr(SEXP obj,      // environment to search for external pointers
                    bool nullPtr)  // whether to look for NULL pointers
{
   std::set<SEXP> visited;
   return hasExternalPointer(obj, nullPtr, visited);
}

SEXP rs_hasExternalPointer(SEXP objSEXP, SEXP nullSEXP)
{
   r::sexp::Protect protect;

   bool nullPtr = r::sexp::asLogical(nullSEXP);
   return r::sexp::create(hasExternalPtr(objSEXP, nullPtr), &protect);
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
         // This code makes use of some internal details about ALTREP class metadata.
         // In particular, for an ALTREP object, the class information is stored as
         // a raw vector as a TAG on the associated object. The attributes of that
         // class give some metadata information about the ALTREP class.
         //
         // https://github.com/wch/r-source/blob/e26e3f02a5e4255c4aad0842a46e141c03eed379/src/main/altrep.c#L38-L42
         //
         // The second entry in the table is the name of the package providing the
         // ALTREP class definition, as a symbol.
         SEXP altrepClassSEXP = TAG(rowNamesInfoSEXP);
         SEXP altrepAttribSEXP = ATTRIB(altrepClassSEXP);
         if (TYPEOF(altrepAttribSEXP) == LISTSXP && r::sexp::length(altrepAttribSEXP) >= 2)
         {
            SEXP packageSEXP = CADR(altrepAttribSEXP);
            if (packageSEXP == Rf_install("duckdb"))
               canComputeRows = false;
         }
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

// Construct a simulated source reference from a context containing a
// function being debugged, and either the context containing the current
// invocation or a string containing the last debug output from R.
// We use this to highlight portions of deparsed functions when visually
// stepping through code for which source references are unavailable.
SEXP simulatedSourceRefsOfContext(const r::context::RCntxt& context,
                                  const r::context::RCntxt& lineContext,
                                  const LineDebugState* pLineDebugState)
{
   SEXP simulatedSrcref = R_NilValue;
   r::sexp::Protect protect;

   // The objects we will later transmit to .rs.simulateSourceRefs below
   // include language objects that we need to protect from early evaluation.
   // Attach them to a carrier SEXP as attributes rather than passing directly.
   SEXP info = r::sexp::create("_rs_sourceinfo", &protect);
   r::sexp::setAttrib(info, "_rs_callfun", context.callfun());

   if (lineContext)
   {
      r::sexp::setAttrib(info, "_rs_callobj", lineContext.call());
   }
   else if (pLineDebugState != nullptr)
   {
      SEXP lastDebugSEXP = r::sexp::create(pLineDebugState->lastDebugText, &protect);
      r::sexp::setAttrib(info, "_rs_calltext", lastDebugSEXP);

      SEXP lastLineSEXP = r::sexp::create(pLineDebugState->lastDebugLine, &protect);
      r::sexp::setAttrib(info, "_rs_lastline", lastLineSEXP);
   }

   Error error = r::exec::RFunction(".rs.simulateSourceRefs", info)
         .call(&simulatedSrcref, &protect);
   if (error)
      LOG_ERROR(error);
   return simulatedSrcref;
}

// Return the call frames and debug information as a JSON object.
json::Array callFramesAsJson(
      int depth,
      r::context::RCntxt* pContext,
      r::context::RCntxt* pSrcContext,
      LineDebugState* pLineDebugState)
{
   Error error;
   using namespace r::context;

   RCntxt prevContext;
   RCntxt srcContext = globalContext();
   json::Array listFrames;
   int contextDepth = 0;
   std::map<SEXP, RCntxt> envSrcrefCtx;

   // We want to treat the function associated with the top-level
   // browser context specially. This allows us to do so.
   enum BrowseContextState {
      BrowserContextNone,
      BrowserContextFound,
      BrowserContextUsed,
   };

   SEXP browserCloenv = R_NilValue;
   BrowseContextState browserContextState = BrowserContextNone;

   // map source contexts to closures
   for (auto context = RCntxt::begin(); context != RCntxt::end(); context++)
   {
      bool isFunctionContext = (context->callflag() & (CTXT_FUNCTION | CTXT_BROWSER));
      if (!isFunctionContext)
         continue;

      // if this context has a valid srcref, use it to supply the srcrefs for
      // debugging in the environment of the callee. note that there may be
      // multiple srcrefs on the stack for a given closure; in this case we
      // always want to take the first one as it's the most current/specific.
      SEXP srcref = context->contextSourceRefs();
      if (!isValidSrcref(srcref))
         continue;

      RCntxt nextContext = context->nextcontext();
      if (nextContext.isNull())
         continue;

      SEXP cloenv = context->nextcontext().cloenv();
      if (cloenv == R_NilValue)
         continue;

      if (envSrcrefCtx.find(cloenv) != envSrcrefCtx.end())
         continue;

      envSrcrefCtx[cloenv] = *context;
   }

   for (auto context = RCntxt::begin(); context != r::context::RCntxt::end(); context++)
   {
      if (browserContextState == BrowserContextNone)
      {
         if (context->callflag() & CTXT_BROWSER)
         {
            browserCloenv = context->cloenv();
            browserContextState = BrowserContextFound;
         }
      }

      if (context->callflag() & CTXT_FUNCTION)
      {
         json::Object varFrame;
         std::string functionName;
         varFrame["context_depth"] = ++contextDepth;

         error = context->functionName(&functionName);
         if (error)
            LOG_ERROR(error);

         varFrame["function_name"] = functionName;
         varFrame["is_error_handler"] = context->isErrorHandler();
         varFrame["is_hidden"] = context->isDebugHidden();

         // attempt to find the refs for the source that invoked this function;
         // use our own refs otherwise
         if (context->cloenv() != R_NilValue)
         {
            auto srcCtx = envSrcrefCtx.find(context->cloenv());
            if (srcCtx != envSrcrefCtx.end())
               srcContext = srcCtx->second;
            else
               srcContext = *context;
         }
         else
         {
            srcContext = *context;
         }

         SEXP srcref = srcContext.contextSourceRefs();

         // mark this as a source-equivalent function if it's evaluating user
         // code into the global environment
         varFrame["is_source_equiv"] =
               context->cloenv() == R_GlobalEnv &&
               isValidSrcref(srcref);

         std::string filename;
         error = srcContext.fileName(&filename);
         if (error)
            LOG_ERROR(error);

         varFrame["file_name"] = filename;
         varFrame["aliased_file_name"] =
               module_context::createAliasedPath(FilePath(filename));

         if (isValidSrcref(srcref))
         {
            varFrame["real_sourceref"] = true;
            sourceRefToJson(srcref, &varFrame);

            std::string lines;
            Error error = r::exec::RFunction(".rs.readSrcrefLines")
                  .addParam(srcref)
                  .addParam(true)
                  .call(&lines);

            if (error)
               LOG_ERROR(error);

            varFrame["lines"] = lines;
         }
         else
         {
            varFrame["real_sourceref"] = false;

            // if this frame is being debugged, we simulate the sourceref
            // using the output of the last debugged statement; if it isn't,
            // we construct it by deparsing calls in the context stack.
            SEXP simulatedSrcref;
            if (browserContextState == BrowserContextFound &&
                browserCloenv == context->cloenv())
            {
               browserContextState = BrowserContextUsed;
               simulatedSrcref =
                     simulatedSourceRefsOfContext(
                        *context, RCntxt(), pLineDebugState);

               if (isValidSrcref(simulatedSrcref))
               {
                  int lastDebugLine = INTEGER(simulatedSrcref)[0] - 1;
                  pLineDebugState->lastDebugLine = lastDebugLine;
               }
            }
            else
            {
               simulatedSrcref =
                     simulatedSourceRefsOfContext(
                        *context, prevContext, nullptr);
            }

            sourceRefToJson(simulatedSrcref, &varFrame);
         }

         varFrame["function_line_number"] = 1;

         std::string callSummary;
         error = context->callSummary(&callSummary);
         if (error)
            LOG_ERROR(error);

         varFrame["call_summary"] = error ? "" : callSummary;

         // If this is a Shiny function, provide its label
         varFrame["shiny_function_label"] = context->shinyFunctionLabel();

         if (depth == contextDepth)
         {
            *pContext = *context;
            *pSrcContext = srcContext;
         }

         listFrames.push_back(varFrame);
         prevContext = *context;
      }
   }

   return listFrames;
}

json::Array environmentListAsJson()
{
    using namespace rstudio::r::sexp;
    Protect rProtect;
    std::vector<Variable> vars;
    json::Array listJson;

    if (s_pEnvironmentMonitor->hasEnvironment())
    {
       SEXP env = s_pEnvironmentMonitor->getMonitoredEnvironment();
       listEnvironment(env,
                       false,
                       prefs::userPrefs().showLastDotValue(),
                       &vars);

       // get object details and transform to json
       std::transform(vars.begin(),
                      vars.end(),
                      std::back_inserter(listJson),
                      boost::bind(varToJson, env, _1));
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
                         const r::context::RCntxt &context,
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
      SEXP env = contextDepth > 0 && context ?
                        context.cloenv() :
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
         env = ENCLOS(env);
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
                     boost::shared_ptr<r::context::RCntxt> pCurrentContext,
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
                                 *pCurrentContext,
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

// given a function context, indicate whether the copy of the source code
// for the function is different than the source code on disk.
bool functionIsOutOfSync(const r::context::RCntxt& context,
                         std::string *pFunctionCode)
{
   Error error;
   r::sexp::Protect protect;
   SEXP sexpCode = R_NilValue;

   // start by extracting the source code from the call site
   error = r::exec::RFunction(".rs.deparseFunction")
         .addParam(context.originalFunctionCall())
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
   if (!context.hasSourceRefs())
   {
      return true;
   }

   return functionDiffersFromSource(context.callFunSourceRefs(), *pFunctionCode);
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
   static json::Object varJson;

   // return the last known value if the session is busy to avoid touching the R runtime
   if (console_input::executing())
      return varJson;

   bool hasCodeInFrame = false;
   bool useProvidedSource = false;
   std::string functionCode;
   bool inFunctionEnvironment = false;

   r::context::RCntxt context;
   r::context::RCntxt srcContext;
   json::Array callFramesJson = callFramesAsJson(depth, &context, &srcContext, pLineDebugState);

   // emit the current list of values in the environment, but only if not monitoring (as the intent
   // of the monitoring switch is to avoid implicit environment listing)
   varJson["environment_monitoring"] = s_monitoring;
   varJson["environment_list"] = includeContents ? environmentListAsJson() : json::Array();

   varJson["context_depth"] = depth;
   varJson["call_frames"] = callFramesJson;
   varJson["function_name"] = "";

   // if we're in a debug context, add information about the function currently
   // being debugged
   if (depth > 0)
   {
      if (!context.isNull())
      {
         std::string functionName;
         Error error = context.functionName(&functionName);
         if (error)
            LOG_ERROR(error);

         // If the environment currently monitored is the function's
         // environment, return that environment, unless the environment is the
         // global environment (which happens for source-equivalent functions).
         SEXP env = s_pEnvironmentMonitor->getMonitoredEnvironment();
         if (env != R_GlobalEnv && env == context.cloenv())
         {
            varJson["environment_name"] = functionName + "()";

            std::string envLocation;
            error = r::exec::RFunction(".rs.environmentName")
                  .addParam(ENCLOS(context.cloenv()))
                  .call(&envLocation);

            if (error)
               LOG_ERROR(error);

            varJson["function_environment_name"] = envLocation;
            varJson["environment_is_local"] = true;
            inFunctionEnvironment = true;
         }

         // Check whether we already have code associated with this frame
         // from the call frames we previously queried.
         json::Value currentFrameJson = callFramesJson.getValueAt(depth - 1);
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
                     functionIsOutOfSync(context, &functionCode) &&
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
            commonEnvironmentStateData(isDebugStepping, depth, s_monitoring, pLineDebugState));
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
   SEXP env = nullptr;
   r::context::getFunctionContext(requestedDepth, nullptr, &env);
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
   using namespace r::context;

   // check to see if we have real source references for the currently
   // executing context
   SEXP srcref = r::context::globalContext().srcref();
   if (isValidSrcref(srcref))
      return srcref;

   r::context::RCntxt context;
   r::context::RCntxt srcContext;
   json::Array callFramesJson = callFramesAsJson(
            depth,
            &context,
            &srcContext,
            pLineDebugState.get());

   srcref = simulatedSourceRefsOfContext(srcContext, RCntxt(), pLineDebugState.get());
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
                     boost::shared_ptr<r::context::RCntxt> pCurrentContext)
{
   // Prevent recursive calls to this function
   DROP_RECURSIVE_CALLS;


   int depth = 0;
   SEXP environmentTop = nullptr;
   r::context::RCntxt context;

   // End debug output capture every time a console prompt occurs
   *pCapturingDebugOutput = false;

   // Update session suspendable state
   s_isGlobalEnvironmentSerializable = isGlobalEnvironmentSerializable();

   // If we were debugging but there's no longer a browser on the context stack,
   // switch back to the top level; otherwise, examine the stack and find the
   // first function there running user code.
   s_browserActive = r::context::inBrowseContext();
   if (*pContextDepth > 0 && !s_browserActive)
   {
      context = r::context::globalContext();
      environmentTop = R_GlobalEnv;
   }
   else
   {
      // If we're not currently debugging, look for user code (we prefer to
      // show the user their own code on entering debug), but once debugging,
      // allow the user to explore other code.
      context = r::context::getFunctionContext(BROWSER_FUNCTION, &depth, &environmentTop);
   }

   if (environmentTop != s_pEnvironmentMonitor->getMonitoredEnvironment() ||
       depth != *pContextDepth ||
       context != *pCurrentContext)
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
      *pCurrentContext = context;
      enqueContextDepthChangedEvent(true, depth, pLineDebugState.get());
   }

   // if we're debugging and stayed in the same frame, update the line number
   else if (depth > 0 && !r::context::inDebugHiddenContext())
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

   s_browserActive = r::context::inBrowseContext();
   if (s_browserActive)
   {
      ClientEvent event(client_events::kBusy, true);
      module_context::enqueClientEvent(event);
   }
}

Error getEnvironmentNames(boost::shared_ptr<int> pContextDepth,
                          boost::shared_ptr<r::context::RCntxt> pCurrentContext,
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
               pCurrentContext->cloenv() :
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
   int contextDepth = 0;
   r::context::RCntxt context = r::context::getFunctionContext(BROWSER_FUNCTION, &contextDepth);
   if (contextDepth == 0 || !r::context::inBrowseContext())
   {
      // Not actively debugging; see if we have a stored environment name to
      // begin monitoring.
      std::string envName = persistentState().activeEnvironmentName();
      if (!envName.empty())
      {
         // It's possible for this to fail if the environment we were
         // monitoring doesn't exist any more. If this is the case, reset
         // the monitor to the global environment.
         Error error = setEnvironmentName(contextDepth, context, envName);
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
         .addParam( s_pEnvironmentMonitor->getMonitoredEnvironment())
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
                     boost::shared_ptr<r::context::RCntxt> pCurrentContext,
                     const json::JsonRpcRequest&,
                     json::JsonRpcResponse*)
{
   onConsolePrompt(
            pContextDepth,
            pLineDebugState,
            boost::make_shared<bool>(false),
            pCurrentContext);

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
      boost::smatch match;
      boost::regex reDebugAtPosition("debug at ([^#]*)#([^:]+): ");

      // start capturing debug output when R outputs "debug: "
      if (output == "debug: ")
      {
         *pCapturingDebugOutput = true;
         pLineDebugState->lastDebugText = "";
      }

      // emitted when browsing with srcref
      else if (boost::regex_match(output, match, reDebugAtPosition))
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
      else if (output.find("Called from: ") == 0)
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
   r::context::getFunctionContext(BROWSER_FUNCTION, &contextDepth);

   // If there's no browser on the stack, stay at the top level even if
   // there are functions on the stack--this is not a user debug session.
   if (!r::context::inBrowseContext())
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

SEXP rs_dumpContexts()
{
   return r::context::dumpContexts();
}

bool isSuspendable()
{
   return s_isGlobalEnvironmentSerializable;
}

Error initialize()
{
   // store on the heap so that the destructor is never called (so we
   // don't end up releasing the underlying environment SEXP after
   // R has already shut down / deinitialized)
   s_pEnvironmentMonitor = new EnvironmentMonitor();

   boost::shared_ptr<int> pContextDepth =
         boost::make_shared<int>(0);

   boost::shared_ptr<r::context::RCntxt> pCurrentContext =
         boost::make_shared<r::context::RCntxt>(r::context::globalContext());

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
   RS_REGISTER_CALL_METHOD(rs_jumpToFunction);
   RS_REGISTER_CALL_METHOD(rs_hasExternalPointer);
   RS_REGISTER_CALL_METHOD(rs_hasAltrep);
   RS_REGISTER_CALL_METHOD(rs_isAltrep);
   RS_REGISTER_CALL_METHOD(rs_dim);
   RS_REGISTER_CALL_METHOD(rs_dumpContexts);
   RS_REGISTER_CALL_METHOD(rs_newTestExternalPointer);
   RS_REGISTER_CALL_METHOD(rs_isSerializable);

   // subscribe to events
   using boost::bind;
   using namespace session::module_context;
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   events().onConsolePrompt.connect(bind(onConsolePrompt,
                                         pContextDepth,
                                         pLineDebugState,
                                         pCapturingDebugOutput,
                                         pCurrentContext));
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
         boost::bind(getEnvironmentNames, pContextDepth, pCurrentContext,
                     _1, _2);
   json::JsonRpcFunction setEnvName =
         boost::bind(setEnvironment, pContextDepth, pCurrentContext,
                     _1, _2);
   json::JsonRpcFunction requeryCtx =
         boost::bind(requeryContext, pContextDepth, pLineDebugState,
                     pCurrentContext, _1, _2);

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

