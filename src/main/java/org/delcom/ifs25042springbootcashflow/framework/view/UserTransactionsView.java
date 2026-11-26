package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25042springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25042springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.delcom.ifs25042springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25042springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25042springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25042springbootcashflow.usecase.UserUseCase;

@Route(value = "user-transactions", layout = MainLayout.class)
@PageTitle("Transaksi Pengguna")
public class UserTransactionsView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private final UserUseCase userUseCase;
    private final FinanceUseCase financeUseCase;
    private final FinancePresenter presenter;
    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final H1 title = new H1("Transaksi Pengguna");
    private final Paragraph empty = new Paragraph("Belum ada transaksi.");
    private final Span totalCountValue = new Span("0");
    private final Span incomeTotalValue = new Span("Rp 0.00");
    private final Span expenseTotalValue = new Span("Rp 0.00");
    private final Span balanceValue = new Span("Rp 0.00");

    public UserTransactionsView(
            UserUseCase userUseCase,
            FinanceUseCase financeUseCase,
            FinancePresenter presenter
    ) {
        this.userUseCase = userUseCase;
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        title.getStyle().set("margin", "0");
        empty.getStyle().set("margin", "0").set("color", "#4B5563");

        grid.addComponentColumn(tx -> {
            if (tx.getProofPath() == null || tx.getProofPath().isBlank()) {
                return new Span("-");
            }
            return ImagePreviewDialog.clickableThumbnail(
                    "/uploads/" + tx.getProofPath(),
                    "Bukti Transaksi #" + tx.getId(),
                    "48px"
            );
        }).setHeader("Bukti").setAutoWidth(true);
        grid.addColumn(Transaction::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Transaction::getDescription).setHeader("Keterangan").setFlexGrow(1);
        grid.addColumn(tx -> presenter.formatAmount(tx.getAmount())).setHeader("Jumlah").setAutoWidth(true);
        grid.addColumn(tx -> presenter.formatType(tx.getType())).setHeader("Tipe").setAutoWidth(true);
        grid.addColumn(presenter::formatCreatedAt).setHeader("Waktu").setAutoWidth(true);
        grid.setSizeFull();
        GridStyles.apply(grid);

        Div card = PageCard.wrap(title, buildSummaryCards(), empty, grid);
        add(card);
        expand(card);
    }

    private HorizontalLayout buildSummaryCards() {
        HorizontalLayout summary = new HorizontalLayout(
                createSummaryCard("Total Transaksi", totalCountValue, "#DBEAFE", "#1D4ED8", "#1E40AF"),
                createSummaryCard("Total Pemasukan", incomeTotalValue, "#DCFCE7", "#16A34A", "#166534"),
                createSummaryCard("Total Pengeluaran", expenseTotalValue, "#FEE2E2", "#DC2626", "#991B1B"),
                createSummaryCard("Saldo", balanceValue, "#EDE9FE", "#7C3AED", "#5B21B6")
        );
        summary.setWidthFull();
        summary.setSpacing(true);
        return summary;
    }

    private Div createSummaryCard(
            String label,
            Span value,
            String background,
            String accent,
            String valueColor
    ) {
        Span caption = new Span(label);
        caption.getStyle()
                .set("display", "block")
                .set("font-size", "0.8rem")
                .set("font-weight", "500")
                .set("color", accent);

        value.getStyle()
                .set("display", "block")
                .set("margin-top", "0.15rem")
                .set("font-size", "1.1rem")
                .set("font-weight", "700")
                .set("color", valueColor);

        Div card = new Div(caption, value);
        card.getStyle()
                .set("flex", "1")
                .set("min-width", "0")
                .set("background", background)
                .set("border", "1px solid " + accent)
                .set("border-left", "4px solid " + accent)
                .set("border-radius", "10px")
                .set("padding", "0.5rem 0.8rem")
                .set("box-sizing", "border-box");
        return card;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User current = AuthSession.currentUser().orElse(null);
        if (current == null || !current.isAdmin()) {
            event.forwardTo(FinanceView.class);
        }
    }

    @Override
    public void setParameter(BeforeEvent event, Integer userId) {
        User current = AuthSession.currentUser().orElse(null);
        if (current == null || !current.isAdmin()) {
            return;
        }
        try {
            User owner = userUseCase.getUser(userId);
            title.setText("Cashflow milik " + owner.getFullName() + " (@" + owner.getUsername() + ")");
            var transactions = financeUseCase.getTransactionsByUser(userId);
            totalCountValue.setText(String.valueOf(transactions.size()));
            incomeTotalValue.setText(presenter.formatAmount(financeUseCase.getTotalIncome(userId)));
            expenseTotalValue.setText(presenter.formatAmount(financeUseCase.getTotalExpense(userId)));
            balanceValue.setText(presenter.formatAmount(financeUseCase.getBalance(userId)));
            grid.setItems(transactions);
            empty.setVisible(transactions.isEmpty());
            grid.setVisible(!transactions.isEmpty());
        } catch (EntityNotFoundException ex) {
            event.forwardTo(UsersView.class);
        }
    }
}
