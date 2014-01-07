package com.ft.methodeapi.client.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Validates an iterable group of objects using a regex.</p>
 *
 * <p>Inspired by <a href="http://stackoverflow.com/questions/20096155/pattern-annotation-on-list-of-strings">Nikos Paraskevopoulos' contribution</a> to Stack Overflow.</p>
 *
 * @see javax.validation.constraints.Pattern
 * @author Simon.Gibbs
 */
@Constraint(validatedBy=MultivaluePatternValidator.class)
@Target({ METHOD, FIELD, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
public @interface MultivaluePattern  {

    java.lang.String regexp();

    /**
     * A standard message template, except that "{validatedValue}" will be interpolated by the validator
     * @return The error message template.
     */
    String message() default "value \"{validatedValue}\" must match expression \"{regexp}\"";


    /**
     * @return The groups the constraint belongs to.
     */
    Class<?>[] groups() default { };

    /**
     * @return The payload associated to the constraint
     */
    Class<? extends Payload>[] payload() default {};

}
