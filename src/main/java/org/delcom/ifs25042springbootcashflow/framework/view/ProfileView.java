package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25042springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.delcom.ifs25042springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25042springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25042springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25042springbootcashflow.usecase.UserUseCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profil")
public class ProfileView extends VerticalLayout {

    private static final String PHOTO_SIZE = "120px";

    private final transient UserUseCase userUseCase;
    private final transient FileStorage fileStorage;
    private final Image photoPreview = new Image();
    private final Span photoInitial = new Span();
    private final Div photoFrame = new Div();
    private final Span photoPlaceholder = new Span("Belum ada foto profil. Upload foto di bawah.");

    public ProfileView(UserUseCase userUseCase, FileStorage fileStorage) {
        this.userUseCase = userUseCase;
        this.fileStorage = fileStorage;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_BG);

        User user = AuthSession.currentUser().orElseThrow();

        TextField fullName = new TextField("Nama Lengkap");
        fullName.setWidthFull();
        fullName.setValue(user.getFullName());

        TextField username = new TextField("Username");
        username.setWidthFull();
        username.setValue(user.getUsername());
        username.setReadOnly(true);

        PasswordField password = new PasswordField("Password Baru (opsional)");
        password.setWidthFull();

        configurePhotoFrame();
        refreshPhoto(user);

        AtomicReference<String> pendingPhotoName = new AtomicReference<>();
        AtomicReference<byte[]> pendingPhotoData = new AtomicReference<>();

        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            pendingPhotoName.set(metadata.fileName());
            pendingPhotoData.set(data);
        }));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        upload.setWidthFull();

        ProgressBar loadingBar = ViewStyles.createLoadingBar();

        Button savePhoto = new Button("Simpan Foto");
        savePhoto.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        Button saveProfile = new Button("Simpan Profil");
        saveProfile.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveProfile.getStyle()
                .set(ViewStyles.PROP_MARGIN_TOP, "0.5rem")
                .set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        savePhoto.addClickListener(event -> handleSavePhoto(
                user, pendingPhotoName, pendingPhotoData, loadingBar, savePhoto));

        saveProfile.addClickListener(event -> handleSaveProfile(
                user, fullName, password, loadingBar, saveProfile));

        H1 title = new H1("Profil");
        title.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);
        Paragraph subtitle = new Paragraph("Perbarui nama, password, dan foto profil.");
        subtitle.getStyle()
                .set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_MUTED);

        Span photoLabel = new Span("Foto Profil");
        photoLabel.getStyle()
                .set(ViewStyles.PROP_FONT_WEIGHT, "600")
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_TEXT);

        HorizontalLayout photoRow = new HorizontalLayout(photoFrame, photoPlaceholder);
        photoRow.setAlignItems(FlexComponent.Alignment.CENTER);
        photoRow.setSpacing(true);
        photoRow.setWidthFull();

        Div card = PageCard.wrap(
                false,
                title,
                subtitle,
                photoLabel,
                photoRow,
                upload,
                savePhoto,
                fullName,
                username,
                password,
                loadingBar,
                saveProfile);
        card.getStyle().set("max-width", "560px");
        add(card);
    }

    private void handleSavePhoto(
            User user,
            AtomicReference<String> pendingPhotoName,
            AtomicReference<byte[]> pendingPhotoData,
            ProgressBar loadingBar,
            Button savePhoto
    ) {
        loadingBar.setVisible(true);
        savePhoto.setEnabled(false);
        try {
            String filename = pendingPhotoName.get();
            byte[] data = pendingPhotoData.get();
            if (filename == null || filename.isBlank() || data == null) {
                ViewStyles.showError("Pilih file foto terlebih dahulu");
                return;
            }
            String path = fileStorage.store("profiles", filename, new ByteArrayInputStream(data));
            User updated = userUseCase.updatePhoto(user.getId(), path);
            AuthSession.refresh(updated);
            refreshPhoto(updated);
            pendingPhotoName.set(null);
            pendingPhotoData.set(null);
            ViewStyles.showSuccess("Foto profil diperbarui");
            UI.getCurrent().navigate(ProfileView.class);
        } catch (ValidationException | EntityNotFoundException | IOException ex) {
            ViewStyles.showError(ex.getMessage());
        } finally {
            loadingBar.setVisible(false);
            savePhoto.setEnabled(true);
        }
    }

    private void handleSaveProfile(
            User user,
            TextField fullName,
            PasswordField password,
            ProgressBar loadingBar,
            Button saveProfile
    ) {
        loadingBar.setVisible(true);
        saveProfile.setEnabled(false);
        try {
            User updated = userUseCase.updateProfile(user.getId(), fullName.getValue(), password.getValue());
            AuthSession.refresh(updated);
            password.clear();
            ViewStyles.showSuccess("Profil diperbarui");
        } catch (ValidationException | EntityNotFoundException ex) {
            ViewStyles.showError(ex.getMessage());
        } finally {
            loadingBar.setVisible(false);
            saveProfile.setEnabled(true);
        }
    }

    private void configurePhotoFrame() {
        photoPreview.setWidth(PHOTO_SIZE);
        photoPreview.setHeight(PHOTO_SIZE);
        photoPreview.getStyle()
                .set(ViewStyles.PROP_BORDER_RADIUS, ViewStyles.VAL_ROUND)
                .set(ViewStyles.PROP_OBJECT_FIT, ViewStyles.VAL_COVER)
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_BLOCK)
                .set(ViewStyles.PROP_CURSOR, "zoom-in");
        photoPreview.getElement().setAttribute("title", "Klik untuk memperbesar");
        photoPreview.addClickListener(
                event -> ImagePreviewDialog.open(photoPreview.getSrc(), photoPreview.getAlt().orElse("Foto profil")));

        ViewStyles.styleAvatarInitial(photoInitial, PHOTO_SIZE, "2.5rem");

        photoFrame.getStyle()
                .set(ViewStyles.PROP_WIDTH, PHOTO_SIZE)
                .set(ViewStyles.PROP_HEIGHT, PHOTO_SIZE)
                .set(ViewStyles.PROP_BORDER_RADIUS, ViewStyles.VAL_ROUND)
                .set(ViewStyles.PROP_OVERFLOW, ViewStyles.VAL_HIDDEN)
                .set(ViewStyles.PROP_BORDER, "3px solid " + ViewStyles.COLOR_BLUE_BORDER)
                .set(ViewStyles.PROP_FLEX_SHRINK, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_BACKGROUND, "#EFF6FF");

        photoPlaceholder.getStyle().set(ViewStyles.PROP_COLOR, "#6B7280");
    }

    private void refreshPhoto(User user) {
        photoInitial.setText(ViewStyles.userInitial(user));
        photoFrame.removeAll();
        if (user.getPhotoPath() == null || user.getPhotoPath().isBlank()) {
            photoFrame.add(photoInitial);
            photoPlaceholder.setVisible(true);
            return;
        }
        photoPreview.setSrc("/uploads/" + user.getPhotoPath() + "?t=" + System.currentTimeMillis());
        photoPreview.setAlt(user.getFullName());
        photoFrame.add(photoPreview);
        photoPlaceholder.setVisible(false);
    }
}
