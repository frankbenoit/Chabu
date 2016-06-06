using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;

namespace org.chabu.test.director.gui
{
    /// <summary>
    /// Interaction logic for TestNodeSetup.xaml
    /// </summary>
    public partial class TestNodeSetup : Window
    {
        public string HostA { get; private set; }
        public string HostB { get; private set; }

        public TestNodeSetup( string hostA, string hostB)
        {
            HostA = hostA;
            HostB = hostB;
            InitializeComponent();
            cmbHostA.Text = hostA;
            cmbHostB.Text = hostB;
        }

        private void ButtonSwap(object sender, RoutedEventArgs e)
        {
            var hostA = cmbHostA.Text;
            var hostB = cmbHostB.Text;
            cmbHostA.Text = hostB;
            cmbHostB.Text = hostA;
        }

        private void ButtonOk(object sender, RoutedEventArgs e)
        {
            HostA = cmbHostA.Text;
            HostB = cmbHostB.Text;
            DialogResult = true;
            Close();
        }
    }
}
