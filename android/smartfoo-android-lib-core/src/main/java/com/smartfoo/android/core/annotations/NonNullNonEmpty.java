package com.smartfoo.android.core.annotations;

import android.support.annotation.Size;

//
// TODO:(pv) Implement NonNullNonBlank logic
//  http://grepcode.com/file/repo1.maven.org/maven2/org.hibernate/hibernate-validator/5.2.1.Final/org/hibernate/validator/constraints/NotBlank.java/
//  I am confused on how to do this because supposedly Android projects cannot compile annotation validator code.
//
@Size(min = 1)
public @interface NonNullNonEmpty
{
}