/*
 * SessionNodeTools.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "SessionNodeTools.hpp"
#include "SessionLogging.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Process.hpp>

#include <session/prefs/UserPrefs.hpp>

#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <string>
#include <utility>
#include <vector>

#include <boost/algorithm/string/case_conv.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/asio/ip/address_v6.hpp>

#include <r/ROptions.hpp>
#include <r/RSexp.hpp>

#include <session/SessionOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace node_tools {

namespace {

#ifndef _WIN32
# define kNodeExe "node"
#else
# define kNodeExe "node.exe"
#endif

/**
 * If running on arm64 Mac, substitute the arm64-specific node binary; returns true if this was
 * done, false otherwise.
 */
bool findNodeMacArm64(const FilePath& inputPath, FilePath* pOutputNodePath)
{
#if defined(__APPLE__)
   if (system::isAppleSilicon())
   {
      FilePath nodeExePath;
      if (inputPath.isRegularFile())
      {
         // change /node/bin/node to /node-arm64/bin/node
         nodeExePath = inputPath.getParent().getParent().completeChildPath("node-arm64/bin/" kNodeExe);
      }
      else if (inputPath.isDirectory())
      {
         // change /node to /node-arm64
         nodeExePath = inputPath.getParent().completeChildPath("node-arm64");
      }
      else
         return false;

      if (nodeExePath.exists())
      {
         *pOutputNodePath = nodeExePath;
         return true;
      }
   }
#endif
   return false;
}

} // end anonymous namespace

Error findNode(FilePath* pNodePath, const std::string& rOptionName)
{
   // Check R option override if provided
   if (!rOptionName.empty())
   {
      SEXP nodePathSEXP = r::options::getOption(rOptionName);
      if (nodePathSEXP != R_NilValue)
      {
         std::string nodePath = r::sexp::asString(nodePathSEXP);
         if (!nodePath.empty())
         {
            *pNodePath = FilePath(nodePath);
            if (pNodePath->exists())
            {
               DLOG("Found node via R option '{}': '{}'.",
                    rOptionName, pNodePath->getAbsolutePath());
               return Success();
            }

            WLOG("R option '{}' set to '{}', but path does not exist.",
                 rOptionName, nodePath);
         }
      }
   }

   // Check admin-configured path
   if (!session::options().nodePath().isEmpty())
   {
      FilePath nodePath = session::options().nodePath();
      DLOG("Checking admin-configured node path: '{}'.",
           nodePath.getAbsolutePath());

      FilePath arm64NodePath;

      // on arm64 Mac, substitute the arm64-specific node binary
      if (findNodeMacArm64(nodePath, &arm64NodePath))
      {
         DLOG("Substituted arm64 node path: '{}'.",
              arm64NodePath.getAbsolutePath());
         nodePath = arm64NodePath;
      }

      // Allow both directories containing a 'node' binary, and the path
      // to a 'node' binary directly.
      if (nodePath.isDirectory())
      {
         for (auto&& suffix : { "bin/" kNodeExe, kNodeExe })
         {
            FilePath nodeExePath = nodePath.completeChildPath(suffix);
            if (nodeExePath.exists())
            {
               DLOG("Found node in admin-configured directory: '{}'.",
                    nodeExePath.getAbsolutePath());
               *pNodePath = nodeExePath;
               return Success();
            }
         }

         WLOG("Admin-configured node directory '{}' does not contain a '{}' binary.",
              nodePath.getAbsolutePath(), kNodeExe);
         return Error(fileNotFoundError(nodePath, ERROR_LOCATION));
      }
      else if (nodePath.isRegularFile())
      {
         DLOG("Found node at admin-configured path: '{}'.",
              nodePath.getAbsolutePath());
         *pNodePath = nodePath;
         return Success();
      }
      else
      {
         WLOG("Admin-configured node path '{}' does not exist.",
              nodePath.getAbsolutePath());
         return Error(fileNotFoundError(nodePath, ERROR_LOCATION));
      }
   }

   // Search PATH
   DLOG("No admin-configured node path; searching PATH for '{}'.", kNodeExe);
   Error error = system::findProgramOnPath(kNodeExe, pNodePath);
   if (error)
   {
      WLOG("'{}' not found on PATH.", kNodeExe);
      return error;
   }

   DLOG("Found node on PATH: '{}'.", pNodePath->getAbsolutePath());
   return Success();
}

