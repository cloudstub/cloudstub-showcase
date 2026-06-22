package io.cloudstub.showcase.aws;

/** Database username/password retrieved from Secrets Manager. */
public record DbCredentials(String username, String password) {}
