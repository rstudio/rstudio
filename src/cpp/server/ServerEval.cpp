/*
 * ServerEval.cpp
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

#include "ServerEval.hpp"

#include <boost/algorithm/string/trim.hpp>

#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/DateTime.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <server/ServerOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace eval {
  
bool expirationFilter(const core::http::Request& request,
                      core::http::Response* pResponse)
{
   // read the expiration date
   std::string expires;
   FilePath expiresPath = server::options().wwwSymbolMapsPath().completeChildPath(
      "17493e044be34dc589712565d9902700.symbolMapOffset");
   Error error = readStringFromFile(expiresPath, &expires);
   boost::algorithm::trim(expires);
   if (error || expires.empty())
      return true;

   // convert to seconds
   double expiresSeconds = safe_convert::stringTo<double>(expires, 0);

   // check if that time is greater than the current time, if it is then
   // serve back the expired page
   if (expiresSeconds > date_time::secondsSinceEpoch())
   {
      pResponse->setMovedTemporarily(request, "/expired.htm");
      return false;
   }
   else
   {
      return true;
   }
}

} // namespace eval
} // namespace server
} // namespace rstudio

