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

import org.jboss.as.jpa.config.PersistenceMetadataHolder;
import org.jboss.as.jpa.config.parser.application.PersistenceUnitXmlParser;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.parser.util.NoopXmlResolver;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.SuffixMatchFilter;

import javax.persistence.spi.PersistenceUnitInfo;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Persistence unit deployment unit processor
 *
 * The jar file/directory whose META-INF directory contains the persistence.xml file is termed the root of the persistence
 * unit.
 * root of a persistence unit must be one of the following:
 *  EJB-JAR file
 *  the WEB-INF/classes directory of a WAR file
 *  jar file in the WEB-INF/lib directory of a WAR file
 *  jar file in the EAR library directory
 *  application client jar file
 *
 * @author Scott Marlow
 */
public class PersistenceUnitDUP implements DeploymentUnitProcessor {

    private static final String WEB_PERSISTENCE_XML = "WEB-INF/classes/META-INF/persistence.xml";
    private static final String META_INF_PERSISTENCE_XML = "META-INF/persistence.xml";
    private static final Logger log = Logger.getLogger("org.jboss.jpa");
    private static final SuffixMatchFilter JARFILES = new SuffixMatchFilter(".jar");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        handleWarDeployment(phaseContext);
        handleEarDeployment(phaseContext);
        handleJarDeployment(phaseContext);

        // TODO:  find the application client deployment handling and handle client deployment of persistence units (delete
        // this reminder comment after its handled).

    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // TODO:  undeploy
        // TODO:  make sure that JPA section 9.1 is handled by calling EntityManagerFactory.close()
    }

    private void handleJarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            if (!isEarDeployment(deploymentUnit) && !isWarDeployment(deploymentUnit)) {
                // handle META-INF/persistence.xml
            // ordered list of PUs
            List<PersistenceMetadataHolder> listPUHolders = new ArrayList<PersistenceMetadataHolder>(1);
            // handle META-INF/persistence.xml
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            VirtualFile persistence_xml = deploymentRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
            parse(persistence_xml, listPUHolders);
            PersistenceMetadataHolder holder = normalize(listPUHolders);
            // save the persistent unit definitions
            // deploymentUnit.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS, holder);
            deploymentRoot.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS, holder);
            }
        }

    private void handleWarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isWarDeployment(deploymentUnit)) {
            // ordered list of PUs
            List<PersistenceMetadataHolder> listPUHolders = new ArrayList<PersistenceMetadataHolder>(1);

            // handle WEB-INF/classes/META-INF/persistence.xml
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            VirtualFile persistence_xml = deploymentRoot.getRoot().getChild(WEB_PERSISTENCE_XML);
            parse(persistence_xml, listPUHolders);
            deploymentRoot.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS,  normalize(listPUHolders));

            // look for persistence.xml in jar files in the META-INF/persistence.xml directory
            List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
            assert resourceRoots != null;
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (resourceRoot.getRoot().getLowerCaseName().endsWith(".jar")) {
                    listPUHolders = new ArrayList<PersistenceMetadataHolder>(1);
                    persistence_xml = resourceRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
                    parse(persistence_xml, listPUHolders);
                    resourceRoot.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS, normalize(listPUHolders));
                }
            }
        }
    }

    private void handleEarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isEarDeployment(deploymentUnit)) {
            // ordered list of PUs
            List<PersistenceMetadataHolder> listPUHolders = new ArrayList<PersistenceMetadataHolder>(1);
            // handle META-INF/persistence.xml
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            VirtualFile persistence_xml = deploymentRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
            parse(persistence_xml, listPUHolders);
            deploymentRoot.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS, normalize(listPUHolders));

            // TODO: refactor handleWarDeployment/handleJarDeployment and the following code to share same logic.
            // after proving that handleEarDeployment works (handleWarDeployment/handleJarDeployment currently work).

            // look for persistence.xml in jar/war files
            List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
            assert resourceRoots != null;
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (resourceRoot.getRoot().getLowerCaseName().endsWith(".jar")) {
                    listPUHolders = new ArrayList<PersistenceMetadataHolder>(1);
                    persistence_xml = resourceRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
                    parse(persistence_xml, listPUHolders);
                    resourceRoot.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS, normalize(listPUHolders));
                }
                else if (resourceRoot.getRoot().getLowerCaseName().endsWith(".war")) {
                    // TODO:  delete this entire else block after war subdeployments are working
                    // as the war will be deployed as a separate subdeployment with the ear as its parent.
                    listPUHolders = new ArrayList<PersistenceMetadataHolder>(1);
                    persistence_xml = resourceRoot.getRoot().getChild(WEB_PERSISTENCE_XML);
                    parse(persistence_xml, listPUHolders);
                    resourceRoot.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS,  normalize(listPUHolders));

                    // look for persistence.xml in jar files in the META-INF/persistence.xml directory
                    List<VirtualFile> jars;
                    try {
                        jars = resourceRoot.getRoot().getChildren(JARFILES);
                    } catch (IOException e) {
                        throw new DeploymentUnitProcessingException("error occurred while looking for jar files within " +
                                resourceRoot.getRoot().getName() + ", that is in " + deploymentRoot.getRootName(), e);
                    }

                    assert jars != null;
                    for (VirtualFile jar : jars) {
                        // TODO:  this will all occur in war subdeployment, but if not, need to attach to a resource...
                        persistence_xml = jar.getChild(META_INF_PERSISTENCE_XML);
                        parse(persistence_xml, listPUHolders);
                        }
                    }
                }
            PersistenceMetadataHolder holder = normalize(listPUHolders);
            // save the persistent unit definitions for the WAR
            deploymentUnit.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS, holder);
        }
    }

    private void parse(VirtualFile persistence_xml, List<PersistenceMetadataHolder> listPUHolders) throws DeploymentUnitProcessingException {
        if (persistence_xml.exists() && persistence_xml.isFile()) {
            InputStream is = null;
            try {
                is = persistence_xml.openStream();
                final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setXMLResolver(NoopXmlResolver.create());
                XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
                PersistenceMetadataHolder puHolder = PersistenceUnitXmlParser.parse(xmlReader);
                listPUHolders.add(puHolder);
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to parse " + persistence_xml, e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Eliminate duplicate PU definitions from clustering the deployment (first definition will win)
     * @param listPUHolders
     * @return
     */
    private PersistenceMetadataHolder normalize(List<PersistenceMetadataHolder> listPUHolders) {
        // eliminate duplicates (keeping the first instance of each PU by name)
        Map<String, PersistenceUnitInfo> flattened = new HashMap<String,PersistenceUnitInfo>();
        for (PersistenceMetadataHolder puHolder : listPUHolders ) {
            for (PersistenceUnitInfo pu: puHolder.getPersistenceUnits()) {
                if(!flattened.containsKey(pu.getPersistenceUnitName()))
                    flattened.put(pu.getPersistenceUnitName(), pu);
                else
                    log.info("ignoring duplicate Persistence Unit definition for " + pu.getPersistenceUnitName());
            }
        }
        PersistenceMetadataHolder holder = new PersistenceMetadataHolder();
        holder.setPersistenceUnits(new ArrayList<PersistenceUnitInfo>(flattened.values()));
        return holder;
    }

    static boolean isEarDeployment(final DeploymentUnit context) {
        final Boolean result = context.getAttachment(Attachments.EAR_DEPLOYMENT_MARKER);
        return result != null && result;

    }

    static boolean isWarDeployment(final DeploymentUnit context) {
        final Boolean result = context.getAttachment(Attachments.WAR_DEPLOYMENT_MARKER);
        return result != null && result;
    }

}
