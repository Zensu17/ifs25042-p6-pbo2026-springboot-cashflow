package org.delcom.ifs25042springbootcashflow;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
@PWA(name = "Cashflow", shortName = "Cashflow", iconPath = "icons/icon.png")
public class App implements AppShellConfigurator {

    private static final String LUMO_STYLESHEET_URL = Lumo.STYLESHEET + "?v-c=25.3.4";
    private static final String APP_STYLESHEET_URL = "styles.css?v-c=1";

    public static void main(String[] args) throws Exception {
        DotEnv.load();
        SpringApplication.run(App.class, args);
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addMetaTag(
                "description",
                "Aplikasi Cashflow untuk mencatat pemasukan, pengeluaran, bukti transaksi, dan saldo keuangan pribadi."
        );
        Map<String, String> preloadStyle = Map.of(
                "rel", "preload",
                "as", "style",
                "onload", "this.onload=null;this.rel='stylesheet'"
        );
        settings.addLink(LUMO_STYLESHEET_URL, preloadStyle);
        settings.addLink(APP_STYLESHEET_URL, preloadStyle);
        settings.addInlineWithContents(
                "<noscript><link rel=\"stylesheet\" href=\""
                        + LUMO_STYLESHEET_URL
                        + "\"/><link rel=\"stylesheet\" href=\""
                        + APP_STYLESHEET_URL
                        + "\"/></noscript>",
                Inline.Wrapping.NONE
        );
    }
}
