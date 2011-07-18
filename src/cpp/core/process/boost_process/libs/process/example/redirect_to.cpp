// 
// Boost.Process 
// ~~~~~~~~~~~~~ 
// 
// Copyright (c) 2006, 2007 Julio M. Merino Vidal 
// Copyright (c) 2008 Ilya Sokolov, Boris Schaeling 
// Copyright (c) 2009 Boris Schaeling 
// Copyright (c) 2010 Felipe Tanus, Boris Schaeling 
// 
// Distributed under the Boost Software License, Version 1.0. (See accompanying 
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt) 
// 

#include <boost/process/all.hpp> 
#include <boost/bind.hpp> 
#include <iostream> 

//[redirect_to_stream 
class redirect_to 
{ 
public: 
    redirect_to(boost::process::handle h) 
    : h_(h) 
    { 
    } 

    boost::process::stream_ends operator()(boost::process::stream_type) const 
    { 
        return boost::process::stream_ends(h_, boost::process::handle()); 
    } 

private: 
    boost::process::handle h_; 
}; 
//] 

//[redirect_to_main 
boost::process::stream_ends forward(boost::process::stream_ends ends) 
{ 
    return ends; 
} 

int main() 
{ 
    std::string executable = boost::process::find_executable_in_path( 
        "hostname"); 

    std::vector<std::string> args; 

    boost::process::stream_ends ends = boost::process::behavior::pipe()
        (boost::process::output_stream); 

    boost::process::context ctx; 
    ctx.streams[boost::process::stdout_id] = boost::bind(forward, ends); 
    ctx.streams[boost::process::stderr_id] = redirect_to(ends.child); 

    boost::process::child c = boost::process::create_child( 
        executable, args, ctx); 

    boost::process::pistream is(c.get_handle(boost::process::stdout_id)); 
    std::cout << is.rdbuf() << std::flush; 
} 
//] 
