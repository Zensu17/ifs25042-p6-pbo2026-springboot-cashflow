package org.delcom.ifs25042springbootcashflow.framework.view;

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

@Route("register")
@PageTitle("Registrasi")
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {

    private final transient IUserRepository userRepository;

    public RegisterView(AuthUseCase authUseCase, IUserRepository userRepository) {
        this.userRepository = userRepository;
        ViewStyles.styleAuthPage(this);

        TextField fullName = new TextField("Nama Lengkap");
        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        fullName.setWidthFull();
        username.setWidthFull();
        password.setWidthFull();
        fullName.setValueChangeMode(ValueChangeMode.EAGER);
        username.setValueChangeMode(ValueChangeMode.EAGER);
        password.setValueChangeMode(ValueChangeMode.EAGER);

        ProgressBar loadingBar = ViewStyles.createLoadingBar();

        Button registerButton = new Button("Daftar");

        Runnable submit = () -> {
            loadingBar.setVisible(true);
            registerButton.setEnabled(false);
            try {
                User user = authUseCase.register(username.getValue(), password.getValue(), fullName.getValue());
                AuthSession.login(user);
                UI.getCurrent().navigate(FinanceView.class);
            } catch (ValidationException ex) {
                loadingBar.setVisible(false);
                registerButton.setEnabled(true);
                ViewStyles.showError(ex.getMessage());
            }
        };

        registerButton.addClickListener(event -> submit.run());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
        registerButton.getStyle()
                .set(ViewStyles.PROP_MARGIN_TOP, "0.85rem")
                .set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);
        fullName.addKeyUpListener(Key.ENTER, event -> submit.run());
        username.addKeyUpListener(Key.ENTER, event -> submit.run());
        password.addKeyUpListener(Key.ENTER, event -> submit.run());

        H1 title = new H1("Registrasi");
        title.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

        Paragraph subtitle = new Paragraph("Buat akun baru untuk mengelola catatan keuangan.");
        subtitle.getStyle()
                .set(ViewStyles.PROP_MARGIN, "0 0 0.15rem 0")
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_MUTED);

        RouterLink loginLink = new RouterLink("Sudah punya akun? Masuk", LoginView.class);
        loginLink.getStyle()
                .set("align-self", ViewStyles.VAL_FLEX_END)
                .set("text-align", "right")
                .set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        fullName.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);
        username.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);
        password.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

        VerticalLayout form = new VerticalLayout(
                title,
                subtitle,
                fullName,
                username,
                password,
                loadingBar,
                registerButton,
                loginLink);
        add(ViewStyles.createAuthCard(form));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (AuthSession.currentUser(userRepository).isPresent()) {
            event.forwardTo(FinanceView.class);
        }
    }
}
