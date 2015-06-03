/*
 * RActiveSessions.cpp
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

#include <core/r_util/RActiveSessions.hpp>

#include <boost/foreach.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/system/System.hpp>

#define kSessionDirPrefix "session-"

namespace rstudio {
namespace core {
namespace r_util {

namespace {


} // anonymous namespace


Error ActiveSessions::create(const std::string& project, std::string* pId)
{
   return create(project, project, pId);
}

Error ActiveSessions::create(const std::string& project,
                             const std::string& workingDir,
                             std::string* pId)
{
   // generate a new id
   std::string id = core::system::generateShortenedUuid();

   // create the directory
   FilePath dir = storagePath_.childPath(kSessionDirPrefix + id);
   Error error = dir.ensureDirectory();
   if (error)
      return error;

   // write initial settings
   ActiveSession activeSession(dir);
   activeSession.setProject(project);
   activeSession.setWorkingDir(workingDir);

   // return the id
   *pId = id;
   return Success();
}

std::vector<boost::shared_ptr<ActiveSession> > ActiveSessions::list()
{
   // list to return
   std::vector<boost::shared_ptr<ActiveSession> > sessions;

   // enumerate children and check for sessions
   std::vector<FilePath> children;
   Error error = storagePath_.children(&children);
   if (error)
   {
      LOG_ERROR(error);
      return sessions;
   }
   std::string prefix = kSessionDirPrefix;
   BOOST_FOREACH(const FilePath& child, children)
   {
      if (boost::algorithm::starts_with(child.filename(), prefix))
      {
         std::string id = child.filename().substr(prefix.length());
         boost::shared_ptr<ActiveSession> pSession = get(id);
         if (pSession->hasRequiredProperties())
         {
            sessions.push_back(pSession);
         }
         else
         {
            // this session isn't valid so destroy it
            Error error = pSession->destroy();
            if (error)
               LOG_ERROR(error);
         }
      }

   }

   return sessions;
}

boost::shared_ptr<ActiveSession> ActiveSessions::get(const std::string& id)
{
   FilePath scratchPath = storagePath_.childPath(kSessionDirPrefix + id);
   if (scratchPath.exists())
      return boost::shared_ptr<ActiveSession>(new ActiveSession(scratchPath));
   else
      return emptySession();
}


boost::shared_ptr<ActiveSession> ActiveSessions::emptySession()
{
   return boost::shared_ptr<ActiveSession>(new ActiveSession());
}

} // namespace r_util
} // namespace core 
} // namespace rstudio



