/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.processors.parsing;

// import static org.jboss.as.web.deployment.WarDeploymentMarker.isWarDeployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Persistence unit deployment unit processor
 *
 * @author Scott Marlow
 */
public class PersistenceUnitDUP implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        handleWarDeployment(phaseContext);

        // TODO:  handle an EJB-JAR file
        // handleEJBDeployment

        // TODO:  handle jar file in the EAR library directory
        // handleEARDeployment

        // TODO:  application client deployment (probably would be a separate DUP class)
        // handle client deployment


    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // TODO:  undeploy
    }

    private void handleWarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isWarDeployment(deploymentUnit)) {
            // handle WEB-INF/classes/META-INF/persistence.xml or
            // a jar file in the WEB-INF/lib directory of a WAR file


        }
    }

    // TODO:
    static boolean isWarDeployment(final DeploymentUnit context) {
        final Boolean result = context.getAttachment(Attachments.WAR_DEPLOYMENT_MARKER);
        return result != null && result;
    }

}
