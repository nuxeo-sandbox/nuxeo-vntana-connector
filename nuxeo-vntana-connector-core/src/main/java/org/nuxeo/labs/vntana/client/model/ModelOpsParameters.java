/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Michael Vachette
 */

package org.nuxeo.labs.vntana.client.model;

import java.util.HashMap;
import java.util.Map;

public class ModelOpsParameters {

    public static final String OPTIMIZATION = "OPTIMIZATION";
    public static final String DRACO_COMPRESSION = "DRACO_COMPRESSION";
    public static final String TEXTURE_COMPRESSION = "TEXTURE_COMPRESSION";
    Map<String, Map<String,String>> parameters = new HashMap<>();

    public ModelOpsParameters() {}

    protected Map<String,String> getOptimization() {
        if (!parameters.containsKey(OPTIMIZATION)) {
            parameters.put(OPTIMIZATION, new HashMap<>());
        }
        return parameters.get(OPTIMIZATION);
    }

    protected Map<String,String> getDracoCompression() {
        if (!parameters.containsKey(DRACO_COMPRESSION)) {
            parameters.put(DRACO_COMPRESSION, new HashMap<>());
        }
        return parameters.get(DRACO_COMPRESSION);
    }

    protected Map<String,String> getTextureCompression() {
        if (!parameters.containsKey(TEXTURE_COMPRESSION)) {
            parameters.put(TEXTURE_COMPRESSION, new HashMap<>());
        }
        return parameters.get(TEXTURE_COMPRESSION);
    }

    public ModelOpsParameters setDracoCompression(boolean enabled) {
        getDracoCompression().put("enabled", Boolean.toString(enabled));
        return this;
    }

    public ModelOpsParameters setOptimizationDesiredOuput(String value) {
        getOptimization().put("desiredOutput", value);
        return this;
    }

    public ModelOpsParameters setOptimizationPolyCount(long count) {
        getOptimization().put("poly", Long.toString(count));
        return this;
    }

    public ModelOpsParameters setOptimizationObstructedGeometry(boolean enabled) {
        getOptimization().put("obstructedGeometry", Boolean.toString(enabled));
        return this;
    }

    public ModelOpsParameters setTextureLosslessCompression(boolean lossless) {
        getTextureCompression().put("lossless", Boolean.toString(lossless));
        return this;
    }

    public ModelOpsParameters setTextureAgression(int value) {
        getTextureCompression().put("aggression", Integer.toString(value));
        return this;
    }

    public ModelOpsParameters setTextureMaxDimension(int value) {
        getTextureCompression().put("maxDimension", Integer.toString(value));
        return this;
    }

    public Map<String, Map<String,String>> toMap() {
        return parameters;
    }
}
