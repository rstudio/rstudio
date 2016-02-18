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

#include <core/system/Crypto.hpp>

#include <session/SessionOptions.hpp>

#include <boost/regex.hpp>
#include <boost/foreach.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

core::Error saveChunkScripts(const std::string& contents, 
                             const FilePath& cacheFolder,
                             std::string* pHeader)
{
   const char* dataMarker = "data:application/x-javascript;base64,";
   std::vector<std::string> scripts;
   Error error = extractScriptTags(contents, &scripts);
   if (error)
      return error;

   // ensure we have a folder to save scripts to
   cacheFolder.complete(kChunkLibDir).ensureDirectory();

   int scriptId = 0;
   BOOST_FOREACH(const std::string& script, scripts)
   {
      // move to next ID
      scriptId++;

      // ignore non-self-contained scripts
      if (script.substr(0, strlen(dataMarker)) != dataMarker)
      {
         LOG_WARNING_MESSAGE("Skipping non-self-contained script: " +
               script.substr(0, std::max(strlen(dataMarker), 
                                         static_cast<size_t>(256))));
         continue;
      }

      // decode the script contents
      std::vector<unsigned char> scriptContents;
      error = core::system::crypto::base64Decode(script.substr(strlen(dataMarker), 
               std::string::npos), &scriptContents);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      // associate with library file
      std::string id = safe_convert::numberToString(scriptId);
      FilePath libFile = cacheFolder.complete(kChunkLibDir).complete(
            "script" + id + ".js");
      boost::shared_ptr<std::ostream> pStream;
      error = libFile.open_w(&pStream, true);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      // output script contents to library file
      try
      {
         pStream->write(reinterpret_cast<char*>(&scriptContents[0]), 
                        scriptContents.size());
      }
      CATCH_UNEXPECTED_EXCEPTION

      // append to header
      pHeader->append("<script src=\"" kChunkLibDir "/" + libFile.filename() +
                      "\"></script>\n");
   }

   return Success();
}

core::Error saveChunkHtml(const std::string& chunkId,
                          const std::string& header,
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
   std::string headerContents;
   FilePath headerHtml = options().rResourcesPath().complete("notebook").
      complete("in_header.html");
   error = readStringFromFile(headerHtml, &headerContents);
   if (error)
      return error;

   // append caller specified header
   headerContents.append(header);

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
                          const std::string& headerContents,
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
         {
            LOG_WARNING_MESSAGE("Unexpected chunk marker: " + match.str(0) + 
                  " (expected terminator " + safe_convert::numberToString(id) + 
                  ")");
            continue;
         }

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
               headerContents,
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
   std::string header;
   error = saveChunkScripts(contents, cacheFolder, &header);
   if (error)
      return error;
   error = extractChunks(contents, header, rnbFile, cacheFolder);
   if (error) 
      return error;

   return Success();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

