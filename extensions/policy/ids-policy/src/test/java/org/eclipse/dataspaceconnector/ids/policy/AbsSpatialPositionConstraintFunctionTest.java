package org.eclipse.dataspaceconnector.ids.policy;

import org.eclipse.dataspaceconnector.contract.policy.PolicyContextImpl;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.model.Operator.EQ;
import static org.eclipse.dataspaceconnector.policy.model.Operator.GT;
import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;
import static org.eclipse.dataspaceconnector.policy.model.Operator.NEQ;

class AbsSpatialPositionConstraintFunctionTest {

    private final AbsSpatialPositionConstraintFunction constraintFunction = new AbsSpatialPositionConstraintFunction();

    @Test
    void shouldVerifyEqConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());
        var permission = dummyPermission();

        boolean result = constraintFunction.evaluate(EQ, "eu", permission, new PolicyContextImpl(euAgent));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotVerifyEqConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());
        var permission = dummyPermission();

        boolean result = constraintFunction.evaluate(EQ, "us", permission, new PolicyContextImpl(euAgent));

        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyInConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(IN, List.of("eu"), dummyPermission(), new PolicyContextImpl(euAgent));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotVerifyInConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(IN, List.of("us"), dummyPermission(), new PolicyContextImpl(euAgent));

        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyNotEqConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(NEQ, "us", dummyPermission(), new PolicyContextImpl(euAgent));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotVerifyNotEqConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(NEQ, "eu", dummyPermission(), new PolicyContextImpl(euAgent));

        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyGreatherThanConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(GT, "eu", dummyPermission(), new PolicyContextImpl(euAgent));

        assertThat(result).isFalse();
    }

    private Permission dummyPermission() {
        return Permission.Builder.newInstance().build();
    }
}