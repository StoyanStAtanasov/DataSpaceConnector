package org.eclipse.dataspaceconnector.core.protocol.web.rest;

import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorsFilterConfigurationTest {

    @Test
    void ensureCorrectDefaults() {
        ServiceExtensionContext ctx = mock(ServiceExtensionContext.class);
        var defaultValueCapture = ArgumentCaptor.forClass(String.class);
        //always return the default value
        when(ctx.getSetting(anyString(), defaultValueCapture.capture())).thenAnswer(i -> defaultValueCapture.getValue());

        var config = CorsFilterConfiguration.from(ctx);

        assertThat(config.getAllowedMethods()).isEqualTo("GET, POST, DELETE, PUT, OPTIONS");
        assertThat(config.getAllowedHeaders()).isEqualTo("origin, content-type, accept, authorization");
        assertThat(config.getAllowedOrigins()).isEqualTo("*");
        assertThat(config.isCorsEnabled()).isFalse();
        verify(ctx, atLeastOnce()).getSetting(anyString(), defaultValueCapture.capture());
    }

    @Test
    void ensureCorrectSettings() {
        ServiceExtensionContext ctx = mock(ServiceExtensionContext.class);
        when(ctx.getSetting(eq(CorsFilterConfiguration.CORS_CONFIG_ENABLED_SETTING), anyString())).thenReturn("true");
        when(ctx.getSetting(eq(CorsFilterConfiguration.CORS_CONFIG_HEADERS_SETTING), anyString())).thenReturn("origin, authorization");
        when(ctx.getSetting(eq(CorsFilterConfiguration.CORS_CONFIG_ORIGINS_SETTING), anyString())).thenReturn("localhost");
        when(ctx.getSetting(eq(CorsFilterConfiguration.CORS_CONFIG_METHODS_SETTING), anyString())).thenReturn("GET, POST");

        var config = CorsFilterConfiguration.from(ctx);

        assertThat(config.getAllowedMethods()).isEqualTo("GET, POST");
        assertThat(config.getAllowedHeaders()).isEqualTo("origin, authorization");
        assertThat(config.getAllowedOrigins()).isEqualTo("localhost");
        assertThat(config.isCorsEnabled()).isTrue();
        verify(ctx, atLeastOnce()).getSetting(anyString(), anyString());
    }
}