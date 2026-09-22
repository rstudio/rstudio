/*
 * PosixGroup.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_SYSTEM_POSIX_GROUP_HPP
#define CORE_SYSTEM_POSIX_GROUP_HPP

#include <string>
#include <vector>
#include <unistd.h>
#include "PosixUser.hpp"

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace core {
namespace system {
namespace group {

struct Group
{
   GidType groupId;
   std::string name;
   std::vector<std::string> members;
};

Error groupFromName(const std::string& name, Group* pGroup);
Error groupFromId(gid_t gid, Group* pGroup);

// the ids of the groups the user belongs to, from a cache refreshed every few
// minutes; a failed lookup is logged and yields whatever it filled in
std::vector<GidType> userGroupIds(const User& user);

// the same list, read from the group database each time (as initgroups(3)
// would), with a failed lookup reported to the caller
Error queryUserGroupIds(const User& user, std::vector<GidType>* pGroupIds);
Error userGroups(const std::string& userName, std::vector<Group>* pGroups);
void removeUserFromGroupCache(const std::string& username);


} // namespace group
} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_POSIX_GROUP_HPP

