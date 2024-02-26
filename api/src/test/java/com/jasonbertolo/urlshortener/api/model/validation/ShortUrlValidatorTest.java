package com.jasonbertolo.urlshortener.api.model.validation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jasonbertolo.urlshortener.api.model.ShortUrl;
import com.jasonbertolo.urlshortener.api.model.dto.ShortUrlCreateDto;
import com.jasonbertolo.urlshortener.api.model.dto.ShortUrlResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.validation.SimpleErrors;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ShortUrlValidatorTest {

    // System under test
    private static final ShortUrlValidator SHORT_URL_VALIDATOR = new ShortUrlValidator();

    @BeforeEach
    void beforeEach() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);
    }

    @Test
    @DisplayName("ShortUrlValidator class support")
    void shortUrlValidatorSupportsClass() {
        ShortUrlValidator shortUrlDtoValidator = new ShortUrlValidator();
        assertThat(shortUrlDtoValidator.supports(String.class)).isFalse();
        assertThat(shortUrlDtoValidator.supports(Integer.class)).isFalse();
        assertThat(shortUrlDtoValidator.supports(ShortUrl.class)).isFalse();
        assertThat(shortUrlDtoValidator.supports(ShortUrlResponseDto.class)).isFalse();
        assertThat(shortUrlDtoValidator.supports(ShortUrlCreateDto.class)).isTrue();
    }

    @Test
    @DisplayName("Validate ShortUrlCreateDto")
    void validateShortUrlCreateDto() {
        ShortUrlCreateDto dto = new ShortUrlCreateDto();
        dto.setUrl("https://www.example.com?param=value");
        dto.setDescription("Example URL");

        SimpleErrors errors = new SimpleErrors(dto);
        SHORT_URL_VALIDATOR.validate(dto, errors);
        assertThat(errors.hasErrors()).isFalse();
    }

    static Stream<ShortUrlCreateDto> testShortUrlCreateDtoObjs() {
        ShortUrlCreateDto dto1 = new ShortUrlCreateDto();
        dto1.setUrl(null);

        ShortUrlCreateDto dto2 = new ShortUrlCreateDto();
        dto2.setUrl("");

        ShortUrlCreateDto dto3 = new ShortUrlCreateDto();
        dto3.setUrl("invalid_url");
        dto3.setDescription("Test");

        ShortUrlCreateDto dto4 = new ShortUrlCreateDto();
        dto4.setUrl("invalid_url".repeat(200));

        ShortUrlCreateDto dto5 = new ShortUrlCreateDto();
        dto5.setUrl("^.^!@#$%^&*()[]{}?><;':%20ççççççççç≈ΩΩ≈çßç");

        ShortUrlCreateDto dto6 = new ShortUrlCreateDto();
        dto5.setUrl("学中文");

        ShortUrlCreateDto dto7 = new ShortUrlCreateDto();
        dto7.setUrl("https://example.com");
        dto7.setDescription("Some long description that is over 100 characters a long description long description " +
                "long description long description");

        return Stream.of(dto1, dto2, dto3, dto4, dto5, dto6, dto7);
    }
    @ParameterizedTest(name="dto{index}")
    @MethodSource("testShortUrlCreateDtoObjs")
    @DisplayName("Validate ShortUrlCreateDto, has errors")
    void validateShortUrlCreateDtoHasErrors(ShortUrlCreateDto dto) {
        SimpleErrors errors = new SimpleErrors(dto);
        SHORT_URL_VALIDATOR.validate(dto, errors);
        assertThat(errors.hasErrors()).isTrue();
    }
}
