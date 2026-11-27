package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25042springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.delcom.ifs25042springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25042springbootcashflow.framework.auth.AuthSession;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Konstanta CSS dan helper komponen UI bersama untuk menghindari duplikasi kode pada layer view.
 */
final class ViewStyles {

    static final String ATTR_ROLE = "role";
    static final String ROLE_MAIN = "main";

    static final String PROP_CURSOR = "cursor";
    static final String PROP_MARGIN = "margin";
    static final String PROP_PADDING = "padding";
    static final String PROP_COLOR = "color";
    static final String PROP_BACKGROUND = "background";
    static final String PROP_DISPLAY = "display";
    static final String PROP_WIDTH = "width";
    static final String PROP_HEIGHT = "height";
    static final String PROP_MIN_HEIGHT = "min-height";
    static final String PROP_MIN_WIDTH = "min-width";
    static final String PROP_BORDER = "border";
    static final String PROP_BORDER_RADIUS = "border-radius";
    static final String PROP_BOX_SIZING = "box-sizing";
    static final String PROP_FONT_SIZE = "font-size";
    static final String PROP_FONT_WEIGHT = "font-weight";
    static final String PROP_ALIGN_ITEMS = "align-items";
    static final String PROP_JUSTIFY_CONTENT = "justify-content";
    static final String PROP_OVERFLOW = "overflow";
    static final String PROP_GAP = "gap";
    static final String PROP_FLEX_SHRINK = "flex-shrink";
    static final String PROP_OBJECT_FIT = "object-fit";
    static final String PROP_LINE_HEIGHT = "line-height";
    static final String PROP_FLEX_DIRECTION = "flex-direction";
    static final String PROP_BORDER_TOP = "border-top";
    static final String PROP_BORDER_BOTTOM = "border-bottom";
    static final String PROP_MARGIN_TOP = "margin-top";

    static final String VAL_POINTER = "pointer";
    static final String VAL_DEFAULT = "default";
    static final String VAL_BLOCK = "block";
    static final String VAL_FLEX = "flex";
    static final String VAL_INLINE_FLEX = "inline-flex";
    static final String VAL_CENTER = "center";
    static final String VAL_FLEX_END = "flex-end";
    static final String VAL_FLEX_START = "flex-start";
    static final String VAL_BORDER_BOX = "border-box";
    static final String VAL_HIDDEN = "hidden";
    static final String VAL_FULL = "100%";
    static final String VAL_ZERO = "0";
    static final String VAL_COLUMN = "column";
    static final String VAL_COVER = "cover";
    static final String VAL_ROUND = "50%";

    static final String COLOR_WHITE = "#FFFFFF";
    static final String COLOR_BG = "#F3F4F6";
    static final String COLOR_PRIMARY = "#2563EB";
    static final String COLOR_DANGER = "#DC2626";
    static final String COLOR_TEXT_DARK = "#111827";
    static final String COLOR_TEXT = "#1F2937";
    static final String COLOR_MUTED = "#4B5563";
    static final String COLOR_ACCENT_BLUE = "#1D4ED8";
    static final String COLOR_BLUE_SOFT = "#DBEAFE";
    static final String COLOR_BLUE_BORDER = "#93C5FD";
    static final String BORDER_LIGHT = "1px solid #E5E7EB";
    static final String BORDER_RADIUS_CARD = "12px";

    static final String ZERO_AMOUNT = "Rp 0.00";
    static final String LABEL_CANCEL = "Batal";

    private static final String REMOVE_OVERLAY_PADDING_JS = """
            const overlay = this.$.overlay;
            if (!overlay || !overlay.shadowRoot) {
              return;
            }
            const content = overlay.shadowRoot.querySelector('[part="content"]');
            if (content) {
              content.style.padding = '0';
              content.style.overflow = 'visible';
              content.style.maxHeight = 'none';
            }
            const overlayPart = overlay.shadowRoot.querySelector('[part="overlay"]');
            if (overlayPart) {
              overlayPart.style.padding = '0';
              overlayPart.style.overflow = 'hidden';
            }
            """;

    private record CardPalette(String background, String border, String caption, String value) {
    }

    @FunctionalInterface
    interface AuthSubmitAction {
        User execute() throws ValidationException;
    }

    private ViewStyles() {
    }

