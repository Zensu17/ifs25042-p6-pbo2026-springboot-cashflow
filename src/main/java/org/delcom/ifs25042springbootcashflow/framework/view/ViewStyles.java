package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;

/**
 * Konstanta CSS dan helper komponen UI bersama untuk menghindari duplikasi kode pada layer view.
 */
final class ViewStyles {

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

    private ViewStyles() {
    }

    static void removeOverlayPadding(Dialog dialog) {
        dialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                dialog.getElement().executeJs(REMOVE_OVERLAY_PADDING_JS);
            }
        });
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
                createSummaryCard("Total Transaksi", totalCount, COLOR_BLUE_SOFT, COLOR_ACCENT_BLUE, "#1E40AF"),
                createSummaryCard("Total Pemasukan", incomeTotal, "#DCFCE7", "#16A34A", "#166534"),
                createSummaryCard("Total Pengeluaran", expenseTotal, "#FEE2E2", COLOR_DANGER, "#991B1B"),
                createSummaryCard("Saldo", balance, "#EDE9FE", "#7C3AED", "#5B21B6")
        );
        summary.setWidthFull();
        summary.setSpacing(true);
        summary.getStyle().set(PROP_ALIGN_ITEMS, "stretch");
        return summary;
    }

    static Div createSummaryCard(
            String label,
            Span value,
            String background,
            String accent,
            String valueColor
    ) {
        Span caption = new Span(label);
        caption.getStyle()
                .set(PROP_DISPLAY, VAL_BLOCK)
                .set(PROP_FONT_SIZE, "0.8rem")
                .set(PROP_FONT_WEIGHT, "500")
                .set(PROP_COLOR, accent);

        value.getStyle()
                .set(PROP_DISPLAY, VAL_BLOCK)
                .set(PROP_MARGIN_TOP, "0.15rem")
                .set(PROP_FONT_SIZE, "1.15rem")
                .set(PROP_FONT_WEIGHT, "700")
                .set(PROP_LINE_HEIGHT, "1.1")
                .set(PROP_COLOR, valueColor);

        Div card = new Div(caption, value);
        card.getStyle()
                .set("flex", "1")
                .set(PROP_MIN_WIDTH, VAL_ZERO)
                .set(PROP_BACKGROUND, background)
                .set(PROP_BORDER, "1px solid " + accent)
                .set("border-left", "4px solid " + accent)
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
                .set(PROP_BORDER_RADIUS, "12px")
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
