#include "NamedPipeServer.hpp"

#include <boost/asio.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <core/http/Util.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

using namespace core;

namespace {

bool isConnectionTerminatedError(const Error& error)
{
   return false;
}

class NamedPipeHttpConnection : public HttpConnection, boost::noncopyable
{
public:
   NamedPipeHttpConnection(boost::asio::io_service& ioService, HANDLE hPipe)
      : stream_(ioService, hPipe)
   {
      // parse the request (can use incremental parsing to stop
      // reading when the request is fully submitted)

   }

   virtual const core::http::Request& request()
   {
      return request_;
   }

   virtual void sendResponse(const core::http::Response& response)
   {
      try
      {
        // write the response
        boost::asio::write(stream_,
                           response.toBuffers(
                                 core::http::Header::connectionClose()));
      }
      catch(const boost::system::system_error& e)
      {
        // establish error
        Error error = core::Error(e.code(), ERROR_LOCATION);
        error.addProperty("request-uri", request_.uri());

        // log the error if it wasn't connection terminated
        if (isConnectionTerminatedError(error))
           LOG_ERROR(error);
      }
      CATCH_UNEXPECTED_EXCEPTION

      // always close connection
      try
      {
        close();
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

   // close (occurs automatically after writeResponse, here in case it
   // need to be closed in other circumstances
   virtual void close()
   {
      if (stream_.is_open())
      {
         // TODO: confirm that the native_handle is the hPipe we were passed
         if (stream_.native_handle() != INVALID_HANDLE_VALUE)
         {
            ::DisconnectNamedPipe(stream_.native_handle());
         }

         boost::system::error_code ec;
         stream_.close(ec);
         if (ec)
            LOG_ERROR(Error(ec, ERROR_LOCATION));
      }
   }

private:
   boost::asio::windows::stream_handle stream_;
   http::Request request_;
};

} // anonymous namespace

Error runServer(const std::string& pipeName,
                boost::function<void(boost::shared_ptr<HttpConnection>)>
                                                         connectionHandler)
{

   return Success();
}
