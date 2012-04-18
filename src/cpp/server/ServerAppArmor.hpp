/*
 * ServerAppArmor.hpp
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

#ifndef SERVER_APP_ARMOR_HPP
#define SERVER_APP_ARMOR_HPP

#include <boost/utility.hpp>

namespace core {
   class Error;
}

namespace server {
namespace app_armor {
   
core::Error enforceRestricted();

} // namespace app_armor
} // namespace server

#endif // SERVER_APP_ARMOR_HPP

