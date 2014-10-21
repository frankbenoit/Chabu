package chabu.tester;

import org.eclipse.jface.wizard.Wizard;

public class ConfigureTest extends Wizard {

	private ConfigureTestBasic    configureTestBasic;
	private ConfigureTestRxBlocks configureTestRxBlocks;

	public ConfigureTest(ConfigureTestData data) {
		setWindowTitle("Configure Test");
		configureTestBasic    = new ConfigureTestBasic(data);
		configureTestRxBlocks = new ConfigureTestRxBlocks(data);
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		addPage(configureTestBasic);
		addPage(configureTestRxBlocks);
	}

	@Override
	public boolean performFinish() {
		configureTestBasic.retrieveData();
		configureTestRxBlocks.retrieveData();
		return true;
	}

}
