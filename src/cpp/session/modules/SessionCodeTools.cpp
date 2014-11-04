/*
 * SessionCodeTools.cpp
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

#include "SessionCodeTools.hpp"

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RSessionUtils.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace code_tools {

char ends(char begins)
{
   
   switch (begins) {
   case '(': return ')';
   case '[': return ']';
   case '{': return '}';
   }
   
   return '\0';
}

std::string finishedExpression(std::string expression)
{
   
   std::string::size_type n = expression.length();
   std::vector<char> terminators;
   
   char top = '\0';
   terminators.push_back(top);
   bool in_string = false;
   bool in_escape = false;
   
   for(std::string::size_type i = 0; i < n; i++) 
   {
      
      char cur = expression[i];
      
      if (in_string) {
         
         if (in_escape) 
         {
            in_escape = false;
            continue;
         }
         
         if (cur == '\\')
         {
            in_escape = true;
            continue;
         }
         
         if (cur != top)
            continue;
         
         // String terminates
         in_string = false;
         terminators.pop_back();
         top = terminators.back();
      }
      
      if (cur == top)
      {
         terminators.pop_back();
         top = terminators.back();
      }
      else if (cur == '(' || cur == '{' || cur == '[')
      {
         char end = ends(cur);
         top = end;
         terminators.push_back(top);
      }
      else if (cur == '"' || cur == '`' || cur == '\'')
      {
         top = cur;
         in_string = true;
         terminators.push_back(top);
      }
   }
   
   // Reverse order, and drop first
   std::vector<char> out;
   for (std::string::size_type i = terminators.size() - 1; i > 0; --i)
      out.push_back(terminators[i]);
   
   // Append terminators to the string
   std::string result = expression;
   for (std::string::size_type i = 0; i < out.size(); i++)
      result.append(std::string(1, out[i]));
   
   return result;
}

SEXP rs_finishedExpression(SEXP expressionSEXP)
{
   std::string expression(CHAR(STRING_ELT(expressionSEXP, 0)));
   std::string result = finishedExpression(expression);
   r::sexp::Protect protect;
   return r::sexp::create(result, &protect);
}

Error initialize()
{
   // register finishedExpression
   R_CallMethodDef finishedExpressionDef;
   finishedExpressionDef.name = "rs_finishedExpression";
   finishedExpressionDef.fun = (DL_FUNC) rs_finishedExpression;
   finishedExpressionDef.numArgs = 1;
   r::routines::addCallMethod(finishedExpressionDef);
   
   return Success();
}

} // end namespace code_tools
} // end namespace modules
} // end namespace session
