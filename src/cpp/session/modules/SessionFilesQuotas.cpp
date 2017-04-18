/*
 * SessionFilesQuotas.cpp
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

#include "SessionFilesQuotas.hpp"

#include <iostream>

#include <boost/tokenizer.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/numeric/conversion/cast.hpp>

#include <core/BoostThread.hpp>
#include <core/Error.hpp>
#include <core/BoostErrors.hpp>
#include <core/Log.hpp>

#include <core/system/Process.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace session {
namespace modules { 
namespace files {
namespace quotas {

namespace {

// does the system have quotas?
bool s_systemHasQuotas = false;   
   
struct QuotaInfo
{
   typedef long long size_type;
   
   QuotaInfo()
      : hasQuota(false), used(0), quota(0), limit(0)
   {
   }
   
   bool hasQuota;
   size_type used;
   size_type quota;
   size_type limit;
};
    
void quotaInfoToJson(const QuotaInfo& quotaInfo,
                     json::Object* pQuotaInfoJson)
{
   // first get the data into json serializable types
   double used = 0;
   double quota = 0;
   double limit = 0;
   try
   {
      used = boost::numeric_cast<double>(quotaInfo.used);
      quota = boost::numeric_cast<double>(quotaInfo.quota);
      limit = boost::numeric_cast<double>(quotaInfo.limit);
   }
   catch(boost::numeric::bad_numeric_cast& e)
   {
      LOG_ERROR_MESSAGE(std::string("Error converting quota info to double: ") +
                        e.what());
   }
   
   // write to json
   json::Object& quotaInfoJson = *pQuotaInfoJson;
   quotaInfoJson["used"] = used;
   quotaInfoJson["quota"] = quota;
   quotaInfoJson["limit"] = limit;
}
   
QuotaInfo::size_type quotaBytes(const std::string& quotaKb)
{
   QuotaInfo::size_type kb = boost::lexical_cast<QuotaInfo::size_type>(quotaKb);
   return kb * 1024L;
}

Error parseQuotaInfo(const std::string& quotaInfo, QuotaInfo* pInfo)
{
   // if there was no quota info returned then there is no quota on this box
   if (quotaInfo.empty())
   {
      pInfo->hasQuota = false;
      return Success();
   }
   
   // tokenzie results and seek quota info
   int resultIndex = 0;
   boost::char_separator<char> sep(" \t");
   typedef boost::tokenizer<boost::char_separator<char> > QuotaInfoTokenizer;
   QuotaInfoTokenizer tok(quotaInfo, sep);
   for (QuotaInfoTokenizer::const_iterator it = tok.begin();
        it != tok.end();
        ++it)
   {
      try
      {
         switch(resultIndex++)
         {
            // ingore first entry (device)
            case 0:
               break;
            
            // Used
            case 1:
               pInfo->used = quotaBytes(*it);
               break;
               
            // Quota
            case 2:
               pInfo->quota = quotaBytes(*it);
               break;
               
            // Limit
            case 3:
               pInfo->limit = quotaBytes(*it);
               pInfo->hasQuota = true;
               break;
         }
      }
      catch(boost::bad_lexical_cast&)
      {
         return systemError(boost::system::errc::result_out_of_range, 
                            ERROR_LOCATION);
      }
      
      // out of here....
      if (pInfo->hasQuota)
         break;
   }
   
   // make sure we got all of the results
   if (!pInfo->hasQuota)
   {
      return systemError(boost::system::errc::result_out_of_range, 
                         ERROR_LOCATION);
   } 
   else
   {
      return Success();
   }
}

void checkQuotaThread()
{
   try
   {
      // run the command
      core::system::ProcessResult result;
      Error error = runCommand("xfs_quota -c 'quota -N'",
                               core::system::ProcessOptions(),
                               &result);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      // parse output
      QuotaInfo quotaInfo;
      error = parseQuotaInfo(result.stdOut, &quotaInfo);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
      
      // send event only if there are quotas established
      if (quotaInfo.hasQuota)
      {
         json::Object quotaInfoJson;
         quotaInfoToJson(quotaInfo, &quotaInfoJson);
         ClientEvent event(client_events::kQuotaStatus, quotaInfoJson);
         module_context::enqueClientEvent(event);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}

} // anonymous namespace

Error initialize()
{
    // one time initialization of s_systemHasQuotas
   if ( (session::options().programMode() == kSessionProgramModeServer) &&
        session::options().limitXfsDiskQuota() )
   {
      std::string out;
      Error error = r::exec::system("which xfs_quota", &out);
      s_systemHasQuotas = !out.empty();
   }
   else
   {
      s_systemHasQuotas = false;
   }

   return Success();
}
   

void checkQuotaStatus()
{
   if (s_systemHasQuotas)
   {
      try
      {
         // block all signals for launch of background thread (will cause it
         // to never receive signals)
         core::system::SignalBlocker signalBlocker;
         Error error = signalBlocker.blockAll();
         if (error)
            LOG_ERROR(error);
         
         boost::thread t(checkQuotaThread);
      }
      catch(const boost::thread_resource_error& e)
      {
         LOG_ERROR(Error(boost::thread_error::ec_from_exception(e),
                         ERROR_LOCATION));
      }
   }
}

} // namespace quotas
} // namepsace files
} // namespace modules
} // namespace session
} // namespace rstudio

