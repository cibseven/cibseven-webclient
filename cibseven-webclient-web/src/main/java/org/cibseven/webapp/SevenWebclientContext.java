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
package org.cibseven.webapp;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.cibseven.webapp.auth.BaseUserProvider;
import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.providers.BpmProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import org.cibseven.webapp.rest.CustomRestTemplate;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@ComponentScan({ "org.cibseven.webapp.providers", "org.cibseven.webapp.auth", "org.cibseven.webapp.rest", "org.cibseven.webapp.template", "org.cibseven.webapp.config" })
public class SevenWebclientContext implements WebMvcConfigurer, HandlerMethodArgumentResolver {

	BaseUserProvider provider;

	@Value("${cibseven.webclient.custom.spring.jackson.parser.max-size:20000000}")
	int jacksonParserMaxSize;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        StreamReadConstraints streamReadConstraints = StreamReadConstraints
                .builder()
                .maxStringLength(jacksonParserMaxSize)
                .build();
        objectMapper.getFactory().setStreamReadConstraints(streamReadConstraints);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(new ResourceHttpMessageConverter()); // needed for DocumentService.download
		converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8)); // needed for UiService
		converters.add(new ByteArrayHttpMessageConverter()); // needed for fetching data variables
		converters.add(new FormHttpMessageConverter());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(objectMapper());
		converters.add(converter);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("GET", "POST", "DELETE", "PUT");
	}

	@Override // https://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.setUseSuffixPatternMatch(false);
	}

	@Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false);
    }

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		WebContentInterceptor cacheConfig = new WebContentInterceptor();
		// Default cache control for most resources
		cacheConfig.setCacheControl(CacheControl.noCache());

		// Strict cache control: prevents any caching (including private caches)
		// Used for HTML entry points to ensure fresh content delivery
		CacheControl strictNoStoreControl = CacheControl.noStore();

		// Apply strict no-store policy to main application entry points
		cacheConfig.addCacheMapping(strictNoStoreControl, "/index.html");
		cacheConfig.addCacheMapping(strictNoStoreControl, "/");
		cacheConfig.addCacheMapping(strictNoStoreControl, "/embedded-forms.html");
		cacheConfig.addCacheMapping(strictNoStoreControl, "/sso-login.html");

		registry.addInterceptor(cacheConfig);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(this);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return User.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest rq,
			WebDataBinderFactory binderFactory) {
		return provider.authenticateUser(((ServletWebRequest) rq).getRequest());
	}

	@Bean
	public BpmProvider bpmProvider(@Value("${cibseven.webclient.bpm.provider:org.cibseven.webapp.providers.SevenProvider}") Class<BpmProvider> providerClass)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		return (BpmProvider) providerClass.getConstructor().newInstance();
	}

	@Bean
	public BaseUserProvider baseUserProvider(@Value("${cibseven.webclient.user.provider:org.cibseven.webapp.auth.SevenUserProvider}") Class<BaseUserProvider> providerClass)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		this.provider = (BaseUserProvider) providerClass.getConstructor().newInstance();
		return provider;
	}

	@Bean // http://blog.codeleak.pl/2015/09/placeholders-support-in-value.html
	public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	/**
	 * Creates a custom RestTemplate bean with configurable settings.
	 * This can be injected into services that need to make HTTP requests.
	 * 
	 * The bean is configured using properties from application.yaml under
	 * the cibseven.webclient.rest namespace.
	 * 
	 * This bean is conditional and will only be created if cibseven.webclient.rest.enabled=true
	 * or if the property is not specified (default behavior).
	 * 
	 * @return a configured CustomRestTemplate instance
	 */
	@Bean
	@ConditionalOnProperty(
		prefix = "cibseven.webclient.rest",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true
	)
	public CustomRestTemplate customRestTemplate() {
		// Create a new CustomRestTemplate instance
		// It will be configured via @PostConstruct using @Autowired dependencies
		return new CustomRestTemplate();
	}

}
