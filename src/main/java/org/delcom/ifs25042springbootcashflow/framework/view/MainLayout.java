package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.delcom.ifs25042springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25042springbootcashflow.framework.auth.AuthSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout utama untuk halaman yang memerlukan login.
 */
public class MainLayout extends AppLayout implements BeforeEnterObserver, AfterNavigationObserver {

    private static final String ATTR_ACTIVE = "data-active";
    private static final String AVATAR_SIZE = "40px";
    private static final String FONT_SIZE_SUB = "0.95rem";
    private static final String BORDER_TRANSPARENT = "1px solid transparent";

    private final transient IUserRepository userRepository;
    private final transient List<NavEntry> navEntries = new ArrayList<>();
    private final Image headerAvatarImage = new Image();
    private final Span headerAvatarInitial = new Span();
    private final Div headerAvatar = new Div();

    private record NavEntry(RouterLink link, String pathPrefix) {
    }

    public MainLayout(IUserRepository userRepository) {
        this.userRepository = userRepository;
        setPrimarySection(Section.DRAWER);
        AuthSession.currentUser(userRepository);
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        HorizontalLayout brandTitle = appBrandTitle("1.2rem", "28px");

        configureHeaderAvatar();
        refreshHeaderAvatar(AuthSession.currentUser().orElse(null));

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), brandTitle, headerAvatar);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(brandTitle);
        header.setWidthFull();
        header.setPadding(true);
        header.getStyle()
                .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_WHITE)
                .set(ViewStyles.PROP_BORDER_BOTTOM, ViewStyles.BORDER_LIGHT);
        addToNavbar(header);
    }

    private static HorizontalLayout appBrandTitle(String fontSize, String iconSize) {
        Image logo = new Image("icons/icon.png", "Cashflow");
        logo.setWidth(iconSize);
        logo.setHeight(iconSize);
        logo.getStyle()
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_BLOCK)
                .set(ViewStyles.PROP_BORDER_RADIUS, "6px")
                .set(ViewStyles.PROP_OBJECT_FIT, ViewStyles.VAL_COVER)
                .set(ViewStyles.PROP_FLEX_SHRINK, ViewStyles.VAL_ZERO);

        Span title = new Span("Cashflow");
        title.getStyle()
                .set(ViewStyles.PROP_FONT_SIZE, fontSize)
                .set(ViewStyles.PROP_FONT_WEIGHT, "700")
                .set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_TEXT_DARK)
                .set(ViewStyles.PROP_LINE_HEIGHT, "1.2");

        HorizontalLayout brand = new HorizontalLayout(logo, title);
        brand.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        brand.setSpacing(false);
        brand.setPadding(false);
        brand.getStyle().set(ViewStyles.PROP_GAP, "0.65rem");
        return brand;
    }

    private void configureHeaderAvatar() {
        headerAvatarImage.setWidth(AVATAR_SIZE);
        headerAvatarImage.setHeight(AVATAR_SIZE);
        headerAvatarImage.getStyle()
                .set(ViewStyles.PROP_BORDER_RADIUS, ViewStyles.VAL_ROUND)
                .set(ViewStyles.PROP_OBJECT_FIT, ViewStyles.VAL_COVER)
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_BLOCK);

        ViewStyles.styleAvatarInitial(headerAvatarInitial, AVATAR_SIZE, FONT_SIZE_SUB);

        headerAvatar.removeAll();
        headerAvatar.add(headerAvatarInitial);
        headerAvatar.getStyle()
                .set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER)
                .set(ViewStyles.PROP_BORDER, "2px solid " + ViewStyles.COLOR_BLUE_BORDER)
                .set(ViewStyles.PROP_BORDER_RADIUS, ViewStyles.VAL_ROUND)
                .set(ViewStyles.PROP_OVERFLOW, ViewStyles.VAL_HIDDEN)
                .set(ViewStyles.PROP_WIDTH, AVATAR_SIZE)
                .set(ViewStyles.PROP_HEIGHT, AVATAR_SIZE)
                .set(ViewStyles.PROP_FLEX_SHRINK, ViewStyles.VAL_ZERO);
        headerAvatar.getElement().setAttribute("title", "Menu akun");

        ContextMenu accountMenu = new ContextMenu(headerAvatar);
        accountMenu.setOpenOnClick(true);

        AuthSession.currentUser().ifPresent(user -> {
            var identityItem = accountMenu.addItem(accountIdentity(user));
            identityItem.setEnabled(false);
            identityItem.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_DEFAULT);
            var profileItem = accountMenu.addItem(
                    menuItemContent(VaadinIcon.USER, "Profil", ViewStyles.COLOR_MUTED, ViewStyles.COLOR_TEXT),
                    event -> UI.getCurrent().navigate(ProfileView.class));
            profileItem.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);
            var logoutItem = accountMenu.addItem(
                    menuItemContent(VaadinIcon.SIGN_OUT, "Keluar", ViewStyles.COLOR_DANGER, ViewStyles.COLOR_DANGER),
                    event -> {
                        AuthSession.logout();
                        UI.getCurrent().getPage().setLocation("login");
                    });
            logoutItem.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);
        });
    }

    private static VerticalLayout accountIdentity(User user) {
        Span username = new Span(user.getUsername());
        username.getStyle()
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_BLOCK)
                .set(ViewStyles.PROP_FONT_WEIGHT, "700")
                .set(ViewStyles.PROP_FONT_SIZE, FONT_SIZE_SUB)
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_TEXT_DARK)
                .set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

        Span role = new Span(user.getRole().name());
        role.getStyle()
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_BLOCK)
                .set(ViewStyles.PROP_MARGIN_TOP, "0.15rem")
                .set(ViewStyles.PROP_FONT_SIZE, "0.75rem")
                .set(ViewStyles.PROP_FONT_WEIGHT, "600")
                .set("letter-spacing", "0.02em")
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_ACCENT_BLUE);

        VerticalLayout identity = new VerticalLayout(username, role);
        identity.setPadding(false);
        identity.setSpacing(false);
        identity.setMargin(false);
        identity.getStyle()
                .set(ViewStyles.PROP_GAP, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_PADDING, "0.15rem 0")
                .set(ViewStyles.PROP_MIN_WIDTH, "8rem");
        return identity;
    }

    private static HorizontalLayout menuItemContent(VaadinIcon iconType, String label, String iconColor, String textColor) {
        Icon icon = iconType.create();
        icon.setSize("1rem");
        icon.getStyle()
                .set(ViewStyles.PROP_COLOR, iconColor)
                .set(ViewStyles.PROP_FLEX_SHRINK, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

        Span text = new Span(label);
        text.getStyle()
                .set(ViewStyles.PROP_COLOR, textColor)
                .set(ViewStyles.PROP_FONT_WEIGHT, "600")
                .set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

        HorizontalLayout content = new HorizontalLayout(icon, text);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        content.setSpacing(false);
        content.setPadding(false);
        content.setMargin(false);
        content.setWidth(null);
        content.getStyle()
                .set(ViewStyles.PROP_GAP, "0.5rem")
                .set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_PADDING, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_JUSTIFY_CONTENT, ViewStyles.VAL_FLEX_START)
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_INLINE_FLEX);
        return content;
    }

    private void refreshHeaderAvatar(User user) {
        headerAvatarInitial.setText(ViewStyles.userInitial(user));
        headerAvatar.removeAll();
        if (user != null && user.getPhotoPath() != null && !user.getPhotoPath().isBlank()) {
            headerAvatarImage.setSrc("/uploads/" + user.getPhotoPath() + "?t=" + System.currentTimeMillis());
            headerAvatarImage.setAlt(user.getFullName());
            headerAvatar.add(headerAvatarImage);
        } else {
            headerAvatar.add(headerAvatarInitial);
        }
    }

    private void createDrawer() {
        VerticalLayout drawer = new VerticalLayout();
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.getStyle()
                .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_WHITE)
                .set("border-right", ViewStyles.BORDER_LIGHT);

        Div brand = new Div();
        brand.getStyle()
                .set(ViewStyles.PROP_PADDING, "1.25rem 1rem 1rem")
                .set(ViewStyles.PROP_BORDER_BOTTOM, ViewStyles.BORDER_LIGHT);
        brand.add(appBrandTitle("1.05rem", "30px"));

        VerticalLayout menu = new VerticalLayout();
        menu.setPadding(true);
        menu.setSpacing(false);
        menu.getStyle().set(ViewStyles.PROP_GAP, "0.35rem").set("padding-top", "1rem");

        menu.add(navItem("Cashflow Saya", FinanceView.class, "", VaadinIcon.WALLET));
        menu.add(navItem("Profil", ProfileView.class, "profile", VaadinIcon.USER));

        AuthSession.currentUser()
                .filter(User::isAdmin)
                .ifPresent(user -> menu.add(navItem("Daftar Pengguna", UsersView.class, "users", VaadinIcon.USERS)));

        drawer.add(brand, menu);
        drawer.expand(menu);
        addToDrawer(drawer);
    }

    private RouterLink navItem(
            String label,
            Class<? extends Component> view,
            String pathPrefix,
            VaadinIcon iconType
    ) {
        Icon icon = iconType.create();
        icon.setSize("1.05rem");
        icon.getStyle().set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_MUTED);

        Span text = new Span(label);
        text.getStyle()
                .set(ViewStyles.PROP_FONT_SIZE, FONT_SIZE_SUB)
                .set(ViewStyles.PROP_FONT_WEIGHT, "500")
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_TEXT);

        HorizontalLayout content = new HorizontalLayout(icon, text);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setSpacing(true);
        content.setPadding(false);
        content.getStyle().set(ViewStyles.PROP_GAP, "0.65rem");

        RouterLink link = new RouterLink();
        link.setRoute(view);
        link.add(content);
        link.getStyle()
                .set(ViewStyles.PROP_DISPLAY, ViewStyles.VAL_BLOCK)
                .set(ViewStyles.PROP_WIDTH, ViewStyles.VAL_FULL)
                .set("text-decoration", "none")
                .set(ViewStyles.PROP_PADDING, "0.7rem 0.85rem")
                .set(ViewStyles.PROP_BORDER_RADIUS, "10px")
                .set(ViewStyles.PROP_BOX_SIZING, ViewStyles.VAL_BORDER_BOX)
                .set(ViewStyles.PROP_BORDER, BORDER_TRANSPARENT)
                .set("transition", "background 0.15s ease")
                .set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        link.getElement().addEventListener("mouseover", e -> {
            if (!"true".equals(link.getElement().getAttribute(ATTR_ACTIVE))) {
                link.getStyle().set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_BG);
            }
        });
        link.getElement().addEventListener("mouseout", e -> {
            if (!"true".equals(link.getElement().getAttribute(ATTR_ACTIVE))) {
                link.getStyle().set(ViewStyles.PROP_BACKGROUND, "transparent");
            }
        });

        navEntries.add(new NavEntry(link, pathPrefix));
        return link;
    }

    private void highlightActive(String path) {
        String normalized = path == null ? "" : path;
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        for (NavEntry entry : navEntries) {
            boolean active = isNavActive(entry.pathPrefix(), normalized);
            RouterLink link = entry.link();
            if (active) {
                link.getElement().setAttribute(ATTR_ACTIVE, "true");
                link.getStyle()
                        .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_BLUE_SOFT)
                        .set(ViewStyles.PROP_BORDER, "1px solid " + ViewStyles.COLOR_BLUE_BORDER);
                styleNavChildren(link, ViewStyles.COLOR_ACCENT_BLUE, "#1E40AF", "600");
            } else {
                link.getElement().setAttribute(ATTR_ACTIVE, "false");
                link.getStyle()
                        .set(ViewStyles.PROP_BACKGROUND, "transparent")
                        .set(ViewStyles.PROP_BORDER, BORDER_TRANSPARENT);
                styleNavChildren(link, ViewStyles.COLOR_MUTED, ViewStyles.COLOR_TEXT, "500");
            }
        }
    }

    private static boolean isNavActive(String prefix, String normalized) {
        if (prefix.isEmpty()) {
            return normalized.isEmpty();
        }
        if ("users".equals(prefix) && normalized.startsWith("user-transactions")) {
            return true;
        }
        return normalized.equals(prefix) || normalized.startsWith(prefix + "/");
    }

    private static void styleNavChildren(RouterLink link, String iconColor, String textColor, String weight) {
        link.getChildren()
                .filter(HorizontalLayout.class::isInstance)
                .map(HorizontalLayout.class::cast)
                .findFirst()
                .ifPresent(row -> row.getChildren().forEach(child -> {
                    if (child instanceof Icon icon) {
                        icon.getStyle().set(ViewStyles.PROP_COLOR, iconColor);
                    }
                    if (child instanceof Span span) {
                        span.getStyle().set(ViewStyles.PROP_COLOR, textColor).set(ViewStyles.PROP_FONT_WEIGHT, weight);
                    }
                }));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        highlightActive(event.getLocation().getPath());
        refreshHeaderAvatar(AuthSession.currentUser(userRepository).orElse(null));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (AuthSession.currentUser(userRepository).isEmpty()) {
            event.forwardTo(LoginView.class);
        }
    }
}
