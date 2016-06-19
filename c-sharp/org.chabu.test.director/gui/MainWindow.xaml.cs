using System;
using System.Collections.ObjectModel;
using System.Windows;
using System.Windows.Documents;
using org.chabu.test.director.prot;
using org.chabu.test.director.Properties;
using org.chabu.test.director.tests;
using OxyPlot;
using OxyPlot.Series;
using System.Collections.Generic;
using System.Diagnostics;
using Trace = org.chabu.test.director.prot.Trace;

namespace org.chabu.test.director.gui
{

    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow
    {
        private readonly ObservableCollection<ITest> tests = new ObservableCollection<ITest>();
        private readonly ILogContainer logContainer;

        public MainWindow(ILogContainer logContainer)
        {
            this.logContainer = logContainer;
            this.logContainer.Updated += LoggingUpdated;
            InitializeComponent();
            MenuMru.MenuClick += (s, e) => FileOpenCore(e.Filepath);

            ITest selectedTest = null;
            var testFactory = new TestFactory();
            foreach (var test in testFactory.GetList())
            {
                tests.Add(test);
                if (test.Name.Equals(Settings.Default.SelectedTest))
                {
                    selectedTest = test;
                }
            }
            CmbTests.DataContext = tests;

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
            var lineSerie = new StairStepSeries()
            {
                Smooth = true, MarkerType = MarkerType.Diamond
            };
            lineSerie.Points.Add(new DataPoint(0.4, 4));
            lineSerie.Points.Add(new DataPoint(10, 13));
            lineSerie.Points.Add(new DataPoint(20, 15));
            lineSerie.Points.Add(new DataPoint(30, 16));
            lineSerie.Points.Add(new DataPoint(40, 12));
            lineSerie.Points.Add(new DataPoint(50, 12));
            var model = new PlotModel();
            model.Series.Add(lineSerie);
            Plot.Model = model;
        }
        private void FileOpenCore(string filepath)
        {
            MessageBox.Show(filepath);
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
            logContainer.Clear();
        }

        private async void ButtonStart(object sender, RoutedEventArgs e)
        {
            BtnStart.IsEnabled = false;
            try
            {
                logContainer.Add(@"cmbTests.SelectedItem : {0}", CmbTests.SelectedItem);
                logContainer.Add(@"cmbTests.SelectedValue: {0}", CmbTests.SelectedValue);
                var ctx = new TestCtx(logContainer, Settings.Default.HostA, Settings.Default.HostB );
                var test = (ITest)CmbTests.SelectedItem;
                var runner = new TestRunner(test, ctx );
                await runner.Run();

                UpdateTrace(runner.GetTrace());
            }
            finally
            {
                BtnStart.IsEnabled = true;
            }
        }

        private static IEnumerable<StairStepSeries> GetLineSeriesForHost(Trace trace, Host host)
        {
            var res = new List<StairStepSeries>(trace.ChannelCount*2);

            for (var i = 0; i < trace.ChannelCount*2; i++)
            {
                var channelId = i/2;
                var type = i%2 == 0 ? "Recv" : "Xmit";
                res.Add( new StairStepSeries()
                {
                    Smooth = true,
                    MarkerType = MarkerType.Diamond,
                    Title = $@"{host}[{channelId}] {type}"
                });
            }

            foreach (var itemChannel in trace.GetLastTraceItemChannelsFor(host))
            {
                for (var i = 0; i < trace.ChannelCount; i++)
                {
                    if (itemChannel.PreviousTime == long.MinValue)
                    {
                        continue;
                    }
                    var channel = itemChannel.Channels[i];
                    var time0 = (itemChannel.PreviousTime - trace.FirstTime) / (double)Stopwatch.Frequency;
                    var time1 = (itemChannel.Time - trace.FirstTime) / (double)Stopwatch.Frequency;
                    res[i*2].Points.Add(new DataPoint(time0, channel.RecvPositionSpeedKbps));
                    res[i*2].Points.Add(new DataPoint(time1, channel.RecvPositionSpeedKbps));
                    res[i*2+1].Points.Add(new DataPoint(time0, channel.XmitPositionSpeedKbps));
                    res[i*2+1].Points.Add(new DataPoint(time1, channel.XmitPositionSpeedKbps));
                    Console.WriteLine($@"{host}ch[{channel}]: {time0} {time1} -> {channel.RecvPositionSpeedKbps}");

                }
            }
            return res;
        }
        private void UpdateTrace(Trace trace)
        {
            var lineSeries = new List<StairStepSeries>(trace.ChannelCount*2);
            lineSeries.AddRange( GetLineSeriesForHost(trace, Host.A));
            lineSeries.AddRange( GetLineSeriesForHost(trace, Host.B));
            var model = new PlotModel();
            foreach (var serie in lineSeries)
            {
                model.Series.Add(serie);
            }
            Plot.Model = model;
            //model.Series[1].IsVisible
        }

        private void LoggingUpdated(object sender, EventArgs args)
        {
            var doc = new FlowDocument();
            TxtLogging.Document = doc;
            doc.Blocks.Add( new Paragraph(new Run( logContainer.GetText() )));
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
            logContainer.Add("HostA: {0} HostB: {1}", Settings.Default.HostA, Settings.Default.HostB);
        }

        private void CmbTests_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var selectedTest = (ITest)CmbTests.SelectedItem;
            if (selectedTest == null) return;
            Settings.Default.SelectedTest = selectedTest.Name;
            BtnStart.IsEnabled = true;
            Settings.Default.Save();
        }
    }
}
