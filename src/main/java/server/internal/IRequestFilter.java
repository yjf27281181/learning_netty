package server.internal;

import server.KidsContext;
import server.KidsRequest;

public interface IRequestFilter {
    public boolean filter(KidsContext ctx, KidsRequest req, boolean beforeOrAfter);
}
