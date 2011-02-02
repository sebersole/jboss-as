/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.common;

import java.util.Collection;

import org.jboss.arquillian.spi.DeploymentPackager;
import org.jboss.arquillian.spi.TestDeployment;
import org.jboss.shrinkwrap.api.Archive;

/**
 * A {@link DeploymentPackager} that for AS7 test deployments.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public class JBossASDeploymentPackager implements DeploymentPackager {

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment) {

        // Arquillian generates auxiliary archives that aren't bundles
        Collection<Archive<?>> auxArchives = testDeployment.getAuxiliaryArchives();
        // auxArchives.clear();

        Archive<?> appArchive = testDeployment.getApplicationArchive();
        for (Archive<?> aux : auxArchives) {
            appArchive.merge(aux);
        }

        return appArchive;
    }
}
