/*
 * SessionCRANMirrors.cpp
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

#include "SessionCRANMirrors.hpp"

#include <boost/bind/bind.hpp>

#include <core/http/URL.hpp>
#include <core/http/TcpIpBlockingClient.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionOptions.hpp>
#include <session/prefs/UserPrefs.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace module_context {

namespace {

bool isSecure(const std::string& url)
{
   // both https URLs and local file URLs are cosnidered secure
   return boost::algorithm::starts_with(url, "https:") ||
          boost::algorithm::starts_with(url, "file:");
}

void insecureReposURLWarning(const std::string& url,
                             const std::string& extraMsg = "")
{
   std::string msg =
         "Your CRAN mirror is set to \"" + url + "\" which "
         "has an insecure (non-HTTPS) URL.";

   if (!extraMsg.empty())
      msg += " " + extraMsg;

   Error error = r::exec::RFunction(".rs.insecureReposWarning", msg).call();
   if (error)
      LOG_ERROR(error);
}

void insecureDownloadWarning(const std::string& msg)
{
   Error error = r::exec::RFunction(".rs.insecureDownloadWarning", msg).call();
   if (error)
      LOG_ERROR(error);
}


void unableToSecureConnectionWarning(const std::string& url)
{
   boost::format fmt(
      "You are configured to use the CRAN mirror at %1%. This mirror "
      "supports secure (HTTPS) downloads however your system is unable to "
      "communicate securely with the server (possibly due to out of date "
      "certificate files on your system). Falling back to using insecure "
      "URL for this mirror."
   );

   insecureDownloadWarning(boost::str(fmt % url));
}

bool isCRANReposFromSettings()
{
   bool fromSettings = true;
   Error error = r::exec::RFunction(".rs.isCRANReposFromSettings").call(
                                                              &fromSettings);
   if (error)
      LOG_ERROR(error);
   return fromSettings;
}


class CRANMirrorHttpsUpgrade : public async_r::AsyncRProcess
{
public:
   static void attemptUpgrade()
   {
      // get the URL currently in settings. if it's https already then bail
      prefs::CRANMirror mirror = 
         prefs::userPrefs().getCRANMirror();
      if (isSecure(mirror.url))
         return;

      // modify to be secure
      mirror.url = boost::algorithm::replace_first_copy(mirror.url,
                                                        "http://",
                                                        "https://");

      // build the command
      std::string cmd("{ " + module_context::CRANDownloadOptions() + "; ");
      cmd += "tmp <- tempfile(); ";
      cmd += "download.file(paste(contrib.url('" + mirror.url +
              "'), '/PACKAGES.gz', sep = ''), destfile = tmp); ";
      cmd += "cat(readLines(tmp)); ";
      cmd += "} ";

      // kickoff the process
      boost::shared_ptr<CRANMirrorHttpsUpgrade> pUpgrade(
                                    new CRANMirrorHttpsUpgrade(mirror));
      pUpgrade->start(cmd.c_str(), FilePath(), async_r::R_PROCESS_VANILLA);
   }

   virtual void onStdout(const std::string& output)
   {
      output_ += output;
   }

   virtual void onCompleted(int exitStatus)
   {
      if ((exitStatus == EXIT_SUCCESS) && checkOutputForSuccess())
      {
         prefs::userPrefs().setCRANMirror(secureMirror_, true);
      }
      else
      {
         std::string url = prefs::userPrefs().getCRANMirror().url;
         if (isKnownSecureMirror(url))
            unableToSecureConnectionWarning(secureMirror_.url);
         else
            insecureReposURLWarning(url);
      }
   }

private:
   bool checkOutputForSuccess()
   {
      return boost::algorithm::contains(output_, "Package: Matrix");
   }

   bool isKnownSecureMirror(const std::string& url)
   {
      std::vector<std::string> mirrors;
      mirrors.push_back("http://cran.rstudio.com/");
      return std::find(mirrors.begin(), mirrors.end(), url) != mirrors.end();
   }

private:
   explicit CRANMirrorHttpsUpgrade(const prefs::CRANMirror& secureMirror)
      : secureMirror_(secureMirror)
   {
   }
   std::string output_;
   prefs::CRANMirror secureMirror_;
};


void revertCRANMirrorToHTTP()
{
   prefs::CRANMirror mirror = prefs::userPrefs().getCRANMirror();
   std::string previous(mirror.url);
   boost::algorithm::replace_first(mirror.url, "https://", "http://");
   if (previous != mirror.url)
   {
      // Only set the value if it's actually changed (to avoid looping back here when we reconcile
      // the HTTP type for the new value)
      prefs::userPrefs().setCRANMirror(mirror, true);
   }
}

} // anonymous namespace

void reconcileSecureDownloadConfiguration()
{
   // secure downloads enabled
   if (prefs::userPrefs().useSecureDownload())
   {
      // ensure we have a secure download method
      Error error = r::exec::RFunction(".rs.initSecureDownload").call();
      if (error)
         LOG_ERROR(error);

      // if we couldn't get one then a suitable warning has been printed,
      // revert any https mirror and exit
      if (!module_context::haveSecureDownloadFileMethod())
      {
         revertCRANMirrorToHTTP();
         return;
      }

      // if the current repository is secure then don't bother (it may
      // be secure via the setting or by the user setting it explicitly
      // within .Rprofile)
      std::string reposURL = module_context::CRANReposURL();
      if (isSecure(reposURL))
         return;

      // if there is a global repository set and it's inscure then warn
      // (in this case the global repository is always overriding the user
      // provided repository so it only makes sense to check/verify the
      // global repository)
      std::string globalRepos = session::options().rCRANUrl();
      if (!globalRepos.empty() && !isSecure(globalRepos))
      {
         insecureReposURLWarning(globalRepos,
            "Please report this to your server administrator."
         );
      }

      // if the repository was set in R profile then we also need to
      // just warn and bail
      else if (!isCRANReposFromSettings())
      {
         insecureReposURLWarning(reposURL,
            "The repository was likely specified in .Rprofile or "
            "Rprofile.site so if you wish to change it you may need "
            "to edit one of those files.");
      }

      // let's see if we can automatically update the user's CRAN repos to
      // an HTTPS connection
      else
      {
         CRANMirrorHttpsUpgrade::attemptUpgrade();
      }
   }

   // secure downloads not enabled -- back out any https url
   else
   {
      revertCRANMirrorToHTTP();
   }
}

} // namespace module_context

namespace modules {
namespace cran_mirrors {
namespace {

void onUserSettingsChanged(const std::string& layer, const std::string& pref)
{
   if (pref != kCranMirror)
      return;

   if (!options().allowCRANReposEdit() && layer == kUserPrefsUserLayer)
   {
      // if admin has disallowed CRAN mirror editing, ignore this change
      return;
   }

   // extract the CRAN mirror option
   auto mirror = prefs::userPrefs().getCRANMirror();

   // make the change to the underlying CRAN option
   Error error = r::exec::RFunction(".rs.setCRANReposFromSettings",
                                    mirror.url, mirror.secondary).call();
   if (error)
      LOG_ERROR(error);
}

void onDeferredInit(bool newSession)
{
   // ensure we have a secure connection to CRAN
   module_context::reconcileSecureDownloadConfiguration();

   // begin monitoring user prefs
   prefs::userPrefs().onChanged.connect(onUserSettingsChanged);
}

SEXP rs_getCranReposUrl()
{
   r::sexp::Protect protect;
   std::string rCRANReposUrl = session::options().rCRANReposUrl();

   return r::sexp::create(rCRANReposUrl, &protect);
}


} // anonymous namespace


Error initialize()
{
   using boost::bind;
   using namespace module_context;

   module_context::events().onDeferredInit.connect(onDeferredInit);

   RS_REGISTER_CALL_METHOD(rs_getCranReposUrl);

   return Success();
}

} // namespace cran_mirrors
} // namespace modules
} // namespace session
} // namespace rstudio
