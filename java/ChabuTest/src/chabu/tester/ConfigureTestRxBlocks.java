package chabu.tester;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class ConfigureTestRxBlocks extends WizardPage {

	private ConfigureTestData data;
	private Table table;

	/**
	 * Create the wizard.
	 */
	public ConfigureTestRxBlocks(ConfigureTestData data) {
		super("wizardPage");
		this.data = data;
		setTitle("Wizard Page title");
		setDescription("Wizard Page description");
	}

	/**
	 * Create contents of the wizard.
	 * @param parent
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);

		setControl(container);
		container.setLayout(new GridLayout(1, false));
		
		TableViewer tableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
		table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnChannel = tableViewerColumn.getColumn();
		tblclmnChannel.setWidth(100);
		tblclmnChannel.setText("Channel");
		
		TableViewerColumn tableViewerColumnAB = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnAB = tableViewerColumnAB.getColumn();
		tblclmnAB.setWidth(200);
		tblclmnAB.setText("A \u21E8 B RxBlocks");
		
		TableViewerColumn tableViewerColumnBA = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnBA = tableViewerColumnBA.getColumn();
		tblclmnBA.setWidth(200);
		tblclmnBA.setText("A \u21E6 B RxBlocks");
	}

	public void retrieveData() {
		// TODO Auto-generated method stub
		
	}

}
