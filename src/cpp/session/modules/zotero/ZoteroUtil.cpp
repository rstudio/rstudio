/*
 * ZoteroUtil.cpp
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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

#include "ZoteroUtil.hpp"

#include <iostream>

#include <r/ROptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

const char * const kMyLibrary = "My Library";

#ifndef NDEBUG
void TRACE(const std::string& message, boost::optional<std::size_t> items)
{
   if (r::options::getOption<bool>("rstudio.zotero.trace", false, false))
   {
      std::cerr << "[Zotero] " << message;
      if (items)
         std::cerr << " [" << *items << " items]";
      std::cerr << std::endl;
   }
}
#else
void TRACE(const std::string&, boost::optional<std::size_t>)
{
}
#endif


} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