namespace {

// Whether `token` appears in `options` as a whole, whitespace-delimited token
// (so "--use-system-ca" does not match inside "--use-system-cafoo").
bool containsOptionToken(const std::string& options, const std::string& token)
{
   if (token.empty())
      return false;

   std::string::size_type pos = 0;
   while ((pos = options.find(token, pos)) != std::string::npos)
   {
      bool atStart = (pos == 0) ||
         std::isspace(static_cast<unsigned char>(options[pos - 1]));
      std::string::size_type end = pos + token.size();
      bool atEnd = (end == options.size()) ||
         std::isspace(static_cast<unsigned char>(options[end]));
      if (atStart && atEnd)
         return true;
      pos = end;
   }
   return false;
}

} // anonymous namespace

std::string appendNodeOption(const std::string& existingOptions,
                             const std::string& option)
{
   // Preserve the caller's NODE_OPTIONS verbatim (quoted values may contain
   // intentional whitespace); only append our flag when it is not already
   // present as a whole token. No tokenize/rejoin, so nothing is rewritten.
   if (containsOptionToken(existingOptions, option))
      return existingOptions;
   if (existingOptions.empty())
      return option;
   return existingOptions + " " + option;
}

bool parseNodeVersion(const std::string& versionOutput, int* pMajor, int* pMinor)
{
   std::string trimmed = boost::algorithm::trim_copy(versionOutput);
   if (!trimmed.empty() && (trimmed[0] == 'v' || trimmed[0] == 'V'))
      trimmed = trimmed.substr(1);

   int major = 0, minor = 0;
   if (std::sscanf(trimmed.c_str(), "%d.%d", &major, &minor) != 2)
      return false;

   if (major < 0 || minor < 0)
      return false;

   *pMajor = major;
   *pMinor = minor;
   return true;
}

bool versionSupportsSystemCa(int major, int minor)
{
   // Patch is intentionally ignored: the floor (22.17.0) is a .0 release, so
   // major.minor fully determines support.
   return (major > 22) || (major == 22 && minor >= 17);
}

bool nodeSupportsSystemCa(const core::FilePath& nodePath)
{
   DLOG("Probing node version at '{}' for --use-system-ca support.",
        nodePath.getAbsolutePath());

   core::system::ProcessOptions options;
   core::system::ProcessResult result;
   Error error = core::system::runProgram(nodePath.getAbsolutePath(),
                                          { "--version" },
                                          options,
                                          &result);
   if (error)
   {
      WLOG("Could not determine node version at '{}': {}",
           nodePath.getAbsolutePath(), error.getMessage());
      return false;
   }

   if (result.exitStatus != EXIT_SUCCESS)
   {
      WLOG("node --version at '{}' exited with status {}.",
           nodePath.getAbsolutePath(), result.exitStatus);
      return false;
   }

   int major = 0, minor = 0;
   if (!parseNodeVersion(result.stdOut, &major, &minor))
   {
      WLOG("Could not parse node version from output '{}'.", result.stdOut);
      return false;
   }

   bool supported = versionSupportsSystemCa(major, minor);
   if (!supported)
   {
      WLOG("Node {}.{} at '{}' does not support --use-system-ca via NODE_OPTIONS "
           "(requires 22.17.0+); the system certificate store will not be trusted.",
           major, minor, nodePath.getAbsolutePath());
   }

   return supported;
}

void applySystemCaOption(core::system::Options* pEnvironment,
                         const core::FilePath& nodePath)
{
   // Trust the OS certificate store when the user has opted in and the resolved
   // Node supports the flag. Additive to NODE_EXTRA_CA_CERTS; preserves any
   // existing NODE_OPTIONS. Guarded on the Node version because
   // NODE_OPTIONS=--use-system-ca is rejected by Node < 22.17.0 (it would
   // otherwise break agent startup).
   if (!prefs::userPrefs().assistantUseSystemCa())
      return;
   if (!nodeSupportsSystemCa(nodePath))
      return;

   std::string nodeOptions = core::system::getenv(*pEnvironment, "NODE_OPTIONS");
   core::system::setenv(pEnvironment, "NODE_OPTIONS",
                        appendNodeOption(nodeOptions, "--use-system-ca"));
}

