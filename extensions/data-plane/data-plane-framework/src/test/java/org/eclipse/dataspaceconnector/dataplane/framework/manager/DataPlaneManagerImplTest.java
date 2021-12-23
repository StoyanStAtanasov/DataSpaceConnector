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
package org.eclipse.dataspaceconnector.dataplane.framework.manager;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataPlaneManagerImplTest {

    /**
     * Verifies a request is enqueued, dequeued, and dispatched to the pipeline service.
     */
    @Test
    void verifyWorkDispatch() throws InterruptedException {
        var monitor = mock(Monitor.class);
        var pipelineService = mock(PipelineService.class);

        var latch = new CountDownLatch(1);

        when(pipelineService.transfer(isA(DataFlowRequest.class))).thenAnswer(i -> {
            latch.countDown();
            return completedFuture(Result.success("ok"));
        });

        var dataPlaneManager = DataPlaneManagerImpl.Builder.newInstance()
                .queueCapacity(100)
                .workers(1)
                .waitTimeout(10)
                .pipelineService(pipelineService)
                .monitor(monitor).build();

        var request = DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .transferType(TransferType.Builder.transferType().contentType("application/octet-stream").build())
                .sourceDataAddress(DataAddress.Builder.newInstance().build())
                .destinationDataAddress(DataAddress.Builder.newInstance().build())
                .build();

        dataPlaneManager.start();

        dataPlaneManager.initiateTransfer(request);

        latch.await(10000, TimeUnit.MILLISECONDS);

        dataPlaneManager.stop();
    }

    @BeforeEach
    void setUp() {

    }
}
