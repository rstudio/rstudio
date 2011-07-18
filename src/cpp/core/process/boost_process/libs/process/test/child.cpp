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

#include <boost/process/config.hpp> 

#if defined(BOOST_POSIX_API) 
#   include <stdlib.h> 
#   include <unistd.h> 
#   include <sys/wait.h> 
#elif defined(BOOST_WINDOWS_API) 
#   include <windows.h> 
#else 
#   error "Unsupported platform." 
#endif 

#define BOOST_TEST_MAIN 
#include "util/boost.hpp" 
#include "util/use_helpers.hpp" 
#include <string> 
#include <vector> 
#include <utility> 
#include <istream> 
#include <fstream> 
#include <cstdlib> 

BOOST_AUTO_TEST_CASE(test_close_stdin) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("is-closed-stdin"); 

    bp::context ctx; 
    ctx.streams[bp::stdin_id] = bpb::close(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_close_stdout) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("is-closed-stdout"); 

    bp::context ctx1; 
    ctx1.streams[bp::stdout_id] = bpb::close(); 

    bp::child c1 = bp::create_child(get_helpers_path(), args, ctx1); 

    int s1 = c1.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s1)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s1), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s1, EXIT_SUCCESS); 
#endif 

    bp::context ctx2; 
    ctx2.streams[bp::stdout_id] = bpb::pipe(); 

    bp::child c2 = bp::create_child(get_helpers_path(), args, ctx2); 

    int s2 = c2.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s2)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s2), EXIT_FAILURE); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s2, EXIT_FAILURE); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_close_stderr) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("is-closed-stderr"); 

    bp::context ctx1; 
    ctx1.streams[bp::stderr_id] = bpb::close(); 

    bp::child c1 = bp::create_child(get_helpers_path(), args, ctx1); 

    int s1 = c1.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s1)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s1), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s1, EXIT_SUCCESS); 
