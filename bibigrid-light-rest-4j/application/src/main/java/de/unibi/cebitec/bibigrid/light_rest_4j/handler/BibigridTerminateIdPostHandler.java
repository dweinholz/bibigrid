package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import java.util.HashMap;
import java.util.Map;

public class BibigridTerminateIdPostHandler implements LightHttpHandler {
    
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.endExchange();
    }
}
