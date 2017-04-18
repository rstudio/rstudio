/*
 * SessionContentUrls.cpp
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

#include <session/SessionContentUrls.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/Exec.hpp>

#include <core/system/System.hpp>

#include <core/http/Util.hpp>
#include <core/http/Response.hpp>
#include <core/http/Request.hpp>

#include <core/json/JsonRpc.hpp>

extern "C" const char *locale2charset(const char *);

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace session {
namespace content_urls {

namespace {

FilePath contentUrlPath()
{
   FilePath filePath = module_context::userScratchPath().complete("content_urls");
   Error error = filePath.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return filePath;
}

std::string buildContentUrl(const std::string& title,
                            const std::string& contentFile)
{
   // sanitize and encode title
   std::string encodedTitle =
            http::util::urlEncode(string_utils::htmlEscape(title), true);

   // calculate and return content url
   std::string url = std::string("content") +
                     "?title=" + encodedTitle +
                     "&file=" + http::util::urlEncode(contentFile, true);
   return url;
}

Error contentFileInfo(const std::string& contentUrl,
                      std::string* pTitle,
                      core::FilePath* pFilePath)
{
   // extract and parse query string
   std::string queryString;
   std::string::size_type pos = contentUrl.find('?');
   if (pos != std::string::npos)
   {
      if ((pos+1) < contentUrl.length())
         queryString = contentUrl.substr(pos+1);
      else
         return systemError(ENOENT, ERROR_LOCATION);
   }
   else
   {
      return systemError(ENOENT, ERROR_LOCATION);
   }
   http::Fields fields;
   http::util::parseQueryString(queryString, &fields);

   // get title parameter
   *pTitle = http::util::fieldValue<std::string>(fields, "title", "(Untitled)");

   // use only the first line of the title 
   size_t newline = pTitle->find("\n");
   if (newline != std::string::npos)
      *pTitle = pTitle->substr(0, newline);
   
   // get file parameter
   std::string contentFile = http::util::fieldValue(fields, "file");
   if (contentFile.empty())
       return systemError(ENOENT, ERROR_LOCATION);
   *pFilePath = contentUrlPath().complete(contentFile);

   // return success
   return Success();
}


} // anonymous namespace


std::string provision(const std::string& title, const FilePath& filePath)
{
   // calculate content path
   std::string contentFile = core::system::generateUuid(false) +
                             filePath.extension();
   FilePath contentPath = contentUrlPath().complete(contentFile);

   // copy the file
   Error error = filePath.copy(contentPath);
   if (error)
      LOG_ERROR(error);

   // calculate and return content url
   return buildContentUrl(title, contentFile);
}

std::string provision(const std::string& title,
                      const std::string& content,
                      const std::string& extension)
{
   // calculate content path
   std::string contentFile = core::system::generateUuid(false) + extension;
   FilePath contentPath = contentUrlPath().complete(contentFile);

   // write the file
   Error error = writeStringToFile(contentPath, content);
   if (error)
      LOG_ERROR(error);

   // calculate and return content url
   return buildContentUrl(title, contentFile);
}



void handleContentRequest(const http::Request& request, http::Response* pResponse)
{
   // get content file info
   std::string title;
   FilePath contentFilePath;
   Error error = contentFileInfo(request.uri(), &title, &contentFilePath);
   if (error)
   {
      pResponse->setError(error);
      return;
   }

   // set private cache forever headers
   pResponse->setPrivateCacheForeverHeaders();

   // read file
   std::string contents;
   error = core::readStringFromFile(contentFilePath, &contents);
   if (error)
   {
      pResponse->setError(error);
      return;
   }

   // Ensure we are over the Qt iframe (?) minimum of 513 bytes for our custom
   // network reply handler to work correctly (for this case and apparently
   // this case only if bytesAvailable is less than 512 then Qt never calls
   // readData after the call to bytesAvailable). This is the behavior we used
   // to see in Qt 4.X and for that case we had a special workaround (see
   // https://github.com/rstudio/rstudio/blob/master/src/cpp/desktop/DesktopNetworkReply.cpp#L186-L195)
   //
   // Note that we can't just restore the workaround because the IDE didn't
   // even come up with the workaround in place! (due to RPC calls that
   // were less than 512 bytes). I'm not sure why this case and this case
   // only presents problems -- it may be because it's a request in an
   // iframe? (I tried to find other iframes where we might have less than
   // 512 bytes -- e.g. Plots, Help but couldn't even construct a case
   // where the payload was that small)
   if (contents.length() <= 512)
   {
      std::string fill(512 - contents.length() + 1, ' ');
      contents.append(fill);
   }

   // set contents
   pResponse->setContentType(contentFilePath.mimeContentType());
   pResponse->setBody(contents);

   bool isUtf8 = true;
   if (boost::algorithm::starts_with(contentFilePath.mimeContentType(), "text/"))
   {
      // If the content looks like valid UTF-8, assume it is. Otherwise, assume
      // it's the system encoding.
      std::string contents;
      error = core::readStringFromFile(contentFilePath, &contents);
      if (!error)
      {
         for (std::string::iterator pos = contents.begin(); pos != contents.end(); )
         {
            error = string_utils::utf8Advance(pos, 1, contents.end(), &pos);
            if (error)
            {
               isUtf8 = false;
               break;
            }
         }
      }
   }

   // reset content-type with charset
   pResponse->setContentType(contentFilePath.mimeContentType() +
                             std::string("; charset=") +
                             (isUtf8 ? "UTF-8" : ::locale2charset(NULL)));

   // set title header
   pResponse->setHeader("Title", title);
}

Error removeContentUrl(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // get content url
   std::string contentUrl;
   Error error = json::readParams(request.params, &contentUrl);
   if (error)
      return error;

   // get content file info
   std::string title;
   FilePath contentFilePath;
   error = contentFileInfo(contentUrl, &title, &contentFilePath);
   if (error)
      return error;

   // remove it
   return contentFilePath.removeIfExists();
}


Error initialize()
{
   using boost::bind;
   using namespace session::module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerUriHandler, "/content", handleContentRequest))
      (bind(registerRpcMethod, "remove_content_url", removeContentUrl));
   return initBlock.execute();
}
   
   
} // namespace content_urls
} // namespace session
} // namespace rstudio

