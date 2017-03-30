/*
 * URL.cpp
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

#include <core/http/URL.hpp>
#include <core/RegexUtils.hpp>

#include <iostream>

#include <boost/algorithm/string.hpp>
#include <boost/regex.hpp>

namespace rstudio {
namespace core {
namespace http {

URL::URL(const std::string& absoluteURL)
{
   std::string protocol, host, path;
   boost::regex re("(http|https)://([^/#?]+)(.*)", boost::regex::icase);
   boost::cmatch matches ;
   if (regex_utils::match(absoluteURL.c_str(), matches, re))
   {
      protocol = matches[1];
      host = matches[2];
      path = matches[3];
   }
   
   // thwart ref-counting
   assign(absoluteURL, protocol, host, path); 
}
   
void URL::split(std::string* pBaseURL, std::string* pQueryParams) const
{
   if (empty())
      return;
   
   // look for '?'
   std::string::size_type pos = absoluteURL_.find('?');
   if (pos != std::string::npos)
   {
      *pBaseURL = absoluteURL_.substr(0, pos);
      if ((pos+1) < absoluteURL_.length())
         *pQueryParams = absoluteURL_.substr(pos+1);
   }
   else
   {
      *pBaseURL = absoluteURL_;
   }
}
   
std::ostream& operator << (std::ostream& stream, const URL& url)
{
   stream << url.absoluteURL();
   return stream;
}

namespace {

class Path
{
public:
   Path(std::string path)
   {
      if (path.empty())
      {
         rooted_ = false;
         return;
      }
      
      rooted_ = path.at(0) == '/';
      if (rooted_)
         path = path.substr(1);

      size_t lastSlash = path.find_last_of('/');
      if (lastSlash == std::string::npos)
      {
         file_ = path;
         path.clear();
      }
      else if (lastSlash < path.length() - 1)
      {
         file_ = path.substr(lastSlash + 1);
         path.erase(lastSlash);
      }

      boost::algorithm::split(dirs_,
                              path,
                              boost::is_any_of("/"),
                              boost::algorithm::token_compress_off);
   }

   std::string value() const
   {
      std::string output;
      if (rooted_)
         output.push_back('/');
      for (size_t i = 0; i < dirs_.size(); i++)
      {
         output.append(dirs_.at(i));
         output.push_back('/');
      }
      output.append(file_);
      return output;
   }

   void cleanup()
   {
      std::vector<std::string> result;
      for (size_t i = 0; i < dirs_.size(); i++)
      {
         if (dirs_.at(i).empty() || dirs_.at(i) == ".")
            continue;

         if (dirs_.at(i) == "..")
         {
            if (!result.empty() && result.back() != "..")
            {
               result.pop_back();
               continue;
            }
         }

         result.push_back(dirs_.at(i));
      }
      dirs_ = result;
   }

   bool rooted() const { return rooted_; }
   void setRooted(bool rooted) { rooted_ = rooted; }

   std::string file() const { return file_; }
   void setFile(const std::string& file) { file_ = file; }

   std::vector<std::string> dirs() { return dirs_; }
   void setDirs(std::vector<std::string> dirs) { dirs_ = dirs; }

private:
   bool rooted_;
   std::vector<std::string> dirs_;
   std::string file_;
};

void splitParts(const std::string pathInfo, std::string* path, std::string* extraInfo)
{
   size_t end = pathInfo.find_first_of('?');
   if (end == std::string::npos)
      end = pathInfo.find_first_of('#');

   if (end == std::string::npos)
   {
      if (path)
         *path = pathInfo;
      if (extraInfo)
         *extraInfo = std::string();
   }
   else
   {
      if (path)
         *path = pathInfo.substr(0, end);
      if (extraInfo)
         *extraInfo = pathInfo.substr(end);
   }
}

// If a file path, strips the file. If a dir path (i.e. has trailing slash)
// then returns the path unchanged.
std::string getDir(std::string fileOrDirPath)
{
   splitParts(fileOrDirPath, &fileOrDirPath, NULL);
   size_t lastSlash = fileOrDirPath.find_last_of('/');
   if (lastSlash != std::string::npos)
      fileOrDirPath = fileOrDirPath.substr(0, lastSlash + 1);
   return fileOrDirPath;
}

std::string cleanupPath(std::string path)
{
   std::string suffix;
   splitParts(path, &path, &suffix);

   Path richPath(path);
   richPath.cleanup();
   return richPath.value() + suffix;
}

} // anonymous namespace
   
std::string URL::complete(std::string absoluteUri, std::string targetUri)
{
   URL uri(targetUri);
   if (uri.isValid())
      return targetUri;

   std::string prefix;
   std::string path;
   URL absUrl(absoluteUri);
   if (absUrl.isValid())
   {
      prefix = absUrl.protocol() + "://" + absUrl.host();
      path = getDir(absUrl.path());
      // Deal with URLs with no path at all (e.g. "http://www.example.com")
      if (path.size() == 0)
         path = "/";
   }
   else
   {
      path = getDir(absoluteUri);
   }

   if (!targetUri.empty() && targetUri.at(0) == '/')
      path = targetUri;
   else
      path = path + targetUri;

   return prefix + cleanupPath(path);
}

std::string URL::uncomplete(std::string baseUri, std::string targetUri)
{
   splitParts(baseUri, &baseUri, NULL);
   std::string targetPath, targetExtra;
   splitParts(targetUri, &targetPath, &targetExtra);

   Path from(baseUri);
   Path to(targetPath);

   if (!from.rooted())
      return targetPath;
   if (!to.rooted())
      to = Path(complete(baseUri, targetPath));

   from.cleanup();
   to.cleanup();

   std::vector<std::string> fromDirs = from.dirs();
   std::vector<std::string> toDirs = to.dirs();

   while (fromDirs.size() > 0 && toDirs.size() > 0)
   {
      if (fromDirs.front() == toDirs.front())
      {
         fromDirs.erase(fromDirs.begin());
         toDirs.erase(toDirs.begin());
      }
      else
         break;
   }

   for (size_t i = 0; i < fromDirs.size(); i++)
   {
      toDirs.insert(toDirs.begin(), "..");
   }

   to.setDirs(toDirs);
   to.setRooted(false);

   return to.value() + targetExtra;
}

void URL::test()
{
   BOOST_ASSERT(cleanupPath("") == "");
   BOOST_ASSERT(cleanupPath("/") == "/");
   BOOST_ASSERT(cleanupPath("./") == "");
   BOOST_ASSERT(cleanupPath("/./") == "/");
   BOOST_ASSERT(cleanupPath("/.") == "/.");
   BOOST_ASSERT(cleanupPath("/foo/../") == "/");
   BOOST_ASSERT(cleanupPath("foo/../") == "");
   BOOST_ASSERT(cleanupPath("/foo/bar/../../") == "/");
   BOOST_ASSERT(cleanupPath("foo/bar/../../") == "");
   BOOST_ASSERT(cleanupPath("/foo/bar/../../") == "/");
   BOOST_ASSERT(cleanupPath("/foo/bar/../..") == "/foo/..");
   BOOST_ASSERT(cleanupPath("/foo/?/../") == "/foo/?/../");
   BOOST_ASSERT(cleanupPath("/foo/#/../") == "/foo/#/../");
   BOOST_ASSERT(cleanupPath("/foo/?/../#/../") == "/foo/?/../#/../");

   BOOST_ASSERT(complete("http://www.example.com", "foo") == "http://www.example.com/foo");
   BOOST_ASSERT(complete("http://www.example.com/foo", "bar") == "http://www.example.com/bar");
   BOOST_ASSERT(complete("http://www.example.com/foo/", "bar") == "http://www.example.com/foo/bar");
   BOOST_ASSERT(complete("http://www.example.com:80/foo/", "/bar") == "http://www.example.com:80/bar");
   BOOST_ASSERT(complete("http://www.example.com:80/foo/bar", "baz/qux") == "http://www.example.com:80/foo/baz/qux");
   BOOST_ASSERT(complete("http://www.example.com:80/foo/bar", "../baz/qux") == "http://www.example.com:80/baz/qux");
   BOOST_ASSERT(complete("http://www.example.com:80/foo/bar/", "../baz/qux") == "http://www.example.com:80/foo/baz/qux");
   BOOST_ASSERT(complete("http://www.example.com:80/foo/bar/", "baz/../qux") == "http://www.example.com:80/foo/bar/qux");
   BOOST_ASSERT(complete("http://www.example.com:80/foo/bar", "http://baz") == "http://baz");

   BOOST_ASSERT(complete("foo/bar/", "baz/qux") == "foo/bar/baz/qux");
   BOOST_ASSERT(complete("foo/bar/", "../baz/qux") == "foo/baz/qux");
   BOOST_ASSERT(complete("../foo/bar/", "../baz/qux") == "../foo/baz/qux");
   BOOST_ASSERT(complete("../../foo/bar/", "../baz/qux") == "../../foo/baz/qux");

   BOOST_ASSERT(uncomplete("/foo/bar/baz", "/foo/qux/quux") == "../qux/quux");
   BOOST_ASSERT(uncomplete("/foo/bar/baz/", "/foo/qux/quux") == "../../qux/quux");
   BOOST_ASSERT(uncomplete("/bar/baz", "/qux/quux") == "../qux/quux");
}
 
} // namespace http
} // namespace core
} // namespace rstudio
