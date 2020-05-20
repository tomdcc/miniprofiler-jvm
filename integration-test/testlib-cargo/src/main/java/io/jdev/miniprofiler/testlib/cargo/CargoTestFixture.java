/*
 * Copyright 2020 the original author or authors.
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

package io.jdev.miniprofiler.testlib.cargo;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

import java.net.URL;

import static org.codehaus.cargo.container.ContainerType.INSTALLED;
import static org.codehaus.cargo.container.configuration.ConfigurationType.STANDALONE;

class CargoTestFixture {

    private final String containerId;
    private final URL downloadUrl;
    private final String warPath;
    private final int port;
    private final String context;

    private InstalledLocalContainer container;

    public CargoTestFixture(String containerId, URL downloadUrl, int port, String warPath, String context) {
        this.containerId = containerId;
        this.downloadUrl = downloadUrl;
        this.warPath = warPath;
        this.port = port;
        this.context = context;
    }

    void start() {
        System.out.println("Installing from " + downloadUrl.toExternalForm());
        Installer installer = new ZipURLInstaller(downloadUrl);
        installer.install();
        System.out.println("Installed " + containerId + " into " + installer.getHome());

        LocalConfiguration configuration = (LocalConfiguration) new DefaultConfigurationFactory().createConfiguration(containerId, INSTALLED, STANDALONE);
        configuration.setProperty(ServletPropertySet.PORT, String.valueOf(port));
        container = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(containerId, INSTALLED, configuration);
        container.setHome(installer.getHome());

        WAR war = new WAR(warPath);
        war.setContext(context);
        configuration.addDeployable(war);

        container.start();
        System.out.println("Started " + containerId + " on port " + container.getConfiguration().getPropertyValue(ServletPropertySet.PORT));

    }

    void stop() {
        container.stop();
    }
}
