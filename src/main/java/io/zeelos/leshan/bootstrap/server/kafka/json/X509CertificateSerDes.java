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

package io.zeelos.leshan.bootstrap.server.kafka.json;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import org.eclipse.leshan.core.model.json.JsonSerDes;
import org.eclipse.leshan.util.Base64;

///!\ This class is a COPY of org.eclipse.leshan.server.demo.servlet.json.X509CertificateSerDes /!\
// TODO create a leshan-demo project ?
public class X509CertificateSerDes extends JsonSerDes<X509Certificate> {

    private PublicKeySerDes publicKeySerDes = new PublicKeySerDes();

    @Override
    public JsonObject jSerialize(X509Certificate certificate) {
        final JsonObject o = Json.object();
        // add pubkey info
        o.add("pubkey", publicKeySerDes.jSerialize(certificate.getPublicKey()));

        // Get certificate (DER encoding)
        try {
            o.add("b64Der", Base64.encodeBase64String(certificate.getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }

        return o;
    }

    @Override
    public X509Certificate deserialize(JsonObject o) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
