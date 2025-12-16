package org.ruyisdk.news.views;

import java.util.regex.Pattern;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.ruyisdk.news.Activator;
import org.ruyisdk.news.model.NewsItem;
import org.ruyisdk.news.viewmodel.NewsDetailsViewModel;
import org.ruyisdk.news.viewmodel.NewsListViewModel;

/**
 * View showing the news list and details.
 */
public class NewsView extends ViewPart {
    public static final String ID = "org.ruyisdk.news.views.NewsView";

    private Composite topComposite;
    private Composite middleComposite;
    private Composite bottomComposite;

    private Text searchTextBox;
    private Button unreadCheckBox;

    private TableViewer tableViewer;
    private Text detailTextBox;

    private Button updateButton;
    private Label updateInfoLabel;
    private Button hideDetailsButton;

    private DataBindingContext dbc;
    private NewsDetailsViewModel newsDetailsViewModel;
    private NewsListViewModel newsListViewModel;

    @Override
    public void createPartControl(Composite parent) {
        dbc = new DataBindingContext();
        newsDetailsViewModel = new NewsDetailsViewModel(Activator.getDefault().getService());
        newsListViewModel = new NewsListViewModel(Activator.getDefault().getService());

        createLayouts(parent);
        addControls();
        registerEvents();

        // initialize states
        toggleDetailControls(false);
    }

