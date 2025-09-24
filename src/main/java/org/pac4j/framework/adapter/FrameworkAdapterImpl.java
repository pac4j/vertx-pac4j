package org.pac4j.framework.adapter;

import lombok.val;
import org.pac4j.core.adapter.DefaultFrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.vertx.VertxFrameworkParameters;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.context.session.VertxSessionStore;
import org.pac4j.vertx.http.VertxHttpActionAdapter;

/**
 * Specific config startup for Vert.x.
 *
 * @author Jerome LELEU
 * @since 7.0.0
 */
public class FrameworkAdapterImpl extends DefaultFrameworkAdapter {

    @Override
    public void applyDefaultSettingsIfUndefined(final Config config) {
        CommonHelper.assertNotNull("config", config);

        config.setWebContextFactoryIfUndefined(fp -> new VertxWebContext( ((VertxFrameworkParameters)fp).routingContext() ));
        config.setHttpActionAdapterIfUndefined(VertxHttpActionAdapter.INSTANCE);

        super.applyDefaultSettingsIfUndefined(config);
    }

    @Override
    public String toString() {
        return "Vert.x";
    }
}
