/*
 * AdvisoryFileLock.cpp
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

#include <sstream>

#include <core/FileLock.hpp>

#include <boost/scope_exit.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>

#include <core/BoostErrors.hpp>

#define LOG(__X__)                                                             \
   do                                                                          \
   {                                                                           \
      if (::rstudio::core::FileLock::isLoggingEnabled())                       \
      {                                                                        \
         std::stringstream ss;                                                 \
         ss << "(PID " << ::getpid() << "): " << __X__ << std::endl;           \
         ::rstudio::core::FileLock::log(ss.str());                             \
      }                                                                        \
   } while (0)

// we define BOOST_USE_WINDOWS_H on mingw64 to work around some
// incompatabilities. however, this prevents the interprocess headers
// from compiling so we undef it in this localized context
#if defined(__GNUC__) && defined(_WIN64)
   #undef BOOST_USE_WINDOWS_H
#endif
#include <boost/interprocess/sync/file_lock.hpp>

namespace rstudio {
namespace core {

namespace {
typedef boost::interprocess::file_lock BoostFileLock;
}

struct AdvisoryFileLock::Impl
{
   FilePath lockFilePath;
   BoostFileLock lock;
};

bool AdvisoryFileLock::isLocked(const FilePath& lockFilePath) const
{
   // if the lock file doesn't exist then it's not locked
   if (!lockFilePath.exists())
      return false;

   // check if it is locked
   try
   {
      BoostFileLock lock(string_utils::utf8ToSystem(lockFilePath.absolutePath()).c_str());

      if (lock.try_lock())
      {
         lock.unlock();
         return false;
      }
      else
      {
         return true;
      }
   }
   catch(boost::interprocess::interprocess_exception& e)
   {
      Error error(boost::interprocess::ec_from_exception(e), ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      LOG_ERROR(error);
      return false;
   }
}


AdvisoryFileLock::AdvisoryFileLock()
   : pImpl_(new Impl())
{
}

AdvisoryFileLock::~AdvisoryFileLock()
{
}

Error AdvisoryFileLock::acquire(const FilePath& lockFilePath)
{
   using namespace boost::interprocess;

   // make sure the lock file exists
   if (!lockFilePath.exists())
   {
      Error error = core::writeStringToFile(lockFilePath, "");
      if (error)
         return error;
   }

   // try to acquire the lock
   try
   {
      BoostFileLock lock(string_utils::utf8ToSystem(lockFilePath.absolutePath()).c_str());

      if (lock.try_lock())
      {
         LOG("Acquired lock: " << lockFilePath.absolutePath());
         // set members
         pImpl_->lockFilePath = lockFilePath;
         pImpl_->lock.swap(lock);

         return Success();
      }
      else
      {
         LOG("Failed to acquire lock: " << lockFilePath.absolutePath());
         return systemError(boost::system::errc::no_lock_available,
                            ERROR_LOCATION);
      }
   }
   catch(interprocess_exception& e)
   {
      Error error(ec_from_exception(e), ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   return Success();
}

Error AdvisoryFileLock::release()
{
   using namespace boost::interprocess;

   // make sure the lock file exists
   if (!pImpl_->lockFilePath.exists())
   {
      return systemError(boost::system::errc::no_lock_available,
                         ERROR_LOCATION);
   }

   // always cleanup the lock file on exit
   FilePath lockFilePath = pImpl_->lockFilePath;
   BOOST_SCOPE_EXIT( (&lockFilePath) )
   {
      Error error = lockFilePath.remove();
      if (error)
         LOG_ERROR(error);
   }
   BOOST_SCOPE_EXIT_END

   // try to unlock it
   try
   {
      pImpl_->lock.unlock();
      pImpl_->lock = BoostFileLock();
      LOG("Released lock: " << pImpl_->lockFilePath.absolutePath());
      pImpl_->lockFilePath = FilePath();
      return Success();
   }
   catch(interprocess_exception& e)
   {
      Error error(ec_from_exception(e), ERROR_LOCATION);
      error.addProperty("lock-file", pImpl_->lockFilePath);
      return error;
   }

   return Success();
}

FilePath AdvisoryFileLock::lockFilePath() const
{
   return pImpl_->lockFilePath;
}

void AdvisoryFileLock::refresh()
{
}

void AdvisoryFileLock::cleanUp()
{
}

} // namespace core
} // namespace rstudio


