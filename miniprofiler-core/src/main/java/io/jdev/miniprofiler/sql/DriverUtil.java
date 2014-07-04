/*
 * Copyright 2014 the original author or authors.
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

package io.jdev.miniprofiler.sql;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public class DriverUtil {

	public static void deregisterDriverSpy() {
		deregisterDriverFromClassloader("io.jdev.miniprofiler.sql.log4jdbc.DriverSpy", Thread.currentThread().getContextClassLoader());
	}

	public static void deregisterDriverFromClassloader(String driverClassName, ClassLoader cl) {
		// pass a class name through rather than a class so that we don't end up loading it accidentally

		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			Class<? extends Driver> driverClass = driver.getClass();
			if (driverClass.getName().equals(driverClassName) && driverClass.getClassLoader() == cl) {
				// This driver was registered by the app's ClassLoader, so deregister it:
				try {
					DriverManager.deregisterDriver(driver);
				} catch (SQLException ex) {
					// oh well
				}
			}
		}
	}
}
