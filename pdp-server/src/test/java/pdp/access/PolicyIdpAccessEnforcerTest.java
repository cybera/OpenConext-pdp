package pdp.access;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import pdp.domain.EntityMetaData;
import pdp.domain.PdpPolicy;
import pdp.policies.PolicyLoader;
import pdp.serviceregistry.TestingServiceRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class PolicyIdpAccessEnforcerTest {

  private TestingServiceRegistry serviceRegistry = new TestingServiceRegistry("service-registry/saml20-idp.test.json","service-registry/saml20-sp.test.json");
  private PolicyIdpAccessEnforcer subject = new PolicyIdpAccessEnforcer(serviceRegistry);
  private PdpPolicy pdpPolicy;

  private String uid = "uid";
  private String displayName = "John Doe";
  private String authenticatingAuthority = PolicyLoader.authenticatingAuthority;
  private String[] identityProviderIds = {authenticatingAuthority, "http://mock-idp2"};
  private String[] serviceProviderIds = {"http://mock-sp", "http://mock-sp2"};
  private String institutionId = "MOCK";
  private String notOwnedIdp = "http://not-owned-idp";
  private String notOwnedSp = "http://not-owned-sp";

  @Before
  public void before() {
    this.pdpPolicy = new PdpPolicy("N/A", "pdpPolicyName", true, uid, authenticatingAuthority, displayName, true);
    //individual tests can overwrite this behaviour
    setupSecurityContext(true, entityMetadata(identityProviderIds), entityMetadata(serviceProviderIds));
    serviceRegistry.allowAll(false);
  }

  private void setupSecurityContext(boolean policyIdpAccessEnforcement, Set<EntityMetaData> idpEntities, Set<EntityMetaData> spEntities) {
    SecurityContext context = new SecurityContextImpl();
    Authentication authentication = new PolicyIdpAccessAwareToken(
        new RunAsFederatedUser(uid, authenticatingAuthority, displayName, idpEntities, spEntities, EMPTY_LIST) {
          @Override
          public boolean isPolicyIdpAccessEnforcementRequired() {
            return policyIdpAccessEnforcement;
          }
        });
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }

  @Test
  public void testActionAllowedHappyFlowNoIdps() throws Exception {
    this.subject.actionAllowed(pdpPolicy, PolicyAccess.WRITE, serviceProviderIds[0], EMPTY_LIST);
  }

  @Test
  public void testActionAllowedHappyFlowOwnedIdps() throws Exception {
    this.subject.actionAllowed(pdpPolicy, PolicyAccess.WRITE, serviceProviderIds[0], Arrays.asList(identityProviderIds));
  }

  @Test(expected = PolicyIdpAccessMismatchServiceProviderException.class)
  public void testActionNotAllowedSpDoesNotMatch() throws Exception {
    setupSecurityContext(true, entityMetadata(identityProviderIds), entityMetadata("http://not-owned-sp"));
    this.subject.actionAllowed(pdpPolicy, PolicyAccess.WRITE, serviceProviderIds[0], EMPTY_LIST);
  }

  @Test(expected = PolicyIdpAccessMismatchIdentityProvidersException.class)
  public void testActionNotAllowedIdpsDoNotMatch() throws Exception {
    this.subject.actionAllowed(pdpPolicy, PolicyAccess.WRITE, serviceProviderIds[0], singletonList(notOwnedIdp));
  }

  @Test(expected = PolicyIdpAccessOriginatingIdentityProviderException.class)
  public void testActionNotAllowedWrongAuthenticatingAuthority() throws Exception {
    this.pdpPolicy.setAuthenticatingAuthority(notOwnedIdp);
    this.subject.actionAllowed(pdpPolicy, PolicyAccess.WRITE, serviceProviderIds[0], EMPTY_LIST);
  }

  @Test
  public void testActionNotAllowedDifferentAuthenticatingAuthorityButOwnerDifferentIdp() throws Exception {
    this.pdpPolicy.setAuthenticatingAuthority(notOwnedIdp);
    //we now do own notOwnedIdp
    setupSecurityContext(false, entityMetadata(identityProviderIds[0]), entityMetadata(notOwnedIdp));
    this.subject.actionAllowed(pdpPolicy, PolicyAccess.WRITE, serviceProviderIds[0], singletonList(identityProviderIds[0]));
  }

  @Test
  public void testActionNotAllowedButNoEnforcementForUser() throws Exception {
    setupSecurityContext(false, entityMetadata(identityProviderIds), entityMetadata(serviceProviderIds));
    this.subject.actionAllowed(pdpPolicy, PolicyAccess.WRITE, null, null);
  }

  @Test
  public void testActionAllowedViolations() throws Exception {
    this.subject.actionAllowed(null, PolicyAccess.VIOLATIONS, null, null);
  }

  @Test
  public void testActionAllowedIdpsAndSpAllowed() throws Exception {
    serviceRegistry.allowAll(true);
    this.subject.actionAllowed(pdpPolicy, PolicyAccess.READ, notOwnedSp, Collections.EMPTY_LIST);
  }

  @Test
  public void testAuthenticatingAuthority() throws Exception {
    assertEquals(authenticatingAuthority, subject.authenticatingAuthority());
  }

  @Test
  public void testUserIdentifier() throws Exception {
    assertEquals(uid, subject.username());
  }

  @Test
  public void testUserDisplayName() throws Exception {
    assertEquals(displayName, subject.userDisplayName());
  }

  private Set<EntityMetaData> entityMetadata(String... entityIds) {
    return asList(entityIds).stream().map(id -> new EntityMetaData(id, institutionId, null, null, null, null, true, true, new HashSet<String>())).collect(toSet());
  }

}