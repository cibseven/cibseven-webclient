/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.rest;

import java.util.ArrayList;
import java.util.List;

import org.cibseven.webapp.config.SevenWebclientProperties;
import org.cibseven.webapp.config.WebclientJsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

/**
 * Configuration class for tests that extends RestTemplateConfiguration.
 * This ensures that a CustomRestTemplate bean is available in the test module.
 */
@Configuration
public class TestRestTemplateConfiguration extends RestTemplateConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TestRestTemplateConfiguration.class);

    /**
     * Creates a CustomRestTemplate bean if one doesn't already exist.
     * This bean is conditional and will only be created if cibseven.webclient.rest.enabled=true
     * or if the property is not specified (default behavior).
     * 
     * @return a configured CustomRestTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(CustomRestTemplate.class)
    @ConditionalOnProperty(
        prefix = "cibseven.webclient.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public CustomRestTemplate customRestTemplate() {
        log.info("Creating CustomRestTemplate bean for tests");
        // It will be configured via @PostConstruct using @Autowired dependencies
        CustomRestTemplate template = webclientCustomRestTemplate();
        log.info("CustomRestTemplate created");
        return template;
    }

    /**
     * Same converter setup as SevenWebclientContext#customRestTemplate: the JSON converter Spring 7
     * registers by default is swapped for one on the webclient's own mapper.
     */
    public static CustomRestTemplate webclientCustomRestTemplate() {
        CustomRestTemplate template = new CustomRestTemplate();
        JacksonJsonHttpMessageConverter json = new JacksonJsonHttpMessageConverter(
                WebclientJsonMapper.create(new SevenWebclientProperties()));
        List<HttpMessageConverter<?>> converters = new ArrayList<>(template.getMessageConverters());
        converters.replaceAll(c -> c instanceof JacksonJsonHttpMessageConverter ? json : c);
        template.setMessageConverters(converters);
        return template;
    }
}