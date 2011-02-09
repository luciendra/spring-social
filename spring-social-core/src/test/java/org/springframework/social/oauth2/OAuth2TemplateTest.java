package org.springframework.social.oauth2;

import static org.junit.Assert.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.web.client.test.RequestMatchers.*;
import static org.springframework.web.client.test.ResponseCreators.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.test.MockRestServiceServer;

public class OAuth2TemplateTest {
	
	private static final String ACCESS_TOKEN_URL = "http://www.someprovider.com/oauth/accessToken";

	private OAuth2Template oAuth2Template;
	
	@Before
	public void setup() {
		String authorizeUrl = "http://www.someprovider.com/oauth/authorize?client_id={client_id}&redirect_uri={redirect_uri}&scope={scope}";
		oAuth2Template = new OAuth2Template("client_id", "client_secret", authorizeUrl, ACCESS_TOKEN_URL);
	}

	@Test
	public void buildAuthorizeUrl() {
		String expected = "http://www.someprovider.com/oauth/authorize?client_id=client_id&redirect_uri=http://www.someclient.com/connect/foo&scope=read,write";
		String actual = oAuth2Template.buildAuthorizeUrl("http://www.someclient.com/connect/foo", "read,write");
		assertEquals(expected, actual);
	}

	@Test
	public void buildAuthorizeUrl_noScopeInParameters() {
		String expected = "http://www.someprovider.com/oauth/authorize?client_id=client_id&redirect_uri=http://www.someclient.com/connect/foo&scope=";
		String actual = oAuth2Template.buildAuthorizeUrl("http://www.someclient.com/connect/foo", null);
		assertEquals(expected, actual);
	}

	@Test
	public void buildAuthorizeUrl_noScopeInUrlTemplate() {
		String authorizeUrl = "http://www.someprovider.com/oauth/authorize?client_id={client_id}&redirect_uri={redirect_uri}";
		oAuth2Template = new OAuth2Template("client_id", "client_secret", authorizeUrl, null);
		String expected = "http://www.someprovider.com/oauth/authorize?client_id=client_id&redirect_uri=http://www.someclient.com/connect/foo";
		String actual = oAuth2Template.buildAuthorizeUrl("http://www.someclient.com/connect/foo", "read");
		assertEquals(expected, actual);
	}

	@Test
	public void exchangeForAccess_facebookStyle() {
		// Facebook returns form-encoded results as text/plain. There is no refresh token.
		MediaType responseContentType = MediaType.TEXT_PLAIN;
		String responseFile = "accessToken.facebook";
		AccessGrant accessGrant = getAccessGrant(responseContentType, responseFile);
		assertEquals("162868103756545|bf4de6ed613f7901204c222g-738140579|YAufyoV9n7LmEAgzRKqnc300B0B", accessGrant.getAccessToken());
		assertNull(accessGrant.getRefreshToken());
	}

	@Test
	public void exchangeForAccess_jsonResponse() {
		// The OAuth 2 spec draft specifies JSON as the response content type. Gowalla and Github return the access token this way.
		MediaType responseContentType = MediaType.APPLICATION_JSON;
		String responseFile = "accessToken.json";
		AccessGrant accessGrant = getAccessGrant(responseContentType, responseFile);
		assertEquals("8d0a88a5c4f1ae4937ad864cafa8e857", accessGrant.getAccessToken());
		assertEquals("6b0411401bf8751e34f57feb29fb8e32", accessGrant.getRefreshToken());
	}

	private AccessGrant getAccessGrant(MediaType responseContentType, String responseFile) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(responseContentType);
		MockRestServiceServer mockServer = MockRestServiceServer.createServer((RestTemplate) oAuth2Template
				.getRestOperations());
		mockServer.expect(requestTo(ACCESS_TOKEN_URL))
				.andExpect(method(POST))
				.andExpect(body("client_id=client_id&client_secret=client_secret&code=code&"
								+ "redirect_uri=http%3A%2F%2Fwww.someclient.com%2Fcallback&grant_type=authorization_code"))
				.andRespond(withResponse(new ClassPathResource(responseFile, getClass()), responseHeaders));
		AccessGrant accessGrant = oAuth2Template.exchangeForAccess("code", "http://www.someclient.com/callback");
		return accessGrant;
	}

}