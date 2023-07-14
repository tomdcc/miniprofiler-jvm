<!DOCTYPE html>
<%@ taglib prefix="miniprofiler" uri="https://miniprofiler-jvm.jdev.io/1.0" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.util.*" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="io.jdev.miniprofiler.MiniProfiler" %>
<%
    DataSource ds = (DataSource) application.getAttribute("dataSource");
    List<Map<String,String>> results = new ArrayList<Map<String,String>>();
    // don't ask me why jetty isn't compiling jsps with java 7 syntax...
    Connection con = null;
    Statement st = null;
    ResultSet rs = null;
    try {
        con = ds.getConnection();
        st = con.createStatement();
        rs = st.executeQuery("select * from people");
        while(rs.next()) {
            Map<String,String> row = new HashMap<String,String>();
            row.put("id", rs.getString("id"));
            row.put("name", rs.getString("name"));
            results.add(row);
        }
    } catch(SQLException sqle) {
        throw new ServletException(sqle);
    } finally {
        if(rs != null) try { rs.close(); } catch(SQLException s2) {}
        if(st != null) try { st.close(); } catch(SQLException s2) {}
        if(con != null) try { con.close(); } catch(SQLException s2) {}
    }
    // make a predictable delay after the queries for the int test
    Thread.sleep(200);
%>
<html>
<head>
    <title>MiniProfiler Servlet Test</title>
    <link href="bootstrap.min.css" rel="stylesheet"/>
    <script src="jquery-2.0.3.min.js"></script>
</head>
<body>
    <div class="container">
        <h1>MiniProfiler Servlet Test</h1>

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
