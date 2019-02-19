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
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;

import io.zeelos.leshan.bootstrap.server.kafka.BootstrapStoreImpl;
import io.zeelos.leshan.bootstrap.server.kafka.ConfigurationChecker.ConfigurationException;

/**
 * Servlet for REST API in charge of adding bootstrap information to the bootstrap server.
 */
public class BootstrapServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static class SignedByteUnsignedByteAdapter implements JsonSerializer<Byte>, JsonDeserializer<Byte> {

        @Override
        public Byte deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return json.getAsByte();
        }

        @Override
        public JsonElement serialize(Byte src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive((int) src & 0xff);
        }
    }

    private final BootstrapStoreImpl bsStore;

    private final Gson gson;

    public BootstrapServlet(BootstrapStoreImpl bsStore) {
        this.bsStore = bsStore;

        this.gson = new GsonBuilder().registerTypeHierarchyAdapter(Byte.class, new SignedByteUnsignedByteAdapter())
                .create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() != null) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "bad URL");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getOutputStream().write(gson.toJson(bsStore.getBootstrapConfigs()).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            // we need the endpoint in the URL
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "endpoint name should be specified in the URL");
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');

        // endPoint
        if (path.length != 1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "endpoint name should be specified in the URL, nothing more");
            return;
        }

        String endpoint = path[0];

        try {
            BootstrapConfig cfg = gson.fromJson(new InputStreamReader(req.getInputStream()), BootstrapConfig.class);

            if (cfg == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "no content");
            } else {
                bsStore.addConfig(endpoint, cfg);
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (JsonSyntaxException | ConfigurationException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            // we need the endpoint in the URL
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "endpoint name should be specified in the URL");
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');

        // endPoint
        if (path.length != 1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "endpoint name should be specified in the URL, nothing more");
            return;
        }

        String endpoint = path[0];

        if (bsStore.deleteConfig(endpoint)) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "no config for " + endpoint);
        }
    }

    private void sendError(HttpServletResponse resp, int statusCode, String errorMessage) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain; charset=UTF-8");
        if (errorMessage != null)
            resp.getOutputStream().write(errorMessage.getBytes(StandardCharsets.UTF_8));
    }
}