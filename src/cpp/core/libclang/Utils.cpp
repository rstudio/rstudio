/*
 * Utils.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/libclang/Utils.hpp>

#include <core/libclang/LibClang.hpp>

namespace rstudio {
namespace core {
namespace libclang {

// note that this function disposes the underlying CXString so it
// shouldn't be used after this call
std::string toStdString(CXString cxStr)
{
   const char* str = clang().getCString(cxStr);
   if (str != nullptr)
   {
      std::string stdString(str);
      clang().disposeString(cxStr);
      return stdString;
   }
   else
   {
      return std::string();
   }
}

} // namespace libclang
} // namespace core
} // namespace rstudio

