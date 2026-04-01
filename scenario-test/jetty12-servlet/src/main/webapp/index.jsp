<!DOCTYPE html>
<%@ taglib prefix="miniprofiler" uri="https://miniprofiler-jvm.jdev.io/1.0" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.util.*" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="jakarta.servlet.ServletException" %>
<%@ page import="io.jdev.miniprofiler.MiniProfiler" %>
<%
    DataSource ds = (DataSource) application.getAttribute("dataSource");
    List<Map<String,String>> results = new ArrayList<>();
    try (Connection con = ds.getConnection();
         Statement st = con.createStatement();
         ResultSet rs = st.executeQuery("select * from people")) {
        while(rs.next()) {
            Map<String,String> row = new HashMap<>();
            row.put("id", rs.getString("id"));
            row.put("name", rs.getString("name"));
            results.add(row);
        }
    } catch(SQLException sqle) {
        throw new ServletException(sqle);
    }
    // make a predictable delay after the queries for the int test
    Thread.sleep(200);
%>
<html>
<head>
    <title>MiniProfiler Jetty 12 Servlet Test</title>
    <link href="bootstrap.min.css" rel="stylesheet"/>
</head>
<body>
    <div class="container">
        <h1>MiniProfiler Jetty 12 Servlet Test</h1>

        <h2>People</h2>
        <table class='table table-striped'>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                </tr>
            </thead>
            <tbody>
            <% for(Map<String,String> row : results) { %>
                <tr>
                    <td><%= row.get("id") %></td>
                    <td><%= row.get("name") %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
    <% if ("true".equals(request.getParameter("override"))) { %>
        <miniprofiler:script
                profilerProvider="<%= MiniProfiler.getProfilerProvider() %>"
                position="left"
                colorScheme="dark"
                toggleShortcut="none"
                maxTraces="99"
                trivialMilliseconds="66"
                trivial="true"
                children="true"
                controls="true"
        />
    <% } else { %>
        <miniprofiler:script/>
    <% } %>
</body>
</html>
