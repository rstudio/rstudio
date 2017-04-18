/*
 * RJson.cpp
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

/* Convert R objects to json, conversions are performed as follows:

1) R nil values are converted to null. you can explicity return nil
   from a function using "return ()"

2) R vectors of primitive types are returned to json & GWT as follows:
      
      string   - array of string values (JsArrayString)
      logical  - array of true/false values (JsArrayBoolean)
      real     - array of number values (JsArrayNumber)
      integer  - array of number values (JsArrayInteger)
      complex  - array of complex objects (JsArray<Complex>)  

3) R new style lists (VECSXP) with named elements are returned to json & GWT as
   json objects (with each named list element constituting an object field)
 
4) R new style lists (VECSXP) without named elements (or with only some elements
   named) are returned as a json array. this array may be heterogenous and 
   therefore not readily mappable to a JS overlay type so in general this 
   scenario should be avoided.
 
5) R data frame (class="data.frame") are returned as arrays of json objects
   JsArray<Object>. Note that when creating a data frame to be marshalled
   back to javascript that check.rows = TRUE & stringsAsFactors = FALSE
   should be specified.
 
4) R old style lists (LISTSXP) are not currently supported.

5) There is as of yet no explicit support for arrays or matrixes so they
   will be returned as plain flattened vectors of primitive types
 
*/

#include <iostream>

#define R_INTERNAL_FUNCTIONS
#include <r/RJson.hpp>

#include <core/Error.hpp>
#include <core/StringUtils.hpp>

