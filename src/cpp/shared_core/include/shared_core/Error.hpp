/*
 * Error.hpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#ifndef SHARED_CORE_ERROR_HPP
#define SHARED_CORE_ERROR_HPP

#include <iosfwd>
#include <string>
#include <vector>

#include <boost/shared_ptr.hpp>

#include <boost/system/error_code.hpp>

#include <boost/current_function.hpp>

namespace rstudio {
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

   void addOrUpdateProperty(const std::string& name, const std::string& value);
   void addOrUpdateProperty(const std::string& name, const FilePath& value);
   void addOrUpdateProperty(const std::string& name, int value);
   
   void setExpected();
   bool expected() const;

   const boost::system::error_code& code() const;

   std::string summary() const;

   std::string description() const;
   
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
// No error subclass created for syntactic convenience:
//   return Success();
//
class Success : public Error
{
public:
   Success() : Error() {}
};

#ifdef _WIN32

// Use this macro instead of systemError(::GetLastError(), ERROR_LOCATION) otherwise
// the ERROR_LOCATION macro may evaluate first and reset the Win32 error code to
// zero (no error), causing the wrong value to be passed to systemError. This is currently
// the case on debug builds using MSVC.
#define LAST_SYSTEM_ERROR() []() {auto lastErr = ::GetLastError(); return systemError(lastErr, ERROR_LOCATION);}()

#endif // _WIN32

Error systemError(int value, const ErrorLocation& location) ; 
Error systemError(int value,
                  const std::string& description,
                  const ErrorLocation& location) ;
Error systemError(int value,
                  const Error& cause,
                  const ErrorLocation& location) ;

Error fileExistsError(const ErrorLocation& location);
Error fileNotFoundError(const ErrorLocation& location);
Error fileNotFoundError(const std::string& path,
                        const ErrorLocation& location);
Error fileNotFoundError(const FilePath& filePath,
                        const ErrorLocation& location);
bool isFileNotFoundError(const Error& error);

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
} // namespace rstudio

#define ERROR_LOCATION rstudio::core::ErrorLocation( \
      BOOST_CURRENT_FUNCTION,__FILE__,__LINE__)

#define CATCH_UNEXPECTED_EXCEPTION \
   catch(const std::exception& e) \
   { \
      logErrorMessage(std::string("Unexpected exception: ") + \
                        e.what()) ;  \
   } \
   catch(...) \
   { \
      logErrorMessage("Unknown exception"); \
   } 


#endif

