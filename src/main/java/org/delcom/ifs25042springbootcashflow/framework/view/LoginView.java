package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.delcom.ifs25042springbootcashflow.domain.exception.ValidationException;
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

        ProgressBar loadingBar = ViewStyles.createLoadingBar();

        Button loginButton = new Button("Masuk");
        loginButton.setId("login-submit");

        Runnable submit = () -> {
            loadingBar.setVisible(true);
            loginButton.setEnabled(false);
            try {
                User user = authUseCase.login(username.getValue(), password.getValue());
                AuthSession.login(user);
                UI.getCurrent().navigate(FinanceView.class);
            } catch (ValidationException ex) {
                loadingBar.setVisible(false);
                loginButton.setEnabled(true);
                ViewStyles.showError(ex.getMessage());
            }
        };

        loginButton.addClickListener(event -> submit.run());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.getStyle()
                .set(ViewStyles.PROP_MARGIN_TOP, "0.85rem")
                .set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);
        username.addKeyUpListener(Key.ENTER, event -> submit.run());
        password.addKeyUpListener(Key.ENTER, event -> submit.run());

        H1 title = new H1("Masuk");
        title.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

        Paragraph subtitle = new Paragraph("Masuk untuk mengelola catatan keuangan Cashflow.");
        subtitle.getStyle()
                .set(ViewStyles.PROP_MARGIN, "0 0 0.15rem 0")
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_MUTED);

        RouterLink registerLink = new RouterLink("Belum punya akun? Daftar", RegisterView.class);
        registerLink.getStyle()
                .set("align-self", ViewStyles.VAL_FLEX_END)
                .set("text-align", "right")
                .set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        username.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);
        password.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

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
