package vn.nitrogen.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vn.nitrogen.identity.service.VerificationTokenGenerator.GeneratedToken;

@Tag("unit")
class VerificationTokenGeneratorTest {

    private final VerificationTokenGenerator generator = new VerificationTokenGenerator();

    @Test
    void generatesUrlSafe256BitTokenWithMatchingHash() {
        GeneratedToken token = generator.generate();

        assertThat(token.raw()).matches("[A-Za-z0-9_-]{43}");
        assertThat(token.hash()).matches("[0-9a-f]{64}").isEqualTo(generator.hash(token.raw()));
    }

    @Test
    void tokensAreUnique() {
        assertThat(generator.generate().raw()).isNotEqualTo(generator.generate().raw());
    }

    @Test
    void toStringDoesNotRevealRawToken() {
        GeneratedToken token = generator.generate();

        assertThat(token.toString()).doesNotContain(token.raw());
    }
}
