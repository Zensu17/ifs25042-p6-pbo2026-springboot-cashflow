package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Modal untuk menampilkan gambar bukti transaksi / foto profil dengan zoom interaktif.
 */
final class ImagePreviewDialog {

    private static final String CENTER_ORIGIN = "center center";

    private ImagePreviewDialog() {
    }

    static void open(String imageUrl, String alt) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String title = alt == null || alt.isBlank() ? "Pratinjau Gambar" : alt;

        Span headerTitle = new Span(title);
        headerTitle.getStyle()
                .set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_WHITE)
                .set(ViewStyles.PROP_FONT_SIZE, "1.125rem")
                .set(ViewStyles.PROP_FONT_WEIGHT, "600")
                .set(ViewStyles.PROP_LINE_HEIGHT, "1.4");

        Span headerHint = new Span("Klik gambar untuk memperbesar / memperkecil");
        headerHint.getStyle()
                .set(ViewStyles.PROP_MARGIN_TOP, "0.2rem")
                .set(ViewStyles.PROP_COLOR, "#BFDBFE")
                .set(ViewStyles.PROP_FONT_SIZE, "0.8rem")
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_BLOCK);

        Div header = new Div(headerTitle, headerHint);
        header.getStyle()
                .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_PRIMARY)
                .set(ViewStyles.PROP_PADDING, "0.9rem 1.5rem")
                .set(ViewStyles.PROP_BOX_SIZING, ViewStyles.VAL_BORDER_BOX)
                .set(ViewStyles.PROP_WIDTH, ViewStyles.VAL_FULL);

        Image image = new Image(imageUrl, title);
        image.getStyle()
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_BLOCK)
                .set("max-width", ViewStyles.VAL_FULL)
                .set("max-height", "68vh")
                .set(ViewStyles.PROP_WIDTH, "auto")
                .set(ViewStyles.PROP_HEIGHT, "auto")
                .set(ViewStyles.PROP_OBJECT_FIT, "contain")
                .set(ViewStyles.PROP_BORDER_RADIUS, "8px")
                .set(ViewStyles.PROP_CURSOR, "zoom-in")
                .set("transition", "transform 0.25s ease")
                .set("user-select", "none");
        image.getElement().setAttribute("title", "Klik untuk zoom");

        Div imageWrapper = new Div(image);
        imageWrapper.getStyle()
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_FLEX)
                .set(ViewStyles.PROP_JUSTIFY_CONTENT, ViewStyles.VAL_CENTER)
                .set(ViewStyles.PROP_ALIGN_ITEMS, ViewStyles.VAL_CENTER)
                .set(ViewStyles.PROP_OVERFLOW, "auto")
                .set("max-height", "70vh")
                .set("max-width", "min(88vw, 860px)")
                .set(ViewStyles.PROP_WIDTH, ViewStyles.VAL_FULL)
                .set(ViewStyles.PROP_BOX_SIZING, ViewStyles.VAL_BORDER_BOX);

        Div body = new Div(imageWrapper);
        body.getStyle()
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_FLEX)
                .set(ViewStyles.PROP_JUSTIFY_CONTENT, ViewStyles.VAL_CENTER)
                .set(ViewStyles.PROP_ALIGN_ITEMS, ViewStyles.VAL_CENTER)
                .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_WHITE)
                .set(ViewStyles.PROP_PADDING, "1rem 1.25rem")
                .set(ViewStyles.PROP_BOX_SIZING, ViewStyles.VAL_BORDER_BOX)
                .set(ViewStyles.PROP_WIDTH, ViewStyles.VAL_FULL)
                .set(ViewStyles.PROP_OVERFLOW, ViewStyles.VAL_HIDDEN);

        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.setResizable(false);
        dialog.setMaxWidth("95vw");

        AtomicReference<Double> currentScale = new AtomicReference<>(1.0);

        Runnable applyScale = () -> {
            double scale = currentScale.get();
            if (scale <= 1.0) {
                currentScale.set(1.0);
                image.getStyle()
                        .set("transform", "scale(1)")
                        .set(ViewStyles.PROP_CURSOR, "zoom-in");
            } else {
                image.getStyle()
                        .set("transform", "scale(" + scale + ")")
                        .set("transform-origin", CENTER_ORIGIN)
                        .set(ViewStyles.PROP_CURSOR, "zoom-out");
            }
        };

        image.addClickListener(event -> {
            if (currentScale.get() > 1.0) {
                currentScale.set(1.0);
            } else {
                currentScale.set(1.8);
            }
            applyScale.run();
        });

        Button zoomOutButton = new Button("Perkecil", VaadinIcon.SEARCH_MINUS.create(), event -> {
            currentScale.set(Math.max(currentScale.get() - 0.4, 1.0));
            applyScale.run();
        });
        zoomOutButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        Button zoomInButton = new Button("Perbesar", VaadinIcon.SEARCH_PLUS.create(), event -> {
            currentScale.set(Math.min(currentScale.get() + 0.4, 3.0));
            applyScale.run();
        });
        zoomInButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        Button closeButton = new Button("Tutup", VaadinIcon.CLOSE.create(), event -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        Div footer = new Div(zoomOutButton, zoomInButton, closeButton);
        footer.getStyle()
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_FLEX)
                .set(ViewStyles.PROP_JUSTIFY_CONTENT, ViewStyles.VAL_FLEX_END)
                .set(ViewStyles.PROP_ALIGN_ITEMS, ViewStyles.VAL_CENTER)
                .set(ViewStyles.PROP_GAP, "0.6rem")
                .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_BG)
                .set(ViewStyles.PROP_BORDER_TOP, ViewStyles.BORDER_LIGHT)
                .set(ViewStyles.PROP_PADDING, "0.85rem 1.5rem 1.1rem")
                .set(ViewStyles.PROP_BOX_SIZING, ViewStyles.VAL_BORDER_BOX)
                .set(ViewStyles.PROP_WIDTH, ViewStyles.VAL_FULL);

        Div shell = new Div(header, body, footer);
        shell.getStyle()
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_FLEX)
                .set(ViewStyles.PROP_FLEX_DIRECTION, ViewStyles.VAL_COLUMN)
                .set(ViewStyles.PROP_WIDTH, ViewStyles.VAL_FULL)
                .set(ViewStyles.PROP_OVERFLOW, ViewStyles.VAL_HIDDEN)
                .set(ViewStyles.PROP_BORDER_RADIUS, "12px")
                .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_WHITE);

        dialog.removeAll();
        dialog.add(shell);
        ViewStyles.removeOverlayPadding(dialog);
        dialog.open();
    }

    static Image clickableThumbnail(String imageUrl, String alt, String height) {
        return clickableThumbnail(imageUrl, alt, height, "6px");
    }

    static Image clickableThumbnail(String imageUrl, String alt, String height, String borderRadius) {
        Image image = new Image(imageUrl, alt);
        image.setHeight(height);
        image.getStyle()
                .set(ViewStyles.PROP_BORDER_RADIUS, borderRadius)
                .set(ViewStyles.PROP_OBJECT_FIT, ViewStyles.VAL_COVER)
                .set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER)
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_BLOCK);
        image.getElement().setAttribute("title", "Klik untuk memperbesar");
        image.addClickListener(event -> open(imageUrl, alt));
        return image;
    }
}
