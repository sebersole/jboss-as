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

package org.jboss.as.jpa.spi;

import org.jboss.as.server.deployment.DeploymentUnit;

import javax.persistence.spi.PersistenceUnitInfo;


/**
 * The SPI for the persistence unit service.
 *
 * @author Scott Marlow
 */
public interface PersistenceUnitService {

    /**
     * Resolve persistence unit by persistence unit name within the specified deployment.
     *
     * returns fully qualified (scoped) persistence unit name
     *
     */
    String resolvePersistenceUnitSupplier(DeploymentUnit du, String persistenceUnitName);


    /**
     * Resolve persistence unit by "scoped" persistent unit name.  The scoped name includes the
     * application deployment unit name.
     *
     * e.g. ejb3_ext_propagation.ear/lib/ejb3_ext_propagation.jar#CTS-EXT-UNIT
     *
     */
    PersistenceUnitInfo getPersistenceUnit(String scopedPersistenceUnitName);


}
