<!DOCTYPE html>
<%@ page import="java.util.*" %>
<%@ page import="io.jdev.miniprofiler.json.ScriptTagWriter" %>
<%@ page import="io.jdev.miniprofiler.MiniProfiler" %>
<%@ page import="io.jdev.miniprofiler.wildfly8.funtest.Person" %>
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
                    <th>First name</th>
                    <th>Last name</th>
                </tr>
            </thead>
            <tbody>
            <% for(Person p : (List<Person>) request.getAttribute("people")) { %>
                <tr>
                    <td><%= p.getId() %></td>
                    <td><%= p.getFirstName() %></td>
                    <td><%= p.getLastName() %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
    <%= new ScriptTagWriter().printScriptTag(MiniProfiler.getCurrentProfiler(), request.getContextPath() + "/miniprofiler")%>
</body>
</html>
