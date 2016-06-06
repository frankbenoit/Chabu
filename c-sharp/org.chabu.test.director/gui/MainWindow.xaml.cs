using System;
using System.Collections.ObjectModel;
using System.Net;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Documents;
using org.chabu.test.director.prot;
using org.chabu.test.director.Properties;
using org.chabu.test.director.tests;
using StructureMap;

namespace org.chabu.test.director.gui
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        private readonly ObservableCollection<ITest> _tests = new ObservableCollection<ITest>();
        private readonly ILogContainer _logContainer;

        public MainWindow(ILogContainer logContainer)
        {
            _logContainer = logContainer;
            _logContainer.Updated += LoggingUpdated;
            InitializeComponent();
            MenuMru.MenuClick += (s, e) => FileOpenCore(e.Filepath);

            ITest selectedTest = null;
            var testFactory = new TestFactory();
            foreach (var test in testFactory.GetList())
            {
                _tests.Add(test);
                if (test.Name.Equals(Settings.Default.SelectedTest))
                {
                    selectedTest = test;
                }
            }
            CmbTests.DataContext = _tests;

            if (selectedTest != null)
            {
                CmbTests.SelectedItem = selectedTest;
                BtnStart.Focus();
            }
            else
            {
                CmbTests.Focus();
                CmbTests.IsDropDownOpen = true;
                BtnStart.IsEnabled = false;
            }


        }

        private void FileOpenCore(string filepath)
        {
            MessageBox.Show(filepath);
        }

        private void MenuItem_Click(object sender, RoutedEventArgs e)
        {

        }

        private void ConfigureChannel_Click(object sender, RoutedEventArgs e)
        {
            
            var dlg = new ConfigureChannels();
            dlg.ShowDialog();
        }

        private void MenuFileOpen(object sender, RoutedEventArgs e)
        {
            // Configure open file dialog box
            Microsoft.Win32.OpenFileDialog dlg = new Microsoft.Win32.OpenFileDialog();
            //dlg.FileName = ""; // Default file name
            dlg.DefaultExt = ".tdir"; // Default file extension
            dlg.CheckFileExists = false;
            dlg.Filter = "ChabuTestDirector (*.tdir)|*.tdir|All files (*.*)|*.*"; // Filter files by extension

            // Show open file dialog box
            var result = dlg.ShowDialog();

            // Process open file dialog box results
            if (result == true)
            {
                // Open document
                string filename = dlg.FileName;
                MessageBox.Show("FileName: " + filename);
                MenuMru.InsertFile(filename);
            }

        }

        private void MenuFileClose(object sender, RoutedEventArgs e)
        {
        }

        private void MenuFileSave(object sender, RoutedEventArgs e)
        {

        }

        private void MenuFileSaveAs(object sender, RoutedEventArgs e)
        {

        }

        private void MenuExit(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private void MenuHelpAbout_Click(object sender, RoutedEventArgs e)
        {
            var dlg = new AboutBox();
            dlg.ShowDialog();
        }

        private void MenuLogClear_Click(object sender, RoutedEventArgs e)
        {
            _logContainer.Clear();
        }

        private async void ButtonStart(object sender, RoutedEventArgs e)
        {
            BtnStart.IsEnabled = false;
            try
            {
                _logContainer.Add(@"cmbTests.SelectedItem : {0}", CmbTests.SelectedItem);
                _logContainer.Add(@"cmbTests.SelectedValue: {0}", CmbTests.SelectedValue);
                var ctx = new TestCtx(_logContainer, Settings.Default.HostA, Settings.Default.HostB );
                var test = (ITest)CmbTests.SelectedItem;
                var runner = new TestRunner(test, ctx, Settings.Default.HostA, Settings.Default.HostB );
                await runner.Run();
            }
            finally
            {
                BtnStart.IsEnabled = true;
            }
        }

        private void LoggingUpdated(object sender, EventArgs args)
        {
            var doc = new FlowDocument();
            TxtLogging.Document = doc;
            doc.Blocks.Add( new Paragraph(new Run( _logContainer.GetText() )));
            TxtLogging.ScrollToEnd();
        }

        private void ConfigureHosts_Click(object sender, RoutedEventArgs e)
        {
            var hostA = Settings.Default.HostA;
            var hostB = Settings.Default.HostB;
            var dlg = new TestNodeSetup(hostA, hostB);
            var res = dlg.ShowDialog();

            if (res != true || (hostA.Equals(dlg.HostA) && hostB.Equals(dlg.HostB))) return;

            Settings.Default.HostA = dlg.HostA;
            Settings.Default.HostB = dlg.HostB;
            Settings.Default.Save();
            LogHostSettings();
        }

        private void WindowLoaded(object sender, RoutedEventArgs e)
        {
            LogHostSettings();
        }

        private void LogHostSettings()
        {
            _logContainer.Add("HostA: {0} HostB: {1}", Settings.Default.HostA, Settings.Default.HostB);
        }

        private void CmbTests_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var selectedTest = (ITest)CmbTests.SelectedItem;
            if (selectedTest != null)
            {
                Settings.Default.SelectedTest = selectedTest.Name;
                BtnStart.IsEnabled = true;
                Settings.Default.Save();
            }
        }
    }
}
