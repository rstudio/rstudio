/*
 * RExec.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#define R_INTERNAL_FUNCTIONS
#include <r/RExec.hpp>

#include <core/FilePath.hpp>
#include <core/Log.hpp>

#include <r/RErrorCategory.hpp>
#include <r/RSourceManager.hpp>
#include <r/RInterface.hpp>
#include <r/ROptions.hpp>

#include <R_ext/Parse.h>

#include <R_ext/libextern.h> 
LibExtern Rboolean R_interrupts_suspended;
LibExtern int R_interrupts_pending;
#ifdef _WIN32
LibExtern int UserBreak;
#endif

extern "C" void Rf_PrintWarnings();

using namespace core ;

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
      : didDisable_(false),
        previousErrorHandlerSEXP_(R_NilValue)
   {
      previousErrorHandlerSEXP_ = r::options::getOption("error");
      if (previousErrorHandlerSEXP_ != R_NilValue)
      {
         rProtect_.add(previousErrorHandlerSEXP_);
         r::options::setOption(Rf_install("error"), R_NilValue);
         didDisable_ = true;
      }
   }
   virtual ~DisableErrorHandlerScope()
   {
      try
      {
         if (didDisable_)
            r::options::setOption(Rf_install("error"), previousErrorHandlerSEXP_);
      }
      catch(...)
      {
      }
   }

private:
   bool didDisable_;
   r::sexp::Protect rProtect_;
   SEXP previousErrorHandlerSEXP_;
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

Error evaluateExpressions(SEXP expr, 
                          SEXP env, 
                          SEXP* pSEXP,
                          sexp::Protect* pProtect)   
{
   // disable custom error handlers while we execute code
   DisableErrorHandlerScope disableErrorHandler;

   int er=0;
   int i=0,l;
   
   // if we have an entire expression list, evaluate its contents one-by-one 
   // and return only the last one
   if (TYPEOF(expr)==EXPRSXP) 
   {
      l = LENGTH(expr);
      while (i<l) 
      {
         *pSEXP = R_tryEval(VECTOR_ELT(expr, i), env, &er);
         i++;
      }
   } 
   // evaluate single expression
   else
   {
      *pSEXP = R_tryEval(expr, R_GlobalEnv, &er);
   }
   
   // protect the result
   pProtect->add(*pSEXP);
   
   if (er)
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
   SEXP* pReturnSEXP ;
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

   SEXPTopLevelExecContext context ;
   context.function = function ;
   context.pReturnSEXP = pSEXP ;
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
  
Error executeString(const std::string& str)
{
   sexp::Protect rProtect;
   SEXP ignoredSEXP ;
   return evaluateString(str, &ignoredSEXP, &rProtect);
}
   
Error evaluateString(const std::string& str, 
                     SEXP* pSEXP, 
                     sexp::Protect* pProtect)
{
   // refresh source if necessary (no-op in production)
   r::sourceManager().reloadIfNecessary();
   
   // surrond the string with try in silent mode so we can capture error text
   std::string rCode = "try(" + str + ", TRUE)";

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
      std::string errorMsg ;
      Error extractError = sexp::extract(*pSEXP, &errorMsg);
      if (extractError)
         LOG_ERROR(extractError);
   
      // add it to the error
      return rCodeExecutionError(errorMsg, ERROR_LOCATION);
   }
   
   return Success();
}
   
RFunction::RFunction(SEXP functionSEXP)
{
   functionSEXP_ = functionSEXP;
   rProtect_.add(functionSEXP_);
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
      rProtect_.add(functionSEXP_);
}
   
   
Error RFunction::call(SEXP evalNS)
{
   sexp::Protect rProtect;
   SEXP ignoredResultSEXP ;
   return call(evalNS, &ignoredResultSEXP, &rProtect);  
}

Error RFunction::call(SEXP* pResultSEXP, sexp::Protect* pProtect)
{
   return call(R_GlobalEnv, pResultSEXP, pProtect);
}
   
Error RFunction::call(SEXP evalNS, SEXP* pResultSEXP, sexp::Protect* pProtect)
{
   // verify the function
   if (functionSEXP_ == R_UnboundValue)
   {
      Error error(errc::SymbolNotFoundError, ERROR_LOCATION);
      if (!functionName_.empty())
         error.addProperty("symbol", functionName_);
      return error;
   }
   
   // create the call object (LANGSXP) with the correct number of elements
   SEXP callSEXP ;
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
   Error error = evaluateExpressions(callSEXP, evalNS, pResultSEXP, pProtect);  
   if (error)
      return error;
   
   // return success
   return Success();
}

FilePath rBinaryPath()
{
   FilePath binPath = FilePath(R_HomeDir()).complete("bin");
#ifdef _WIN32
   return binPath.complete("Rterm.exe");
#else
   return binPath.complete("R");
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
      if (error.code() == r::errc::NoDataAvailableError)
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
   Rf_error(message.c_str());
}

void errorCall(SEXP call, const std::string& message)
{
   Rf_errorcall(call, message.c_str());
}
   
std::string getErrorMessage()
{
   std::string errMessage ;
   Error callError = RFunction("geterrmessage").call(&errMessage);
   if (callError)
      LOG_ERROR(callError);   
   return errMessage;
}
   

void warning(const std::string& warning)
{
   Rf_warning(warning.c_str());
}
   
   
void printWarnings()
{
   // NOTE: within the R source code R_CollectWarnings is checked prior
   // to calling PrintWarnings -- however, on Ubuntu we get a linker
   // error when attempting an extern declaration  of R_CollectWarnings.
   // We have observed that Rf_PrintWarnings actually returns immediately
   // if R_CollectWarnings is 0 so calling this function without the 
   // check should not pose a problem.

   Rf_PrintWarnings();
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

} // namespace exec   
} // namespace r



