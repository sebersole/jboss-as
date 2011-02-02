/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.config;

import java.io.File;

import javax.management.MBeanServer;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.common.management.AbstractServerConfig;
import org.jboss.wsf.common.management.AbstractServerConfigMBean;

/**
 * AS specific ServerConfig.
 *
 * @author <a href="mailto:asoldano@redhat.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
public final class ServerConfigImpl extends AbstractServerConfig implements AbstractServerConfigMBean {

    //TODO evaluate if the mbeanServer dependency can be removed for AS7
    private final InjectedValue<MBeanServer> mbeanServer;
    private final InjectedValue<ServerEnvironment> serverEnvironment;

    /**
     * Constructor.
     */
    public ServerConfigImpl(InjectedValue<MBeanServer> mbeanServer, InjectedValue<ServerEnvironment> serverEnvironment) {
        super();
        this.mbeanServer = mbeanServer;
        this.serverEnvironment = serverEnvironment;
    }

    /**
     * Gets server temp directory.
     *
     * @return temp directory
     */
    public File getServerTempDir() {
        return this.getServerEnvironment().getServerTempDir();
    }

    /**
     * Gets server home directory.
     *
     * @return home directory
     */
    public File getHomeDir() {
        return this.getServerEnvironment().getHomeDir();
    }

    /**
     * Gets server data directory.
     *
     * @return data directory
     */
    public File getServerDataDir() {
        return this.getServerEnvironment().getServerDataDir();
    }

    private ServerEnvironment getServerEnvironment() {
        return serverEnvironment.getValue();
    }

    @Override
    public MBeanServer getMbeanServer() {
        return mbeanServer.getValue();
    }

    @Override
    public void setMbeanServer(MBeanServer mbeanServer) {
        throw new RuntimeException(this.getClass()
                + " does not support setting MBeanServer instance; the value should have already been automatically injected");
    }
}
