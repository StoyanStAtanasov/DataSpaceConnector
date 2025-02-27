package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Map;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants.DID_URL_SETTING;

@Provides(IdentityService.class)
public class DistributedIdentityServiceExtension implements ServiceExtension {

    @Inject
    private DidResolverRegistry resolverRegistry;
    @Inject
    private CredentialsVerifier credentialsVerifier;

    @Override
    public String name() {
        return "Distributed Identity Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vcProvider = createSupplier(context);
        var identityService = new DistributedIdentityService(vcProvider, resolverRegistry, credentialsVerifier, context.getMonitor());
        context.registerService(IdentityService.class, identityService);
    }

    @Override
    public void start() {
        ServiceExtension.super.start();
    }

    Supplier<SignedJWT> createSupplier(ServiceExtensionContext context) {
        var didUrl = context.getSetting(DID_URL_SETTING, null);
        if (didUrl == null) {
            throw new EdcException(format("The DID Url setting '(%s)' was null!", DID_URL_SETTING));
        }

        return () -> {
            // we'll use the connector name to restore the Private Key
            var connectorName = context.getConnectorId();
            var resolver = context.getService(PrivateKeyResolver.class);
            var privateKeyString = resolver.resolvePrivateKey(connectorName, ECKey.class); //to get the private key

            // we cannot store the VerifiableCredential in the Vault, because it has an expiry date
            // the Issuer claim must contain the DID URL
            return VerifiableCredentialFactory.create(privateKeyString, Map.of(VerifiableCredentialFactory.OWNER_CLAIM, connectorName), didUrl);
        };
    }
}
