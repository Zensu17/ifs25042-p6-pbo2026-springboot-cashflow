package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.delcom.ifs25042springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25042springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25042springbootcashflow.usecase.AuthUseCase;

@Route("login")
@PageTitle("Login")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final String ASSIGN_INPUT_ID_JS = """
            const applyId = () => {
              const input = this.inputElement || this.querySelector('input');
              if (input) {
                input.id = $0;
                const label = this.querySelector('label');
                if (label) label.setAttribute('for', $0);
                return true;
              }
              return false;
            };
            if (!applyId()) {
              if (this.updateComplete) { this.updateComplete.then(applyId); }
              const obs = new MutationObserver(() => { if (applyId()) obs.disconnect(); });
              obs.observe(this, { childList: true, subtree: true });
            }
            """;

    private final transient IUserRepository userRepository;

    public LoginView(AuthUseCase authUseCase, IUserRepository userRepository) {
        this.userRepository = userRepository;
        ViewStyles.styleAuthPage(this);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        setInnerInputId(username, "login-username");
        setInnerInputId(password, "login-password");
        username.setWidthFull();
        password.setWidthFull();
        username.setValueChangeMode(ValueChangeMode.EAGER);
        password.setValueChangeMode(ValueChangeMode.EAGER);
        username.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);
        password.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

        ProgressBar loadingBar = ViewStyles.createLoadingBar();
        Button loginButton = new Button("Masuk");
        loginButton.setId("login-submit");

        ViewStyles.wireAuthSubmit(
                loginButton,
                loadingBar,
                () -> authUseCase.login(username.getValue(), password.getValue()),
                username,
                password
        );

        H1 title = ViewStyles.createPageHeading("Masuk");
        Paragraph subtitle = ViewStyles.createSubtitle(
                "Masuk untuk mengelola catatan keuangan Cashflow.", "0 0 0.15rem 0");
        RouterLink registerLink = ViewStyles.createAuthSwitchLink("Belum punya akun? Daftar", RegisterView.class);

        VerticalLayout form = new VerticalLayout(
                title,
                subtitle,
                username,
                password,
                loadingBar,
                loginButton,
                registerLink);
        add(ViewStyles.createAuthCard(form));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (AuthSession.currentUser(userRepository).isPresent()) {
            event.forwardTo(FinanceView.class);
        }
    }

    private static void setInnerInputId(Component component, String id) {
        Runnable apply = () -> component.getElement().executeJs(ASSIGN_INPUT_ID_JS, id);
        apply.run();
        component.addAttachListener(event -> apply.run());
    }
}
