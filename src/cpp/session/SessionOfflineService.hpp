/*
 * SessionOfflineService.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#ifndef SESSION_OFFLINE_SERVICE_HPP
#define SESSION_OFFLINE_SERVICE_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/BoostThread.hpp>

/*
namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}
*/

namespace rstudio {
namespace session {

// singleton
class OfflineService;
OfflineService& offlineService();

class OfflineService : boost::noncopyable
{
private:
   OfflineService() {}
   friend OfflineService& offlineService();

public:
   core::Error start();
   void stop();

private:
   void run();
  
private:
   boost::thread offlineThread_;
};


} // namespace session
} // namespace rstudio

#endif // SESSION_OFFLINE_SERVICE_HPP
