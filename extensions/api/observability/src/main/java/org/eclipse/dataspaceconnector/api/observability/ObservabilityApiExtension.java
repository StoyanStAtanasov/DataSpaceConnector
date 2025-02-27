package org.eclipse.dataspaceconnector.api.observability;

import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

public class ObservabilityApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private HealthCheckService healthCheckService;

    public ObservabilityApiExtension(WebService webServiceMock, HealthCheckService healthCheckService) {
        webService = webServiceMock;
        this.healthCheckService = healthCheckService;
    }

    public ObservabilityApiExtension() {
    }

    @Override
    public String name() {
        return "EDC Control API";
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {


        webService.registerController(new ObservabilityApiController(healthCheckService));

        // contribute to the liveness probe
        healthCheckService.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("Observability API").build());
        healthCheckService.addLivenessProvider(() -> HealthCheckResult.Builder.newInstance().component("Observability API").build());
    }

}
