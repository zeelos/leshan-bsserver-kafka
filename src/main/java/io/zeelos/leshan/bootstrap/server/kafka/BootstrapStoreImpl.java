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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapStore;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zeelos.leshan.bootstrap.server.kafka.ConfigurationChecker.ConfigurationException;

/**
 * Simple bootstrap store implementation storing bootstrap information in memory
 */
public class BootstrapStoreImpl implements BootstrapStore {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapStoreImpl.class);

    // default location for persistence
    public static final String DEFAULT_FILE = "data/bootstrap.json";

    private final String filename;
    private final Gson gson;
    private final Type gsonType;

    public BootstrapStoreImpl() {
        this(DEFAULT_FILE);
    }

    /**
     * @param filename the file path to persist the registry
     */
    public BootstrapStoreImpl(String filename) {
        Validate.notEmpty(filename);
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        this.gson = builder.create();
        this.gsonType = new TypeToken<Map<String, BootstrapConfig>>() {
        }.getType();
        this.filename = filename;
        this.loadFromFile();
    }

    private Map<String, BootstrapConfig> bootstrapByEndpoint = new ConcurrentHashMap<>();

    @Override
    public BootstrapConfig getBootstrap(String endpoint, Identity deviceIdentity) {
        return bootstrapByEndpoint.get(endpoint);
    }

    public void addConfig(String endpoint, BootstrapConfig config) throws ConfigurationException {
        ConfigurationChecker.verify(config);
        bootstrapByEndpoint.put(endpoint, config);
        saveToFile();
    }

    public Map<String, BootstrapConfig> getBootstrapConfigs() {
        return Collections.unmodifiableMap(bootstrapByEndpoint);
    }

    public boolean deleteConfig(String enpoint) {
        BootstrapConfig res = bootstrapByEndpoint.remove(enpoint);
        saveToFile();
        return res != null;
    }

    // /////// File persistence

    private void loadFromFile() {
        try {
            File file = new File(filename);
            if (file.exists()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(file))) {
                    Map<String, BootstrapConfig> config = gson.fromJson(in, gsonType);
                    bootstrapByEndpoint.putAll(config);
                }
            }
        } catch (Exception e) {
            LOG.error("Could not load bootstrap infos from file", e);
        }
    }

    private synchronized void saveToFile() {
        try {
            // Create file if it does not exists.
            File file = new File(filename);
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                file.createNewFile();
            }

            // Write file
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filename))) {
                out.write(gson.toJson(getBootstrapConfigs(), gsonType));
            }
        } catch (Exception e) {
            LOG.error("Could not save bootstrap infos to file", e);
        }
    }
}