#endif 

    bp::context ctx2; 
    ctx2.streams[bp::stderr_id] = bpb::pipe(); 

    bp::child c2 = bp::create_child(get_helpers_path(), args, ctx2); 

    int s2 = c2.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s2)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s2), EXIT_FAILURE); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s2, EXIT_FAILURE); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_input) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("stdin-to-stdout"); 

    bp::context ctx; 
    ctx.streams[bp::stdin_id] = bpb::pipe(); 
    ctx.streams[bp::stdout_id] = bpb::pipe(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    bp::postream os(c.get_handle(bp::stdin_id)); 
    bp::pistream is(c.get_handle(bp::stdout_id)); 

    os << "message-to-process" << std::endl; 
    os.close(); 

    std::string word; 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "message-to-process"); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_output) 
{ 
    std::vector<std::string> args; 
    args.push_back("echo-stdout"); 
    args.push_back("message-stdout"); 

    bp::context ctx; 
    ctx.streams[bp::stdout_id] = bpb::pipe(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    std::string word; 
    bp::pistream is(c.get_handle(bp::stdout_id)); 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "message-stdout"); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_output_error) 
{ 
    std::vector<std::string> args; 
    args.push_back("echo-stderr"); 
    args.push_back("message-stderr"); 

    bp::context ctx; 
    ctx.streams[bp::stderr_id] = bpb::pipe(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    std::string word; 
    bp::pistream is(c.get_handle(bp::stderr_id)); 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "message-stderr"); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_output_stdout_stderr) 
{ 
    std::vector<std::string> args; 
    args.push_back("echo-stdout-stderr"); 
    args.push_back("message-to-two-streams"); 

    bp::context ctx; 
    ctx.streams[bp::stdout_id] = bpb::pipe(); 
    ctx.streams[bp::stderr_id] = bpb::pipe(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    std::string word; 
    bp::pistream isout(c.get_handle(bp::stdout_id)); 
    isout >> word; 
    BOOST_CHECK_EQUAL(word, "stdout"); 
    isout >> word; 
    BOOST_CHECK_EQUAL(word, "message-to-two-streams"); 
    bp::pistream iserr(c.get_handle(bp::stderr_id)); 
    iserr >> word; 
    BOOST_CHECK_EQUAL(word, "stderr"); 
    iserr >> word; 
    BOOST_CHECK_EQUAL(word, "message-to-two-streams"); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

class redirect_to 
{ 
public: 
    redirect_to(bp::handle h) 
    : h_(h) 
    { 
    } 

    bp::stream_ends operator()(bp::stream_type) const 
    { 
        return bp::stream_ends(h_, bp::handle()); 
    } 

private: 
    bp::handle h_; 
}; 

bp::stream_ends forward(bp::stream_ends ends) 
{ 
    return ends; 
} 

BOOST_AUTO_TEST_CASE(test_redirect_err_to_out) 
{ 
    std::vector<std::string> args; 
    args.push_back("echo-stdout-stderr"); 
    args.push_back("message-to-two-streams"); 

    bp::stream_ends ends = bpb::pipe()(bp::output_stream); 

    bp::context ctx; 
    ctx.streams[bp::stdout_id] = boost::bind(forward, ends); 
    ctx.streams[bp::stderr_id] = redirect_to(ends.child); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    bp::pistream is(c.get_handle(bp::stdout_id)); 
    std::string word; 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "stdout"); 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "message-to-two-streams"); 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "stderr"); 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "message-to-two-streams"); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(check_work_directory) 
{ 
    std::vector<std::string> args; 
    args.push_back("pwd"); 

    bp::context ctx; 
    BOOST_CHECK(bfs::equivalent(ctx.work_dir, bfs::current_path().string())); 

    ctx.streams[bp::stdout_id] = bpb::pipe(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    bp::pistream is(c.get_handle(bp::stdout_id)); 
    std::string dir; 
    std::getline(is, dir); 
    std::string::size_type pos = dir.rfind('\r'); 
    if (pos != std::string::npos) 
        dir.erase(pos); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 

    BOOST_CHECK_EQUAL(bfs::path(dir), bfs::path(ctx.work_dir)); 
} 

BOOST_AUTO_TEST_CASE(check_work_directory2) 
{ 
    std::vector<std::string> args; 
    args.push_back("pwd"); 

    bfs::path wdir = bfs::current_path() / "test.dir"; 
    BOOST_REQUIRE_NO_THROW(bfs::create_directory(wdir)); 

    try 
    { 
        bp::context ctx; 
        ctx.work_dir = wdir.string(); 
        ctx.streams[bp::stdout_id] = bpb::pipe(); 

        bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

        bp::pistream is(c.get_handle(bp::stdout_id)); 
        std::string dir; 
        std::getline(is, dir); 
        std::string::size_type pos = dir.rfind('\r'); 
        if (pos != std::string::npos) 
            dir.erase(pos); 

        int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
        BOOST_REQUIRE(WIFEXITED(s)); 
        BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
        BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 

        BOOST_CHECK_EQUAL(bfs::path(dir), bfs::path(ctx.work_dir)); 
        BOOST_CHECK_NO_THROW(bfs::remove_all(wdir)); 
    } 
    catch (...) 
    { 
        BOOST_CHECK_NO_THROW(bfs::remove_all(wdir)); 
        throw; 
    } 
} 

std::pair<bool, std::string> get_var_value(bp::context &ctx, const std::string &var) 
{ 
    std::vector<std::string> args; 
    args.push_back("query-env"); 
    args.push_back(var); 

    ctx.streams[bp::stdout_id] = bpb::pipe(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    bp::pistream is(c.get_handle(bp::stdout_id)); 
    std::string status; 
    is >> status; 
    std::string gotval; 
    if (status == "defined") 
        is >> gotval; 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 

    return std::pair<bool, std::string>(status == "defined", gotval); 
} 

BOOST_AUTO_TEST_CASE(test_clear_environment) 
{ 
    bp::context ctx; 
    ctx.env.erase("TO_BE_QUERIED"); 

#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(setenv("TO_BE_QUERIED", "test", 1) != -1); 
    BOOST_REQUIRE(getenv("TO_BE_QUERIED") != 0); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_REQUIRE(SetEnvironmentVariableA("TO_BE_QUERIED", "test") != 0); 
    char buf[5]; 
    BOOST_REQUIRE(GetEnvironmentVariableA("TO_BE_QUERIED", buf, 5) == 4); 
#endif 

    std::pair<bool, std::string> p = get_var_value(ctx, "TO_BE_QUERIED"); 
    BOOST_REQUIRE(!p.first); 
} 

BOOST_AUTO_TEST_CASE(test_unset_environment) 
{ 
    std::vector<std::string> args; 
    args.push_back("query-env"); 
    args.push_back("TO_BE_UNSET"); 

#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(setenv("TO_BE_UNSET", "test", 1) != -1); 
    BOOST_REQUIRE(getenv("TO_BE_UNSET") != 0); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_REQUIRE(SetEnvironmentVariableA("TO_BE_UNSET", "test") != 0); 
    char buf[5]; 
    BOOST_REQUIRE(GetEnvironmentVariableA("TO_BE_UNSET", buf, 5) == 4); 
#endif 

    bp::context ctx; 
    ctx.env.erase("TO_BE_UNSET"); 
    std::pair<bool, std::string> p = get_var_value(ctx, "TO_BE_UNSET"); 
    BOOST_CHECK(!p.first); 
} 

BOOST_AUTO_TEST_CASE(test_set_environment_var) 
{ 
    std::vector<std::string> args; 
    args.push_back("query-env"); 
    args.push_back("TO_BE_SET"); 

#if defined(BOOST_POSIX_API) 
    unsetenv("TO_BE_SET"); 
    BOOST_REQUIRE(getenv("TO_BE_SET") == 0); 
#elif defined(BOOST_WINDOWS_API) 
    char buf[5]; 
    BOOST_REQUIRE(GetEnvironmentVariableA("TO_BE_SET", buf, 5) == 0 || 
                    SetEnvironmentVariableA("TO_BE_SET", NULL) != 0); 
    BOOST_REQUIRE(GetEnvironmentVariableA("TO_BE_SET", buf, 5) == 0); 
#endif 

    bp::context ctx; 
    ctx.env.insert(bp::environment::value_type("TO_BE_SET", "some-value")); 
    std::pair<bool, std::string> p = get_var_value(ctx, "TO_BE_SET"); 
    BOOST_CHECK(p.first); 
    BOOST_CHECK_EQUAL(p.second, "'some-value'"); 

#if defined(BOOST_POSIX_API) 
    bp::context ctx2; 
    ctx2.env.insert(bp::environment::value_type("TO_BE_SET", "")); 
    std::pair<bool, std::string> p2 = get_var_value(ctx2, "TO_BE_SET"); 
    BOOST_CHECK(p2.first); 
    BOOST_CHECK_EQUAL(p2.second, "''"); 
#endif 
} 

void write_handler(const boost::system::error_code &ec, 
    std::size_t bytes_transferred) 
{ 
    BOOST_REQUIRE_EQUAL(ec, boost::system::error_code()); 
    BOOST_CHECK_EQUAL(bytes_transferred, 5u); 
} 

void read_handler(const boost::system::error_code &ec, 
    std::size_t bytes_transferred, ba::streambuf &buf) 
{ 
    BOOST_REQUIRE_EQUAL(ec, boost::system::error_code()); 
    std::istream is(&buf); 
    std::string line; 
    std::getline(is, line); 
#if defined(BOOST_POSIX_API) 
    BOOST_CHECK_EQUAL(line, "async-test"); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(line, "async-test\r"); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_async) 
{ 
    std::vector<std::string> args; 
    args.push_back("prefix-once"); 
    args.push_back("async-"); 

    bp::context ctx; 
    ctx.streams[bp::stdin_id] = bpb::async_pipe(); 
    ctx.streams[bp::stdout_id] = bpb::async_pipe(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 
    bp::handle in = c.get_handle(bp::stdin_id); 
    bp::handle out = c.get_handle(bp::stdout_id); 

    ba::io_service ioservice; 
    bp::pipe write_end(ioservice, in.release()); 
    bp::pipe read_end(ioservice, out.release()); 

    ba::async_write(write_end, ba::buffer("test\n", 5), write_handler); 
    ba::streambuf buf; 
    ba::async_read_until(read_end, buf, '\n', boost::bind(read_handler, 
        ba::placeholders::error, ba::placeholders::bytes_transferred, 
        boost::ref(buf))); 
    ioservice.run(); 

    int exit_code = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(exit_code)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(exit_code), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(exit_code, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_shell) 
{ 
#if defined(BOOST_POSIX_API) 
    std::string cmd = "echo test | sed 's,^,LINE-,'"; 
#elif defined(BOOST_WINDOWS_API) 
    std::string cmd = "if foo==foo echo LINE-test"; 
#endif 

    bp::context ctx; 
    ctx.streams[bp::stdout_id] = bpb::pipe(); 

    bp::child c = bp::shell(cmd, ctx); 

    std::string word; 
    bp::pistream is(c.get_handle(bp::stdout_id)); 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "LINE-test"); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_null_stdin) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("is-nul-stdin"); 

    bp::context ctx; 
    ctx.streams[bp::stdin_id] = bpb::null(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_null_stdout) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("is-nul-stdout"); 

    bp::context ctx; 
    ctx.streams[bp::stdout_id] = bpb::null(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_null_stderr) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("is-nul-stderr"); 

    bp::context ctx; 
    ctx.streams[bp::stderr_id] = bpb::null(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_inherit_file_descriptor) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("echo-stdout"); 
    args.push_back("message-stdout"); 

    bio::file_descriptor_sink fd("6DB18578-DD0C-4ACB-AFD0-417F5CF2011D.txt"); 

    bp::context ctx; 
    ctx.streams[bp::stdout_id] = bpb::inherit(fd.handle()); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 

    fd.close(); 

    std::string word; 
    std::ifstream is("6DB18578-DD0C-4ACB-AFD0-417F5CF2011D.txt"); 
    is >> word; 
    is.close(); 

    BOOST_CHECK_EQUAL(word, "message-stdout"); 

    BOOST_REQUIRE(bfs::remove("6DB18578-DD0C-4ACB-AFD0-417F5CF2011D.txt")); 
} 

#if defined(BOOST_POSIX_API) 
BOOST_AUTO_TEST_CASE(test_posix) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("posix-echo-one"); 
    args.push_back("10"); 
    args.push_back("test"); 

    bp::context ctx; 
    ctx.streams[10] = bpb::pipe(bp::output_stream); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    std::string word; 
    bp::pistream is(c.get_handle(10)); 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "test"); 

    int s = c.wait(); 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
} 

