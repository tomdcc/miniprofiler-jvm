<!DOCTYPE html>
<%@ taglib prefix="miniprofiler" uri="https://miniprofiler-jvm.jdev.io/1.0" %>
<%@ page import="java.util.*" %>
<%@ page import="io.jdev.miniprofiler.glassfish7.funtest.Person" %>
<html>
<head>
    <title>MiniProfiler GlassFish 7 Test</title>
    <link href="bootstrap.min.css" rel="stylesheet"/>
</head>
<body>
    <div class="container">
        <h1>MiniProfiler GlassFish 7 Test</h1>

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
