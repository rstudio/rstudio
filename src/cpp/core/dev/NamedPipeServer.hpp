#ifndef NAMEDPIPESERVER_HPP
#define NAMEDPIPESERVEr_HPP

#include <string>

#include <boost/function.hpp>
#include <boost/shared_ptr.hpp>

namespace core {
   class Error;
}

#include "HttpConnection.hpp"

core::Error runServer(const std::string& pipeName,
                      boost::function<void(boost::shared_ptr<HttpConnection>)>
                                                         connectionHandler);


#endif // NAMEDPIPESERVER_HPP