class raii_dup2 
{ 
public: 
    raii_dup2(int fd1, int fd2) 
        : reverted_(false), 
        fd1_(fd1), 
        fd2_(fd2) 
    { 
        BOOST_REQUIRE(dup2(fd1_, fd2_) != -1); 
    } 

    ~raii_dup2() 
    { 
        if (!reverted_) 
            revert(); 
    } 

    void revert() 
    { 
        reverted_ = true; 
        BOOST_REQUIRE(dup2(fd2_, fd1_) != -1); 
    } 

private: 
    bool reverted_; 
    int fd1_; 
    int fd2_; 
}; 

BOOST_AUTO_TEST_CASE(test_posix2) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("posix-echo-one"); 
    args.push_back("3"); 
    args.push_back("test"); 

    bp::context ctx; 
    ctx.streams[3] = bpb::pipe(bp::output_stream); 

    // File descriptors must be closed after context is instantiated as the 
    // context constructor uses the behavior inherit which tries to dup() 
    // stdin, stdout and stderr. 
    raii_dup2 raii_stdin(STDIN_FILENO, 100); 
    close(STDIN_FILENO); 
    raii_dup2 raii_stdout(STDOUT_FILENO, 101); 
    close(STDOUT_FILENO); 
    raii_dup2 raii_stderr(STDERR_FILENO, 102); 
    close(STDERR_FILENO); 

    ctx.streams[bp::stdin_id] = bpb::null(); 
    ctx.streams[bp::stdout_id] = bpb::null(); 
    ctx.streams[bp::stderr_id] = bpb::null(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    int res = dup2(c.get_handle(3).native(), 0); 
    std::string word; 
    if (res != -1) 
        std::cin >> word; 

    raii_stdin.revert(); 
    raii_stdout.revert(); 
    raii_stderr.revert(); 

    int s = c.wait(); 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 

    BOOST_REQUIRE(res != -1); 
    BOOST_CHECK_EQUAL(word, "test"); 
} 

