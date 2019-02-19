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

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import com.eclipsesource.json.JsonObject;

import org.eclipse.leshan.core.model.json.JsonSerDes;
import org.eclipse.leshan.util.Base64;
import org.eclipse.leshan.util.Hex;

///!\ This class is a COPY of org.eclipse.leshan.server.demo.servlet.json.PublicKeySerDes /!\
//TODO create a leshan-demo project ?
public class PublicKeySerDes extends JsonSerDes<PublicKey> {

    @Override
    public JsonObject jSerialize(PublicKey publicKey) {
        if (!(publicKey instanceof ECPublicKey))
            throw new IllegalStateException("Unsupported Public Key Format (only ECPublicKey supported).");

        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        JsonObject o = new JsonObject();

        // Get x coordinate
        byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
        if (x[0] == 0)
            x = Arrays.copyOfRange(x, 1, x.length);
        o.add("x", Hex.encodeHexString(x));

        // Get Y coordinate
        byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
        if (y[0] == 0)
            y = Arrays.copyOfRange(y, 1, y.length);
        o.add("y", Hex.encodeHexString(y));

        // Get Curves params
        o.add("params", ecPublicKey.getParams().toString());

        // Get raw public key in format SubjectPublicKeyInfo (DER encoding)
        o.add("b64Der", Base64.encodeBase64String(ecPublicKey.getEncoded()));

        return o;
    }

    @Override
    public PublicKey deserialize(JsonObject o) {
        throw new UnsupportedOperationException("not implemented yet.");
    }
}
