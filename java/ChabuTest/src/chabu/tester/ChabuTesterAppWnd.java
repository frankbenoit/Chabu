package chabu.tester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.ResourceManager;
import org.swtchart.Chart;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries.SeriesType;

import chabu.tester.dlg.ConfigureTest;
import chabu.tester.dlg.ConfigureTestData;


public class ChabuTesterAppWnd extends ApplicationWindow {
	private Action actionStartTest;
	private Action actionInfo;
	ChabuTestNw nw;
	private Action actionPlay;
	private Chart chart;

	private static final String PLOT_TX = "TX";
	private static final String PLOT_RX = "RX";
	
	private ListViewer listViewer;

    /**
	 * Create the application window,
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public ChabuTesterAppWnd() throws IOException, InterruptedException {
		super(null);
		setBlockOnOpen(true);
		createActions();
		addCoolBar(SWT.FLAT);
		addMenuBar();
		addStatusLine();
	}

	@Override
	public int open() {
		try {
			nw = new ChabuTestNw("Tester");
			int res = super.open();
			nw.shutDown();
			return res;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return Window.CANCEL;
		}
	}
	
	/**
	 * Create contents of the application window.
	 * @param parent
	 */
	@Override
	protected Control createContents(Composite parent) {
		SashForm sf = new SashForm(parent, SWT.NONE);
		
		//container.setLayout(new GridLayout(2, false));
		{
			listViewer = new ListViewer(sf, SWT.BORDER | SWT.V_SCROLL);
			listViewer.setContentProvider( new ArrayContentProvider() );
			listViewer.setLabelProvider( new LabelProvider() {
				@Override
				public String getText(Object element) {
					StreamStats cs = (StreamStats)element;
					return String.format("[%s] %s -> %s", cs.tx.channelId, cs.tx.dutId, cs.rx.dutId );
				}
			});
			listViewer.addSelectionChangedListener( this::plotSelection );
			List list = listViewer.getList();
			list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		}
		
		chart = new Chart(sf, SWT.NONE);
		chart.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		sf.setWeights(new int[] {1, 3});
		{
	        // set titles
	        chart.getTitle().setText("Bandwidth of channel");
	        chart.getAxisSet().getXAxis(0).getTitle().setText("Time [ms]");
	        chart.getAxisSet().getYAxis(0).getTitle().setText("Data Rate [kB/s]");

	        // create line series
	        {
				ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, PLOT_TX);
		        lineSeries.setLineColor( Display.getCurrent().getSystemColor(SWT.COLOR_RED ));
		        lineSeries.setSymbolSize(2);
	        }
	        {
	        	ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, PLOT_RX);
	        	lineSeries.setLineColor( Display.getCurrent().getSystemColor(SWT.COLOR_BLUE ));
	        	lineSeries.setSymbolSize(2);
	        }

