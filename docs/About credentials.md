# Basic Auth

The very first step is to obtain a token from Identity. To do this you'll find a request named *Generate token* under *SpringBoot Oauth2 Server* folder. To make this request work you'll need a user and a password (Ask to the Identity team how can you obtain them). On the Headers Authorization field type `Basic user:password` (the `user:password` string must be base 64 encoded)  

This auth corresponds with the environment variable named = {{BASIC_AUTH_IDENTITY}}

# Bearer

After the firs step you'll obtain a Bearer. This Bearer can be used for the rest of requests inside Identity.Token-mgt.postman_collection.

This Bearer will go in the environment variable named = {{GENERATED_INDENTITY_BEARER}}