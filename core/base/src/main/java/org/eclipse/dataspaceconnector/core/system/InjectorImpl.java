package org.eclipse.dataspaceconnector.core.system;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

final class InjectorImpl implements Injector {

    @Override
    public <T> T inject(InjectionContainer<T> container, ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        container.getInjectionPoints().forEach(ip -> {
            try {
                var service = context.getService(ip.getType(), !ip.isRequired());
                if (service != null) { //can only be if not required
                    ip.setTargetValue(service);
                }
            } catch (EdcException ex) { //thrown e.g. if the service is not present and is not optional
                monitor.warning("Error during injection", ex);
                throw new EdcInjectionException(ex);
            } catch (IllegalAccessException e) { //e.g. when the field is marked "final"
                monitor.warning("Could not set injection target", e);
                throw new EdcInjectionException(e);
            }
        });

        return container.getInjectionTarget();
    }
}
