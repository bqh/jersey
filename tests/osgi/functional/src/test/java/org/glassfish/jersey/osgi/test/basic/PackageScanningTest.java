/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.osgi.test.basic;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.glassfish.jersey.osgi.test.util.Helper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.TestProperties;

import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(JUnit4TestRunner.class)
public class PackageScanningTest {

    private static final int port = Helper.getEnvVariable(TestProperties.CONTAINER_PORT, 8080);

    private static final String CONTEXT = "/jersey";

    private static final URI baseUri = UriBuilder.
            fromUri("http://localhost").
            port(port).
            path(CONTEXT).build();

    @Configuration
    public static Option[] configuration() {
        List<Option> options = Helper.getCommonOsgiOptions();

        options.addAll(Helper.expandedList(
                // vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

                mavenBundle().groupId("org.glassfish.jersey.media").artifactId("jersey-media-sse").versionAsInProject(),

                mavenBundle().groupId("javax.servlet").artifactId("javax.servlet-api").version("3.1-b02"),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-http-servlet").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.containers").artifactId("jersey-container-servlet-core").
                        versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.containers").artifactId("jersey-container-grizzly2-servlet").
                        versionAsInProject(),

                // MBR/MBW for JSON-P is on the classpath.
                mavenBundle().groupId("javax.json").artifactId("javax.json-api").versionAsInProject()
        ));

        options = Helper.addPaxExamMavenLocalRepositoryProperty(options);
        return Helper.asArray(options);
    }

    @Test
    public void testSimpleResource() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig().packages(SimpleResource.class.getPackage().getName());
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);

        _testSimpleResource(server);
    }

    @Test
    public void testSimpleResourceInitParameters() throws Exception {
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put(
                ServerProperties.PROVIDER_PACKAGES,
                SimpleResource.class.getPackage().getName());
        final HttpServer server = GrizzlyWebContainerFactory.create(baseUri, ServletContainer.class, initParams);

        _testSimpleResource(server);
    }

    private void _testSimpleResource(final HttpServer server) throws Exception {
        final Client client = ClientBuilder.newClient();
        final String response = client.target(baseUri).path("/simple").request().get(String.class);

        System.out.println("RESULT = " + response);
        assertEquals("OK", response);

        server.stop();
    }

}
