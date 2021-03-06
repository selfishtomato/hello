package hello.spring.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;

import hello.spring.security.basic.MyBasicAuthenticationProvider;
import hello.spring.security.digest.MyDigestAuthenticationProvider;
import hello.spring.security.digest.MyDigestUserDetailsService;
import hello.spring.security.token.custom.MyTokenAuthenticationFilter;
import hello.spring.security.token.custom.MyTokenAuthenticationProvider;
import hello.spring.security.token.jwt.MyJWTAuthenticationFilter;
import hello.spring.security.token.jwt.MyJWTAuthenticationProvider;

@EnableWebSecurity(debug = false)
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private static final Logger log = Logger.getLogger(SecurityConfig.class);

	@Autowired
	private MyBasicAuthenticationProvider basicAuthProvider;
	@Autowired
	private MyDigestAuthenticationProvider digestAuthProvider;
	@Autowired
	private MyTokenAuthenticationProvider tokenAuthProvider;
	@Autowired
	private MyJWTAuthenticationProvider jwtAuthProvider;

	@Bean
	public AuthenticationManager authenticationManager() {
		List<AuthenticationProvider> providers = new ArrayList<>();
		providers.add(basicAuthProvider);
		providers.add(digestAuthProvider);
		return new ProviderManager(providers);
	}


	@Configuration
	@Order(1)
	public class BasicAuthenticationConfig extends WebSecurityConfigurerAdapter {

		public static final String REALM_NAME = "Hello Basic Auth";


		public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
			BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
			entryPoint.setRealmName(REALM_NAME);
			return entryPoint;
		}

		@Bean
		public BasicAuthenticationFilter basicAuthenticationFilter() throws Exception {
			BasicAuthenticationFilter filter = new BasicAuthenticationFilter(
					authenticationManager(), basicAuthenticationEntryPoint());
			return filter;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			log.debug("configure basic auth");
			http
			.antMatcher("/basic/**")
			.addFilterAfter(basicAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling().authenticationEntryPoint(basicAuthenticationEntryPoint())
			;
		}

		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			auth.authenticationProvider(basicAuthProvider);
		}
	}

	@Configuration
	@Order(2)
	public class DigestAuthenticationConfig extends WebSecurityConfigurerAdapter {

		public static final String REALM_NAME = "Hello Digest Auth";

		@Bean
		public DigestAuthenticationFilter digestAuthenticationFilter() throws Exception {
			DigestAuthenticationFilter filter = new DigestAuthenticationFilter();
			filter.setAuthenticationEntryPoint(digestAuthenticationEntryPoint());
			filter.setUserDetailsService(userDetailsService());
			filter.setPasswordAlreadyEncoded(true);
			filter.setCreateAuthenticatedToken(false);
			return filter;
		}

		public DigestAuthenticationEntryPoint digestAuthenticationEntryPoint() {
			DigestAuthenticationEntryPoint entryPoint = new DigestAuthenticationEntryPoint();
			entryPoint.setRealmName(REALM_NAME);
			entryPoint.setNonceValiditySeconds(300);
			entryPoint.setKey("acegi");
			return entryPoint;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			log.debug("configure digest auth");
			http
			.exceptionHandling().authenticationEntryPoint(digestAuthenticationEntryPoint())
			.and()
			.addFilter(digestAuthenticationFilter())
			.authenticationProvider(digestAuthProvider)
			.antMatcher("/digest/**")
			.csrf().disable()
			.authorizeRequests()
			.anyRequest()
			.authenticated()
			;
		}

		@Bean
		@Override
		public MyDigestUserDetailsService userDetailsService() {
			return new MyDigestUserDetailsService();
		}
	}

	@Configuration
	@Order(3)
	public class TokenAuthenticationConfig extends WebSecurityConfigurerAdapter {

		@Bean
		public MyTokenAuthenticationFilter tokenAuthenticationFilter() throws Exception {
			MyTokenAuthenticationFilter filter = new MyTokenAuthenticationFilter();
			filter.setAuthenticationManager(authenticationManager());
			return filter;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			log.debug("configure token auth");
			http
			.antMatcher("/token/**")
			.addFilterAfter(tokenAuthenticationFilter(), DigestAuthenticationFilter.class)
			.authenticationProvider(tokenAuthProvider)
			.exceptionHandling().authenticationEntryPoint(tokenAuthenticationEntryPoint())
			;
		}

		private AuthenticationEntryPoint tokenAuthenticationEntryPoint() {
			return new AuthenticationEntryPoint() {
				@Override
				public void commence(HttpServletRequest request, HttpServletResponse response,
						AuthenticationException authException) throws IOException, ServletException {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
				}
			};
		}

		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			auth.authenticationProvider(tokenAuthProvider);
		}
	}

	@Configuration
	@Order(4)
	public class MyJWTAuthenticationConfig extends WebSecurityConfigurerAdapter {

		@Bean
		public MyJWTAuthenticationFilter jwtAuthenticationFilter() throws Exception {
			MyJWTAuthenticationFilter filter = new MyJWTAuthenticationFilter();
			filter.setAuthenticationManager(authenticationManager());
			return filter;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			log.debug("configure jwt auth");
			http
			.antMatcher("/jwt/**")
			.addFilterAfter(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
			.authenticationProvider(jwtAuthProvider)
			.authorizeRequests()
			.anyRequest()
			.authenticated()
			;
		}

		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			auth.authenticationProvider(jwtAuthProvider);
		}
	}

}
