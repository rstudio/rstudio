/*
 * ROptions.cpp
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
#include <r/ROptions.hpp>

#include <boost/format.hpp>

#include <core/Log.hpp>
#include <core/FilePath.hpp>

#include <r/RExec.hpp>

using namespace core ;

namespace r {
namespace options {
      
Error saveOptions(const FilePath& filePath)
{
   return exec::RFunction(".rs.saveOptions", filePath.absolutePath()).call();
}
   
Error restoreOptions(const FilePath& filePath)
{
   return exec::RFunction(".rs.restoreOptions", filePath.absolutePath()).call();
}
   
const int kDefaultWidth = 80;   
   
void setOptionWidth(int width)
{
   boost::format fmt("options(width=%1%)");
   Error error = r::exec::executeString(boost::str(fmt % width));
   if (error)
      LOG_ERROR(error);
}
   
int getOptionWidth()
{
   return getOption<int>("width", kDefaultWidth);
}

SEXP getOption(const std::string& name)
{
   return Rf_GetOption(Rf_install(name.c_str()), R_BaseEnv);
}

SEXP setErrorOption(SEXP value)
{
   SEXP errorTag = Rf_install("error");
   SEXP option = SYMVALUE(Rf_install(".Options"));
   while (option != R_NilValue)
   {
      // is this the error option?
      if (TAG(option) == errorTag)
      {
         // set and return previous value
         SEXP previous = CAR(option);
         SETCAR(option, value);
         return previous;
      }

      // next option
      option = CDR(option);
   }

   return R_NilValue;
}

} // namespace options   
} // namespace r



