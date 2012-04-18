/*
 * Error.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <ostream>
#include <sstream>

#include <core/Error.hpp>

#include <core/FilePath.hpp>

#include <boost/lexical_cast.hpp>

#ifdef _WIN32
#include <boost/system/windows_error.hpp>
#endif

namespace core {

struct Error::Impl
{
   boost::system::error_code ec ;
   ErrorProperties properties ;
   Error cause ;
   ErrorLocation location ;
};

Error::Error()
   : pImpl_()
{
}

Error::Error(const boost::system::error_code& ec, 
             const ErrorLocation& location)
   : pImpl_(new Impl()) 
{
   pImpl_->ec = ec ;
   pImpl_->location = location ;
}
   
Error::Error(const boost::system::error_code& ec,
             const Error& cause,
             const ErrorLocation& location)
   : pImpl_(new Impl()) 
{
   pImpl_->ec = ec ;
   pImpl_->cause = cause ;
   pImpl_->location = location ;
}
   
 
Error::~Error() 
{
}

void Error::copyOnWrite()
{
   if ( (pImpl_.get() != NULL) && !pImpl_.unique())
   {
      Impl* pOldImpl = pImpl_.get() ;
      pImpl_ = boost::shared_ptr<Impl>(new Impl(*pOldImpl)) ;
   }
}

const boost::system::error_code& Error::code() const
{
   return impl().ec;
}

   
std::string Error::summary() const
{
   std::ostringstream ostr;
   ostr << code().category().name() << " error "
        << code().value() << " (" << code().message() << ")"  ;
   return ostr.str();
}

const Error& Error::cause() const
{
   return impl().cause ;
}

const ErrorLocation& Error::location() const
{
   return impl().location ;
}

void Error::addProperty(const std::string& name, const std::string& value)
{
   copyOnWrite() ;
   impl().properties.push_back(std::make_pair(name, value)) ;
}
   
void Error::addProperty(const std::string& name, const FilePath& value)
{
   addProperty(name, value.absolutePath());
}
   
void Error::addProperty(const std::string& name, int value)
{
   addProperty(name, boost::lexical_cast<std::string>(value));
}

   
const std::vector<std::pair<std::string,std::string> >& 
      Error::properties() const
{
   return impl().properties ;
}
   
std::string Error::getProperty(const std::string& name) const
{
   for (ErrorProperties::const_iterator it = properties().begin();
        it != properties().end(); ++it)
   {
      if (it->first == name)
         return it->second;
   }
   
   return std::string();
}

bool Error::isError() const 
{
   if ( pImpl_.get() != NULL )
      return pImpl_->ec.value() != 0 ;
   else
      return false ;
}

Error::Impl& Error::impl() const
{
   if (pImpl_.get() == NULL)
      pImpl_.reset(new Impl()) ;
   return *pImpl_ ;
}

Error systemError(int value, const ErrorLocation& location) 
{
   using namespace boost::system ;
   return Error(error_code(value, get_system_category()), location);
}

Error systemError(int value,
                  const std::string& description,
                  const ErrorLocation& location)
{
   Error error = systemError(value, location);
   error.addProperty("description", description);
   return error;
}

Error fileExistsError(const ErrorLocation& location)
{
#ifdef _WIN32
   return systemError(boost::system::windows_error::file_exists, location);
#else
   return systemError(boost::system::errc::file_exists, location);
#endif
}

Error fileNotFoundError(const ErrorLocation& location)
{
#ifdef _WIN32
   return systemError(boost::system::windows_error::file_not_found, location);
#else
   return systemError(boost::system::errc::no_such_file_or_directory, location);
#endif
}

Error fileNotFoundError(const std::string& path,
                        const ErrorLocation& location)
{
   Error error = fileNotFoundError(location);
   error.addProperty("path", path);
   return error;
}

Error fileNotFoundError(const FilePath& filePath,
                        const ErrorLocation& location)
{
   Error error = fileNotFoundError(location);
   error.addProperty("path", filePath);
   return error;
}

bool isPathNotFoundError(const Error& error)
{
#ifdef _WIN32
   return error.code() == boost::system::windows_error::path_not_found;
#else
   return error.code() == boost::system::errc::no_such_file_or_directory;
#endif
}

Error pathNotFoundError(const ErrorLocation& location)
{
#ifdef _WIN32
   return systemError(boost::system::windows_error::path_not_found, location);
#else
   return systemError(boost::system::errc::no_such_file_or_directory, location);
#endif
}

Error pathNotFoundError(const std::string& path, const ErrorLocation& location)
{
   Error error = pathNotFoundError(location);
   error.addProperty("path", path);
   return error;
}
   
struct ErrorLocation::Impl 
{
   Impl() : line(0) 
   {
   }
   
   Impl(const char* function, const char* file, long line)
      : function(function), file(file), line(line) 
   {
   }
   
   std::string function ;
   std::string file ;
   long line ;
};

ErrorLocation::ErrorLocation()
   : pImpl_(new Impl()) 
{
}

ErrorLocation::ErrorLocation(const char* function, const char* file, long line)
   : pImpl_(new Impl(function, file, line)) 
{
}

ErrorLocation::~ErrorLocation() 
{
}


bool ErrorLocation::hasLocation() const 
{
   return line() > 0 ;
}

const std::string& ErrorLocation::function() const 
{
   return pImpl_->function ;
}

const std::string& ErrorLocation::file() const 
{
   return pImpl_->file ;
}

long ErrorLocation::line() const
{
   return pImpl_->line ;
}
   
std::string ErrorLocation::asString() const
{
   std::ostringstream ostr ;
   ostr << *this;
   return ostr.str();
}
   
bool ErrorLocation::operator==(const ErrorLocation& location) const
{
   return function() == location.function() &&
          file() == location.file() &&
          line() == location.line();
}
   
std::ostream& operator<<(std::ostream& os, const ErrorLocation& location)
{
   os << location.function() << " " 
      << location.file() << ":" 
      << location.line() ;
   
   return os;
}
   
   

} // namespace core 


