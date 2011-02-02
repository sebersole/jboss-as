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
            PersistenceMetadataHolder holder = flatten(listPUHolders);
            // save the persistent unit definitions for the WAR
            deploymentUnit.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS, holder);
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

            // look for persistence.xml in jar files in the META-INF/persistence.xml directory
            List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
            assert resourceRoots != null;
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (resourceRoot.getRoot().getLowerCaseName().endsWith(".jar")) {
                    persistence_xml = resourceRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
                    parse(persistence_xml, listPUHolders);
                }
            }
            PersistenceMetadataHolder holder = flatten(listPUHolders);
            // save the persistent unit definitions for the WAR
            deploymentUnit.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS, holder);
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

            // TODO: refactor handleWarDeployment/handleJarDeployment and the following code to share same logic.
            // after proving that handleEarDeployment works (handleWarDeployment/handleJarDeployment currently work).

            // look for persistence.xml in jar/war files
            List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
            assert resourceRoots != null;
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (resourceRoot.getRoot().getLowerCaseName().endsWith(".jar")) {
                    persistence_xml = resourceRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
                    parse(persistence_xml, listPUHolders);
                }
                else if (resourceRoot.getRoot().getLowerCaseName().endsWith(".war")) {
                    // TODO:  delete this entire else block after EAR deployment support is complete
                    // as the war will be deployed as a separate subdeployment with the ear as its parent.
                    persistence_xml = resourceRoot.getRoot().getChild(WEB_PERSISTENCE_XML);
                    parse(persistence_xml, listPUHolders);

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
                            persistence_xml = jar.getChild(META_INF_PERSISTENCE_XML);
                            parse(persistence_xml, listPUHolders);
                        }
                    }
                }
            PersistenceMetadataHolder holder = flatten(listPUHolders);
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

    // TODO:  FIX ME.  this is the wrong approach, we cannot flatten by name, instead we need to
    // associate the PU with the containing component.  Then we can do proper determination
    // of which PU to use (and support # references).
    //
    // as6 example of a scoped pu reference:
    // persistence.unit:unitName=ejb3_ext_propagation.ear/lib/ejb3_ext_propagation.jar#CTS-EXT-UNIT
    // The above was associated with the injected EM referencing PU name=CTS-EXT-UNIT
    /**
     *  8.2.2 Persistence Unit Scope
     *
     * An EJB-JAR, WAR, application client jar, or EAR can define a persistence unit.  When referencing a persistence unit using
     * the unitName annotation element or persis-tence-unit-name deployment descriptor element, the visibility scope of the
     * persistence unit is determined by its point of definition
     *
     * A persistence unit that is defined at the level of an EJB-JAR, WAR, or application client jar is scoped to that EJB-JAR,
     * WAR, or application jar respectively and is visible to the components defined in that jar or war.
     *
     * A persistence unit that is defined at the level of the EAR is generally visible to all components in the application.
     * However, if a persistence unit of the same name is defined by an EJB-JAR, WAR, or application jar file within the EAR,
     * the persistence unit of that name defined at EAR level will not be visible to the components defined by that EJB-JAR,
     * WAR, or application jar file unless the persistence unit reference uses the persistence unit name # syntax to specify
     * a path name to disambiguate the reference. When the # syntax is used, the path name is relative to the referencing
     * application component jar file. For example, the syntax ../lib/persistenceUnitRoot.jar#myPersistenceUnit refers to a
     * persistence unit whose name, as specified in the name element of the persistence.xml file, is myPersistenceUnit and for
     * which the relative path name of the root of the persistence unit is ../lib/persistenceUnitRoot.jar. The # syntax may be
     * used with both the unitName annotation element or persistence-unit-name deployment descriptor element to reference a
     * persistence unit defined at EAR level.
     *
     */
    private PersistenceMetadataHolder flatten(List<PersistenceMetadataHolder>listPUHolders) {
        // eliminate duplicates (keeping the first instance of each PU by name)
        Map<String, PersistenceUnitInfo> flattened = new HashMap<String,PersistenceUnitInfo>();
        for (PersistenceMetadataHolder puHolder : listPUHolders ) {
            for (PersistenceUnitInfo pu: puHolder.getPersistenceUnits()) {
                if(!flattened.containsKey(pu.getPersistenceUnitName()))
                    flattened.put(pu.getPersistenceUnitName(), pu);
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
