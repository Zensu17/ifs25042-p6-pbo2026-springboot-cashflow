package org.delcom.ifs25042springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25042springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25042springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25042springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25042springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25042springbootcashflow.domain.entity.User;
import org.delcom.ifs25042springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25042springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25042springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25042springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25042springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25042springbootcashflow.usecase.FinanceUseCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tampilan web Vaadin catatan keuangan (Cashflow) milik pengguna yang sedang login.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Cashflow")
public class FinanceView extends VerticalLayout {

    private static final String LABEL_DESCRIPTION = "Keterangan";
    private static final String LABEL_AMOUNT_INPUT = "Jumlah (Rp)";
    private static final String MSG_NO_DATA = "- Data transaksi belum tersedia!";
    private static final String PROOF_FOLDER = "proofs";

    private final transient FinanceUseCase financeUseCase;
    private final transient FinancePresenter presenter;
    private final transient FileStorage fileStorage;
    private final transient FinanceAiUseCase financeAiUseCase;

    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final TextField searchField = new TextField();
    private final Select<TransactionTypeFilter> typeFilter = new Select<>();
    private final Select<SortOption> sortSelect = new Select<>();
    private final Paragraph statusLabel = new Paragraph();
    private final Span emptyState = new Span(MSG_NO_DATA);
    private final Span totalCountValue = new Span("0");
    private final Span incomeTotalValue = new Span(ViewStyles.ZERO_AMOUNT);
    private final Span expenseTotalValue = new Span(ViewStyles.ZERO_AMOUNT);
    private final Span balanceValue = new Span(ViewStyles.ZERO_AMOUNT);

    private record AiDialogControls(
            Dialog dialog,
            TextArea promptArea,
            ProgressBar loadingBar,
            Button generateButton,
            Button applyButton,
            Button cancelButton,
            Grid<AiGeneratedTransaction> previewGrid,
            List<AiGeneratedTransaction> generatedItems
    ) {
    }

    @FunctionalInterface
    private interface TransactionSaveAction {
        void execute(long amount, String proofPath) throws ValidationException, EntityNotFoundException;
    }

    public FinanceView(
            FinanceUseCase financeUseCase,
            FinancePresenter presenter,
            FileStorage fileStorage,
            FinanceAiUseCase financeAiUseCase
    ) {
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;
        this.financeAiUseCase = financeAiUseCase;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_BG)
                .set(ViewStyles.PROP_PADDING, "0.75rem")
                .set(ViewStyles.PROP_MIN_HEIGHT, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_BOX_SIZING, ViewStyles.VAL_BORDER_BOX);

