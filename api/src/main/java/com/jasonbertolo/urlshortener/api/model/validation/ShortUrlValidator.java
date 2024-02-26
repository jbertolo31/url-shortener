package com.jasonbertolo.urlshortener.api.model.validation;

import com.jasonbertolo.urlshortener.api.model.dto.ShortUrlCreateDto;
import org.springframework.lang.NonNull;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public class ShortUrlValidator implements Validator {

    // OWASP URL validation regex
    // See https://owasp.org/www-community/OWASP_Validation_Regex_Repository
    @SuppressWarnings({"RegExpSimplifiable", "RegExpRedundantNestedCharacterClass", "RegExpDuplicateCharacterInClass"})
    public static final String VALID_URL_REGEX = "^((((https?|ftps?|gopher|telnet|nntp)://)|(mailto:|news:))" +
            "(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)([).!';/?:,][[:blank:]])?$"; //NOSONAR

    private static final Pattern URL_PATTERN = Pattern.compile(VALID_URL_REGEX);

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return ShortUrlCreateDto.class.equals(clazz);
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "url", "url is required");
        ShortUrlCreateDto dto = (ShortUrlCreateDto) target;

        // url - required
        if (dto.getUrl() != null && !isUrlValid(dto.getUrl())) {
            errors.rejectValue("url", "url is invalid");
        }
        if (dto.getUrl() != null && dto.getUrl().length() > 2000) {
            errors.rejectValue("url", "url is too long, 2000 max");
        }

        // description
        if (dto.getDescription() != null && dto.getDescription().length() > 100) {
            errors.rejectValue("description", "description is too long");
        }
    }

    private boolean isUrlValid(String url) {
        if (isNullOrEmpty(url)) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }
}
