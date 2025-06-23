/*
 * EnvironmentUtils.cpp
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

#include "EnvironmentUtils.hpp"

#include <r/RCntxt.hpp>
#include <r/RCntxtUtils.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/RSxpInfo.hpp>
#include <r/RVersionInfo.hpp>
#include <core/FileSerializer.hpp>
#include <core/FileUtils.hpp>
#include <session/SessionModuleContext.hpp>

#define MAX_ALTREP_LEN   65535   // maximum width/length for altrep inspection
#define MAX_ALTREP_DEPTH 5       // maximum depth for altrep inspection

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace environment {
namespace {

// the string sent to the client when we're unable to get the display value
// of a variable
const char UNKNOWN_VALUE[] = "<unknown>";

json::Value descriptionOfVar(SEXP var)
{
   std::string value;
   Error error = r::exec::RFunction(
            isUnevaluatedPromise(var) ?
                  ".rs.promiseDescription" :
                  ".rs.valueDescription",
               var).call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value(); // return null
   }
   else
   {
      return json::Value(value);
   }
}

} // anonymous namespace

// a variable is an unevaluated promise if its promise value is still unbound
bool isUnevaluatedPromise (SEXP var)
{
   return (TYPEOF(var) == PROMSXP) && (PRVALUE(var) == R_UnboundValue);
}

bool isAltrepImpl(SEXP var)
{
   // Reject nulls
   if (var == nullptr || var == R_NilValue)
      return false;

   // SEXP is a pointer to a structure that begins with an sxpinfo struct, so cast appropriately.
   r::sxpinfo& info = *reinterpret_cast<r::sxpinfo*>(var);

   // Select the bit referring to the ALTREP flag
   return info.alt;
}

bool isAltrep(SEXP var)
{
   return isAltrepImpl(var);
}

bool hasAltrepImpl(SEXP var, std::set<SEXP>& visited, unsigned maxDepth)
{
   // ignore if already visited
   if (visited.find(var) != visited.end())
      return false;

   if (r::sexp::isList(var) && maxDepth > 0)
   {
      // recurse if this is a list and we have recursion depth remaining; don't attempt to check
      // every element of extremely long vectors
      int len = std::min(r::sexp::length(var), MAX_ALTREP_LEN);

      // ensure we don't visit this list again
      visited.insert(var);

      for (int i = 0; i < len; i++)
      {
         if (hasAltrepImpl(VECTOR_ELT(var, i), visited, maxDepth - 1))
         {
            return true;
         }
      }
   }
   else
   {
      // this isn't a list, just check the object itself
      return isAltrep(var);
   }

   // we didn't find an ALTREP
   return false;
}

bool hasAltrep(SEXP var)
{
   // recursively scan for ALTREP objects
   std::set<SEXP> visited;
   return hasAltrepImpl(var, visited, MAX_ALTREP_DEPTH);
}

// convert a language variable to a value. language variables are special in
// that we can't allow them to be evaluated (doing so may e.g. trigger early
// evaluation of a call), so instead we pass the name of the variable and a
// reference to its environment so the lookup only happens in the context of
// the R session.
json::Value languageVarToJson(SEXP env, std::string objectName)
{
   std::string value;
   Error error = r::exec::RFunction(".rs.languageDescription",
               env, objectName)
               .call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value(UNKNOWN_VALUE);
   }
   else
   {
      return json::Value(value);
   }
}

json::Value varToJson(SEXP env, const r::sexp::Variable& var)
{
   json::Object varJson;
   SEXP varSEXP = var.second;

   // We can get a value from almost any object type from R, but there are
   // a few cases in which attempting to inspect the object will lead to
   // undesirable behavior. For these special value types, construct the
   // object definition manually.
   bool isActiveBinding = r::sexp::isActiveBinding(var.first, env);
   bool hasActiveBinding = isActiveBinding
         ? true
         : r::sexp::hasActiveBinding(var.first, env);
   
   if ((varSEXP == R_UnboundValue) ||
       (varSEXP == R_MissingArg) ||
       isUnevaluatedPromise(varSEXP) ||
       hasActiveBinding)
   {
      varJson["name"] = var.first;
      if (isUnevaluatedPromise(varSEXP))
      {
         varJson["type"] = std::string("promise");
         varJson["value"] = descriptionOfVar(varSEXP);
      }
      else if (isActiveBinding)
      {
         varJson["type"] = std::string("active binding");
         varJson["value"] = std::string("<Active binding>");
      }
      else if (hasActiveBinding)
      {
         varJson["type"] = std::string("object containing active binding");
         varJson["value"] = std::string("<Object containing active binding>");
      }
      else
      {
         varJson["type"] = std::string("unknown");
         varJson["value"] =  (varSEXP == R_MissingArg) ?
                                 descriptionOfVar(varSEXP) :
                                 json::Value(UNKNOWN_VALUE);
      }
      varJson["description"] = std::string("");
      varJson["contents"] = json::Array();
      varJson["length"] = 0;
      varJson["size"] = 0;
      varJson["contents_deferred"] = false;
   }
   // For all other value types, construct the definition normally.
   else
   {
      SEXP description;
      json::Value val;
      r::sexp::Protect protect;
      Error error = r::exec::RFunction(".rs.describeObject")
         .addParam(env)
         .addParam(var.first)
         .call(&description, &protect);
      if (error)
      {
         LOG_ERROR(error);
      }
      else
      {
         error = r::json::jsonValueFromObject(description, &val);
         if (error)
            LOG_ERROR(error);
         else
            return val;
      }
   }

   return std::move(varJson);
}

bool functionDiffersFromSource(
      SEXP srcRef,
      const std::string& functionCode)
{
   std::string fileName;
   Error error = sourceFileFromRef(srcRef, &fileName);
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }

   // check for ~/.active-rstudio-document -- we never want to match sources
   // in this file, as it's used to source unsaved changes from RStudio
   // editor buffers. don't match sources to an empty filename, either
   // (this will resolve to the user's home directory below).
   boost::algorithm::trim(fileName);
   if (fileName == "~/.active-rstudio-document" ||
       fileName.length() == 0)
   {
      return true;
   }

#ifdef WIN32
   // on Windows, check for reserved device names--attempting to read from
   // these may hang the session. source() can put things besides file names
   // in the source file attribute (for instance, the name of a variable
   // containing a connection).
   if (file_utils::isWindowsReservedName(fileName))
   {
       return true;
   }
#endif

   // make sure the file exists and isn't a directory
   FilePath sourceFilePath = module_context::resolveAliasedPath(fileName);
   if (!sourceFilePath.exists() ||
       sourceFilePath.isDirectory())
   {
      return true;
   }

   // read the portion of the file pointed to by the source refs from disk
   // the sourceref structure (including the array offsets used below)
   // is documented here:
   //
   // http://journal.r-project.org/archive/2010-2/RJournal_2010-2_Murdoch.pdf
   std::string fileContent;
   error = readStringFromFile(
         sourceFilePath,
         &fileContent,
         string_utils::LineEndingPosix,
         INTEGER(srcRef)[0],    // the first line
         INTEGER(srcRef)[2] + 1 // the last line
   );
   
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }

   // ignore leading/trailing whitespace
   std::string trimmedFunctionCode(functionCode);
   boost::algorithm::trim(trimmedFunctionCode);
   boost::algorithm::trim(fileContent);

   return trimmedFunctionCode != fileContent;
}

// given a source reference and a JSON object, add the line and character data
// from the source reference to the JSON object.
void sourceRefToJson(const SEXP srcref, json::Object* pObject)
{
   if (srcref != nullptr && TYPEOF(srcref) == INTSXP)
   {
      (*pObject)["line_number"] = INTEGER(srcref)[0];
      (*pObject)["end_line_number"] = INTEGER(srcref)[2];
      (*pObject)["character_number"] = INTEGER(srcref)[4];
      (*pObject)["end_character_number"] = INTEGER(srcref)[5];
   }
   else
   {
      (*pObject)["line_number"] = 0;
      (*pObject)["end_line_number"] = 0;
      (*pObject)["character_number"] = 0;
      (*pObject)["end_character_number"] = 0;
   }
}

Error sourceFileFromRef(const SEXP srcref, std::string* pFileName)
{
   r::sexp::Protect protect;
   SEXP fileName;
   Error error = r::exec::RFunction(".rs.sourceFileFromRef", srcref)
                 .call(&fileName, &protect);
   if (error)
       return error;

   return r::sexp::extract(fileName, pFileName, true);
}

} // namespace environment
} // namespace modules
} // namespace session
} // namespace rstudio