        H1 title = new H1("Cashflow Saya");
        title.getStyle()
                .set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_FONT_SIZE, "1.45rem")
                .set(ViewStyles.PROP_LINE_HEIGHT, "1.2");

        statusLabel.getStyle()
                .set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO)
                .set(ViewStyles.PROP_FONT_SIZE, "0.875rem")
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_MUTED);

        configureGrid();
        configureToolbar();

        Div card = PageCard.wrap(
                title,
                ViewStyles.buildSummaryCards(totalCountValue, incomeTotalValue, expenseTotalValue, balanceValue),
                buildToolbar(),
                statusLabel,
                grid
        );
        add(card);
        expand(card);
        card.getStyle().set(ViewStyles.PROP_MIN_HEIGHT, ViewStyles.VAL_ZERO);
        refreshAll();
    }

    private static User requireUser() {
        return AuthSession.currentUser().orElseThrow();
    }

    private void updateSummary(int userId) {
        int count = financeUseCase.getTransactionsByUser(userId).size();
        long income = financeUseCase.getTotalIncome(userId);
        long expense = financeUseCase.getTotalExpense(userId);
        long balance = financeUseCase.getBalance(userId);

        totalCountValue.setText(String.valueOf(count));
        incomeTotalValue.setText(presenter.formatAmount(income));
        expenseTotalValue.setText(presenter.formatAmount(expense));
        balanceValue.setText(presenter.formatAmount(balance));
    }

    private void configureGrid() {
        grid.addComponentColumn(FinanceView::createProofPreview)
                .setHeader("Bukti")
                .setKey("proof")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(false);

        grid.addColumn(Transaction::getId)
                .setHeader("ID")
                .setKey("id")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(true)
                .setComparator(Transaction::getId);

        grid.addColumn(Transaction::getDescription)
                .setHeader(LABEL_DESCRIPTION)
                .setKey("description")
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(Transaction::getDescription, String.CASE_INSENSITIVE_ORDER));

        grid.addColumn(tx -> presenter.formatAmount(tx.getAmount()))
                .setHeader("Jumlah")
                .setKey("amount")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingLong(Transaction::getAmount));

        grid.addComponentColumn(this::createTypeBadge)
                .setHeader("Tipe")
                .setKey("type")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(tx -> tx.getType().name()));

        grid.addColumn(presenter::formatCreatedAt)
                .setHeader("Waktu")
                .setKey("createdAt")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(Transaction::getCreatedAt));

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Tindakan")
                .setKey("tindakan")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(false);

        grid.setEmptyStateComponent(emptyState);
        grid.addItemDoubleClickListener(event -> openEditDialog(event.getItem()));
        grid.setSizeFull();
        GridStyles.apply(grid);
    }

    private static Component createProofPreview(Transaction transaction) {
        if (transaction.getProofPath() == null || transaction.getProofPath().isBlank()) {
            return new Span("-");
        }
        return ImagePreviewDialog.clickableThumbnail(
                "/uploads/" + transaction.getProofPath(),
                "Bukti Transaksi #" + transaction.getId(),
                "48px"
        );
    }

    private Span createTypeBadge(Transaction transaction) {
        Span badge = new Span(presenter.formatType(transaction.getType()));
        boolean income = transaction.isIncome();
        badge.getStyle()
                .set(ViewStyles.PROP_DISPLAY, "inline-block")
                .set(ViewStyles.PROP_PADDING, "0.2rem 0.6rem")
                .set(ViewStyles.PROP_BORDER_RADIUS, "999px")
                .set(ViewStyles.PROP_FONT_SIZE, "0.78rem")
                .set(ViewStyles.PROP_FONT_WEIGHT, "600")
                .set(ViewStyles.PROP_BACKGROUND, income ? "#DCFCE7" : "#FEE2E2")
                .set(ViewStyles.PROP_COLOR, income ? "#166534" : "#991B1B");
        return badge;
    }

    private HorizontalLayout createActionButtons(Transaction transaction) {
        Button deleteButton = new Button("Hapus", VaadinIcon.TRASH.create(), event -> openDeleteDialog(transaction));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        deleteButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);
        return new HorizontalLayout(deleteButton);
    }

    private void configureToolbar() {
        searchField.setPlaceholder("Cari keterangan...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.addKeyPressListener(Key.ENTER, event -> applyFilters());
        searchField.addValueChangeListener(event -> {
            if (event.isFromClient() && (event.getValue() == null || event.getValue().isBlank())) {
                applyFilters();
            }
        });

        typeFilter.setItems(TransactionTypeFilter.values());
        typeFilter.setItemLabelGenerator(TransactionTypeFilter::label);
        typeFilter.setValue(TransactionTypeFilter.ALL);
        typeFilter.setWidth("10.5rem");
        typeFilter.setAriaLabel("Filter tipe");
        typeFilter.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                applyFilters();
            }
        });

        sortSelect.setItems(SortOption.values());
        sortSelect.setItemLabelGenerator(SortOption::label);
        sortSelect.setValue(SortOption.NEWEST_FIRST);
        sortSelect.setWidth("12rem");
        sortSelect.setAriaLabel("Urutan");
        sortSelect.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                applyFilters();
            }
        });
    }

    private HorizontalLayout buildToolbar() {
        Button searchButton = new Button("Cari", VaadinIcon.SEARCH.create(), event -> applyFilters());
        searchButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        Button refreshButton = new Button("Muat Ulang", VaadinIcon.REFRESH.create(), event -> refreshAll());
        refreshButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        Button aiButton = new Button("Generate AI", VaadinIcon.MAGIC.create(), event -> openAiDialog());
        aiButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        aiButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        Button addIncomeButton = new Button("Tambah Pemasukan", VaadinIcon.PLUS.create(),
                event -> openAddDialog(TransactionType.INCOME));
        addIncomeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addIncomeButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        Button addExpenseButton = new Button("Tambah Pengeluaran", VaadinIcon.MINUS_CIRCLE.create(),
                event -> openAddDialog(TransactionType.EXPENSE));
        addExpenseButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        addExpenseButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        HorizontalLayout toolbar = new HorizontalLayout(
                searchField,
                typeFilter,
                sortSelect,
                searchButton,
                refreshButton,
                aiButton,
                addIncomeButton,
                addExpenseButton
        );
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.END);
        toolbar.setFlexGrow(1, searchField);
        return toolbar;
    }

    private void openAiDialog() {
        Dialog dialog = new Dialog();
        ProgressBar loadingBar = ViewStyles.createLoadingBar();

        Span hint = new Span("Masukkan deskripsi/rencana pemasukan atau pengeluaran yang ingin dicatat oleh AI:");
        hint.getStyle()
                .set(ViewStyles.PROP_FONT_SIZE, "0.875rem")
                .set(ViewStyles.PROP_COLOR, ViewStyles.COLOR_MUTED);

        TextArea promptArea = new TextArea("Instruksi Prompt");
        promptArea.setPlaceholder("Contoh: Gaji bulan ini 5 juta, bayar kos 1.2 juta, beli makan 500 ribu...");
        promptArea.setWidthFull();
        promptArea.setMinHeight("100px");

        Grid<AiGeneratedTransaction> previewGrid = new Grid<>(AiGeneratedTransaction.class, false);
        previewGrid.addColumn(AiGeneratedTransaction::description).setHeader(LABEL_DESCRIPTION).setFlexGrow(2);
        previewGrid.addColumn(item -> presenter.formatType(item.type())).setHeader("Tipe").setAutoWidth(true);
        previewGrid.addColumn(item -> presenter.formatAmount(item.amount())).setHeader("Jumlah").setAutoWidth(true);
        previewGrid.setWidthFull();
        previewGrid.setHeight("190px");
        previewGrid.setVisible(false);
        GridStyles.apply(previewGrid);

        Button applyButton = new Button("Tambahkan ke Catatan", VaadinIcon.PLUS_CIRCLE.create());
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyButton.setEnabled(false);

        Button cancelButton = new Button(ViewStyles.LABEL_CANCEL, VaadinIcon.CLOSE.create(), event -> dialog.close());

        Button generateButton = new Button("Proses AI", VaadinIcon.MAGIC.create());
        generateButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        generateButton.getStyle().set(ViewStyles.PROP_CURSOR, ViewStyles.VAL_POINTER);

        AiDialogControls controls = new AiDialogControls(
                dialog, promptArea, loadingBar, generateButton, applyButton, cancelButton, previewGrid, new ArrayList<>()
        );
        generateButton.addClickListener(event -> runAiGeneration(controls));
        applyButton.addClickListener(event -> saveAiGeneratedItems(controls));

        HorizontalLayout promptActionRow = new HorizontalLayout(generateButton);
        promptActionRow.setWidthFull();
        promptActionRow.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout body = new VerticalLayout(hint, promptArea, promptActionRow, previewGrid);
        body.setPadding(false);
        body.setSpacing(true);

        buildDialog(dialog, "Generate Catatan Keuangan dengan AI", DialogTone.PRIMARY, loadingBar, body, cancelButton, applyButton);
        dialog.setWidth("620px");
        dialog.open();
    }

    private void runAiGeneration(AiDialogControls controls) {
        String promptText = controls.promptArea().getValue();
        if (promptText == null || promptText.trim().isEmpty()) {
            ViewStyles.showError("Prompt instruksi tidak boleh kosong");
            return;
        }
        if (financeAiUseCase == null) {
            ViewStyles.showError("Layanan AI belum tersedia");
            return;
        }

        UI ui = UI.getCurrent();
        setAiBusy(controls, true);
        ui.setPollInterval(400);

        String prompt = promptText.trim();
        CompletableFuture.supplyAsync(() -> {
            try {
                return financeAiUseCase.generateTransactions(prompt);
            } catch (DomainException ex) {
                throw new CompletionException(ex);
            }
        }).whenComplete((result, error) -> ui.access(() -> {
            try {
                if (error != null) {
                    Throwable cause = unwrapCause(error);
                    ViewStyles.showError("Gagal menghasilkan konten AI: " + cause.getMessage());
                    return;
                }
                controls.generatedItems().clear();
                controls.generatedItems().addAll(result);
                controls.previewGrid().setItems(controls.generatedItems());
                controls.previewGrid().setVisible(true);
                ViewStyles.showSuccess("AI berhasil menghasilkan " + controls.generatedItems().size() + " catatan transaksi");
            } finally {
                setAiBusy(controls, false);
                controls.applyButton().setEnabled(!controls.generatedItems().isEmpty());
                ui.setPollInterval(-1);
            }
        }));
    }

    private static Throwable unwrapCause(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private void saveAiGeneratedItems(AiDialogControls controls) {
        if (controls.generatedItems().isEmpty()) {
            ViewStyles.showError("Tidak ada transaksi yang dapat ditambahkan");
            return;
        }
        controls.loadingBar().setVisible(true);
        controls.applyButton().setEnabled(false);
        controls.generateButton().setEnabled(false);
        controls.cancelButton().setEnabled(false);
        try {
            User user = requireUser();
            financeAiUseCase.saveGeneratedTransactions(user.getId(), controls.generatedItems());
            ViewStyles.showSuccess("Berhasil menambahkan " + controls.generatedItems().size() + " transaksi dari AI");
            controls.dialog().close();
            refreshAll();
        } catch (ValidationException ex) {
            ViewStyles.showError(presenter.showValidationError(ex.getMessage()));
            controls.loadingBar().setVisible(false);
            controls.applyButton().setEnabled(true);
            controls.generateButton().setEnabled(true);
            controls.cancelButton().setEnabled(true);
        }
    }

    private void openAddDialog(TransactionType type) {
        TextField descriptionField = new TextField(LABEL_DESCRIPTION);
        descriptionField.setPlaceholder("Contoh: Gaji bulanan / Beli makan siang");
        descriptionField.setWidthFull();

        TextField amountField = new TextField(LABEL_AMOUNT_INPUT);
        amountField.setPlaceholder("Contoh: 500000");
        amountField.setWidthFull();

        String modalTitle = type == TransactionType.INCOME ? "Tambah Pemasukan" : "Tambah Pengeluaran";
        openTransactionFormDialog(
                modalTitle,
                amountField,
                "Bukti transaksi (opsional)",
                (amount, proofPath) -> {
                    Transaction created = financeUseCase.addTransaction(
                            requireUser().getId(),
                            descriptionField.getValue(),
                            amount,
                            type,
                            proofPath
                    );
                    ViewStyles.showSuccess(presenter.showAddSuccess(created));
                },
                descriptionField,
                amountField
        );
    }

    private void openEditDialog(Transaction transaction) {
        TextField descriptionField = new TextField(LABEL_DESCRIPTION);
        descriptionField.setValue(transaction.getDescription());
        descriptionField.setWidthFull();

        TextField amountField = new TextField(LABEL_AMOUNT_INPUT);
        amountField.setValue(String.valueOf(transaction.getAmount()));
        amountField.setWidthFull();

        Select<TransactionType> typeSelect = new Select<>();
        typeSelect.setLabel("Tipe Transaksi");
        typeSelect.setItems(TransactionType.values());
        typeSelect.setItemLabelGenerator(TransactionType::label);
        typeSelect.setValue(transaction.getType());
        typeSelect.setWidthFull();

        openTransactionFormDialog(
                "Mengubah Transaksi #" + transaction.getId(),
                amountField,
                "Ganti bukti transaksi (opsional)",
                (amount, proofPath) -> {
                    financeUseCase.updateTransactionForUser(
                            requireUser().getId(),
                            transaction.getId(),
                            descriptionField.getValue(),
                            amount,
                            typeSelect.getValue(),
                            proofPath,
                            false
                    );
                    ViewStyles.showSuccess(presenter.showUpdateSuccess());
                },
                descriptionField,
                amountField,
                typeSelect
        );
    }

    private void openTransactionFormDialog(
            String modalTitle,
            TextField amountField,
            String uploadLabel,
            TransactionSaveAction saveAction,
            Component... fields
    ) {
        AtomicReference<String> pendingProofName = new AtomicReference<>();
        AtomicReference<byte[]> pendingProofData = new AtomicReference<>();
        Upload upload = imageUpload(pendingProofName, pendingProofData);
        Span uploadHint = new Span(uploadLabel);

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = ViewStyles.createLoadingBar();

        Button saveButton = new Button("Simpan", VaadinIcon.CHECK.create());
        saveButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            saveButton.setEnabled(false);
            try {
                long amount = financeUseCase.parseAmount(amountField.getValue());
                String proofPath = storePendingProof(pendingProofName, pendingProofData);
                saveAction.execute(amount, proofPath);
                dialog.close();
                refreshAll();
            } catch (EntityNotFoundException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                ViewStyles.showError(presenter.showUpdateFailed(ex.getId()));
            } catch (ValidationException | IOException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                ViewStyles.showError(presenter.showValidationError(ex.getMessage()));
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button(ViewStyles.LABEL_CANCEL, VaadinIcon.CLOSE.create(), event -> dialog.close());
        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(true);
        body.add(fields);
        body.add(uploadHint, upload);
        buildDialog(dialog, modalTitle, DialogTone.PRIMARY, loadingBar, body, cancelButton, saveButton);
        dialog.open();
    }

    private String storePendingProof(
            AtomicReference<String> pendingName,
            AtomicReference<byte[]> pendingData
    ) throws IOException {
        String filename = pendingName.get();
        byte[] data = pendingData.get();
        if (filename != null && !filename.isBlank() && data != null) {
            try (InputStream content = new ByteArrayInputStream(data)) {
                return fileStorage.store(PROOF_FOLDER, filename, content);
            }
        }
        return null;
    }

    private void openDeleteDialog(Transaction transaction) {
        Paragraph message = new Paragraph(
                "Yakin ingin menghapus transaksi \"" + transaction.getDescription()
                        + "\" (" + presenter.formatAmount(transaction.getAmount()) + ", ID " + transaction.getId() + ")?"
        );
        message.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO);

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = ViewStyles.createLoadingBar();

        Button confirmButton = new Button("Hapus", VaadinIcon.TRASH.create());
        confirmButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            confirmButton.setEnabled(false);
            deleteTransaction(transaction);
            dialog.close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelButton = new Button(ViewStyles.LABEL_CANCEL, VaadinIcon.CLOSE.create(), event -> dialog.close());
        buildDialog(dialog, "Menghapus Transaksi", DialogTone.DANGER, loadingBar, message, cancelButton, confirmButton);
        dialog.open();
    }

    private enum DialogTone {
        PRIMARY,
        DANGER
    }

    private static void buildDialog(
            Dialog dialog,
            String title,
            DialogTone tone,
            ProgressBar loadingBar,
            Component bodyContent,
            Button... footerButtons
    ) {
        dialog.setWidth("500px");
        dialog.setMaxWidth("95vw");
        dialog.setResizable(false);
        dialog.addClassName("cashflow-form-dialog");

        String headerBackground = tone == DialogTone.DANGER ? ViewStyles.COLOR_DANGER : ViewStyles.COLOR_PRIMARY;
        Div header = ViewStyles.createDialogHeader(title, headerBackground);

        loadingBar.getStyle().set(ViewStyles.PROP_MARGIN, ViewStyles.VAL_ZERO).set(ViewStyles.PROP_HEIGHT, "4px");

        Div body = new Div(bodyContent);
        body.getStyle()
                .set(ViewStyles.PROP_BACKGROUND, ViewStyles.COLOR_WHITE)
                .set(ViewStyles.PROP_PADDING, "1.25rem 1.5rem")
                .set(ViewStyles.PROP_OVERFLOW, ViewStyles.VAL_HIDDEN)
                .set(ViewStyles.PROP_BOX_SIZING, ViewStyles.VAL_BORDER_BOX)
                .set(ViewStyles.PROP_WIDTH, ViewStyles.VAL_FULL);

        Div footer = ViewStyles.createDialogFooter("0.75rem", footerButtons);
        ViewStyles.mountDialogShell(dialog, header, loadingBar, body, footer);
    }

    private void deleteTransaction(Transaction transaction) {
        try {
            financeUseCase.removeTransactionForUser(requireUser().getId(), transaction.getId());
            ViewStyles.showSuccess(presenter.showRemoveSuccess());
            refreshAll();
        } catch (EntityNotFoundException ex) {
            ViewStyles.showError(presenter.showRemoveFailed(ex.getId()));
        } catch (ValidationException ex) {
            ViewStyles.showError(presenter.showValidationError(ex.getMessage()));
        }
    }

    private void applyFilters() {
        String keyword = searchField.getValue() == null ? "" : searchField.getValue().trim();
        TransactionTypeFilter filter = typeFilter.getValue() == null ? TransactionTypeFilter.ALL : typeFilter.getValue();
        SortOption sort = sortSelect.getValue() == null ? SortOption.NEWEST_FIRST : sortSelect.getValue();

        int userId = requireUser().getId();
        List<Transaction> results = financeUseCase.listTransactions(userId, keyword, filter, sort);

        String title = "Daftar Transaksi";
        if (!keyword.isBlank()) {
            title = "Hasil: \"" + keyword + "\"";
        }
        if (filter != TransactionTypeFilter.ALL) {
            title = title + " · " + filter.label();
        }
        title = title + " · " + sort.label();

        if (results.isEmpty()) {
            emptyState.setText(keyword.isBlank() && filter == TransactionTypeFilter.ALL
                    ? MSG_NO_DATA
                    : "- Transaksi tidak ditemukan!");
        } else {
            emptyState.setText(MSG_NO_DATA);
        }
        refreshGrid(results, title, userId);
    }

    private void refreshAll() {
        searchField.clear();
        typeFilter.setValue(TransactionTypeFilter.ALL);
        sortSelect.setValue(SortOption.NEWEST_FIRST);
        emptyState.setText(MSG_NO_DATA);
        applyFilters();
    }

    private void refreshGrid(List<Transaction> transactions, String title, int userId) {
        grid.setItems(transactions);
        updateSummary(userId);
        if (transactions.isEmpty()) {
            statusLabel.setText(title + " — " + emptyState.getText().replaceFirst("^- ", ""));
        } else {
            statusLabel.setText(title + " · " + presenter.formatBalance(financeUseCase.getBalance(userId)));
        }
    }

    private static Upload imageUpload(AtomicReference<String> pendingName, AtomicReference<byte[]> pendingData) {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            pendingName.set(metadata.fileName());
            pendingData.set(data);
        }));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        return upload;
    }

    private static void setAiBusy(AiDialogControls controls, boolean busy) {
        controls.loadingBar().setVisible(busy);
        controls.generateButton().setEnabled(!busy);
        controls.generateButton().setText(busy ? "Memproses..." : "Proses AI");
        controls.applyButton().setEnabled(false);
        controls.cancelButton().setEnabled(!busy);
        controls.promptArea().setReadOnly(busy);
    }
}