    private void createLayouts(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        topComposite = new Composite(parent, SWT.NONE);
        topComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        topComposite.setLayout(new GridLayout(3, false));

        middleComposite = new Composite(parent, SWT.NONE);
        middleComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));
        middleComposite.setLayout(new GridLayout(1, false));

        bottomComposite = new Composite(parent, SWT.NONE);
        bottomComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        bottomComposite.setLayout(new GridLayout(3, false));

    }

    private void addControls() {
        new Label(topComposite, SWT.NULL).setText("Search:");

        searchTextBox = new Text(topComposite, SWT.SINGLE | SWT.BORDER);
        searchTextBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        searchTextBox.setMessage("Title or ID");

        unreadCheckBox = new Button(topComposite, SWT.CHECK);
        unreadCheckBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        unreadCheckBox.setText("Unread Only");

        // table
        {
            var tableComposite = new Composite(middleComposite, SWT.NONE);
            {
                final var gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
                gridData.heightHint = 100;
                tableComposite.setLayoutData(gridData);
            }

            tableViewer = new TableViewer(tableComposite, SWT.BORDER);

            final var tableColumnLayout = new TableColumnLayout();
            // TODO: use Tuple and loop to eliminate duplicates
            {
                final var column = new TableViewerColumn(tableViewer, SWT.CENTER);
                column.getColumn().setText("Unread");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(5, 60));
            }
            {
                final var column = new TableViewerColumn(tableViewer, SWT.NONE);
                column.getColumn().setText("Title");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(100, 20));
            }
            {
                final var column = new TableViewerColumn(tableViewer, SWT.NONE);
                column.getColumn().setText("ID");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(50, 20));
            }
            tableComposite.setLayout(tableColumnLayout);

            tableViewer.getTable().setHeaderVisible(true);
            tableViewer.getTable().setLinesVisible(true);

            final var contentProvider = new ObservableListContentProvider<NewsItem>();
            tableViewer.setContentProvider(contentProvider);
            // do NOT use ViewerSupport.bind() due to customized cell content.
            final var labelProvider =
                            new ObservableMapLabelProvider(Properties.observeEach(contentProvider.getKnownElements(),
                                            BeanProperties.values(NewsItem.class, "unread", "title", "id"))) {
                                @Override
                                public String getColumnText(Object element, int columnIndex) {
                                    if (columnIndex == 0 /* unread */) {
                                        return ((NewsItem) element).getUnread() ? "*" : "";
                                    }
                                    return super.getColumnText(element, columnIndex);
                                }
                            };
            tableViewer.setLabelProvider(labelProvider);

            tableViewer.setInput(newsListViewModel.getNewsList());
        }

        detailTextBox = new Text(middleComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        final var gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
        gridData.heightHint = 100;
        detailTextBox.setLayoutData(gridData);

        updateButton = new Button(bottomComposite, SWT.PUSH);
        updateButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        updateButton.setText("Update");

        updateInfoLabel = new Label(bottomComposite, SWT.NULL);
        updateInfoLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        updateInfoLabel.setText("<updateInfo>");

        hideDetailsButton = new Button(bottomComposite, SWT.PUSH);
        hideDetailsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL));
        hideDetailsButton.setText("Hide Details");
    }

    private void registerEvents() {
        new ComputedValue<NewsListViewerFilter>() {
            @Override
            protected NewsListViewerFilter calculate() {
                final var newsFilter = new NewsListViewerFilter();
                newsFilter.setPattern(WidgetProperties.text(SWT.Modify).observe(searchTextBox).getValue());
                newsFilter.setOnlyUnread(WidgetProperties.buttonSelection().observe(unreadCheckBox).getValue());
                return newsFilter;
            }
        }.addValueChangeListener(e -> {
            tableViewer.resetFilters();
            tableViewer.addFilter(e.diff.getNewValue());
        });

        tableViewer.getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if (tableViewer.getTable().getItem(new Point(e.x, e.y)) == null) {
                    tableViewer.getTable().deselectAll();
                    tableViewer.setSelection(StructuredSelection.EMPTY);
                }
            }
        });

        updateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateButton.setEnabled(false);
                newsListViewModel.onUpdateNewsList();
            }
        });

        hideDetailsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tableViewer.setSelection(StructuredSelection.EMPTY);
            }
        });

        {
            final var selectionObservable = ViewerProperties.singleSelection(NewsItem.class).observe(tableViewer);
            selectionObservable.addValueChangeListener(e -> {
                final var selected = e.diff.getNewValue();
                if (selected != null) {
                    newsDetailsViewModel.onAcquireNewsDetails(selected);
                    toggleDetailControls(true);
                } else {
                    toggleDetailControls(false);
                }
            });
            dbc.bindValue(WidgetProperties.text().observe(detailTextBox), BeanProperties
                            .value(NewsItem.class, "details", String.class).observeDetail(selectionObservable));
        }

        {
            final var updateInfoObservable = WidgetProperties.text().observe(updateInfoLabel);
            updateInfoObservable.addValueChangeListener(e -> {
                updateInfoLabel.requestLayout();
            });
            dbc.bindValue(updateInfoObservable, BeanProperties.value(NewsListViewModel.class, "infoText", String.class)
                            .observe(newsListViewModel));
            dbc.bindValue(WidgetProperties.enabled().observe(updateButton), new ComputedValue<Boolean>() {
                @Override
                protected Boolean calculate() {
                    return !BeanProperties.value(NewsListViewModel.class, "fetching", Boolean.class)
                                    .observe(newsListViewModel).getValue();
                }
            });
        }
    }

    private void toggleDetailControls(Boolean isShow) {
        hideDetailsButton.setEnabled(isShow);

        // do not use setVisible() here.
        ((GridData) detailTextBox.getLayoutData()).exclude = !isShow;
        detailTextBox.requestLayout();
    }

    static class NewsListViewerFilter extends ViewerFilter {
        private String pattern = "";
        private Boolean onlyUnread = false;

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public void setOnlyUnread(Boolean onlyUnread) {
            this.onlyUnread = onlyUnread;
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            final var newsItem = (NewsItem) element;

            final Pattern compiledPattern = Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE);
            final boolean matchesPattern = compiledPattern.matcher(newsItem.getTitle()).find()
                            || compiledPattern.matcher(newsItem.getId()).find();
            final boolean onlyUnreadValue = Boolean.TRUE.equals(onlyUnread);
            final boolean matchesUnread = !onlyUnreadValue || newsItem.getUnread();
            return matchesPattern && matchesUnread;
        }
    }

    @Override
    public void setFocus() {}
}
