package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * Konfigurasi indikator loading global dan landmark aksesibilitas untuk indikator koneksi Vaadin.
 */
@Component
public class LoadingIndicatorInitListener implements VaadinServiceInitListener {

    private static final String MARK_CONNECTION_INDICATOR_JS = """
            const markIndicator = () => {
              const el = document.querySelector('vaadin-connection-indicator');
              if (el) {
                el.setAttribute('role', 'region');
                el.setAttribute('aria-label', 'Status koneksi');
                return true;
              }
              return false;
            };
            if (!markIndicator() && document.body) {
              const obs = new MutationObserver(() => { if (markIndicator()) obs.disconnect(); });
              obs.observe(document.body, { childList: true });
            }
            """;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            LoadingIndicatorConfiguration conf = uiEvent.getUI().getLoadingIndicatorConfiguration();
            conf.setApplyDefaultTheme(true);
            conf.setFirstDelay(150);
            conf.setSecondDelay(600);
            conf.setThirdDelay(1500);
            uiEvent.getUI().getPage().executeJs(MARK_CONNECTION_INDICATOR_JS);
        });
    }
}
