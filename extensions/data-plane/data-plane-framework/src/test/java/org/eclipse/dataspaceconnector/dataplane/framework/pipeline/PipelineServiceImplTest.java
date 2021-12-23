package org.eclipse.dataspaceconnector.dataplane.framework.pipeline;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineServiceImplTest {
    private PipelineService service;

    @Test
    void verifyTransfer() {
        var sourceFactory = mock(DataSourceFactory.class);
        var sinkFactory = mock(DataSinkFactory.class);

        var source = mock(DataSource.class);
        var sink = mock(DataSink.class);

        when(sourceFactory.canHandle(isA(DataFlowRequest.class))).thenReturn(true);
        when(sourceFactory.createSource(isA(DataFlowRequest.class))).thenReturn(source);
        when(sinkFactory.canHandle(isA(DataFlowRequest.class))).thenReturn(true);
        when(sinkFactory.createSink(isA(DataFlowRequest.class))).thenReturn(sink);
        when(sink.transfer(eq(source))).thenReturn(completedFuture(Result.success()));

        var request = DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .transferType(TransferType.Builder.transferType().contentType("application/octet-stream").build())
                .sourceDataAddress(DataAddress.Builder.newInstance().build())
                .destinationDataAddress(DataAddress.Builder.newInstance().build())
                .build();

        service.registerFactory(sourceFactory);
        service.registerFactory(sinkFactory);

        service.transfer(request);

        verify(sink).transfer(eq(source));
    }

    @BeforeEach
    void setUp() {
        service = new PipelineServiceImpl();
    }
}
