package org.eclipse.jetty;

// Required to make v9 compatible source work against the v8 version of Jetty.
// TODO: make including this dependent on the jetty version so there's one place to
// change it.
jetty.v8compat extends lib {
   compiledOnly = true;
}
