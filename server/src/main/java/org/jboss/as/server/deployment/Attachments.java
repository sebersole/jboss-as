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

package org.jboss.as.server.deployment;

import java.util.jar.Manifest;

import org.jboss.as.server.deployment.annotation.AnnotationIndexProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.as.server.deployment.module.ClassPathEntry;
import org.jboss.as.server.deployment.module.DeploymentModuleLoader;
import org.jboss.as.server.deployment.module.ExtensionListEntry;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Attachments {

    //
    // GENERAL
    //
    /**
     * A list of service dependencies that must be satisfied before the next deployment phase can begin executing.
     */
    public static final AttachmentKey<AttachmentList<ServiceName>> NEXT_PHASE_DEPS = AttachmentKey.createList(ServiceName.class);


    /**
     * The deployments runtime name
     */
    public static final AttachmentKey<String> RUNTIME_NAME = AttachmentKey.create(String.class);

    /**
     * The deployment hash
     */
    public static final AttachmentKey<byte[]> DEPLOYMENT_HASH = AttachmentKey.create(byte[].class);


    //
    // STRUCTURE
    //

    /**
     * The primary deployment root.
     */
    public static final AttachmentKey<ResourceRoot> DEPLOYMENT_ROOT = AttachmentKey.create(ResourceRoot.class);
    /**
     * The additional resource roots of the deployment unit.
     */
    public static final AttachmentKey<AttachmentList<ResourceRoot>> RESOURCE_ROOTS = AttachmentKey.createList(ResourceRoot.class);
    /**
     * The MANIFEST.MF of the deployment unit.
     */
    public static final AttachmentKey<Manifest> MANIFEST = AttachmentKey.create(Manifest.class);
    /**
     * Available when the deployment contains a valid OSGi manifest
     */
    public static final AttachmentKey<Manifest> OSGI_MANIFEST = AttachmentKey.create(Manifest.class);

    /**
     * The list of class path entries given in the manifest and structure configurations.
     */
    public static final AttachmentKey<AttachmentList<ClassPathEntry>> CLASS_PATH_ENTRIES = AttachmentKey.createList(ClassPathEntry.class);
    /**
     * The list of extensions given in the manifest and structure configurations.
     */
    public static final AttachmentKey<AttachmentList<ExtensionListEntry>> EXTENSION_LIST_ENTRIES = AttachmentKey.createList(ExtensionListEntry.class);

    /**
     * The server deployment repository
     */
    public static final AttachmentKey<ServerDeploymentRepository> SERVER_DEPLOYMENT_REPOSITORY = AttachmentKey.create(ServerDeploymentRepository.class);

    /**
     * An annotation index for a (@link ResourceRoot). This is attached to the {@link ResourceRoot}s of the deployment that contain
     * the annotations
     */
    public static final AttachmentKey<Index> ANNOTATION_INDEX = AttachmentKey.create(Index.class);

    /**
     * The composite annotation index for this deployment.
     */
    public static final AttachmentKey<CompositeIndex> COMPOSITE_ANNOTATION_INDEX = AttachmentKey.create(CompositeIndex.class);

    /**
     * Flag to indicate whether to compute the composite annotation index for this deployment.  Absence of this flag will
     * be cause the composite index to be attached.
     */
    public static final AttachmentKey<Boolean> COMPUTE_COMPOSITE_ANNOTATION_INDEX = AttachmentKey.create(Boolean.class);

    /**
     * An attachment that indicates if a {@link ResourceRoot} should be indexed by the {@link AnnotationIndexProcessor}. If this
     * is not present then the resource root is indexed by default.
     */
    public static final AttachmentKey<Boolean> INDEX_RESOURCE_ROOT = AttachmentKey.create(Boolean.class);

     /**
     * A list of paths within a root to ignore when indexing.
     */
    public static final AttachmentKey<AttachmentList<String>> INDEX_IGNORE_PATHS = AttachmentKey.createList(String.class);

    /**
     * Flag to determine whether to process the child annotation indexes as part of the parent deployment.
     * Ex.  An EAR deployment should not processes nested JAR index when checking for deployable annotations.
     * It should rely on the child actually being deployed.  WARs and RARs on the other hand should process all the
     * children as though the are all one index.
     */
    public static final AttachmentKey<Boolean> PROCESS_CHILD_ANNOTATION_INDEX = AttachmentKey.create(Boolean.class);

    /**
     * A marker attachment to identify a resource root that should be included as a module root.
     */
    public static final AttachmentKey<Boolean> MODULE_ROOT_MARKER = AttachmentKey.create(Boolean.class);

    /**
     * A marker attachment to identify a resource root that is also a sub-deployment.
     */
    public static final AttachmentKey<Boolean> SUB_DEPLOYMENT_MARKER = AttachmentKey.create(Boolean.class);

    /**
     * A Marker attachment to identify an EAR deployment.
     */
    public static final AttachmentKey<Boolean> EAR_DEPLOYMENT_MARKER = AttachmentKey.create(Boolean.class);

    /**
     * A Marker attachment to identify an WAR deployment.
     */
    public static final AttachmentKey<Boolean> WAR_DEPLOYMENT_MARKER = AttachmentKey.create(Boolean.class);


    //
    // VALIDATE
    //

    //
    // PARSE
    //

    /**
     *  List<PersistenceMetadata> that represents the JPA persistent units
     */
    // public static final AttachmentKey<PersistenceMetadataHolder> PERSISTENCEUNITS = AttachmentKey.create
    // (PersistenceMetadataHolder.class);
    public static final AttachmentKey<Object> PERSISTENCE_UNITS = AttachmentKey.create(Object.class);

    //
    // DEPENDENCIES
    //
    /**
     * The list of module dependencies.
     */
    public static final AttachmentKey<AttachmentList<ModuleDependency>> MODULE_DEPENDENCIES = AttachmentKey.createList(ModuleDependency.class);

    //
    // CONFIGURE
    //
    /**
     * The module idetifier.
     */
    public static final AttachmentKey<ModuleIdentifier> MODULE_IDENTIFIER = AttachmentKey.create(ModuleIdentifier.class);

    //
    // MODULARIZE
    //

    /**
     * The module of this deployment unit.
     */
    public static final AttachmentKey<Module> MODULE = AttachmentKey.create(Module.class);

    /**
     * The module loader for the deployment
     */
    public static final AttachmentKey<DeploymentModuleLoader> DEPLOYMENT_MODULE_LOADER = AttachmentKey.create(DeploymentModuleLoader.class);

    /**
     * An index of {@link java.util.ServiceLoader}-type services in this deployment unit
     */
    public static final AttachmentKey<ServicesAttachment> SERVICES = AttachmentKey.create(ServicesAttachment.class);

    //
    // POST_MODULE
    //

    //
    // INSTALL
    //

    /**
     * The reflection index for the deployment.
     */
    public static final AttachmentKey<DeploymentReflectionIndex> REFLECTION_INDEX = AttachmentKey.create(DeploymentReflectionIndex.class);

    //
    // CLEANUP
    //

    private Attachments() {
    }


}
