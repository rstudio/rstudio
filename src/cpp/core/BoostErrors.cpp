/*
 * BoostErrors.cpp
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

#include <core/BoostErrors.hpp>

#include <core/BoostThread.hpp>

// we define BOOST_USE_WINDOWS_H on mingw64 to work around some
// incompatabilities. however, this prevents the interprocess headers
// from compiling so we undef it in this localized context
#if defined(__GNUC__) && defined(_WIN64)
   #undef BOOST_USE_WINDOWS_H
#endif

#include <boost/interprocess/errors.hpp>
#include <boost/interprocess/exceptions.hpp>

using namespace boost::system ;

namespace RSTUDIO_BOOST_NAMESPACE {
namespace interprocess {

class interprocess_error_category : public error_category
{
public:
   const char* name() const BOOST_NOEXCEPT;
   std::string message(int ev) const ;
};

const error_category& interprocess_category()
{
   static interprocess_error_category interprocessCategoryConst ;
   return interprocessCategoryConst ;
}

const char* interprocess_error_category::name() const BOOST_NOEXCEPT
{
   return "interprocess" ;
}

std::string interprocess_error_category::message(int ev) const 
{
   std::string message ;
   switch (ev)
   {
      case no_error:
         message = "No error";
         break;

      case system_error:
         message = "System error";
         break;

      case other_error:
         message = "Library generated error";
         break;

      case security_error:
         message = "Security error";
         break;

      case read_only_error:
         message = "Read only error";
         break;

      case io_error:
         message = "IO error";
         break;

      case path_error:
         message = "Path error";
         break;

      case not_found_error:
         message = "Not found error";
         break;

      case busy_error:
         message = "Busy error" ;
         break;

      case already_exists_error:
         message = "Already exists error";
         break;

      case not_empty_error:
         message = "Not empty error";
         break;

      case is_directory_error:
         message = "Is directory error";
         break;

      case out_of_space_error:
         message = "Out of space error";
         break;

      case out_of_memory_error:
         message = "Out of memory error";
         break;

      case out_of_resource_error:
         message = "Out of resource error" ;
         break;

      case lock_error:
         message = "Lock error" ;
         break ;

      case sem_error:
         message = "Sem error" ;
         break ;

      case mode_error:
         message = "Mode error" ;
         break;

      case size_error:
         message = "Size error" ;
         break;

      case corrupted_error:
         message = "Corrupted error" ;
         break ;

      default:
         message = "Unknown error" ;
         break;
   }

   return message ;
}



boost::system::error_code ec_from_exception(const interprocess_exception& e) 
{
   if (e.get_error_code() == system_error)
      return error_code(e.get_native_error(), get_system_category()) ;
   else
      return error_code(e.get_error_code(), interprocess_category()) ;
}

} // namespace interprocess


// thread_error 
namespace thread_error {

namespace errc {
enum errc_t {
   thread_resource_error = 1
};
} // namespace thread_errc


class thread_error_category : public error_category
{
public:
   const char* name() const BOOST_NOEXCEPT;
   std::string message(int ev) const ;
};

const error_category& thread_category()
{
   static thread_error_category threadCategoryConst ;
   return threadCategoryConst ;
}

const char* thread_error_category::name() const BOOST_NOEXCEPT
{
   return "thread" ;
}

std::string thread_error_category::message(int ev) const 
{
   std::string message ;
   switch (ev)
   {
      // only one error code for now
      case errc::thread_resource_error:
      default:
         message = "Thread resource error" ;
         break ;
   }
   return message ;
}

boost::system::error_code ec_from_exception(
      const boost::thread_resource_error& e) 
{
   return error_code(errc::thread_resource_error, thread_category()) ;
}

} // namespace thread_error

} // namespace boost