namespace {

// Whether every run of percent-escapes in a proxy URL's credentials decodes
// to well-formed UTF-8. Node hands the credentials to decodeURIComponent(),
// which throws on anything else.
bool hasValidPercentEscapes(const std::string& userinfo)
{
   std::string decoded;
   for (std::size_t i = 0; i <= userinfo.size();)
   {
      if (i < userinfo.size() && userinfo[i] == '%')
      {
         if (i + 2 >= userinfo.size() ||
             !std::isxdigit(static_cast<unsigned char>(userinfo[i + 1])) ||
             !std::isxdigit(static_cast<unsigned char>(userinfo[i + 2])))
         {
            return false;
         }

         decoded.push_back(static_cast<char>(std::stoi(userinfo.substr(i + 1, 2), nullptr, 16)));
         i += 3;
         continue;
      }

      // the run of escapes (if any) ends here
      if (!string_utils::isValidUtf8(decoded))
         return false;
      decoded.clear();
      i += 1;
   }

   return true;
}

bool isDottedQuad(const std::vector<std::string>& labels)
{
   if (labels.size() != 4)
      return false;

   for (const std::string& label : labels)
   {
      if (label.empty() || label.size() > 3)
         return false;
      if (label.find_first_not_of("0123456789") != std::string::npos)
         return false;

      // a leading zero makes the part octal to Node; not worth supporting
      if (label.size() > 1 && label[0] == '0')
         return false;
      if (std::stoi(label) > 255)
         return false;
   }

   return true;
}

bool isValidProxyHost(const std::string& host)
{
   if (host.empty())
      return false;

   if (host.front() == '[')
   {
      // IPv6 literal. Zone identifiers and embedded IPv4 are left out: Node
      // rejects the former and is stricter than the system parser on the latter.
      if (host.size() < 3 || host.back() != ']')
         return false;

      std::string address = host.substr(1, host.size() - 2);
      if (address.find_first_not_of("0123456789abcdefABCDEF:") != std::string::npos)
         return false;

      boost::system::error_code ec;
      boost::asio::ip::make_address_v6(address, ec);
      return !ec;
   }

   static const char* const kHostCharacters =
      "abcdefghijklmnopqrstuvwxyz"
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      "0123456789-._";
   if (host.find_first_not_of(kHostCharacters) != std::string::npos)
      return false;

   // Node's URL parser reads a host whose last label is a number (decimal, or
   // hex with an 0x prefix) as an IPv4 address, and rejects the URL when it is
   // not one. It takes more spellings than a plain dotted quad, but those are
   // not worth the risk of accepting something Node will not.
   std::vector<std::string> labels;
   boost::algorithm::split(labels, host, boost::algorithm::is_any_of("."));

   std::string last = labels.back();
   if (last.empty() && labels.size() > 1)
      last = labels[labels.size() - 2];

   bool isNumber = false;
   if (last.size() >= 2 && last[0] == '0' && (last[1] == 'x' || last[1] == 'X'))
      isNumber = last.find_first_not_of("0123456789abcdefABCDEF", 2) == std::string::npos;
   else
      isNumber = !last.empty() && last.find_first_not_of("0123456789") == std::string::npos;

   return !isNumber || isDottedQuad(labels);
}

bool isValidProxyPort(const std::string& port)
{
   if (port.find_first_not_of("0123456789") != std::string::npos)
      return false;

   // empty (the scheme's default) or zero
   std::string::size_type first = port.find_first_not_of('0');
   if (first == std::string::npos)
      return true;

   std::string digits = port.substr(first);
   return digits.size() <= 5 && std::stoi(digits) <= 65535;
}

} // anonymous namespace

