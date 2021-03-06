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

package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.host.controller.HostController;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} determining the status of a server.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerStatusHandler implements ModelQueryOperationHandler {

    public static final String ATTRIBUTE_NAME = "status";

    private final HostController hostController;
    public ServerStatusHandler(final HostController hostController) {
        this.hostController = hostController;
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final PathElement element = address.getLastElement();
        final String serverName = element.getValue();

        final ModelNode subModel = context.getSubModel();
        final boolean isStart;
        if(subModel.hasDefined(AUTO_START)) {
            isStart = subModel.get(AUTO_START).asBoolean();
        } else {
            isStart = true;
        }

        ServerStatus status = hostController.getServerStatus(serverName);

        if (status == ServerStatus.STOPPED) {
            status = isStart ? status : ServerStatus.DISABLED;
        }

        if(status != null) {
            resultHandler.handleResultFragment(Util.NO_LOCATION, new ModelNode().set(status.toString()));
            resultHandler.handleResultComplete();
        } else {
            resultHandler.handleFailed(new ModelNode().set("Failed to get server status"));
        }
        return new BasicOperationResult();
    }

}