BOOST_AUTO_TEST_CASE(test_posix_daemon) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("echo-stdout-stderr"); 
    args.push_back("message-to-two-streams"); 

    bp::context ctx; 

    // File descriptors must be closed after context is instantiated as the 
    // context constructor uses the behavior inherit which tries to dup() 
    // stdin, stdout and stderr. 
    raii_dup2 raii_stdin(STDIN_FILENO, 100); 
    close(STDIN_FILENO); 
    raii_dup2 raii_stdout(STDOUT_FILENO, 101); 
    close(STDOUT_FILENO); 
    raii_dup2 raii_stderr(STDERR_FILENO, 102); 
    close(STDERR_FILENO); 

    ctx.streams[bp::stdin_id] = bpb::null(); 
    ctx.streams[bp::stderr_id] = bpb::pipe(); 
    ctx.streams[bp::stdout_id] = bpb::pipe(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    std::string words[4]; 
    bp::pistream isout(c.get_handle(bp::stdout_id)); 
    isout >> words[0] >> words[1]; 
    bp::pistream iserr(c.get_handle(bp::stderr_id)); 
    iserr >> words[2] >> words[3]; 

    raii_stdin.revert(); 
    raii_stdout.revert(); 
    raii_stderr.revert(); 

    int s = c.wait(); 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 

    BOOST_CHECK_EQUAL(words[0], "stdout"); 
    BOOST_CHECK_EQUAL(words[1], "message-to-two-streams"); 
    BOOST_CHECK_EQUAL(words[2], "stderr"); 
    BOOST_CHECK_EQUAL(words[3], "message-to-two-streams"); 
} 
#endif 

