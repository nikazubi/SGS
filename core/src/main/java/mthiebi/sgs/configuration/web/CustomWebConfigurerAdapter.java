package mthiebi.sgs.configuration.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CustomWebConfigurerAdapter implements WebMvcConfigurer {

//	@Bean
//	public FilterRegistrationBean<CachingFilter> cachingRegistrationBean() {
//		FilterRegistrationBean<CachingFilter> registrationBean
//			= new FilterRegistrationBean<>();
//
//		registrationBean.setFilter(new CachingFilter());
//		return registrationBean;
//	}
}
