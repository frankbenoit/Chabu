package chabu.tester;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class ConfigureTestBasic extends WizardPage {

	private ConfigureTestData data;
	private DateTime dateTime;
	private Spinner spinnerNumChannels;
	private Combo comboTestType;

	/**
	 * Create the wizard.
	 * @param data 
	 */
	public ConfigureTestBasic(ConfigureTestData data) {
		super("Configure-First");
		this.data = data;
		setTitle("Configure Test Type");
		setDescription("Select the basic detail parameter for the test to start");
	}

	/**
	 * Create contents of the wizard.
	 * @param parent
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);

		setControl(container);
		container.setLayout(new GridLayout(2, false));
		
		Label lblTestType = new Label(container, SWT.NONE);
		lblTestType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lblTestType.setText("Test Type:");
		
		comboTestType = new Combo(container, SWT.READ_ONLY);
		comboTestType.setItems(new String[] {"Unidirectional", "Bidirectional", "Bidirection, one with recv interruptions", "Bidirection, random"});
		comboTestType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboTestType.select( data.testType );
		
		Label lblDuration = new Label(container, SWT.NONE);
		lblDuration.setText("Duration:");
		
		dateTime = new DateTime(container, SWT.BORDER | SWT.TIME | SWT.LONG);
		dateTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dateTime.setHours  ( 0 );
		dateTime.setMinutes( 0 );
		dateTime.setSeconds( 20 );
		
		Label lblOfChannels = new Label(container, SWT.NONE);
		lblOfChannels.setText("\u2116 of Channels:");
		
		spinnerNumChannels = new Spinner(container, SWT.BORDER);
		spinnerNumChannels.setMaximum(255);
		spinnerNumChannels.setMinimum(1);
		spinnerNumChannels.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		spinnerNumChannels.setSelection( data.numberOfChannels );
		
		Label lblMaxPayloadSize = new Label(container, SWT.NONE);
		lblMaxPayloadSize.setText("Max Payload Size:");
		
		Spinner spinnerPayloadSize = new Spinner(container, SWT.BORDER);
		spinnerPayloadSize.setPageIncrement(25);
		spinnerPayloadSize.setMaximum(100000);
		spinnerPayloadSize.setMinimum(1);
		spinnerPayloadSize.setSelection(1400);
		spinnerPayloadSize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setText("A \u21E8 B \u2116 of RxBlocks:");
		
		Spinner spinnerBRxBlocks = new Spinner(container, SWT.BORDER);
		spinnerBRxBlocks.setMaximum(1000);
		spinnerBRxBlocks.setMinimum(1);
		spinnerBRxBlocks.setSelection(1);
		spinnerBRxBlocks.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		Label lblOfRxblocks = new Label(container, SWT.NONE);
		lblOfRxblocks.setText("A \u21E6 B \u2116 of RxBlocks:");
		
		Spinner spinnerARxBlocks = new Spinner(container, SWT.BORDER);
		spinnerARxBlocks.setMaximum(1000);
		spinnerARxBlocks.setMinimum(1);
		spinnerARxBlocks.setSelection(1);
		spinnerARxBlocks.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
	}

	public void retrieveData() {
		data.testType         = comboTestType.getSelectionIndex();
		data.numberOfChannels = spinnerNumChannels.getSelection();
		data.durationHours    = dateTime.getHours  ();
		data.durationMinutes  = dateTime.getMinutes();
		data.durationSeconds  = dateTime.getSeconds();
	}

	
}
