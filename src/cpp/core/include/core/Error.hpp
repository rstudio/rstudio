/*
 * Error.hpp
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

#ifndef CORE_ERROR_HPP
#define CORE_ERROR_HPP

#include <iosfwd>
#include <string>
#include <vector>

#include <boost/shared_ptr.hpp>

#include <boost/system/error_code.hpp>

#include <boost/current_function.hpp>

namespace core {

class FilePath;
class ErrorLocation ;
   
class Error ;
class Success ;
   
class Error_lock 
{
   friend class Error ;
   friend class Success ;
private:
   Error_lock() {} 
   Error_lock(const Error_lock&) {}
};
 
// typedef for error properties
typedef std::vector<std::pair<std::string,std::string> > ErrorProperties ;

// Concrete class for returning error codes from methods. 
// Since Error is copied during returns, it should not be derived
// from to create "convenience" subclasses for various error domains.
// Rater, a global function like systemError below should be created
// on a per-domain basis as a helper. 
// derive from Error_lock to prevent further derivation (see comment above)
class Error : public virtual Error_lock
{
public:
   Error() ;

   Error(const boost::system::error_code& ec, 
         const ErrorLocation& location); 

   Error(const boost::system::error_code& ec, 
         const Error& cause,
         const ErrorLocation& location); 

   // non-virtual destructor because no subclasses are permitted and we
   // want to keep Error as lightweight as possible
   ~Error() ;
  
   // COPYING: via shared_ptr, mutating functions must call copyOnWrite 
   // prior to executing
   
   void addProperty(const std::string& name, const std::string& value); 
   void addProperty(const std::string& name, const FilePath& value); 
   void addProperty(const std::string& name, int value);
   
   const boost::system::error_code& code() const;

   std::string summary() const;
   
   const Error& cause() const ;

   const ErrorLocation& location() const ;

   const ErrorProperties& properties() const;
   std::string getProperty(const std::string& name) const;

   // below based on boost::system::error_code   
   typedef void (*unspecified_bool_type)();
   static void unspecified_bool_true() {}
   operator unspecified_bool_type() const 
   { 
      return !isError() ? 0 : unspecified_bool_true;
   }
   bool operator!() const 
   {
      return !isError();
   }

private:
   bool isError() const ;
   void copyOnWrite() ;

private:
   struct Impl ;
   mutable boost::shared_ptr<Impl> pImpl_ ;
   Impl& impl() const;
};

// 
// No error subclass created for syntactic conveneince:
//   return Success();
//
class Success : public Error
{
public:
   Success() : Error() {}
};


Error systemError(int value, const ErrorLocation& location) ; 
Error systemError(int value,
                  const std::string& description,
                  const ErrorLocation& location) ;

Error fileExistsError(const ErrorLocation& location);
Error fileNotFoundError(const ErrorLocation& location);
Error fileNotFoundError(const std::string& path,
                        const ErrorLocation& location);
Error fileNotFoundError(const FilePath& filePath,
                        const ErrorLocation& location);

bool isPathNotFoundError(const Error& error);
Error pathNotFoundError(const ErrorLocation& location);
Error pathNotFoundError(const std::string& path,
                        const ErrorLocation& location);


class ErrorLocation
{
public:
   ErrorLocation() ;
   ErrorLocation(const char* function, const char* file, long line) ;
   virtual ~ErrorLocation() ;
   
   // immutable - copying and assignment via shared_ptr 

   bool hasLocation() const ;

   const std::string& function() const ;
   const std::string& file() const ;
   long line() const ;
   
   std::string asString() const;
   
   bool operator==(const ErrorLocation& location) const;
   bool operator!=(const ErrorLocation& location) const
   {
      return !(*this == location); 
   }

private:
   struct Impl ;
   boost::shared_ptr<Impl> pImpl_ ;
};
   
std::ostream& operator<<(std::ostream& os, const ErrorLocation& location);
   
} // namespace core 

#define ERROR_LOCATION core::ErrorLocation( \
      BOOST_CURRENT_FUNCTION,__FILE__,__LINE__)

#define CATCH_UNEXPECTED_EXCEPTION \
   catch(const std::exception& e) \
   { \
      LOG_ERROR_MESSAGE(std::string("Unexpected exception: ") + \
                        e.what()) ;  \
   } \
   catch(...) \
   { \
      LOG_ERROR_MESSAGE("Unknown exception"); \
   } 


#endif // CORE_ERROR_HPP

