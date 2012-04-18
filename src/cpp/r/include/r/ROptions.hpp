/*
 * ROptions.hpp
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

#ifndef R_R_OPTIONS_HPP
#define R_R_OPTIONS_HPP

#include <string>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RErrorCategory.hpp>

// IMPORTANT NOTE: all code in r::options must provide "no jump" guarantee.
// See comment in RInternal.hpp for more info on this

namespace core {
   class FilePath;
}

namespace r {
namespace options {
   
core::Error saveOptions(const core::FilePath& filePath);  
core::Error restoreOptions(const core::FilePath& filePath);

// console width
extern const int kDefaultWidth;
void setOptionWidth(int width);
int getOptionWidth();

// generic get and set   
   
SEXP getOption(const std::string& name);
   
template <typename T>
T getOption(const std::string& name, const T& defaultValue = T())
{
   // prevent Rf_install from encountering error/longjmp condition
   if (name.empty())
      return defaultValue;
   
   SEXP valueSEXP = getOption(name);
   if (valueSEXP != R_NilValue)
   {
      T value;
      core::Error error = sexp::extract(valueSEXP, &value);
      if (error)
      {
         error.addProperty("symbol (option)", name);
         LOG_ERROR(error);
         return defaultValue;
      }
      
      return value;
   }
   else
   {
      core::Error error(errc::SymbolNotFoundError, ERROR_LOCATION);
      error.addProperty("symbol (option)", name);
      LOG_ERROR(error);
      return defaultValue;
   }
}
   
template <typename T>
core::Error setOption(const std::string& name, const T& value)
{
   r::exec::RFunction optionsFunction("options");
   optionsFunction.addParam(name, value);
   core::Error error = optionsFunction.call();
   if (error)
   {
      error.addProperty("option-name", name);
      return error;
   }
   else
   {
      return core::Success();
   }
}

SEXP setOption(SEXP tag, SEXP value);


} // namespace options   
} // namespace r


#endif // R_R_OPTIONS_HPP 

