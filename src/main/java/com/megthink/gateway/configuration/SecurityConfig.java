package com.megthink.gateway.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Bean
	public UserDetailsService userDetailsService() {
		return new UserInfoUserDetailsService();
	}

	@SuppressWarnings("deprecation")
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf().disable().authorizeRequests().requestMatchers("/", "/login","/api/suspension-dno-request").permitAll().anyRequest()
				.authenticated().and().formLogin().loginPage("/login").loginProcessingUrl("/login")
				.defaultSuccessUrl("/dashboard.html", true).failureUrl("/login?error=true").and().logout()
				.logoutUrl("/logout").deleteCookies("JSESSIONID");
		return http.build();
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService());
		authenticationProvider.setPasswordEncoder(bCryptPasswordEncoder);
		return authenticationProvider;
	}

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.debug(false).ignoring().requestMatchers("/resources/**", "/static/**", "/assets/**",
				"/css/**", "/js/**", "/images/**", "/favicon.ico");
	}
}