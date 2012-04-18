/*
 * RExec.hpp
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

#ifndef R_R_EXEC_HPP
#define R_R_EXEC_HPP

#include <string>
#include <vector>
#include <stdexcept>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/function.hpp>

#include <core/Error.hpp>
#include <core/system/System.hpp>

#include <r/RSexp.hpp> 


namespace core {
   class FilePath;
}

// IMPORTANT NOTE: all code in r::exec must provide "no jump" guarantee.
// See comment in RInternal.hpp for more info on this

namespace r {
namespace exec {
   
// safe (no r error longjump) execution of abritrary nullary function
core::Error executeSafely(boost::function<void()> function);

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
   boost::function<T()> function_ ;
   T* pReturn_ ;
};
 
// safe (no r error longjump) execution of abritrary nullary function w/ return
template <typename T>
core::Error executeSafely(boost::function<T()> function, T* pReturn)
{
   ExecuteTargetWithReturn<T> target(function, pReturn);
   return executeSafely(target);
}

   
// parse and evaluate expressions  
core::Error executeString(const std::string& str);
core::Error evaluateString(const std::string& str, 
                           SEXP* pSEXP, 
                           sexp::Protect* pProtect);
template <typename T>
core::Error evaluateString(const std::string& str, T* pValue)
{
   sexp::Protect rProtect;
   SEXP valueSEXP ;
   core::Error error = evaluateString(str, &valueSEXP, &rProtect);
   if (error)
      return error ;

   return sexp::extract(valueSEXP, pValue);
}
   
// call R functions
class RFunction : boost::noncopyable
{
public:
   explicit RFunction(const std::string& name)
      : functionSEXP_(R_UnboundValue)
   {
      commonInit(name);
   }
   
   template <typename ParamType>
   RFunction(const std::string& name, const ParamType& param)
      : functionSEXP_(R_UnboundValue)
   {
      commonInit(name);
      addParam(param);
   }
  
   template <typename Param1Type, typename Param2Type>
   RFunction(const std::string& name, 
             const Param1Type& param1, 
             const Param2Type& param2)
      : functionSEXP_(R_UnboundValue)
   {
      commonInit(name);
      addParam(param1);
      addParam(param2);
   }
   
   template <typename Param1Type, typename Param2Type, typename Param3Type>
   RFunction(const std::string& name, 
             const Param1Type& param1, 
             const Param2Type& param2,
             const Param3Type& param3)
      : functionSEXP_(R_UnboundValue)
   {
      commonInit(name);
      addParam(param1);
      addParam(param2);
      addParam(param3);
   }
   
   explicit RFunction(SEXP functionSEXP);
   
   virtual ~RFunction() ;
   
   // COPYING: boost::noncopyable
   
   void addParam(SEXP param)
   {
      addParam(std::string(), param);
   }
   
   template <typename T>
   void addParam(const T& param)
   {
      addParam(std::string(), param);
   }
   
   void addParam(const std::string& name, SEXP param)
   {
      params_.push_back(Param(name, param));
   }
                        
   template <typename T>
   void addParam(const std::string& name, const T& param)
   {
      SEXP paramSEXP = sexp::create(param, &rProtect_);
      params_.push_back(Param(name, paramSEXP));
   }
                        
   core::Error call(SEXP evalNS = R_GlobalEnv);

   core::Error call(SEXP* pResultSEXP, sexp::Protect* pProtect);
   core::Error call(SEXP evalNS, SEXP* pResultSEXP, sexp::Protect* pProtect);
 
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
      SEXP resultSEXP ;
      core::Error error = call(evalNS, &resultSEXP, &rProtect);  
      if (error)
         return error ;
      
      // convert result to c++ accessible type
      return sexp::extract(resultSEXP, pValue) ;
   }
   
private:
   void commonInit(const std::string& functionName);
   
private:
   // protect included SEXPs
   sexp::Protect rProtect_ ;
   
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
      std::string name ;
      SEXP valueSEXP ;
   };
   std::vector<Param> params_ ;
};

// print any pending warnings
void warning(const std::string& warning);
void printWarnings();

   
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
   bool previousInterruptsSuspended_ ;
   boost::scoped_ptr<core::system::SignalBlocker> pSignalBlocker_;
};

class InterruptException {};
   
} // namespace exec   
} // namespace r


#endif // R_R_EXEC_HPP 

