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

#include <session/SessionModuleContext.hpp>

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

std::string finishExpression(const std::string& expression)
{
   int n = expression.length();
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
   std::string result = expression;
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

} // end anonymous namespace

Error initialize() {
   
   r::routines::registerCallMethod(
            "rs_finishExpression",
            (DL_FUNC) r_completions::rs_finishExpression,
            1);
   
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
