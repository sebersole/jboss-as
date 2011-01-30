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

package org.jboss.as.jpa.processor;

import org.jboss.as.jpa.config.PersistenceUnitMetadata;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.service.PersistenceUnitServiceWrapper;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import java.util.HashMap;
import java.util.List;

/**
 * Handle the installation of the Persistence Unit deployment
 *
 * @author Scott Marlow
 */
public class PersistenceUnitDeploymentProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.jpa");

    private static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("jpa", "deployment", "persistenceunit");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // handleWarDeployment(phaseContext);
        // handleEarDeployment(phaseContext);
        handleJarDeployment(phaseContext);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // TODO:  undeploy
    }

    private void handleJarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!isEarDeployment(deploymentUnit) && !isWarDeployment(deploymentUnit)) {
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            PersistenceUnitMetadataHolder holder;
            if(deploymentRoot != null &&
                    (holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS)) != null &&
                    holder.getPersistenceUnits().size() > 0) {
                // assemble and install the PU service
                final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
                if (module == null)
                    throw new DeploymentUnitProcessingException("Failed to get module attachment for " + phaseContext.getDeploymentUnit());

                final ClassLoader classLoader = module.getClassLoader();
                for(PersistenceUnitMetadata pu:holder.getPersistenceUnits()) {
                    pu.setClassLoader(classLoader);
                    pu.setScopedPersistenceUnitName(createScopedName(deploymentUnit, module, pu.getPersistenceUnitName()));

                    PersistenceProvider provider = lookupProvider(pu.getPersistenceProviderClassName());
                    log.info("got provider= " + provider);
                    HashMap properties = new HashMap();
                    properties.put("javax.persistence.validation.factory", null);   // TODO:  pass the validator

                    // TODO:  Move the JNDI datasource lookup to here since the parse phase seems to early

                    //String datasourceName = pu.getJtaDataSourceName();
                    // DataSourcesService
                    //final BinderService<DataSource> applicationNameBinder = new BinderService<DataSource>(datasourceName, Values
                    //        .immediateValue(deploymentUnit.getName()));
                    //serviceTarget.addService(applicationContextServiceName.append("AppName"), applicationNameBinder).addDependency(
                    //        applicationContextServiceName, Context.class, applicationNameBinder.getContextInjector()).install();

                    EntityManagerFactory emf = provider.createContainerEntityManagerFactory(pu, properties);

                    try {

                        ServiceName namespaceSelectorServiceName = SERVICE_NAME_BASE.append(phaseContext.getDeploymentUnit().getName());
                        PersistenceUnitServiceWrapper service = new PersistenceUnitServiceWrapper();
                        ServiceTarget serviceTarget = phaseContext.getServiceTarget();
                        serviceTarget.addService(namespaceSelectorServiceName, service)
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .install();
                    } catch (ServiceRegistryException e) {
                        throw new DeploymentUnitProcessingException("Failed to add persistence unit service", e);
                    }
                }
            }
        }
    }

    /**
     * Look up the persistence provider
     * @param providerName
     * @return
     */
    private PersistenceProvider lookupProvider(String providerName) {
        List<PersistenceProvider> providers =
                PersistenceProviderResolverHolder.getPersistenceProviderResolver().getPersistenceProviders();
        for(PersistenceProvider provider: providers) {
            if (provider.getClass().getName().equals(providerName)) {
                return provider;
            }
        }
        StringBuilder sb = new StringBuilder();
        for(PersistenceProvider provider: providers) {
            sb.append(provider.getClass().getName()).append(", ");
        }
        throw new RuntimeException("PersistenceProvider '" + providerName + "' not found in {" + sb.toString() + "}");
    }

    private String createScopedName(DeploymentUnit deploymentUnit, Module module, String persistenceUnitName)
      {
         // persistenceUnitName must be a simple name
         assert persistenceUnitName.indexOf('/') == -1;
         assert persistenceUnitName.indexOf('#') == -1;

         String appName =  deploymentUnit.getName();
         String modulePath = "" ;//module.getModuleLoader().toString(); // javaEEModuleInformer.getModulePath(deploymentUnit);
         String unitName = (appName != null ? appName + "/" : "") + modulePath + "#" + persistenceUnitName;
         return "persistence.unit:unitName=" + unitName;
      }

    private static boolean isEarDeployment(final DeploymentUnit context) {
        final Boolean result = context.getAttachment(Attachments.EAR_DEPLOYMENT_MARKER);
        return result != null && result;

    }

    private static boolean isWarDeployment(final DeploymentUnit context) {
        final Boolean result = context.getAttachment(Attachments.WAR_DEPLOYMENT_MARKER);
        return result != null && result;
    }

}
