package chabu.tester;

import java.io.IOException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.wb.swt.ResourceManager;

import chabu.tester.data.CmdChannelCreateStat;
import chabu.tester.data.CmdConnectionAwait;
import chabu.tester.data.CmdConnectionClose;
import chabu.tester.data.CmdConnectionConnect;
import chabu.tester.data.CmdDutApplicationClose;
import chabu.tester.data.CmdDutConnect;
import chabu.tester.data.CmdDutDisconnect;
import chabu.tester.data.CmdSetupActivate;
import chabu.tester.data.CmdSetupChannelAdd;
import chabu.tester.dlg.ConfigureTest;
import chabu.tester.dlg.ConfigureTestData;


public class ChabuTesterAppWnd extends ApplicationWindow {
	private Table table;
	private Action actionStartTest;
	private Action actionInfo;
	ChabuTestNw nw;
	private Action actionPlay;

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
		
		nw = new ChabuTestNw("Tester");
	}

	/**
	 * Create contents of the application window.
	 * @param parent
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		{
			TableViewer tableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
			table = tableViewer.getTable();
			table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		}

		return container;
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
			actionPlay = new Action("New Action") {				public void run() {
					System.out.println("Start:");
					Thread actions = new Thread( ()->{
						try {
							long st = System.nanoTime();
							final long MSEC = 1_000_000L;
							final long SEC = 1_000*MSEC;
							nw.addCommand( DutId.A, new CmdDutConnect( st, "localhost", 2300 ));
							nw.addCommand( DutId.B, new CmdDutConnect( st, "localhost", 2310 ));
							nw.flush(DutId.ALL);
							nw.addCommand( DutId.ALL, new CmdSetupChannelAdd( st, 0, 10 ));
							nw.addCommand( DutId.ALL, new CmdSetupActivate( st, true, 1000 ));
							st += 400*MSEC;
							nw.addCommand( DutId.A, new CmdConnectionAwait( st, 2301 ));
							nw.addCommand( DutId.B, new CmdConnectionConnect( st, "localhost", 2301 ));
							nw.flush(DutId.ALL);
							System.out.println("A+B connected");
							synchronized(this){
								for( int i = 0; i < 3; i++ ){
									st += 600*MSEC;
									nw.addCommand( DutId.A, new CmdChannelCreateStat( st, 1 ));
								}
								wait(4000);
							}
							nw.addCommand( DutId.ALL, new CmdConnectionClose( System.nanoTime() ) );
							nw.addCommand( DutId.ALL, new CmdDutApplicationClose( System.nanoTime() ) );
							nw.flush(DutId.ALL);
							System.out.println("disconnecting");
							nw.addCommand( DutId.ALL, new CmdDutDisconnect( System.nanoTime() ) );
							System.out.println("--- Actions finished ---");
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
		return new Point(450, 300);
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
