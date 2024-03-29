/*
 * SessionProjectSharing.hpp
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

#ifndef SESSION_PROJECTS_SHARING_HPP
#define SESSION_PROJECTS_SHARING_HPP

#define kRStudioDisableProjectSharing "RSTUDIO_DISABLE_PROJECT_SHARING"
#define kProjectEntryExt ".proj"
#define kProjectSharedDir "shared-projects"

// a server option passed to the sesion
#define kSessionSharedStoragePath "server-shared-storage-path"

#define kProjectEntryDir        "project_dir"
#define kProjectEntryFile       "project_file"
#define kProjectEntryOwner      "project_owner"
#define kProjectEntryUpdatedBy  "updated_by"
#define kProjectEntrySharedWith "shared_with"
#define kProjectEntryShareTimes "share_times"
#define kProjectEntryShareUser  "user"
#define kProjectEntryShareTime  "time"

namespace rstudio {
namespace session {
namespace projects {


} // namespace projects
} // namespace session
} // namespace rstudio

#endif // SESSION_PROJECTS_SHARING_HPP