bool normalizeProxyUrl(const std::string& value, std::string* pNormalized)
{
   std::string proxy = boost::algorithm::trim_copy(value);

   // A value quoted where the shell would not unquote it (a Windows
   // environment variable, say) reaches Node with its quotes intact.
   if (proxy.size() >= 2 &&
       (proxy.front() == '"' || proxy.front() == '\'') &&
       proxy.back() == proxy.front())
   {
      proxy = boost::algorithm::trim_copy(proxy.substr(1, proxy.size() - 2));
   }

   if (proxy.empty())
   {
      pNormalized->clear();
      return true;
   }

   std::string::size_type schemeEnd = proxy.find("://");
   if (schemeEnd == std::string::npos)
   {
      proxy = "http://" + proxy;
      schemeEnd = 4;
   }

   // Node exits during startup on a proxy URL it cannot use, and what it
   // rejects is open-ended (other schemes, a path or query, a bad port or
   // host, malformed escapes in the credentials). So accept only the one form
   // it is known to take,
   //
   //    http(s)|socks5 :// [userinfo@] host [:port] [/]
   //
   // and let everything else fall back to no proxy.
   std::string scheme = boost::algorithm::to_lower_copy(proxy.substr(0, schemeEnd));
   if (scheme != "http" && scheme != "https" && scheme != "socks5")
      return false;

   std::string authority = proxy.substr(schemeEnd + 3);
   if (!authority.empty() && authority.back() == '/')
      authority.pop_back();
   if (authority.find_first_of("/?#\\") != std::string::npos)
      return false;

   // Whitespace is fine in the credentials, which Node percent-encodes.
   std::string hostAndPort = authority;
   std::string::size_type at = authority.rfind('@');
   if (at != std::string::npos)
   {
      if (!hasValidPercentEscapes(authority.substr(0, at)))
         return false;
      hostAndPort = authority.substr(at + 1);
   }

   // An IPv6 literal carries colons of its own, so the port starts after it.
   std::string::size_type hostEnd = 0;
   if (!hostAndPort.empty() && hostAndPort.front() == '[')
   {
      hostEnd = hostAndPort.find(']');
      if (hostEnd == std::string::npos)
         return false;
      hostEnd += 1;
   }
   else
   {
      hostEnd = hostAndPort.find(':');
      if (hostEnd == std::string::npos)
         hostEnd = hostAndPort.size();
   }

   std::string port;
   if (hostEnd < hostAndPort.size())
   {
      if (hostAndPort[hostEnd] != ':')
         return false;
      port = hostAndPort.substr(hostEnd + 1);
   }

   if (!isValidProxyHost(hostAndPort.substr(0, hostEnd)) || !isValidProxyPort(port))
      return false;

   *pNormalized = proxy;
   return true;
}

void applyEnvProxyOption(core::system::Options* pEnvironment)
{
   // NODE_USE_ENV_PROXY=1 makes Node.js 22.21+ route fetch() through
   // http_proxy / https_proxy (https://github.com/nodejs/node/pull/57165).
   // Node parses those variables during startup and exits on a value it
   // cannot use, including one without a scheme, which curl and R accept. So
   // repair the child's copy first, and when a value cannot be repaired,
   // leave proxy support off (also dropping a flag inherited from the
   // session) rather than take the child down with it. Values are kept out of
   // the log: they may carry credentials.
   static const std::pair<const char*, const char*> kProxyVariables[] = {
      { "http_proxy", "HTTP_PROXY" },
      { "https_proxy", "HTTPS_PROXY" }
   };

   for (const auto& spelling : kProxyVariables)
   {
      // Node prefers the lowercase spelling. fetch() stops at it whenever it
      // is set, but the http module falls through an empty one to the
      // uppercase spelling, so that one is read whenever the lowercase value
      // is (or normalizes to) empty.
      for (const char* name : { spelling.first, spelling.second })
      {
         std::string value = core::system::getenv(*pEnvironment, name);

         std::string normalized;
         if (!normalizeProxyUrl(value, &normalized))
         {
            WLOG("Not enabling Node.js proxy support: the {} environment variable "
                 "is not a proxy URL that Node.js accepts.", name);
            core::system::unsetenv(pEnvironment, "NODE_USE_ENV_PROXY");
            return;
         }

         if (normalized != value)
         {
            DLOG("Normalized the {} environment variable for Node.js.", name);
            core::system::setenv(pEnvironment, name, normalized);
         }

         if (!normalized.empty())
            break;
      }
   }

   core::system::setenv(pEnvironment, "NODE_USE_ENV_PROXY", "1");
}

} // namespace node_tools
} // namespace modules
} // namespace session
} // namespace rstudio
