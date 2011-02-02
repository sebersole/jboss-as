/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.processor;

import java.lang.reflect.Method;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycleConfiguration;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycleMethod;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.modules.Module;

/**
 * @author John Bailey
 */
public class LifecycleInstallProcessor extends AbstractComponentConfigProcessor {
    protected void processComponentConfig(DeploymentUnit deploymentUnit, DeploymentPhaseContext phaseContext, ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();

        // Process the component's PostConstruct methods
        for (ComponentLifecycleConfiguration lifecycleConfiguration : componentConfiguration.getPostConstructLifecycleConfigurations()) {
            try {
                componentConfiguration.addPostConstructLifecycle(createLifecycleInterceptor(classLoader, componentConfiguration, lifecycleConfiguration));
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to create lifecycle interceptor instance: " + lifecycleConfiguration.getMethodName(), e);
            }
        }

        // Process the component's PreDestroy methods
        for (ComponentLifecycleConfiguration lifecycleConfiguration : componentConfiguration.getPreDestroyLifecycleConfigurations()) {
            try {
                componentConfiguration.addPreDestroyLifecycel(createLifecycleInterceptor(classLoader, componentConfiguration, lifecycleConfiguration));
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to create lifecycle interceptor instance: " + lifecycleConfiguration.getMethodName(), e);
            }
        }
    }

    private ComponentLifecycle createLifecycleInterceptor(final ClassLoader classLoader, final ComponentConfiguration componentConfiguration, final ComponentLifecycleConfiguration lifecycleConfiguration) throws NoSuchMethodException, ClassNotFoundException {
        final Class<?> interceptorClass = classLoader.loadClass(componentConfiguration.getComponentClassName());
        final Method lifecycleMethod = interceptorClass.getMethod(lifecycleConfiguration.getMethodName());
        return new ComponentLifecycleMethod(lifecycleMethod);
    }
}
