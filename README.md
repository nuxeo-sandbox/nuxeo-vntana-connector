# Description
A plugin that provides an integration between [Vntana](https://www.vntana.com/) and the Nuxeo Platform.

# How to build
```bash
git clone https://github.com/nuxeo-sandbox/nuxeo-vntana-connector
cd nuxeo-vntana-connector
mvn clean install -DskipTests
```
In order to run the integration tests, several parameters must be passed:
```bash
mvn clean install -DvntanaApiKey=<API_KEY> -DvntanaOrganizationUUID=<ORGANIZATION_UUID> -DvntanaClientUUID=<CLIENT_UUID> -DvntanaProductUUID=<PRODUCT_UUID>
```

# Configuration
Several configuration settings are available.

## nuxeo.conf

| Property name                   | description                                                        |
|---------------------------------|:-------------------------------------------------------------------|
| vntana.enabled                  | A boolean proprerty to enable/disable the plugin                   |
| vntana.api.key                  | The API key to use when calling Vntana's REST API                  |
| vntana.api.default.organization | The default vnatana organization UUID where models get published   |
| vntana.api.default.client       | The default vntana client (folder) UUID where models get published |

## Supported Documents
Whether a Document can be published or not to Vntana is controled by a Nuxeo Filter extension. The [default contribution](https://github.com/nuxeo-sandbox/nuxeo-vntana-connector/blob/master/nuxeo-vntana-connector-core/src/main/resources/OSGI-INF/service-service.xml) can be overridden in a configuration template, plugin or Nuxeo Studio.  

```xml
  <extension point="filters" target="org.nuxeo.ecm.platform.actions.ActionService">
    <filter id="vntanaDocumentFilter">
      <rule grant="true">
        <type>MyType</type>
      </rule>
    </filter>
  </extension>
```

# Plugin Features

## A Java client for the Vntana REST API
This projects contains a [Java client](https://github.com/nuxeo-sandbox/nuxeo-vntana-connector/tree/master/nuxeo-vntana-connector-client) for the [Vnatana REST API](https://www.vntana.com/resource/rest-api-documentation-v1-2/) which autogenerated from the [swagger definition](https://app.swaggerhub.com/apis-docs/vntana/VNTANA-ADMIN-API/1.2) using the [OpenAPI Generator Maven Plugin](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-maven-plugin) 

## Data Model
The plugin includes a `vnatana` schema that contains the model information in Vntana once published. 
The schema is typically added to a Document in Nuxeo using the `Vntana` Facet.

## Service
The [VntanaService](https://github.com/nuxeo-sandbox/nuxeo-vntana-connector/blob/master/nuxeo-vntana-connector-core/src/main/resources/OSGI-INF/service-service.xml) provides the glue between the Java Client and the Nuxeo Java APIs

## An Automation API to publish, update and un-publish models
Several Automation operations are made available by this plugin.

### Publish Model
```bash
curl 'http://localhost:8080/nuxeo/api/v1/automation/Vntana.PublishModel' \
  -H 'Content-Type: application/json' \
  -H 'properties: *' \
  --data-raw '{"params":{"save":true},"context":{},"input":"<DOC_UUID>"}' \
  --compressed
```

Parameters:

| Name             | Description                                    | Type    | Required | Default value |
|:-----------------|:-----------------------------------------------|:--------|:---------|:--------------|
| autoPublish      | Automatically make the model embeddable        | boolean | false    | false         |
| organizationUUID | The Vntana destination organization            | string  | false    |               |
| clientUUID       | The client destination within the organization | string  | false    |               |
| save             | Save the document                              | boolean | false    | false         |

### Fetch Model status from Vntana

```bash
curl 'http://localhost:8080/nuxeo/api/v1/automation/Vntana.UpdateModelRemoteProcessingStatus' \
  -H 'Content-Type: application/json' \
  -H 'properties: *' \
  --data-raw '{"params":{"save":true},"context":{},"input":"<DOC_UUID>"}'
```

Parameters:

| Name             | Description                                    | Type    | Required | Default value |
|:-----------------|:-----------------------------------------------|:--------|:---------|:--------------|
| save             | Save the document                              | boolean | false    | false         |

### Update Model
Update the Vntana related product binary file with the document current file.

```bash
curl 'http://localhost:8080/nuxeo/api/v1/automation/Vntana.UpdateModel' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'properties: *' \
  --data-raw '{"params":{"save":true},"context":{},"input":"<DOC_UUID>"}'
```

Parameters:

| Name             | Description                                    | Type    | Required | Default value |
|:-----------------|:-----------------------------------------------|:--------|:---------|:--------------|
| save             | Save the document                              | boolean | false    | false         |

### Unpublish Model
```bash
curl 'http://localhost:8080/nuxeo/api/v1/automation/Vntana.UnpublishModel' \
  -H 'Content-Type: application/json' \
  -H 'properties: *' \
  --data-raw '{"params":{"save":true},"context":{},"input":"<DOC_UUID>"}' \
  --compressed
```

Parameters:

| Name             | Description                                    | Type    | Required | Default value |
|:-----------------|:-----------------------------------------------|:--------|:---------|:--------------|
| save             | Save the document                              | boolean | false    | false         |

### Download Model Conversion
```bash
curl 'http://localhost:8080/nuxeo/api/v1/automation/Vntana.DownloadModel' \
  -H 'Content-Type: application/json' \
  -H 'properties: *' \
  --data-raw '{"params":{"format":"GLB"},"context":{},"input":"<DOC_UUID>"}' 
```

Parameters:

| Name   | Description                                          | Type   | Required | Default value |
|:-------|:-----------------------------------------------------|:-------|:---------|:--------------|
| format | The file format to download from Vntana: GLB or USDZ | string | true     | "GLB"         |

## Webui Resources

### Configuration
The plugin automatically contributes a webui configuration corresponding to the `vntana.enabled` nuxeo.conf property

```js
console.log(Nuxeo.UI.config.vntana)
{enabled: 'true'}
```

### API Document Enricher
An API document enricher is available to easily determine if a document can be published to Vntana

```bash
curl 'http://localhost:8080/nuxeo/api/v1/path/<PATH>' \
  -H 'Accept-Language: en-US,en;q=0.9,fr;q=0.8' \
  -H 'Content-Type: application/json' \
  -H 'enrichers-document: vntana' 
```
```json
{
   "entity-type":"document",
   "contextParameters": {
     "vntana": {
       "isSupported": true
     }
   }
}
```

### Actions
UI actions corresponding to the Automation operations are included into the plugin. The actions are automatically displayed depending on whether the publin is enabled and the document is supported.

![UI Publish Model Action Screenshot](https://github.com/nuxeo-sandbox/nuxeo-vntana-connector/raw/master/documentation_assets/publish_to_vntana_action.png)

![UI Published Model Actions Screenshot](https://github.com/nuxeo-sandbox/nuxeo-vntana-connector/raw/master/documentation_assets/published_asset_actions.png)

# Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

# Nuxeo Marketplace
This plugin is published on the [marketplace](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-vntana-connector)

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

# About Nuxeo

Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/), and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com).
