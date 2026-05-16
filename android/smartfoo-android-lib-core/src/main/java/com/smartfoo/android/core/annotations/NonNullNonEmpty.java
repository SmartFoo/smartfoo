package com.smartfoo.android.core.annotations;

import androidx.annotation.Size;

//
// TODO:(pv) Implement NonNullNonBlank logic
//  http://grepcode.com/file/repo1.maven.org/maven2/org.hibernate/hibernate-validator/5.2.1.Final/org/hibernate/validator/constraints/NotBlank.java/
//  I am confused on how to do this because supposedly Android projects cannot compile annotation validator code.
//
/**
 * Marker annotation indicating that the annotated {@link String} (or array/collection element)
 * must be non-null and must contain at least one character.
 *
 * <p>Enforced statically via {@link Size#min()} = 1 for lint/IDE tooling. Runtime enforcement
 * is not currently implemented.</p>
 */
@Size(min = 1)
public @interface NonNullNonEmpty
{
}