#include <r/RSexp.hpp>
#include <r/RErrorCategory.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace r {
namespace json {

namespace {

Error jsonValueFromVectorElement(SEXP vectorSEXP, 
                                 int i, 
                                 core::json::Value* pValue)
{   
   // NOTE: currently NaN is represented in json as null. this is problematic
   // as parsing routines (such as JS overlay types in GWT) won't handle
   // this properly. we need to either have a higher level representation
   // of r values (which would be quite verbose) or we need to represent
   // r numbers as strings which can contain embedded NaN (other other special
   // values like infinity) or we need to use a lower level JSON interface
   // (not JS overlay types for accessing R data
   
   // NOTE: we currently don't use NA_REAL (rather we use ISNAN). these
   // are different concepts (no value and NaN). distinguish these cases
   // and also make sure they are distinguished for other types
   
   // default to null
   *pValue = core::json::Value();
   
   // check for underlying value
   switch(TYPEOF(vectorSEXP))
   {
      case NILSXP:
      {
         *pValue = core::json::Value();
         break;
      }
      case STRSXP:
      {
         SEXP stringSEXP = STRING_ELT(vectorSEXP, i);
         if (stringSEXP != NA_STRING)
         {
            std::string value(Rf_translateCharUTF8(stringSEXP));
            *pValue = value;
         }
         break;
      }   
      case INTSXP:
      {
         int value = INTEGER(vectorSEXP)[i] ;
         if (value != NA_INTEGER)
            *pValue = value;
         break;
      }
      case REALSXP:
      {
         double value = REAL(vectorSEXP)[i] ;
         if (!ISNAN(value))
            *pValue = value;
         break;
      }
      case LGLSXP:
      {
         int value = LOGICAL(vectorSEXP)[i];
         if (value != NA_LOGICAL)
         {
            bool boolValue = (value == TRUE);
            *pValue = boolValue;
         }
         break;
      }
      case CPLXSXP:
      {
         double real = COMPLEX(vectorSEXP)[i].r;
         double imaginary = COMPLEX(vectorSEXP)[i].i;
         if ( !ISNAN(real) && !ISNAN(imaginary))
         {
            core::json::Object jsonComplex ;
            jsonComplex["r"] = real;
            jsonComplex["i"] = imaginary;
            *pValue = jsonComplex;
         }
         break;
      }
      case ENVSXP:
      {
         *pValue = std::string("<environment>");
         break;
      }
      default:
      {
         return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
      }
   }
   
   return Success();
}  


Error jsonValueArrayFromList(SEXP listSEXP, core::json::Value* pValue)
{
   // value array to return
   core::json::Array jsonValueArray;
   
   // return a value for each list item
   int listLength = Rf_length(listSEXP);
   for (int i=0; i<listLength; i++)
   {
      // get the value
      SEXP valueSEXP = VECTOR_ELT(listSEXP, i);
      
      // extract the value
      core::json::Value jsonValue ;
      Error error = jsonValueFromObject(valueSEXP, &jsonValue);
      if (error)
         return error;
      
      // add it to our result array
      jsonValueArray.push_back(jsonValue);
   }
   
   // set value then return success
   *pValue = jsonValueArray;
   return Success();
}

bool isNamedList(SEXP listSEXP)
{
   // must have the same number of names as elements
   int listLength = Rf_length(listSEXP);
   SEXP namesSEXP = Rf_getAttrib(listSEXP, R_NamesSymbol);
   if (namesSEXP == R_NilValue)
      return false;
   if (Rf_length(namesSEXP) != listLength)
      return false;
   
   // must have a name for each element
   std::vector<std::string> fieldNames ;
   Error error = sexp::getNames(listSEXP, &fieldNames);
   if (error)
      return false ;
   int nameCount = std::count_if(fieldNames.begin(), 
                                 fieldNames.end(),
                                 &core::string_utils::stringNotEmpty);
   if (nameCount != listLength)
      return false;   
   
   // passed all the tests!
   return true;
}
   
Error jsonObjectFromListElement(SEXP listSEXP, 
                                const std::vector<std::string>& fieldNames,
                                int index,
                                core::json::Value* pValue)
{
   // note list length
   int listLength = Rf_length(listSEXP);
   
   // compose an object by iterating through the fields
   core::json::Object jsonObject ;
   for (int f=0; f<listLength; f++)
   {
      // get the field
      SEXP fieldSEXP = VECTOR_ELT(listSEXP, f);
      
      // extract the value
      core::json::Value fieldValue ;
      switch(TYPEOF(fieldSEXP))
      {
         case VECSXP:
         {
            SEXP valueSEXP = VECTOR_ELT(fieldSEXP, index);
            Error error = jsonValueFromObject(valueSEXP, &fieldValue);
            if (error)
               return error;
            
            break;
         }
         default:
         {
            Error error = jsonValueFromVectorElement(fieldSEXP, 
                                                     index,
                                                     &fieldValue);
            if (error)
               return error;
            
            break;
         }
      }
      
      // add it to the json object
      jsonObject[fieldNames[f]] = fieldValue;
   }
   
   // set value and return success
   *pValue = jsonObject;
   return Success();
}

//   
// NOTE: this function assumes that isNamedList has been called
// and returned true for this list (validates a name for each element)
//   
Error jsonObjectFromList(SEXP listSEXP, core::json::Value* pValue)  
{
   // get the names of the list elements
   std::vector<std::string> fieldNames ;
   Error error = sexp::getNames(listSEXP, &fieldNames);
   if (error)
      return error;
   
   // compose object
   core::json::Object object ;
   int fields = Rf_length(listSEXP);
   for (int i=0; i<fields; i++)
   {
      SEXP fieldSEXP = VECTOR_ELT(listSEXP, i);
      
      core::json::Value objectValue ;
      error = jsonValueFromObject(fieldSEXP, &objectValue);
      if (error)
         return error ;
      
      object[fieldNames[i]] = objectValue;
   }
   
   // set object as return value
   *pValue = object;
   return Success();
}

//   
// NOTE: this function assumes that isNamedList has been called
// and returned true for this list (validates a name for each element)
//   
Error jsonObjectArrayFromDataFrame(SEXP listSEXP, core::json::Value* pValue)
{      
   // get the names of the list elements
   std::vector<std::string> fieldNames ;
   Error error = sexp::getNames(listSEXP, &fieldNames);
   if (error)
      return error;
   
   // object array to return
   core::json::Array jsonObjectArray ;
   
   // iterate through the values
   int values = Rf_length(VECTOR_ELT(listSEXP, 0));
   for (int v=0; v<values; v++)
   {
      core::json::Value objectValue ;
      error = jsonObjectFromListElement(listSEXP, fieldNames, v, &objectValue);
      if (error)
         return error ;
      
      jsonObjectArray.push_back(objectValue);
   }
   
   // return array and success
   *pValue = jsonObjectArray;
   return Success();
}

} // anonymous namespace

Error jsonValueFromScalar(SEXP scalarSEXP, core::json::Value* pValue)
{
   // verify length
   if (sexp::length(scalarSEXP) != 1)
      return Error(errc::UnexpectedDataTypeError, ERROR_LOCATION);
   
   // get element
   return jsonValueFromVectorElement(scalarSEXP, 0, pValue);
}
   
   
Error jsonValueFromVector(SEXP vectorSEXP, core::json::Value* pValue)
{
   int vectorLength = Rf_length(vectorSEXP);

   if (Rf_inherits(vectorSEXP, "rs.scalar"))
   {
      if (vectorLength > 0)
         return jsonValueFromVectorElement(vectorSEXP, 0, pValue);
      else
      {
         // return null
         *pValue = core::json::Value();
         return Success();
      }
   }

   core::json::Array vectorValues ;
   for (int i=0; i<vectorLength; i++)
   {
      core::json::Value elementValue ;
      Error error = jsonValueFromVectorElement(vectorSEXP, i, &elementValue);
      if (error)
         return error;
      
      vectorValues.push_back(elementValue);
   }
   
   *pValue = vectorValues;
   return Success();
}   
   
   
Error jsonValueFromList(SEXP listSEXP, core::json::Value* pValue)
{
   if (isNamedList(listSEXP))
   {
      if (Rf_inherits(listSEXP, "data.frame"))
          return jsonObjectArrayFromDataFrame(listSEXP, pValue);
      else
          return jsonObjectFromList(listSEXP, pValue);
   }
   else
   {
      return jsonValueArrayFromList(listSEXP, pValue);
   }
}
   
   
Error jsonValueFromObject(SEXP objectSEXP, core::json::Value* pValue)
{
   // NOTE: a few additional types/scenarios we could support are:
   //         - special handling for array
   //         - special handling for matrix
   //         - special handling for S3 and S4 style objects
   //         - support for LISTSXP? (old style lists)
   //         - isVectorAtomic should replace default: label
 
   switch(TYPEOF(objectSEXP))
   {
      case NILSXP:
      {
         *pValue = core::json::Value();
         return Success();
      }   
      case VECSXP:
      {
         return jsonValueFromList(objectSEXP, pValue);
      }  
      case SYMSXP:
      case LANGSXP:
      {
         *pValue = sexp::asString(objectSEXP);
         return Success();
      }
      default:
      {
         return jsonValueFromVector(objectSEXP, pValue);
      }
   }
} 
   
} // namespace json
} // namespace r
} // namespace rstudio

