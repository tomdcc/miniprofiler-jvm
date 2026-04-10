/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jdev.miniprofiler.wildfly27.funtest;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Servlet that renders the index page listing all people, for the WildFly 27 functional test application. */
@WebServlet("")
public class IndexServlet extends HttpServlet {

    @EJB
    private PersonService personService;

    /**
     * Handles GET requests by loading all people and forwarding to the index JSP.
     *
     * @param req  the HTTP request
     * @param resp the HTTP response
     * @throws ServletException if the request cannot be handled
     * @throws IOException      on I/O error
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("people", personService.getAllPeople());
        getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
    }
}
