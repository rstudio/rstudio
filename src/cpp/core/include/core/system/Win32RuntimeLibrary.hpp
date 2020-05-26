/*
 * Win32RuntimeLibrary.hpp
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

#ifndef CORE_WIN32_RUNTIME_LIBRARY_HPP
#define CORE_WIN32_RUNTIME_LIBRARY_HPP

namespace rstudio {
namespace core {
namespace runtime {

int errorNumber();

} // end namespace runtime
} // end namespace core
} // end namespace rstudio

#define MSVC_ERRNO (::rstudio::core::runtime::errorNumber())

#endif /* CORE_WIN32_RUNTIME_LIBRARY_HPP */
