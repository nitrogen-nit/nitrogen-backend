package vn.nitrogen.common.domain;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a database reference to another business module.
 *
 * <p>The field must be a {@link java.util.UUID}. The value is the owning module
 * name, for example {@code "identity"} for {@code identity.users(id)}.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface ModuleReference {

    String value();
}
