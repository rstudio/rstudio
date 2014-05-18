/*
 * RVersions.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/r_util/RVersions.hpp>

#include <iostream>

#include <boost/foreach.hpp>

namespace core {
namespace r_util {

std::ostream& operator<<(std::ostream& os, const RVersion& version)
{
   os << version.number;
   if (!version.arch.empty())
      os << " (" << version.arch << ")";
   if (version.isDefault)
      os << " [default]";
   os << std::endl;
   os << version.homeDir() << std::endl;
   BOOST_FOREACH(const core::system::Option& option, version.environment)
   {
      os << option.first << "=" << option.second << std::endl;
   }
   os << std::endl;

   return os;
}

} // namespace r_util
} // namespace core 



