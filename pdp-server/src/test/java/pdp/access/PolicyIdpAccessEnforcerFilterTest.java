package pdp.access;


import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import pdp.serviceregistry.TestingServiceRegistry;

import javax.servlet.FilterChain;

import static java.util.Base64.getEncoder;
import static org.junit.Assert.assertEquals;

public class PolicyIdpAccessEnforcerFilterTest {

  private PolicyIdpAccessEnforcerFilter subject = new PolicyIdpAccessEnforcerFilter(
      new BasicAuthenticationManager("user", "password"),
      new TestingServiceRegistry());

  @Test
  public void testOnSuccessfulAuthentication() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = new MockFilterChain();

    request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + getEncoder().encodeToString(new String("user:password").getBytes()));

    request.addHeader("X-IDP-ENTITY-ID", "http://mock-idp");
    request.addHeader("X-UNSPECIFIED-NAME-ID", "uuid");
    request.addHeader("X-DISPLAY-NAME", "John Doe");

    subject.doFilter(request, response, filterChain);

    RunAsFederatedUser user = (RunAsFederatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    assertEquals("uuid", user.getIdentifier());
    assertEquals("John Doe", user.getDisplayName());
    assertEquals(1, user.getIdpEntities().size());
    assertEquals(1, user.getSpEntities().size());
    assertEquals("http://mock-idp", user.getAuthenticatingAuthority());
    assertEquals("[ROLE_PEP, ROLE_USER]", user.getAuthorities().toString());


  }
}