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

import java.lang.reflect.Type;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.util.Base64;
import org.eclipse.leshan.util.Hex;

///!\ This class is a COPY of org.eclipse.leshan.server.demo.servlet.json.SecuritySerializer /!\
public class SecuritySerializer implements JsonSerializer<SecurityInfo> {

    @Override
    public JsonElement serialize(SecurityInfo src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();

        element.addProperty("endpoint", src.getEndpoint());

        if (src.getIdentity() != null) {
            JsonObject psk = new JsonObject();
            psk.addProperty("identity", src.getIdentity());
            psk.addProperty("key", Hex.encodeHexString(src.getPreSharedKey()));
            element.add("psk", psk);
        }

        if (src.getRawPublicKey() != null) {
            JsonObject rpk = new JsonObject();
            PublicKey rawPublicKey = src.getRawPublicKey();
            if (rawPublicKey instanceof ECPublicKey) {
                ECPublicKey ecPublicKey = (ECPublicKey) rawPublicKey;
                // Get x coordinate
                byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
                if (x[0] == 0)
                    x = Arrays.copyOfRange(x, 1, x.length);
                rpk.addProperty("x", Hex.encodeHexString(x));

                // Get Y coordinate
                byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
                if (y[0] == 0)
                    y = Arrays.copyOfRange(y, 1, y.length);
                rpk.addProperty("y", Hex.encodeHexString(y));

                // Get Curves params
                rpk.addProperty("params", ecPublicKey.getParams().toString());

                // Get raw public key in format PKCS8 (DER encoding)
                rpk.addProperty("pkcs8", Base64.encodeBase64String(ecPublicKey.getEncoded()));
            } else {
                throw new JsonParseException("Unsupported Public Key Format (only ECPublicKey supported).");
            }
            element.add("rpk", rpk);
        }

        if (src.useX509Cert()) {
            element.addProperty("x509", true);
        }

        return element;
    }
}
