<!DOCTYPE html>
<%@ taglib prefix="miniprofiler" uri="https://miniprofiler-jvm.jdev.io/1.0" %>
<%@ page import="java.util.*" %>
<%@ page import="io.jdev.miniprofiler.glassfish4.funtest.Person" %>
<html>
<head>
    <title>MiniProfiler Wildfly 8 Test</title>
    <link href="bootstrap.min.css" rel="stylesheet"/>
    <script src="jquery-2.0.3.min.js"></script>
</head>
<body>
    <div class="container">
        <h1>MiniProfiler Wildfly 8 Test</h1>

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
    <miniprofiler:script path="/admin/miniprofiler"/>
</body>
</html>
