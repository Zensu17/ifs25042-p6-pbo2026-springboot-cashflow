package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Card konten halaman agar tampilan konsisten.
 */
final class PageCard {

    private PageCard() {
    }

    static Div wrap(Component... content) {
        return wrap(true, content);
    }

    static Div wrap(boolean fullHeight, Component... content) {
        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setWidthFull();
        body.getStyle().set(ViewStyles.PROP_GAP, "0.65rem");

        for (Component component : content) {
            body.add(component);
        }

        if (fullHeight) {
            body.setSizeFull();
            body.getStyle().set(ViewStyles.PROP_MIN_HEIGHT, ViewStyles.VAL_ZERO).set("flex", "1 1 auto");
            if (content.length > 0) {
                Component last = content[content.length - 1];
                body.setFlexGrow(1, last);
                last.getElement().getStyle().set(ViewStyles.PROP_MIN_HEIGHT, ViewStyles.VAL_ZERO);
            }
        }

        Div card = new Div(body);
        card.setWidthFull();
        if (fullHeight) {
            card.setHeightFull();
            card.getStyle()
                    .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_FLEX)
                    .set(ViewStyles.PROP_FLEX_DIRECTION, ViewStyles.VAL_COLUMN)
                    .set(ViewStyles.PROP_MIN_HEIGHT, ViewStyles.VAL_ZERO)
                    .set(ViewStyles.PROP_OVERFLOW, ViewStyles.VAL_HIDDEN);
        }
        card.getStyle()
                .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_WHITE)
                .set(ViewStyles.PROP_BORDER, ViewStyles.BORDER_LIGHT)
                .set(ViewStyles.PROP_BORDER_RADIUS, ViewStyles.BORDER_RADIUS_CARD)
                .set(ViewStyles.PROP_PADDING, "1rem 1.1rem")
                .set("box-shadow", "0 8px 24px rgba(15, 23, 42, 0.06)")
                .set(ViewStyles.PROP_BOX_SIZING, ViewStyles.VAL_BORDER_BOX);
        return card;
    }
}
