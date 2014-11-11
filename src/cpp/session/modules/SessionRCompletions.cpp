/*
 * SessionRCompletions.cpp
 *
 * Copyright (C) 2014 by RStudio, Inc.
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

#include "SessionRCompletions.hpp"

#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RInternal.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/RRoutines.hpp>
#include <r/ROptions.hpp>
#include <r/session/RSessionUtils.hpp>

#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RSourceIndex.hpp>
#include <core/r_util/RPackageInfo.hpp>
#include <core/r_util/RSourceIndex.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionCodeSearch.hpp"

using namespace core;

namespace session {
namespace modules {
namespace r_completions {

namespace {

char ends(char begins) {
   switch(begins) {
   case '(': return ')';
   case '[': return ']';
   case '{': return '}';
   }
   return '\0';
}

bool isBinaryOp(char character)
{
   return character == '~' ||
         character == '!' ||
         character == '@' ||
         character == '$' ||
         character == '%' ||
         character == '^' ||
         character == '&' ||
         character == '*' ||
         character == '-' ||
         character == '+' ||
         character == '*' ||
         character == '/' ||
         character == '=' ||
         character == '|' ||
         character == '<' ||
         character == '>' ||
         character == '?';

}

std::string finishExpression(const std::string& expression)
{
   std::string result = expression;

   // If the last character of the expression is a binary op, then we
   // place a '.' after it
   int n = expression.length();
   if (isBinaryOp(expression[n - 1]))
      result.append(".");

   std::vector<char> terminators;

   char top = '\0';
   terminators.push_back(top);

   bool in_string = false;
   bool in_escape = false;

   for (int i = 0; i < n; i++) {

      char cur = expression[i];

      if (in_string) {

         if (in_escape) {
            in_escape = false;
            continue;
         }

         if (cur == '\\') {
            in_escape = true;
            continue;
         }

         if (cur != top) {
            continue;
         }

         in_string = false;
         terminators.pop_back();
         top = terminators.back();
         continue;

      }

      if (cur == top) {
         terminators.pop_back();
         top = terminators.back();
      } else if (cur == '(' || cur == '{' || cur == '[') {
         char end = ends(cur);
         top = end;
         terminators.push_back(top);
      } else if (cur == '"' || cur == '`' || cur == '\'') {
         top = cur;
         in_string = true;
         terminators.push_back(top);
      }

   }

   // append to the output
   for (std::size_t i = terminators.size() - 1; i > 0; --i)
      result.push_back(terminators[i]);

   return result;
}

SEXP rs_finishExpression(SEXP stringSEXP)
{
   r::sexp::Protect rProtect;
   int n = r::sexp::length(stringSEXP);

   std::vector<std::string> output;
   output.reserve(n);
   for (int i = 0; i < n; ++i)
      output.push_back(finishExpression(CHAR(STRING_ELT(stringSEXP, i))));

   return r::sexp::create(output, &rProtect);
}

bool isPackageProject()
{
   return r_util::isPackageDirectory(projects::projectContext().directory());
}

SEXP rs_isPackageProject()
{
   r::sexp::Protect protect;
   const core::FilePath projectPath = projects::projectContext().directory();
   bool isPackageProject = r_util::isPackageDirectory(projectPath);
   return r::sexp::create(isPackageProject, &protect);
}

std::string projectName()
{
   if (!projects::projectContext().hasProject())
      return std::string();

   std::string projectFileName =
         projects::projectContext().file().filename();

   // Trim .Rproj extension
   return projectFileName.substr(0, projectFileName.length() - 6);
}

SEXP rs_projectName()
{
   r::sexp::Protect protect;
   return r::sexp::create(projectName(), &protect);
}

struct PackageCompletions {
   std::vector<std::string> completions;
   std::vector<bool> isFunction;
   bool moreAvailable;
};

PackageCompletions getPackageCompletions(const std::string& token)
{
   // bail if we're not in a project
   if (!isPackageProject())
      return PackageCompletions();

   // get functions from the source index
   std::vector<core::r_util::RSourceItem> items;
   bool moreAvailable = false;

   // TODO: wire up 'moreAvailable'
   modules::code_search::searchSource(token,
                                      1E3,
                                      true,
                                      &items,
                                      &moreAvailable);

   PackageCompletions pkgCompletions;
   BOOST_FOREACH(const core::r_util::RSourceItem& item, items)
   {
      if (item.braceLevel() == 0)
      {
         pkgCompletions.completions.push_back(item.name());
         pkgCompletions.isFunction.push_back(item.isFunction() || item.isMethod());
      }
   }

   pkgCompletions.moreAvailable = moreAvailable;
   return pkgCompletions;

}

SEXP rs_getPackageCompletions(SEXP tokenSEXP)
{
   r::sexp::Protect protect;
   std::string token = r::sexp::asString(tokenSEXP);
   PackageCompletions pkgCompletions = getPackageCompletions(token);

   json::Object object;
   object["completions"] = json::toJsonArray(pkgCompletions.completions);
   object["isFunction"] = json::toJsonArray(pkgCompletions.isFunction);
   object["moreAvailable"] = pkgCompletions.moreAvailable;

   return r::sexp::create(object, &protect);
}

} // end anonymous namespace

Error initialize() {

   r::routines::registerCallMethod(
            "rs_finishExpression",
            (DL_FUNC) r_completions::rs_finishExpression,
            1);

   r::routines::registerCallMethod(
            "rs_getPackageCompletions",
            (DL_FUNC) r_completions::rs_getPackageCompletions,
            1);

   r::routines::registerCallMethod(
            "rs_isPackageProject",
            (DL_FUNC) r_completions::rs_isPackageProject,
            0);

   r::routines::registerCallMethod(
            "rs_projectName",
            (DL_FUNC) r_completions::rs_projectName,
            0);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionRCompletions.R"));
   return initBlock.execute();
}

} // namespace r_completions
} // namespace modules
} // namespace session