    static void removeOverlayPadding(Dialog dialog) {
        dialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                dialog.getElement().executeJs(REMOVE_OVERLAY_PADDING_JS);
            }
        });
    }

    static ProgressBar createLoadingBar() {
        ProgressBar bar = new ProgressBar();
        bar.setIndeterminate(true);
        bar.setVisible(false);
        bar.setWidthFull();
        return bar;
    }

    static Div createDialogHeader(String title, String background, Component... extra) {
        Span headerTitle = new Span(title);
        headerTitle.getStyle()
                .set(PROP_MARGIN, VAL_ZERO)
                .set(PROP_COLOR, COLOR_WHITE)
                .set(PROP_FONT_SIZE, "1.125rem")
                .set(PROP_FONT_WEIGHT, "600")
                .set(PROP_LINE_HEIGHT, "1.4");

        Div header = new Div(headerTitle);
        for (Component component : extra) {
            header.add(component);
        }
        header.getStyle()
                .set(PROP_BACKGROUND, background)
                .set(PROP_PADDING, "1rem 1.5rem")
                .set(PROP_BOX_SIZING, VAL_BORDER_BOX)
                .set(PROP_WIDTH, VAL_FULL);
        return header;
    }

    static Div createDialogFooter(String gap, Button... buttons) {
        Div footer = new Div();
        footer.getStyle()
                .set(PROP_DISPLAY, VAL_FLEX)
                .set(PROP_JUSTIFY_CONTENT, VAL_FLEX_END)
                .set(PROP_ALIGN_ITEMS, VAL_CENTER)
                .set(PROP_GAP, gap)
                .set(PROP_BACKGROUND, COLOR_BG)
                .set(PROP_BORDER_TOP, BORDER_LIGHT)
                .set(PROP_PADDING, "0.85rem 1.5rem 1.1rem")
                .set(PROP_BOX_SIZING, VAL_BORDER_BOX)
                .set(PROP_WIDTH, VAL_FULL);
        for (Button btn : buttons) {
            btn.getStyle().set(PROP_CURSOR, VAL_POINTER);
        }
        footer.add(buttons);
        return footer;
    }

    static void mountDialogShell(Dialog dialog, Component... sections) {
        Div shell = new Div(sections);
        shell.getStyle()
                .set(PROP_DISPLAY, VAL_FLEX)
                .set(PROP_FLEX_DIRECTION, VAL_COLUMN)
                .set(PROP_WIDTH, VAL_FULL)
                .set(PROP_OVERFLOW, VAL_HIDDEN)
                .set(PROP_BORDER_RADIUS, BORDER_RADIUS_CARD)
                .set(PROP_BACKGROUND, COLOR_WHITE);

        dialog.removeAll();
        dialog.add(shell);
        removeOverlayPadding(dialog);
    }

    static void styleAvatarInitial(Span initial, String size, String fontSize) {
        initial.getStyle()
                .set(PROP_WIDTH, size)
                .set(PROP_HEIGHT, size)
                .set(PROP_BORDER_RADIUS, VAL_ROUND)
                .set(PROP_DISPLAY, VAL_INLINE_FLEX)
                .set(PROP_ALIGN_ITEMS, VAL_CENTER)
                .set(PROP_JUSTIFY_CONTENT, VAL_CENTER)
                .set(PROP_BACKGROUND, COLOR_BLUE_SOFT)
                .set(PROP_COLOR, COLOR_ACCENT_BLUE)
                .set(PROP_FONT_WEIGHT, "700")
                .set(PROP_FONT_SIZE, fontSize);
    }

    static boolean requireAdmin(BeforeEnterEvent event) {
        User current = AuthSession.currentUser().orElse(null);
        if (current == null || !current.isAdmin()) {
            event.forwardTo(FinanceView.class);
            return false;
        }
        return true;
    }

    static void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    static void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    static HorizontalLayout buildSummaryCards(
            Span totalCount,
            Span incomeTotal,
            Span expenseTotal,
            Span balance
    ) {
        HorizontalLayout summary = new HorizontalLayout(
                createSummaryCard("Total Transaksi", totalCount,
                        new CardPalette(COLOR_BLUE_SOFT, COLOR_ACCENT_BLUE, "#1E40AF", "#1E3A8A")),
                createSummaryCard("Total Pemasukan", incomeTotal,
                        new CardPalette("#DCFCE7", "#16A34A", "#166534", "#14532D")),
                createSummaryCard("Total Pengeluaran", expenseTotal,
                        new CardPalette("#FEE2E2", COLOR_DANGER, "#991B1B", "#7F1D1D")),
                createSummaryCard("Saldo", balance,
                        new CardPalette("#EDE9FE", "#7C3AED", "#5B21B6", "#4C1D95"))
        );
        summary.setWidthFull();
        summary.setSpacing(true);
        summary.getStyle().set(PROP_ALIGN_ITEMS, "stretch");
        return summary;
    }

    private static Div createSummaryCard(String label, Span value, CardPalette palette) {
        Span caption = new Span(label);
        caption.getStyle()
                .set(PROP_DISPLAY, VAL_BLOCK)
                .set(PROP_FONT_SIZE, "0.8rem")
                .set(PROP_FONT_WEIGHT, "600")
                .set(PROP_COLOR, palette.caption());

        value.getStyle()
                .set(PROP_DISPLAY, VAL_BLOCK)
                .set(PROP_MARGIN_TOP, "0.15rem")
                .set(PROP_FONT_SIZE, "1.15rem")
                .set(PROP_FONT_WEIGHT, "700")
                .set(PROP_LINE_HEIGHT, "1.1")
                .set(PROP_COLOR, palette.value());

        Div card = new Div(caption, value);
        card.getStyle()
                .set("flex", "1")
                .set(PROP_MIN_WIDTH, VAL_ZERO)
                .set(PROP_BACKGROUND, palette.background())
                .set(PROP_BORDER, "1px solid " + palette.border())
                .set("border-left", "4px solid " + palette.border())
                .set(PROP_BORDER_RADIUS, "10px")
                .set(PROP_PADDING, "0.55rem 0.85rem")
                .set(PROP_BOX_SIZING, VAL_BORDER_BOX);
        return card;
    }

    static void styleAuthPage(VerticalLayout page) {
        page.setSizeFull();
        page.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        page.setAlignItems(FlexComponent.Alignment.CENTER);
        page.getStyle().set(PROP_BACKGROUND, COLOR_BG);
        page.getElement().setAttribute(ATTR_ROLE, ROLE_MAIN);
    }

    static void styleContentPage(VerticalLayout page, boolean padded) {
        page.setSizeFull();
        page.setPadding(padded);
        page.setSpacing(false);
        page.getStyle().set(PROP_BACKGROUND, COLOR_BG);
        page.getElement().setAttribute(ATTR_ROLE, ROLE_MAIN);
    }

    static H1 createPageHeading(String text) {
        H1 heading = new H1(text);
        heading.getStyle().set(PROP_MARGIN, VAL_ZERO);
        return heading;
    }

    static Paragraph createSubtitle(String text, String margin) {
        Paragraph subtitle = new Paragraph(text);
        subtitle.getStyle()
                .set(PROP_MARGIN, margin)
                .set(PROP_COLOR, COLOR_MUTED);
        return subtitle;
    }

    static RouterLink createAuthSwitchLink(String text, Class<? extends Component> target) {
        RouterLink link = new RouterLink(text, target);
        link.getStyle()
                .set("align-self", VAL_FLEX_END)
                .set("text-align", "right")
                .set(PROP_CURSOR, VAL_POINTER);
        return link;
    }

    static void wireAuthSubmit(
            Button submitButton,
            ProgressBar loadingBar,
            AuthSubmitAction action,
            KeyNotifier... enterFields
    ) {
        Runnable submit = () -> {
            loadingBar.setVisible(true);
            submitButton.setEnabled(false);
            try {
                User user = action.execute();
                AuthSession.login(user);
                UI.getCurrent().navigate(FinanceView.class);
            } catch (ValidationException ex) {
                loadingBar.setVisible(false);
                submitButton.setEnabled(true);
                showError(ex.getMessage());
            }
        };

        submitButton.addClickListener(event -> submit.run());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidthFull();
        submitButton.getStyle()
                .set(PROP_MARGIN_TOP, "0.85rem")
                .set(PROP_CURSOR, VAL_POINTER);
        for (KeyNotifier field : enterFields) {
            field.addKeyUpListener(Key.ENTER, event -> submit.run());
        }
    }

    static Upload createImageUpload(AtomicReference<String> pendingName, AtomicReference<byte[]> pendingData) {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            pendingName.set(metadata.fileName());
            pendingData.set(data);
        }));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        return upload;
    }

    static Component createProofThumbnail(Transaction transaction) {
        if (transaction.getProofPath() == null || transaction.getProofPath().isBlank()) {
            return new Span("-");
        }
        return ImagePreviewDialog.clickableThumbnail(
                "/uploads/" + transaction.getProofPath(),
                "Bukti Transaksi #" + transaction.getId(),
                "48px"
        );
    }

    static Div createAuthCard(VerticalLayout form) {
        form.setPadding(false);
        form.setSpacing(false);
        form.setWidthFull();
        form.getStyle().set(PROP_GAP, "0.45rem");

        Div card = new Div(form);
        card.getStyle()
                .set(PROP_WIDTH, VAL_FULL)
                .set("max-width", "420px")
                .set(PROP_BACKGROUND, COLOR_WHITE)
                .set(PROP_BORDER, BORDER_LIGHT)
                .set(PROP_BORDER_RADIUS, BORDER_RADIUS_CARD)
                .set(PROP_PADDING, "1.35rem 1.35rem")
                .set("box-shadow", "0 8px 24px rgba(15, 23, 42, 0.08)")
                .set(PROP_BOX_SIZING, VAL_BORDER_BOX);
        return card;
    }

    static String userInitial(User user) {
        if (user == null) {
            return "?";
        }
        String source = user.getFullName() == null || user.getFullName().isBlank()
                ? user.getUsername()
                : user.getFullName();
        return source.substring(0, 1).toUpperCase();
    }
}
