#include "NamedPipeServer.hpp"

#include <boost/asio.hpp>
#include <boost/array.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <core/http/Util.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/RequestParser.hpp>

using namespace core;

namespace {

bool isConnectionTerminatedError(const Error& error)
{
   return false;
}

class NamedPipeHttpConnection :
            public HttpConnection,
            public boost::enable_shared_from_this<NamedPipeHttpConnection>,
            boost::noncopyable
{
public:
   typedef boost::function<void(
         boost::shared_ptr<NamedPipeHttpConnection>)> Handler;


public:
   NamedPipeHttpConnection(boost::asio::io_service& ioService,
                           HANDLE hPipe,
                           const Handler& handler)
      : stream_(ioService, hPipe), handler_(handler)
   {
   }

   void readRequest()
   {
      boost::array<char, 8192> buffer;
      http::RequestParser parser;

      while (true)
      {
         boost::system::error_code ec;
         std::size_t bytesRead = stream_.read_some(boost::asio::buffer(buffer),
                                                   ec);
         if (!ec)
         {
            // parse next chunk
            http::RequestParser::status status = parser.parse(
                                       request_,
                                       buffer.data(),
                                       buffer.data() + bytesRead);

            // error - return bad request
            if (status == core::http::RequestParser::error)
            {
               core::http::Response response;
               response.setStatusCode(core::http::status::BadRequest);
               sendResponse(response);

               // this object now dies on it's own since we don't pass
               // the shared ptr to the handler
            }

            // incomplete -- keep reading
            else if (status == core::http::RequestParser::incomplete)
            {
               continue;
            }

            // got valid request -- handle it
            else
            {
               handler_(NamedPipeHttpConnection::shared_from_this());
               break;
            }
         }
         else
         {
            // log if not connection terminated
            Error error(ec, ERROR_LOCATION);
            if (!isConnectionTerminatedError(error))
               LOG_ERROR(error);

            // request dies here
            close();
         }
      }


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
   Handler handler_;
   http::Request request_;
};

} // anonymous namespace

Error runServer(const std::string& pipeName,
                boost::function<void(boost::shared_ptr<HttpConnection>)>
                                                         connectionHandler)
{

   return Success();
}
