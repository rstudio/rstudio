/*
 * RSexp.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#ifndef R_R_SEXP_HPP
#define R_R_SEXP_HPP

#include <string>
#include <vector>
#include <deque>
#include <map>
#include <set>

#include <boost/function.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/any.hpp>
#include <boost/utility.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/json/Json.hpp>
#include <core/r_util/RFunctionInformation.hpp>

#include <r/RErrorCategory.hpp>
#include <r/RInternal.hpp>


// IMPORTANT NOTE: all code in r::sexp must provide "no jump" guarantee.
// See comment in RInternal.hpp for more info on this


namespace rstudio {
namespace r {
namespace sexp {
   
class ListBuilder;
class Protect;
   
// environments and namespaces
SEXP asEnvironment(std::string name);
std::vector<std::string> getLoadedNamespaces();
SEXP findNamespace(const std::string& name);
SEXP asNamespace(const std::string& name);
   
// variables within an environment
typedef std::pair<std::string,SEXP> Variable ;
void listEnvironment(SEXP env, 
                     bool includeAll,
                     bool includeLastDotValue,
                     Protect* pProtect,
                     std::vector<Variable>* pVariables);
      
// object info
SEXP findVar(const std::string& name,
             const std::string& ns = std::string()); 
SEXP findVar(const std::string& name,
             const SEXP env);
SEXP findFunction(const std::string& name,
                  const std::string& ns = std::string());
std::string typeAsString(SEXP object);
std::string classOf(SEXP objectSEXP);
int length(SEXP object);
   
SEXP getNames(SEXP sexp);
bool setNames(SEXP sexp, const std::vector<std::string>& names);

core::Error getNames(SEXP sexp, std::vector<std::string>* pNames);  
bool hasActiveBinding(const std::string&, const SEXP);
bool isActiveBinding(const std::string&, const SEXP);

// function introspection
SEXP functionBody(SEXP functionSEXP);

// type checking
bool isString(SEXP object);
bool isLanguage(SEXP object);
bool isList(SEXP object);
bool isMatrix(SEXP object);
bool isDataFrame(SEXP object);   
bool isNull(SEXP object);
bool isEnvironment(SEXP object);
bool isPrimitiveEnvironment(SEXP object);

// type coercions
std::string asString(SEXP object);
std::string safeAsString(SEXP object, 
                         const std::string& defValue = std::string());
int asInteger(SEXP object);
double asReal(SEXP object);
bool asLogical(SEXP object);

bool fillVectorString(SEXP object, std::vector<std::string>* pVector);
bool fillSetString(SEXP object, std::set<std::string>* pSet);

SEXP getAttrib(SEXP object, SEXP attrib);
SEXP getAttrib(SEXP object, const std::string& attrib);
SEXP setAttrib(SEXP object, const std::string& attrib, SEXP val);

// weak/external pointers and finalizers
SEXP makeWeakRef(SEXP key, SEXP val, R_CFinalizer_t fun, Rboolean onexit);
void registerFinalizer(SEXP s, R_CFinalizer_t fun);
SEXP makeExternalPtr(void* ptr, R_CFinalizer_t fun, Protect* protect);
void* getExternalPtrAddr(SEXP extptr);
void clearExternalPtr(SEXP extptr);

// extract c++ type from R SEXP
core::Error extract(SEXP valueSEXP, int* pInt);
core::Error extract(SEXP valueSEXP, bool* pBool);
core::Error extract(SEXP valueSEXP, double* pDouble);
core::Error extract(SEXP valueSEXP, std::vector<int>* pVector);   
core::Error extract(SEXP valueSEXP, std::string* pString, bool asUtf8 = false);
core::Error extract(SEXP valueSEXP, std::vector<std::string>* pVector);
core::Error extract(SEXP valueSEXP, std::set<std::string>* pSet);
core::Error extract(SEXP valueSEXP, std::map< std::string, std::set<std::string> >* pMap);
      
// create SEXP from c++ type
SEXP create(SEXP valueSEXP, Protect* pProtect);
SEXP create(const core::json::Value& value, Protect* pProtect);
SEXP create(const char* value, Protect* pProtect);
SEXP create(const std::string& value, Protect* pProtect);
SEXP create(int value, Protect* pProtect);
SEXP create(double value, Protect* pProtect);
SEXP create(bool value, Protect* pProtect);
SEXP create(const std::vector<std::string>& value, Protect* pProtect);
SEXP create(const std::vector<int>& value, Protect*pProtect);
SEXP create(const std::vector<double>& value, Protect*pProtect);
SEXP create(const std::vector<bool>& value, Protect*pProtect);
SEXP create(const std::vector<boost::posix_time::ptime>& value,
            Protect* pProtect);
SEXP create(const std::map<std::string, std::vector<std::string> >& value,
            Protect* pProtect);

SEXP create(const std::vector<std::pair<std::string,std::string> >& value, 
            Protect* pProtect);
SEXP create(const std::set<std::string>& value, Protect* pProtect);
SEXP create(const core::json::Array& value, Protect* pProtect);
SEXP create(const core::json::Object& value, Protect* pProtect);
SEXP create(const ListBuilder& builder, Protect* pProtect);
SEXP create(const std::map<std::string, std::string>& value, Protect* pProtect);

// Create a raw vector (binary data)
SEXP createRawVector(const std::string& data, Protect* pProtect);

// Create a named list
SEXP createList(const std::vector<std::string>& names, Protect* pProtect);

inline int indexOfElementNamed(SEXP listSEXP, const std::string& name)
{
   // get the names so we can determine which slot the element is in are in
   std::vector<std::string> names;
   core::Error error = r::sexp::getNames(listSEXP, &names);
   if (error)
      return -1;

   // find the index
   int valueIndex = -1;
   for (int i = 0; i<(int)names.size(); i++)
   {
      if (names[i] == name)
      {
         valueIndex = i;
         break;
      }
   }

   // return
   return valueIndex;

}

core::Error getNamedListSEXP(SEXP listSEXP, const std::string& name,
                             SEXP* pValueSEXP);

template <typename T>
core::Error getNamedListElement(SEXP listSEXP,
                                const std::string& name,
                                T* pValue)
{
   SEXP valueSEXP;
   core::Error error = getNamedListSEXP(listSEXP, name, &valueSEXP);
   if (error)
      return error;
   else
      return sexp::extract(valueSEXP, pValue);
}

template <typename T>
core::Error getNamedListElement(SEXP listSEXP,
                                const std::string& name,
                                T* pValue,
                                const T& defaultValue)
{
  core:: Error error = getNamedListElement(listSEXP, name, pValue);
  if (error)
  {
     if (error.code() == r::errc::ListElementNotFoundError)
     {
        *pValue = defaultValue;
        return core::Success();
     }
     else
     {
        return error;
     }
   }
   else
   {
      return core::Success();
   }
}

template <typename T>
core::Error getNamedAttrib(SEXP object, const std::string& name, T* pValue)
{
   SEXP attrib = getAttrib(object, name); 
   if (attrib == R_NilValue) 
   {
      core::Error error(r::errc::AttributeNotFoundError, ERROR_LOCATION);
      error.addProperty("attribute", name);
      return error;
   }

   return extract(attrib, pValue);
}

// protect R expressions
class Protect : boost::noncopyable
{
public:
   Protect()
   : protectCount_(0)
   {
   }
   
   explicit Protect(SEXP sexp)
   : protectCount_(0)
   {
      add(sexp);
   }
   
   virtual ~Protect();
   
   // COPYING: boost::noncopyable
   
   void add(SEXP sexp);
   void unprotectAll();
   
private:
   int protectCount_ ;
};

// set list element by name. note that the specified element MUST already
// exist before the call
template <typename T>
core::Error setNamedListElement(SEXP listSEXP,
                                const std::string& name,
                                const T& value)
{
   // convert to SEXP
   r::sexp::Protect rProtect;
   SEXP valueSEXP = create(value, &rProtect);

   // find the element
   int valueIndex = indexOfElementNamed(listSEXP, name);

   if (valueIndex != -1)
   {
      // set the appropriate value and return success
      SET_VECTOR_ELT(listSEXP, valueIndex, valueSEXP);
      return core::Success();
   }
   else
   {
      // otherwise an error
      core::Error error(r::errc::ListElementNotFoundError, ERROR_LOCATION);
      error.addProperty("element", name);
      return error;
   }
}


class PreservedSEXP : boost::noncopyable
{
public:
   PreservedSEXP();
   explicit PreservedSEXP(SEXP sexp);
   virtual ~PreservedSEXP();

   void set(SEXP sexp);
   SEXP get() const { return sexp_; }
   bool isNil() const { return sexp_ == R_NilValue; }

   typedef void (*unspecified_bool_type)();
   static void unspecified_bool_true() {}
   operator unspecified_bool_type() const
   {
      return isNil() ? 0 : unspecified_bool_true;
   }
   bool operator!() const
   {
      return isNil();
   }

   void releaseNow();

private:
   SEXP sexp_;
};

class SEXPPreserver : boost::noncopyable
{
public:
   SEXPPreserver() {}
   ~SEXPPreserver();
   SEXP add(SEXP dataSEXP);
   
private:
   std::vector<SEXP> preservedSEXPs_;
};

class ListBuilder : boost::noncopyable
{
public:
   explicit ListBuilder(Protect* pProtect)
      : pProtect_(pProtect) {}
   
   template <typename T>
   void add(const std::string& name, const T& object)
   {
      objects_.push_back(create(object, pProtect_));
      names_.push_back(name);
   }
   
   template <typename T>
   void add(const T& object)
   {
      objects_.push_back(create(object, pProtect_));
      names_.push_back(std::string());
   }
   
   const std::vector<SEXP>& objects() const
   {
      return objects_;
   }
   
   const std::vector<std::string>& names() const
   {
      return names_;
   }

private:
   std::vector<SEXP> objects_;
   std::vector<std::string> names_;
   Protect* pProtect_;
};

void printValue(SEXP object);
bool inherits(SEXP object, const char* S3Class);
bool maybePerformsNSE(SEXP functionSEXP);

SEXP objects(SEXP environment, 
             bool allNames,
             Protect* pProtect);

core::Error objects(SEXP environment,
                    bool allNames,
                    std::vector<std::string>* pNames);

core::Error getNamespaceExports(SEXP ns,
                                std::vector<std::string>* pNames);

const std::set<std::string>& nsePrimitives();

// NOTE: Primarily to be used with boost::bind, to add functions that are then
// called on each node within the call. Functions can return true to signal the
// recursion should end.
class CallRecurser : boost::noncopyable
{
public:
   typedef boost::function<bool(SEXP)> Operation;
   
private:
   typedef std::vector<Operation> Operations;
   
public:
   
   explicit CallRecurser(SEXP callSEXP)
      : callSEXP_(callSEXP)
   {}

   void add(const Operation& operation)
   {
      operations_.push_back(operation);
   }
   
   void run()
   {
      runImpl(callSEXP_, operations_, operations_.size());
   }
   
private:
   
   static void runImpl(
         SEXP nodeSEXP,
         Operations operations,
         std::size_t n)
   {
      for (std::size_t i = 0; i < n; ++i)
         if (operations[i](nodeSEXP))
            return;
      
      if (TYPEOF(nodeSEXP) == LANGSXP)
      {
         while (nodeSEXP != R_NilValue)
         {
            runImpl(CAR(nodeSEXP), operations, n);
            nodeSEXP = CDR(nodeSEXP);
         }
      }
   }
      
   SEXP callSEXP_;
   Operations operations_;
};

core::Error extractFunctionInfo(
      SEXP functionSEXP,
      core::r_util::FunctionInformation* pInfo,
      bool extractDefaultArguments,
      bool recordSymbolUsage);

std::string environmentName(SEXP envSEXP);

} // namespace sexp
} // namespace r
} // namespace rstudio
   

#endif // R_R_SEXP_HPP 

