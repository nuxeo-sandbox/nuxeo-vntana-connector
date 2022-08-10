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

package org.nuxeo.labs.vntana.webhooks.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VntanaEvent implements Serializable {

    protected String event;

    protected Product product;

    public VntanaEvent(@JsonProperty("event") String event, @JsonProperty("product") Product product) {
        this.event = event;
        this.product = product;
    }

    public String getEvent() {
        return event;
    }

    public Product getProduct() {
        return product;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VntanaEvent)) return false;
        VntanaEvent that = (VntanaEvent) o;
        return Objects.equals(getEvent(), that.getEvent()) && Objects.equals(getProduct(), that.getProduct());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEvent(), getProduct());
    }

    @Override
    public String toString() {
        return "VntanaEvent{" +
                "event='" + event + '\'' +
                ", product=" + product +
                '}';
    }

    /**
     * POJO representing product
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {

        protected String uuid;

        protected String name;

        protected String status;

        public Product(@JsonProperty("uuid") String uuid, @JsonProperty("name") String name,
                       @JsonProperty("status") String status) {
            this.uuid = uuid;
            this.status = status;
            this.name = name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Product)) return false;
            Product product = (Product) o;
            return Objects.equals(getUuid(), product.getUuid()) && Objects.equals(getName(), product.getName()) && Objects.equals(getStatus(), product.getStatus());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getUuid(), getName(), getStatus());
        }

        @Override
        public String toString() {
            return "Product{" +
                    "uuid='" + uuid + '\'' +
                    ", name='" + name + '\'' +
                    ", status='" + status + '\'' +
                    '}';
        }
    }

}
