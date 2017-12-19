/*
 * Asset Share Commons
 *
 * Copyright (C) 2017 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.adobe.aem.commons.assetshare.content.impl;

import com.adobe.aem.commons.assetshare.content.MetadataProperties;
import com.adobe.granite.confmgr.ConfMgr;
import com.day.cq.dam.commons.util.SchemaFormHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.*;

@Component(service = MetadataProperties.class)
public class MetadataSchemaPropertiesImpl implements MetadataProperties {

    @Reference
    private ConfigurationResourceResolver configResolver;

    @Reference
    private ConfMgr confMgr;

    @Override
    public Map<String, List<String>> getMetadataProperties(final SlingHttpServletRequest request) {
        return getMetadataProperties(request, Collections.EMPTY_LIST);
    }

    @Override
    public Map<String, List<String>> getMetadataProperties(final SlingHttpServletRequest request, final List<String> metadataFieldTypes) {
        Map<String, List<String>> collectedMetadata = new HashMap<>();

        ResourceResolver resourceResolver = request.getResourceResolver();

        String suffix = getMetadataSchemaSuffix( request.getRequestPathInfo().getSuffixResource() );

        final Iterator<Resource> resourceIterator = SchemaFormHelper.getSchemaFormsIterator(resourceResolver, suffix,
                0, 0);

        while (resourceIterator.hasNext()){
            final Resource resource = resourceIterator.next();

            if (resource.getValueMap().get("allowCustomization", true)) {
                final MetadataSchemaResourceVisitor visitor = new MetadataSchemaResourceVisitor(collectedMetadata, metadataFieldTypes);
                visitor.accept(resource);
                collectedMetadata = visitor.getMetadata();
            }
        }

        return collectedMetadata;
    }

    private String getMetadataSchemaSuffix( Resource resource ){

        Resource confResource = configResolver.getResource(resource, "settings", "dam/adminui-extension/metadataschema" );
        if( confResource != null ){
            return confResource.getPath();
        } else {
            return "/conf/global/settings/dam/adminui-extension/metadataschema";
        }

    }

    private class MetadataSchemaResourceVisitor extends AbstractResourceVisitor {
        private final Map<String, List<String>> metadata;
        private final List<String> metadataFieldTypes;

        public MetadataSchemaResourceVisitor(Map<String, List<String>> metadata, List<String> metadataFieldTypes) {
            this.metadata = metadata;
            this.metadataFieldTypes = metadataFieldTypes;
        }

        public final  Map<String, List<String>> getMetadata() {
            return metadata;
        }

        @Override
        protected void visit(final Resource resource) {
            final ValueMap properties = resource.getValueMap();

            final String type = properties.get("type", String.class);
            final String metaType = properties.get("metaType", String.class);

            if (metadataFieldTypes.size() > 0 &&
                    !metadataFieldTypes.contains(type) &&
                    !metadataFieldTypes.contains(metaType)) {
                return;
            }

            final String fieldLabel = properties.get("fieldLabel", String.class);
            final String propertyName = properties.get("name", String.class);

            if (StringUtils.isNotBlank(fieldLabel) && StringUtils.isNotBlank(propertyName)) {
                if (metadata.containsKey(propertyName)) {
                    final List<String> tmp = metadata.get(propertyName);
                    if (!tmp.contains(fieldLabel)) {
                        tmp.add(fieldLabel);
                    }
                    metadata.put(propertyName, tmp);
                } else {
                    final ArrayList<String> tmp = new ArrayList<>();
                    tmp.add(fieldLabel);
                    metadata.put(propertyName, tmp);
                }
            }
        }
    }
}
