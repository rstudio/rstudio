/*
 * SessionRnbParser.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionRmdNotebook.hpp"
#include "SessionRnbParser.hpp"

#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>

#include <session/SessionOptions.hpp>

#include <boost/regex.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

core::Error saveChunkHtml(const std::string& chunkId,
                          const std::string& body, 
                          const FilePath& cacheFolder)
{
   // open the chunk HTML file for writing
   FilePath target = cacheFolder.complete(chunkId + ".html");
   boost::shared_ptr<std::ostream> pStream;
   Error error = target.open_w(&pStream, true);
   if (error)
      return error;

   // extract chunk header includes
   FilePath headerHtml = options().rResourcesPath().complete("notebook").
      complete("in_header.html");
   std::string headerContents;
   error = readStringFromFile(headerHtml, &headerContents);
   if (error)
      return error;

   *pStream << 
      "<html>\n"
      "<head>\n" <<
      // TODO: insert dependent scripts
      headerContents << 
      "</head>\n"
      "<body>\n" <<
      body <<
      "</body>\n"
      "</html>\n";

   return Success();
}

core::Error extractChunks(const std::string& contents,
                          const FilePath& docPath,
                          const FilePath& cacheFolder)
{
   Error error;
   int ordinal = 0;   
   std::string::const_iterator start, pos = contents.begin(); 
   boost::regex re("<!--\\s+rnb-chunk-(\\w+)-(\\d+)\\s+(\\d+)\\s+-->");
   boost::smatch match;
   json::Array chunkDefs;
   while (boost::regex_search(pos, contents.end(), match, re, 
                              boost::match_default))
   {
      int id = safe_convert::stringTo<int>(match.str(2), 0);
      if (match.str(1) == "start") 
      {
         start = match[0].second;
         ordinal = id;
      }
      else if (match.str(1) == "end")
      {
         if (id != ordinal) 
            continue;

         // create the chunk definition
         std::string chunkId("rnbchunk" + match.str(2));
         json::Object chunkDef;
         chunkDef["chunk_id"]  = chunkId;
         chunkDef["row"]       = safe_convert::stringTo<int>(match.str(3), 1) - 1;
         chunkDef["visible"]   = true;
         chunkDef["row_count"] = 1;
         chunkDefs.push_back(chunkDef);

         // save the chunk contents
         error = saveChunkHtml(chunkId, 
               std::string(start, match[0].first),
               cacheFolder);
         if (error)
            return error;
      }

      // move to the next match
      pos = match[0].second;
   }

   return setChunkDefs(docPath.absolutePath(), "", std::time(NULL), chunkDefs);
}


} // anonymous namespace

core::Error parseRnb(const core::FilePath& rnbFile, 
                     const core::FilePath& cacheFolder)
{
   std::string contents;
   Error error = readStringFromFile(rnbFile, &contents);
   if (error)
      return error;
   error = ensureCacheFolder(cacheFolder);
   if (error)
      return error;
   error = extractChunks(contents, rnbFile, cacheFolder);
   if (error) 
      return error;

   return Success();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