#if defined(BOOST_WINDOWS_API) 
STARTUPINFOA sa; 

void setup(STARTUPINFOA &sainfo) 
{ 
    sa.dwFlags = sainfo.dwFlags |= STARTF_USEPOSITION | STARTF_USESIZE; 
    sa.dwX = sainfo.dwX = 100; 
    sa.dwY = sainfo.dwY = 200; 
    sa.dwXSize = sainfo.dwXSize = 640; 
    sa.dwYSize = sainfo.dwYSize = 480; 
} 

BOOST_AUTO_TEST_CASE(test_windows) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("windows-print-startupinfo"); 

    bp::context ctx; 
    ctx.streams[bp::stdout_id] = bpb::pipe(); 
    ctx.setup = &setup; 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    bp::pistream is(c.get_handle(bp::stdout_id)); 
    std::string line; 
    std::getline(is, line); 
    BOOST_CHECK_EQUAL(line, "dwFlags = " + boost::lexical_cast<std::string>( 
        sa.dwFlags) + "\r"); 
    std::getline(is, line); 
    BOOST_CHECK_EQUAL(line, "dwX = " + boost::lexical_cast<std::string>( 
        sa.dwX) + "\r"); 
    std::getline(is, line); 
    BOOST_CHECK_EQUAL(line, "dwY = " + boost::lexical_cast<std::string>( 
        sa.dwY) + "\r"); 
    std::getline(is, line); 
    BOOST_CHECK_EQUAL(line, "dwXSize = " + boost::lexical_cast<std::string>( 
        sa.dwXSize) + "\r"); 
    std::getline(is, line); 
    BOOST_CHECK_EQUAL(line, "dwYSize = " + boost::lexical_cast<std::string>( 
        sa.dwYSize) + "\r"); 

    int s = c.wait(); 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
} 

std::string create_unique_pipe_name() 
{ 
    bu::random_generator gen; 
    bu::uuid u = gen(); 
#if defined(BOOST_POSIX_API) 
    std::string name = "/tmp/boost_process_test_"; 
#elif defined(BOOST_WINDOWS_API) 
    std::string name = "\\\\.\\pipe\\boost_process_test_"; 
#endif 
    return name + boost::lexical_cast<std::string>(u); 
} 

BOOST_AUTO_TEST_CASE(test_sync_io_with_named_pipe) 
{ 
    check_helpers(); 

    std::vector<std::string> args; 
    args.push_back("stdin-to-stdout"); 

    bp::context ctx; 
    ctx.streams[bp::stdin_id] = bpb::named_pipe(create_unique_pipe_name()); 
    ctx.streams[bp::stdout_id] = bpb::named_pipe(create_unique_pipe_name()); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 

    bp::postream os(c.get_handle(bp::stdin_id)); 
    bp::pistream is(c.get_handle(bp::stdout_id)); 

    os << "message-to-process" << std::endl; 
    os.close(); 

    std::string word; 
    is >> word; 
    BOOST_CHECK_EQUAL(word, "message-to-process"); 

    int s = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(s)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(s), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(s, EXIT_SUCCESS); 
#endif 
} 
#endif 
