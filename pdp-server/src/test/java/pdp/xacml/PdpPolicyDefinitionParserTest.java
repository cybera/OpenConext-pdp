package pdp.xacml;

import org.apache.commons.io.IOUtils;
import org.apache.openaz.xacml.pdp.policy.Policy;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import pdp.domain.LoA;
import pdp.domain.PdpPolicy;
import pdp.domain.PdpPolicyDefinition;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PdpPolicyDefinitionParserTest {

    private PdpPolicyDefinitionParser subject = new PdpPolicyDefinitionParser();

    @Test
    public void testParseWithoutSp() throws Exception {
        //happy path is extensively tested in the StandAlonePdpEngineTest
        //we test here the various constraints
        List<PdpPolicy> policies = Arrays.asList("no.advice.deny.xml", "no.nl.advice.xml", "no.any.xml", "no.assignment.deny.xml", "no.sp.xml")
            .stream().map(name -> getPolicy(name)).collect(toList());
        policies.forEach(policy -> {
            try {
                subject.parse(policy);
                fail();
            } catch (PdpParseException | IllegalArgumentException e) {
            }
        });
    }

    @Test
    public void parseStepUpPolicy() throws IOException {
        String xml = IOUtils.toString(new ClassPathResource("xacml/test-policies/stepup.policy.template.xml").getInputStream());
        PdpPolicy policy = new PdpPolicy(xml, "policy-name", true, "me",
            "mock-idp", "me", true, "step");
        PdpPolicyDefinition policyDefinition = subject.parse(policy);

        List<LoA> loas = policyDefinition.getLoas();
        assertEquals(3 ,loas.size());
    }

    private PdpPolicy getPolicy(String name) {
        PdpPolicy policy = new PdpPolicy();
        //called form lambda
        try {
            String xml = IOUtils.toString(new ClassPathResource("xacml/invalid-policies/" + name).getInputStream());
            policy.setPolicyXml(xml);
            policy.setName(name);
            return policy;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}