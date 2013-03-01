/*
 * RSexp.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <boost/shared_ptr.hpp>
#include <boost/any.hpp>
#include <boost/utility.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/Error.hpp>
#include <core/json/Json.hpp>

#include <r/RErrorCategory.hpp>
#include <r/RInternal.hpp>


// IMPORTANT NOTE: all code in r::sexp must provide "no jump" guarantee.
// See comment in RInternal.hpp for more info on this


namespace r {
namespace sexp {
   
class Protect;
   
// environments and namespaces
SEXP findNamespace(const std::string& name);
   
// variables within an environment
typedef std::pair<std::string,SEXP> Variable ;
void listEnvironment(SEXP env, 
                     bool includeAll,
                     Protect* pProtect,
                     std::vector<Variable>* pVariables);
      
// object info
SEXP findVar(const std::string& name,
             const std::string& ns = std::string()); 
SEXP findFunction(const std::string& name,
                  const std::string& ns = std::string());
std::string typeAsString(SEXP object);
std::string classOf(SEXP objectSEXP);
int length(SEXP object);
   
SEXP getNames(SEXP sexp);
core::Error getNames(SEXP sexp, std::vector<std::string>* pNames);  
 
// type checking
bool isString(SEXP object);
bool isLanguage(SEXP object);
bool isMatrix(SEXP object);
bool isDataFrame(SEXP object);   
     
bool isNull(SEXP object);

// type coercions
std::string asString(SEXP object);
std::string safeAsString(SEXP object, 
                         const std::string& defValue = std::string());
int asInteger(SEXP object);
double asReal(SEXP object);
bool asLogical(SEXP object);

SEXP getAttrib(SEXP object, SEXP attrib);

// extract c++ type from R SEXP
core::Error extract(SEXP valueSEXP, int* pInt);
core::Error extract(SEXP valueSEXP, bool* pBool);
core::Error extract(SEXP valueSEXP, double* pDouble);
core::Error extract(SEXP valueSEXP, std::vector<int>* pVector);   
core::Error extract(SEXP valueSEXP, std::string* pString);
core::Error extract(SEXP valueSEXP, std::vector<std::string>* pVector);
      
// create SEXP from c++ type
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

SEXP create(const std::vector<std::pair<std::string,std::string> >& value, 
            Protect* pProtect);
SEXP create(const core::json::Array& value, Protect* pProtect);
SEXP create(const core::json::Object& value, Protect* pProtect);


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

template <typename T>
core::Error getNamedListElement(SEXP listSEXP,
                                const std::string& name,
                                T* pValue)
{
   // find the element
   int valueIndex = indexOfElementNamed(listSEXP, name);

   if (valueIndex != -1)
   {
      // get the appropriate value
      SEXP valueSEXP = VECTOR_ELT(listSEXP, valueIndex);
      return sexp::extract(valueSEXP, pValue);
   }
   else
   {
      // otherwise an error
      core::Error error(r::errc::ListElementNotFoundError, ERROR_LOCATION);
      error.addProperty("element", name);
      return error;
   }
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

} // namespace sexp
} // namespace r
   

#endif // R_R_SEXP_HPP 

