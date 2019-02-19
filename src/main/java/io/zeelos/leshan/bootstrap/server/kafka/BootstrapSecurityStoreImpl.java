/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.zeelos.leshan.bootstrap.server.kafka;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig.ServerSecurity;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A DTLS security store using the provisioned bootstrap information for finding the DTLS/PSK credentials.
 */
public class BootstrapSecurityStoreImpl implements BootstrapSecurityStore {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapSecurityStoreImpl.class);

    private final BootstrapStoreImpl bsStore;

    public BootstrapSecurityStoreImpl(BootstrapStoreImpl bsStore) {
        this.bsStore = bsStore;
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        byte[] identityBytes = identity.getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<String, BootstrapConfig> e : bsStore.getBootstrapConfigs().entrySet()) {
            BootstrapConfig bsConfig = e.getValue();
            if (bsConfig.security != null) {
                for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> ec : bsConfig.security.entrySet()) {
                    ServerSecurity serverSecurity = ec.getValue();
                    if (serverSecurity.bootstrapServer && serverSecurity.securityMode == SecurityMode.PSK
                            && Arrays.equals(serverSecurity.publicKeyOrId, identityBytes)) {
                        return SecurityInfo.newPreSharedKeyInfo(e.getKey(), identity, serverSecurity.secretKey);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<SecurityInfo> getAllByEndpoint(String endpoint) {

        BootstrapConfig bsConfig = bsStore.getBootstrap(endpoint, null);

        if (bsConfig == null || bsConfig.security == null)
            return null;

        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> bsEntry : bsConfig.security.entrySet()) {
            ServerSecurity value = bsEntry.getValue();

            // Extract PSK security info
            if (value.bootstrapServer && value.securityMode == SecurityMode.PSK) {
                SecurityInfo securityInfo = SecurityInfo.newPreSharedKeyInfo(endpoint,
                        new String(value.publicKeyOrId, StandardCharsets.UTF_8), value.secretKey);
                return Arrays.asList(securityInfo);
            }
            // Extract RPK security info
            else if (value.bootstrapServer && value.securityMode == SecurityMode.RPK) {
                try {
                    SecurityInfo securityInfo = SecurityInfo.newRawPublicKeyInfo(endpoint,
                            SecurityUtil.publicKey.decode(value.publicKeyOrId));
                    return Arrays.asList(securityInfo);
                } catch (IOException | GeneralSecurityException e) {
                    LOG.error("Unable to decode Client public key for {}", endpoint, e);
                    return null;
                }
            }
            // Extract X509 security info
            else if (value.bootstrapServer && value.securityMode == SecurityMode.X509) {
                SecurityInfo securityInfo = SecurityInfo.newX509CertInfo(endpoint);
                return Arrays.asList(securityInfo);
            }
        }
        return null;

    }
}
