package org.delcom.ifs25042springbootcashflow.framework.view;

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
        fullName.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);
        username.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);
        password.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

        ProgressBar loadingBar = ViewStyles.createLoadingBar();
        Button registerButton = new Button("Daftar");

        ViewStyles.wireAuthSubmit(
                registerButton,
                loadingBar,
                () -> authUseCase.register(username.getValue(), password.getValue(), fullName.getValue()),
                fullName,
                username,
                password
        );

        H1 title = ViewStyles.createPageHeading("Registrasi");
        Paragraph subtitle = ViewStyles.createSubtitle(
                "Buat akun baru untuk mengelola catatan keuangan.", "0 0 0.15rem 0");
        RouterLink loginLink = ViewStyles.createAuthSwitchLink("Sudah punya akun? Masuk", LoginView.class);

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
