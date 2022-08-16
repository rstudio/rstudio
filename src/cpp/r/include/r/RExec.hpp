/*
 * RExec.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_R_EXEC_HPP
#define R_R_EXEC_HPP

#include <string>
#include <vector>
#include <stdexcept>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/function.hpp>

#include <shared_core/Error.hpp>
#include <core/system/System.hpp>

#include <r/RSexp.hpp> 
#include <r/RInterface.hpp>


namespace rstudio {
namespace core {
   class FilePath;
}
}

// IMPORTANT NOTE: all code in r::exec must provide "no jump" guarantee.
// See comment in RInternal.hpp for more info on this

namespace rstudio {
namespace r {
namespace exec {

// safe (no r error longjump) execution of arbitrary nullary function
core::Error executeSafely(boost::function<void()> function);

typedef boost::function<bool()> MainThreadFunction;

// helper class for variation of executeSafely w/ return value (impl below)
template <typename T>
class ExecuteTargetWithReturn 
{
public:
   ExecuteTargetWithReturn(const boost::function<T()>& function, T* pReturn)
      : function_(function), pReturn_(pReturn) {}

   // COPYING: via compiler (copyable members)

   void operator()() { *pReturn_ = function_(); }
     
private:
   boost::function<T()> function_;
   T* pReturn_;
};
 
// safe (no r error longjump) execution of arbitrary nullary function w/ return
template <typename T>
core::Error executeSafely(boost::function<T()> function, T* pReturn)
{
   ExecuteTargetWithReturn<T> target(function, pReturn);
   return executeSafely(target);
}

// flags that can be set during evaluation
enum EvalFlags
{
   EvalFlagsNone             = 0,
   EvalFlagsSuppressWarnings = 1,
   EvalFlagsSuppressMessages = 2
};

inline EvalFlags operator|(EvalFlags lhs, EvalFlags rhs)
{
   return static_cast<EvalFlags>(static_cast<int>(lhs) | static_cast<int>(rhs));
}

// parse and evaluate expressions
core::Error executeCallUnsafe(SEXP callSEXP,
                              SEXP envirSEXP,
                              SEXP* pResultSEXP,
                              sexp::Protect* pProtect);

core::Error executeStringUnsafe(const std::string& str, 
                                SEXP* pSEXP, 
                                sexp::Protect* pProtect);

core::Error executeStringUnsafe(const std::string& str,
                                SEXP envirSEXP,
                                SEXP* pSEXP,
                                sexp::Protect* pProtect);

core::Error executeString(const std::string& str);

core::Error evaluateString(const std::string& str,
                           SEXP* pSEXP,
                           sexp::Protect* pProtect,
                           EvalFlags flags = EvalFlagsNone);

template <typename T>
core::Error evaluateString(const std::string& str, T* pValue)
{
   sexp::Protect rProtect;
   SEXP valueSEXP;
   core::Error error = evaluateString(str, &valueSEXP, &rProtect);
   if (error)
      return error;

   return sexp::extract(valueSEXP, pValue);
}
   
// call R functions
class RFunction : boost::noncopyable
{
public:
   
   template <typename... T>
   explicit RFunction(const std::string& name, const T&... params)
      : functionSEXP_(R_UnboundValue)
   {
      commonInit(name);
      initParams(params...);
   }
   
   explicit RFunction(SEXP functionSEXP);
   
   ~RFunction();
   
   // COPYING: boost::noncopyable
   
   RFunction& addParam(SEXP paramSEXP)
   {
      addParam(std::string(), paramSEXP);
      return *this;
   }
   
   template <typename T>
   RFunction& addParam(const T& param)
   {
      addParam(std::string(), param);
      return *this;
   }
   
   RFunction& addParam(const std::string& name, SEXP paramSEXP)
   {
      preserver_.add(paramSEXP);
      params_.push_back(Param(name, paramSEXP));
      return *this;
   }
                        
   template <typename T>
   RFunction& addParam(const std::string& name, const T& param)
   {
      r::sexp::Protect protect;
      SEXP paramSEXP = sexp::create(param, &protect);
      preserver_.add(paramSEXP);
      params_.push_back(Param(name, paramSEXP));
      return *this;
   }
   
   template <typename T>
   RFunction& addUtf8Param(const T& param)
   {
      return addUtf8Param(std::string(), param);
   }
   
   template <typename T>
   RFunction& addUtf8Param(const std::string& name, const T& param)
   {
      r::sexp::Protect protect;
      SEXP paramSEXP = sexp::createUtf8(param, &protect);
      preserver_.add(paramSEXP);
      params_.push_back(Param(name, paramSEXP));
      return *this;
   }
   
   core::Error call(SEXP evalNS = R_GlobalEnv, bool safely = true);
   core::Error callUnsafe();

   core::Error call(SEXP* pResultSEXP, sexp::Protect* pProtect);
   core::Error call(SEXP evalNS, SEXP* pResultSEXP, sexp::Protect* pProtect);
   core::Error call(SEXP evalNS, bool safely, SEXP* pResultSEXP,
                    sexp::Protect* pProtect);

   template <typename T>
   core::Error call(T* pValue)
   {
      return call(R_GlobalEnv, pValue);
   }

   template <typename T>
   core::Error call(SEXP evalNS, T* pValue)
   {
      // call the function
      sexp::Protect rProtect;
      SEXP resultSEXP;
      core::Error error = call(evalNS, &resultSEXP, &rProtect);
      if (error)
         return error;
      
      // convert result to c++ accessible type
      return sexp::extract(resultSEXP, pValue);
   }

   template <typename T>
   core::Error callUtf8(T* pValue)
   {
      // call the function
      sexp::Protect rProtect;
      SEXP resultSEXP;
      core::Error error = call(R_GlobalEnv, &resultSEXP, &rProtect);
      if (error)
         return error;

      // convert result to c++ accessible type
      return sexp::extract(resultSEXP, pValue, true);
   }
   
private:
   void commonInit(const std::string& functionName);
   
   void initParams()
   {
   }
   
   template <typename T, typename... Rest>
   void initParams(const T& param, const Rest&... rest)
   {
      addParam(std::string(), param);
      initParams(rest...);
   }
   
private:
   // preserve SEXPs
   r::sexp::SEXPPreserver preserver_;
   
   // function 
   SEXP functionSEXP_;
   
   // function name (optional)
   std::string functionName_;

   // params
   struct Param 
   {
      Param(const std::string& name, SEXP valueSEXP)
         : name(name), valueSEXP(valueSEXP)
      {
      }
      std::string name;
      SEXP valueSEXP;
   };
   std::vector<Param> params_;
};

void warning(const std::string& warning);
   
void message(const std::string& message);

// special exception type used to raise R errors. allows for correct
// exiting from c++ context (with destructors called, etc.) while still
// propagating the error to a point where it will be re-raised to r
// using Rf_error
#define R_ERROR_BUFF_SIZE 256
class RErrorException : public std::exception
{
public:
   RErrorException(const std::string& message)
   {
      size_t length = message.copy(msgBuff_, R_ERROR_BUFF_SIZE);
      msgBuff_[length] = '\0';
   }
      
   const char* message() const { return msgBuff_; }
   
private:
   char msgBuff_[R_ERROR_BUFF_SIZE+1];
};
   
   
core::FilePath rBinaryPath();

core::Error system(const std::string& command, std::string* pOutput);
   
// raise error and get last error message
void error(const std::string& message);
void errorCall(SEXP call, const std::string& message);
std::string getErrorMessage();

bool interruptsPending();
void setInterruptsPending(bool pending);
void checkUserInterrupt();

class IgnoreInterruptsScope : boost::noncopyable
{
public:
   IgnoreInterruptsScope();
   virtual ~IgnoreInterruptsScope();
private:
   bool previousInterruptsSuspended_;
   boost::scoped_ptr<core::system::SignalBlocker> pSignalBlocker_;
};

// returns true if the global context is on the top (i.e. the context stack is
// empty and we're not debugging)
bool atTopLevelContext();

// create a scope for disabling debugging while evaluating an expression in a
// given environment--this is needed to protect internal functions from being
// stepped into when we execute them while the user is stepping while
// debugging. R does this itself for expressions entered at the Browse prompt,
// but we need to do it manually. 
// Discussion here: https://bugs.r-project.org/bugzilla/show_bug.cgi?id=15770
class DisableDebugScope : boost::noncopyable
{
public:
   DisableDebugScope(SEXP env);
   virtual ~DisableDebugScope();

private:
   int rdebug_;  // stored debug flag
   SEXP env_;    // debug environment (or NULL if not debugging)
};


class InterruptException {};

// track whether the session was interrupted
bool getWasInterrupted();
void setWasInterrupted(bool wasInterrupted);

} // namespace exec   
} // namespace r
} // namespace rstudio


#endif // R_R_EXEC_HPP 

