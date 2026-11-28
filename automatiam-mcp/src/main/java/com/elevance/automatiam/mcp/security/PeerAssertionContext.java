package com.elevance.automatiam.mcp.security;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

/** Carries the transport-layer observations through the reactive chain. */
public final class PeerAssertionContext {

    private static final String KEY = PeerAssertionContext.class.getName();

    private PeerAssertionContext() {
    }

    public static Context with(PeerAssertion assertion) {
        return Context.of(KEY, assertion);
    }

    /** Yields {@link PeerAssertion#NONE} when the filter did not run, which den […] */
    public static PeerAssertion from(ContextView view) {
        return view.getOrDefault(KEY, PeerAssertion.NONE);
    }
}
