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

package org.jboss.as.jpa.service;

import org.jboss.as.jpa.config.PersistenceUnitMetadata;
import org.jboss.as.server.deployment.DeploymentUnit;

import javax.persistence.spi.PersistenceUnitInfo;
import java.util.concurrent.ConcurrentHashMap;

/**
 * registry of all deployed persistence unit definitions
 *
 * @author Scott Marlow
 */
public class PersistenceUnitRegistry {

    /**
     * Map of Persistence units keyed by the scoped persistence unit name.  The scoped name identifies the deployment
     * location.
     */
    private static ConcurrentHashMap<String, PersistenceUnitInfo> persistenceUnits = new ConcurrentHashMap<String, PersistenceUnitInfo>();


    public static PersistenceUnitInfo getPersistenceUnit(String scopedPersistenceUnitName) {
        return persistenceUnits.get(scopedPersistenceUnitName);
    }

    public static void register(PersistenceUnitInfo container) {
        // internal error check
        if (!(container instanceof PersistenceUnitMetadata)) {
            throw new RuntimeException("Persistence unit cannot be registered without using a PersistenceUnitMetadata implementation of PU");
        }
        PersistenceUnitMetadata pu = (PersistenceUnitMetadata)container;
        String name = pu.getScopedPersistenceUnitName();
        if (persistenceUnits.containsKey(name))
            throw new RuntimeException("Persistence Unit is already registered: " + name);
        persistenceUnits.put(pu.getScopedPersistenceUnitName(), pu);
    }

    public static void unregister(PersistenceUnitInfo container) {
        // internal error check
        if (!(container instanceof PersistenceUnitMetadata)) {
            throw new RuntimeException("Persistence unit cannot be registered without using a PersistenceUnitMetadata implementation of PU");
        }
        PersistenceUnitMetadata pu = (PersistenceUnitMetadata)container;
        String name = pu.getScopedPersistenceUnitName();
        Object removedValue = persistenceUnits.remove(name);
        // if we have a leak due to have the wrong name, we need to know.
        // Also need to know if we attempt to remove the same unit multiple times.
        if (removedValue != null) {
            throw new RuntimeException("Could not remove Persistence Unit " + name);
        }
    }

}
