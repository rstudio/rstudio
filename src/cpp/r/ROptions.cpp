/*
 * ROptions.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#define R_INTERNAL_FUNCTIONS
#include <r/ROptions.hpp>

#include <boost/format.hpp>

#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/Environment.hpp>

#include <r/RExec.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace r {
namespace options {

namespace {

// last-known width of the build pane, in characters
int s_buildWidth = -1;

} // anonymous namespace

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
   core::system::setenv("RSTUDIO_CONSOLE_WIDTH",
                        core::safe_convert::numberToString(width));

   boost::format fmt("options(width=%1%)");
   Error error = r::exec::executeString(boost::str(fmt % width));
   if (error)
      LOG_ERROR(error);
}
   
int getOptionWidth()
{
   return getOption<int>("width", kDefaultWidth);
}

void setBuildOptionWidth(int width)
{
   s_buildWidth = width;
}

int getBuildOptionWidth()
{
   return s_buildWidth;
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
} // namespace rstudio



