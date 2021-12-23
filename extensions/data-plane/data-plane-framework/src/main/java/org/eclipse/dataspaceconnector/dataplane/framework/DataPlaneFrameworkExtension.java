/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataplane.framework;

import org.eclipse.dataspaceconnector.dataplane.framework.manager.DataPlaneManagerImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 * Provides core services for the Data Plane Framework.
 */
public class DataPlaneFrameworkExtension implements ServiceExtension {

    @EdcSetting
    private static final String QUEUE_CAPACITY = "edc.dataplane.queue.capacity";
    private static final String DEFAULT_QUEUE_CAPACITY = "10000";

    @EdcSetting
    private static final String WORKERS = "edc.dataplane.workers";
    private static final String DEFAULT_WORKERS = "10";

    @EdcSetting
    private static final String WAIT_TIMEOUT = "edc.dataplane.wait";
    private static final String DEFAULT_WAIT_TIMEOUT = "1000";

    @Override
    public String name() {
        return "Data Plane Framework";
    }

    @Override
    public Set<String> provides() {
        return Set.of("edc:dataplane:framework");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var pipelineService = new PipelineServiceImpl();
        context.registerService(PipelineService.class, pipelineService);

        var monitor = context.getMonitor();
        var queueCapacity = Integer.parseInt(context.getSetting(QUEUE_CAPACITY, DEFAULT_QUEUE_CAPACITY));
        var workers = Integer.parseInt(context.getSetting(WORKERS, DEFAULT_WORKERS));
        var waitTimeout = Long.parseLong(context.getSetting(WAIT_TIMEOUT, DEFAULT_WAIT_TIMEOUT));

        var dataPlaneManager = DataPlaneManagerImpl.Builder.newInstance()
                .queueCapacity(queueCapacity)
                .workers(workers)
                .waitTimeout(waitTimeout)
                .pipelineService(pipelineService)
                .monitor(monitor).build();

        context.registerService(DataPlaneManager.class, dataPlaneManager);
    }
}
