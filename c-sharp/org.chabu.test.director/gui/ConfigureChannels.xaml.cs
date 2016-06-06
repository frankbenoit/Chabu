using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
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
    public class Channel
    {
        public String name { get; set; }
        public int priority { get; set; }
    }
    /// <summary>
    /// Interaction logic for ConfigureChannels.xaml
    /// </summary>
    public partial class ConfigureChannels : Window
    {
        public ObservableCollection<Channel> data = new ObservableCollection<Channel>();

        public ConfigureChannels()
        {
            InitializeComponent();
            data.Add(new Channel() { name="HighPrio", priority=3});
            data.Add(new Channel() { name="MidPrio", priority=2 });
            channelGrid.DataContext = data;
        }

        private void btnDialogOk_Click(object sender, RoutedEventArgs e)
        {
            StringBuilder sb = new StringBuilder();
            foreach (Channel c in data)
            {
                sb.Append(String.Format("{0} {1}", c.name, c.priority));
                sb.AppendLine();
            }

            MessageBox.Show(String.Format("Count {0}\r\n{1}", data.Count, sb));
            Close();
        }
    }
}
