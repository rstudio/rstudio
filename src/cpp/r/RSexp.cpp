/*
 * RSexp.cpp
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
#include <r/RSexp.hpp>
#include <r/RInternal.hpp>

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/numeric/conversion/cast.hpp>

#include <core/Log.hpp>
#include <core/DateTime.hpp>

#include <r/RExec.hpp>
#include <r/RErrorCategory.hpp>


// clean out global definitions of TRUE and FALSE so we can
// use the Rboolean variations of them
#undef TRUE
#undef FALSE

using namespace core ;

namespace r {
   
using namespace exec ;
   
namespace sexp {
   
std::string asString(SEXP object) 
{
    return std::string(Rf_translateChar(Rf_asChar(object)));
}
   
std::string safeAsString(SEXP object, const std::string& defValue)
{
   if (object != R_NilValue)
      return asString(object);
   else 
      return defValue;
}
    
int asInteger(SEXP object)
{
   return Rf_asInteger(object);
}
   
double asReal(SEXP object)
{
   return Rf_asReal(object);
}
   
bool asLogical(SEXP object)
{
   return Rf_asLogical(object) ? true : false;
}
   
SEXP findNamespace(const std::string& name)
{
   if (name.empty())
       return R_UnboundValue;
   
   return R_FindNamespace(Rf_mkString(name.c_str()));
}
   
void listEnvironment(SEXP env, 
                     bool includeAll,
                     Protect* pProtect,
                     std::vector<Variable>* pVariables)
{
   // reset passed vars
   pVariables->clear();
   
   // get the list of environment vars (protect locally because we 
   // we don't acutally return this list to the caller
   SEXP envVarsSEXP;
   Protect rProtect(envVarsSEXP = R_lsInternal(env, includeAll ? TRUE : FALSE));
   
   // populate pVariables
   for (int i=0; i<Rf_length(envVarsSEXP); i++)
   {
      std::string varName(CHAR(STRING_ELT(envVarsSEXP, i)));
      SEXP varSEXP = Rf_findVar(Rf_install(varName.c_str()), env);
      if (varSEXP != R_UnboundValue) // should never be unbound 
      {
         pProtect->add(varSEXP);
         pVariables->push_back(std::make_pair(varName, varSEXP));
      }
      else
      {
         LOG_WARNING_MESSAGE(
                  "Unexpected R_UnboundValue returned from R_lsInternal");
      }
   }
}

SEXP findVar(const std::string& name, const std::string& ns)
{
   if (name.empty())
      return R_UnboundValue;
   
   SEXP env = ns.empty() ? R_GlobalEnv : findNamespace(ns);
   
   return Rf_findVar(Rf_install(name.c_str()), env);
}
  
SEXP findFunction(const std::string& name, const std::string& ns) 
{
   if (name.empty())
      return R_UnboundValue;
   
   SEXP env = ns.empty() ? R_GlobalEnv : findNamespace(ns);
   
   SEXP functionSEXP;
   Error error = executeSafely<SEXP>(
      boost::bind(Rf_findFun, Rf_install(name.c_str()), env), 
      &functionSEXP
   );
   
   if (error)
      return R_UnboundValue;
   else
      return functionSEXP ;
}   
   
std::string typeAsString(SEXP object)
{
   return Rf_type2char(TYPEOF(object));  
}

std::string classOf(SEXP objectSEXP)
{
   return asString(Rf_getAttrib(objectSEXP, Rf_install("class")));
}
   
int length(SEXP object)
{
   return Rf_length(object);
}
 
   
bool isLanguage(SEXP object)
{
   return Rf_isLanguage(object);
}
   
bool isString(SEXP object)
{
   return Rf_isString(object);
}
   
bool isMatrix(SEXP object)
{
   return Rf_isMatrix(object);
}
   
bool isDataFrame(SEXP object)
{
   return Rf_isFrame(object);
}

bool isNull(SEXP object)
{
   return Rf_isNull(object) == TRUE;
}


SEXP getNames(SEXP sexp)
{
   return Rf_getAttrib(sexp, R_NamesSymbol);
}
   
Error getNames(SEXP sexp, std::vector<std::string>* pNames)   
{
   // attempt to get the field names
   SEXP namesSEXP = getNames(sexp);
   
   if (namesSEXP == R_NilValue || TYPEOF(namesSEXP) != STRSXP)
      return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
   else if (Rf_length(namesSEXP) != Rf_length(sexp))
      return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
   
   // copy them into the vector
   for (int i=0; i<Rf_length(namesSEXP); i++)
      pNames->push_back(Rf_translateChar(STRING_ELT(namesSEXP, i)) );
   
   return Success();
}

SEXP getAttrib(SEXP object, SEXP attrib)
{
   return Rf_getAttrib(object, attrib);
}


Error extract(SEXP valueSEXP, int* pInt)
{
   if (TYPEOF(valueSEXP) != INTSXP)
      return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
   
   if (Rf_length(valueSEXP) < 1)
      return Error(errc::NoDataAvailableError, ERROR_LOCATION);
      
   *pInt = INTEGER(valueSEXP)[0] ;
   return Success();
}
   
Error extract(SEXP valueSEXP, bool* pBool)
{
   if (TYPEOF(valueSEXP) != LGLSXP)
      return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
   
   if (Rf_length(valueSEXP) < 1)
      return Error(errc::NoDataAvailableError, ERROR_LOCATION);
   
   *pBool = LOGICAL(valueSEXP)[0] == TRUE ? true : false ;
   return Success();
   
}
   
Error extract(SEXP valueSEXP, std::vector<int>* pVector)
{
   if (TYPEOF(valueSEXP) != INTSXP)
      return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
   
   pVector->clear();
   for (int i=0; i<Rf_length(valueSEXP); i++)
      pVector->push_back(INTEGER(valueSEXP)[i]);
   
   return Success(); 
}
   
   
Error extract(SEXP valueSEXP, std::string* pString)
{
   if (TYPEOF(valueSEXP) != STRSXP)
      return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
   
   if (Rf_length(valueSEXP) < 1)
      return Error(errc::NoDataAvailableError, ERROR_LOCATION);
      
   *pString = std::string(Rf_translateChar(STRING_ELT(valueSEXP, 0)));
   
   return Success();
}
   
   
Error extract(SEXP valueSEXP, std::vector<std::string>* pVector)
{
   if (TYPEOF(valueSEXP) != STRSXP)
      return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);

   pVector->clear();
   for (int i=0; i<Rf_length(valueSEXP); i++)
      pVector->push_back(Rf_translateChar(STRING_ELT(valueSEXP, i)));
   
   return Success();
}
   
   
SEXP create(const json::Value& value, Protect* pProtect)
{
   // call embedded create function based on type
   if (value.type() == json::StringType)
   {
      return create(value.get_str(), pProtect);
   }
   else if (value.type() == json::IntegerType)
   {
      return create(value.get_int(), pProtect);
   }
   else if (value.type() == json::RealType)
   {
      return create(value.get_real(), pProtect);
   }
   else if (value.type() == json::BooleanType)
   {
      return create(value.get_bool(), pProtect);
   }
   else if (value.type() == json::ArrayType)
   {
      return create(value.get_array(), pProtect);
   }
   else if (value.type() == json::ObjectType)
   {
      return create(value.get_obj(), pProtect);
   }
   else if (value.is_null())
   {
      return R_NilValue;
   }
   else
   {
      return R_NilValue;
   }
}

SEXP create(const char* value, Protect* pProtect)
{
   return create(std::string(value), pProtect);
}

SEXP create(const std::string& value, Protect* pProtect)
{
   SEXP valueSEXP;
   pProtect->add(valueSEXP = Rf_allocVector(STRSXP, 1));
   SET_STRING_ELT(valueSEXP, 0, Rf_mkChar(value.c_str()));
   return valueSEXP;
}
   
SEXP create(int value, Protect* pProtect)
{
   SEXP valueSEXP;
   pProtect->add(valueSEXP = Rf_allocVector(INTSXP, 1));
   INTEGER(valueSEXP)[0] = value ;
   return valueSEXP;
}
   
SEXP create(double value, Protect* pProtect)
{
   SEXP valueSEXP;
   pProtect->add(valueSEXP = Rf_allocVector(REALSXP, 1));
   REAL(valueSEXP)[0] = value ;
   return valueSEXP;
}

SEXP create(bool value, Protect* pProtect)
{
   SEXP valueSEXP;
   pProtect->add(valueSEXP = Rf_allocVector(LGLSXP, 1));
   LOGICAL(valueSEXP)[0] = value ;
   return valueSEXP;
}

SEXP create(const json::Array& value, Protect* pProtect)
{
   // create the list
   SEXP listSEXP;
   pProtect->add(listSEXP = Rf_allocVector(VECSXP, value.size()));
   
   // add each array element to it
   for (json::Array::size_type i=0; i<value.size(); i++)
   {
      SEXP valueSEXP = create(value[i], pProtect);
      SET_VECTOR_ELT(listSEXP, i,  valueSEXP);
   }
   return listSEXP;
}
   
SEXP create(const json::Object& value, Protect* pProtect)
{
   // create the list
   SEXP listSEXP ;
   pProtect->add(listSEXP = Rf_allocVector(VECSXP, value.size()));
   
   // build list of names
   SEXP namesSEXP ;
   pProtect->add(namesSEXP = Rf_allocVector(STRSXP, value.size()));
   
   // add each object field to it
   int index = 0;
   for (json::Object::const_iterator 
            it = value.begin();
            it != value.end();
            ++it)
   {
      // set name
      SET_STRING_ELT(namesSEXP, index, Rf_mkChar(it->first.c_str()));
      
      // set value
      SEXP valueSEXP = create(it->second, pProtect);
      SET_VECTOR_ELT(listSEXP, index,  valueSEXP);
      
      // increment element index
      index++;
   }
   
   // attach names
   Rf_setAttrib(listSEXP, R_NamesSymbol, namesSEXP);
   
   // return the list
   return listSEXP;
}
   
SEXP create(const std::vector<std::string>& value, Protect* pProtect)
{
   SEXP valueSEXP;
   pProtect->add(valueSEXP = Rf_allocVector(STRSXP, value.size()));
   
   int index = 0;
   for (std::vector<std::string>::const_iterator 
        it = value.begin(); it != value.end(); ++it)
   {
      SET_STRING_ELT(valueSEXP, index++, Rf_mkChar(it->c_str()));
   }
   
   return valueSEXP;
}
   
SEXP create(const std::vector<int>& value, Protect *pProtect)
{
   SEXP valueSEXP;
   pProtect->add(valueSEXP = Rf_allocVector(INTSXP, value.size()));
   
   for (std::size_t i = 0; i < value.size(); ++i) 
      INTEGER(valueSEXP)[i] = value[i] ;
   
   return valueSEXP;
}

SEXP create(const std::vector<double>& value, Protect *pProtect)
{
   SEXP valueSEXP;
   pProtect->add(valueSEXP = Rf_allocVector(REALSXP, value.size()));
   
   for (std::size_t i = 0; i < value.size(); ++i) 
      REAL(valueSEXP)[i] = value[i] ;
   
   return valueSEXP;
}

SEXP create(const std::vector<bool>& value, Protect *pProtect)
{
   SEXP valueSEXP;
   pProtect->add(valueSEXP = Rf_allocVector(LGLSXP, value.size()));
   
   for (std::size_t i = 0; i < value.size(); ++i) 
      LOGICAL(valueSEXP)[i] = value[i] ;
   
   return valueSEXP;
}
   
namespace {  
int secondsSinceEpoch(boost::posix_time::ptime date)
{
   return boost::numeric_cast<int>(date_time::secondsSinceEpoch(date));
}}
   
SEXP create(const std::vector<boost::posix_time::ptime>& value,
            Protect* pProtect)
{
   // first create a vector of doubles containing seconds since epoch
   std::vector<int> seconds ;
   std::transform(value.begin(), 
                  value.end(),
                  std::back_inserter(seconds),
                  secondsSinceEpoch);
   
   // now turn this into an R vector and call as.POSIXct
   SEXP secondsSEXP = create(seconds, pProtect);
   SEXP posixCtSEXP = R_NilValue;           
   r::exec::RFunction asPOSIXct("as.POSIXct", secondsSEXP);
   asPOSIXct.addParam("tz", "GMT");
   asPOSIXct.addParam("origin", "1970-01-01");
   Error error = asPOSIXct.call(&posixCtSEXP, pProtect);
   if (error)
      LOG_ERROR(error);
   
   // return it
   return posixCtSEXP;
}
   
SEXP create(const std::vector<std::pair<std::string,std::string> >& value, 
            Protect* pProtect)
{
   // create the character vector and the names vector
   SEXP charSEXP, namesSEXP;
   pProtect->add(charSEXP = Rf_allocVector(STRSXP, value.size()));
   pProtect->add(namesSEXP = Rf_allocVector(STRSXP, value.size()));
   
   int index = 0;
   for (std::vector<std::pair<std::string,std::string> >::const_iterator 
         it = value.begin(); it != value.end(); ++it)
   {
      // set name and value
      SET_STRING_ELT(namesSEXP, index, Rf_mkChar(it->first.c_str()));
      SET_STRING_ELT(charSEXP, index,  Rf_mkChar(it->second.c_str()));
      
      // increment element index
      index++;
   }
   
   // attach names
   Rf_setAttrib(charSEXP, R_NamesSymbol, namesSEXP);
   
   // return the vector
   return charSEXP;   
}
   
Protect::~Protect()
{
   try
   {
      unprotectAll();
   }
   catch(...)
   {
   }
}

void Protect::add(SEXP sexp)
{
   PROTECT(sexp);
   protectCount_++;
}   

void Protect::unprotectAll()
{
   if (protectCount_ > 0)
      UNPROTECT(protectCount_);
   protectCount_ = 0;
}


PreservedSEXP::PreservedSEXP()
   : sexp_(R_NilValue)
{
}

PreservedSEXP::PreservedSEXP(SEXP sexp)
   : sexp_(R_NilValue)
{
   set(sexp);
}

void PreservedSEXP::set(SEXP sexp)
{
   releaseNow();
   sexp_ = sexp ;
   if (sexp_ != R_NilValue)
      ::R_PreserveObject(sexp_);
}

PreservedSEXP::~PreservedSEXP()
{
   try
   {
      releaseNow();
   }
   catch(...)
   {
   }
}

void PreservedSEXP::releaseNow()
{
   if (sexp_ != R_NilValue)
   {
      ::R_ReleaseObject(sexp_);
      sexp_ = R_NilValue;
   }
}


} // namespace sexp   
} // namespace r



