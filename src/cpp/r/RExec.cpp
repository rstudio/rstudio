/*
 * RExec.cpp
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

#define R_INTERNAL_FUNCTIONS
#include <r/RExec.hpp>

#include <shared_core/FilePath.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>

#include <r/RErrorCategory.hpp>
#include <r/RSourceManager.hpp>
#include <r/RInterface.hpp>
#include <r/RCntxt.hpp>
#include <r/ROptions.hpp>

#include <R_ext/Parse.h>

#include <R_ext/libextern.h>

extern "C" {
LibExtern Rboolean R_interrupts_suspended;
LibExtern int R_interrupts_pending;
#ifdef _WIN32
LibExtern int UserBreak;
#endif
}

using namespace rstudio::core;

namespace rstudio {
namespace r {
   
namespace exec {
   
namespace {

// create a scope for disabling any installed error handlers (e.g. recover)
// we need to do this so that recover isn't invoked while we are running
// R code within an r::exec scope -- when the user presses 0 to exit
// from recover and jump_to_top it gets eaten by the R_ToplevelExecute
// context so the console becomes unresponsive
class DisableErrorHandlerScope : boost::noncopyable
{
public:
   DisableErrorHandlerScope()
      : didDisable_(false)
   {
      // allow users to enable / disable suppression of error handlers
      // (primarily for debugging when behind-the-scenes R code emits an error
      // that we'd like to learn a bit more about)
      bool suppressed = r::options::getOption("rstudio.errors.suppressed", true, false);
      if (!suppressed)
         return;
      
      SEXP handlerSEXP = r::options::setErrorOption(R_NilValue);
      if (handlerSEXP != R_NilValue)
      {
         preservedSEXP_.set(handlerSEXP);
         didDisable_ = true;
      }
   }
   virtual ~DisableErrorHandlerScope()
   {
      try
      {
         if (didDisable_)
            r::options::setErrorOption(preservedSEXP_.get());
      }
      catch(...)
      {
      }
   }

private:
   bool didDisable_;
   r::sexp::PreservedSEXP preservedSEXP_;
};


Error parseString(const std::string& str, SEXP* pSEXP, sexp::Protect* pProtect)
{
   // string to parse
   SEXP cv = sexp::create(str, pProtect);

   // do the parse and protect the result
   ParseStatus ps;
   *pSEXP=R_ParseVector(cv, 1, &ps, R_NilValue);
   pProtect->add(*pSEXP);

   // check error/success
   if (ps != PARSE_OK)
   {
      Error error(errc::ExpressionParsingError, ERROR_LOCATION);
      error.addProperty("code", str);
      return error;
   }
   else
   {
      return Success();
   }
}


// evaluate expressions without altering the error handler (use with caution--
// a user-supplied error handler may be invoked if the expression raises
// an error!)
enum EvalType {
   EvalTry,    // use R_tryEval
   EvalDirect  // use Rf_eval directly
};
Error evaluateExpressionsUnsafe(SEXP expr,
                                SEXP envir,
                                SEXP* pSEXP,
                                sexp::Protect* pProtect,
                                EvalType evalType)
{
   // detect if an error occurred (only relevant for EvalTry)
   int errorOccurred = 0;
   
   // if we have an entire expression list, evaluate its contents one-by-one 
   // and return only the last one
   if (TYPEOF(expr) == EXPRSXP) 
   {
      DisableDebugScope disableStepInto(envir);

      for (int i = 0, n = LENGTH(expr); i < n; i++)
      {
         if (evalType == EvalTry)
         {
            SEXP result = R_tryEval(VECTOR_ELT(expr, i), envir, &errorOccurred);
            if (errorOccurred == 0)
               *pSEXP = result;
         }
         else
         {
            *pSEXP = Rf_eval(VECTOR_ELT(expr, i), envir);
         }
      }
   }
   
   // otherwise, evaluate a single expression / call
   else
   {
      DisableDebugScope disableStepInto(envir);
      
      if (evalType == EvalTry)
      {
         SEXP result = R_tryEval(expr, envir, &errorOccurred);
         if (errorOccurred == 0)
            *pSEXP = result;
      }
      else
      {
         *pSEXP = Rf_eval(expr, envir);
      }
   }
   
   // protect the result
   pProtect->add(*pSEXP);
   
   if (errorOccurred)
   {
      // get error message -- note this results in a recursive call to
      // evaluate expressions during the fetching of the error. if this 
      // call yielded an error then this could infinitely recurse. it doesn't
      // appears as if geterrmessage will ever return an error state so
      // this is likely not an issue. still, if we were concerned about it
      // then we could simply read the error buffer directly from the module
      // where do_geterrmessage is defined (errors.c)
      return rCodeExecutionError(getErrorMessage(), ERROR_LOCATION);
   }
   else
   {
      return Success();
   }
}
   
Error evaluateExpressions(SEXP expr,
                          SEXP env,
                          SEXP* pSEXP,
                          sexp::Protect* pProtect)
{
   // disable custom error handlers while we execute code
   DisableErrorHandlerScope disableErrorHandler;

   return evaluateExpressionsUnsafe(expr, env, pSEXP, pProtect, EvalTry);
}

Error evaluateExpressions(SEXP expr, SEXP* pSEXP, sexp::Protect* pProtect)
{
   return evaluateExpressions(expr, R_GlobalEnv, pSEXP, pProtect);
}
       
void topLevelExec(void *data)
{
   boost::function<void()>* pFunction = (boost::function<void()>*)data;
   pFunction->operator()();
}
   
struct SEXPTopLevelExecContext
{
   boost::function<SEXP()> function;
   SEXP* pReturnSEXP;
};
   
void SEXPTopLevelExec(void *data)
{
   SEXPTopLevelExecContext* pContext = (SEXPTopLevelExecContext*)data;
   *(pContext->pReturnSEXP) = pContext->function();
}
   
} // anonymous namespace
   
Error executeSafely(boost::function<void()> function)
{
   // disable custom error handlers while we execute code
   DisableErrorHandlerScope disableErrorHandler;
   DisableDebugScope disableStepInto(R_GlobalEnv);

   Rboolean success = R_ToplevelExec(topLevelExec, (void*)&function);
   if (!success)
   {
      return rCodeExecutionError(getErrorMessage(), ERROR_LOCATION);
   }
   else
   {
      return Success();
   }
}
   
core::Error executeSafely(boost::function<SEXP()> function, SEXP* pSEXP)
{
   // disable custom error handlers while we execute code
   DisableErrorHandlerScope disableErrorHandler;
   DisableDebugScope disableStepInto(R_GlobalEnv);

   SEXPTopLevelExecContext context;
   context.function = function;
   context.pReturnSEXP = pSEXP;
   Rboolean success = R_ToplevelExec(SEXPTopLevelExec, (void*)&context);
   if (!success)
   {
      return rCodeExecutionError(getErrorMessage(), ERROR_LOCATION);
   }
   else
   {
      return Success();
   }
}

Error executeStringUnsafe(const std::string& str,
                          SEXP envirSEXP,
                          SEXP* pSEXP, 
                          sexp::Protect* pProtect)
{
   SEXP parsedSEXP = R_NilValue;
   Error error = r::exec::parseString(str, &parsedSEXP, pProtect);
   if (error)
      return error;
   
   return evaluateExpressionsUnsafe(parsedSEXP, envirSEXP, pSEXP, pProtect, EvalDirect);
}

Error executeStringUnsafe(const std::string& str,
                          SEXP* pSEXP, 
                          sexp::Protect* pProtect)
{
   return executeStringUnsafe(str, R_GlobalEnv, pSEXP, pProtect);
}
  
Error executeString(const std::string& str)
{
   sexp::Protect rProtect;
   SEXP ignoredSEXP;
   return evaluateString(str, &ignoredSEXP, &rProtect);
}
   
Error evaluateString(const std::string& str, 
                     SEXP* pSEXP, 
                     sexp::Protect* pProtect,
                     EvalFlags flags)
{
   // refresh source if necessary (no-op in production)
   r::sourceManager().reloadIfNecessary();
   
   // surround the string with try in silent mode so we can capture error text
   std::string rCode = "base::try(" + str + ", silent = TRUE)";
   
   // suppress warnings if requested
   if (flags & EvalFlagsSuppressWarnings)
      rCode = "base::suppressWarnings(" + rCode + ")";
   
   if (flags & EvalFlagsSuppressMessages)
      rCode = "base::suppressMessages(" + rCode + ")";

   // parse expression
   SEXP ps;
   Error parseError = parseString(rCode, &ps, pProtect);
   if (parseError)
      return parseError;

   // evaluate the expression
   Error evalError = evaluateExpressions(ps, pSEXP, pProtect);
   if (evalError)
   {
      evalError.addProperty("code", str);
      return evalError;
   }
   
   // check for try-error
   if (Rf_inherits(*pSEXP, "try-error"))
   {
      // get error message (merely log on failure so we can continue
      // and return the real error)
      std::string errorMsg;
      Error extractError = sexp::extract(*pSEXP, &errorMsg);
      if (extractError)
         LOG_ERROR(extractError);
   
      // add it to the error
      return rCodeExecutionError(errorMsg, ERROR_LOCATION);
   }
   
   return Success();
}
   
bool atTopLevelContext() 
{
   return context::RCntxt::begin()->callflag() == CTXT_TOPLEVEL;
}

RFunction::RFunction(SEXP functionSEXP)
{
   functionSEXP_ = functionSEXP;
   preserver_.add(functionSEXP_);
}
   
RFunction::~RFunction()
{
}
   
void RFunction::commonInit(const std::string& functionName)
{
   // refresh source if necessary (no-op in production)
   r::sourceManager().reloadIfNecessary();
   
   // record functionName (used later for diagnostics)
   functionName_ = functionName;
   
   // get name & ns
   std::string name, ns;
   
   // check for namespace qualifier
   std::string nsQual(":::");
   size_t pos = functionName_.find(nsQual);
   if (pos != std::string::npos)
   {
      ns = functionName_.substr(0, pos);
      name = functionName_.substr(pos + nsQual.size());
   }
   else
   {
      name = functionName_;
   }
   
   // lookup function
   functionSEXP_ = sexp::findFunction(name, ns);
   if (functionSEXP_ != R_UnboundValue)
      preserver_.add(functionSEXP_);
}
   
Error RFunction::callUnsafe()
{
   return call(R_GlobalEnv, false);
}

Error RFunction::call(SEXP evalNS, bool safely)
{
   sexp::Protect rProtect;
   SEXP ignoredResultSEXP;
   return call(evalNS, safely, &ignoredResultSEXP, &rProtect);
}

Error RFunction::call(SEXP* pResultSEXP, sexp::Protect* pProtect)
{
   return call(R_GlobalEnv, pResultSEXP, pProtect);
}
   
Error RFunction::call(SEXP evalNS, SEXP* pResultSEXP, sexp::Protect* pProtect)
{
   return call(evalNS, true, pResultSEXP, pProtect);
}

Error RFunction::call(SEXP evalNS, bool safely, SEXP* pResultSEXP,
                      sexp::Protect* pProtect)
{
   // verify the function
   if (functionSEXP_ == R_UnboundValue)
   {
      LOG_ERROR_MESSAGE("Failed to find function: '" + functionName_ + "'");
      Error error(errc::SymbolNotFoundError, ERROR_LOCATION);
      if (!functionName_.empty())
         error.addProperty("symbol", functionName_);
      return error;
   }
   
   // create the call object (LANGSXP) with the correct number of elements
   SEXP callSEXP;
   pProtect->add(callSEXP = Rf_allocVector(LANGSXP, 1 + params_.size()));
   SET_TAG(callSEXP, R_NilValue); // just like do_ascall() does 
   
   // assign the function to the first element of the call
   SETCAR(callSEXP, functionSEXP_);
   
   // assign parameters to the subseqent elements of the call
   SEXP nextSlotSEXP = CDR(callSEXP);
   for (std::vector<Param>::const_iterator 
            it = params_.begin(); it != params_.end(); ++it)
   {
      SETCAR(nextSlotSEXP, it->valueSEXP);
      // parameters can optionally be named
      if (!(it->name.empty()))
         SET_TAG(nextSlotSEXP, Rf_install(it->name.c_str()));
      nextSlotSEXP = CDR(nextSlotSEXP);
   }
   
   // call the function
   Error error = safely ?
            evaluateExpressions(callSEXP, evalNS, pResultSEXP, pProtect) :
            evaluateExpressionsUnsafe(callSEXP, evalNS, pResultSEXP, pProtect,
                  EvalTry);
   if (error)
      return error;
   
   // return success
   return Success();
}

FilePath rBinaryPath()
{
   FilePath binPath = FilePath(R_HomeDir()).completePath("bin");
#ifdef _WIN32
   return binPath.completePath("Rterm.exe");
#else
   return binPath.completePath("R");
#endif
}
   
Error system(const std::string& command, std::string* pOutput)
{
   r::exec::RFunction system("system", command);
   system.addParam("intern", true);
   system.addParam("ignore.stderr", true);
   
   // call it
   Error error = system.call(pOutput);
   if (error)
   {
      // if it is NoDataAvailable this means empty output
      if (error == r::errc::NoDataAvailableError)
      {
         pOutput->clear();
         return Success();
      }
      else
      {
         return error;
      }
   }
   else
   {
      return Success();
   }
}
   

void error(const std::string& message)   
{
   Rf_error("%s", message.c_str());
}

void errorCall(SEXP call, const std::string& message)
{
   Rf_errorcall(call, "%s", message.c_str());
}
   
std::string getErrorMessage()
{
   std::string errMessage;
   Error callError = RFunction("geterrmessage").call(&errMessage);
   if (callError)
      LOG_ERROR(callError);
   return errMessage;
}
   

void warning(const std::string& warning)
{
   Rf_warning("%s", warning.c_str());
}

void message(const std::string& message)
{
   Error error = r::exec::RFunction("message", message).call();
   if (error)
      LOG_ERROR(error);
}

bool interruptsPending()
{
#ifdef _WIN32
   return UserBreak == 1 ? true : false;
#else
   return R_interrupts_pending == 1 ? true : false;
#endif
}
   
void setInterruptsPending(bool pending)
{
#ifdef _WIN32
   UserBreak = pending ? 1 : 0;
#else
   R_interrupts_pending = pending ? 1 : 0;
#endif
}

void checkUserInterrupt()
{   
   R_CheckUserInterrupt();
}
   
IgnoreInterruptsScope::IgnoreInterruptsScope()
   : pSignalBlocker_(new core::system::SignalBlocker())
{
   // save suspend state and set suspend flag
   previousInterruptsSuspended_ = (R_interrupts_suspended == TRUE);
   R_interrupts_suspended = TRUE;
   
   // clear existing 
   setInterruptsPending(false);
      
   // enable signal blocker
   Error error = pSignalBlocker_->block(core::system::SigInt);
   if (error)
      LOG_ERROR(error);
}
   
IgnoreInterruptsScope::~IgnoreInterruptsScope()
{
   try
   {
      // delete signal blocker (may cause delivery of one of the blocked
      // interrupts, but we restore the previous interrupt state below
      // so this is no problem)
      pSignalBlocker_.reset();
      
      // restore suspended state
      R_interrupts_suspended = previousInterruptsSuspended_ ? TRUE : FALSE;
      
      // clear state
      setInterruptsPending(false);
   }
   catch(...)
   {
   }
}

DisableDebugScope::DisableDebugScope(SEXP env): 
   rdebug_(0), 
   env_(nullptr)
{
   // nothing to do if no environment 
   if (env == nullptr) {
      return;
   }

   // check to see whether there's a debug flag set on this environment
   rdebug_ = RDEBUG(env);

   // if there is, turn it off and save the old flag for restoration
   if (rdebug_ != 0) 
   {
      SET_RDEBUG(env, 0);
      env_ = env;
   } 
}

DisableDebugScope::~DisableDebugScope()
{
   // if we disabled debugging and debugging didn't end during the command 
   // evaluation, restore debugging
   if (env_ != nullptr && !atTopLevelContext()) 
   {
      SET_RDEBUG(env_, rdebug_);
   }
}


} // namespace exec   
} // namespace r
} // namespace rstudio



