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

package io.zeelos.leshan.bootstrap.server.kafka.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.eclipsesource.json.JsonObject;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.server.californium.impl.LeshanBootstrapServer;
import org.eclipse.leshan.server.security.SecurityInfo;

import io.zeelos.leshan.bootstrap.server.kafka.json.PublicKeySerDes;
import io.zeelos.leshan.bootstrap.server.kafka.json.SecuritySerializer;
import io.zeelos.leshan.bootstrap.server.kafka.json.X509CertificateSerDes;

public class ServerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final X509CertificateSerDes certificateSerDes;
    private final PublicKeySerDes publicKeySerDes;

    private final LeshanBootstrapServer server;
    private final PublicKey publicKey;
    private final X509Certificate serverCertificate;

    public ServerServlet(LeshanBootstrapServer server, X509Certificate serverCertificate) {
        this.server = server;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SecurityInfo.class, new SecuritySerializer());
        certificateSerDes = new X509CertificateSerDes();
        publicKeySerDes = new PublicKeySerDes();

        this.publicKey = null;
        this.serverCertificate = serverCertificate;
    }

    public ServerServlet(LeshanBootstrapServer server, PublicKey serverPublicKey) {
        this.server = server;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SecurityInfo.class, new SecuritySerializer());
        certificateSerDes = new X509CertificateSerDes();
        publicKeySerDes = new PublicKeySerDes();
        this.publicKey = serverPublicKey;
        this.serverCertificate = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');

        if (path.length != 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if ("security".equals(path[0])) {
            JsonObject security = new JsonObject();
            if (publicKey != null) {
                security.add("pubkey", publicKeySerDes.jSerialize(publicKey));
            } else if (serverCertificate != null) {
                security.add("certificate", certificateSerDes.jSerialize(serverCertificate));
            }
            resp.setContentType("application/json");
            resp.getOutputStream().write(security.toString().getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if ("endpoint".equals(path[0])) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getOutputStream()
                    .write(String
                            .format("{ \"securedEndpointPort\":\"%s\", \"unsecuredEndpointPort\":\"%s\"}",
                                    server.getSecuredAddress().getPort(), server.getUnsecuredAddress().getPort())
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }

        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
}