	        // adjust the axis range
	        chart.getAxisSet().adjustRange();

		}
		getShell().getDisplay().timerExec( 2000, this::updateStats );
		return sf;
	}

	public void updateStats(){
		fillData();
		updateChart();
		getShell().getDisplay().timerExec( 400, this::updateStats );
	}
	
	public void fillData() {
		final ArrayList<StreamStats> statsValues = nw.getStatsValues();
		Display.getDefault().syncExec( ()->{
			if( listViewer.getList().getItemCount() != statsValues.size() ){
				listViewer.setInput( statsValues.toArray() );
			}
		});
	}
	public void plotSelection(SelectionChangedEvent event) {
		updateChart();
	}

	private void updateChart() {
		if( listViewer.getSelection().isEmpty() ){
			return;
		}
		IStructuredSelection sel = (IStructuredSelection)listViewer.getSelection();
		StreamStats cd = (StreamStats)sel.getFirstElement();
//		System.out.printf("[%s] %s -> %s\n", cd.rx.channelId, cd.tx.dutId, cd.rx.dutId );
		{
			ILineSeries ls = (ILineSeries) chart.getSeriesSet().getSeries(PLOT_TX);
			ls.setXSeries( Arrays.copyOf( cd.tx.time, cd.tx.count ));
			ls.setYSeries( Arrays.copyOf( cd.tx.rate, cd.tx.count ));
		}
		{
			ILineSeries ls = (ILineSeries) chart.getSeriesSet().getSeries(PLOT_RX);
			ls.setXSeries( Arrays.copyOf( cd.rx.time, cd.rx.count ));
			ls.setYSeries( Arrays.copyOf( cd.rx.rate, cd.rx.count ));
		}
		chart.redraw();
		chart.getAxisSet().adjustRange();
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		// Create the actions
		{
			actionStartTest = new Action("&Start Test" ) {				public void run() {
					try{
						ConfigureTestData data = new ConfigureTestData();
						WizardDialog dlg = new WizardDialog(getShell(), new ConfigureTest(data));
						dlg.setBlockOnOpen(true);
						int res = dlg.open();
						if( res != Window.OK ){
							System.out.println("canceled");
							return;
						}
						System.out.printf("%d %d %d:%02d:%02d\n", data.testType, data.numberOfChannels, data.durationHours, data.durationMinutes, data.durationSeconds );
//						
//						getStatusLineManager().setCancelEnabled(true);
//						ModalContext.run( new IRunnableWithProgress() {
//							public void run(IProgressMonitor mon) throws InvocationTargetException, InterruptedException {
//								mon.beginTask("Task", 20);
//								synchronized(this){
//									for( int i = 0; i < 20; i++ ){
//										wait(200);
//										mon.worked(1);
//									}
//								}
//								mon.done();
//							}
//						}, true, getStatusLineManager().getProgressMonitor(), getShell().getDisplay());
					}
					catch( Exception e ){
						e.printStackTrace();
					}
				}
			};
			actionStartTest.setImageDescriptor(ResourceManager.getImageDescriptor(ChabuTesterAppWnd.class, "running-man-16.png"));
			actionStartTest.setAccelerator(SWT.F9);
		}
		{
			actionInfo = new Action("A&bout") {				public void run() {
					MessageDialog.openInformation(getShell(), "Chabu Tester - About ...", 
							"Chabu - the channel bundle\n"
							+ "Test application, to start 2 sub-processes that communicate with each other.\n"
							+ "Version 1.0");
				}
			};
			actionInfo.setImageDescriptor(ResourceManager.getImageDescriptor(ChabuTesterAppWnd.class, "/chabu/tester/about-16.png"));
		}
		{
			actionPlay = new Action("New Action") {
				public void run() {
					System.out.println("Start:");
					Thread actions = new Thread( ()->{
						try {
							ITestTask task = new SimpleTest();
							task.task( nw );
							fillData();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}, "actions" );
					actions.start();
				}
			};
			actionPlay.setAccelerator(SWT.F8);
			actionPlay.setImageDescriptor(ResourceManager.getImageDescriptor(ChabuTesterAppWnd.class, "/chabu/tester/play-16.png"));
		}
	}

	/**
	 * Create the coolbar manager.
	 * @return the coolbar manager
	 */
	@Override
	protected CoolBarManager createCoolBarManager(int style) {
		CoolBarManager coolBarManager = new CoolBarManager(style);
		{
			ToolBarManager toolBarManager = new ToolBarManager();
			coolBarManager.add(toolBarManager);
			toolBarManager.add(actionPlay);
			toolBarManager.add(actionStartTest);
		}
		{
			ToolBarManager toolBarManager = new ToolBarManager();
			coolBarManager.add(toolBarManager);
			toolBarManager.add(actionInfo);
		}
		return coolBarManager;
	}

	/**
	 * Create the status line manager.
	 * @return the status line manager
	 */
	@Override
	protected StatusLineManager createStatusLineManager() {
		StatusLineManager slm = new StatusLineManager();
//		slm.setErrorMessage("E1");
//		slm.setMessage("M1");
		return slm;
	}

	@Override
	protected MenuManager createMenuManager() {
		MenuManager mm = new MenuManager("menu");
		{
			MenuManager menuManager = new MenuManager("File");
			mm.add(menuManager);
			menuManager.add(actionPlay);
			menuManager.add(actionStartTest);
		}
		{
			MenuManager menuManager = new MenuManager("Help");
			mm.add(menuManager);
			menuManager.add(actionInfo);
		}
		return mm;
	}
	
	/**
	 * Configure the shell.
	 * @param newShell
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Chabu Tester");
	}

	/**
	 * Return the initial size of the window.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(761, 596);
	}

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String args[]) {
		mainInternal();
		System.exit(0);
	}

	public static void mainInternal() {
		try {
			ChabuTesterAppWnd wnd = new ChabuTesterAppWnd();
			wnd.setBlockOnOpen(true);
			wnd.open();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Display.getCurrent().dispose();
		}
	}

	public static Thread mainInternalCreateThread( String threadName) {
		Thread res = new Thread( ()->{
			ChabuTesterAppWnd.mainInternal();
		}, threadName+"GUI");
		res.start();
		return res;
	}
}
