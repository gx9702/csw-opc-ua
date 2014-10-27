/*
 * Copyright 2014 Inductive Automation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csw.opc.opcuasdktest.server;

import com.inductiveautomation.opcua.sdk.server.api.OpcUaServerConfig;
import com.inductiveautomation.opcua.stack.core.security.SecurityPolicy;
import com.inductiveautomation.opcua.stack.core.types.builtin.LocalizedText;
import com.inductiveautomation.opcua.stack.core.types.structured.SignedSoftwareCertificate;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.EnumSet;

public class OpcUaDemoServerConfig implements OpcUaServerConfig {

    private static final String SERVER_ALIAS = "ctt-server-certificate2";
    private static final char[] PASSWORD = "test".toCharArray();

    private volatile Certificate certificate;
    private volatile KeyPair keyPair;

    public OpcUaDemoServerConfig() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            keyStore.load(getClass().getClassLoader().getResourceAsStream("ctt-server-keystore.pfx"), PASSWORD);

            Key serverPrivateKey = keyStore.getKey(SERVER_ALIAS, PASSWORD);

            if (serverPrivateKey instanceof PrivateKey) {
                certificate = keyStore.getCertificate(SERVER_ALIAS);
                PublicKey serverPublicKey = certificate.getPublicKey();
                keyPair = new KeyPair(serverPublicKey, (PrivateKey) serverPrivateKey);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error loading server certificate: {}.", e.getMessage(), e);

            certificate = null;
            keyPair = null;
        }
    }

    @Override
    public int getBindPort() { return 52520; }

    @Override
    public LocalizedText getApplicationName() {
        return LocalizedText.english("OPC-UA Demo Server");
    }

    @Override
    public String getApplicationUri() {
        return "urn:tmt:opc-demo-server";
    }

    @Override
    public String getServerName() {
        return "OPCUA/SampleConsoleServer";
    }

    @Override
    public String getProductUri() {
        return "http://www.inductiveautomation.com/opc-ua/stack";
    }

    @Override
    public KeyPair getKeyPair() {
        return keyPair;
    }

    @Override
    public Certificate getCertificate() {
        return certificate;
    }

    @Override
    public EnumSet<SecurityPolicy> getSecurityPolicies() {
        return EnumSet.of(SecurityPolicy.None, SecurityPolicy.Basic128Rsa15);
    }
}
