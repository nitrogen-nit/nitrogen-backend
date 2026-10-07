package vn.nitrogen.platform.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxRetryPolicyTest {

    private final OutboxRetryPolicy policy = new OutboxRetryPolicy(
            4,
            Duration.ofSeconds(5),
            Duration.ofSeconds(20));

    @Test
    void doublesDelayUntilMaximum() {
        assertThat(policy.delayFor(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(policy.delayFor(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.delayFor(3)).isEqualTo(Duration.ofSeconds(20));
        assertThat(policy.delayFor(20)).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void reportsWhenRetryBudgetIsExhausted() {
        assertThat(policy.exhausted(3)).isFalse();
        assertThat(policy.exhausted(4)).isTrue();
    }

    @Test
    void rejectsInvalidConfigurationAndAttempt() {
        Duration oneSecond = Duration.ofSeconds(1);
        Duration twoSeconds = Duration.ofSeconds(2);

        assertThatThrownBy(() -> new OutboxRetryPolicy(0, oneSecond, twoSeconds))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OutboxRetryPolicy(1, twoSeconds, oneSecond))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.delayFor(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
