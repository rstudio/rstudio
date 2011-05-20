/*
 * ServerAppArmor.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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

// NOTE: like the setuid related functions (e.g. temporarilyDropPriv)
// the App Armor API is global to the process and therefore not thread
// safe. Typically the enforceRestricted call will be made once during
// startup and then the dropRestricted call will be made after a fork
// into a child process (simillar to the restorePriv call)

namespace server {
namespace app_armor {
   
bool isAvailable();

bool isEnforcingRestricted();

core::Error enforceRestricted();

core::Error dropRestricted();

} // namespace app_armor
} // namespace server

#endif // SERVER_APP_ARMOR_HPP

