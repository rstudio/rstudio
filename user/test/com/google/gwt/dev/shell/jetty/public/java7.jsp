<%@page language="java" contentType="text/plain; charset=UTF-8" session="false" %>
<%
// Use a switch-on-string to check whether the page compiles as Java 7
switch (request.getMethod()) {
case "GET":
    out.print("OK");
    break;
default:
    out.print(request.getMethod());
}
%>
